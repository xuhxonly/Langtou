import os
import pickle
from typing import Any, Dict, List, Tuple

import numpy as np
import pandas as pd

from app.features import user_feature_extractor, item_feature_extractor, context_feature_extractor
from config import get_settings


class FeatureAssembler:
    """Assemble features from multiple sources into a flat feature vector."""

    def __init__(self):
        self.user_feature_names = []
        self.item_feature_names = []
        self.context_feature_names = []
        self._init_feature_names()

    def _init_feature_names(self):
        """Initialize feature names by extracting from a dummy sample."""
        # These will be populated when we actually extract features
        # For now, define the expected feature names
        self.user_feature_names = [
            "user_age", "user_gender", "user_level", "user_register_days",
            "user_total_interactions", "user_like_ratio", "user_click_ratio",
            "user_share_ratio", "user_comment_ratio", "user_collect_ratio",
            "user_avg_daily_interactions", "user_interest_entropy",
            "user_top_tag_ratio", "user_tag_count",
        ]

        self.item_feature_names = [
            "item_title_length", "item_content_length", "item_has_image",
            "item_has_video", "item_tag_count", "item_category_encoded",
            "item_likes", "item_comments", "item_shares", "item_views",
            "item_like_rate", "item_comment_rate", "item_share_rate",
            "item_engagement_score", "item_ctr", "author_level",
            "author_followers", "author_total_notes", "author_avg_engagement",
            "item_age_hours", "item_is_new", "item_is_recent", "item_recency_score",
        ]

        self.context_feature_names = [
            "ctx_hour", "ctx_weekday", "ctx_is_weekend", "ctx_time_period",
            "ctx_is_work_time", "ctx_is_leisure_time", "ctx_hour_sin",
            "ctx_hour_cos", "ctx_device_type", "ctx_platform", "ctx_app_version",
            "ctx_is_mobile", "ctx_city_tier", "ctx_has_location",
            "ctx_session_depth", "ctx_position", "ctx_position_inverse",
            "ctx_prev_interaction",
        ]

    def assemble(self, user_id: str, note_id: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """Assemble all features for a user-item pair."""
        user_features = user_feature_extractor.extract(user_id)
        item_features = item_feature_extractor.extract(note_id)
        context_features = context_feature_extractor.extract(user_id, note_id, context)

        # Combine all features
        features = {}
        features.update({k: v for k, v in user_features.items() if k != "user_embedding"})
        features.update({k: v for k, v in item_features.items() if k != "item_embedding"})
        features.update(context_features)

        # Add embedding dot product as cross feature
        user_emb = user_features.get("user_embedding", [])
        item_emb = item_features.get("item_embedding", [])
        if user_emb and item_emb:
            features["embedding_dot"] = np.dot(user_emb, item_emb)
            features["embedding_sim"] = features["embedding_dot"] / (
                np.linalg.norm(user_emb) * np.linalg.norm(item_emb) + 1e-10
            )
        else:
            features["embedding_dot"] = 0.0
            features["embedding_sim"] = 0.0

        return features

    def to_vector(self, features: Dict[str, Any]) -> np.ndarray:
        """Convert feature dict to numpy vector."""
        all_names = self.user_feature_names + self.item_feature_names + self.context_feature_names + ["embedding_dot", "embedding_sim"]
        return np.array([float(features.get(name, 0.0)) for name in all_names])

    def get_feature_names(self) -> List[str]:
        """Get all feature names."""
        return self.user_feature_names + self.item_feature_names + self.context_feature_names + ["embedding_dot", "embedding_sim"]


class RankModel:
    """Ranking model wrapper supporting XGBoost and LightGBM."""

    def __init__(self, model_path: str = None):
        self.settings = get_settings()
        self.model = None
        self.model_type = None
        self.feature_assembler = FeatureAssembler()
        self.model_path = model_path or os.path.join(self.settings.MODEL_DIR, "rank_model.pkl")

    def load(self, path: str = None) -> bool:
        """Load model from file."""
        path = path or self.model_path
        if not os.path.exists(path):
            print(f"Model file not found: {path}")
            return False

        try:
            with open(path, "rb") as f:
                self.model = pickle.load(f)

            # Detect model type
            model_class = type(self.model).__name__
            if "XGB" in model_class:
                self.model_type = "xgboost"
            elif "LGBM" in model_class or "Booster" in model_class:
                self.model_type = "lightgbm"
            else:
                self.model_type = "sklearn"

            return True
        except Exception as e:
            print(f"Failed to load model: {e}")
            return False

    def save(self, path: str = None) -> bool:
        """Save model to file."""
        path = path or self.model_path
        os.makedirs(os.path.dirname(path), exist_ok=True)

        try:
            with open(path, "wb") as f:
                pickle.dump(self.model, f)
            return True
        except Exception as e:
            print(f"Failed to save model: {e}")
            return False

    def predict(self, features_list: List[Dict[str, Any]]) -> np.ndarray:
        """Predict scores for a batch of feature dicts."""
        if self.model is None:
            # Fallback: use heuristic scoring
            return self._heuristic_predict(features_list)

        # Convert to feature matrix
        X = np.array([self.feature_assembler.to_vector(f) for f in features_list])

        try:
            if self.model_type == "xgboost":
                import xgboost as xgb
                dmatrix = xgb.DMatrix(X)
                return self.model.predict(dmatrix)
            elif self.model_type == "lightgbm":
                return self.model.predict(X)
            else:
                return self.model.predict_proba(X)[:, 1] if hasattr(self.model, "predict_proba") else self.model.predict(X)
        except Exception as e:
            print(f"Model prediction failed: {e}, using heuristic fallback")
            return self._heuristic_predict(features_list)

    def _heuristic_predict(self, features_list: List[Dict[str, Any]]) -> np.ndarray:
        """Heuristic scoring when model is not available."""
        scores = []
        for f in features_list:
            # Simple heuristic combining multiple signals
            score = (
                f.get("item_engagement_score", 0) * 0.3 +
                f.get("item_recency_score", 0) * 0.2 +
                f.get("embedding_sim", 0) * 0.2 +
                f.get("author_level", 0) * 0.05 +
                f.get("author_followers", 0) * 0.0001 +
                (1.0 / (f.get("ctx_position", 0) + 1)) * 0.1 +
                f.get("user_like_ratio", 0) * 0.1 +
                f.get("item_ctr", 0) * 0.05
            )
            scores.append(score)
        return np.array(scores)

    def train(self, X: np.ndarray, y: np.ndarray, model_type: str = "xgboost", params: Dict = None):
        """Train a ranking model."""
        if model_type == "xgboost":
            import xgboost as xgb
            default_params = {
                "objective": "binary:logistic",
                "eval_metric": "auc",
                "max_depth": 6,
                "learning_rate": 0.1,
                "n_estimators": 100,
                "subsample": 0.8,
                "colsample_bytree": 0.8,
            }
            if params:
                default_params.update(params)
            self.model = xgb.XGBClassifier(**default_params)
            self.model_type = "xgboost"
        elif model_type == "lightgbm":
            import lightgbm as lgb
            default_params = {
                "objective": "binary",
                "metric": "auc",
                "boosting_type": "gbdt",
                "num_leaves": 31,
                "learning_rate": 0.05,
                "feature_fraction": 0.9,
                "bagging_fraction": 0.8,
                "bagging_freq": 5,
                "verbose": -1,
            }
            if params:
                default_params.update(params)
            self.model = lgb.LGBMClassifier(**default_params)
            self.model_type = "lightgbm"
        else:
            from sklearn.ensemble import GradientBoostingClassifier
            self.model = GradientBoostingClassifier()
            self.model_type = "sklearn"

        self.model.fit(X, y)
        return self


class RankEngine:
    """Ranking engine that scores and sorts candidate items."""

    def __init__(self):
        self.settings = get_settings()
        self.model = RankModel()
        self.feature_assembler = FeatureAssembler()

    def load_model(self, path: str = None) -> bool:
        """Load ranking model."""
        return self.model.load(path)

    def rank(self, user_id: str, candidates: List[Tuple[str, float]], context: Dict[str, Any] = None) -> List[Tuple[str, float]]:
        """
        Rank candidate items and return sorted list of (item_id, score).
        """
        if context is None:
            context = {}

        if not candidates:
            return []

        # Assemble features for all candidates
        features_list = []
        for item_id, _ in candidates:
            features = self.feature_assembler.assemble(user_id, item_id, context)
            features_list.append(features)

        # Predict scores
        scores = self.model.predict(features_list)

        # Combine with recall score (weighted ensemble)
        ranked = []
        for i, (item_id, recall_score) in enumerate(candidates):
            final_score = scores[i] * 0.7 + recall_score * 0.3
            ranked.append((item_id, final_score))

        # Sort by final score
        ranked.sort(key=lambda x: x[1], reverse=True)

        return ranked[:self.settings.RANK_NUM]

    def train_model(self, training_data: List[Dict[str, Any]], model_type: str = "xgboost", params: Dict = None):
        """Train ranking model from training data."""
        # training_data: list of dicts with user_id, note_id, context, label
        features_list = []
        labels = []

        for sample in training_data:
            features = self.feature_assembler.assemble(
                sample["user_id"],
                sample["note_id"],
                sample.get("context", {})
            )
            features_list.append(features)
            labels.append(sample["label"])

        X = np.array([self.feature_assembler.to_vector(f) for f in features_list])
        y = np.array(labels)

        self.model.train(X, y, model_type=model_type, params=params)
        self.model.save()

        return self


rank_engine = RankEngine()
