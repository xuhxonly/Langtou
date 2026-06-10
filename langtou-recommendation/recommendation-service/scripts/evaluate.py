#!/usr/bin/env python3
"""
Recommendation system evaluation script.
Computes metrics like precision, recall, diversity, and coverage.
"""

import json
import os
import sys
from collections import defaultdict
from typing import Dict, List, Set

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.engine.recall import recall_engine
from app.engine.rank import rank_engine
from app.engine.rerank import reranker


def load_test_data(data_dir: str = "./mock_data"):
    """Load test data."""
    with open(os.path.join(data_dir, "users.json"), "r", encoding="utf-8") as f:
        users = json.load(f)
    with open(os.path.join(data_dir, "notes.json"), "r", encoding="utf-8") as f:
        notes = json.load(f)
    with open(os.path.join(data_dir, "interactions.json"), "r", encoding="utf-8") as f:
        interactions = json.load(f)

    return users, notes, interactions


def split_train_test(interactions: list, test_ratio: float = 0.2):
    """Split interactions into train and test sets."""
    # Group by user
    user_interactions = defaultdict(list)
    for inter in interactions:
        user_interactions[inter["user_id"]].append(inter)

    train = []
    test = []

    for user_id, inters in user_interactions.items():
        # Sort by time
        inters.sort(key=lambda x: x.get("create_time", ""))

        # Split: last 20% as test
        split_idx = int(len(inters) * (1 - test_ratio))
        train.extend(inters[:split_idx])
        test.extend(inters[split_idx:])

    return train, test


def get_user_ground_truth(test_interactions: list) -> Dict[str, Set[str]]:
    """Get ground truth items for each user."""
    ground_truth = defaultdict(set)
    for inter in test_interactions:
        if inter.get("score", 0) > 0:
            ground_truth[inter["user_id"]].add(inter["note_id"])
    return dict(ground_truth)


def evaluate_recommendations(
    users: list,
    ground_truth: Dict[str, Set[str]],
    k: int = 20,
    sample_users: int = 100,
) -> Dict[str, float]:
    """Evaluate recommendation quality."""
    # Sample users for evaluation
    eval_users = [u["user_id"] for u in users[:sample_users]]

    precisions = []
    recalls = []
    ndcgs = []
    diversities = []

    for user_id in eval_users:
        if user_id not in ground_truth or not ground_truth[user_id]:
            continue

        # Generate recommendations
        try:
            candidates = recall_engine.recall(user_id, context={})
            ranked = rank_engine.rank(user_id, candidates, context={})
            recommended = reranker.rerank(user_id, ranked, context={})
            recommended_ids = [item_id for item_id, _ in recommended[:k]]
        except Exception as e:
            print(f"Error recommending for {user_id}: {e}")
            continue

        if not recommended_ids:
            continue

        # Compute precision and recall
        truth = ground_truth[user_id]
        hits = len(set(recommended_ids) & truth)

        precision = hits / len(recommended_ids) if recommended_ids else 0
        recall = hits / len(truth) if truth else 0

        precisions.append(precision)
        recalls.append(recall)

        # Compute NDCG
        dcg = 0.0
        for i, item_id in enumerate(recommended_ids):
            if item_id in truth:
                dcg += 1.0 / np.log2(i + 2)

        idcg = sum(1.0 / np.log2(i + 2) for i in range(min(len(truth), k)))
        ndcg = dcg / idcg if idcg > 0 else 0
        ndcgs.append(ndcg)

    # Compute coverage (unique items recommended / total items)
    all_recommended = set()
    for user_id in eval_users:
        try:
            candidates = recall_engine.recall(user_id, context={})
            ranked = rank_engine.rank(user_id, candidates, context={})
            recommended = reranker.rerank(user_id, ranked, context={})
            all_recommended.update([item_id for item_id, _ in recommended[:k]])
        except Exception:
            pass

    # Load notes to get total count
    with open("./mock_data/notes.json", "r", encoding="utf-8") as f:
        notes = json.load(f)
    coverage = len(all_recommended) / len(notes) if notes else 0

    return {
        f"precision@{k}": np.mean(precisions) if precisions else 0,
        f"recall@{k}": np.mean(recalls) if recalls else 0,
        f"ndcg@{k}": np.mean(ndcgs) if ndcgs else 0,
        f"coverage@{k}": coverage,
        "num_eval_users": len(precisions),
    }


def evaluate_diversity(users: list, k: int = 20, sample_users: int = 100) -> Dict[str, float]:
    """Evaluate recommendation diversity."""
    eval_users = [u["user_id"] for u in users[:sample_users]]

    category_diversities = []
    author_diversities = []

    for user_id in eval_users:
        try:
            candidates = recall_engine.recall(user_id, context={})
            ranked = rank_engine.rank(user_id, candidates, context={})
            recommended = reranker.rerank(user_id, ranked, context={})
            recommended_ids = [item_id for item_id, _ in recommended[:k]]
        except Exception:
            continue

        if not recommended_ids:
            continue

        # Get categories and authors
        categories = []
        authors = []
        for nid in recommended_ids:
            # Load from mock data
            with open("./mock_data/notes.json", "r", encoding="utf-8") as f:
                notes = json.load(f)
            note_map = {n["note_id"]: n for n in notes}
            note = note_map.get(nid)
            if note:
                categories.append(note.get("category", ""))
                authors.append(note.get("author_id", ""))

        # Category diversity (number of unique categories / k)
        if categories:
            category_diversities.append(len(set(categories)) / k)

        # Author diversity
        if authors:
            author_diversities.append(len(set(authors)) / k)

    return {
        f"category_diversity@{k}": np.mean(category_diversities) if category_diversities else 0,
        f"author_diversity@{k}": np.mean(author_diversities) if author_diversities else 0,
    }


def main():
    print("Loading test data...")
    users, notes, interactions = load_test_data()

    print(f"Total users: {len(users)}")
    print(f"Total notes: {len(notes)}")
    print(f"Total interactions: {len(interactions)}")

    print("\nSplitting train/test...")
    train, test = split_train_test(interactions, test_ratio=0.2)
    print(f"Train: {len(train)}, Test: {len(test)}")

    print("\nBuilding ground truth...")
    ground_truth = get_user_ground_truth(test)

    print("\nEvaluating recommendations...")
    metrics = evaluate_recommendations(users, ground_truth, k=20, sample_users=100)

    print("\nEvaluating diversity...")
    diversity_metrics = evaluate_diversity(users, k=20, sample_users=100)
    metrics.update(diversity_metrics)

    print("\n" + "=" * 50)
    print("EVALUATION RESULTS")
    print("=" * 50)
    for metric, value in metrics.items():
        print(f"{metric:30s}: {value:.4f}")
    print("=" * 50)


if __name__ == "__main__":
    main()
