# 榔头(Langtou)商品橱窗 MVP 产品需求文档(PRD)

**文档编号**: S3-02
**版本**: v1.0
**日期**: 2026-06-12
**编制**: 榔头产品团队
**状态**: 评审中
**关联文档**: 《榔头内容社区平台 v7.0 产品需求文档(PRD)》

---

## 1. 背景与目标

### 1.1 背景

榔头作为内容社区平台，已积累了大量优质创作者和活跃用户。然而，当前创作者的变现渠道仅限于平台激励和品牌合作，缺乏直接的电商变现能力。同时，用户在浏览种草内容时，无法直接完成从"种草"到"拔草"的转化闭环，导致用户体验断裂和平台商业化能力不足。

商品橱窗是内容电商的核心基础设施，通过"内容+商品"的模式，帮助创作者实现内容变现，同时提升用户的消费体验和平台的商业化收入。

### 1.2 目标

| 目标类型 | 具体目标 | 衡量指标 |
|----------|----------|----------|
| **创作者变现** | 为创作者提供电商带货基础能力 | MVP阶段开通商品橱窗的创作者 > 100人 |
| **用户体验** | 实现内容到商品的转化闭环 | 商品点击率 > 5%，笔记关联商品转化率 > 0.5% |
| **商业化** | 建立平台分佣收入模型 | MVP阶段月均GMV > 10万元 |
| **生态建设** | 建立商品管理基础能力 | 入驻商品 > 500个SKU |

### 1.3 目标用户

| 用户群体 | 描述 | 核心诉求 |
|----------|------|----------|
| **内容创作者** | 有一定粉丝基础的KOL/KOC | 通过内容带货变现，管理商品橱窗 |
| **内容消费者** | 榔头平台普通用户 | 浏览种草内容时便捷购买商品 |
| **品牌商家** | 希望通过创作者推广商品的品牌 | 管理商品信息，追踪带货效果 |
| **平台运营** | 榔头电商运营团队 | 管理商品审核，监控交易数据 |

### 1.4 MVP范围说明

本版本为商品橱窗 MVP（最小可行产品），聚焦核心链路：

**MVP包含**:
- 创作者添加/管理商品
- 笔记内嵌商品链接
- 商品详情页
- 创作者商品橱窗页
- 基础分佣计算

**MVP不包含**（后续版本迭代）:
- 完整订单交易系统（MVP阶段跳转外部链接购买）
- 创作者选品中心
- 品牌商家后台
- 物流/售后系统
- 佣金提现系统（MVP阶段仅记录，后续版本支持提现）

---

## 2. 数据模型设计

### 2.1 Product（商品）实体

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格（元）',
    original_price DECIMAL(10,2) COMMENT '原价（划线价）',
    image_url VARCHAR(500) NOT NULL COMMENT '商品主图URL',
    images JSON COMMENT '商品图片列表（多图）',
    link VARCHAR(1000) NOT NULL COMMENT '商品外部购买链接',
    link_type VARCHAR(20) DEFAULT 'external' COMMENT '链接类型：external(外部链接)/internal(站内)',
    category_id BIGINT COMMENT '商品分类ID',
    brand VARCHAR(100) COMMENT '品牌名称',
    creator_id BIGINT NOT NULL COMMENT '添加该商品的创作者ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-下架/1-上架/2-审核中/3-审核拒绝',
    commission_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '佣金比例（%）',
    sales_count INT DEFAULT 0 COMMENT '销量（累计）',
    click_count INT DEFAULT 0 COMMENT '点击量（累计）',
    source VARCHAR(20) DEFAULT 'creator' COMMENT '来源：creator(创作者添加)/platform(平台配置)/brand(品牌入驻)',
    extra_info JSON COMMENT '扩展信息（规格参数等）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_status (creator_id, status),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

### 2.2 NoteProduct（笔记-商品关联）实体

```sql
CREATE TABLE note_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    position INT DEFAULT 0 COMMENT '商品在笔记中的展示位置（排序）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-移除/1-正常',
    click_count INT DEFAULT 0 COMMENT '该笔记中该商品的点击量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_note_product (note_id, product_id),
    INDEX idx_note (note_id),
    INDEX idx_product (product_id),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记商品关联表';
```

### 2.3 ProductCategory（商品分类）实体

