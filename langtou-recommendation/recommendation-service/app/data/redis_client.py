import json
import pickle
from typing import Any, List, Optional

import redis

from config import get_settings


class RedisClient:
    """Redis client for caching and fast data retrieval."""

    def __init__(self):
        self.settings = get_settings()
        self.client = redis.Redis(
            host=self.settings.REDIS_HOST,
            port=self.settings.REDIS_PORT,
            db=self.settings.REDIS_DB,
            password=self.settings.REDIS_PASSWORD or None,
            decode_responses=False,
        )

    def get(self, key: str) -> Optional[Any]:
        data = self.client.get(key)
        if data is None:
            return None
        try:
            return pickle.loads(data)
        except Exception:
            return data.decode("utf-8")

    def set(self, key: str, value: Any, expire: int = 3600) -> bool:
        try:
            data = pickle.dumps(value)
            return self.client.set(key, data, ex=expire)
        except Exception:
            return False

    def delete(self, key: str) -> int:
        return self.client.delete(key)

    def zadd(self, key: str, mapping: dict, expire: int = 3600) -> int:
        result = self.client.zadd(key, mapping)
        self.client.expire(key, expire)
        return result

    def zrevrange(self, key: str, start: int, end: int, withscores: bool = False) -> List:
        return self.client.zrevrange(key, start, end, withscores=withscores)

    def zscore(self, key: str, member: str) -> Optional[float]:
        score = self.client.zscore(key, member)
        return float(score) if score is not None else None

    def zrangebyscore(self, key: str, min_score: float, max_score: float, withscores: bool = False) -> List:
        return self.client.zrangebyscore(key, min_score, max_score, withscores=withscores)

    def hset(self, key: str, field: str, value: Any, expire: int = 3600) -> int:
        data = json.dumps(value) if not isinstance(value, (str, bytes)) else value
        result = self.client.hset(key, field, data)
        self.client.expire(key, expire)
        return result

    def hget(self, key: str, field: str) -> Optional[Any]:
        data = self.client.hget(key, field)
        if data is None:
            return None
        try:
            return json.loads(data)
        except Exception:
            return data.decode("utf-8")

    def hgetall(self, key: str) -> dict:
        data = self.client.hgetall(key)
        result = {}
        for k, v in data.items():
            k_str = k.decode("utf-8") if isinstance(k, bytes) else k
            try:
                result[k_str] = json.loads(v)
            except Exception:
                result[k_str] = v.decode("utf-8") if isinstance(v, bytes) else v
        return result

    def lpush(self, key: str, values: List[Any], expire: int = 3600) -> int:
        encoded = [json.dumps(v) if not isinstance(v, (str, bytes)) else v for v in values]
        result = self.client.lpush(key, *encoded)
        self.client.expire(key, expire)
        return result

    def lrange(self, key: str, start: int, end: int) -> List[Any]:
        data = self.client.lrange(key, start, end)
        result = []
        for item in data:
            try:
                result.append(json.loads(item))
            except Exception:
                result.append(item.decode("utf-8") if isinstance(item, bytes) else item)
        return result

    def exists(self, key: str) -> bool:
        return self.client.exists(key) > 0

    def keys(self, pattern: str) -> List[str]:
        raw = self.client.keys(pattern)
        return [k.decode("utf-8") if isinstance(k, bytes) else k for k in raw]

    def flushdb(self) -> bool:
        return self.client.flushdb()


redis_client = RedisClient()
