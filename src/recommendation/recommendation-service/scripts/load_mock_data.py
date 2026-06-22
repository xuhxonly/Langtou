#!/usr/bin/env python3
"""
Load mock data into Redis, MySQL, and Elasticsearch.
This script should be run after generate_mock_data.py.
"""

import json
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.data import redis_client, es_client
from app.models.cf_model import cf_model
from app.models.content_model import content_model
from app.models.hot_model import hot_model


def load_users_to_redis(data_dir: str = "./mock_data"):
    """Load users into Redis."""
    with open(os.path.join(data_dir, "users.json"), "r", encoding="utf-8") as f:
        users = json.load(f)

    for user in users:
        user_id = user["user_id"]
        redis_client.hset(f"user:{user_id}", "data", user, expire=86400 * 7)

        # Set user tags from preferred categories
        if user.get("preferred_categories"):
            cats = [c.strip() for c in user["preferred_categories"].split(",") if c.strip()]
            for cat in cats:
                redis_client.hset(f"user_tags:{user_id}", cat, 1.0, expire=86400 * 30)

    print(f"Loaded {len(users)} users to Redis")
    return users


def load_notes_to_redis(data_dir: str = "./mock_data"):
    """Load notes into Redis."""
    with open(os.path.join(data_dir, "notes.json"), "r", encoding="utf-8") as f:
        notes = json.load(f)

    for note in notes:
        note_id = note["note_id"]
        redis_client.hset(f"note:{note_id}", "data", note, expire=86400 * 7)

        # Also store simplified fields for fast access
        simple = {
            "note_id": note_id,
            "category": note.get("category", ""),
            "author_id": note.get("author_id", ""),
            "likes": note.get("likes", 0),
            "comments": note.get("comments", 0),
            "shares": note.get("shares", 0),
            "views": note.get("views", 0),
            "is_new": 1 if note.get("is_new") else 0,
            "is_recent": 1 if note.get("is_recent") else 0,
        }
        redis_client.hset(f"note:{note_id}", "simple", simple, expire=86400 * 7)

    print(f"Loaded {len(notes)} notes to Redis")
    return notes


def load_notes_to_es(data_dir: str = "./mock_data"):
    """Load notes into Elasticsearch."""
    with open(os.path.join(data_dir, "notes.json"), "r", encoding="utf-8") as f:
        notes = json.load(f)

    es_client.ensure_index()

    # Prepare documents
    docs = []
    for note in notes:
        doc = {
            "note_id": note["note_id"],
            "title": note.get("title", ""),
            "content": note.get("content", ""),
            "tags": [t.strip() for t in note.get("tags", "").split(",") if t.strip()],
            "category": note.get("category", ""),
            "author_id": note.get("author_id", ""),
            "create_time": note.get("create_time"),
            "likes": note.get("likes", 0),
            "comments": note.get("comments", 0),
            "shares": note.get("shares", 0),
            "score": note.get("likes", 0) + note.get("comments", 0) * 2 + note.get("shares", 0) * 3,
        }
        docs.append(doc)

    # Bulk index
    es_client.bulk_index(docs)
    print(f"Indexed {len(docs)} notes to Elasticsearch")


def load_interactions_to_redis(data_dir: str = "./mock_data"):
    """Load interactions into Redis."""
    with open(os.path.join(data_dir, "interactions.json"), "r", encoding="utf-8") as f:
        interactions = json.load(f)

    for inter in interactions:
        user_id = inter["user_id"]
        note_id = inter["note_id"]

        # Add to user history
        redis_client.lpush(f"user_history:{user_id}", [note_id], expire=86400 * 7)

        # Update user tags for positive interactions
        if inter.get("score", 0) > 0:
            note = redis_client.hgetall(f"note:{note_id}")
            if note and isinstance(note, dict):
                data = note.get("data", {})
                if isinstance(data, dict) and data.get("tags"):
                    tags = [t.strip() for t in data["tags"].split(",") if t.strip()]
                    tag_key = f"user_tags:{user_id}"
                    for tag in tags:
                        current = redis_client.hget(tag_key, tag)
                        current_score = float(current) if current else 0.0
                        redis_client.hset(tag_key, tag, current_score + inter.get("score", 1), expire=86400 * 30)

    print(f"Loaded {len(interactions)} interactions to Redis")
    return interactions


def load_hot_scores(notes: list):
    """Compute and load hot scores to Redis."""
    hot_model.update_hot_notes(notes)
    print("Updated hot scores in Redis")


def build_cf_model(interactions: list):
    """Build and save collaborative filtering model."""
    cf_model.build(interactions)
    cf_model.save()
    cf_model.update_redis()
    print("Built and saved CF model")


def build_content_model(notes: list):
    """Build and save content similarity model."""
    content_model.build(notes)
    content_model.save()
    content_model.update_redis()
    print("Built and saved content similarity model")


def load_all(data_dir: str = "./mock_data"):
    """Load all mock data."""
    print("Loading mock data...")

    users = load_users_to_redis(data_dir)
    notes = load_notes_to_redis(data_dir)
    load_notes_to_es(data_dir)
    interactions = load_interactions_to_redis(data_dir)
    load_hot_scores(notes)

    print("\nBuilding models...")
    build_cf_model(interactions)
    build_content_model(notes)

    print("\nAll mock data loaded successfully!")


if __name__ == "__main__":
    data_dir = sys.argv[1] if len(sys.argv) > 1 else "./mock_data"
    load_all(data_dir)
