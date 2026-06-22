#!/usr/bin/env python3
"""
Train the XGBoost ranking model using click/impression logs from MySQL.
"""

import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.data import mysql_client
from app.engine.rank import rank_engine


def fetch_training_data_from_mysql(limit: int = 10000) -> list:
    """Fetch click and impression logs from MySQL as training data."""
    # Positive samples: clicks / likes / comments / shares (label=1)
    sql_positive = """
        SELECT user_id, target_id AS note_id, 1 AS label, created_at
        FROM like_record
        ORDER BY created_at DESC
        LIMIT %s
    """
    positive_samples = mysql_client.fetchall(sql_positive, (limit // 2,))

    # Negative samples: impressions without interaction (label=0)
    # Use note views that are not in like_record as negative samples
    sql_negative = """
        SELECT n.user_id AS user_id, n.id AS note_id, 0 AS label, n.created_at
        FROM note n
        WHERE n.id NOT IN (
            SELECT target_id FROM like_record WHERE target_type = 1
        )
        ORDER BY n.created_at DESC
        LIMIT %s
    """
    negative_samples = mysql_client.fetchall(sql_negative, (limit // 2,))

    training_data = []
    for row in positive_samples + negative_samples:
        training_data.append({
            "user_id": str(row["user_id"]),
            "note_id": str(row["note_id"]),
            "label": int(row["label"]),
            "context": {},
        })

    np.random.shuffle(training_data)
    return training_data


def generate_training_data_from_mock(data_dir: str = "./mock_data") -> list:
    """Generate training samples from mock interactions (fallback)."""
    import random

    with open(os.path.join(data_dir, "interactions.json"), "r", encoding="utf-8") as f:
        interactions = json.load(f)

    training_data = []

    for inter in interactions:
        label = 1 if inter.get("score", 0) > 1 else 0
        training_data.append({
            "user_id": inter["user_id"],
            "note_id": inter["note_id"],
            "context": {},
            "label": label,
        })

    # Add some negative samples (random user-note pairs without interaction)
    with open(os.path.join(data_dir, "users.json"), "r", encoding="utf-8") as f:
        users = json.load(f)
    with open(os.path.join(data_dir, "notes.json"), "r", encoding="utf-8") as f:
        notes = json.load(f)

    n_negative = len(training_data) // 4
    for _ in range(n_negative):
        user = random.choice(users)
        note = random.choice(notes)
        training_data.append({
            "user_id": user["user_id"],
            "note_id": note["note_id"],
            "context": {},
            "label": 0,
        })

    np.random.shuffle(training_data)
    return training_data


def main():
    print("Fetching training data...")

    # Try to fetch from MySQL first
    try:
        training_data = fetch_training_data_from_mysql(limit=10000)
        print(f"Fetched {len(training_data)} samples from MySQL")
    except Exception as e:
        print(f"Failed to fetch from MySQL: {e}")
        print("Falling back to mock data...")
        training_data = generate_training_data_from_mock()
        print(f"Generated {len(training_data)} samples from mock data")

    if not training_data:
        print("No training data available.")
        return

    # Count labels
    labels = [s["label"] for s in training_data]
    print(f"Positive: {sum(labels)}, Negative: {len(labels) - sum(labels)}")

    print("\nTraining XGBoost model...")
    save_path = os.path.join(os.path.dirname(__file__), "../saved_models/rank_model.json")
    os.makedirs(os.path.dirname(save_path), exist_ok=True)

    rank_engine.train_model(training_data, model_type="xgboost", save_path=save_path)
    print(f"Model trained and saved to {save_path}")


if __name__ == "__main__":
    main()
