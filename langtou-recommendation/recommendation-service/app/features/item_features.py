import math
from datetime import datetime
from typing import Any, Dict, List, Optional

import numpy as np

from app.data import redis_client, mysql_client, es_client


class ItemFeatureExtractor:
    """Extract item (note) related features for ranking."""

    def __init__(self):
        self.feature_dim = 32

    def extract(self, note_id: str) -> Dict[str, Any]:
        """Extract all item features."""
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
        else:
            # Default features
            features = self._default_features()

        # Embedding
        features["item_embedding"] = self._get_item_embedding(note_id)

        return features

    def _extract_content_features(self, note: Dict[str, Any]) -> Dict[str, Any]:
        """Extract content-related features."""
        title = note.get("title", "")
        content = note.get("content", "")
        tags = note.get("tags", "")

        if isinstance(tags, str):
            tags = [t.strip() for t in tags.split(",") if t.strip()]

        return {
            "item_title_length": len(title) if title else 0,
            "item_content_length": len(content) if content else 0,
            "item_has_image": 1 if note.get("has_image") else 0,
            "item_has_video": 1 if note.get("has_video") else 0,
            "item_tag_count": len(tags),
            "item_category_encoded": self._encode_category(note.get("category", "")),
        }

    def _extract_engagement_features(self, note: Dict[str, Any]) -> Dict[str, float]:
        """Extract engagement statistics."""
        likes = note.get("likes", 0)
        comments = note.get("comments", 0)
        shares = note.get("shares", 0)
        views = note.get("views", 1)

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
        author_id = note.get("author_id")

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
                "author_followers": author.get("followers", 0),
                "author_total_notes": author.get("total_notes", 0),
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
        create_time = note.get("create_time")

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

    def _get_item_embedding(self, note_id: str) -> List[float]:
        """Get item embedding vector (mock implementation)."""
        np.random.seed(hash(note_id) % 2**32)
        return np.random.randn(self.feature_dim).tolist()

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
        }


item_feature_extractor = ItemFeatureExtractor()
