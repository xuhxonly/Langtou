import random
from typing import Any, Dict, List, Tuple

from app.data import redis_client


class ReRanker:
    """
    Re-ranking layer that applies business rules and diversification.
    """

    def __init__(self):
        self.max_same_author = 2
        self.max_same_category = 3
        self.diversity_weight = 0.3
        self.freshness_weight = 0.2
        self.boost_new_user = 0.1

    def rerank(
        self,
        user_id: str,
        ranked_items: List[Tuple[str, float]],
        context: Dict[str, Any] = None,
    ) -> List[Tuple[str, float]]:
        """
        Apply re-ranking rules to ranked items.
        """
        if context is None:
            context = {}

        # Get shown items to avoid repetition
        shown_key = f"shown:{user_id}"
        shown_items = set(redis_client.lrange(shown_key, 0, 1000) or [])

        # Filter out already shown items
        filtered = [
            (item_id, score)
            for item_id, score in ranked_items
            if item_id not in shown_items
        ]

        # Apply diversity
        diversified = self._apply_diversity(filtered)

        # Apply freshness boost
        final = self._apply_freshness(diversified)

        # Limit to requested number
        limit = context.get("page_size", 20)
        return final[:limit]

    def _apply_diversity(
        self,
        items: List[Tuple[str, float]],
    ) -> List[Tuple[str, float]]:
        """Ensure diversity in recommendations."""
        author_counts: Dict[str, int] = {}
        category_counts: Dict[str, int] = {}
        result = []

        for item_id, score in items:
            # Get item metadata from Redis
            note_data = redis_client.hgetall(f"note:{item_id}")

            author = note_data.get("user_id", "unknown") if note_data else "unknown"
            category = note_data.get("category", "unknown") if note_data else "unknown"

            # Check constraints
            if author_counts.get(author, 0) >= self.max_same_author:
                score *= 0.5  # Penalize

            if category_counts.get(category, 0) >= self.max_same_category:
                score *= 0.7  # Penalize

            author_counts[author] = author_counts.get(author, 0) + 1
            category_counts[category] = category_counts.get(category, 0) + 1

            result.append((item_id, score))

        # Re-sort after diversity adjustments
        result.sort(key=lambda x: x[1], reverse=True)
        return result

    def _apply_freshness(
        self,
        items: List[Tuple[str, float]],
    ) -> List[Tuple[str, float]]:
        """Boost fresh content."""
        result = []

        for item_id, score in items:
            note_data = redis_client.hgetall(f"note:{item_id}")

            if note_data:
                is_new = note_data.get("is_new", 0)
                if is_new:
                    score *= (1 + self.freshness_weight)

            result.append((item_id, score))

        result.sort(key=lambda x: x[1], reverse=True)
        return result

    def record_shown(self, user_id: str, item_ids: List[str]):
        """Record shown items to avoid repetition."""
        shown_key = f"shown:{user_id}"
        for item_id in item_ids:
            redis_client.lpush(shown_key, [item_id], expire=86400 * 3)


reranker = ReRanker()
