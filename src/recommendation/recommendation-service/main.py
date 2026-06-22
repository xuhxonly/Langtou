import logging
import sys
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router as recommend_router
from app.consumer.event_consumer import EventConsumer
from config import get_settings

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
    ],
)
logger = logging.getLogger(__name__)

settings = get_settings()

# Kafka消费者实例（全局）
_event_consumer: EventConsumer = None


def ensure_item_similarity():
    """
    启动时检查Redis中是否存在item_sim数据，
    如果不存在则触发离线计算（同步阻塞，仅在首次启动时执行）。
    """
    try:
        from app.data import redis_client
        existing_keys = redis_client.keys("item_sim:*")
        if existing_keys:
            logger.info(f"Redis中已存在 {len(existing_keys)} 个item_sim键，跳过计算")
            return

        logger.info("Redis中无item_sim数据，启动离线计算...")
        from scripts.compute_item_similarity import compute_and_store_item_similarity
        compute_and_store_item_similarity()
    except Exception as e:
        logger.error(f"启动时计算item similarity失败: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler - 启动/关闭Kafka消费者线程。"""
    global _event_consumer
    logger.info(f"Starting {settings.APP_NAME} v{settings.APP_VERSION}")

    # 启动时检查并计算item相似度（如果Redis中无数据）
    ensure_item_similarity()

    # 启动Kafka消费者线程（实时特征管道）
    try:
        _event_consumer = EventConsumer()
        _event_consumer.start()
        logger.info("Kafka实时特征管道消费者已启动")
    except Exception as e:
        logger.error(f"启动Kafka消费者失败: {e}")

    yield

    # 关闭Kafka消费者线程
    if _event_consumer:
        try:
            _event_consumer.stop()
            logger.info("Kafka实时特征管道消费者已停止")
        except Exception as e:
            logger.error(f"停止Kafka消费者失败: {e}")

    logger.info("Shutting down recommendation service")


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Langtou social content community recommendation API",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(recommend_router)

# Prometheus metrics endpoint
from prometheus_client import make_asgi_app
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)


@app.get("/")
async def root():
    return {
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "running",
    }


@app.get("/health")
async def health():
    return {"status": "healthy"}


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        workers=settings.WORKERS,
        reload=settings.DEBUG,
    )
