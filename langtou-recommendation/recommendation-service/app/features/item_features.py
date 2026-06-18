import logging
import math
from datetime import datetime
from typing import Any, Dict, List, Optional

import numpy as np

from app.data import redis_client, mysql_client, es_client
from app.models.content_model import content_model

logger = logging.getLogger(__name__)


class ImageFeatureExtractor:
    """
    图片特征提取器

    提供多层次的图片特征提取能力：
    1. 基础特征：图片数量、宽高比等统计特征（立即可用）
    2. CNN特征：基于ResNet50的深度特征提取（预留接口，需安装torch/torchvision）
    3. 多模态融合：将图片特征与文本特征融合为统一向量
    """

    # ResNet50输出维度（预留）
    CNN_FEATURE_DIM = 2048
    # 融合后的特征维度
    FUSED_FEATURE_DIM = 256

    def __init__(self):
        self._cnn_model = None
        self._cnn_preprocess = None
        self._cnn_ready = False

    def _init_cnn_model(self):
        """
        延迟初始化CNN模型（ResNet50）。

        需要安装: pip install torch torchvision
        仅在首次调用CNN特征提取时加载模型。
        """
        if self._cnn_ready:
            return

        try:
            import torch
            from torchvision import models, transforms

            # 加载预训练ResNet50，去掉最后的全连接层
            self._cnn_model = models.resnet50(pretrained=True)
            self._cnn_model.eval()

            # 移除最后的FC层，保留特征提取部分
            self._cnn_model = torch.nn.Sequential(*list(self._cnn_model.children())[:-1])

            # 标准预处理
            self._cnn_preprocess = transforms.Compose([
                transforms.Resize(256),
                transforms.CenterCrop(224),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                     std=[0.229, 0.224, 0.225]),
            ])

            self._cnn_ready = True
            logger.info("ResNet50 CNN模型加载成功")
        except ImportError:
            logger.warning(
                "torch/torchvision未安装，CNN特征提取不可用。"
                "安装方式: pip install torch torchvision"
            )
        except Exception as e:
            logger.error(f"CNN模型加载失败: {e}")

    def extract_basic_features(self, note: Dict[str, Any]) -> Dict[str, Any]:
        """
        提取图片基础特征（无需深度学习模型）。

        基于笔记的图片元数据提取统计特征：
        - 图片数量
        - 是否有封面图
        - 图片URL列表（用于后续CNN处理）

        Args:
            note: 笔记数据字典

        Returns:
            图片基础特征字典
        """
        images = note.get("images", [])
        cover_url = note.get("cover_url", "")

        if isinstance(images, str):
            try:
                import json
                images = json.loads(images)
            except Exception:
                images = []

        image_count = len(images) if images else 0

        return {
            "image_count": image_count,
            "has_cover": 1 if cover_url else 0,
            "has_multi_images": 1 if image_count > 1 else 0,
            "image_count_normalized": min(image_count / 9.0, 1.0),  # 归一化，最多9张
        }

    def extract_cnn_features(self, image_urls: List[str]) -> Optional[np.ndarray]:
        """
        使用ResNet50提取图片CNN特征（预留接口）。

        对每张图片提取2048维特征向量，多张图片取平均值。

        Args:
            image_urls: 图片URL列表

        Returns:
            平均CNN特征向量 (shape: [2048])，如果模型不可用返回None
        """
        if not image_urls:
            return None

        self._init_cnn_model()
        if not self._cnn_ready or self._cnn_model is None:
            return None

        try:
            import torch
            from PIL import Image
            import requests
            from io import BytesIO

            features_list = []

            for url in image_urls[:5]:  # 最多处理5张图片
                try:
                    response = requests.get(url, timeout=5)
                    img = Image.open(BytesIO(response.content)).convert("RGB")
                    img_tensor = self._cnn_preprocess(img).unsqueeze(0)

                    with torch.no_grad():
                        feat = self._cnn_model(img_tensor).squeeze().numpy()

                    features_list.append(feat)
                except Exception as e:
                    logger.warning(f"提取图片CNN特征失败: url={url}, error={e}")
                    continue

            if features_list:
                avg_features = np.mean(features_list, axis=0)
                return avg_features.flatten()
            return None

        except Exception as e:
            logger.error(f"CNN特征提取异常: {e}")
            return None

    def extract_image_features(self, note: Dict[str, Any]) -> Dict[str, Any]:
        """
        提取完整的图片特征（基础特征 + CNN特征）。

        Args:
            note: 笔记数据字典

        Returns:
            包含基础特征和CNN特征的字典
        """
        features = self.extract_basic_features(note)

        images = note.get("images", [])
        if isinstance(images, str):
            try:
                import json
                images = json.loads(images)
            except Exception:
                images = []

        # 尝试提取CNN特征
        cnn_features = self.extract_cnn_features(images)
        if cnn_features is not None:
            features["image_cnn_embedding"] = cnn_features.tolist()
            features["image_cnn_dim"] = len(cnn_features)
        else:
            features["image_cnn_embedding"] = None
            features["image_cnn_dim"] = 0

        return features

    @staticmethod
    def fuse_text_image_features(
        text_embedding: List[float],
        image_embedding: Optional[List[float]],
        text_weight: float = 0.6,
        image_weight: float = 0.4,
        target_dim: int = 256,
    ) -> List[float]:
        """
        融合文本特征和图片特征为多模态向量。

        使用加权拼接 + 线性投影的方式融合：
        1. 对文本和图片特征分别进行L2归一化
        2. 加权拼接
        3. 如果维度超过target_dim，使用截断；不足则补零

        Args:
            text_embedding: 文本TF-IDF/embedding向量
            image_embedding: 图片CNN特征向量（可为None）
            text_weight: 文本特征权重
            image_weight: 图片特征权重
            target_dim: 融合后的目标维度

        Returns:
            融合后的多模态特征向量
        """
        text_vec = np.array(text_embedding, dtype=np.float32)

        # L2归一化
        text_norm = np.linalg.norm(text_vec)
        if text_norm > 0:
            text_vec = text_vec / text_norm
        text_vec = text_vec * text_weight

        if image_embedding is not None and len(image_embedding) > 0:
            image_vec = np.array(image_embedding, dtype=np.float32)
            image_norm = np.linalg.norm(image_vec)
            if image_norm > 0:
                image_vec = image_vec / image_norm
            image_vec = image_vec * image_weight

            # 拼接
            fused = np.concatenate([text_vec, image_vec])
        else:
            # 无图片特征时，仅使用文本特征
            fused = text_vec

        # 截断或补零到目标维度
        if len(fused) >= target_dim:
            return fused[:target_dim].tolist()
        else:
            padded = np.zeros(target_dim, dtype=np.float32)
            padded[:len(fused)] = fused
            return padded.tolist()


