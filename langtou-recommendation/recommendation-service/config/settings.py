import os
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    APP_NAME: str = "Langtou Recommendation Service"
    DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"
    PORT: int = int(os.getenv("PORT", "8000"))
    HOST: str = os.getenv("HOST", "0.0.0.0")

    # Redis
    REDIS_HOST: str = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))
    REDIS_PASSWORD: str = os.getenv("REDIS_PASSWORD", "")

    # Elasticsearch
    ES_HOST: str = os.getenv("ES_HOST", "http://localhost:9200")
    ES_USER: str = os.getenv("ES_USER", "")
    ES_PASSWORD: str = os.getenv("ES_PASSWORD", "")

    # MySQL
    MYSQL_HOST: str = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_PORT: int = int(os.getenv("MYSQL_PORT", "3306"))
    MYSQL_USER: str = os.getenv("MYSQL_USER", "root")
    MYSQL_PASSWORD: str = os.getenv("MYSQL_PASSWORD", "")
    MYSQL_DB: str = os.getenv("MYSQL_DB", "langtou")

    # Model paths
    MODEL_DIR: str = os.getenv("MODEL_DIR", "./saved_models")

    # Recommendation params
    RECALL_NUM: int = int(os.getenv("RECALL_NUM", "500"))
    RANK_NUM: int = int(os.getenv("RANK_NUM", "100"))
    FINAL_NUM: int = int(os.getenv("FINAL_NUM", "20"))

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
