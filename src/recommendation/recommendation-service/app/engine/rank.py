import math
import os
import pickle
from typing import Any, Dict, List, Tuple, Optional

import numpy as np
import xgboost as xgb

from app.data import redis_client, mysql_client
from app.features import item_feature_extractor, user_feature_extractor, context_feature_extractor
from config import get_settings


class FeatureAssembler:
    """Assemble features from user, item, and context into a flat vector."""

    FEATURE_NAMES = [
        # Item features
        "item_title_length",
        "item_content_length",
        "item_has_image",
        "item_has_video",
        "item_tag_count",
        "item_category_encoded",
        "item_likes",
        "item_comments",
        "item_shares",
        "item_views",
        "item_like_rate",
        "item_comment_rate",
        "item_share_rate",
        "item_engagement_score",
        "item_ctr",
        "author_level",
        "author_followers",
        "author_total_notes",
        "author_avg_engagement",
        "item_age_hours",
        "item_is_new",
        "item_is_recent",
        "item_recency_score",
        # User features
        "user_age",
        "user_gender",
        "user_level",
        "user_register_days",
        "user_total_interactions",
        "user_like_ratio",
        "user_click_ratio",
        "user_share_ratio",
        "user_comment_ratio",
        "user_collect_ratio",
        "user_avg_daily_interactions",
        "user_interest_entropy",
        "user_top_tag_ratio",
        "user_tag_count",
        # Context features
        "ctx_hour",
        "ctx_weekday",
        "ctx_is_weekend",
        "ctx_time_period",
        "ctx_is_work_time",
        "ctx_is_leisure_time",
        "ctx_hour_sin",
        "ctx_hour_cos",
        "ctx_device_type",
        "ctx_platform",
        "ctx_app_version",
        "ctx_is_mobile",
        "ctx_city_tier",
        "ctx_has_location",
        "ctx_session_depth",
        "ctx_position",
        "ctx_position_inverse",
        "ctx_prev_interaction",
        # Cross features
        "cross_user_item_level_diff",
        "cross_user_item_tag_match",
    ]

    def assemble(
        self,
        user_features: Dict[str, Any],
        item_features: Dict[str, Any],
        context_features: Dict[str, Any],
    ) -> Tuple[np.ndarray, List[str]]:
        """Assemble features into a numpy array and return feature names."""
        features = {}
        features.update(item_features)
        features.update(user_features)
        features.update(context_features)

        # Compute cross features
        features["cross_user_item_level_diff"] = float(
            user_features.get("user_level", 0) - item_features.get("author_level", 0)
        )

        # Tag match ratio
        tag_key = f"user_tags:{user_features.get('user_id', '')}"
        user_tags = redis_client.hgetall(tag_key)
        note = mysql_client.get_note(item_features.get("note_id", ""))
        note_tags = []
        if note and note.get("tags"):
            tags = note["tags"]
            if isinstance(tags, str):
                note_tags = [t.strip() for t in tags.split(",") if t.strip()]
            elif isinstance(tags, list):
                note_tags = [str(t).strip() for t in tags if str(t).strip()]

        if user_tags and note_tags:
            match_count = sum(1 for t in note_tags if t in user_tags)
            features["cross_user_item_tag_match"] = match_count / len(note_tags)
        else:
            features["cross_user_item_tag_match"] = 0.0

        # Build vector
        vec = []
        for name in self.FEATURE_NAMES:
            val = features.get(name, 0)
            if isinstance(val, (list, tuple)):
                continue
            try:
                vec.append(float(val))
            except (TypeError, ValueError):
                vec.append(0.0)

        return np.array(vec, dtype=np.float32), self.FEATURE_NAMES


