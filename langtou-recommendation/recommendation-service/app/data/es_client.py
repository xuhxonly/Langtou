from typing import Any, Dict, List, Optional

from elasticsearch import Elasticsearch

from config import get_settings


class ESClient:
    """Elasticsearch client for full-text search and vector search."""

    def __init__(self):
        self.settings = get_settings()
        self.client = Elasticsearch(
            [f"http://{self.settings.ES_HOST}:{self.settings.ES_PORT}"],
            basic_auth=(
                self.settings.ES_USER,
                self.settings.ES_PASSWORD,
            ) if self.settings.ES_USER else None,
        )
        self.index_name = "notes"

    def create_index(self):
        """Create notes index with mappings."""
        if self.client.indices.exists(index=self.index_name):
            return

        mapping = {
            "mappings": {
                "properties": {
                    "note_id": {"type": "keyword"},
                    "title": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                    },
                    "content": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                    },
                    "tags": {"type": "keyword"},
                    "category": {"type": "keyword"},
                    "user_id": {"type": "keyword"},
                    "created_at": {"type": "date"},
                    "like_count": {"type": "integer"},
                    "comment_count": {"type": "integer"},
                    "share_count": {"type": "integer"},
                    "view_count": {"type": "integer"},
                    "score": {"type": "float"},
                    "embedding": {
                        "type": "dense_vector",
                        "dims": 128,
                        "index": True,
                        "similarity": "cosine",
                    },
                }
            }
        }

        self.client.indices.create(index=self.index_name, body=mapping)

    def index_note(self, note: Dict[str, Any]) -> bool:
        """Index a single note."""
        try:
            note_id = note.get("id", note.get("note_id"))
            self.client.index(
                index=self.index_name,
                id=note_id,
                document=note,
            )
            return True
        except Exception as e:
            print(f"ES index_note error: {e}")
            return False

    def bulk_index(self, notes: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Bulk index notes."""
        from elasticsearch.helpers import bulk

        actions = []
        for note in notes:
            note_id = note.get("id", note.get("note_id"))
            actions.append({
                "_index": self.index_name,
                "_id": note_id,
                "_source": note,
            })

        try:
            success, errors = bulk(self.client, actions)
            return {"success": success, "errors": errors}
        except Exception as e:
            print(f"ES bulk_index error: {e}")
            return {"success": 0, "errors": str(e)}

    def search(
        self,
        query: str,
        tags: Optional[List[str]] = None,
        user_id: Optional[str] = None,
        size: int = 20,
    ) -> List[Dict[str, Any]]:
        """Full-text search with optional filters."""
        must_clauses = [
            {
                "multi_match": {
                    "query": query,
                    "fields": ["title^3", "content", "tags^2"],
                    "type": "best_fields",
                }
            }
        ]

        filter_clauses = []
        if tags:
            filter_clauses.append({"terms": {"tags": tags}})
        if user_id:
            filter_clauses.append({"term": {"user_id": user_id}})

        body = {
            "query": {
                "bool": {
                    "must": must_clauses,
                    "filter": filter_clauses,
                }
            },
            "sort": [
                {"_score": {"order": "desc"}},
                {"score": {"order": "desc"}},
            ],
            "size": size,
        }

        try:
            response = self.client.search(index=self.index_name, body=body)
            return [hit["_source"] for hit in response["hits"]["hits"]]
        except Exception as e:
            print(f"ES search error: {e}")
            return []

    def search_by_tags(self, tags: List[str], size: int = 20) -> List[Dict[str, Any]]:
        """Search notes by tags."""
        body = {
            "query": {
                "terms": {"tags": tags}
            },
            "sort": [
                {"score": {"order": "desc"}},
                {"created_at": {"order": "desc"}},
            ],
            "size": size,
        }

        try:
            response = self.client.search(index=self.index_name, body=body)
            return [hit["_source"] for hit in response["hits"]["hits"]]
        except Exception as e:
            print(f"ES search_by_tags error: {e}")
            return []

    def vector_search(
        self,
        embedding: List[float],
        size: int = 20,
        k: int = 100,
    ) -> List[Dict[str, Any]]:
        """Vector similarity search."""
        body = {
            "query": {
                "knn": {
                    "embedding": {
                        "vector": embedding,
                        "k": k,
                    }
                }
            },
            "size": size,
        }

        try:
            response = self.client.search(index=self.index_name, body=body)
            return [hit["_source"] for hit in response["hits"]["hits"]]
        except Exception as e:
            print(f"ES vector_search error: {e}")
            return []

    def get_note(self, note_id: str) -> Optional[Dict[str, Any]]:
        """Get a note by ID."""
        try:
            response = self.client.get(index=self.index_name, id=note_id)
            return response["_source"] if response["found"] else None
        except Exception as e:
            print(f"ES get_note error: {e}")
            return None

    def delete_note(self, note_id: str) -> bool:
        """Delete a note by ID."""
        try:
            self.client.delete(index=self.index_name, id=note_id)
            return True
        except Exception as e:
            print(f"ES delete_note error: {e}")
            return False

    def health(self) -> Dict[str, Any]:
        """Check ES cluster health."""
        try:
            return self.client.cluster.health()
        except Exception as e:
            return {"status": "unavailable", "error": str(e)}


es_client = ESClient()