# 全局图片特征提取器实例
image_feature_extractor = ImageFeatureExtractor()


class ItemFeatureExtractor:
    """Extract item (note) related features for ranking."""

    def __init__(self):
        self.feature_dim = 32

    def extract(self, note_id: str) -> Dict[str, Any]:
        """Extract all item features (including multimodal image features)."""
        features = {}

        # Get note info
        note = mysql_client.get_note(note_id)
        if not note:
            # Try ES
            note = es_client.get_note(note_id)

        if note:
            features.update(self._extract_content_features(note))
            features.update(self._extract_engagement_features(note))
            features.update(self._extract_author_features(note))
            features.update(self._extract_temporal_features(note))
            # 多模态图片特征
            features.update(image_feature_extractor.extract_image_features(note))
        else:
            # Default features
            features = self._default_features()

        # Embedding based on note's own tags TF-IDF vector
        text_embedding = self._get_item_embedding(note_id, note)

        # 多模态融合：将文本embedding与图片CNN特征融合
        image_cnn = features.get("image_cnn_embedding")
        features["item_embedding"] = ImageFeatureExtractor.fuse_text_image_features(
            text_embedding, image_cnn
        )
        features["multimodal_embedding"] = features["item_embedding"]

        return features

    def _extract_content_features(self, note: Dict[str, Any]) -> Dict[str, Any]:
        """Extract content-related features."""
        title = note.get("title", "")
        content = note.get("content", "")
        tags = note.get("tags", "")

        if isinstance(tags, str):
            tags = [t.strip() for t in tags.split(",") if t.strip()]
        elif isinstance(tags, list):
            tags = [str(t).strip() for t in tags if str(t).strip()]
        else:
            tags = []

        return {
            "item_title_length": len(title) if title else 0,
            "item_content_length": len(content) if content else 0,
            "item_has_image": 1 if note.get("images") else 0,
            "item_has_video": 1 if note.get("video_url") else 0,
            "item_tag_count": len(tags),
            "item_category_encoded": self._encode_category(note.get("category", "")),
        }

    def _extract_engagement_features(self, note: Dict[str, Any]) -> Dict[str, float]:
        """Extract engagement statistics."""
        likes = note.get("like_count", 0)
        comments = note.get("comment_count", 0)
        shares = note.get("share_count", 0)
        views = note.get("view_count", 1)

        # Avoid division by zero
        views = max(views, 1)

        # Calculate rates
        like_rate = likes / views
        comment_rate = comments / views
        share_rate = shares / views

        # Engagement score (weighted sum)
        engagement_score = likes * 1.0 + comments * 2.0 + shares * 3.0

        return {
            "item_likes": likes,
            "item_comments": comments,
            "item_shares": shares,
            "item_views": views,
            "item_like_rate": like_rate,
            "item_comment_rate": comment_rate,
            "item_share_rate": share_rate,
            "item_engagement_score": engagement_score,
            "item_ctr": like_rate + comment_rate + share_rate,
        }

    def _extract_author_features(self, note: Dict[str, Any]) -> Dict[str, Any]:
        """Extract author-related features."""
        author_id = note.get("user_id")

        if not author_id:
            return {
                "author_level": 0,
                "author_followers": 0,
                "author_total_notes": 0,
                "author_avg_engagement": 0.0,
            }

        # Try to get from Redis cache
        cache_key = f"author_stats:{author_id}"
        cached = redis_client.hgetall(cache_key)

        if cached:
            return {
                "author_level": float(cached.get("level", 0)),
                "author_followers": float(cached.get("followers", 0)),
                "author_total_notes": float(cached.get("total_notes", 0)),
                "author_avg_engagement": float(cached.get("avg_engagement", 0)),
            }

        # Fallback: get from MySQL
        author = mysql_client.get_user(author_id)
        if author:
            return {
                "author_level": author.get("level", 0),
                "author_followers": author.get("follower_count", 0),
                "author_total_notes": author.get("note_count", 0),
                "author_avg_engagement": author.get("avg_engagement", 0.0),
            }

        return {
            "author_level": 0,
            "author_followers": 0,
            "author_total_notes": 0,
            "author_avg_engagement": 0.0,
        }

    def _extract_temporal_features(self, note: Dict[str, Any]) -> Dict[str, float]:
        """Extract time-related features."""
        create_time = note.get("created_at")

        if not create_time:
            return {
                "item_age_hours": 0.0,
                "item_is_new": 0,
                "item_is_recent": 0,
            }

        # Parse create_time
        if isinstance(create_time, str):
            try:
                create_time = datetime.fromisoformat(create_time.replace("Z", "+00:00"))
            except Exception:
                return {
                    "item_age_hours": 0.0,
                    "item_is_new": 0,
                    "item_is_recent": 0,
                }

        now = datetime.now()
        if create_time.tzinfo:
            from datetime import timezone
            now = now.replace(tzinfo=timezone.utc)

        age_hours = (now - create_time).total_seconds() / 3600

        return {
            "item_age_hours": age_hours,
            "item_is_new": 1 if age_hours < 24 else 0,
            "item_is_recent": 1 if age_hours < 72 else 0,
            "item_recency_score": math.exp(-age_hours / 168),  # Decay over a week
        }

    def _encode_category(self, category: str) -> int:
        """Encode category to integer."""
        categories = [
            "美食", "旅行", "时尚", "美妆", "家居", "健身",
            "摄影", "读书", "音乐", "电影", "科技", "游戏",
            "宠物", "母婴", "职场", "学习", "其他"
        ]
        try:
            return categories.index(category) if category in categories else len(categories)
        except Exception:
            return 0

    def _get_item_embedding(self, note_id: str, note: Optional[Dict[str, Any]] = None) -> List[float]:
        """Get item embedding vector based on note's own tags TF-IDF."""
        if note is None:
            note = mysql_client.get_note(note_id)
            if not note:
                note = es_client.get_note(note_id)

        if not note:
            return np.zeros(self.feature_dim).tolist()

        tags = note.get("tags", [])
        if isinstance(tags, str):
            tags = [t.strip() for t in tags.split(",") if t.strip()]
        elif isinstance(tags, list):
            tags = [str(t).strip() for t in tags if str(t).strip()]
        else:
            tags = []

        if not tags:
            return np.zeros(self.feature_dim).tolist()

        # Use content_model's vectorizer to compute TF-IDF vector from tags
        tag_text = " ".join(tags)
        try:
            vec = content_model.vectorizer.transform([tag_text])
            tfidf_vec = vec.toarray().flatten()
        except Exception:
            # If vectorizer not fitted, fallback to content_model compute method
            tfidf_vec = content_model.compute_note_embedding(note_id)

        # Pad or truncate to feature_dim
        if len(tfidf_vec) >= self.feature_dim:
            return tfidf_vec[:self.feature_dim].tolist()
        else:
            padded = np.zeros(self.feature_dim)
            padded[:len(tfidf_vec)] = tfidf_vec
            return padded.tolist()

    def _default_features(self) -> Dict[str, Any]:
        """Default features when note not found."""
        return {
            "item_title_length": 0,
            "item_content_length": 0,
            "item_has_image": 0,
            "item_has_video": 0,
            "item_tag_count": 0,
            "item_category_encoded": 0,
            "item_likes": 0,
            "item_comments": 0,
            "item_shares": 0,
            "item_views": 1,
            "item_like_rate": 0.0,
            "item_comment_rate": 0.0,
            "item_share_rate": 0.0,
            "item_engagement_score": 0.0,
            "item_ctr": 0.0,
            "author_level": 0,
            "author_followers": 0,
            "author_total_notes": 0,
            "author_avg_engagement": 0.0,
            "item_age_hours": 0.0,
            "item_is_new": 0,
            "item_is_recent": 0,
            "item_recency_score": 0.0,
            # 多模态图片特征默认值
            "image_count": 0,
            "has_cover": 0,
            "has_multi_images": 0,
            "image_count_normalized": 0.0,
            "image_cnn_embedding": None,
            "image_cnn_dim": 0,
        }


item_feature_extractor = ItemFeatureExtractor()
