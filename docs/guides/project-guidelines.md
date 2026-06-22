# 榔头项目开发规范

## 1. 代码规范

### 1.1 通用规则
- 代码必须可直接运行，无语法错误
- 关键逻辑和复杂部分必须添加中文注释
- 单个函数不超过 50 行
- 单个文件不超过 300 行（配置文件除外）
- 控制缩进层级在 4 层以内
- 使用语义化命名，名字要能表达意图

### 1.2 前端规范 (Vue 3)
- 使用 Composition API 和 `<script setup>` 语法
- 使用 2 个空格缩进
- 优先使用 `<script setup>` 语法
- 组件按功能模块组织

### 1.3 后端规范 (Java/Spring Boot)
- 遵循 Controller → Service → Mapper 分层
- DTO、Entity、VO 严格区分
- 使用 Lombok 简化代码
- 统一响应封装 (Result<T>)
- 全局异常处理

### 1.4 Python 规范
- 遵循 PEP8 规范
- 使用 4 个空格缩进
- 使用 snake_case 命名
- 添加类型注解

## 2. 目录结构

```
src/
├── backend/           # 后端微服务
├── frontend/          # 前端应用
│   ├── web/          # 用户端
│   └── admin/        # 管理后台
├── mobile/            # 移动端
└── recommendation/    # 推荐系统
```

## 3. Git 规范

### 3.1 分支策略
- main: 主分支，稳定版本
- develop: 开发分支
- feature/*: 功能分支
- hotfix/*: 紧急修复

### 3.2 提交信息格式
```
<type>(<scope>): <subject>

<body>
```

类型：feat, fix, docs, style, refactor, test, chore

## 4. 命名规范

### 4.1 目录命名
- 使用小写字母 + 连字符: `user-service`, `content-service`
- 保持命名风格统一

### 4.2 文件命名
- Java 类: PascalCase (`UserController.java`)
- Vue 组件: PascalCase (`UserCard.vue`)
- TypeScript: camelCase (`userService.ts`)
- Python: snake_case (`user_service.py`)

## 5. API 规范

### 5.1 RESTful 接口
- GET: 查询
- POST: 创建
- PUT: 更新
- DELETE: 删除

### 5.2 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 6. 测试规范

- 关键功能必须编写单元测试
- 后端测试框架：JUnit 5
- 前端测试框架：Vitest
- 测试覆盖率不低于 70%

## 7. 安全规范

- 禁止在代码中硬编码密钥
- 用户输入必须验证和清理
- 敏感数据加密存储
- API 接口必须鉴权
