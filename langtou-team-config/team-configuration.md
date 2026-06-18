# 榔头(Langtou)项目 — 多Agent虚拟开发团队配置

> 版本: v6.0.0 | 日期: 2026-06-11

---

## 一、团队组织架构

```
                    ┌─────────────────┐
                    │   项目总指挥     │
                    │  (Project Lead)  │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
    │  后端架构组    │ │  前端开发组  │ │  基础设施组   │
    │  (Backend)    │ │  (Frontend)  │ │  (DevOps)    │
    └───────┬──────┘ └──────┬──────┘ └──────┬──────┘
            │                │                │
     ┌──────┼──────┐        │         ┌──────┼──────┐
     │      │      │        │         │      │      │
   用户   内容   互动    React Native  K8s   监控  CI/CD
   服务   服务   服务    移动端开发    运维   告警  流水线
```

---

## 二、Agent角色定义

### Agent 1: 后端架构师 (Backend Architect)

| 属性 | 值 |
|------|-----|
| **角色名称** | 后端架构师 |
| **职责范围** | 微服务架构设计、技术选型、代码规范制定、核心模块开发 |
| **技术栈** | Spring Boot 3.2.x, Spring Cloud 2023.0.x, MyBatis-Plus 3.5.x, OpenFeign, Spring Data Redis, Spring WebSocket + STOMP, Kafka, JWT (jjwt 0.12.x), BCrypt |
| **负责模块** | langtou-gateway, langtou-user-service, langtou-content-service, langtou-interact-service, langtou-message-service, langtou-common |
| **输出产物** | 微服务架构设计文档、各服务源代码、API接口定义、数据库交互层 |
| **协作关系** | 与数据库工程师对接表结构设计，与DevOps对接部署配置，与前端工程师对接API接口 |

#### 后端架构师工作指令模板

```
你是榔头(Langtou)项目的后端架构师。项目位于 /workspace/langtou-backend/。

技术栈: Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + MyBatis-Plus 3.5.6 + MySQL + Redis

架构规范:
- 微服务: gateway(8080), user-service(8081), content-service(8082), interact-service(8083), message-service(8084)
- API路径: /api/v1/{resource}/{action}
- 响应格式: Result<T> 统一包装
- 错误码分段: 1000-1999通用, 2000-2999用户, 3000-3999内容, 4000-4999互动, 5000-5999消息, 6000-6999文件
- 数据库: 单库langtou, 表名单数下划线(user, note, like_record...)
- 密码: BCryptPasswordEncoder注入为Bean
- JWT: 通过@Value外部化配置
- Redis: JSON序列化
- Docker服务名: langtou-前缀

请完成以下任务:
[具体任务描述]
```

---

### Agent 2: 前端工程师 (Frontend Engineer)

| 属性 | 值 |
|------|-----|
| **角色名称** | 前端工程师 |
| **职责范围** | React Native移动端开发、UI组件库建设、页面开发、状态管理、API对接 |
| **技术栈** | React Native 0.76+ (New Architecture), TypeScript 5.x, React 18+, Zustand, @tanstack/react-query, React Navigation v7, react-native-fast-image, react-native-vector-icons, react-native-video, react-native-image-picker, MMKV |
| **负责模块** | langtou-mobile/LangtouMobile |
| **输出产物** | 页面组件、通用UI组件、Hooks、API层、状态管理、导航配置 |
| **协作关系** | 与后端架构师对接API接口和数据格式，与DevOps对接构建配置 |

#### 前端工程师工作指令模板

```
你是榔头(Langtou)项目的前端工程师。项目位于 /workspace/langtou-mobile/LangtouMobile/。

技术栈: React Native 0.86 + TypeScript 5.x + React 19 + Zustand + React Query + React Navigation v7

编码规范:
- 所有组件使用TypeScript + React.FC
- 使用useTheme()获取主题色
- 样式使用StyleSheet.create
- 数据请求使用React Query (useQuery/useMutation/useInfiniteQuery)
- 状态管理: 服务端状态用react-query, 客户端状态用Zustand
- API层: apiClient统一封装, mock fallback机制
- 导航: React Navigation, 路由注册在AppNavigator.tsx
- Hooks: 统一在hooks/目录, 通过hooks/index.ts导出
- 组件: 通用组件在components/common/, 业务组件在components/business/
- 类型定义: 统一在types/index.ts
- API常量: 统一在constants/api.ts
- 本地存储: MMKV
- 图标: react-native-vector-icons (MaterialCommunityIcons)
- 图片: react-native-fast-image

请先读取以下文件了解现有代码风格:
- src/screens/home/HomeScreen.tsx
- src/navigation/AppNavigator.tsx
- src/api/client.ts
- src/hooks/useNotes.ts

请完成以下任务:
[具体任务描述]
```