class RankModel:
    """XGBoost ranking model wrapper."""

    def __init__(self, model_path: Optional[str] = None):
        self.model: Optional[xgb.Booster] = None
        self.model_path = model_path
        self.feature_assembler = FeatureAssembler()
        if model_path and os.path.exists(model_path):
            self.load(model_path)

    def load(self, path: str):
        """Load model from file."""
        self.model = xgb.Booster()
        self.model.load_model(path)
        self.model_path = path

    def save(self, path: str):
        """Save model to file."""
        if self.model is not None:
            self.model.save_model(path)

    def predict(self, features: np.ndarray) -> float:
        """Predict score for a single sample."""
        if self.model is None:
            return 0.5
        dmatrix = xgb.DMatrix(features.reshape(1, -1))
        pred = self.model.predict(dmatrix)
        return float(pred[0])

    def predict_batch(self, features_list: List[np.ndarray]) -> List[float]:
        """Predict scores for multiple samples."""
        if self.model is None or not features_list:
            return [0.5] * len(features_list)
        X = np.vstack(features_list)
        dmatrix = xgb.DMatrix(X)
        preds = self.model.predict(dmatrix)
        return [float(p) for p in preds]

    def train(
        self,
        X: np.ndarray,
        y: np.ndarray,
        params: Optional[Dict[str, Any]] = None,
        num_round: int = 100,
        eval_set: Optional[Tuple[np.ndarray, np.ndarray]] = None,
    ):
        """Train XGBoost model."""
        if params is None:
            params = {
                "objective": "binary:logistic",
                "eval_metric": "auc",
                "max_depth": 6,
                "eta": 0.1,
                "subsample": 0.8,
                "colsample_bytree": 0.8,
                "min_child_weight": 1,
                "seed": 42,
            }

        dtrain = xgb.DMatrix(X, label=y)
        evals = [(dtrain, "train")]
        if eval_set is not None:
            dval = xgb.DMatrix(eval_set[0], label=eval_set[1])
            evals.append((dval, "val"))

        self.model = xgb.train(params, dtrain, num_round, evals, early_stopping_rounds=10, verbose_eval=10)

    def is_loaded(self) -> bool:
        return self.model is not None


