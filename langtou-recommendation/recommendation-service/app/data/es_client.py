from typing import Any, Dict, List, Optional

from elasticsearch import Elasticsearch

from config import get_settings


class ESClient:
    """Elasticsearch client for content search and retrieval."""

    def __init__(self):
        self.settings = get_settings()
        auth = None
        if self.settings.ES_USER and self.settings.ES_PASSWORD:
            auth = (self.settings.ES_USER, self.settings.ES_PASSWORD)
        self.client = Elasticsearch(
            [self.settings.ES_HOST],
            basic_auth=auth,
            verify_certs=False,
        )
        self.index_name = "langtou_notes"

    def ensure_index(self):
        """Create index if not exists with proper mappings."""
        if not self.client.indices.exists(index=self.index_name):
            mapping = {
                "mappings": {
                    "properties": {
                        "note_id": {"type": "keyword"},
                        "title": {"type": "text", "analyzer": "ik_max_word"},
                        "content": {"type": "text", "analyzer": "ik_max_word"},
                        "tags": {"type": "keyword"},
                        "category": {"type": "keyword"},
                        "author_id": {"type": "keyword"},
                        "create_time": {"type": "date"},
                        "likes": {"type": "integer"},
                        "comments": {"type": "integer"},
                        "shares": {"type": "integer"},
                        "score": {"type": "float"},
                    }
                }
            }
            self.client.indices.create(index=self.index_name, body=mapping)

    def index_note(self, note: Dict[str, Any]) -> bool:
        """Index or update a note document."""
        try:
            self.client.index(index=self.index_name, id=note["note_id"], document=note)
            return True
        except Exception as e:
            print(f"ES index error: {e}")
            return False

    def bulk_index(self, notes: List[Dict[str, Any]]) -> bool:
        """Bulk index notes."""
        try:
            from elasticsearch.helpers import bulk
            actions = [
                {"_index": self.index_name, "_id": n["note_id"], "_source": n}
                for n in notes
            ]
            bulk(self.client, actions)
            return True
        except Exception as e:
            print(f"ES bulk index error: {e}")
            return False

    def search(self, query: str, tags: Optional[List[str]] = None, size: int = 20) -> List[Dict[str, Any]]:
        """Full-text search notes."""
        must_clauses = [
            {
                "multi_match": {
                    "query": query,
                    "fields": ["title^3", "content", "tags^2"],
                }
            }
        ]
        if tags:
            must_clauses.append({"terms": {"tags": tags}})

        body = {
            "query": {"bool": {"must": must_clauses}},
            "sort": [{"score": {"order": "desc"}}, "_score"],
            "size": size,
        }
        try:
            resp = self.client.search(index=self.index_name, body=body)
            hits = resp["hits"]["hits"]
            return [{**h["_source"], "_score": h["_score"]} for h in hits]
        except Exception as e:
            print(f"ES search error: {e}")
            return []

    def search_by_tags(self, tags: List[str], size: int = 50) -> List[Dict[str, Any]]:
        """Search notes by tags."""
        body = {
            "query": {"terms": {"tags": tags}},
            "sort": [{"score": {"order": "desc"}}],
            "size": size,
        }
        try:
            resp = self.client.search(index=self.index_name, body=body)
            hits = resp["hits"]["hits"]
            return [{**h["_source"], "_score": h["_score"]} for h in hits]
        except Exception as e:
            print(f"ES tag search error: {e}")
            return []

    def get_note(self, note_id: str) -> Optional[Dict[str, Any]]:
        """Get a note by ID."""
        try:
            resp = self.client.get(index=self.index_name, id=note_id)
            return resp["_source"]
        except Exception:
            return None

    def delete_note(self, note_id: str) -> bool:
        try:
            self.client.delete(index=self.index_name, id=note_id)
            return True
        except Exception:
            return False


es_client = ESClient()