```sql
CREATE TABLE product_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID（0为一级分类）',
    level TINYINT DEFAULT 1 COMMENT '分类层级：1-一级/2-二级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    icon_url VARCHAR(500) COMMENT '分类图标',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用/1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';
```

### 2.4 CommissionRecord（佣金记录）实体

```sql
CREATE TABLE commission_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    note_id BIGINT COMMENT '关联笔记ID（可为空，表示非笔记带货）',
    order_no VARCHAR(64) COMMENT '外部订单号',
    order_amount DECIMAL(10,2) COMMENT '订单金额',
    commission_rate DECIMAL(5,2) NOT NULL COMMENT '佣金比例（%）',
    commission_amount DECIMAL(10,2) NOT NULL COMMENT '佣金金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待确认/1-已确认/2-已结算/3-已失效',
    source VARCHAR(20) DEFAULT 'click' COMMENT '来源：click(点击追踪)/manual(人工录入)',
    click_at DATETIME COMMENT '点击时间',
    order_at DATETIME COMMENT '下单时间',
    confirm_at DATETIME COMMENT '确认时间',
    settle_at DATETIME COMMENT '结算时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_id),
    INDEX idx_product (product_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金记录表';
```

### 2.5 ER关系图

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│   Creator    │     │    Product       │     │   Category   │
│──────────────│     │──────────────────│     │──────────────│
│ id           │<────│ creator_id       │     │ id           │
│ name         │     │ id               │────>│ category_id  │
│ ...          │     │ name             │     │ name         │
└──────────────┘     │ price            │     │ parent_id    │
       │             │ link             │     └──────────────┘
       │             │ status           │
       │             │ commission_rate  │
       │             └──────────────────┘
       │                      │
       │             ┌────────┴─────────┐
       │             │  NoteProduct     │
       │             │──────────────────│
       ├─────────────│ creator_id       │
       │             │ note_id          │
       │             │ product_id       │
┌──────┴──────┐      │ position        │
│    Note      │<─────│ status          │
│──────────────│      └──────────────────┘
│ id           │
│ creator_id   │      ┌──────────────────┐
│ title        │      │CommissionRecord │
│ ...          │      │──────────────────│
└──────────────┘      │ creator_id       │
                      │ product_id       │
                      │ note_id          │
                      │ commission_amount│
                      │ status           │
                      └──────────────────┘
```

---

## 3. 核心功能设计

### 3.1 创作者添加/管理商品

#### 3.1.1 用户故事

| 编号 | 用户故事 | 验收标准 |
|------|----------|----------|
| PS-US-01 | 作为创作者，我希望添加商品到我的橱窗，以便在笔记中推荐给粉丝 | 可通过输入商品链接或手动填写信息添加商品 |
| PS-US-02 | 作为创作者，我希望编辑已添加的商品信息，以便保持商品信息准确 | 可修改商品名称、价格、图片、描述等信息 |
| PS-US-03 | 作为创作者，我希望上架/下架商品，以便控制商品的展示状态 | 可一键切换商品上架/下架状态 |
| PS-US-04 | 作为创作者，我希望删除不再推荐的商品，以便保持橱窗整洁 | 可删除商品，已关联笔记的商品需确认后删除 |
| PS-US-05 | 作为创作者，我希望查看商品数据（点击量/销量），以便优化选品策略 | 可查看每个商品的点击量和销量数据 |

#### 3.1.2 添加商品流程

```
创作者进入"我的橱窗"
  -> 点击"添加商品"
  -> 选择添加方式:
    -> 方式一：粘贴商品链接
      -> 输入外部商品链接（淘宝/京东/拼多多等）
      -> 系统自动解析商品信息（名称/价格/图片）
      -> 创作者确认/编辑信息
      -> 提交审核
    -> 方式二：手动填写
      -> 填写商品名称
      -> 填写商品价格
      -> 上传商品图片（最多9张）
      -> 填写商品描述
      -> 填写商品购买链接
      -> 选择商品分类
      -> 提交审核
  -> 审核通过后自动上架
