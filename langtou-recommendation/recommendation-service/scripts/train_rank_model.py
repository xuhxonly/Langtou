#!/usr/bin/env python3
"""
Train the ranking model using mock data.
"""

import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.engine.rank import rank_engine


def generate_training_data(data_dir: str = "./mock_data"):
    """Generate training samples from mock interactions."""
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

    # Shuffle
    np.random.shuffle(training_data)

    return training_data


def main():
    import random

    print("Generating training data...")
    training_data = generate_training_data()
    print(f"Training samples: {len(training_data)}")

    # Count labels
    labels = [s["label"] for s in training_data]
    print(f"Positive: {sum(labels)}, Negative: {len(labels) - sum(labels)}")

    print("\nTraining model...")
    rank_engine.train_model(training_data, model_type="xgboost")
    print("Model trained and saved!")


if __name__ == "__main__":
    main()