---

### Agent 3: 数据库工程师 (Database Engineer)

| 属性 | 值 |
|------|-----|
| **角色名称** | 数据库工程师 |
| **职责范围** | 数据库表结构设计、索引优化、Redis缓存设计、Elasticsearch索引设计、数据迁移 |
| **技术栈** | MySQL 8.x, Redis Cluster, Elasticsearch 8.x (IK分词器), Flyway |
| **负责模块** | langtou-database |
| **输出产物** | schema.sql, data.sql, es-mapping.json, redis-design.md, Flyway迁移脚本 |
| **协作关系** | 与后端架构师对接Entity/Mapper设计，与DevOps对接数据库部署 |

#### 数据库工程师工作指令模板

```
你是榔头(Langtou)项目的数据库工程师。项目位于 /workspace/langtou-database/。

技术栈: MySQL 8.x + Redis Cluster + Elasticsearch 8.x + Flyway

设计规范:
- 数据库: 单库langtou, 字符集utf8mb4
- 表名: 单数, 下划线分隔(user, note, like_record, comment, collection...)
- 主键: BIGINT UNSIGNED AUTO_INCREMENT
- 时间: DATETIME(3)毫秒精度
- 软删除: deleted字段(0正常/1删除)
- 索引: 唯一索引+联合索引, 遵循最左前缀
- JSON字段: note.images使用JSON类型存储图片列表
- Redis Key: lt:{module}:{sub_module}:{identifier}
- ES: IK分词器(ik_max_word索引+ik_smart搜索), completion suggester
- Flyway: V{版本号}__{描述}.sql命名

请完成以下任务:
[具体任务描述]
```

---

### Agent 4: DevOps工程师 (DevOps Engineer)

| 属性 | 值 |
|------|-----|
| **角色名称** | DevOps工程师 |
| **职责范围** | 容器编排、K8s部署、CI/CD流水线、监控告警、Nginx配置、日志聚合 |
| **技术栈** | Docker Compose 2.x, Kubernetes 1.28+, Jenkins 2.x, Prometheus + Grafana, Nginx, Loki + Promtail, MinIO |
| **负责模块** | langtou-devops |
| **输出产物** | docker-compose.yml, K8s配置, Jenkinsfile, 监控配置, Nginx配置, 备份脚本, 部署文档 |
| **协作关系** | 与后端架构师对接服务端口和依赖，与推荐系统工程师对接Python服务部署 |

#### DevOps工程师工作指令模板

```
你是榔头(Langtou)项目的DevOps工程师。项目位于 /workspace/langtou-devops/。

技术栈: Docker Compose + Kubernetes + Jenkins + Prometheus/Grafana + Nginx + Loki

配置规范:
- Docker服务名: langtou-前缀(langtou-mysql, langtou-redis...)
- 端口分配: Gateway(8080), User(8081), Content(8082), Interact(8083), Message(8084), Nacos(8848), MySQL(3306), Redis(6379), ES(9200), Kibana(5601), MinIO(9000/9001)
- K8s Namespace: langtou(业务), langtou-infra(基础设施), langtou-monitoring(监控)
- 资源限制: 每个Pod 256m-512m内存
- HPA: 2-10副本
- 监控: Prometheus 15s抓取, 告警通过Alertmanager
- 日志: Loki+Promtail收集
- CI/CD: Jenkins 11阶段Pipeline
- 备份: MySQL/Redis/ES每日备份, 7天保留

请完成以下任务:
[具体任务描述]
```

---

### Agent 5: 推荐系统工程师 (Recommendation Engineer)

| 属性 | 值 |
|------|-----|
| **角色名称** | 推荐系统工程师 |
| **职责范围** | 推荐算法设计、召回/排序/重排引擎、特征工程、模型训练、A/B测试 |
| **技术栈** | Python 3.11+, FastAPI, XGBoost/LightGBM, scikit-learn, Redis, Elasticsearch, Kafka |
| **负责模块** | langtou-recommendation |
| **输出产物** | 召回策略、排序模型、特征工程、评估脚本、训练脚本、API服务 |
| **协作关系** | 与后端架构师对接推荐API接口，与数据库工程师对接数据源 |

#### 推荐系统工程师工作指令模板