```

#### 3.1.3 商品管理页面设计

```
┌─────────────────────────────────────┐
│  我的橱窗                     添加商品 │
├─────────────────────────────────────┤
│  全部(15)  上架(12)  下架(2)  审核中(1)│
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │ [图片]  商品名称              │    │
│  │         ¥128.00  原价¥199.00 │    │
│  │         点击: 1,230  销量: 56 │    │
│  │         状态: 上架            │    │
│  │         [编辑] [下架] [删除]  │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ [图片]  商品名称              │    │
│  │         ¥89.00               │    │
│  │         点击: 560  销量: 23   │    │
│  │         状态: 上架            │    │
│  │         [编辑] [下架] [删除]  │    │
│  └─────────────────────────────┘    │
│  ...                                │
└─────────────────────────────────────┘
```

#### 3.1.4 商品审核规则

| 审核项 | 规则 | 处理方式 |
|--------|------|----------|
| 商品信息完整性 | 名称、价格、图片、链接必填 | 不完整则拒绝 |
| 商品图片 | 图片清晰、无水印、与商品相关 | 不合规则拒绝 |
| 商品链接 | 链接可访问、指向商品页面 | 不可访问则拒绝 |
| 商品分类 | 必须选择正确的分类 | 未分类则拒绝 |
| 违禁商品 | 不含违禁品（参照平台违禁品列表） | 命中则拒绝 |
| 价格合理性 | 价格在合理范围内（非0、非负数） | 异常则拒绝 |

### 3.2 笔记内嵌商品链接

#### 3.2.1 用户故事

| 编号 | 用户故事 | 验收标准 |
|------|----------|----------|
| PS-US-06 | 作为创作者，我希望在发布笔记时关联商品，以便粉丝可以直接查看和购买 | 发布/编辑笔记时可添加最多10个商品链接 |
| PS-US-07 | 作为创作者，我希望在笔记中管理已关联的商品（添加/移除/排序），以便灵活调整推荐 | 可在笔记编辑页管理关联商品 |
| PS-US-08 | 作为用户，我希望在浏览笔记时看到推荐的商品，以便了解笔记中提到的产品 | 笔记底部展示关联商品卡片列表 |
| PS-US-09 | 作为用户，我希望点击笔记中的商品可以查看商品详情，以便了解商品信息并购买 | 点击商品卡片跳转商品详情页 |

#### 3.2.2 笔记关联商品交互设计

**创作者端 - 发布笔记时添加商品**:

```
发布/编辑笔记页面
  -> 正文编辑区域下方
  -> "关联商品"模块
    -> 点击"添加商品"
      -> 弹出商品选择面板
        -> 展示创作者橱窗中已上架商品
        -> 支持搜索商品名称
        -> 点击选择商品（最多10个）
    -> 已选商品列表
      -> 可拖拽排序
      -> 可移除已选商品
  -> 发布笔记时一并提交商品关联
```

**用户端 - 笔记中商品展示**:

```
笔记详情页
  -> 正文内容
  -> 商品推荐区域（如有关联商品）
    ┌─────────────────────────────────────┐
    │ 🛒 推荐好物 (3)                       │
    ├─────────────────────────────────────┤
    │  ┌────────┐  ┌────────┐  ┌────────┐ │
    │  │ [图片]  │  │ [图片]  │  │ [图片]  │ │
    │  │ ¥128   │  │ ¥89    │  │ ¥256   │ │
    │  │ 商品名1 │  │ 商品名2 │  │ 商品名3 │ │
    │  └────────┘  └────────┘  └────────┘ │
    └─────────────────────────────────────┘
