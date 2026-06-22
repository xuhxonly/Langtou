"""
离线计算笔记相似度脚本
基于两种策略计算笔记间的相似度，结果写入Redis供CFRecall使用：
1. 基于标签的Jaccard相似度
2. 基于用户共现行为（同时被点赞/收藏的笔记对）

Redis key格式: item_sim:{note_id} -> 有序集合 (member: note_id, score: similarity)
"""

import sys
import os
import logging
from collections import defaultdict
from typing import Any, Dict, List, Set, Tuple

# 将项目根目录加入路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.data import redis_client, mysql_client

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# ============ 配置参数 ============
TOP_K = 20              # 每个笔记保留的Top-K相似笔记
TAG_SIM_WEIGHT = 0.4     # 标签Jaccard相似度权重
COOCCUR_SIM_WEIGHT = 0.6  # 共现相似度权重
REDIS_EXPIRE = 86400 * 7  # Redis过期时间（7天）
MIN_SIM_THRESHOLD = 0.01  # 最低相似度阈值


def load_all_notes() -> Dict[str, Dict[str, Any]]:
    """从MySQL加载所有笔记的基本信息和标签。"""
    logger.info("从MySQL加载笔记数据...")
    notes = mysql_client.fetchall("SELECT id, title, tags FROM note WHERE status = 1")
    note_map: Dict[str, Dict[str, Any]] = {}
    for note in notes:
        note_id = str(note.get("id", ""))
        if not note_id:
            continue

        # 解析标签
        tags_raw = note.get("tags", "")
        if isinstance(tags_raw, str):
            tags = set(t.strip() for t in tags_raw.split(",") if t.strip())
        elif isinstance(tags_raw, list):
            tags = set(str(t).strip() for t in tags_raw if str(t).strip())
        else:
            tags = set()

        note_map[note_id] = {
            "id": note_id,
            "title": note.get("title", ""),
            "tags": tags,
        }

    logger.info(f"加载了 {len(note_map)} 条笔记")
    return note_map


def compute_tag_jaccard_similarity(note_map: Dict[str, Dict[str, Any]]) -> Dict[str, Dict[str, float]]:
    """
    基于标签的Jaccard相似度计算。
    Jaccard(A, B) = |A ∩ B| / |A ∪ B|
    """
    logger.info("计算标签Jaccard相似度...")
    sim_matrix: Dict[str, Dict[str, float]] = defaultdict(dict)

    note_ids = list(note_map.keys())
    total = len(note_ids)

    for i in range(total):
        note_a = note_map[note_ids[i]]
        tags_a = note_a["tags"]
        if not tags_a:
            continue

        for j in range(i + 1, total):
            note_b = note_map[note_ids[j]]
            tags_b = note_b["tags"]
            if not tags_b:
                continue

            intersection = len(tags_a & tags_b)
            union = len(tags_a | tags_b)
            if union == 0:
                continue

            jaccard = intersection / union
            if jaccard >= MIN_SIM_THRESHOLD:
                sim_matrix[note_ids[i]][note_ids[j]] = jaccard
                sim_matrix[note_ids[j]][note_ids[i]] = jaccard

    logger.info(f"标签相似度计算完成，有效笔记对数: {sum(len(v) for v in sim_matrix.values())}")
    return dict(sim_matrix)


def load_user_interactions() -> Dict[str, List[str]]:
    """
    加载用户行为数据（点赞和收藏）。
    返回: {user_id: [note_id, ...]}
    """
    logger.info("加载用户行为数据...")

    # 加载点赞记录
    like_records = mysql_client.fetchall(
        "SELECT user_id, target_id FROM like_record WHERE target_type = 1"
    )

    # 加载收藏记录（如果表结构支持）
    try:
        collect_records = mysql_client.fetchall(
            "SELECT user_id, target_id FROM collect_record"
        )
    except Exception:
        logger.warning("collect_record表不存在，仅使用点赞记录")
        collect_records = []

    user_items: Dict[str, List[str]] = defaultdict(list)

    for record in like_records:
        user_id = str(record.get("user_id", ""))
        target_id = str(record.get("target_id", ""))
        if user_id and target_id:
            user_items[user_id].append(target_id)

    for record in collect_records:
        user_id = str(record.get("user_id", ""))
        target_id = str(record.get("target_id", ""))
        if user_id and target_id:
            user_items[user_id].append(target_id)

    logger.info(f"加载了 {len(user_items)} 个用户的行为数据")
    return dict(user_items)


