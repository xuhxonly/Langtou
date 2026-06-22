from typing import Any, Dict, List, Optional

import numpy as np

from app.data import redis_client, mysql_client
from app.models.content_model import content_model


class UserFeatureExtractor:
    """Extract user-related features for ranking."""

    def __init__(self):
        self.feature_dim = 32

    def extract(self, user_id: str) -> Dict[str, Any]:
        """Extract all user features."""
        features = {}

        # Basic profile features
        user = mysql_client.get_user(user_id)
        if user:
            features["user_age"] = self._parse_age(user.get("age", 0))
            features["user_gender"] = self._encode_gender(user.get("gender", "unknown"))
            features["user_level"] = user.get("level", 0)
            features["user_register_days"] = user.get("register_days", 0)
        else:
            features["user_age"] = 0
            features["user_gender"] = 0
            features["user_level"] = 0
            features["user_register_days"] = 0

        # Activity features
        activity_features = self._extract_activity_features(user_id)
        features.update(activity_features)

        # Interest features
        interest_features = self._extract_interest_features(user_id)
        features.update(interest_features)

        # Embedding based on user's historical interaction tags TF-IDF vector
        features["user_embedding"] = self._get_user_embedding(user_id)

        return features

    def _parse_age(self, age: Any) -> int:
        try:
            age = int(age)
            if age < 0 or age > 100:
                return 0
            return age
        except (ValueError, TypeError):
            return 0

    def _encode_gender(self, gender: str) -> int:
        gender_map = {"male": 1, "female": 2, "unknown": 0}
        return gender_map.get(gender.lower() if isinstance(gender, str) else "unknown", 0)

    def _extract_activity_features(self, user_id: str) -> Dict[str, float]:
        """Extract user activity features."""
        interactions = mysql_client.get_user_interactions(user_id)

        # Count by type
        type_counts = {}
        for inter in interactions:
            t = inter.get("target_type", "unknown")
            type_counts[t] = type_counts.get(t, 0) + 1

        total = len(interactions) if interactions else 1

        return {
            "user_total_interactions": total,
            "user_like_ratio": type_counts.get(1, 0) / total,
            "user_click_ratio": type_counts.get("click", 0) / total,
            "user_share_ratio": type_counts.get("share", 0) / total,
            "user_comment_ratio": type_counts.get(2, 0) / total,
            "user_collect_ratio": type_counts.get("collect", 0) / total,
            "user_avg_daily_interactions": total / max(1, self._get_user_days_since_register(user_id)),
        }

    def _extract_interest_features(self, user_id: str) -> Dict[str, Any]:
        """Extract user interest features based on tag preferences."""
        tag_key = f"user_tags:{user_id}"
        tags = redis_client.hgetall(tag_key)

        if not tags:
            return {
                "user_interest_entropy": 0.0,
                "user_top_tag_ratio": 0.0,
                "user_tag_count": 0,
            }

        tag_scores = list(tags.values())
        if isinstance(tag_scores[0], str):
            tag_scores = [float(s) for s in tag_scores]

        total_score = sum(tag_scores)
        if total_score == 0:
            return {
                "user_interest_entropy": 0.0,
                "user_top_tag_ratio": 0.0,
                "user_tag_count": len(tags),
            }

        # Calculate entropy of interest distribution
        probs = [s / total_score for s in tag_scores]
        entropy = -sum(p * np.log(p + 1e-10) for p in probs)

        # Top tag ratio
        max_score = max(tag_scores)
        top_tag_ratio = max_score / total_score

        return {
            "user_interest_entropy": entropy,
            "user_top_tag_ratio": top_tag_ratio,
            "user_tag_count": len(tags),
        }

    def _get_user_days_since_register(self, user_id: str) -> int:
        user = mysql_client.get_user(user_id)
        if user:
            return user.get("register_days", 1)
        return 1

    def _get_user_embedding(self, user_id: str) -> List[float]:
        """Get user embedding vector based on historical interaction note tags TF-IDF."""
        interactions = mysql_client.get_user_interactions(user_id)
        if not interactions:
            # Fallback: return zero vector if no interactions
            return np.zeros(self.feature_dim).tolist()

        # Aggregate tag TF-IDF vectors from interacted notes
        vectors = []
        for inter in interactions[:50]:
            note_id = str(inter.get("target_id", ""))
            if not note_id:
                continue
            note = mysql_client.get_note(note_id)
            if not note:
                continue

            tags = note.get("tags", [])
            if isinstance(tags, str):
                tags = [t.strip() for t in tags.split(",") if t.strip()]
            elif isinstance(tags, list):
                tags = [str(t).strip() for t in tags if str(t).strip()]
            else:
                tags = []

            if not tags:
                continue

            # Build a pseudo-document from tags and compute TF-IDF vector
            tag_text = " ".join(tags)
            try:
                vec = content_model.vectorizer.transform([tag_text])
                vectors.append(vec.toarray().flatten())
            except Exception:
                continue

        if not vectors:
            return np.zeros(self.feature_dim).tolist()

        # Average all tag vectors to form user embedding, then pad/truncate to feature_dim
        avg_vector = np.mean(vectors, axis=0)
        if len(avg_vector) >= self.feature_dim:
            return avg_vector[:self.feature_dim].tolist()
        else:
            padded = np.zeros(self.feature_dim)
            padded[:len(avg_vector)] = avg_vector
            return padded.tolist()


user_feature_extractor = UserFeatureExtractor()
