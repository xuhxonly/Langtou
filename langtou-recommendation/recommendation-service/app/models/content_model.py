import json
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
        self.model_path = model_path or "./saved_models/content_model.json"
        self.vectorizer = TfidfVectorizer(max_features=5000, stop_words="english")
        self.tfidf_matrix = None
        self.note_ids = []

    def build(self, notes: List[Dict[str, Any]]):
        """
        Build content similarity model from notes.
        """
        self.note_ids = [n.get("id", n.get("note_id", "")) for n in notes]
        texts = []

        for note in notes:
            # Combine title, content, and tags
            tags = note.get("tags", [])
            if isinstance(tags, list):
                tags_str = " ".join(str(t) for t in tags)
            elif isinstance(tags, str):
                tags_str = tags
            else:
                tags_str = ""

            text_parts = [
                note.get("title", ""),
                note.get("content", ""),
                tags_str,
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
        """Save model to disk as JSON (safe serialization)."""
        path = path or self.model_path
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)

        # Serialize TF-IDF matrix to list
        tfidf_dense = self.tfidf_matrix.toarray().tolist() if self.tfidf_matrix is not None else []

        # Serialize vectorizer vocabulary
        vocab = self.vectorizer.vocabulary_ if self.vectorizer else {}

        data = {
            "tfidf_matrix": tfidf_dense,
            "note_ids": self.note_ids,
            "vocabulary": vocab,
            "idf": self.vectorizer.idf_.tolist() if hasattr(self.vectorizer, "idf_") and self.vectorizer.idf_ is not None else [],
        }

        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False)

    def load(self, path: str = None):
        """Load model from disk."""
        path = path or self.model_path
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            tfidf_dense = np.array(data["tfidf_matrix"])
            self.tfidf_matrix = tfidf_dense if tfidf_dense.size > 0 else None
            self.note_ids = data["note_ids"]

            # Reconstruct vectorizer
            if data.get("vocabulary") and data.get("idf"):
                self.vectorizer = TfidfVectorizer(max_features=5000, stop_words="english")
                self.vectorizer.vocabulary_ = {k: int(v) for k, v in data["vocabulary"].items()}
                self.vectorizer.idf_ = np.array(data["idf"])

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