def compute_cooccurrence_similarity(
    user_items: Dict[str, List[str]],
    note_map: Dict[str, Dict[str, Any]],
) -> Dict[str, Dict[str, float]]:
    """
    基于用户共现行为计算相似度。
    如果两个笔记被同一个用户点赞/收藏，则认为它们相似。
    使用余弦相似度: sim(A, B) = |users(A) ∩ users(B)| / sqrt(|users(A)| * |users(B)|)
    """
    logger.info("计算共现相似度...")

    # 构建笔记 -> 用户集合的倒排索引
    note_users: Dict[str, Set[str]] = defaultdict(set)
    for user_id, items in user_items.items():
        for item_id in items:
            if item_id in note_map:
                note_users[item_id].add(user_id)

    sim_matrix: Dict[str, Dict[str, float]] = defaultdict(dict)
    note_ids = list(note_users.keys())
    total = len(note_ids)

    for i in range(total):
        note_a = note_ids[i]
        users_a = note_users[note_a]
        if not users_a:
            continue

        for j in range(i + 1, total):
            note_b = note_ids[j]
            users_b = note_users[note_b]
            if not users_b:
                continue

            common_users = len(users_a & users_b)
            if common_users == 0:
                continue

            # 余弦相似度
            denominator = (len(users_a) * len(users_b)) ** 0.5
            cos_sim = common_users / denominator if denominator > 0 else 0

            if cos_sim >= MIN_SIM_THRESHOLD:
                sim_matrix[note_a][note_b] = cos_sim
                sim_matrix[note_b][note_a] = cos_sim

    logger.info(f"共现相似度计算完成，有效笔记对数: {sum(len(v) for v in sim_matrix.values())}")
    return dict(sim_matrix)


def merge_similarities(
    tag_sim: Dict[str, Dict[str, float]],
    cooccur_sim: Dict[str, Dict[str, float]],
    note_map: Dict[str, Dict[str, Any]],
) -> Dict[str, Dict[str, float]]:
    """合并两种相似度，取加权平均。"""
    logger.info("合并相似度矩阵...")
    all_note_ids = set(note_map.keys())
    merged: Dict[str, Dict[str, float]] = defaultdict(dict)

    # 合并标签相似度
    for note_a, neighbors in tag_sim.items():
        for note_b, score in neighbors.items():
            merged[note_a][note_b] = score * TAG_SIM_WEIGHT

    # 合并共现相似度
    for note_a, neighbors in cooccur_sim.items():
        for note_b, score in neighbors.items():
            if note_b in merged[note_a]:
                merged[note_a][note_b] += score * COOCCUR_SIM_WEIGHT
            else:
                merged[note_a][note_b] = score * COOCCUR_SIM_WEIGHT

    return dict(merged)


def write_to_redis(sim_matrix: Dict[str, Dict[str, float]], top_k: int = TOP_K):
    """将Top-K相似度写入Redis有序集合。"""
    logger.info(f"写入Redis (Top-{top_k})...")
    written_count = 0

    for note_id, neighbors in sim_matrix.items():
        # 按相似度排序，取Top-K
        sorted_neighbors = sorted(neighbors.items(), key=lambda x: x[1], reverse=True)[:top_k]

        if not sorted_neighbors:
            continue

        # 构建有序集合映射 {note_id: similarity_score}
        mapping = {nid: score for nid, score in sorted_neighbors}
        redis_key = f"item_sim:{note_id}"

        try:
            redis_client.zadd(redis_key, mapping, expire=REDIS_EXPIRE)
            written_count += 1
        except Exception as e:
            logger.error(f"写入Redis失败 {redis_key}: {e}")

    logger.info(f"Redis写入完成，共写入 {written_count} 个笔记的相似度数据")


def check_existing_data() -> bool:
    """检查Redis中是否已存在item_sim数据。"""
    try:
        existing_keys = redis_client.keys("item_sim:*")
        return len(existing_keys) > 0
    except Exception:
        return False


def compute_and_store_item_similarity():
    """主流程：计算并存储笔记相似度。"""
    logger.info("=" * 60)
    logger.info("开始离线计算笔记相似度")
    logger.info("=" * 60)

    # 1. 加载数据
    note_map = load_all_notes()
    if not note_map:
        logger.warning("没有笔记数据，跳过计算")
        return

    # 2. 计算标签Jaccard相似度
    tag_sim = compute_tag_jaccard_similarity(note_map)

    # 3. 加载用户行为并计算共现相似度
    user_items = load_user_interactions()
    cooccur_sim = compute_cooccurrence_similarity(user_items, note_map)

    # 4. 合并相似度
    merged_sim = merge_similarities(tag_sim, cooccur_sim, note_map)

    # 5. 写入Redis
    write_to_redis(merged_sim)

    logger.info("=" * 60)
    logger.info("笔记相似度计算完成")
    logger.info("=" * 60)


if __name__ == "__main__":
    compute_and_store_item_similarity()
