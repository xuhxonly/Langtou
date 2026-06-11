import math
from typing import Any, Dict, List, Tuple

import numpy as np

from app.data import redis_client, mysql_client
from app.features import item_feature_extractor, user_feature_extractor, context_feature_extractor
from config import get_settings


class RankEngine:
    """
    Ranking engine that scores candidates using a weighted feature combination.
    In production, this should use a trained ML model (e.g., XGBoost, LightGBM, or neural network).
    """

    def __init__(self):
        self.settings = get_settings()
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
    ) -> List[Tuple[str, float]]:
        """
        Rank candidates and return sorted list of (note_id, score).
        """
        if context is None:
            context = {}

        scored_items = []
        for note_id, base_score in candidates:
            score = self._compute_score(user_id, note_id, base_score, context)
            scored_items.append((note_id, score))

        # Sort by score descending
        scored_items.sort(key=lambda x: x[1], reverse=True)

        return scored_items

    def _compute_score(
        self,
        user_id: str,
        note_id: str,
        base_score: float,
        context: Dict[str, Any],
    ) -> float:
        """Compute final ranking score for a note."""
        # Get features
        item_features = item_feature_extractor.extract(note_id)
        user_features = user_feature_extractor.extract(user_id)
        context_features = context_feature_extractor.extract(context)

        # Combine scores
        score = base_score * self.feature_weights["cf_score"]

        # Content quality score
        quality_score = self._compute_quality_score(item_features)
        score += quality_score * self.feature_weights["quality"]

        # User preference match
        preference_score = self._compute_preference_score(user_features, item_features)
        score += preference_score * self.feature_weights["user_preference"]

        # Recency boost
        recency_score = item_features.get("item_recency_score", 0)
        score += recency_score * self.feature_weights["recency"]

        # Engagement boost
        engagement = item_features.get("item_engagement_score", 0)
        score += math.log1p(engagement) * 0.1

        # Author quality boost
        author_followers = item_features.get("author_followers", 0)
        score += math.log1p(author_followers) * 0.05

        # Context features
        time_score = context_features.get("time_score", 0.5)
        score *= (0.8 + 0.4 * time_score)

        # Apply sigmoid to normalize
        score = 1 / (1 + math.exp(-score))

        return score

    def _compute_quality_score(self, item_features: Dict[str, Any]) -> float:
        """Compute content quality score."""
        title_length = item_features.get("item_title_length", 0)
        content_length = item_features.get("item_content_length", 0)
        has_image = item_features.get("item_has_image", 0)
        has_video = item_features.get("item_has_video", 0)
        tag_count = item_features.get("item_tag_count", 0)

        # Quality heuristics
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
        # Get user tags from Redis
        user_id = user_features.get("user_id", "")
        tag_key = f"user_tags:{user_id}"
        user_tags = redis_client.hgetall(tag_key)

        if not user_tags:
            return 0.5

        # Get note tags
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

        # Compute tag overlap
        if not note_tags:
            return 0.5

        match_score = 0
        for tag in note_tags:
            if tag in user_tags:
                match_score += float(user_tags[tag])

        # Normalize
        max_possible = sum(float(v) for v in user_tags.values())
        if max_possible > 0:
            match_score = match_score / max_possible

        return min(match_score, 1.0)

    def update_weights(self, weights: Dict[str, float]):
        """Update feature weights (for A/B testing)."""
        self.feature_weights.update(weights)


rank_engine = RankEngine()