```

#### 3.2.3 商品展示规则

| 规则 | 说明 |
|------|------|
| 展示位置 | 笔记正文下方，评论区上方 |
| 展示数量 | 最多展示10个商品，横向滑动浏览 |
| 展示样式 | 商品卡片：图片+价格+名称，横向排列 |
| 商品状态 | 仅展示"上架"状态的商品，下架/审核中的不展示 |
| 空状态 | 无关联商品时不展示该区域 |

### 3.3 商品详情页

#### 3.3.1 用户故事

| 编号 | 用户故事 | 验收标准 |
|------|----------|----------|
| PS-US-10 | 作为用户，我希望查看商品的详细信息，以便了解商品并做出购买决策 | 商品详情页展示图片、价格、描述、推荐该商品的创作者信息 |
| PS-US-11 | 作为用户，我希望在商品详情页看到推荐该商品的笔记，以便了解更多使用体验 | 展示关联该商品的所有笔记列表 |
| PS-US-12 | 作为用户，我希望点击购买按钮跳转到购买页面，以便完成购买 | 点击"去购买"跳转外部商品链接 |

#### 3.3.2 商品详情页设计

```
┌─────────────────────────────────────┐
│  ← 商品详情                    分享  │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │         商品大图              │    │  <- 商品图片轮播
│  │                             │    │
│  │         1 / 5                │    │
│  └─────────────────────────────┘    │
│                                     │
│  商品名称（最多两行）                 │
│  ¥128.00    ¥199.00（划线价）        │  <- 价格信息
│                                     │
│  ─────────────────────────────      │
│                                     │
│  📝 商品描述                         │
│  商品详细描述文本...                  │
│                                     │
│  ─────────────────────────────      │
│                                     │
│  🏷️ 商品信息                         │
│  品牌: XXX    分类: 美妆 > 护肤       │
│                                     │
│  ─────────────────────────────      │
│                                     │
│  👤 推荐创作者                        │
│  ┌─────────────────────────────┐    │
│  │ [头像] 创作者名称   [关注]   │    │
│  │ 共推荐了 5 件商品            │    │
│  └─────────────────────────────┘    │
│                                     │
│  ─────────────────────────────      │
│                                     │
│  📖 相关笔记 (12)                    │
│  ┌─────────────────────────────┐    │
│  │ [封面] 笔记标题1              │    │  <- 关联该商品的笔记列表
│  │        创作者名 · 2天前       │    │
│  ├─────────────────────────────┤    │
│  │ [封面] 笔记标题2              │    │
│  │        创作者名 · 5天前       │    │
│  └─────────────────────────────┘    │
│                                     │
├─────────────────────────────────────┤
│  [❤️ 收藏]           [🛒 去购买]     │  <- 底部操作栏
└─────────────────────────────────────┘
```

### 3.4 创作者商品橱窗页

#### 3.4.1 用户故事

| 编号 | 用户故事 | 验收标准 |
|------|----------|----------|
| PS-US-13 | 作为用户，我希望查看创作者的商品橱窗，以便浏览该创作者推荐的所有商品 | 创作者主页展示"橱窗"Tab，展示已上架商品 |
| PS-US-14 | 作为创作者，我希望我的橱窗在个人主页展示，以便粉丝浏览和购买 | 个人主页增加"橱窗"入口，展示商品列表 |

#### 3.4.2 橱窗页设计

```
创作者个人主页
  -> Tab: [笔记] [视频] [收藏] [橱窗]  <- 新增"橱窗"Tab
  -> 橱窗Tab内容:
    ┌─────────────────────────────────────┐
    │  🛒 商品橱窗 (15件)                   │
    ├─────────────────────────────────────┤
    │  ┌────────┐  ┌────────┐  ┌────────┐ │
    │  │ [图片]  │  │ [图片]  │  │ [图片]  │ │  <- 瀑布流/网格布局
    │  │ ¥128   │  │ ¥89    │  │ ¥256   │ │
    │  │ 商品名  │  │ 商品名  │  │ 商品名  │ │
    │  └────────┘  └────────┘  └────────┘ │
    │  ┌────────┐  ┌────────┐  ┌────────┐ │
    │  │ [图片]  │  │ [图片]  │  │ [图片]  │ │
    │  │ ¥168   │  │ ¥45    │  │ ¥320   │ │
    │  │ 商品名  │  │ 商品名  │  │ 商品名  │ │
    │  └────────┘  └────────┘  └────────┘ │
    └─────────────────────────────────────┘
```

### 3.5 分佣计算逻辑

#### 3.5.1 分佣模型

```
佣金金额 = 订单金额 x 佣金比例

其中:
  - 订单金额: 用户实际支付的金额（不含运费）
  - 佣金比例: 由平台统一设置或按商品/分类差异化设置
  - 平台抽成: 佣金中的一定比例归平台（MVP阶段暂不抽成，全额给创作者）
