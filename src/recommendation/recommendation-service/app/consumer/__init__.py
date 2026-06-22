"""
Kafka消费者模块 - 实时特征管道
消费用户行为事件，实时更新用户特征缓存和笔记热度分数
"""

from .event_consumer import EventConsumer, start_consumer_thread

__all__ = ["EventConsumer", "start_consumer_thread"]
