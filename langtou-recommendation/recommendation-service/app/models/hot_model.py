import math
from datetime import datetime, timezone
from typing import Any, Dict, List, Tuple

from app.data import redis_client, mysql_client


class HotModel:
    """
    Hot/Trending content model.
    Computes trending scores based on engagement and recency.
    """

    def __init__(self):
        self.hot_key = "hot_notes"
        self.trending_key = "trending_notes"

    def compute_hot_score(self, note: Dict[str, Any]) -> float:
        """
        Compute hot score for a note using a modified Hacker News algorithm.
        Score = (engagement) / (age_hours + 2)^gravity
        """
        likes = note.get("likes", 0)
        comments = note.get("comments", 0)
        shares = note.get("shares", 0)
        views = max(note.get("views", 1), 1)

        # Engagement score with weights
        engagement = likes * 1.0 + comments * 2.0 + shares * 3.0

        # Normalize by views (CTR-like)
        engagement_rate = engagement / views

        # Time decay
        create_time = note.get("create_time")
        if create_time:
            if isinstance(create_time, str):
                try:
                    create_time = datetime.fromisoformat(create_time.replace("Z", "+00:00"))
                except Exception:
                    create_time = datetime.now()
            now = datetime.now()
            if create_time.tzinfo:
                now = now.replace(tzinfo=timezone.utc)
            age_hours = max((now - create_time).total_seconds() / 3600, 0.1)
        else:
            age_hours = 1.0

        # Gravity factor - higher = faster decay
        gravity = 1.8

        # Hot score
        score = (engagement_rate * 100 + engagement * 0.1) / math.pow(age_hours + 2, gravity)

        return score

    def update_hot_notes(self, notes: List[Dict[str, Any]]):
        """
        Update hot notes in Redis sorted set.
        """
        scores = {}
        for note in notes:
            note_id = note.get("note_id")
            if note_id:
                score = self.compute_hot_score(note)
                scores[note_id] = score

        if scores:
            redis_client.zadd(self.hot_key, scores, expire=86400)

    def get_hot_notes(self, top_k: int = 100) -> List[Tuple[str, float]]:
        """Get top-k hot notes from Redis."""
        hot_items = redis_client.zrevrange(self.hot_key, 0, top_k - 1, withscores=True)

        if hot_items:
            return [
                (item.decode("utf-8") if isinstance(item, bytes) else item, float(score))
                for item, score in hot_items
            ]

        # Fallback: compute from MySQL
        hot_notes = mysql_client.get_hot_notes(limit=top_k)
        return [(n["note_id"], float(n.get("hot_score", 0))) for n in hot_notes]

    def get_trending_notes(self, top_k: int = 50) -> List[Tuple[str, float]]:
        """
        Get trending notes (rapidly gaining engagement).
        """
        # Get notes from last 24 hours
        trending_items = redis_client.zrevrange(self.trending_key, 0, top_k - 1, withscores=True)

        if trending_items:
            return [
                (item.decode("utf-8") if isinstance(item, bytes) else item, float(score))
                for item, score in trending_items
            ]

        return []

    def record_engagement(self, note_id: str, action: str, delta: float = 1.0):
        """
        Record real-time engagement and update trending score.
        """
        # Update trending score
        current_score = redis_client.zscore(self.trending_key, note_id) or 0
        new_score = current_score + delta
        redis_client.zadd(self.trending_key, {note_id: new_score}, expire=86400)

        # Also update hot score
        note = mysql_client.get_note(note_id)
        if note:
            hot_score = self.compute_hot_score(note)
            redis_client.zadd(self.hot_key, {note_id: hot_score}, expire=86400)

    def get_category_hot(self, category: str, top_k: int = 50) -> List[Tuple[str, float]]:
        """Get hot notes for a specific category."""
        # This would require category-specific sorted sets in production
        # For now, filter from global hot
        all_hot = self.get_hot_notes(top_k * 2)

        category_notes = []
        for note_id, score in all_hot:
            note = mysql_client.get_note(note_id)
            if note and note.get("category") == category:
                category_notes.append((note_id, score))

        return category_notes[:top_k]


hot_model = HotModel()