```

#### 3.5.2 佣金比例设置

| 设置维度 | 默认值 | 说明 |
|----------|--------|------|
| 全局默认佣金比例 | 10% | 所有商品的默认佣金比例 |
| 分类佣金比例 | 按分类设置 | 可针对不同商品分类设置不同佣金比例 |
| 商品级佣金比例 | 按商品设置 | 可针对单个商品设置特定佣金比例（优先级最高） |

**佣金比例优先级**: 商品级 > 分类级 > 全局默认

#### 3.5.3 佣金结算流程（MVP阶段）

```
MVP阶段（简化版）:
  用户点击商品链接
    -> 记录点击行为（creator_id, product_id, note_id, click_time）
    -> 跳转外部购买链接
    -> MVP阶段无法追踪实际订单
    -> 佣金记录标记为"待确认"
    -> 后续版本对接电商平台API实现自动追踪

后续版本（完整版）:
  用户点击商品链接
    -> 记录点击行为
    -> 跳转站内/联盟购买链接
    -> 用户下单支付
    -> 电商平台回调订单信息
    -> 计算佣金
    -> 订单确认收货后佣金生效
    -> 月度结算
    -> 创作者提现
```

#### 3.5.4 MVP阶段佣金记录

MVP阶段采用"点击计费"模式作为过渡方案：

| 指标 | 说明 |
|------|------|
| 计费方式 | 按有效点击计费（每次点击记录一笔佣金） |
| 单次点击佣金 | 商品价格 x 佣金比例 x 0.01（即预估转化率1%） |
| 佣金上限 | 单个商品每日佣金上限 = 商品价格 x 佣金比例 x 10 |
| 结算方式 | 月度汇总，人工审核后结算 |

---

## 4. 接口设计

### 4.1 接口清单

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/products` | POST | 创建商品 | 创作者Token |
| `/api/v1/products/{id}` | GET | 商品详情 | 无需认证 |
| `/api/v1/products/{id}` | PUT | 更新商品信息 | 创作者Token(本人) |
| `/api/v1/products/{id}` | DELETE | 删除商品 | 创作者Token(本人) |
| `/api/v1/products/{id}/status` | PUT | 修改商品状态(上架/下架) | 创作者Token(本人) |
| `/api/v1/creators/{id}/products` | GET | 创作者商品列表 | 无需认证 |
| `/api/v1/notes/{noteId}/products` | POST | 笔记关联商品 | 创作者Token(本人) |
| `/api/v1/notes/{noteId}/products` | GET | 获取笔记关联商品 | 无需认证 |
| `/api/v1/notes/{noteId}/products/{productId}` | DELETE | 笔记移除关联商品 | 创作者Token(本人) |
| `/api/v1/products/{id}/click` | POST | 记录商品点击 | 用户Token |
| `/api/v1/products/categories` | GET | 商品分类列表 | 无需认证 |

### 4.2 核心接口详细设计

#### 4.2.1 创建商品

```
POST /api/v1/products

请求体:
{
  "name": "兰蔻小黑瓶精华液 50ml",
  "description": "兰蔻第二代小黑瓶精华，修护肌肤屏障...",
  "price": 799.00,
  "originalPrice": 1080.00,
  "imageUrl": "https://cdn.langtou.com/products/xxx.jpg",
  "images": [
    "https://cdn.langtou.com/products/xxx1.jpg",
    "https://cdn.langtou.com/products/xxx2.jpg"
  ],
  "link": "https://item.taobao.com/xxx",
  "linkType": "external",
  "categoryId": 102,
  "brand": "兰蔻"
}

响应 (成功):
{
  "code": 0,
  "data": {
    "id": 100001,
    "name": "兰蔻小黑瓶精华液 50ml",
    "status": 2,
    "statusText": "审核中",
    "createdAt": "2026-06-12T10:00:00Z"
  }
}
```

#### 4.2.2 商品详情

