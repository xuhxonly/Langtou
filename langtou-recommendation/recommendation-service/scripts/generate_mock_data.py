#!/usr/bin/env python3
"""
Generate mock data for Langtou recommendation system.
This script creates simulated users, notes, and interactions.
"""

import json
import os
import random
import uuid
from datetime import datetime, timedelta

import numpy as np


def generate_users(n_users: int = 1000) -> list:
    """Generate mock user data."""
    genders = ["male", "female", "unknown"]
    cities = ["北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "", ""]
    categories = [
        "美食", "旅行", "时尚", "美妆", "家居", "健身",
        "摄影", "读书", "音乐", "电影", "科技", "游戏",
        "宠物", "母婴", "职场", "学习", "其他"
    ]

    users = []
    for i in range(n_users):
        user_id = f"user_{i:06d}"
        preferred = ",".join(random.sample(categories, k=random.randint(1, 4)))

        users.append({
            "user_id": user_id,
            "username": f"user_{i}",
            "age": random.randint(18, 60),
            "gender": random.choice(genders),
            "level": random.randint(1, 10),
            "register_days": random.randint(1, 1000),
            "city": random.choice(cities),
            "preferred_categories": preferred,
            "followers": random.randint(0, 10000),
            "following": random.randint(0, 500),
        })

    return users


def generate_notes(n_notes: int = 5000, n_users: int = 1000) -> list:
    """Generate mock note data."""
    categories = [
        "美食", "旅行", "时尚", "美妆", "家居", "健身",
        "摄影", "读书", "音乐", "电影", "科技", "游戏",
        "宠物", "母婴", "职场", "学习", "其他"
    ]

    tags_pool = {
        "美食": ["火锅", "烧烤", "甜品", "咖啡", "探店", "家常菜", "西餐", "日料"],
        "旅行": ["国内游", "出境游", "自驾游", "民宿", "攻略", "打卡", "风景"],
        "时尚": ["穿搭", "街拍", "潮流", "品牌", "OOTD", "配饰", "包包"],
        "美妆": ["护肤", "彩妆", "口红", "眼影", "测评", "教程", "好物"],
        "家居": ["装修", "收纳", "软装", "绿植", "DIY", "好物推荐"],
        "健身": ["瑜伽", "跑步", "减脂", "增肌", "饮食", "打卡"],
        "摄影": ["人像", "风景", "后期", "器材", "技巧", "胶片"],
        "读书": ["小说", "非虚构", "书单", "读后感", "推荐", "经典"],
        "音乐": ["流行", "摇滚", "古典", "独立", "演唱会", "歌单"],
        "电影": ["影评", "推荐", "经典", "院线", "独立电影", "导演"],
        "科技": ["数码", "AI", "编程", "评测", "教程", "新闻"],
        "游戏": ["手游", "主机", "PC", "攻略", "评测", "电竞"],
        "宠物": ["猫", "狗", "养护", "萌宠", "领养", "日常"],
        "母婴": ["育儿", "辅食", "早教", "好物", "经验", "孕期"],
        "职场": ["面试", "简历", "技能", "沟通", "管理", "跳槽"],
        "学习": ["英语", "考研", "考证", "效率", "方法", "资源"],
        "其他": ["日常", "心情", "吐槽", "分享", "求助"],
    }

    title_templates = [
        "{}攻略 | 必看",
        "关于{}的一些想法",
        "{}好物分享",
        "{}入门指南",
        "我的{}日常",
        "{}避坑指南",
        "{}测评",
        "{}打卡",
    ]

    notes = []
    now = datetime.now()

    for i in range(n_notes):
        note_id = f"note_{i:08d}"
        category = random.choice(categories)
        tags = random.sample(tags_pool.get(category, ["日常"]), k=random.randint(2, 5))

        # Generate title
        template = random.choice(title_templates)
        title = template.format(tags[0] if tags else category)

        # Generate content
        content = f"这是一篇关于{category}的笔记。"
        content += f"主要分享了{', '.join(tags)}相关的内容。"
        content += "希望对大家有所帮助！"

        # Time distribution: more recent notes
        days_ago = int(np.random.exponential(scale=30))
        create_time = now - timedelta(days=min(days_ago, 365))

        # Engagement correlated with quality (simulated)
        quality = random.random()
        views = int(quality * random.randint(100, 10000))
        likes = int(views * random.uniform(0.02, 0.15))
        comments = int(likes * random.uniform(0.1, 0.5))
        shares = int(likes * random.uniform(0.05, 0.2))

        notes.append({
            "note_id": note_id,
            "title": title,
            "content": content,
            "tags": ",".join(tags),
            "category": category,
            "author_id": f"user_{random.randint(0, n_users - 1):06d}",
            "create_time": create_time.isoformat(),
            "likes": likes,
            "comments": comments,
            "shares": shares,
            "views": views,
            "has_image": random.random() > 0.3,
            "has_video": random.random() > 0.8,
        })

    return notes


def generate_interactions(users: list, notes: list, n_interactions: int = 50000) -> list:
    """Generate mock user-item interactions."""
    interaction_types = ["click", "like", "share", "comment", "collect"]
    type_weights = [0.5, 0.25, 0.05, 0.1, 0.1]

    interactions = []
    now = datetime.now()

    for _ in range(n_interactions):
        user = random.choice(users)
        note = random.choice(notes)

        # Users prefer certain categories
        user_cats = user.get("preferred_categories", "").split(",")
        if note["category"] in user_cats:
            if random.random() > 0.3:
                # Higher engagement for preferred categories
                pass
            else:
                continue
        else:
            if random.random() > 0.1:
                continue

        inter_type = random.choices(interaction_types, weights=type_weights)[0]
        score_map = {"click": 1.0, "like": 3.0, "share": 5.0, "comment": 4.0, "collect": 4.0}

        days_ago = random.randint(0, 60)
        create_time = now - timedelta(days=days_ago)

        interactions.append({
            "user_id": user["user_id"],
            "note_id": note["note_id"],
            "interaction_type": inter_type,
            "score": score_map[inter_type],
            "create_time": create_time.isoformat(),
        })

    return interactions


def generate_follow_relations(users: list, n_relations: int = 5000) -> list:
    """Generate mock follow relations."""
    relations = []
    for _ in range(n_relations):
        follower = random.choice(users)
        followee = random.choice(users)
        if follower["user_id"] != followee["user_id"]:
            relations.append({
                "follower_id": follower["user_id"],
                "followee_id": followee["user_id"],
            })

    # Deduplicate
    seen = set()
    unique = []
    for r in relations:
        key = (r["follower_id"], r["followee_id"])
        if key not in seen:
            seen.add(key)
            unique.append(r)

    return unique


def save_data(data_dir: str = "./mock_data"):
    """Generate and save all mock data."""
    os.makedirs(data_dir, exist_ok=True)

    print("Generating users...")
    users = generate_users(1000)
    with open(os.path.join(data_dir, "users.json"), "w", encoding="utf-8") as f:
        json.dump(users, f, ensure_ascii=False, indent=2)

    print("Generating notes...")
    notes = generate_notes(5000, 1000)
    with open(os.path.join(data_dir, "notes.json"), "w", encoding="utf-8") as f:
        json.dump(notes, f, ensure_ascii=False, indent=2)

    print("Generating interactions...")
    interactions = generate_interactions(users, notes, 50000)
    with open(os.path.join(data_dir, "interactions.json"), "w", encoding="utf-8") as f:
        json.dump(interactions, f, ensure_ascii=False, indent=2)

    print("Generating follow relations...")
    relations = generate_follow_relations(users, 5000)
    with open(os.path.join(data_dir, "relations.json"), "w", encoding="utf-8") as f:
        json.dump(relations, f, ensure_ascii=False, indent=2)

    print(f"Mock data saved to {data_dir}")
    print(f"  Users: {len(users)}")
    print(f"  Notes: {len(notes)}")
    print(f"  Interactions: {len(interactions)}")
    print(f"  Relations: {len(relations)}")


if __name__ == "__main__":
    save_data()
