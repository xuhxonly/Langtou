"""
Kafka事件消费者 - 实时特征管道

消费用户行为事件（like/comment/follow/view），实时更新：
1. 用户特征缓存到Redis（兴趣标签、活跃度等）
2. 笔记热度分数到Redis（加权热度计算）
"""

import json
import logging
import math
import threading
import time
from datetime import datetime
from typing import Any, Dict, Optional

from config import get_settings
from app.data.redis_client import redis_client

logger = logging.getLogger(__name__)

# 事件类型常量
EVENT_LIKE = "like"
EVENT_COMMENT = "comment"
EVENT_FOLLOW = "follow"
EVENT_VIEW = "view"
EVENT_COLLECT = "collect"
EVENT_SHARE = "share"

# 热度计算权重
HOT_SCORE_WEIGHTS = {
    EVENT_LIKE: 1.0,
    EVENT_COMMENT: 2.0,
    EVENT_COLLECT: 3.0,
    EVENT_SHARE: 4.0,
    EVENT_VIEW: 0.1,
}

# Redis key 前缀
USER_FEATURE_KEY_PREFIX = "user_features:"
USER_TAGS_KEY_PREFIX = "user_tags:"
USER_HISTORY_KEY_PREFIX = "user_history:"
NOTE_HOT_SCORE_KEY = "note_hot_scores"
USER_ACTIVITY_KEY_PREFIX = "user_activity:"