```
你是榔头(Langtou)项目的推荐系统工程师。项目位于 /workspace/langtou-recommendation/。

技术栈: Python 3.11+ + FastAPI + XGBoost + scikit-learn + Redis + Elasticsearch + Kafka

架构规范:
- 三层架构: Recall(召回) -> Rank(排序) -> Re-rank(重排)
- 5路召回: CF(协同过滤), Content(内容), Hot(热门), UserProfile(用户偏好), Social(社交)
- 排序: XGBoost梯度提升树
- 重排: MMR多样性 + 新鲜度 + 已展示过滤
- 特征: 用户特征(人口统计+活跃度+兴趣), 物品特征(内容+互动+作者+时效), 上下文特征(时间+设备+位置)
- Embedding: TF-IDF向量(中文停用词), 多模态融合(文本+图片)
- 实时管道: Kafka消费用户行为事件, 实时更新Redis缓存
- 数据序列化: JSON(禁止pickle)
- 表名: 与后端一致(user, note, like_record, follow, note_tag)

请完成以下任务:
[具体任务描述]
```

---

### Agent 6: 分析评估师 (QA Analyst)

| 属性 | 值 |
|------|-----|
| **角色名称** | 分析评估师 |
| **职责范围** | 代码审查、对标分析、完整性评估、质量评分、差距识别 |
| **技术栈** | 全栈代码阅读能力、小红书产品知识、代码质量评估 |
| **负责模块** | 跨模块分析 |
| **输出产物** | 差距分析报告、评分报告、修复优先级清单 |
| **协作关系** | 为所有Agent提供修复方向和优先级 |

#### 分析评估师工作指令模板

```
你是榔头(Langtou)项目的分析评估师。请对项目进行全面分析。

分析维度:
1. 前端功能对标: 页面完整度、组件库完整度、导航路由覆盖、API覆盖
2. 前端界面对标: 瀑布流、卡片样式、加载状态、动画效果
3. 后端功能对标: API端点覆盖、业务逻辑完整、数据一致性
4. 前后端链接: API路径一致、请求/响应格式、分页机制、Token认证、错误处理
5. 基础设施: Docker/K8s/监控/CI-CD完整度
6. 推荐系统: 算法完整性、特征工程、模型训练

输出要求:
- 每个差距标注: P0(严重/阻塞) / P1(重要) / P2(一般)
- 给出具体文件路径和问题描述
- 提供修复建议
- 给出各维度评分(0-100%)

注意: 请务必实际读取每个源代码文件进行分析，不要基于文件名猜测。
```

---

## 三、协作流程

### 3.1 开发流程

```
1. 分析评估师 → 全面审查代码，输出差距报告
2. 项目总指挥 → 根据差距报告制定修复计划
3. 并行启动Agent（最多3个同时）:
   - 后端架构师: 修复后端问题
   - 前端工程师: 修复前端问题
   - DevOps/推荐系统工程师: 修复基础设施问题
4. 各Agent完成后 → 分析评估师验证
5. 项目总指挥 → 打包交付
```

### 3.2 Agent并行策略

| 阶段 | Agent 1 (后端) | Agent 2 (前端) | Agent 3 (DevOps/推荐) |
|------|---------------|---------------|----------------------|
| 对标分析 | - | - | 分析评估师 |
| P0修复 | 后端P0阻塞 | 前端P0阻塞 | - |
| 功能补齐 | 后端功能 | 前端页面+组件 | 基础设施 |
| 体验优化 | 后端优化 | 前端优化 | K8s+监控 |
| 差异化 | 后端高级功能 | 前端高级功能 | DevOps上线准备 |

### 3.3 接口对接约定

```
后端定义 → API路径 + 请求DTO + 响应VO(Result<T>)
    ↓
前端对接 → constants/api.ts定义路径 + types/index.ts定义类型 + api/目录实现调用
    ↓
验证 → 检查路径一致、参数名一致、响应格式一致
```

### 3.4 数据库对接约定

```
数据库工程师定义 → schema.sql(表结构) + es-mapping.json(索引)
    ↓
后端架构师对接 → Entity(@TableName) + Mapper(XML/注解) + DTO
    ↓
推荐系统对接 → 表名一致、字段名一致、数据序列化JSON
```

---

## 四、项目技术栈总览

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | React Native | 0.86+ |
| | TypeScript | 5.x |
| | React | 19.x |
| | Zustand | 最新 |
| | @tanstack/react-query | 最新 |
| | React Navigation | v7 |
| | react-native-vector-icons | 10.2.0 |
| | react-native-fast-image | 最新 |
| | react-native-video | 6.10.0 |
| | react-native-image-picker | 最新 |
| | MMKV | 最新 |
| **后端** | Spring Boot | 3.2.5 |
| | Spring Cloud | 2023.0.1 |
| | Spring Cloud Alibaba | 2023.0.1.0 |
| | MyBatis-Plus | 3.5.6 |
| | MySQL | 8.x |
| | Redis | Cluster 6主6从 |
| | Elasticsearch | 8.x (IK分词器) |
| | JWT (jjwt) | 0.12.5 |
| | Kafka | 3.x |
| | Nacos | 2.x |
| **推荐** | Python | 3.11+ |
| | FastAPI | 最新 |
| | XGBoost | 最新 |
| | scikit-learn | 最新 |
| **DevOps** | Docker Compose | 2.x |
| | Kubernetes | 1.28+ |
| | Jenkins | 2.x |
| | Prometheus + Grafana | 最新 |
| | Nginx | 最新 |
| | Loki + Promtail | 最新 |
| | MinIO | 最新 |
| | Locust | 最新 |

