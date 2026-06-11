# 榔头(Langtou) 推荐系统

榔头(Langtou)社交内容社区APP的个性化推荐系统原型，采用经典的三层推荐架构：召回 -> 排序 -> 重排。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│         /api/v1/recommend/feed | hot | search | feedback    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      推荐引擎 (Recommendation Engine)         │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────────┐  │
│  │   召回层     │ → │   排序层     │ → │     重排层       │  │
│  │  Recall     │   │   Rank      │   │    Re-rank      │  │
│  └─────────────┘   └─────────────┘   └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      数据层 (Data Layer)                     │
│    Redis (缓存)    Elasticsearch (检索)    MySQL (持久化)    │
└─────────────────────────────────────────────────────────────┘
```

## 项目结构

```
langtou-recommendation/
├── recommendation-service/          # 推荐服务主项目
│   ├── app/
│   │   ├── api/                     # FastAPI接口
│   │   │   ├── routes.py            # API路由
│   │   │   └── models.py            # Pydantic模型
│   │   ├── engine/                  # 推荐引擎
│   │   │   ├── recall.py            # 召回层（多路召回）
│   │   │   ├── rank.py              # 排序层（特征+模型）
│   │   │   └── rerank.py            # 重排层（多样性+规则）
│   │   ├── models/                  # 机器学习模型
│   │   │   ├── cf_model.py          # 协同过滤
│   │   │   ├── content_model.py     # 内容相似度
│   │   │   └── hot_model.py         # 热度模型
│   │   ├── features/                # 特征工程
│   │   │   ├── user_features.py     # 用户特征
│   │   │   ├── item_features.py     # 物品特征
│   │   │   └── context_features.py  # 上下文特征
│   │   └── data/                    # 数据层
│   │       ├── redis_client.py      # Redis客户端
│   │       ├── es_client.py         # ES客户端
│   │       └── mysql_client.py      # MySQL客户端
│   ├── config/                      # 配置文件
│   │   └── settings.py              # 环境配置
│   ├── tests/                       # 单元测试
│   ├── scripts/                     # 工具脚本
│   │   ├── generate_mock_data.py    # 生成Mock数据
│   │   ├── load_mock_data.py        # 加载数据到存储
│   │   ├── train_rank_model.py      # 训练排序模型
│   │   └── evaluate.py              # 效果评估
│   ├── main.py                      # 服务入口
│   ├── requirements.txt             # Python依赖
│   └── Dockerfile                   # Docker镜像
└── README.md                        # 项目说明
```

## 核心算法

### 1. 召回层 (Recall)

采用多路召回策略，每路召回负责不同的信号：

| 召回策略 | 权重 | 说明 |
|---------|------|------|
| 协同过滤 (CFRecall) | 1.0 | 基于用户行为的Item-based CF |
| 内容相似 (ContentRecall) | 0.8 | 基于标签/文本的内容召回 |
| 实时热度 (HotRecall) | 0.5 | 热门内容兜底 |
| 用户画像 (UserProfileRecall) | 0.7 | 基于用户偏好分类 |
| 社交关系 (SocialRecall) | 0.9 | 关注作者内容 |

多路召回结果通过加权融合，对出现在多路中的item给予额外奖励。

### 2. 排序层 (Rank)

#### 特征工程

- **用户特征**: 年龄、性别、等级、活跃度、兴趣熵、交互比率
- **物品特征**: 内容长度、标签数、互动率、作者等级、时效性
- **上下文特征**: 时间、设备、位置、session深度、位置偏置
- **交叉特征**: 用户-物品embedding相似度

#### 排序模型

支持 XGBoost / LightGBM / sklearn GradientBoosting：

```python
# XGBoost 默认参数
{
    "objective": "binary:logistic",
    "eval_metric": "auc",
    "max_depth": 6,
    "learning_rate": 0.1,
    "n_estimators": 100,
}

# LightGBM 默认参数
{
    "objective": "binary",
    "metric": "auc",
    "num_leaves": 31,
    "learning_rate": 0.05,
}
```

模型加载失败时自动回退到启发式评分。

### 3. 重排层 (Re-rank)

- **多样性提升**: MMR算法，lambda=0.7，惩罚同类内容
- **新鲜度提升**: 新内容(24h) boost 1.15，近期内容(72h) boost 1.05
- **业务规则**: 过滤已曝光、关注作者boost、低质内容降权
- **探索机制**: epsilon-greedy (默认10%)，随机打乱头部内容

## API接口

### 获取个性化Feed

```
GET  /api/v1/recommend/feed?user_id={user_id}&page={page}&page_size={size}
POST /api/v1/recommend/feed
```

### 获取热门内容

```
GET  /api/v1/recommend/hot?category={cat}&page={page}
POST /api/v1/recommend/hot
```

### 用户反馈

```
POST /api/v1/recommend/feedback
Body: {
  "user_id": "user_000001",
  "note_id": "note_00000001",
  "action": "like",
  "score": 3.0
}
```

### 搜索推荐

```
GET  /api/v1/recommend/search?query={query}&user_id={user_id}
POST /api/v1/recommend/search
```

## 快速开始

### 环境要求

- Python 3.11+
- Redis
- Elasticsearch 8.x
- MySQL 8.x

### 安装依赖

```bash
cd recommendation-service
pip install -r requirements.txt
```

### 生成Mock数据

```bash
python scripts/generate_mock_data.py
python scripts/load_mock_data.py
```

### 训练排序模型

```bash
python scripts/train_rank_model.py
```

### 启动服务

```bash
python main.py
```

服务将在 `http://localhost:8000` 启动，API文档访问 `http://localhost:8000/docs`。

### Docker部署

```bash
docker build -t langtou-recommendation .
docker run -p 8000:8000 langtou-recommendation
```

### 运行测试

```bash
pytest tests/
```

### 效果评估

```bash
python scripts/evaluate.py
```

## 配置说明

通过环境变量或 `.env` 文件配置：

```env
DEBUG=true
PORT=8000

REDIS_HOST=localhost
REDIS_PORT=6379

ES_HOST=http://localhost:9200

MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=password
MYSQL_DB=langtou

RECALL_NUM=500
RANK_NUM=100
FINAL_NUM=20
```

## 评估指标

| 指标 | 说明 |
|------|------|
| Precision@K | 推荐列表中相关item的比例 |
| Recall@K | 相关item被推荐的比例 |
| NDCG@K | 考虑排序位置的归一化折扣累积增益 |
| Coverage@K | 推荐系统覆盖的item比例 |
| Category Diversity | 推荐列表中的类别多样性 |
| Author Diversity | 推荐列表中的作者多样性 |

## 未来优化方向

1. **深度学习排序**: 引入DIN/DCN等深度模型
2. **实时特征**: Flink实时特征计算
3. **向量召回**: 双塔模型 + Faiss向量检索
4. **多目标优化**: 同时优化点击、点赞、时长
5. **A/B测试框架**: 实验平台支持
6. **冷启动策略**: 新用户/新内容专项优化