class EventConsumer:
    """
    Kafka事件消费者

    消费user_events topic中的用户行为事件，
    实时更新用户特征和笔记热度到Redis。
    """

    def __init__(self):
        self.settings = get_settings()
        self._running = False
        self._consumer = None
        self._thread: Optional[threading.Thread] = None

    def _create_consumer(self):
        """创建Kafka消费者实例。"""
        try:
            import kafka_python  # noqa: F401 - 检查是否安装
        except ImportError:
            try:
                from kafka import KafkaConsumer
            except ImportError:
                logger.warning(
                    "kafka-python未安装，使用模拟消费者模式。"
                    "安装方式: pip install kafka-python"
                )
                return None

        try:
            from kafka import KafkaConsumer

            self._consumer = KafkaConsumer(
                self.settings.KAFKA_TOPIC_EVENTS,
                bootstrap_servers=self.settings.KAFKA_BOOTSTRAP_SERVERS.split(","),
                group_id="langtou-recommendation-consumer",
                auto_offset_reset="latest",
                enable_auto_commit=True,
                auto_commit_interval_ms=1000,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                consumer_timeout_ms=1000,
            )
            logger.info(
                f"Kafka消费者已创建, topic={self.settings.KAFKA_TOPIC_EVENTS}, "
                f"servers={self.settings.KAFKA_BOOTSTRAP_SERVERS}"
            )
            return self._consumer
        except Exception as e:
            logger.error(f"创建Kafka消费者失败: {e}")
            return None

    def consume(self):
        """
        消费事件主循环。

        从Kafka读取用户行为事件，根据事件类型更新：
        - 用户兴趣标签（Redis Hash）
        - 用户行为历史（Redis List）
        - 用户活跃度指标（Redis Hash）
        - 笔记热度分数（Redis Sorted Set）
        """
        consumer = self._create_consumer()
        if consumer is None:
            logger.info("Kafka不可用，使用模拟消费者模式")
            self._mock_consume()
            return

        logger.info("开始消费Kafka事件...")
        while self._running:
            try:
                for message in consumer:
                    if not self._running:
                        break
                    try:
                        event = message.value
                        self._process_event(event)
                    except Exception as e:
                        logger.error(f"处理事件失败: {e}, message={message.value}")
            except Exception as e:
                if self._running:
                    logger.warning(f"Kafka消费异常: {e}, 将重试...")
                    time.sleep(5)

        if self._consumer:
            self._consumer.close()
            logger.info("Kafka消费者已关闭")

    def _mock_consume(self):
        """模拟消费者模式（开发/测试环境无Kafka时使用）。"""
        logger.info("模拟消费者模式已启动（无实际Kafka连接）")
        while self._running:
            time.sleep(10)

    def _process_event(self, event: Dict[str, Any]):
        """
        处理单个用户行为事件。

        Args:
            event: 事件数据，格式示例:
                {
                    "event_type": "like",
                    "user_id": "123",
                    "target_id": "456",
                    "target_type": "note",
                    "timestamp": "2024-01-01T00:00:00",
                    "extra": {}
                }
        """
        # 消息格式校验
        user_id = event.get("user_id")
        target_id = event.get("target_id")
        event_type = event.get("event_type", "")

        # 校验 user_id 存在且为正整数
        if user_id is None or not isinstance(user_id, (int, str)):
            logger.warning(f"无效事件: user_id 缺失或类型错误, event={event}")
            return
        try:
            user_id_int = int(user_id)
            if user_id_int <= 0:
                raise ValueError()
        except (ValueError, TypeError):
            logger.warning(f"无效事件: user_id 不是正整数, user_id={user_id}, event={event}")
            return

        # 校验 target_id 存在且为正整数
        if target_id is None or not isinstance(target_id, (int, str)):
            logger.warning(f"无效事件: target_id 缺失或类型错误, event={event}")
            return
        try:
            target_id_int = int(target_id)
            if target_id_int <= 0:
                raise ValueError()
        except (ValueError, TypeError):
            logger.warning(f"无效事件: target_id 不是正整数, target_id={target_id}, event={event}")
            return

        # 校验 event_type 为已知类型
        VALID_EVENT_TYPES = {EVENT_LIKE, EVENT_COMMENT, EVENT_FOLLOW, EVENT_VIEW, EVENT_COLLECT, EVENT_SHARE}
        if event_type not in VALID_EVENT_TYPES:
            logger.warning(f"无效事件: event_type 未知, event_type={event_type}, event={event}")
            return

        user_id = str(user_id_int)
        target_id = str(target_id_int)
        target_type = event.get("target_type", "note")
        timestamp = event.get("timestamp", datetime.now().isoformat())

        logger.debug(f"处理事件: type={event_type}, user={user_id}, target={target_id}")

        # 更新用户行为历史
        self._update_user_history(user_id, target_id, event_type, timestamp)

        # 更新用户活跃度
        self._update_user_activity(user_id, event_type)

        # 如果目标是笔记，更新笔记热度和用户兴趣标签
        if target_type == "note" and target_id:
            # 更新笔记热度分数
            self._update_note_hot_score(target_id, event_type)

            # 更新用户兴趣标签（基于笔记标签）
            self._update_user_interest_tags(user_id, target_id, event_type)

    def _update_user_history(self, user_id: str, target_id: str, event_type: str, timestamp: str):
        """
        更新用户行为历史到Redis。

        使用Redis List存储最近的行为记录，用于协同过滤召回。
        Key格式: user_history:{user_id}
        """
        history_key = f"{USER_HISTORY_KEY_PREFIX}{user_id}"
        record = {
            "target_id": target_id,
            "event_type": event_type,
            "timestamp": timestamp,
        }

        try:
            redis_client.lpush(history_key, [record], expire=7 * 24 * 3600)
            # 只保留最近200条记录
            try:
                redis_client.client.ltrim(history_key, 0, 199)
            except Exception:
                pass
        except Exception as e:
            logger.error(f"更新用户历史失败: user={user_id}, error={e}")

    def _update_user_activity(self, user_id: str, event_type: str):
        """
        更新用户活跃度指标到Redis。

        统计用户各类型行为次数，用于用户画像和特征提取。
        Key格式: user_activity:{user_id}
        """
        activity_key = f"{USER_ACTIVITY_KEY_PREFIX}{user_id}"

        try:
            # 获取当前活跃度数据
            activity = redis_client.hgetall(activity_key)
            if not activity:
                activity = {
                    "like_count": 0,
                    "comment_count": 0,
                    "follow_count": 0,
                    "view_count": 0,
                    "collect_count": 0,
                    "share_count": 0,
                    "last_active": timestamp_now_iso(),
                    "total_actions": 0,
                }

            # 更新对应计数
            count_field = f"{event_type}_count"
            if count_field in activity:
                activity[count_field] = int(activity.get(count_field, 0)) + 1
            activity["total_actions"] = int(activity.get("total_actions", 0)) + 1
            activity["last_active"] = timestamp_now_iso()

            # 写回Redis（24小时过期）
            for field, value in activity.items():
                redis_client.hset(activity_key, field, value, expire=24 * 3600)

        except Exception as e:
            logger.error(f"更新用户活跃度失败: user={user_id}, error={e}")

    def _update_note_hot_score(self, note_id: str, event_type: str):
        """
        更新笔记热度分数到Redis。

        使用加权公式计算热度：
        hot_score = likes * 1.0 + comments * 2.0 + collects * 3.0 + shares * 4.0 + views * 0.1
        并加入时间衰减因子。

        Key格式: note_hot_scores (Redis Sorted Set)
        """
        weight = HOT_SCORE_WEIGHTS.get(event_type, 0.1)

        try:
            # 获取当前热度分数
            current_score = redis_client.zscore(NOTE_HOT_SCORE_KEY, note_id)
            if current_score is None:
                current_score = 0.0

            # 增加热度
            new_score = current_score + weight

            # 更新到Redis Sorted Set（7天过期）
            redis_client.zadd(
                NOTE_HOT_SCORE_KEY,
                {note_id: new_score},
                expire=7 * 24 * 3600,
            )

            logger.debug(f"更新笔记热度: note={note_id}, type={event_type}, "
                         f"score={current_score:.2f} -> {new_score:.2f}")

        except Exception as e:
            logger.error(f"更新笔记热度失败: note={note_id}, error={e}")

    def _update_user_interest_tags(self, user_id: str, note_id: str, event_type: str):
        """
        基于用户交互的笔记更新用户兴趣标签。

        从笔记中提取标签，根据行为类型加权累加到用户兴趣标签中。
        Key格式: user_tags:{user_id}
        """
        # 行为权重：收藏 > 点赞 > 评论 > 浏览
        tag_weight = {
            EVENT_COLLECT: 3.0,
            EVENT_LIKE: 2.0,
            EVENT_COMMENT: 1.5,
            EVENT_VIEW: 0.5,
            EVENT_SHARE: 2.5,
        }.get(event_type, 1.0)

        try:
            from app.data.mysql_client import mysql_client

            # 获取笔记信息
            note = mysql_client.get_note(note_id)
            if not note:
                return

            tags = note.get("tags", [])
            if isinstance(tags, str):
                tags = [t.strip() for t in tags.split(",") if t.strip()]
            elif not isinstance(tags, list):
                tags = []

            if not tags:
                return

            # 更新用户兴趣标签
            tags_key = f"{USER_TAGS_KEY_PREFIX}{user_id}"
            current_tags = redis_client.hgetall(tags_key)

            for tag in tags:
                current_score = 0.0
                if current_tags and tag in current_tags:
                    current_score = float(current_tags[tag])
                new_score = current_score + tag_weight
                redis_client.hset(tags_key, tag, round(new_score, 2), expire=30 * 24 * 3600)

            logger.debug(f"更新用户兴趣标签: user={user_id}, tags={tags}, weight={tag_weight}")

        except Exception as e:
            logger.error(f"更新用户兴趣标签失败: user={user_id}, note={note_id}, error={e}")

    def start(self):
        """启动消费者线程。"""
        if self._running:
            logger.warning("消费者已在运行中")
            return

        self._running = True
        self._thread = threading.Thread(target=self.consume, daemon=True, name="kafka-consumer")
        self._thread.start()
        logger.info("Kafka消费者线程已启动")

    def stop(self):
        """停止消费者线程。"""
        self._running = False
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=10)
        logger.info("Kafka消费者线程已停止")


def start_consumer_thread() -> EventConsumer:
    """
    创建并启动Kafka消费者线程。

    Returns:
        EventConsumer实例
    """
    consumer = EventConsumer()
    consumer.start()
    return consumer


def timestamp_now_iso() -> str:
    """返回当前时间的ISO格式字符串。"""
    return datetime.now().isoformat()
