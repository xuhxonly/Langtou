# 榔头(Langtou) - 运营活动后台方案设计

> **文档版本**: v1.0
> **所属Sprint**: Sprint 3
> **负责人**: 产品经理
> **创建日期**: 2026-06-12
> **状态**: 待评审

---

## 目录

1. [概述](#1-概述)
2. [话题挑战活动](#2-话题挑战活动)
3. [活动数据统计](#3-活动数据统计)
4. [官方账号体系](#4-官方账号体系)
5. [数据模型设计](#5-数据模型设计)
6. [API设计](#6-api设计)
7. [管理后台页面设计](#7-管理后台页面设计)
8. [用户端交互设计](#8-用户端交互设计)
9. [里程碑与排期](#9-里程碑与排期)

---

## 1. 概述

### 1.1 背景

运营活动是内容社区平台驱动用户活跃、内容产出和社区氛围建设的核心手段。榔头需要一套完整的运营活动后台系统，支持运营人员高效创建和管理话题挑战活动，并通过官方账号体系输出平台权威内容，维护社区规范。

### 1.2 目标

- **提升运营效率**：运营人员可在30分钟内完成一个话题挑战活动的创建和上线
- **数据驱动运营**：提供实时活动数据看板，支持数据化决策
- **规范化管理**：通过官方账号体系统一管理平台权威内容输出
- **激励内容创作**：通过活动奖励机制激励优质内容产出

### 1.3 功能范围

| 功能模块 | 优先级 | 使用端 | 说明 |
|---------|--------|-------|------|
| 话题挑战活动CRUD | P0 | 管理后台 | 活动创建、编辑、上下线 |
| 活动参与/退出 | P0 | 用户端 | 用户参与和退出活动 |
| 活动数据统计 | P0 | 管理后台 | 参与人数、笔记数、曝光、互动 |
| 活动排行榜 | P1 | 用户端 | 参与者排行展示 |
| 官方账号管理 | P1 | 管理后台 | 官方账号创建、认证、管理 |
| 官方内容推荐位 | P2 | 管理后台 | 官方内容在首页推荐位展示 |

---

## 2. 话题挑战活动

### 2.1 活动创建

**功能描述**：运营人员在管理后台创建话题挑战活动，配置活动基本信息、时间范围、参与规则和奖励机制。

**活动创建流程**：

```
管理后台 - 活动管理
  │
  ├─ Step 1: 基本信息
  │    ├─ 活动标题 (必填, 5-30字)
  │    ├─ 活动描述 (必填, 10-500字)
  │    ├─ 活动封面 (必填, 推荐尺寸 750x420px)
  │    ├─ 活动类型 (话题挑战/征稿活动/打卡活动)
  │    └─ 活动标签 (绑定话题标签, 1-5个)
  │
  ├─ Step 2: 时间设置
  │    ├─ 活动开始时间 (必填)
  │    ├─ 活动结束时间 (必填)
  │    ├─ 报名开始时间 (可选, 默认=活动开始时间)
  │    ├─ 报名截止时间 (可选, 默认=活动结束时间)
  │    └─ 结果公布时间 (可选)
  │
  ├─ Step 3: 参与条件
  │    ├─ 粉丝数要求 (无限制/100+/500+/1000+/5000+)
  │    ├─ 内容类型要求 (图文/视频/不限)
  │    ├─ 账号等级要求 (无限制/已实名/认证创作者)
  │    ├─ 参与次数限制 (不限/每人1次/每人3次/每人5次)
  │    └─ 内容审核要求 (自动审核/人工审核)
  │
  ├─ Step 4: 奖励机制
  │    ├─ 流量扶持
  │    │    ├─ 参与笔记额外曝光 (是/否)
  │    │    ├─ 额外曝光量 (500/1000/2000/5000)
  │    │    └─ 优质笔记首页推荐
  │    ├─ 积分奖励
  │    │    ├─ 参与奖励积分 (10/20/50/100)
  │    │    ├─ 优质内容额外积分 (50/100/200)
  │    │    └─ 积分发放时机 (参与时/活动结束后)
  │    └─ 实物奖励
  │         ├─ 奖品名称
  │         ├─ 奖品数量
  │         ├─ 获奖人数
  │         └─ 评选方式 (随机抽取/运营评选/用户投票)
  │
  └─ Step 5: 预览确认
       ├─ 活动预览页
       ├─ 活动详情页预览
       └─ [保存草稿] [提交审核] [直接发布]
```

**活动状态流转**：

```
                    ┌──────────┐
                    │  草稿    │
                    │ (draft)  │
                    └────┬─────┘
                         │ 提交审核
                    ┌────┴─────┐
                    │  待审核  │
                    │ (pending)│
                    └────┬─────┘
                    ┌────┴─────┐
               审核通过         审核驳回
            ┌────┴─────┐  ┌────┴─────┐
            │  已上线  │  │  已驳回  │
            │ (active) │  │(rejected)│
            └────┬─────┘  └────┬─────┘
                 │              │ 修改后重新提交
            ┌────┴─────┐       │
            │  已结束  │<──────┘
            │ (ended)  │
            └──────────┘

运营操作:
- 草稿 → 提交审核 / 编辑 / 删除
- 待审核 → 通过 / 驳回 / 撤回
- 已上线 → 手动结束 / 编辑(仅限非核心信息)
- 已结束 → 归档 / 重新开启
```

### 2.2 话题标签绑定

**功能描述**：每个活动绑定1-5个话题标签，用户发布笔记时携带这些标签即视为参与活动。

**绑定规则**：

| 规则 | 说明 |
|-----|------|
| 标签数量 | 1-5个 |
| 主标签 | 必须设置1个主标签（参与判定的核心标签） |
| 标签状态 | 绑定的标签必须为已存在的标签 |
| 标签互斥 | 同一时间段内，不同活动不可绑定相同主标签 |
| 标签创建 | 运营可在创建活动时同步创建新标签 |

**参与判定逻辑**：

```
用户发布笔记
  │
  ├─ 提取笔记标签列表
  │
  ├─ 查询当前进行中的活动
  │
  ├─ 匹配规则:
  │    ├─ 笔记标签包含活动的任一绑定标签 → 匹配成功
  │    ├─ 同一笔记可匹配多个活动 → 均视为参与
  │    └─ 笔记发布时间在活动时间范围内 → 有效参与
  │
  ├─ 检查参与条件
  │    ├─ 粉丝数是否满足
  │    ├─ 账号等级是否满足
  │    ├─ 参与次数是否超限
  │    └─ 内容类型是否满足
  │
  └─ 记录参与关系
       ├─ 写入 activity_participants 表
       ├─ 触发流量扶持（如配置）
       └─ 发放参与积分（如配置）
```

### 2.3 参与条件设置

**条件组合矩阵**：

| 条件类型 | 可选值 | 说明 |
|---------|--------|------|
| 粉丝数 | 无限制 / 100+ / 500+ / 1000+ / 5000+ | 按粉丝数筛选参与者 |
| 内容类型 | 图文 / 视频 / 不限 | 限制参与笔记的内容形式 |
| 账号等级 | 无限制 / 已实名 / 认证创作者 | 按账号认证等级筛选 |
| 参与次数 | 不限 / 1次 / 3次 / 5次 | 同一用户可参与的笔记数上限 |
| 内容审核 | 自动审核 / 人工审核 | 参与笔记是否需要人工审核 |

**条件校验流程**：

```
用户尝试参与活动
  │
  ├─ 检查活动状态 (必须为 active)
  ├─ 检查时间范围 (当前时间在活动时间内)
  ├─ 检查粉丝数 (user.follower_count >= rule.min_followers)
  ├─ 检查账号等级 (user.verification_level >= rule.min_level)
  ├─ 检查参与次数 (user.participated_count < rule.max_participations)
  └─ 检查内容类型 (note.type in rule.allowed_types)
```

### 2.4 奖励机制

#### 2.4.1 流量扶持

| 扶持类型 | 说明 | 配置项 |
|---------|------|--------|
| 额外曝光 | 参与笔记获得额外推荐流量 | 曝光增量: 500/1000/2000/5000 |
| 首页推荐 | 优质参与笔记在首页活动专区展示 | 推荐位数量: 1-10个 |
| 搜索加权 | 参与笔记在相关搜索中排名提升 | 加权系数: 1.1x / 1.2x / 1.5x |

**流量扶持发放逻辑**：

```
笔记参与活动
  │
  ├─ 基础扶持 (所有参与笔记)
  │    └─ 额外曝光量直接写入推荐系统权重
  │
  ├─ 优质扶持 (运营标记或算法评选)
  │    ├─ 首页推荐位展示
  │    ├─ 搜索排名加权
  │    └─ 推送通知推荐给关注该话题的用户
  │
  └─ 扶持记录
       ├─ 记录扶持类型和数量
       └─ 用于活动效果统计
```

#### 2.4.2 积分奖励

| 奖励场景 | 积分数量 | 发放时机 |
|---------|---------|---------|
| 参与活动 | 10/20/50/100 | 笔记发布且审核通过后即时发放 |
| 优质内容 | 50/100/200 | 运营标记或活动结束后统一发放 |
| 获奖奖励 | 自定义 | 活动结束后统一发放 |

#### 2.4.3 实物奖励

| 配置项 | 说明 |
|-------|------|
| 奖品名称 | 奖品描述（如"榔头定制帆布袋"） |
| 奖品图片 | 奖品展示图 |
| 奖品数量 | 可发放的奖品总数 |
| 获奖人数 | 实际获奖人数（<= 奖品数量） |
| 评选方式 | 随机抽取 / 运营评选 / 用户投票 |
| 领奖方式 | 填写收货地址 / 线下领取 / 虚拟兑换码 |

---

## 3. 活动数据统计

### 3.1 核心指标

| 指标类别 | 指标名称 | 定义 | 数据来源 |
|---------|---------|------|---------|
| 参与 | 参与人数 | 参与活动的独立用户数 | activity_participants |
| 参与 | 参与笔记数 | 活动关联的笔记总数 | activity_participants |
| 参与 | 日均新增参与 | 每日新增参与人数/笔记数 | activity_participants |
| 曝光 | 总曝光量 | 活动页面及参与笔记的总曝光 | 推荐系统 + 埋点 |
| 曝光 | 活动页PV | 活动详情页的页面浏览量 | 埋点 |
| 曝光 | 活动页UV | 活动详情页的独立访客数 | 埋点 |
| 互动 | 总互动量 | 参与笔记的点赞+评论+收藏总数 | 埋点 |
| 互动 | 平均互动率 | 总互动量 / 参与笔记数 | 计算 |
| 质量 | 优质内容数 | 运营标记为优质的内容数 | 运营标记 |
| 质量 | 笔记通过率 | 审核通过数 / 总提交数 | 审核系统 |

### 3.2 参与者排行

**排行维度**：

| 排行类型 | 排序依据 | 展示数量 |
|---------|---------|---------|
| 热度排行 | 笔记互动量（点赞+评论+收藏） | Top 50 |
| 参与排行 | 参与笔记数 | Top 50 |
| 最新参与 | 参与时间（倒序） | 最新 50 |

**排行榜数据结构**：

```json
{
  "rankings": {
    "by_interaction": [
      {
        "rank": 1,
        "user_id": "user_001",
        "nickname": "咖啡爱好者小王",
        "avatar": "https://cdn.langtou.com/avatar/user_001.jpg",
        "verified": true,
        "note_count": 5,
        "total_interactions": 2340,
        "top_note": {
          "note_id": "note_101",
          "title": "胡同咖啡探店合集",
          "cover": "https://cdn.langtou.com/note/cover_101.jpg",
          "likes": 890,
          "comments": 120,
          "collects": 340
        }
      }
    ],
    "by_participation": [...],
    "latest": [...]
  }
}
```

### 3.3 活动趋势图

**趋势维度**：

| 图表类型 | X轴 | Y轴 | 用途 |
|---------|-----|-----|------|
| 参与趋势 | 日期 | 日新增参与人数 | 观察活动热度变化 |
| 笔记趋势 | 日期 | 日新增笔记数 | 观察内容产出节奏 |
| 互动趋势 | 日期 | 日互动量 | 观察用户互动活跃度 |
| 曝光趋势 | 日期 | 日曝光量 | 观察流量分发效果 |

**趋势数据API输出**：

```json
{
  "trends": {
    "participation": {
      "period": "daily",
      "data": [
        {"date": "2026-06-01", "new_users": 120, "new_notes": 180},
        {"date": "2026-06-02", "new_users": 150, "new_notes": 220},
        {"date": "2026-06-03", "new_users": 200, "new_notes": 310}
      ]
    },
    "interaction": {
      "period": "daily",
      "data": [
        {"date": "2026-06-01", "likes": 1500, "comments": 300, "collects": 800},
        {"date": "2026-06-02", "likes": 2100, "comments": 450, "collects": 1100}
      ]
    },
    "exposure": {
      "period": "daily",
      "data": [
        {"date": "2026-06-01", "page_pv": 5000, "page_uv": 3200, "note_exposure": 50000},
        {"date": "2026-06-02", "page_pv": 6200, "page_uv": 4100, "note_exposure": 65000}
      ]
    }
  }
}
```

### 3.4 数据看板设计

**管理后台活动数据看板布局**：

```
┌─────────────────────────────────────────────────────────────┐
│  活动名称: 「夏日穿搭挑战」          状态: 进行中           │
│  活动时间: 2026-06-01 ~ 2026-06-30                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ 参与人数  │ │ 参与笔记  │ │ 总曝光量  │ │ 总互动量  │      │
│  │  3,256   │ │  5,120   │ │  890,000 │ │  45,600  │      │
│  │ +12% vs昨日│ │ +15% vs昨日│ │ +8% vs昨日│ │ +20% vs昨日│  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  参与趋势 (折线图)                                    │  │
│  │  ─── 新增参与人数  ─── 新增笔记数                     │  │
│  │  [日] [周] [月]                                      │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  参与者排行榜 (Tab: 热度 | 笔记数 | 最新)            │  │
│  │  1. 咖啡爱好者小王  5篇笔记  2340互动               │  │
│  │  2. 穿搭博主小美    4篇笔记  1890互动               │  │
│  │  3. 生活方式达人    3篇笔记  1560互动               │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────┐ ┌──────────────────────────┐  │
│  │  互动构成 (饼图)      │ │  内容类型分布 (柱状图)     │  │
│  │  点赞: 60%            │ │  图文: 75%                │  │
│  │  评论: 20%            │ │  视频: 25%                │  │
│  │  收藏: 20%            │ │                           │  │
│  └──────────────────────┘ └──────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 官方账号体系

### 4.1 官方账号标识

**蓝V认证标识**：

| 元素 | 说明 |
|-----|------|
| 认证标识 | 用户名旁蓝色"V"图标 |
| 认证类型 | 官方账号 / 机构账号 / 品牌账号 |
| 认证描述 | 鼠标悬浮/点击显示认证信息 |
| 认证信息 | "榔头官方认证账号" / "XX品牌官方账号" |

**官方账号类型**：

| 账号类型 | 用途 | 示例 |
|---------|------|------|
| 平台官方 | 平台公告、社区规范 | @榔头官方 |
| 内容推荐 | 官方内容推荐和精选 | @榔头精选 |
| 活动运营 | 活动通知和互动 | @榔头活动 |
| 社区治理 | 社区规范和举报反馈 | @榔头社区小助手 |
| 垂直领域 | 官方垂类内容运营 | @榔头美食 / @榔头旅行 |

### 4.2 官方内容推荐位

**推荐位类型**：

| 推荐位 | 位置 | 展示内容 | 更新频率 |
|-------|------|---------|---------|
| 首页Banner | 首页顶部轮播 | 活动推广、重要公告 | 运营配置 |
| 发现页推荐 | 发现页顶部 | 官方精选内容 | 每日更新 |
| 话题页置顶 | 话题详情页顶部 | 官方话题引导内容 | 按需更新 |
| 搜索推荐 | 搜索结果页顶部 | 官方推荐内容 | 实时 |
| 新人引导 | 新用户首页 | 新人引导内容 | 固定 |

**推荐位管理功能**：

```
推荐位管理
  │
  ├─ 推荐位列表
  │    ├─ 首页Banner (3个轮播位)
  │    ├─ 发现页推荐 (5个推荐位)
  │    ├─ 话题页置顶 (按话题)
  │    └─ 搜索推荐 (10个推荐位)
  │
  ├─ 内容配置
  │    ├─ 选择推荐内容 (笔记/活动/外部链接)
  │    ├─ 设置展示时间范围
  │    ├─ 设置展示优先级/排序
  │    ├─ 设置目标人群 (全部/新用户/特定标签用户)
  │    └─ 设置跳转目标
  │
  └─ 效果监控
       ├─ 展示次数
       ├─ 点击次数
       ├─ 点击率
       └─ 转化数据
```

### 4.3 社区规范/新人引导内容管理

**内容管理功能**：

| 功能 | 说明 |
|-----|------|
| 社区规范 | 创建和管理社区规范文档，支持富文本编辑 |
| 新人引导 | 创建新人引导内容，按步骤配置引导流程 |
| 公告管理 | 发布和管理平台公告，支持定时发布 |
| 帮助中心 | 创建FAQ和帮助文档 |

**社区规范内容结构**：

```
社区规范
  │
  ├─ 总则
  │    ├─ 社区宗旨
  │    └─ 基本原则
  │
  ├─ 内容规范
  │    ├─ 鼓励发布的内容
  │    ├─ 禁止发布的内容
  │    └─ 内容质量标准
  │
  ├─ 行为规范
  │    ├─ 互动行为准则
  │    ├─ 禁止的行为
  │    └─ 举报与处理
  │
  └─ 处罚规则
       ├─ 违规等级划分
       ├─ 处罚措施
       └─ 申诉流程
```

---

## 5. 数据模型设计

### 5.1 activities 表

活动主表，存储活动基本信息。

```sql
CREATE TABLE activities (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    title               VARCHAR(64) NOT NULL COMMENT '活动标题',
    description         TEXT NOT NULL COMMENT '活动描述',
    cover_url           VARCHAR(512) NOT NULL COMMENT '活动封面图URL',
    activity_type       VARCHAR(32) NOT NULL DEFAULT 'topic_challenge' COMMENT '活动类型: topic_challenge/call_for_submission/check_in',
    status              VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/pending/active/ended/rejected/archived',
    creator_id          BIGINT NOT NULL COMMENT '创建人(运营人员)ID',
    official_account_id BIGINT DEFAULT NULL COMMENT '关联官方账号ID',

    -- 时间设置
    start_time          DATETIME NOT NULL COMMENT '活动开始时间',
    end_time            DATETIME NOT NULL COMMENT '活动结束时间',
    enroll_start_time   DATETIME DEFAULT NULL COMMENT '报名开始时间',
    enroll_end_time     DATETIME DEFAULT NULL COMMENT '报名截止时间',
    result_announce_time DATETIME DEFAULT NULL COMMENT '结果公布时间',

    -- 参与条件
    min_followers       INT NOT NULL DEFAULT 0 COMMENT '最低粉丝数要求',
    allowed_content_types VARCHAR(64) NOT NULL DEFAULT 'all' COMMENT '允许的内容类型: all/image/video',
    min_account_level   VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT '最低账号等级: none/verified/creator',
    max_participations  INT NOT NULL DEFAULT 0 COMMENT '最大参与次数(0=不限)',
    review_mode         VARCHAR(16) NOT NULL DEFAULT 'auto' COMMENT '审核模式: auto/manual',

    -- 奖励配置
    reward_config       JSON DEFAULT NULL COMMENT '奖励配置(JSON)',
    /*
    reward_config 示例:
    {
      "traffic_boost": {
        "enabled": true,
        "extra_exposure": 1000,
        "homepage_recommend": true,
        "search_weight": 1.2
      },
      "points": {
        "participation_points": 20,
        "quality_points": 100,
        "distribution_timing": "immediate"
      },
      "prize": {
        "name": "榔头定制帆布袋",
        "image": "https://cdn.langtou.com/prize/bag.jpg",
        "quantity": 100,
        "winner_count": 10,
        "selection_method": "operation_pick"
      }
    }
    */

    -- 统计数据
    participant_count   INT NOT NULL DEFAULT 0 COMMENT '参与人数(冗余)',
    note_count          INT NOT NULL DEFAULT 0 COMMENT '参与笔记数(冗余)',
    total_exposure      BIGINT NOT NULL DEFAULT 0 COMMENT '总曝光量(冗余)',
    total_interactions  BIGINT NOT NULL DEFAULT 0 COMMENT '总互动量(冗余)',

    -- 审核信息
    reviewer_id         BIGINT DEFAULT NULL COMMENT '审核人ID',
    review_comment      VARCHAR(256) DEFAULT NULL COMMENT '审核意见',
    reviewed_at         DATETIME DEFAULT NULL COMMENT '审核时间',

    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_creator (creator_id),
    INDEX idx_official_account (official_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';
```

### 5.2 activity_participants 表

活动参与记录表。

```sql
CREATE TABLE activity_participants (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    user_id         BIGINT NOT NULL COMMENT '参与用户ID',
    note_id         BIGINT NOT NULL COMMENT '参与笔记ID',
    status          VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/disqualified/removed',
    is_quality      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否优质内容',
    is_winner       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否获奖',
    prize_id        BIGINT DEFAULT NULL COMMENT '获奖奖品ID',
    traffic_boosted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发放流量扶持',
    points_awarded  INT NOT NULL DEFAULT 0 COMMENT '已发放积分',
    participated_at  DATETIME NOT NULL COMMENT '参与时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_activity_note (activity_id, note_id),
    INDEX idx_activity_user (activity_id, user_id),
    INDEX idx_user (user_id),
    INDEX idx_activity_status (activity_id, status),
    INDEX idx_is_quality (activity_id, is_quality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录表';
```

### 5.3 activity_rules 表

活动参与规则表，支持灵活的规则配置。

```sql
CREATE TABLE activity_rules (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    rule_type       VARCHAR(32) NOT NULL COMMENT '规则类型: follower_count/content_type/account_level/participation_limit/review_mode',
    rule_key        VARCHAR(64) NOT NULL COMMENT '规则Key',
    rule_value      VARCHAR(256) NOT NULL COMMENT '规则值',
    rule_description VARCHAR(256) DEFAULT NULL COMMENT '规则描述(面向用户展示)',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_activity (activity_id),
    UNIQUE KEY uk_activity_type (activity_id, rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动规则表';
```

**规则数据示例**：

```sql
-- 活动 #1001 的参与规则
INSERT INTO activity_rules (activity_id, rule_type, rule_key, rule_value, rule_description, sort_order) VALUES
(1001, 'follower_count', 'min_followers', '0', '无粉丝数限制', 1),
(1001, 'content_type', 'allowed_types', 'all', '图文和视频均可参与', 2),
(1001, 'account_level', 'min_level', 'none', '所有用户均可参与', 3),
(1001, 'participation_limit', 'max_notes', '3', '每人最多参与3篇笔记', 4),
(1001, 'review_mode', 'mode', 'auto', '参与笔记自动审核', 5);
```

### 5.4 activity_tags 表

活动与话题标签的绑定关系表。

```sql
CREATE TABLE activity_tags (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    tag_id          BIGINT NOT NULL COMMENT '话题标签ID',
    tag_name        VARCHAR(32) NOT NULL COMMENT '标签名称(冗余)',
    is_primary      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主标签',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_activity_tag (activity_id, tag_id),
    INDEX idx_tag (tag_id),
    INDEX idx_activity_primary (activity_id, is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动标签绑定表';
```

### 5.5 official_accounts 表

官方账号管理表。

```sql
CREATE TABLE official_accounts (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_name    VARCHAR(64) NOT NULL COMMENT '账号名称',
    account_type    VARCHAR(32) NOT NULL COMMENT '账号类型: platform/content/activity/community/vertical',
    user_id         BIGINT NOT NULL COMMENT '关联的用户ID',
    description     VARCHAR(256) DEFAULT NULL COMMENT '账号简介',
    avatar_url      VARCHAR(512) NOT NULL COMMENT '头像URL',
    cover_url       VARCHAR(512) DEFAULT NULL COMMENT '背景图URL',
    verify_type     VARCHAR(32) NOT NULL COMMENT '认证类型: official/institution/brand',
    verify_info     VARCHAR(128) NOT NULL COMMENT '认证信息(如"榔头官方认证账号")',
    status          VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled',
    permissions     JSON DEFAULT NULL COMMENT '权限配置',
    /*
    permissions 示例:
    {
      "can_publish_note": true,
      "can_manage_activity": true,
      "can_manage_recommend": true,
      "can_manage_announcement": true,
      "can_pin_topic": true
    }
    */
    managed_by      BIGINT NOT NULL COMMENT '管理运营人员ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user (user_id),
    UNIQUE KEY uk_name (account_name),
    INDEX idx_type (account_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官方账号表';
```

### 5.6 recommend_positions 表

推荐位管理表。

```sql
CREATE TABLE recommend_positions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_key    VARCHAR(64) NOT NULL COMMENT '推荐位标识: home_banner/discover_top/topic_pin/search_top',
    position_name   VARCHAR(64) NOT NULL COMMENT '推荐位名称',
    content_type    VARCHAR(32) NOT NULL COMMENT '内容类型: note/activity/link/announcement',
    content_id      BIGINT DEFAULT NULL COMMENT '内容ID',
    title           VARCHAR(128) DEFAULT NULL COMMENT '展示标题',
    image_url       VARCHAR(512) DEFAULT NULL COMMENT '展示图片',
    link_url        VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
    target_user     VARCHAR(32) NOT NULL DEFAULT 'all' COMMENT '目标人群: all/new_user/tag_user',
    target_tags     JSON DEFAULT NULL COMMENT '目标标签(当target_user=tag_user时)',
    priority        INT NOT NULL DEFAULT 0 COMMENT '优先级(越大越靠前)',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序',
    start_time      DATETIME DEFAULT NULL COMMENT '展示开始时间',
    end_time        DATETIME DEFAULT NULL COMMENT '展示结束时间',
    status          VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/paused/expired',
    impression_count BIGINT NOT NULL DEFAULT 0 COMMENT '展示次数',
    click_count     BIGINT NOT NULL DEFAULT 0 COMMENT '点击次数',
    created_by      BIGINT NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_position_status (position_key, status, priority DESC),
    INDEX idx_time (start_time, end_time),
    INDEX idx_content (content_type, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐位管理表';
```

---

## 6. API设计

### 6.1 活动管理API（管理员端）

#### 6.1.1 创建活动

**POST /api/v1/admin/activities**

创建新的话题挑战活动。

**Request Headers**:
```
Authorization: Bearer {admin_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "夏日穿搭挑战",
  "description": "分享你的夏日穿搭灵感，展示你的时尚态度！参与即有机会获得流量扶持和精美奖品。",
  "cover_url": "https://cdn.langtou.com/activity/summer_outfit.jpg",
  "activity_type": "topic_challenge",
  "start_time": "2026-06-15T00:00:00Z",
  "end_time": "2026-06-30T23:59:59Z",
  "enroll_start_time": "2026-06-15T00:00:00Z",
  "enroll_end_time": "2026-06-28T23:59:59Z",
  "result_announce_time": "2026-07-05T12:00:00Z",
  "tags": [
    {"tag_id": 101, "tag_name": "夏日穿搭", "is_primary": true},
    {"tag_id": 102, "tag_name": "穿搭分享", "is_primary": false},
    {"tag_id": 103, "tag_name": "时尚达人", "is_primary": false}
  ],
  "rules": [
    {"rule_type": "follower_count", "rule_key": "min_followers", "rule_value": "0", "rule_description": "无粉丝数限制"},
    {"rule_type": "content_type", "rule_key": "allowed_types", "rule_value": "all", "rule_description": "图文和视频均可参与"},
    {"rule_type": "account_level", "rule_key": "min_level", "rule_value": "none", "rule_description": "所有用户均可参与"},
    {"rule_type": "participation_limit", "rule_key": "max_notes", "rule_value": "5", "rule_description": "每人最多参与5篇笔记"},
    {"rule_type": "review_mode", "rule_key": "mode", "rule_value": "auto", "rule_description": "参与笔记自动审核"}
  ],
  "reward_config": {
    "traffic_boost": {
      "enabled": true,
      "extra_exposure": 1000,
      "homepage_recommend": true,
      "search_weight": 1.2
    },
    "points": {
      "participation_points": 20,
      "quality_points": 100,
      "distribution_timing": "immediate"
    },
    "prize": {
      "name": "榔头定制夏日礼包",
      "image": "https://cdn.langtou.com/prize/summer_pack.jpg",
      "quantity": 100,
      "winner_count": 10,
      "selection_method": "operation_pick"
    }
  },
  "official_account_id": 1
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "activity_id": 1001,
    "status": "draft",
    "created_at": "2026-06-12T10:00:00Z"
  }
}
```

---

#### 6.1.2 获取活动详情（管理员）

**GET /api/v1/admin/activities/{activity_id}**

获取活动完整信息（含统计数据）。

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "activity_id": 1001,
    "title": "夏日穿搭挑战",
    "description": "分享你的夏日穿搭灵感...",
    "cover_url": "https://cdn.langtou.com/activity/summer_outfit.jpg",
    "activity_type": "topic_challenge",
    "status": "active",
    "start_time": "2026-06-15T00:00:00Z",
    "end_time": "2026-06-30T23:59:59Z",
    "tags": [
      {"tag_id": 101, "tag_name": "夏日穿搭", "is_primary": true},
      {"tag_id": 102, "tag_name": "穿搭分享", "is_primary": false}
    ],
    "rules": [...],
    "reward_config": {...},
    "statistics": {
      "participant_count": 3256,
      "note_count": 5120,
      "total_exposure": 890000,
      "total_interactions": 45600,
      "quality_note_count": 128,
      "avg_interaction_rate": 8.9
    },
    "created_at": "2026-06-12T10:00:00Z",
    "updated_at": "2026-06-15T00:00:00Z"
  }
}
```

---

#### 6.1.3 更新活动

**PUT /api/v1/admin/activities/{activity_id}**

更新活动信息。活动上线后仅允许修改非核心信息（描述、封面、结束时间延长）。

**Request Body**:
```json
{
  "description": "更新后的活动描述...",
  "cover_url": "https://cdn.langtou.com/activity/summer_outfit_v2.jpg",
  "end_time": "2026-07-07T23:59:59Z"
}
```

---

#### 6.1.4 活动列表（管理员）

**GET /api/v1/admin/activities**

分页查询活动列表。

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| status | String | 否 | 按状态筛选 |
| activity_type | String | 否 | 按类型筛选 |
| keyword | String | 否 | 按标题搜索 |
| page | Integer | 否 | 页码，默认1 |
| page_size | Integer | 否 | 每页数量，默认20 |

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 42,
    "page": 1,
    "page_size": 20,
    "items": [
      {
        "activity_id": 1001,
        "title": "夏日穿搭挑战",
        "cover_url": "https://cdn.langtou.com/activity/summer_outfit.jpg",
        "status": "active",
        "activity_type": "topic_challenge",
        "start_time": "2026-06-15T00:00:00Z",
        "end_time": "2026-06-30T23:59:59Z",
        "participant_count": 3256,
        "note_count": 5120,
        "created_at": "2026-06-12T10:00:00Z"
      }
    ]
  }
}
```

---

#### 6.1.5 活动审核

**POST /api/v1/admin/activities/{activity_id}/review**

审核活动（通过或驳回）。

**Request Body**:
```json
{
  "action": "approve",
  "comment": "活动内容审核通过，可以上线"
}
```

| action值 | 说明 |
|---------|------|
| approve | 审核通过，活动状态变为 active |
| reject | 审核驳回，活动状态变为 rejected |

---

#### 6.1.6 活动数据统计

**GET /api/v1/admin/activities/{activity_id}/statistics**

获取活动详细统计数据。

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| period | String | 否 | 统计周期: daily/weekly/monthly |
| start_date | String | 否 | 统计开始日期 |
| end_date | String | 否 | 统计结束日期 |

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "overview": {
      "participant_count": 3256,
      "note_count": 5120,
      "total_exposure": 890000,
      "total_interactions": 45600,
      "quality_note_count": 128,
      "avg_interaction_rate": 8.9,
      "pass_rate": 95.2
    },
    "trends": {
      "participation": [
        {"date": "2026-06-15", "new_users": 120, "new_notes": 180},
        {"date": "2026-06-16", "new_users": 150, "new_notes": 220}
      ],
      "interaction": [
        {"date": "2026-06-15", "likes": 1500, "comments": 300, "collects": 800}
      ],
      "exposure": [
        {"date": "2026-06-15", "page_pv": 5000, "page_uv": 3200, "note_exposure": 50000}
      ]
    },
    "content_type_distribution": {
      "image": 3840,
      "video": 1280
    }
  }
}
```

---

### 6.2 活动API（用户端）

#### 6.2.1 活动列表（用户端）

**GET /api/v1/activities**

获取当前可参与的活动列表。

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| status | String | 否 | 筛选状态: active/ended |
| type | String | 否 | 筛选类型 |
| page | Integer | 否 | 页码，默认1 |
| page_size | Integer | 否 | 每页数量，默认10 |

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 8,
    "page": 1,
    "page_size": 10,
    "items": [
      {
        "activity_id": 1001,
        "title": "夏日穿搭挑战",
        "description": "分享你的夏日穿搭灵感，展示你的时尚态度！",
        "cover_url": "https://cdn.langtou.com/activity/summer_outfit.jpg",
        "activity_type": "topic_challenge",
        "status": "active",
        "start_time": "2026-06-15T00:00:00Z",
        "end_time": "2026-06-30T23:59:59Z",
        "tags": ["夏日穿搭", "穿搭分享", "时尚达人"],
        "participant_count": 3256,
        "note_count": 5120,
        "has_prize": true,
        "prize_preview": {
          "name": "榔头定制夏日礼包",
          "image": "https://cdn.langtou.com/prize/summer_pack.jpg"
        },
        "user_participated": false,
        "user_can_participate": true
      }
    ]
  }
}
```

---

#### 6.2.2 活动详情（用户端）

**GET /api/v1/activities/{activity_id}**

获取活动详情页展示信息。

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "activity_id": 1001,
    "title": "夏日穿搭挑战",
    "description": "分享你的夏日穿搭灵感，展示你的时尚态度！参与即有机会获得流量扶持和精美奖品。",
    "cover_url": "https://cdn.langtou.com/activity/summer_outfit.jpg",
    "activity_type": "topic_challenge",
    "status": "active",
    "start_time": "2026-06-15T00:00:00Z",
    "end_time": "2026-06-30T23:59:59Z",
    "tags": [
      {"tag_name": "夏日穿搭", "is_primary": true},
      {"tag_name": "穿搭分享", "is_primary": false},
      {"tag_name": "时尚达人", "is_primary": false}
    ],
    "rules": [
      "所有用户均可参与",
      "图文和视频均可参与",
      "每人最多参与5篇笔记",
      "笔记需携带 #夏日穿搭 标签"
    ],
    "rewards": {
      "traffic_boost": "参与笔记额外获得1000曝光",
      "points": "参与即可获得20积分",
      "prize": {
        "name": "榔头定制夏日礼包",
        "image": "https://cdn.langtou.com/prize/summer_pack.jpg",
        "winner_count": 10,
        "selection_method": "运营评选"
      }
    },
    "statistics": {
      "participant_count": 3256,
      "note_count": 5120
    },
    "user_status": {
      "participated": false,
      "participation_count": 0,
      "can_participate": true,
      "cannot_reason": null
    },
    "official_account": {
      "account_name": "榔头活动",
      "avatar_url": "https://cdn.langtou.com/avatar/official_activity.jpg",
      "verified": true
    }
  }
}
```

---

#### 6.2.3 参与活动

**POST /api/v1/activities/{activity_id}/participate**

用户通过发布笔记参与活动（笔记需携带活动标签）。

**Request Body**:
```json
{
  "note_id": 2001
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "participation_id": 3001,
    "activity_id": 1001,
    "note_id": 2001,
    "status": "active",
    "traffic_boosted": true,
    "points_awarded": 20,
    "participated_at": "2026-06-15T10:30:00Z"
  }
}
```

**Error Codes**:
| HTTP Status | Code | 说明 |
|------------|------|------|
| 400 | 30001 | 活动不存在或已结束 |
| 400 | 30002 | 笔记未携带活动标签 |
| 400 | 30003 | 不满足参与条件（粉丝数/等级等） |
| 400 | 30004 | 参与次数已达上限 |
| 409 | 30005 | 该笔记已参与此活动 |

---

#### 6.2.4 退出活动

**DELETE /api/v1/activities/{activity_id}/participate**

用户取消笔记参与活动。

**Request Body**:
```json
{
  "note_id": 2001
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

#### 6.2.5 活动排行榜

**GET /api/v1/activities/{activity_id}/rankings**

获取活动参与者排行榜。

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| type | String | 否 | 排行类型: interaction/participation/latest，默认interaction |
| page | Integer | 否 | 页码，默认1 |
| page_size | Integer | 否 | 每页数量，默认20 |

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "activity_id": 1001,
    "ranking_type": "interaction",
    "total": 3256,
    "page": 1,
    "page_size": 20,
    "items": [
      {
        "rank": 1,
        "user_id": "user_001",
        "nickname": "穿搭博主小美",
        "avatar": "https://cdn.langtou.com/avatar/user_001.jpg",
        "verified": true,
        "follower_count": 12500,
        "note_count": 5,
        "total_interactions": 2340,
        "top_note": {
          "note_id": "note_101",
          "title": "夏日清凉穿搭合集",
          "cover": "https://cdn.langtou.com/note/cover_101.jpg",
          "likes": 890,
          "comments": 120,
          "collects": 340
        }
      }
    ]
  }
}
```

---

### 6.3 官方账号管理API（管理员端）

#### 6.3.1 创建官方账号

**POST /api/v1/admin/official-accounts**

**Request Body**:
```json
{
  "account_name": "榔头精选",
  "account_type": "content",
  "user_id": 9001,
  "description": "榔头官方精选内容账号，为你推荐社区最优质的内容",
  "avatar_url": "https://cdn.langtou.com/avatar/official_pick.jpg",
  "cover_url": "https://cdn.langtou.com/cover/official_pick.jpg",
  "verify_type": "official",
  "verify_info": "榔头官方认证账号",
  "permissions": {
    "can_publish_note": true,
    "can_manage_activity": false,
    "can_manage_recommend": true,
    "can_manage_announcement": true,
    "can_pin_topic": true
  },
  "managed_by": 1001
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "account_id": 1,
    "status": "active",
    "created_at": "2026-06-12T10:00:00Z"
  }
}
```

---

#### 6.3.2 官方账号列表

**GET /api/v1/admin/official-accounts**

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "account_id": 1,
        "account_name": "榔头官方",
        "account_type": "platform",
        "user_id": 9001,
        "description": "榔头平台官方账号",
        "avatar_url": "https://cdn.langtou.com/avatar/official.jpg",
        "verify_type": "official",
        "verify_info": "榔头官方认证账号",
        "status": "active",
        "managed_by": 1001,
        "created_at": "2026-06-01T00:00:00Z"
      }
    ]
  }
}
```

---

#### 6.3.3 更新官方账号

**PUT /api/v1/admin/official-accounts/{account_id}**

更新官方账号信息。

---

#### 6.3.4 推荐位管理

**POST /api/v1/admin/recommend-positions**

创建推荐位内容。

**Request Body**:
```json
{
  "position_key": "home_banner",
  "position_name": "首页Banner",
  "content_type": "activity",
  "content_id": 1001,
  "title": "夏日穿搭挑战火热进行中",
  "image_url": "https://cdn.langtou.com/banner/summer_outfit.jpg",
  "link_url": "langtou://activity/1001",
  "target_user": "all",
  "priority": 10,
  "sort_order": 1,
  "start_time": "2026-06-15T00:00:00Z",
  "end_time": "2026-06-30T23:59:59Z"
}
```

**GET /api/v1/admin/recommend-positions**

获取推荐位列表。

**PUT /api/v1/admin/recommend-positions/{position_id}**

更新推荐位配置。

**DELETE /api/v1/admin/recommend-positions/{position_id}**

删除推荐位内容。

---

## 7. 管理后台页面设计

### 7.1 活动管理页面

```
┌─────────────────────────────────────────────────────────────┐
│  运营管理后台 > 活动管理                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [+ 创建活动]   [筛选: 全部状态 v]  [搜索活动名称...]       │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 活动列表                                            │    │
│  │ ┌─────┬──────────┬──────┬──────┬────┬──────┬─────┐ │    │
│  │ │ ID  │ 标题     │ 类型  │ 状态  │参与 │ 笔记 │ 操作 │ │    │
│  │ ├─────┼──────────┼──────┼──────┼────┼──────┼─────┤ │    │
│  │ │1001 │夏日穿搭   │话题  │进行中│3256│ 5120 │详情 │ │    │
│  │ │1002 │美食探店   │征稿  │待审核│  - │   -  │审核 │ │    │
│  │ │1003 │读书打卡   │打卡  │草稿  │  - │   -  │编辑 │ │    │
│  │ │1004 │春日摄影   │话题  │已结束│2100│ 3800 │数据 │ │    │
│  │ └─────┴──────────┴──────┴──────┴────┴──────┴─────┘ │    │
│  │                                                     │    │
│  │  < 1 2 3 4 5 ... 共42条 >                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 活动数据看板页面

```
┌─────────────────────────────────────────────────────────────┐
│  运营管理后台 > 活动管理 > 夏日穿搭挑战 - 数据看板            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [返回列表]  [导出报表]  [编辑活动]                          │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ 参与人数  │ │ 参与笔记  │ │ 总曝光量  │ │ 总互动量  │      │
│  │  3,256   │ │  5,120   │ │  890K    │ │  45.6K   │      │
│  │ +12%     │ │ +15%     │ │ +8%      │ │ +20%     │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  参与趋势                    [日 v] [周] [月]         │  │
│  │  📈 (折线图)                                        │  │
│  │  ─── 新增参与人数  ─── 新增笔记数                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  参与者排行  [热度 v] [笔记数] [最新]                 │  │
│  │  1. 穿搭博主小美  5篇  2340互动  [标记优质] [取消参与] │  │
│  │  2. 时尚达人Lisa  4篇  1890互动  [标记优质] [取消参与] │  │
│  │  3. 生活美学家    3篇  1560互动  [标记优质] [取消参与] │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────┐ ┌──────────────────────────┐  │
│  │  互动构成 (饼图)      │ │  内容类型 (柱状图)         │  │
│  │  点赞: 60%  评论: 20% │ │  图文: 75%  视频: 25%     │  │
│  │  收藏: 20%            │ │                          │  │
│  └──────────────────────┘ └──────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.3 官方账号管理页面

```
┌─────────────────────────────────────────────────────────────┐
│  运营管理后台 > 官方账号管理                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [+ 创建官方账号]                                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ ┌──────┬──────────┬──────┬──────┬──────┬──────┐   │    │
│  │ │ 头像  │ 账号名称  │ 类型  │ 认证  │ 状态  │ 操作  │   │    │
│  │ ├──────┼──────────┼──────┼──────┼──────┼──────┤   │    │
│  │ │ [V]  │ 榔头官方  │ 平台  │ 蓝V  │ 正常  │ 管理  │   │    │
│  │ │ [V]  │ 榔头精选  │ 内容  │ 蓝V  │ 正常  │ 管理  │   │    │
│  │ │ [V]  │ 榔头活动  │ 活动  │ 蓝V  │ 正常  │ 管理  │   │    │
│  │ │ [V]  │ 社区助手  │ 社区  │ 蓝V  │ 正常  │ 管理  │   │    │
│  │ └──────┴──────────┴──────┴──────┴──────┴──────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 用户端交互设计

### 8.1 活动发现页

```
┌─────────────────────────────────────────────────────────────┐
│  榔头 App - 活动中心                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  🔥 热门活动                                         │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │ [活动封面图 - 夏日穿搭挑战]                    │    │    │
│  │  │                                              │    │    │
│  │  │ 夏日穿搭挑战                                  │    │    │
│  │  │ 分享你的夏日穿搭灵感                           │    │    │
│  │  │ 3,256人参与 | 5,120篇笔记 | 进行中            │    │    │
│  │  │ [立即参与]                                    │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  │                                                      │    │
│  │  ┌──────────┐ ┌──────────┐                          │    │
│  │  │ [封面图]  │ │ [封面图]  │                          │    │
│  │  │ 美食探店  │ │ 读书打卡  │                          │    │
│  │  │ 征稿活动  │ │ 打卡活动  │                          │    │
│  │  │ 1,800参与 │ │ 960参与   │                          │    │
│  │  └──────────┘ └──────────┘                          │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  📅 全部活动  [进行中 v] [已结束]                      │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │ [封面] 夏日穿搭挑战  3,256参与  进行中         │    │    │
│  │  │ [封面] 美食探店征稿  1,800参与  进行中         │    │    │
│  │  │ [封面] 读书打卡活动    960参与   进行中         │    │    │
│  │  │ [封面] 春日摄影挑战  2,100参与  已结束         │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 活动详情页

```
┌─────────────────────────────────────────────────────────────┐
│  榔头 App - 活动详情                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  [活动封面图 - 全宽]                                 │    │
│  │                                                      │    │
│  │  夏日穿搭挑战                              进行中     │    │
│  │  2026.06.15 - 2026.06.30                            │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  活动简介                                             │    │
│  │  分享你的夏日穿搭灵感，展示你的时尚态度！               │    │
│  │  参与即有机会获得流量扶持和精美奖品。                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  参与规则                                             │    │
│  │  1. 发布笔记时携带 #夏日穿搭 标签                     │    │
│  │  2. 图文和视频均可参与                                │    │
│  │  3. 每人最多参与5篇笔记                               │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  活动奖励                                             │    │
│  │  🚀 流量扶持: 参与笔记额外1000曝光                    │    │
│  │  ⭐ 积分奖励: 参与即可获得20积分                      │    │
│  │  🎁 实物奖品: 榔头定制夏日礼包 x10份                  │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  参与排行  [热度 v] [笔记数]                         │    │
│  │  1. 穿搭博主小美  5篇  2340互动                       │    │
│  │  2. 时尚达人Lisa  4篇  1890互动                       │    │
│  │  3. 生活美学家    3篇  1560互动                       │    │
│  │  [查看完整排行]                                      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  优质参与内容                                         │    │
│  │  ┌────┐ ┌────┐ ┌────┐ ┌────┐                        │    │
│  │  │笔记1│ │笔记2│ │笔记3│ │笔记4│  (瀑布流)           │    │
│  │  └────┘ └────┘ └────┘ └────┘                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  [        立即参与 (携带 #夏日穿搭 发布笔记)       ]    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.3 发布笔记时的活动关联

```
用户发布笔记页面
  │
  ├─ 用户在标签输入框输入 #夏日穿搭
  │    │
  │    ├─ 系统检测到该标签关联进行中的活动
  │    │
  │    └─ 弹出提示:
  │         ┌──────────────────────────────────┐
  │         │  🎉 发现相关活动！                  │
  │         │                                   │
  │         │  「夏日穿搭挑战」正在进行中          │
  │         │  参与即可获得流量扶持+20积分          │
  │         │                                   │
  │         │  [参与活动] [不再提示]              │
  │         └──────────────────────────────────┘
  │
  ├─ 用户点击「参与活动」
  │    │
  │    ├─ 检查参与条件
  │    │    ├─ 满足 → 标记笔记参与活动
  │    │    └─ 不满足 → 提示原因
  │    │
  │    └─ 笔记发布后
  │         ├─ 显示参与成功提示
  │         ├─ 发放流量扶持
  │         └─ 发放参与积分
  │
  └─ 笔记详情页展示活动标识
       ┌──────────────────────────────────┐
       │  [活动标签: 夏日穿搭挑战]          │
       │  参与活动 | 获得20积分 | +1000曝光 │
       └──────────────────────────────────┘
```

---

## 9. 里程碑与排期

| 阶段 | 任务 | 预估工时 | 依赖 |
|-----|------|---------|------|
| Week 1 | 数据模型设计 + 建表 | 1.5人天 | 无 |
| Week 1 | 活动管理CRUD API (管理员端) | 3人天 | 数据模型 |
| Week 2 | 活动参与/退出API (用户端) | 2人天 | 活动管理API |
| Week 2 | 活动标签匹配 + 参与条件校验 | 2人天 | 活动管理API |
| Week 2 | 奖励机制实现 (流量扶持+积分) | 2人天 | 参与API |
| Week 3 | 活动数据统计API | 2人天 | 参与数据 |
| Week 3 | 活动排行榜API | 1.5人天 | 参与数据 |
| Week 3 | 官方账号管理API | 2人天 | 数据模型 |
| Week 3 | 推荐位管理API | 1.5人天 | 数据模型 |
| Week 4 | 管理后台 - 活动管理页面 | 3人天 | 管理员API |
| Week 4 | 管理后台 - 数据看板页面 | 2.5人天 | 统计API |
| Week 4 | 管理后台 - 官方账号管理页面 | 1.5人天 | 官方账号API |
| Week 4 | 移动端 - 活动发现页 + 详情页 | 3人天 | 用户端API |
| Week 4 | 移动端 - 发布笔记活动关联 | 1.5人天 | 参与API |
| Week 4 | 联调测试 + Bug修复 | 2人天 | 全部 |

**总预估**: 约31.5人天

---

## 附录

### A. 活动效果评估指标

| 指标 | 定义 | 目标值 |
|-----|------|--------|
| 活动参与率 | 参与人数 / 活动页UV | > 15% |
| 人均产出笔记数 | 参与笔记数 / 参与人数 | > 1.5 |
| 参与笔记平均互动率 | 总互动量 / 参与笔记数 | > 5% |
| 活动笔记通过率 | 审核通过数 / 总提交数 | > 90% |
| 流量扶持ROI | 扶持后互动增量 / 扶持曝光量 | > 3% |
| 活动结束后留存 | 参与者7日留存率 vs 非参与者 | > +5% |

### B. 运营活动日历模板

| 月份 | 活动主题 | 活动类型 | 预期目标 |
|-----|---------|---------|---------|
| 1月 | 新年心愿清单 | 话题挑战 | 参与人数2000+ |
| 2月 | 春节美食分享 | 话题挑战 | 参与人数3000+ |
| 3月 | 女神节穿搭 | 话题挑战 | 参与人数2500+ |
| 4月 | 春日摄影大赛 | 征稿活动 | 参与人数1500+ |
| 5月 | 母亲节礼物推荐 | 话题挑战 | 参与人数2000+ |
| 6月 | 夏日穿搭挑战 | 话题挑战 | 参与人数3000+ |
| 7月 | 暑期旅行日记 | 打卡活动 | 参与人数4000+ |
| 8月 | 美食探店季 | 话题挑战 | 参与人数2500+ |
| 9月 | 开学季好物 | 话题挑战 | 参与人数2000+ |
| 10月 | 国庆出游攻略 | 征稿活动 | 参与人数3000+ |
| 11月 | 双十一好物推荐 | 话题挑战 | 参与人数3500+ |
| 12月 | 年度盘点 | 话题挑战 | 参与人数2000+ |
