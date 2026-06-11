import os
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Application
    APP_NAME: str = "Langtou Recommendation Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"
    ENV: str = os.getenv("ENV", "development")

    # Server
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    WORKERS: int = int(os.getenv("WORKERS", "1"))

    # MySQL
    MYSQL_HOST: str = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_PORT: int = int(os.getenv("MYSQL_PORT", "3306"))
    MYSQL_USER: str = os.getenv("MYSQL_USER", "langtou")
    MYSQL_PASSWORD: str = os.getenv("MYSQL_PASSWORD", "CHANGE_ME")
    MYSQL_DB: str = os.getenv("MYSQL_DB", "langtou")

    # Redis
    REDIS_HOST: str = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))
    REDIS_PASSWORD: str = os.getenv("REDIS_PASSWORD", "")

    # Elasticsearch
    ES_HOST: str = os.getenv("ES_HOST", "localhost")
    ES_PORT: int = int(os.getenv("ES_PORT", "9200"))
    ES_USER: str = os.getenv("ES_USER", "")
    ES_PASSWORD: str = os.getenv("ES_PASSWORD", "")

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    KAFKA_TOPIC_EVENTS: str = os.getenv("KAFKA_TOPIC_EVENTS", "user_events")
    KAFKA_TOPIC_FEEDBACK: str = os.getenv("KAFKA_TOPIC_FEEDBACK", "user_feedback")

    # Recommendation
    RECALL_NUM: int = int(os.getenv("RECALL_NUM", "500"))
    RANK_NUM: int = int(os.getenv("RANK_NUM", "100"))
    RETURN_NUM: int = int(os.getenv("RETURN_NUM", "20"))

    # Model paths
    CF_MODEL_PATH: str = os.getenv("CF_MODEL_PATH", "./saved_models/cf_model.json")
    CONTENT_MODEL_PATH: str = os.getenv("CONTENT_MODEL_PATH", "./saved_models/content_model.json")
    HOT_MODEL_PATH: str = os.getenv("HOT_MODEL_PATH", "./saved_models/hot_model.json")

    # Feature store
    FEATURE_STORE_TYPE: str = os.getenv("FEATURE_STORE_TYPE", "redis")
    FEATURE_STORE_HOST: str = os.getenv("FEATURE_STORE_HOST", "localhost")
    FEATURE_STORE_PORT: int = int(os.getenv("FEATURE_STORE_PORT", "6379"))

    # Logging
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    LOG_FORMAT: str = os.getenv("LOG_FORMAT", "json")

    # Metrics
    METRICS_ENABLED: bool = os.getenv("METRICS_ENABLED", "true").lower() == "true"
    METRICS_PORT: int = int(os.getenv("METRICS_PORT", "9090"))

    # CORS
    CORS_ORIGINS: list = os.getenv("CORS_ORIGINS", "*").split(",")

    class Config:
        env_file = ".env"
        case_sensitive = True


@lru_cache()
def get_settings() -> Settings:
    return Settings()
