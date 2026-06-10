import pickle
from typing import Any, Dict, List, Tuple

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.data import redis_client, es_client


class ContentSimilarityModel:
    """
    Content-based similarity model using TF-IDF on note text.
    """

    def __init__(self, model_path: str = None):
        self.model_path = model_path or "./saved_models/content_model.pkl"
        self.vectorizer = TfidfVectorizer(max_features=5000, stop_words="english")
        self.tfidf_matrix = None
        self.note_ids = []

    def build(self, notes: List[Dict[str, Any]]):
        """
        Build content similarity model from notes.
        """
        self.note_ids = [n["note_id"] for n in notes]
        texts = []

        for note in notes:
            # Combine title, content, and tags
            text_parts = [
                note.get("title", ""),
                note.get("content", ""),
                " ".join(note.get("tags", [])),
            ]
            texts.append(" ".join(text_parts))

        # Fit TF-IDF
        self.tfidf_matrix = self.vectorizer.fit_transform(texts)

        return self

    def get_similar_items(self, note_id: str, top_k: int = 20) -> List[Tuple[str, float]]:
        """Get top-k content-similar items."""
        if note_id not in self.note_ids or self.tfidf_matrix is None:
            return []

        idx = self.note_ids.index(note_id)
        note_vector = self.tfidf_matrix[idx]

        # Compute similarity with all notes
        similarities = cosine_similarity(note_vector, self.tfidf_matrix).flatten()

        # Get top-k (excluding self)
        top_indices = np.argsort(similarities)[::-1][1:top_k + 1]

        results = []
        for i in top_indices:
            if similarities[i] > 0:
                results.append((self.note_ids[i], float(similarities[i])))

        return results

    def compute_note_embedding(self, note_id: str) -> np.ndarray:
        """Get TF-IDF embedding for a note."""
        if note_id not in self.note_ids or self.tfidf_matrix is None:
            return np.zeros(self.tfidf_matrix.shape[1] if self.tfidf_matrix is not None else 100)

        idx = self.note_ids.index(note_id)
        return self.tfidf_matrix[idx].toarray().flatten()

    def save(self, path: str = None):
        """Save model to disk."""
        path = path or self.model_path
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)

        with open(path, "wb") as f:
            pickle.dump({
                "vectorizer": self.vectorizer,
                "tfidf_matrix": self.tfidf_matrix,
                "note_ids": self.note_ids,
            }, f)

    def load(self, path: str = None):
        """Load model from disk."""
        path = path or self.model_path
        try:
            with open(path, "rb") as f:
                data = pickle.load(f)
            self.vectorizer = data["vectorizer"]
            self.tfidf_matrix = data["tfidf_matrix"]
            self.note_ids = data["note_ids"]
            return True
        except Exception as e:
            print(f"Failed to load content model: {e}")
            return False

    def update_redis(self):
        """Store content similarities in Redis for fast recall."""
        if self.tfidf_matrix is None:
            return

        for i, note_id in enumerate(self.note_ids):
            similarities = cosine_similarity(
                self.tfidf_matrix[i], self.tfidf_matrix
            ).flatten()
            top_indices = np.argsort(similarities)[::-1][1:21]

            sim_dict = {}
            for idx in top_indices:
                if similarities[idx] > 0:
                    sim_dict[self.note_ids[idx]] = float(similarities[idx])

            if sim_dict:
                redis_client.zadd(f"content_sim:{note_id}", sim_dict, expire=86400 * 7)


content_model = ContentSimilarityModel()