```
GET /api/v1/products/{id}

响应:
{
  "code": 0,
  "data": {
    "id": 100001,
    "name": "兰蔻小黑瓶精华液 50ml",
    "description": "兰蔻第二代小黑瓶精华，修护肌肤屏障...",
    "price": 799.00,
    "originalPrice": 1080.00,
    "imageUrl": "https://cdn.langtou.com/products/xxx.jpg",
    "images": [
      "https://cdn.langtou.com/products/xxx1.jpg",
      "https://cdn.langtou.com/products/xxx2.jpg"
    ],
    "link": "https://item.taobao.com/xxx",
    "categoryId": 102,
    "categoryName": "护肤 > 精华",
    "brand": "兰蔻",
    "creator": {
      "id": 5001,
      "name": "美妆小课堂",
      "avatar": "https://cdn.langtou.com/avatars/xxx.jpg",
      "followerCount": 125000,
      "productCount": 15,
      "isFollowed": false
    },
    "stats": {
      "clickCount": 5230,
      "salesCount": 128,
      "noteCount": 8
    },
    "relatedNotes": [
      {
        "id": 20001,
        "title": "我的护肤好物分享",
        "coverImage": "https://cdn.langtou.com/notes/xxx.jpg",
        "author": {"id": 5001, "name": "美妆小课堂"},
        "stats": {"likes": 1520, "comments": 230}
      }
    ]
  }
}
```

#### 4.2.3 创作者商品列表

```
GET /api/v1/creators/{id}/products

请求参数:
  - status (string, 可选): 商品状态 (all/on_shelf/off_shelf)，默认on_shelf
  - category_id (int, 可选): 分类筛选
  - page (int, 可选): 页码，默认1
  - page_size (int, 可选): 每页数量，默认20

响应:
{
  "code": 0,
  "data": {
    "creator": {
      "id": 5001,
      "name": "美妆小课堂",
      "avatar": "...",
      "productCount": 15
    },
    "products": [
      {
        "id": 100001,
        "name": "兰蔻小黑瓶精华液 50ml",
        "price": 799.00,
        "originalPrice": 1080.00,
        "imageUrl": "...",
        "clickCount": 5230,
        "status": 1
      }
    ],
    "total": 15,
    "page": 1,
    "pageSize": 20
  }
}
```

#### 4.2.4 笔记关联商品

```
POST /api/v1/notes/{noteId}/products

请求体:
{
  "productIds": [100001, 100002, 100003],
  "positions": [1, 2, 3]
}

响应:
{
  "code": 0,
  "data": {
    "noteId": 20001,
    "products": [
      {"productId": 100001, "position": 1},
      {"productId": 100002, "position": 2},
      {"productId": 100003, "position": 3}
    ]
  }
}

GET /api/v1/notes/{noteId}/products

响应:
{
  "code": 0,
  "data": {
    "noteId": 20001,
    "products": [
      {
        "productId": 100001,
        "name": "兰蔻小黑瓶精华液 50ml",
        "price": 799.00,
        "imageUrl": "...",
        "position": 1
      }
    ]
  }
}
```

#### 4.2.5 商品点击记录

```
POST /api/v1/products/{id}/click

请求体:
{
  "noteId": 20001,
  "source": "note_detail"
}

响应:
{
  "code": 0,
  "data": {
    "redirectUrl": "https://item.taobao.com/xxx",
    "clickId": "clk_20260612143000_001"
  }
}
```

---

## 5. 数据埋点设计

### 5.1 埋点事件清单

| 事件名 | 触发时机 | 参数 | 说明 |
|--------|----------|------|------|
| `product_create` | 创作者创建商品 | product_id, source(link/manual) | 商品创建 |
| `product_edit` | 创作者编辑商品 | product_id | 商品编辑 |
| `product_status_change` | 商品上架/下架 | product_id, old_status, new_status | 状态变更 |
| `product_showcase_view` | 用户查看橱窗页 | creator_id, product_count | 橱窗曝光 |
| `product_card_show` | 商品卡片曝光 | product_id, position, source(note/showcase) | 商品曝光 |
| `product_card_click` | 点击商品卡片 | product_id, note_id, source | 商品点击 |
| `product_detail_view` | 查看商品详情页 | product_id, creator_id | 详情页浏览 |
| `product_buy_click` | 点击"去购买"按钮 | product_id, note_id | 购买点击 |
| `note_product_add` | 笔记关联商品 | note_id, product_id | 关联操作 |
| `note_product_remove` | 笔记移除商品 | note_id, product_id | 移除操作 |

---

## 6. 数据指标

### 6.1 核心指标

