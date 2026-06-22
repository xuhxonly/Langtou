# 榔头 Langtou Admin

榔头 Langtou 管理后台前端项目，基于 **Vue 3.4 + Vite + TypeScript + Element Plus + Pinia** 打造。

## ✨ 技术栈

- Vue 3.4 + `<script setup>` 组合式 API
- TypeScript 严格模式
- Vite 5.x 构建工具
- Element Plus 2.x 组件库（深色主题）
- Pinia 状态管理
- Vue Router 4.x
- Axios + 统一拦截器
- ECharts 5.x 数据可视化
- Sass 样式预处理

## 📁 项目结构

```
langtou-admin/
├── index.html                  # Vite 入口
├── package.json                # 依赖配置
├── vite.config.ts              # Vite 配置（别名、代理）
├── tsconfig.json               # TS 配置
├── .env.development            # 开发环境变量
├── .env.production             # 生产环境变量
└── src/
    ├── main.ts                 # 应用入口
    ├── App.vue                 # 根组件
    ├── router/index.ts         # 路由配置
    ├── stores/user.ts          # 用户状态（Pinia）
    ├── utils/request.ts        # axios 封装
    ├── api/                    # 各业务模块 API
    ├── layouts/Layout.vue      # 全局布局
    ├── views/                  # 页面组件
    └── styles/variables.scss   # 品牌色变量
```

## 🚀 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 5173）
npm run dev

# 生产构建
npm run build

# 预览构建产物
npm run preview
```

## 📝 说明

- 品牌主色：`#3b82f6`（榔头蓝）
- 后端接口：默认代理 `http://localhost:8080`，可通过 `.env.*` 修改
- 默认登录账号：`admin / admin123`
