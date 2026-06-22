import json
from typing import Any, Dict, List, Optional, Tuple

import numpy as np
from scipy.sparse import csr_matrix
from sklearn.metrics.pairwise import cosine_similarity

from app.data import redis_client, mysql_client


class CollaborativeFilteringModel:
    """
    Item-based Collaborative Filtering model.
    Computes item-item similarity matrix from user behavior.
    """

    def __init__(self, model_path: str = None):
        self.model_path = model_path or "./saved_models/cf_model.json"
        self.item_similarity = None
        self.item_id_map = {}
        self.reverse_item_map = {}
        self.user_item_matrix = None

    def build(self, interactions: List[Dict[str, Any]] = None, min_common_users: int = 3):
        """
        Build CF model from interaction data.
        """
        if interactions is None:
            # Load from MySQL (mock - in production use batch query)
            interactions = []

        # Build user-item matrix
        users = list(set(i["user_id"] for i in interactions))
        items = list(set(i["target_id"] for i in interactions))

        self.item_id_map = {item: idx for idx, item in enumerate(items)}
        self.reverse_item_map = {idx: item for item, idx in self.item_id_map.items()}

        # Create sparse matrix
        rows = []
        cols = []
        data = []

        user_id_map = {user: idx for idx, user in enumerate(users)}

        for inter in interactions:
            user_idx = user_id_map[inter["user_id"]]
            item_idx = self.item_id_map[inter["target_id"]]
            score = inter.get("score", 1.0)

            rows.append(user_idx)
            cols.append(item_idx)
            data.append(score)

        self.user_item_matrix = csr_matrix(
            (data, (rows, cols)),
            shape=(len(users), len(items))
        )

        # Compute item-item similarity
        item_vectors = self.user_item_matrix.T
        self.item_similarity = cosine_similarity(item_vectors, dense_output=False)

        return self

    def get_similar_items(self, item_id: str, top_k: int = 20) -> List[Tuple[str, float]]:
        """Get top-k similar items for a given item."""
        if item_id not in self.item_id_map or self.item_similarity is None:
            return []

        item_idx = self.item_id_map[item_id]
        sim_vector = self.item_similarity[item_idx].toarray().flatten()

        # Get top-k (excluding self)
        top_indices = np.argsort(sim_vector)[::-1][1:top_k + 1]

        results = []
        for idx in top_indices:
            if sim_vector[idx] > 0:
                results.append((self.reverse_item_map[idx], float(sim_vector[idx])))

        return results

    def recommend(self, user_id: str, top_k: int = 50) -> List[Tuple[str, float]]:
        """Recommend items for a user based on their history."""
        # Get user history
        history = redis_client.lrange(f"user_history:{user_id}", 0, 100)

        if not history:
            interactions = mysql_client.get_user_interactions(user_id)
            history = [i["target_id"] for i in interactions[:50]]

        if not history:
            return []

        # Aggregate scores from similar items
        candidate_scores: Dict[str, float] = {}

        for note_id in history:
            similar_items = self.get_similar_items(note_id, top_k=20)
            for sim_item, sim_score in similar_items:
                if sim_item not in history:
                    candidate_scores[sim_item] = candidate_scores.get(sim_item, 0) + sim_score

        # Sort and return top-k
        sorted_items = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
        return sorted_items[:top_k]

    def save(self, path: str = None):
        """Save model to disk as JSON (safe serialization)."""
        path = path or self.model_path
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)

        # Convert sparse matrix to dense for JSON serialization
        sim_dense = self.item_similarity.toarray().tolist() if self.item_similarity is not None else []

        data = {
            "item_similarity": sim_dense,
            "item_id_map": self.item_id_map,
            "reverse_item_map": {str(k): v for k, v in self.reverse_item_map.items()},
        }

        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False)

    def load(self, path: str = None):
        """Load model from disk."""
        path = path or self.model_path
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            sim_dense = np.array(data["item_similarity"])
            self.item_similarity = csr_matrix(sim_dense) if sim_dense.size > 0 else None
            self.item_id_map = data["item_id_map"]
            self.reverse_item_map = {int(k): v for k, v in data["reverse_item_map"].items()}
            return True
        except Exception as e:
            print(f"Failed to load CF model: {e}")
            return False

    def update_redis(self):
        """Update Redis with item similarities for fast recall."""
        if self.item_similarity is None:
            return

        for item_id, item_idx in self.item_id_map.items():
            sim_vector = self.item_similarity[item_idx].toarray().flatten()
            top_indices = np.argsort(sim_vector)[::-1][1:21]

            sim_dict = {}
            for idx in top_indices:
                if sim_vector[idx] > 0:
                    sim_item = self.reverse_item_map[idx]
                    sim_dict[sim_item] = float(sim_vector[idx])

            if sim_dict:
                redis_client.zadd(f"item_sim:{item_id}", sim_dict, expire=86400 * 7)


cf_model = CollaborativeFilteringModel()