| 指标 | 定义 | 计算方式 | 目标值 |
|------|------|----------|--------|
| 商品点击率(CTR) | 商品被点击的比例 | 商品点击数 / 商品曝光数 | > 5% |
| 笔记关联率 | 关联商品的笔记比例 | 关联商品的笔记数 / 总笔记数 | > 10% |
| 带货转化率 | 通过笔记带货产生的购买比例 | 购买点击数 / 商品曝光数 | > 0.5% |
| 创作者入驻率 | 开通橱窗的创作者比例 | 开通橱窗创作者数 / 总创作者数 | > 5% |

### 6.2 运营指标

| 指标 | 定义 | 用途 |
|------|------|------|
| 商品审核通过率 | 审核通过的商品比例 | 评估商品质量 |
| 人均橱窗商品数 | 创作者平均添加商品数 | 评估橱窗丰富度 |
| 商品曝光量 | 商品被展示的总次数 | 评估分发能力 |
| 笔记带货GMV | 通过笔记带货产生的交易额 | 评估商业化效果 |
| 创作者佣金收入 | 创作者通过带货获得的佣金 | 评估创作者变现效果 |

---

## 7. 非功能需求

| 需求 | 要求 |
|------|------|
| 商品详情页响应时间 | P99 < 200ms |
| 商品列表响应时间 | P99 < 150ms |
| 商品创建响应时间 | P99 < 500ms（含图片上传） |
| 商品图片加载 | CDN加速，首屏加载 < 1秒 |
| 商品审核时效 | 提交后24小时内完成审核 |
| 并发能力 | 支持3万QPS |

---

## 8. 里程碑与排期

| 阶段 | 时间 | 交付内容 |
|------|------|----------|
| **PRD评审** | Sprint 3 Week 1 | PRD评审通过 |
| **数据模型** | Sprint 3 Week 1 | 数据库表创建完成 |
| **后端开发** | Sprint 3 Week 1-3 | 商品CRUD+关联接口+点击追踪 |
| **前端开发** | Sprint 3 Week 2-3 | 橱窗管理页+笔记关联+商品详情页 |
| **联调测试** | Sprint 3 Week 3 | 前后端联调，功能测试通过 |
| **灰度发布** | Sprint 3 Week 4 | 50位种子创作者灰度 |
| **全量发布** | Sprint 4 Week 1 | 全部创作者开放 |

---

## 9. 风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| 商品链接解析失败 | 创作者添加商品体验差 | 中 | 支持手动填写兜底，逐步扩展解析能力 |
| 商品审核积压 | 商品上架延迟 | 中 | AI预审+人工复核，优先处理高粉丝创作者 |
| 外部链接失效 | 用户点击后无法购买 | 高 | 定期检测链接有效性，失效后通知创作者更新 |
| 创作者使用率低 | 橱窗功能价值未体现 | 中 | 运营激励首批创作者，提供选品指导 |
| MVP阶段佣金追踪不准 | 创作者收益不透明 | 高 | 明确MVP阶段为预估佣金，后续版本优化追踪 |

---

## 10. 后续版本规划

| 版本 | 功能 | 预计时间 |
|------|------|----------|
| **v2.0** | 站内交易系统（下单/支付/物流） | Sprint 5-6 |
| **v2.0** | 创作者选品中心（平台商品库） | Sprint 5-6 |
| **v2.0** | 品牌商家后台 | Sprint 5-6 |
| **v3.0** | 佣金自动结算与提现 | Sprint 7-8 |
| **v3.0** | 电商平台API对接（自动订单追踪） | Sprint 7-8 |
| **v3.0** | 带货数据分析看板 | Sprint 7-8 |
| **v4.0** | 直播带货 | Sprint 9-10 |
| **v4.0** | C2C二手交易 | Sprint 9-10 |

---

## 11. 附录

### 11.1 术语表

| 术语 | 解释 |
|------|------|
| 商品橱窗 | 创作者个人主页中展示推荐商品的模块 |
| 种草 | 通过内容推荐激发用户购买欲望的行为 |
| 拔草 | 用户被种草后完成购买的行为 |
| GMV | Gross Merchandise Volume，商品交易总额 |
| 分佣/佣金 | 创作者通过带货获得的收入分成 |
| SKU | Stock Keeping Unit，商品库存单位 |
| 选品中心 | 平台提供的商品库，创作者可从中选择商品添加到橱窗 |

### 11.2 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| v1.0 | 2026-06-12 | 初稿创建 | 产品团队 |

---

**文档结束**
