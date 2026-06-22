# 榔头(Langtou) - 消息推送方案设计

> **文档版本**: v1.0
> **所属Sprint**: Sprint 3
> **负责人**: 产品经理
> **创建日期**: 2026-06-12
> **状态**: 待评审

---

## 目录

1. [概述](#1-概述)
2. [推送场景分类](#2-推送场景分类)
3. [技术方案](#3-技术方案)
4. [推送策略](#4-推送策略)
5. [数据埋点](#5-数据埋点)
6. [API设计](#6-api设计)
7. [数据模型](#7-数据模型)
8. [交互流程](#8-交互流程)
9. [里程碑与排期](#9-里程碑与排期)

---

## 1. 概述

### 1.1 背景

榔头作为内容社区平台，消息推送是连接用户与平台的关键触达通道。合理的推送机制能够有效提升用户活跃度、内容互动率和社区粘性。本方案旨在建立一套完整、可靠、可扩展的消息推送体系。

### 1.2 目标

- **实时触达**：私信场景推送延迟 < 3秒
- **精准推送**：基于用户偏好和行为进行个性化推送
- **用户体验**：避免过度打扰，支持用户自定义推送策略
- **可观测**：推送全链路数据可追踪、可分析

### 1.3 范围

- 覆盖 Android (FCM) 和 iOS (APNs) 两大平台
- 支持私信、互动通知、系统通知、营销推送四大场景
- 提供用户端推送设置和管理员端推送管理能力

---

## 2. 推送场景分类

### 2.1 场景总览

| 场景分类 | 实时性要求 | 优先级 | 用户可关闭 | 需要授权 |
|---------|-----------|--------|-----------|---------|
| 私信推送 | 极高 (< 3s) | P0 | 否（仅可免打扰） | 否 |
| 互动通知 | 高 (< 30s) | P1 | 是 | 否 |
| 系统通知 | 中 (< 5min) | P2 | 是 | 否 |
| 营销推送 | 低 (< 1h) | P3 | 是 | 是 |

### 2.2 私信推送

**场景描述**：用户收到其他用户发送的私信时触发推送。

**推送规则**：
- 仅推送来自非屏蔽用户的私信
- 同一会话在免打扰时段内最多合并为1条推送
- 推送内容展示发送者昵称 + 消息摘要（截取前30字）
- 群聊消息仅展示"群名: 最新消息摘要"

**推送示例**：
```
标题: 榔头 - 新私信
内容: [用户昵称]: 消息内容摘要...
点击行为: 跳转至对应聊天会话页
```

### 2.3 互动通知

**场景描述**：用户发布的内容被其他用户互动时触发推送。

**子场景明细**：

| 子场景 | 触发条件 | 推送文案模板 | 合并规则 |
|-------|---------|------------|---------|
| 点赞 | 笔记被点赞 | "[用户] 赞了你的笔记" | 5分钟内同笔记点赞合并 |
| 评论 | 笔记被评论 | "[用户] 评论了你的笔记: 评论摘要" | 不合并，逐条推送 |
| 收藏 | 笔记被收藏 | "[用户] 收藏了你的笔记" | 5分钟内同笔记收藏合并 |
| 关注 | 被新用户关注 | "[用户] 关注了你" | 1小时内批量合并 |
| 回复 | 评论被回复 | "[用户] 回复了你的评论: 回复摘要" | 不合并，逐条推送 |
| 提及 | 在评论/笔记中被@ | "[用户] 在评论中提到了你" | 不合并 |

**推送示例**：
```
标题: 榔头 - 互动通知
内容: 小明 赞了你的笔记《周末探店推荐》
点击行为: 跳转至对应笔记详情页
```

### 2.4 系统通知

**场景描述**：平台级别的通知消息，包括审核结果、系统公告等。

**子场景明细**：

| 子场景 | 触发条件 | 推送文案模板 |
|-------|---------|------------|
| 审核通过 | 笔记审核通过 | "你的笔记《标题》已通过审核" |
| 审核驳回 | 笔记审核未通过 | "你的笔记《标题》未通过审核，原因: xxx" |
| 账号处罚 | 账号被限流/封禁 | "你的账号因违反社区规范，已被xxx处理" |
| 系统公告 | 平台发布重要公告 | "【公告】榔头社区规范更新通知" |
| 活动通知 | 用户参与的活动有更新 | "你参与的「话题挑战」活动已开始" |

**推送示例**：
```
标题: 榔头 - 系统通知
内容: 你的笔记《周末探店推荐》已通过审核，快去看看吧！
点击行为: 跳转至对应笔记详情页
```

### 2.5 营销推送

**场景描述**：运营活动推广、个性化内容推荐等营销类推送。

**推送规则**：
- 必须获得用户明确授权（首次推送前弹窗征求同意）
- 用户可随时在设置中关闭
- 每用户每天最多推送2条营销消息
- 不在免打扰时段内发送

**子场景明细**：

| 子场景 | 触发条件 | 推送文案模板 |
|-------|---------|------------|
| 活动推广 | 新活动上线 | "限时话题挑战「夏日穿搭」等你参与！" |
| 内容推荐 | 基于兴趣推荐热门内容 | "你关注的「美食探店」话题有新热门笔记" |
| 功能更新 | 新功能上线 | "榔头新功能上线！AI创作助手帮你写文案" |

---

## 3. 技术方案

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      榔头推送系统架构                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │ 业务服务  │───>│  推送网关     │───>│  推送路由服务     │  │
│  │ (微服务)  │    │ (Gateway)    │    │  (Router)        │  │
│  └──────────┘    └──────────────┘    └──────┬───────────┘  │
│       │                 │                    │              │
│       │          ┌──────┴──────┐        ┌─────┴─────┐       │
│       │          │  推送队列    │        │  设备注册  │       │
│       │          │ (Kafka)     │        │  服务      │       │
│       │          └─────────────┘        └───────────┘       │
│       │                                      │              │
│  ┌────┴──────────────────────────────────────┴──────────┐  │
│  │                    推送渠道层                          │  │
│  │  ┌─────────────────────┐  ┌──────────────────────┐  │  │
│  │  │  FCM (Android)      │  │  APNs (iOS)          │  │  │
│  │  │  - Firebase Console │  │  - Apple Developer    │  │  │
│  │  │  - Server Key       │  │  - Certificate/P8    │  │  │
│  │  └─────────────────────┘  └──────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│                    ┌──────┴──────┐                          │
│                    │  用户设备    │                          │
│                    │  Android/iOS │                          │
│                    └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Android: Firebase Cloud Messaging (FCM)

**集成方案**：

| 项目 | 说明 |
|-----|------|
| SDK版本 | Firebase Cloud Messaging v23.x+ |
| 认证方式 | Service Account JSON Key |
| 推送协议 | HTTP v1 API |
| 数据消息 | 支持自定义键值对，用于静默推送 |
| 通知消息 | 由FCM SDK自动处理系统通知栏展示 |

**FCM推送流程**：

```
1. 客户端启动 → 获取FCM Device Token
2. 客户端调用后端API → 注册Token到推送服务
3. 业务事件触发 → 推送网关组装消息 → 调用FCM HTTP v1 API
4. FCM服务 → 将消息推送到目标设备
5. 设备收到推送 → 系统通知栏展示 / 应用内处理
```

**FCM消息体设计**：

```json
{
  "message": {
    "token": "DEVICE_FCM_TOKEN",
    "notification": {
      "title": "榔头 - 新私信",
      "body": "小明: 你好，请问这个店在哪里？",
      "image": "https://cdn.langtou.com/avatar/default.png"
    },
    "data": {
      "type": "private_message",
      "conversation_id": "conv_12345",
      "sender_id": "user_67890",
      "note_id": "",
      "click_action": "OPEN_CONVERSATION"
    },
    "android": {
      "priority": "high",
      "notification": {
        "channel_id": "langtou_message",
        "sound": "default",
        "default_sound": true,
        "default_vibrate_timings": true
      }
    }
  }
}
```

### 3.3 iOS: Apple Push Notification service (APNs)

**集成方案**：

| 项目 | 说明 |
|-----|------|
| 认证方式 | P8证书 (Token-Based Authentication) |
| 推送协议 | HTTP/2 (APNs Provider API) |
| 证书管理 | Apple Developer Portal 配置 |
| 后台推送 | 支持 Background Push (Content Available) |

**APNs推送流程**：

```
1. 客户端启动 → 向APNs注册获取Device Token
2. 客户端调用后端API → 注册Token到推送服务
3. 业务事件触发 → 推送网关组装消息 → 调用APNs HTTP/2 API
4. APNs → 将消息推送到目标设备
5. 设备收到推送 → 系统处理展示 / 应用内处理
```

**APNs消息体设计**：

```json
{
  "aps": {
    "alert": {
      "title": "榔头 - 互动通知",
      "body": "小明 赞了你的笔记《周末探店推荐》",
      "subtitle": ""
    },
    "sound": "default",
    "badge": 5,
    "content-available": 1,
    "mutable-content": 1,
    "category": "INTERACTION"
  },
  "type": "like",
  "note_id": "note_12345",
  "actor_id": "user_67890",
  "click_action": "OPEN_NOTE"
}
```

### 3.4 推送网关设计

**推送网关 (Push Gateway)** 是推送系统的核心组件，负责接收业务推送请求、消息路由、频率控制和渠道适配。

**核心职责**：

```
推送网关
├── 接收层: 接收各微服务的推送请求 (REST API)
├── 预处理层
│   ├── 用户推送设置检查
│   ├── 频率控制检查
│   ├── 免打扰时段检查
│   └── 消息去重/合并
├── 路由层
│   ├── 设备Token查询
│   ├── 平台路由 (Android → FCM, iOS → APNs)
│   └── 批量推送优化
├── 发送层
│   ├── FCM适配器
│   ├── APNs适配器
│   └── 重试机制
└── 监控层
    ├── 推送日志记录
    ├── 埋点数据上报
    └── 异常告警
```

**技术选型**：

| 组件 | 技术方案 | 说明 |
|-----|---------|------|
| 推送网关服务 | Spring Boot (Java) | 作为独立微服务部署 |
| 消息队列 | Apache Kafka | 异步解耦，削峰填谷 |
| 设备Token存储 | Redis + MySQL | Redis做热缓存，MySQL做持久化 |
| 频率控制 | Redis (滑动窗口) | 基于用户维度的频率限制 |
| 推送日志 | Elasticsearch | 推送全链路日志存储与查询 |

### 3.5 设备Token注册与管理

**Token生命周期管理**：

```
注册 → 活跃 → 刷新 → 失效 → 注销

1. 注册: App启动时获取平台Token，调用后端API注册
2. 活跃: 正常使用中，Token有效
3. 刷新: 平台Token更新（FCM Token轮换），客户端主动更新
4. 失效: Token过期或无效，推送失败后标记
5. 注销: 用户登出或卸载App，主动注销Token
```

**多设备支持**：

- 一个用户可绑定多个设备Token（手机 + 平板）
- 同一用户所有设备同时推送
- 用户可在设置中管理已绑定设备

**Token失效处理**：

- FCM/APNs返回无效Token错误码时，自动标记Token失效
- 连续3次推送失败，标记Token为可疑状态
- 可疑状态Token下次App启动时强制刷新

### 3.6 推送消息模板设计

**模板引擎设计**：

```java
// 模板定义格式
TemplateKey: {
  "title": "榔头 - {scene_name}",
  "body": "{actor_name} {action}了你的笔记《{note_title}》",
  "data": {
    "type": "{event_type}",
    "click_action": "{click_target}"
  }
}
```

**模板列表**：

| 模板Key | 场景 | Title | Body |
|---------|------|-------|------|
| PUSH_PRIVATE_MSG | 私信 | 榔头 - 新私信 | {sender_name}: {msg_preview} |
| PUSH_LIKE | 点赞 | 榔头 - 互动通知 | {actor_name} 赞了你的笔记《{note_title}》 |
| PUSH_COMMENT | 评论 | 榔头 - 互动通知 | {actor_name} 评论了你的笔记: {comment_preview} |
| PUSH_FOLLOW | 关注 | 榔头 - 新粉丝 | {actor_name} 关注了你 |
| PUSH_REVIEW_PASS | 审核通过 | 榔头 - 系统通知 | 你的笔记《{note_title}》已通过审核 |
| PUSH_REVIEW_REJECT | 审核驳回 | 榔头 - 系统通知 | 你的笔记《{note_title}》未通过审核 |
| PUSH_ACTIVITY | 活动通知 | 榔头 - 活动提醒 | 你参与的「{activity_name}」活动有新动态 |
| PUSH_MARKETING | 营销推广 | 榔头 - 精选推荐 | {marketing_content} |

---

## 4. 推送策略

### 4.1 频率控制

**全局频率限制**：

| 维度 | 限制 | 说明 |
|-----|------|------|
| 每用户每天总推送数 | 最多20条 | 超出后当日停止推送 |
| 营销推送每天 | 最多2条 | 独立于互动/系统推送计数 |
| 同一类型推送间隔 | 最少30秒 | 防止短时间内同类推送轰炸 |
| 同一笔记互动合并窗口 | 5分钟 | 窗口内同类互动合并为1条 |

**频率控制实现**：

```
Redis滑动窗口方案:

Key: push:freq:{user_id}:{date}
Value: Sorted Set (score=timestamp, member=push_id)

每次推送前:
1. ZREMRANGEBYSCORE key 0 (current_time - window_size)  // 清理过期记录
2. ZCARD key  // 获取窗口内推送数
3. 如果 count >= limit → 拒绝推送
4. 否则 → ZADD key current_time push_id  // 记录推送
```

### 4.2 免打扰时段设置

**默认免打扰时段**：23:00 - 08:00

**用户自定义设置**：

```json
{
  "do_not_disturb": {
    "enabled": true,
    "start_time": "23:00",
    "end_time": "08:00",
    "override_rules": {
      "private_message": false,    // 私信不受免打扰限制（默认）
      "interaction": true,           // 互动通知受免打扰限制
      "system": true,               // 系统通知受免打扰限制
      "marketing": true             // 营销推送受免打扰限制
    }
  }
}
```

**免打扰判断逻辑**：

```
function shouldSendPush(userSettings, pushType, currentTime):
    if not userSettings.do_not_disturb.enabled:
        return true
    if isInRange(currentTime, userSettings.do_not_disturb):
        if pushType == "private_message" and not userSettings.override_rules.private_message:
            return true  // 私信始终推送
        return false  // 免打扰时段内不推送
    return true
```

### 4.3 消息优先级分级

| 优先级 | 级别 | 场景 | 推送策略 |
|-------|------|------|---------|
| P0 | 紧急 | 私信 | 立即推送，无视频率限制和免打扰 |
| P1 | 高 | 评论、回复、@提及 | 立即推送，受免打扰限制 |
| P2 | 中 | 点赞、收藏、关注、系统通知 | 延迟推送，受频率和免打扰限制 |
| P3 | 低 | 营销推送 | 批量推送，受所有策略限制 |

**优先级队列**：

```
Kafka Topic分区设计:

push.high.priority   → P0, P1 消息，消费者优先处理
push.normal.priority → P2 消息
push.low.priority    → P3 消息，闲时批量处理

消费者组配置:
- high.priority: 3个消费者实例
- normal.priority: 2个消费者实例
- low.priority: 1个消费者实例
```

### 4.4 离线消息合并策略

**合并规则**：

| 场景 | 合并窗口 | 合并方式 |
|-----|---------|---------|
| 同一笔记多条点赞 | 5分钟 | "小明等{N}人赞了你的笔记" |
| 同一笔记多条收藏 | 5分钟 | "小明等{N}人收藏了你的笔记" |
| 多人关注 | 1小时 | "小明等{N}人关注了你" |
| 多条系统通知 | 1小时 | "你有{N}条新的系统通知" |
| 私信（同一会话） | 不合并 | 每条独立推送（P0） |

**离线消息处理**：

- 用户离线超过30分钟：登录后推送未读消息摘要
- 用户离线超过24小时：登录后仅推送最近24小时的重要消息
- 离线期间消息存储在服务端，用户上线后拉取

---

## 5. 数据埋点

### 5.1 埋点指标体系

| 指标类别 | 指标名称 | 定义 | 计算方式 |
|---------|---------|------|---------|
| 到达 | 推送发送数 | 推送网关发出的消息总数 | push_sent_count |
| 到达 | 推送到达数 | FCM/APNs确认送达的消息数 | push_delivered_count |
| 到达 | 推送到达率 | 到达数 / 发送数 | delivery_rate |
| 点击 | 推送点击数 | 用户点击推送通知的次数 | push_click_count |
| 点击 | 推送点击率 | 点击数 / 到达数 | click_rate |
| 转化 | 推送转化数 | 用户点击后完成目标行为的次数 | push_conversion_count |
| 转化 | 推送转化率 | 转化数 / 到达数 | conversion_rate |
| 退订 | 推送关闭率 | 关闭推送权限的用户比例 | opt_out_rate |

### 5.2 埋点事件设计

**事件1: push_sent（推送发送）**

```json
{
  "event": "push_sent",
  "timestamp": "2026-06-12T10:30:00Z",
  "push_id": "push_abc123",
  "user_id": "user_456",
  "device_id": "device_789",
  "platform": "android",
  "push_type": "interaction",
  "push_priority": "P1",
  "template_key": "PUSH_LIKE",
  "note_id": "note_101",
  "actor_id": "user_202"
}
```

**事件2: push_delivered（推送到达）**

```json
{
  "event": "push_delivered",
  "timestamp": "2026-06-12T10:30:02Z",
  "push_id": "push_abc123",
  "user_id": "user_456",
  "device_id": "device_789",
  "platform": "android",
  "delivery_latency_ms": 2000
}
```

**事件3: push_clicked（推送点击）**

```json
{
  "event": "push_clicked",
  "timestamp": "2026-06-12T10:31:00Z",
  "push_id": "push_abc123",
  "user_id": "user_456",
  "device_id": "device_789",
  "push_type": "interaction",
  "click_action": "OPEN_NOTE",
  "target_id": "note_101",
  "time_to_click_ms": 60000
}
```

**事件4: push_converted（推送转化）**

```json
{
  "event": "push_converted",
  "timestamp": "2026-06-12T10:35:00Z",
  "push_id": "push_abc123",
  "user_id": "user_456",
  "push_type": "interaction",
  "conversion_action": "reply_comment",
  "target_id": "note_101"
}
```

### 5.3 数据看板需求

| 看板名称 | 展示指标 | 维度 |
|---------|---------|------|
| 推送总览 | 发送量、到达率、点击率、转化率 | 日/周/月趋势 |
| 场景分析 | 各场景推送量、到达率、点击率 | 推送类型维度 |
| 平台对比 | Android vs iOS 推送效果 | 平台维度 |
| 用户分群 | 不同活跃度用户的推送效果 | 用户分群维度 |
| 异常监控 | 推送失败率、Token失效率 | 实时监控 |

---

## 6. API设计

### 6.1 设备Token注册

**POST /api/v1/push/token**

注册或更新设备推送Token。

**Request Headers**:
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "platform": "android",
  "device_token": "fcm_device_token_xxx",
  "device_id": "device_uuid_xxx",
  "device_model": "Pixel 7",
  "os_version": "Android 14",
  "app_version": "2.3.0"
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token_id": "token_abc123",
    "status": "active",
    "registered_at": "2026-06-12T10:00:00Z"
  }
}
```

**Error Codes**:
| HTTP Status | Code | 说明 |
|------------|------|------|
| 400 | 10001 | 参数校验失败（platform/device_token为空） |
| 401 | 10002 | 未授权（Token无效或过期） |
| 500 | 10003 | 服务内部错误 |

---

### 6.2 设备Token注销

**DELETE /api/v1/push/token**

注销设备推送Token，用户登出或卸载时调用。

**Request Headers**:
```
Authorization: Bearer {access_token}
```

**Request Body**:
```json
{
  "device_id": "device_uuid_xxx"
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

### 6.3 更新推送设置

**PUT /api/v1/push/settings**

更新用户推送偏好设置。

**Request Headers**:
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "interaction": {
    "like": true,
    "comment": true,
    "collect": true,
    "follow": true,
    "reply": true,
    "mention": true
  },
  "system": {
    "review_result": true,
    "announcement": true,
    "activity": true
  },
  "marketing": {
    "enabled": true,
    "authorized": true
  },
  "do_not_disturb": {
    "enabled": true,
    "start_time": "23:00",
    "end_time": "08:00"
  }
}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "updated_at": "2026-06-12T10:00:00Z"
  }
}
```

---

### 6.4 获取推送设置

**GET /api/v1/push/settings**

获取当前用户的推送偏好设置。

**Request Headers**:
```
Authorization: Bearer {access_token}
```

**Response (200 OK)**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "interaction": {
      "like": true,
      "comment": true,
      "collect": true,
      "follow": true,
      "reply": true,
      "mention": true
    },
    "system": {
      "review_result": true,
      "announcement": true,
      "activity": true
    },
    "marketing": {
      "enabled": true,
      "authorized": true
    },
    "do_not_disturb": {
      "enabled": true,
      "start_time": "23:00",
      "end_time": "08:00"
    },
    "registered_devices": [
      {
        "device_id": "device_uuid_001",
        "platform": "android",
        "device_model": "Pixel 7",
        "last_active_at": "2026-06-12T09:00:00Z",
        "status": "active"
      },
      {
        "device_id": "device_uuid_002",
        "platform": "ios",
        "device_model": "iPhone 15",
        "last_active_at": "2026-06-11T20:00:00Z",
        "status": "active"
      }
    ]
  }
}
```

---

## 7. 数据模型

### 7.1 push_device_tokens 表

存储用户设备推送Token信息。

```sql
CREATE TABLE push_device_tokens (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    device_id       VARCHAR(128) NOT NULL COMMENT '设备唯一标识',
    platform        VARCHAR(20) NOT NULL COMMENT '平台: android/ios',
    device_token    VARCHAR(512) NOT NULL COMMENT 'FCM/APNs Device Token',
    device_model    VARCHAR(64) DEFAULT NULL COMMENT '设备型号',
    os_version      VARCHAR(32) DEFAULT NULL COMMENT '操作系统版本',
    app_version     VARCHAR(16) DEFAULT NULL COMMENT 'App版本号',
    status          VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/suspended/expired',
    last_active_at  DATETIME NOT NULL COMMENT '最后活跃时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device (user_id, device_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_token (device_token(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备推送Token表';
```

### 7.2 push_settings 表

存储用户推送偏好设置。

```sql
CREATE TABLE push_settings (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    like_enabled        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '点赞推送开关',
    comment_enabled     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '评论推送开关',
    collect_enabled     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '收藏推送开关',
    follow_enabled      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '关注推送开关',
    reply_enabled       TINYINT(1) NOT NULL DEFAULT 1 COMMENT '回复推送开关',
    mention_enabled     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '@提及推送开关',
    review_enabled      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '审核结果推送开关',
    announcement_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '系统公告推送开关',
    activity_enabled    TINYINT(1) NOT NULL DEFAULT 1 COMMENT '活动通知推送开关',
    marketing_enabled   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '营销推送开关',
    marketing_authorized TINYINT(1) NOT NULL DEFAULT 0 COMMENT '营销推送授权',
    dnd_enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '免打扰开关',
    dnd_start_time      VARCHAR(8) NOT NULL DEFAULT '23:00' COMMENT '免打扰开始时间',
    dnd_end_time        VARCHAR(8) NOT NULL DEFAULT '08:00' COMMENT '免打扰结束时间',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户推送设置表';
```

### 7.3 push_logs 表

存储推送发送日志，用于数据分析和问题排查。

```sql
CREATE TABLE push_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    push_id         VARCHAR(64) NOT NULL COMMENT '推送唯一ID',
    user_id         BIGINT NOT NULL COMMENT '目标用户ID',
    device_id       VARCHAR(128) NOT NULL COMMENT '目标设备ID',
    platform        VARCHAR(20) NOT NULL COMMENT '平台',
    push_type       VARCHAR(32) NOT NULL COMMENT '推送类型',
    push_priority   VARCHAR(4) NOT NULL COMMENT '优先级: P0/P1/P2/P3',
    template_key    VARCHAR(64) DEFAULT NULL COMMENT '消息模板Key',
    title           VARCHAR(128) NOT NULL COMMENT '推送标题',
    body            VARCHAR(256) NOT NULL COMMENT '推送内容',
    data            JSON DEFAULT NULL COMMENT '推送附加数据',
    status          VARCHAR(16) NOT NULL COMMENT '状态: sent/delivered/failed/dropped',
    error_code      VARCHAR(32) DEFAULT NULL COMMENT '失败错误码',
    sent_at         DATETIME NOT NULL COMMENT '发送时间',
    delivered_at    DATETIME DEFAULT NULL COMMENT '到达时间',
    clicked_at      DATETIME DEFAULT NULL COMMENT '点击时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, sent_at),
    INDEX idx_push_id (push_id),
    INDEX idx_status_time (status, sent_at),
    INDEX idx_type_time (push_type, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送日志表';
```

### 7.4 push_frequency_counters 表

存储用户每日推送频率计数（辅助持久化，主计数在Redis）。

```sql
CREATE TABLE push_frequency_counters (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    counter_date    DATE NOT NULL COMMENT '计数日期',
    total_count     INT NOT NULL DEFAULT 0 COMMENT '总推送数',
    marketing_count INT NOT NULL DEFAULT 0 COMMENT '营销推送数',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, counter_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送频率计数表';
```

---

## 8. 交互流程

### 8.1 Token注册流程

```
App启动
  │
  ├─ 检查推送权限
  │    ├─ 未授权 → 弹窗请求权限
  │    │    ├─ 用户允许 → 获取Device Token
  │    │    └─ 用户拒绝 → 记录状态，后续不再弹窗
  │    └─ 已授权 → 获取Device Token
  │
  ├─ 获取到Token
  │    ├─ POST /api/v1/push/token
  │    ├─ 后端验证Token有效性
  │    ├─ 存储到 push_device_tokens 表
  │    └─ 缓存到 Redis (热数据)
  │
  └─ Token注册完成
```

### 8.2 推送发送全流程

```
业务事件触发 (如: 用户A点赞了用户B的笔记)
  │
  ├─ 1. 业务服务发送推送请求到推送网关
  │    Request: { user_id, push_type, template_key, template_params }
  │
  ├─ 2. 推送网关预处理
  │    ├─ 查询用户推送设置 (Redis缓存 → MySQL)
  │    ├─ 检查推送开关是否开启
  │    ├─ 检查免打扰时段
  │    ├─ 检查频率限制 (Redis滑动窗口)
  │    ├─ 检查是否需要合并 (同类型消息合并)
  │    └─ 生成推送消息 (模板渲染)
  │
  ├─ 3. 消息入队
  │    ├─ 根据优先级选择Kafka Topic
  │    └─ push.high.priority / push.normal.priority / push.low.priority
  │
  ├─ 4. 消费者拉取消息
  │    ├─ 查询用户设备Token列表
  │    ├─ 按平台分组
  │    └─ 批量发送
  │
  ├─ 5. 渠道适配发送
  │    ├─ Android → FCM HTTP v1 API
  │    ├─ iOS → APNs HTTP/2 API
  │    └─ 记录推送日志
  │
  ├─ 6. 回调处理
  │    ├─ 发送成功 → 更新状态为delivered
  │    ├─ Token失效 → 标记Token过期
  │    └─ 发送失败 → 重试 (最多3次，指数退避)
  │
  └─ 7. 埋点上报
       ├─ push_sent 事件
       ├─ push_delivered 事件
       └─ 等待客户端上报 push_clicked / push_converted
```

### 8.3 用户设置推送偏好流程 (移动端)

```
设置页面
  │
  ├─ 进入「通知设置」
  │    │
  │    ├─ 互动通知区域
  │    │    ├─ [开关] 点赞通知
  │    │    ├─ [开关] 评论通知
  │    │    ├─ [开关] 收藏通知
  │    │    ├─ [开关] 新粉丝通知
  │    │    ├─ [开关] 回复通知
  │    │    └─ [开关] @提及通知
  │    │
  │    ├─ 系统通知区域
  │    │    ├─ [开关] 审核结果通知
  │    │    ├─ [开关] 系统公告
  │    │    └─ [开关] 活动通知
  │    │
  │    ├─ 营销推送区域
  │    │    ├─ [开关] 推荐内容
  │    │    └─ [开关] 活动推广
  │    │
  │    ├─ 免打扰设置区域
  │    │    ├─ [开关] 免打扰模式
  │    │    ├─ [时间选择器] 开始时间 (默认23:00)
  │    │    └─ [时间选择器] 结束时间 (默认08:00)
  │    │
  │    └─ 已绑定设备列表
  │         ├─ Pixel 7 (Android) - 最后活跃: 今天 09:00
  │         └─ iPhone 15 (iOS) - 最后活跃: 昨天 20:00
  │
  └─ 用户修改设置 → PUT /api/v1/push/settings → 本地缓存更新
```

---

## 9. 里程碑与排期

| 阶段 | 任务 | 预估工时 | 依赖 |
|-----|------|---------|------|
| Week 1 | 推送网关服务搭建 + Kafka集成 | 3人天 | 无 |
| Week 1 | FCM/APNs渠道适配器开发 | 2人天 | 推送网关 |
| Week 2 | 设备Token注册/注销API | 1人天 | 推送网关 |
| Week 2 | 推送设置API + 数据模型 | 1人天 | 无 |
| Week 2 | 频率控制 + 免打扰策略实现 | 2人天 | 推送网关 |
| Week 3 | 私信推送接入 | 1人天 | 推送网关 |
| Week 3 | 互动通知推送接入 | 2人天 | 推送网关 |
| Week 3 | 系统通知 + 营销推送接入 | 1人天 | 推送网关 |
| Week 4 | 移动端Token注册集成 | 2人天 | Token API |
| Week 4 | 移动端推送设置页面 | 2人天 | Settings API |
| Week 4 | 数据埋点 + 看板搭建 | 2人天 | 推送日志 |
| Week 4 | 联调测试 + Bug修复 | 2人天 | 全部 |

**总预估**: 约21人天

---

## 附录

### A. 推送渠道对比

| 维度 | FCM (Android) | APNs (iOS) |
|-----|---------------|------------|
| 协议 | HTTP v1 | HTTP/2 |
| 认证 | Service Account | P8 Token |
| 消息大小限制 | 4KB (通知) / 5KB (数据) | 4KB |
| 批量推送 | 支持 (Multicast) | 不支持原生批量 |
| 送达确认 | 支持 | 支持 |
| 静默推送 | 数据消息 | Content-available |
| 成本 | 免费 | 免费 |

### B. 推送测试方案

1. **单设备推送测试**: 验证单设备Token推送到达
2. **多设备推送测试**: 验证同一用户多设备同时推送
3. **频率控制测试**: 验证超出频率限制后推送被拦截
4. **免打扰测试**: 验证免打扰时段内推送行为
5. **Token失效测试**: 验证Token失效后的处理逻辑
6. **弱网环境测试**: 验证网络不稳定时推送到达情况
7. **离线推送测试**: 验证设备离线后上线时的推送行为
