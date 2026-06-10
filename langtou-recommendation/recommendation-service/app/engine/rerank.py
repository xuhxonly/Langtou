import random
from typing import Any, Dict, List, Set, Tuple

import numpy as np

from app.data import redis_client
from config import get_settings


class Reranker:
    """Re-ranking layer for diversity and business rules."""

    def __init__(self):
        self.settings = get_settings()

    def rerank(self, user_id: str, ranked_items: List[Tuple[str, float]], context: Dict[str, Any] = None) -> List[Tuple[str, float]]:
        """
        Apply re-ranking strategies.
        Returns final list of (item_id, score).
        """
        if context is None:
            context = {}

        # Step 1: Apply diversity boosting
        items = self._diversity_boost(user_id, ranked_items)

        # Step 2: Apply freshness boost
        items = self._freshness_boost(items)

        # Step 3: Apply business rules
        items = self._apply_business_rules(user_id, items, context)

        # Step 4: Apply exploration (epsilon-greedy)
        items = self._exploration(user_id, items, context)

        # Step 5: Final sorting and truncation
        items.sort(key=lambda x: x[1], reverse=True)

        return items[:self.settings.FINAL_NUM]

    def _diversity_boost(self, user_id: str, items: List[Tuple[str, float]]) -> List[Tuple[str, float]]:
        """Boost diversity by penalizing similar consecutive items."""
        if not items:
            return items

        # Get item categories/tags
        item_categories = {}
        for item_id, _ in items:
            note = redis_client.hgetall(f"note:{item_id}")
            if note:
                item_categories[item_id] = note.get("category", "")
            else:
                item_categories[item_id] = ""

        # MMR-like re-ranking
        selected = []
        remaining = list(items)

        # Select first item (highest score)
        if remaining:
            selected.append(remaining.pop(0))

        while remaining and len(selected) < len(items):
            best_idx = 0
            best_score = -float("inf")

            for i, (item_id, score) in enumerate(remaining):
                # Calculate similarity with already selected items
                max_sim = 0.0
                for sel_id, _ in selected:
                    if item_categories.get(item_id) == item_categories.get(sel_id):
                        max_sim = max(max_sim, 0.8)
                    elif item_categories.get(item_id) and item_categories.get(sel_id):
                        max_sim = max(max_sim, 0.3)

                # MMR score: lambda * relevance - (1 - lambda) * max_sim
                lambda_param = 0.7
                mmr_score = lambda_param * score - (1 - lambda_param) * max_sim

                if mmr_score > best_score:
                    best_score = mmr_score
                    best_idx = i

            selected.append(remaining.pop(best_idx))

        return selected

    def _freshness_boost(self, items: List[Tuple[str, float]]) -> List[Tuple[str, float]]:
        """Boost fresh content."""
        boosted = []
        for item_id, score in items:
            note = redis_client.hgetall(f"note:{item_id}")
            if note:
                is_new = note.get("is_new", 0)
                is_recent = note.get("is_recent", 0)
                # Boost new content
                boost = 1.0
                if is_new:
                    boost = 1.15
                elif is_recent:
                    boost = 1.05
                boosted.append((item_id, score * boost))
            else:
                boosted.append((item_id, score))
        return boosted

    def _apply_business_rules(self, user_id: str, items: List[Tuple[str, float]], context: Dict[str, Any]) -> List[Tuple[str, float]]:
        """Apply business rules."""
        # Rule 1: Filter out recently shown items
        shown_key = f"user_shown:{user_id}"
        shown_items = set(redis_client.lrange(shown_key, 0, 200))

        filtered = []
        for item_id, score in items:
            if item_id not in shown_items:
                filtered.append((item_id, score))

        # Rule 2: Ensure minimum author diversity
        # (handled in diversity boost)

        # Rule 3: Boost followee content
        followees = context.get("followees", [])
        boosted = []
        for item_id, score in filtered:
            note = redis_client.hgetall(f"note:{item_id}")
            if note and note.get("author_id") in followees:
                boosted.append((item_id, score * 1.1))
            else:
                boosted.append((item_id, score))

        # Rule 4: Penalize low quality content
        final = []
        for item_id, score in boosted:
            note = redis_client.hgetall(f"note:{item_id}")
            if note:
                likes = note.get("likes", 0)
                views = max(note.get("views", 1), 1)
                ctr = likes / views
                # If CTR is very low, penalize
                if ctr < 0.01 and views > 100:
                    score *= 0.8
            final.append((item_id, score))

        return final

    def _exploration(self, user_id: str, items: List[Tuple[str, float]], context: Dict[str, Any]) -> List[Tuple[str, float]]:
        """Apply epsilon-greedy exploration for cold-start and diversity."""
        epsilon = context.get("epsilon", 0.1)

        if random.random() < epsilon and len(items) > 5:
            # Shuffle top 20% items for exploration
            explore_count = max(1, len(items) // 5)
            top_items = items[:explore_count]
            rest_items = items[explore_count:]

            random.shuffle(top_items)
            return top_items + rest_items

        return items

    def record_shown(self, user_id: str, items: List[str]):
        """Record shown items to avoid repetition."""
        shown_key = f"user_shown:{user_id}"
        redis_client.lpush(shown_key, items, expire=86400 * 3)  # Keep for 3 days


reranker = Reranker()
