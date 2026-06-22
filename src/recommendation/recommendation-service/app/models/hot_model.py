import json
from datetime import datetime, timedelta
from typing import Any, Dict, List, Tuple

import numpy as np

from app.data import mysql_client, redis_client


class HotContentModel:
    """
    Hot content model for trending recommendations.
    Uses time-decay algorithm to compute trending scores.
    """

    def __init__(self, model_path: str = None):
        self.model_path = model_path or "./saved_models/hot_model.json"
        self.hot_notes = []
        self.last_update = None

    def compute_hot_score(
        self,
        likes: int,
        comments: int,
        shares: int,
        views: int,
        age_hours: float,
    ) -> float:
        """
        Compute hot score using time-decay algorithm.
        Formula: (likes + 2*comments + 3*shares) / (age_hours + 2)^1.5
        """
        engagement_score = likes * 1.0 + comments * 2.0 + shares * 3.0
        time_decay = (age_hours + 2.0) ** 1.5
        return engagement_score / time_decay

    def update(self, notes: List[Dict[str, Any]] = None):
        """
        Update hot content list.
        """
        if notes is None:
            # Get recent notes from MySQL
            notes = mysql_client.get_hot_notes(limit=1000)

        scored_notes = []
        now = datetime.now()

        for note in notes:
            create_time = note.get("created_at")
            if isinstance(create_time, str):
                try:
                    create_time = datetime.fromisoformat(create_time.replace("Z", "+00:00"))
                except Exception:
                    continue

            if not create_time:
                continue

            if create_time.tzinfo:
                from datetime import timezone
                now = now.replace(tzinfo=timezone.utc)

            age_hours = (now - create_time).total_seconds() / 3600

            score = self.compute_hot_score(
                likes=note.get("like_count", 0),
                comments=note.get("comment_count", 0),
                shares=note.get("share_count", 0),
                views=note.get("view_count", 1),
                age_hours=age_hours,
            )

            scored_notes.append({
                "note_id": note.get("id", note.get("note_id", "")),
                "score": score,
                "age_hours": age_hours,
                **note,
            })

        # Sort by hot score
        scored_notes.sort(key=lambda x: x["score"], reverse=True)
        self.hot_notes = scored_notes[:500]
        self.last_update = datetime.now()

        # Update Redis
        self._update_redis()

        return self.hot_notes[:100]

    def get_hot_notes(
        self,
        category: str = None,
        limit: int = 50,
        exclude_ids: List[str] = None,
    ) -> List[Dict[str, Any]]:
        """Get hot notes, optionally filtered by category."""
        if not self.hot_notes or self._is_stale():
            self.update()

        exclude_ids = set(exclude_ids or [])
        filtered = [
            n for n in self.hot_notes
            if n["note_id"] not in exclude_ids
            and (category is None or n.get("category") == category)
        ]

        return filtered[:limit]

    def get_trending_notes(self, hours: int = 24, limit: int = 50) -> List[Dict[str, Any]]:
        """Get notes trending in the last N hours."""
        if not self.hot_notes or self._is_stale():
            self.update()

        cutoff = datetime.now() - timedelta(hours=hours)
        trending = [
            n for n in self.hot_notes
            if n.get("created_at") and n.get("created_at") > cutoff
        ]

        return trending[:limit]

    def _is_stale(self) -> bool:
        """Check if hot list needs refresh."""
        if self.last_update is None:
            return True
        return (datetime.now() - self.last_update).total_seconds() > 300  # 5 minutes

    def _update_redis(self):
        """Update Redis with hot notes."""
        # Clear old data
        redis_client.delete("hot_notes")

        # Add to sorted set
        mapping = {}
        for note in self.hot_notes[:200]:
            mapping[note["note_id"]] = note["score"]

        if mapping:
            redis_client.zadd("hot_notes", mapping, expire=3600)

    def save(self, path: str = None):
        """Save model to disk as JSON (safe serialization)."""
        path = path or self.model_path
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)

        data = {
            "hot_notes": self.hot_notes,
            "last_update": self.last_update.isoformat() if self.last_update else None,
        }

        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, default=str)

    def load(self, path: str = None):
        """Load model from disk."""
        path = path or self.model_path
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            self.hot_notes = data.get("hot_notes", [])
            if data.get("last_update"):
                self.last_update = datetime.fromisoformat(data["last_update"])

            return True
        except Exception as e:
            print(f"Failed to load hot model: {e}")
            return False


hot_model = HotContentModel()