class RankEngine:
    """
    Ranking engine that scores candidates using XGBoost model.
    Falls back to weighted feature combination when model is not available.
    """

    def __init__(self):
        self.settings = get_settings()
        self.feature_assembler = FeatureAssembler()
        model_path = getattr(self.settings, "RANK_MODEL_PATH", "./saved_models/rank_model.json")
        self.rank_model = RankModel(model_path)
        self.feature_weights = {
            "cf_score": 1.0,
            "content_score": 0.8,
            "hot_score": 0.6,
            "user_preference": 0.7,
            "recency": 0.5,
            "quality": 0.4,
            "diversity": 0.3,
        }

    def rank(
        self,
        user_id: str,
        candidates: List[Tuple[str, float]],
        context: Dict[str, Any] = None,
        ab_test_group: Optional[str] = None,
    ) -> List[Tuple[str, float]]:
        """
        Rank candidates and return sorted list of (note_id, score).
        Supports A/B test group to choose different ranking strategies.
        """
        if context is None:
            context = {}

        # A/B test: use weighted fallback for control group if specified
        if ab_test_group == "control":
            return self._weighted_rank(user_id, candidates, context)

        # If XGBoost model is loaded, use it
        if self.rank_model.is_loaded():
            return self._xgboost_rank(user_id, candidates, context)

        # Fallback to weighted ranking
        return self._weighted_rank(user_id, candidates, context)

    def _xgboost_rank(
        self,
        user_id: str,
        candidates: List[Tuple[str, float]],
        context: Dict[str, Any],
    ) -> List[Tuple[str, float]]:
        """Rank using XGBoost model predictions."""
        user_features = user_feature_extractor.extract(user_id)
        context_features = context_feature_extractor.extract(user_id, "", context)

        features_list = []
        valid_candidates = []

        for note_id, base_score in candidates:
            item_features = item_feature_extractor.extract(note_id)
            vec, _ = self.feature_assembler.assemble(user_features, item_features, context_features)
            # Append base_score as an additional feature
            vec = np.append(vec, float(base_score))
            features_list.append(vec)
            valid_candidates.append((note_id, base_score))

        if not features_list:
            return []

        scores = self.rank_model.predict_batch(features_list)
        scored_items = [
            (note_id, score)
            for (note_id, _), score in zip(valid_candidates, scores)
        ]
        scored_items.sort(key=lambda x: x[1], reverse=True)
        return scored_items

    def _weighted_rank(
        self,
        user_id: str,
        candidates: List[Tuple[str, float]],
        context: Dict[str, Any],
    ) -> List[Tuple[str, float]]:
        """Fallback weighted ranking."""
        scored_items = []
        for note_id, base_score in candidates:
            score = self._compute_score(user_id, note_id, base_score, context)
            scored_items.append((note_id, score))
        scored_items.sort(key=lambda x: x[1], reverse=True)
        return scored_items

    def _compute_score(
        self,
        user_id: str,
        note_id: str,
        base_score: float,
        context: Dict[str, Any],
    ) -> float:
        """Compute final ranking score for a note (fallback method)."""
        item_features = item_feature_extractor.extract(note_id)
        user_features = user_feature_extractor.extract(user_id)
        context_features = context_feature_extractor.extract(user_id, note_id, context)

        score = base_score * self.feature_weights["cf_score"]

        quality_score = self._compute_quality_score(item_features)
        score += quality_score * self.feature_weights["quality"]

        preference_score = self._compute_preference_score(user_features, item_features)
        score += preference_score * self.feature_weights["user_preference"]

        recency_score = item_features.get("item_recency_score", 0)
        score += recency_score * self.feature_weights["recency"]

        engagement = item_features.get("item_engagement_score", 0)
        score += math.log1p(engagement) * 0.1

        author_followers = item_features.get("author_followers", 0)
        score += math.log1p(author_followers) * 0.05

        time_score = context_features.get("time_score", 0.5)
        score *= (0.8 + 0.4 * time_score)

        score = 1 / (1 + math.exp(-score))
        return score

    def _compute_quality_score(self, item_features: Dict[str, Any]) -> float:
        """Compute content quality score."""
        title_length = item_features.get("item_title_length", 0)
        content_length = item_features.get("item_content_length", 0)
        has_image = item_features.get("item_has_image", 0)
        has_video = item_features.get("item_has_video", 0)
        tag_count = item_features.get("item_tag_count", 0)

        score = 0
        score += min(title_length / 20, 1.0) * 0.2
        score += min(content_length / 500, 1.0) * 0.3
        score += has_image * 0.2
        score += has_video * 0.3
        score += min(tag_count / 5, 1.0) * 0.1

        return score

    def _compute_preference_score(
        self,
        user_features: Dict[str, Any],
        item_features: Dict[str, Any],
    ) -> float:
        """Compute user-item preference match score."""
        user_id = user_features.get("user_id", "")
        tag_key = f"user_tags:{user_id}"
        user_tags = redis_client.hgetall(tag_key)

        if not user_tags:
            return 0.5

        note = mysql_client.get_note(item_features.get("note_id", ""))
        if not note or not note.get("tags"):
            return 0.5

        note_tags = note.get("tags", [])
        if isinstance(note_tags, str):
            note_tags = [t.strip() for t in note_tags.split(",") if t.strip()]
        elif isinstance(note_tags, list):
            note_tags = [str(t).strip() for t in note_tags if str(t).strip()]
        else:
            note_tags = []

        if not note_tags:
            return 0.5

        match_score = 0
        for tag in note_tags:
            if tag in user_tags:
                match_score += float(user_tags[tag])

        max_possible = sum(float(v) for v in user_tags.values())
        if max_possible > 0:
            match_score = match_score / max_possible

        return min(match_score, 1.0)

    def update_weights(self, weights: Dict[str, float]):
        """Update feature weights (for A/B testing)."""
        self.feature_weights.update(weights)

    def load_model(self, path: str):
        """Load XGBoost model from file."""
        self.rank_model.load(path)

    def train_model(
        self,
        training_data: List[Dict[str, Any]],
        model_type: str = "xgboost",
        save_path: Optional[str] = None,
    ):
        """
        Train ranking model on click/impression logs.
        training_data: list of dicts with keys:
            - user_id, note_id, label (1 for click/like, 0 for impression only),
            - context (optional)
        """
        if not training_data:
            print("No training data provided, skipping training.")
            return

        X_list = []
        y_list = []

        for sample in training_data:
            user_id = str(sample.get("user_id", ""))
            note_id = str(sample.get("note_id", ""))
            label = float(sample.get("label", 0))
            context = sample.get("context", {})

            user_features = user_feature_extractor.extract(user_id)
            item_features = item_feature_extractor.extract(note_id)
            context_features = context_feature_extractor.extract(user_id, note_id, context)

            vec, _ = self.feature_assembler.assemble(user_features, item_features, context_features)
            # Add base_score as extra feature (default 0 for training)
            vec = np.append(vec, 0.0)

            X_list.append(vec)
            y_list.append(label)

        X = np.vstack(X_list)
        y = np.array(y_list, dtype=np.float32)

        print(f"Training data shape: X={X.shape}, y={y.shape}")
        print(f"Positive samples: {sum(y)}, Negative samples: {len(y) - sum(y)}")

        if model_type == "xgboost":
            # Split train/val
            from sklearn.model_selection import train_test_split
            X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

            self.rank_model.train(X_train, y_train, eval_set=(X_val, y_val))

            if save_path:
                self.rank_model.save(save_path)
                print(f"Model saved to {save_path}")
        else:
            raise ValueError(f"Unsupported model type: {model_type}")

        print("Training completed.")


rank_engine = RankEngine()
