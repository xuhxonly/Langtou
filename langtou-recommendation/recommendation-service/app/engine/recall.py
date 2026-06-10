import random
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Set, Tuple

import numpy as np

from app.data import redis_client, es_client, mysql_client
from config import get_settings


class RecallStrategy(ABC):
    """Abstract base class for recall strategies."""

    def __init__(self, name: str, weight: float = 1.0):
        self.name = name
        self.weight = weight
        self.settings = get_settings()

    @abstractmethod
    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        """
        Return list of (item_id, score) tuples.
        """
        pass


class CFRecall(RecallStrategy):
    """Collaborative Filtering Recall based on user behavior."""

    def __init__(self, weight: float = 1.0):
        super().__init__("cf", weight)

    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        # Get user's interaction history from Redis cache
        user_key = f"user_history:{user_id}"
        history = redis_client.lrange(user_key, 0, 100)

        if not history:
            # Fallback to MySQL
            interactions = mysql_client.get_user_interactions(user_id)
            history = [i["note_id"] for i in interactions[:50]]

        if not history:
            return []

        # Item-based CF: find similar items
        candidate_scores: Dict[str, float] = {}

        for note_id in history:
            # Get similar items from Redis
            similar_key = f"item_sim:{note_id}"
            similar_items = redis_client.zrevrange(similar_key, 0, 20, withscores=True)

            for similar_id, sim_score in similar_items:
                similar_id = similar_id.decode("utf-8") if isinstance(similar_id, bytes) else similar_id
                if similar_id not in history:
                    candidate_scores[similar_id] = candidate_scores.get(similar_id, 0) + float(sim_score)

        # Sort by score and return top N
        sorted_items = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
        return sorted_items[:num]


class ContentRecall(RecallStrategy):
    """Content-based Recall based on tags and text similarity."""

    def __init__(self, weight: float = 0.8):
        super().__init__("content", weight)

    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        # Get user's interest tags from Redis
        tag_key = f"user_tags:{user_id}"
        user_tags = redis_client.hgetall(tag_key)

        if not user_tags:
            # Fallback: get tags from user's liked notes
            interactions = mysql_client.get_user_interactions(user_id, "like")
            tag_counts: Dict[str, int] = {}
            for inter in interactions[:20]:
                note = mysql_client.get_note(inter["note_id"])
                if note and note.get("tags"):
                    for tag in note["tags"].split(","):
                        tag_counts[tag.strip()] = tag_counts.get(tag.strip(), 0) + 1
            user_tags = {k: float(v) for k, v in tag_counts.items()}

        if not user_tags:
            return []

        # Get top tags
        top_tags = sorted(user_tags.items(), key=lambda x: x[1], reverse=True)[:10]
        tag_list = [t[0] for t in top_tags]

        # Search ES for notes with these tags
        results = es_client.search_by_tags(tag_list, size=num * 2)

        # Score based on tag match count
        scored_items = []
        for note in results:
            note_tags = set(note.get("tags", []))
            match_count = len(note_tags & set(tag_list))
            score = match_count / max(len(tag_list), 1)
            scored_items.append((note["note_id"], score))

        return scored_items[:num]


class HotRecall(RecallStrategy):
    """Hot content recall based on trending scores."""

    def __init__(self, weight: float = 0.6):
        super().__init__("hot", weight)

    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        # Get hot notes from Redis sorted set
        hot_key = "hot_notes"
        hot_items = redis_client.zrevrange(hot_key, 0, num - 1, withscores=True)

        if hot_items:
            return [
                (item.decode("utf-8") if isinstance(item, bytes) else item, float(score))
                for item, score in hot_items
            ]

        # Fallback: compute from MySQL
        hot_notes = mysql_client.get_hot_notes(limit=num)
        return [(n["note_id"], float(n.get("hot_score", 0))) for n in hot_notes]


class UserProfileRecall(RecallStrategy):
    """Recall based on user profile and preferences."""

    def __init__(self, weight: float = 0.7):
        super().__init__("profile", weight)

    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        # Get user profile
        user = mysql_client.get_user(user_id)
        if not user:
            return []

        # Get user's preferred categories
        preferred_cats = user.get("preferred_categories", "")
        if isinstance(preferred_cats, str):
            preferred_cats = [c.strip() for c in preferred_cats.split(",") if c.strip()]

        if not preferred_cats:
            return []

        # Search notes in preferred categories
        results = []
        for cat in preferred_cats[:3]:
            # Use ES to search by category
            body = {
                "query": {"term": {"category": cat}},
                "sort": [{"score": {"order": "desc"}}],
                "size": num // len(preferred_cats) + 10,
            }
            try:
                resp = es_client.client.search(index=es_client.index_name, body=body)
                for hit in resp["hits"]["hits"]:
                    results.append((hit["_source"]["note_id"], hit["_source"].get("score", 0)))
            except Exception:
                pass

        # Deduplicate and return
        seen = set()
        unique_results = []
        for item_id, score in results:
            if item_id not in seen:
                seen.add(item_id)
                unique_results.append((item_id, score))

        return unique_results[:num]


class SocialRecall(RecallStrategy):
    """Recall from followees (social graph)."""

    def __init__(self, weight: float = 0.9):
        super().__init__("social", weight)

    def recall(self, user_id: str, context: Dict[str, Any], num: int) -> List[Tuple[str, float]]:
        # Get user's followees
        followees = mysql_client.get_user_followees(user_id)

        if not followees:
            return []

        # Get recent notes from followees
        results = []
        for followee_id in followees[:10]:
            notes = mysql_client.get_notes_by_author(followee_id, limit=5)
            for note in notes:
                # Score based on recency and engagement
                score = note.get("likes", 0) + note.get("comments", 0) * 2
                results.append((note["note_id"], score))

        # Sort by score
        results.sort(key=lambda x: x[1], reverse=True)
        return results[:num]


class RecallEngine:
    """Multi-channel recall engine that combines multiple strategies."""

    def __init__(self):
        self.strategies: List[RecallStrategy] = [
            CFRecall(weight=1.0),
            ContentRecall(weight=0.8),
            HotRecall(weight=0.5),
            UserProfileRecall(weight=0.7),
            SocialRecall(weight=0.9),
        ]
        self.settings = get_settings()

    def recall(self, user_id: str, context: Dict[str, Any] = None) -> List[Tuple[str, float]]:
        """
        Execute multi-channel recall and merge results.
        Returns list of (item_id, combined_score) sorted by score.
        """
        if context is None:
            context = {}

        num_per_channel = self.settings.RECALL_NUM // len(self.strategies) + 20

        all_candidates: Dict[str, List[float]] = {}

        for strategy in self.strategies:
            try:
                items = strategy.recall(user_id, context, num_per_channel)
                for item_id, score in items:
                    weighted_score = score * strategy.weight
                    if item_id not in all_candidates:
                        all_candidates[item_id] = []
                    all_candidates[item_id].append(weighted_score)
            except Exception as e:
                print(f"Recall strategy {strategy.name} failed: {e}")

        # Merge scores (sum of weighted scores from different channels)
        merged = []
        for item_id, scores in all_candidates.items():
            # Use sum of scores, with bonus for appearing in multiple channels
            combined_score = sum(scores) * (1 + 0.1 * (len(scores) - 1))
            merged.append((item_id, combined_score))

        # Sort by combined score
        merged.sort(key=lambda x: x[1], reverse=True)

        return merged[:self.settings.RECALL_NUM]

    def add_strategy(self, strategy: RecallStrategy):
        """Add a custom recall strategy."""
        self.strategies.append(strategy)


recall_engine = RecallEngine()
