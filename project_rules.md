# 榔头项目规则

## 项目概述
- 项目名称: 榔头 (Langtou)
- 项目类型: 社交内容社区 APP (类小红书)
- 技术架构: 微服务架构，前后端分离

## 技术栈
### 前端
- Web: Vue 3 + Vite + Pinia
- Admin: Vue 3 + TypeScript + Vite
- Mobile: React Native + TypeScript

### 后端
- 语言: Java 21
- 框架: Spring Boot 3.x
- 数据库: MySQL 8, Redis, Elasticsearch
- 消息队列: Apache Kafka

### 推荐系统
- 语言: Python 3.10+
- 框架: FastAPI
- 算法: XGBoost, LightGBM

## 项目结构
```
src/
├── backend/         # 后端微服务
├── frontend/        # 前端应用
├── mobile/          # 移动端
└── recommendation/  # 推荐系统
```

## 代码规范
请参考 `docs/guides/project-guidelines.md`

## Git 规范
- 分支: main, develop, feature/*, hotfix/*
- 提交信息: feat/fix/docs/style/refactor/test/chore

## 命名规范
- 目录: kebab-case (user-service)
- Java 类: PascalCase (UserController)
- Vue 组件: PascalCase (UserCard)
- TypeScript: camelCase (userService)
- Python: snake_case (user_service)