---

## 五、项目目录结构

```
/workspace/
├── langtou-backend/                    # 后端微服务
│   ├── pom.xml                         # 父POM（版本管理）
│   ├── langtou-gateway/                # API网关 (8080)
│   ├── langtou-user-service/           # 用户服务 (8081)
│   ├── langtou-content-service/        # 内容服务 (8082)
│   ├── langtou-interact-service/      # 互动服务 (8083)
│   ├── langtou-message-service/        # 消息服务 (8084)
│   └── langtou-common/                 # 公共模块
├── langtou-mobile/                     # 前端React Native
│   └── LangtouMobile/
│       ├── src/
│       │   ├── screens/                 # 页面（14+个）
│       │   ├── components/             # 组件（13+个）
│       │   ├── hooks/                   # Hooks（25+个）
│       │   ├── api/                     # API层（10+文件）
│       │   ├── store/                   # 状态管理（4个Store）
│       │   ├── navigation/             # 导航配置
│       │   ├── constants/               # 常量
│       │   ├── types/                   # 类型定义
│       │   └── utils/                   # 工具函数
│       ├── android/                    # Android原生配置
│       └── ios/                        # iOS原生配置
├── langtou-database/                   # 数据库
│   ├── schema.sql                      # 表结构（11张核心表）
│   ├── data.sql                         # 测试数据
│   ├── es-mapping.json                 # ES索引
│   ├── redis-design.md                  # Redis缓存设计
│   └── flyway/migrations/               # 数据库迁移（V1-V4）
├── langtou-devops/                     # DevOps
│   ├── docker-compose.yml               # 生产环境编排（18+服务）
│   ├── docker-compose.dev.yml           # 开发环境
│   ├── k8s/                            # Kubernetes配置
│   │   ├── services/                    # 微服务Deployment
│   │   ├── statefulset/                 # 基础设施StatefulSet
│   │   ├── canary/                      # 金丝雀部署
│   │   ├── network/                     # 网络策略
│   │   └── rbac/                        # RBAC
│   ├── nginx/                          # Nginx配置
│   ├── monitoring/                     # 监控告警
│   ├── scripts/                        # 运维脚本
│   │   ├── backup/                      # 备份脚本
│   │   └── performance-test/            # 压测脚本
│   ├── security/                       # 安全审计
│   ├── docs/                           # 部署文档
│   └── Jenkinsfile                     # CI/CD流水线
├── langtou-recommendation/              # 推荐系统
│   └── recommendation-service/
│       ├── app/
│       │   ├── api/                     # API路由
│       │   ├── engine/                  # 召回/排序/重排引擎
│       │   ├── features/                # 特征工程
│       │   ├── models/                  # 离线模型
│       │   ├── data/                    # 数据客户端
│       │   └── consumer/                # Kafka消费者
│       ├── config/                      # 配置
│       ├── scripts/                     # 训练/评估脚本
│       └── tests/                       # 测试
├── DEVELOPMENT_GUIDELINES.md            # 开发规范
├── xiaohongshu-architecture/            # 架构设计文档
└── langtou-xhs-gap-analysis/             # 差距分析报告
```

---

## 六、版本迭代记录

| 版本 | 日期 | 阶段 | 主要内容 |
|------|------|------|---------|
| v1.0 | - | 架构设计 | 初始架构设计文档 |
| v2.0 | - | 团队开发 | 5个Agent初始开发 |
| v3.0 | - | 功能补全 | 对标修复第一轮 |
| v4.0 | - | 标准化 | 代码规范化整理 |
| v5.0 | - | 深度优化 | 完整性评估+优化 |
| v6.0 | 2026-06-11 | 四阶段完善 | P0修复+功能完整+体验优化+差异化竞争 |

---

## 七、评分演进

| 版本 | 评分 | 变化 |
|------|------|------|
| v5.0 | 58/100 | 基准 |
| v6.0 阶段1 | ~70/100 | +12 (P0阻塞修复) |
| v6.0 阶段2 | ~80/100 | +10 (功能完整) |
| v6.0 阶段3 | ~90/100 | +10 (体验优化) |
| v6.0 阶段4 | ~95/100 | +5 (差异化竞争) |
