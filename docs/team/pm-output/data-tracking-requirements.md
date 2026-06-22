# 榔头(Langtou)内容社区平台 v7.0 数据埋点需求文档

**版本**: v7.0  
**日期**: 2026-06-12  
**编制**: 榔头产品团队 / 数据团队  
**状态**: 评审中  

---

## 1. 文档概述

### 1.1 目的
本文档定义榔头(Langtou)内容社区平台 v7.0 版本的核心数据埋点需求，为产品决策、算法优化、商业变现提供数据支撑。

### 1.2 适用范围
- 前端: React Native 移动端（iOS/Android）
- 后台: Web Admin 管理后台
- 服务端: 5个微服务（Gateway/User/Content/Interact/Message）

### 1.3 埋点规范

| 规范项 | 说明 |
|--------|------|
| **埋点方式** | 代码埋点为主，辅以服务端日志采集 |
| **上报协议** | HTTP POST，JSON格式，批量上报 |
| **上报时机** | 事件触发后实时上报，网络异常时本地缓存，恢复后补报 |
| **上报频率** | 单条事件即时上报；批量上报每30秒或满50条触发 |
| **去重策略** | 事件唯一ID（event_id = UUID），服务端幂等处理 |
| **公共属性** | 所有事件必须携带公共属性（见2.1节） |
| **版本管理** | 事件定义变更需升级版本号，向后兼容至少2个版本 |

### 1.4 事件命名规范

```
{业务域}_{动作}_{对象}

业务域: app(客户端) / ad(广告) / creator(创作者) / ecom(电商) / live(直播)
动作: expose(曝光) / click(点击) / stay(停留) / play(播放) / like(点赞) / share(分享) / collect(收藏) / comment(评论) / follow(关注) / publish(发布) / buy(购买) / search(搜索)
对象: feed(信息流) / content(内容) / video(视频) / image(图片) / user(用户) / page(页面) / ad(广告) / product(商品) / live(直播)
```

---

## 2. 公共属性定义

### 2.1 所有事件必须携带的公共属性

| 属性名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| `event_id` | String | 事件唯一标识，UUID | "evt_abc123def456" |
| `event_name` | String | 事件名称 | "app_expose_feed" |
| `event_time` | Long | 事件触发时间戳(毫秒) | 1718185600000 |
| `event_source` | String | 事件来源: client/server | "client" |
| `app_version` | String | App版本号 | "7.0.0" |
| `app_channel` | String | 应用渠道 | "AppStore" / "Huawei" / "Xiaomi" |
| `device_id` | String | 设备唯一标识 | "dev_xyz789" |
| `user_id` | String | 用户ID(未登录传空) | "u_123456" |
| `session_id` | String | 会话ID | "sess_abc789" |
| `network_type` | String | 网络类型 | "wifi" / "4g" / "5g" / "unknown" |
| `os_type` | String | 操作系统 | "iOS" / "Android" |
| `os_version` | String | 系统版本 | "17.0" |
| `device_model` | String | 设备型号 | "iPhone15,2" |
| `screen_width` | Int | 屏幕宽度 | 390 |
| `screen_height` | Int | 屏幕高度 | 844 |
| `page_id` | String | 当前页面标识 | "home_feed" |
| `page_url` | String | 当前页面路径 | "/pages/home/feed" |
| `referrer_page` | String | 来源页面 | "home_tab" |
| `ab_test_group` | String | A/B测试分组标识 | "rec_algo_v2_exp" |

### 2.2 服务端补充属性

服务端接收到客户端上报后，补充以下属性：

| 属性名 | 类型 | 说明 | 来源 |
|--------|------|------|------|
| `server_time` | Long | 服务端接收时间戳 | 服务端生成 |
| `ip_address` | String | 用户IP地址 | 服务端提取 |
| `geo_country` | String | 国家 | IP解析 |
| `geo_province` | String | 省份 | IP解析 |
| `geo_city` | String | 城市 | IP解析 |
| `user_type` | String | 用户类型: new/returning/churned | 用户画像服务 |
| `user_vip_level` | Int | 用户VIP等级 | 用户画像服务 |
| `creator_level` | String | 创作者等级(非创作者为空) | 用户画像服务 |

---

## 3. 用户行为事件埋点

### 3.1 页面曝光事件

#### 3.1.1 页面浏览曝光 (app_expose_page)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page_id` | String | 是 | 页面标识，见公共属性 |
| `page_type` | String | 是 | 页面类型: feed/detail/profile/search/message/publish/mine |
| `enter_type` | String | 是 | 进入方式: auto(自动)/click(点击)/push(推送)/deeplink |
| `stay_duration` | Long | 否 | 页面停留时长(毫秒)，页面离开时补发 |
| `load_duration` | Long | 否 | 页面加载时长(毫秒) |

**触发时机**: 页面可见时触发（onShow/onAppear）

**漏斗定义**: 
- 漏斗名称: 核心页面访问漏斗
- 步骤: 启动App -> 首页曝光 -> 内容详情页曝光 -> 互动行为
- 计算方式: 相邻步骤转化人数/上一步人数

---

### 3.2 内容曝光事件

#### 3.2.1 信息流内容曝光 (app_expose_feed)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容唯一ID |
| `content_type` | String | 是 | 内容类型: note(图文)/video(视频)/live(直播) |
| `author_id` | String | 是 | 作者用户ID |
| `position` | Int | 是 | 内容在列表中的位置(从0开始) |
| `rec_trace_id` | String | 是 | 推荐追踪ID，用于关联曝光-点击-转化 |
| `rec_algorithm` | String | 是 | 推荐算法标识: cf(协同过滤)/vector(向量)/hot(热度)/follow(关注)/operation(运营) |
| `rec_score` | Double | 否 | 推荐排序分数 |
| `is_followed` | Boolean | 是 | 用户是否已关注该作者 |
| `exposure_duration` | Long | 否 | 内容在屏幕可见时长(毫秒) |
| `is_full_exposure` | Boolean | 是 | 是否完整曝光(内容50%以上可见超过1秒) |

**触发时机**: 内容卡片在屏幕中50%以上可见且停留超过1秒时触发

**漏斗定义**:
- 漏斗名称: 内容消费漏斗
- 步骤: 内容曝光 -> 内容点击 -> 内容详情页加载完成 -> 内容消费(播放/阅读) -> 互动行为(点赞/评论/收藏)
- 计算方式: 每个步骤的事件数/第一步曝光事件数

#### 3.2.2 搜索结果曝光 (app_expose_search_result)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `search_keyword` | String | 是 | 搜索关键词 |
| `search_type` | String | 是 | 搜索类型: keyword(关键词)/suggest(推荐)/history(历史) |
| `result_position` | Int | 是 | 结果位置 |
| `search_request_id` | String | 是 | 搜索请求唯一ID |

**触发时机**: 搜索结果在屏幕可见时触发

---

### 3.3 点击事件

#### 3.3.1 内容点击 (app_click_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `content_type` | String | 是 | 内容类型 |
| `author_id` | String | 是 | 作者ID |
| `click_position` | Int | 是 | 点击位置: 0-封面/1-标题/2-作者/3-更多按钮 |
| `rec_trace_id` | String | 是 | 推荐追踪ID(从曝光事件透传) |
| `source_page` | String | 是 | 来源页面: home_feed/search_result/profile/notification |

**触发时机**: 用户点击内容卡片时触发

#### 3.3.2 按钮点击通用事件 (app_click_button)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `button_id` | String | 是 | 按钮唯一标识 |
| `button_name` | String | 是 | 按钮名称 |
| `button_type` | String | 是 | 按钮类型: primary/secondary/icon/text |
| `target_page` | String | 否 | 点击后跳转页面 |
| `extra_params` | JSON | 否 | 额外参数 |

**触发时机**: 用户点击可交互按钮时触发

---

### 3.4 停留/浏览事件

#### 3.4.1 内容详情页停留 (app_stay_content_detail)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `content_type` | String | 是 | 内容类型 |
| `stay_duration` | Long | 是 | 停留总时长(毫秒) |
| `read_ratio` | Float | 否 | 阅读/播放完成比例: 0-1 |
| `max_scroll_depth` | Float | 否 | 最大滚动深度: 0-1 |
| `is_bounce` | Boolean | 是 | 是否跳出(停留<3秒且无互动) |
| `image_view_count` | Int | 否 | 浏览图片数量(图文内容) |
| `image_view_duration` | Long | 否 | 图片浏览总时长 |

**触发时机**: 用户离开内容详情页时触发（onHide/onDisappear）

**漏斗定义**:
- 漏斗名称: 内容深度消费漏斗
- 步骤: 进入详情页 -> 停留>10秒 -> 停留>30秒 -> 阅读/播放完成 -> 互动
- 计算方式: 会话级别去重后计算

---

## 4. 内容消费事件埋点

### 4.1 视频播放事件

#### 4.1.1 视频开始播放 (app_play_video_start)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 视频内容ID |
| `author_id` | String | 是 | 作者ID |
| `video_duration` | Long | 是 | 视频总时长(毫秒) |
| `video_resolution` | String | 是 | 视频分辨率: 720p/1080p/4k |
| `play_source` | String | 是 | 播放来源: auto(自动播放)/click(点击播放) |
| `rec_trace_id` | String | 是 | 推荐追踪ID |
| `network_type` | String | 是 | 当前网络类型 |
| `buffer_duration` | Long | 否 | 缓冲时长(毫秒) |

**触发时机**: 视频开始播放时触发

#### 4.1.2 视频播放进度 (app_play_video_progress)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 视频内容ID |
| `progress_ratio` | Float | 是 | 当前播放进度比例: 0/0.25/0.5/0.75/0.9/1.0 |
| `play_duration` | Long | 是 | 已播放时长(毫秒) |
| `is_replay` | Boolean | 是 | 是否重播 |

**触发时机**: 播放进度达到0%、25%、50%、75%、90%、100%时触发

#### 4.1.3 视频播放结束 (app_play_video_end)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 视频内容ID |
| `play_duration` | Long | 是 | 实际播放时长(毫秒) |
| `completion_ratio` | Float | 是 | 完成比例: 0-1 |
| `end_reason` | String | 是 | 结束原因: complete(播完)/user_exit(用户退出)/error(错误)/switch(切换) |
| `pause_count` | Int | 否 | 暂停次数 |
| `seek_count` | Int | 否 | 拖动进度条次数 |
| `speed_mode` | String | 否 | 播放速度: 0.5x/1.0x/1.5x/2.0x |

**触发时机**: 视频播放结束时触发

**漏斗定义**:
- 漏斗名称: 视频完播漏斗
- 步骤: 视频曝光 -> 开始播放 -> 播放25% -> 播放50% -> 播放75% -> 播放完成
- 计算方式: 去重用户计算各阶段转化率

---

### 4.2 互动事件

#### 4.2.1 点赞 (app_like_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `content_type` | String | 是 | 内容类型 |
| `author_id` | String | 是 | 作者ID |
| `action` | String | 是 | 动作: like(点赞)/unlike(取消点赞) |
| `trigger_position` | String | 是 | 触发位置: feed(信息流)/detail(详情页)/profile(个人主页) |
| `rec_trace_id` | String | 是 | 推荐追踪ID |
| `like_duration` | Long | 否 | 从曝光到点赞的时长(毫秒) |

**触发时机**: 用户点击点赞按钮时触发

#### 4.2.2 评论 (app_comment_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `author_id` | String | 是 | 作者ID |
| `comment_id` | String | 是 | 评论唯一ID |
| `parent_comment_id` | String | 否 | 父评论ID(回复评论时) |
| `comment_type` | String | 是 | 评论类型: text(纯文本)/image(带图)/mention(@用户) |
| `comment_length` | Int | 是 | 评论字符数 |
| `has_mention` | Boolean | 是 | 是否@用户 |
| `mention_count` | Int | 否 | @用户数量 |
| `trigger_position` | String | 是 | 触发位置 |

**触发时机**: 用户成功提交评论时触发

#### 4.2.3 收藏 (app_collect_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `content_type` | String | 是 | 内容类型 |
| `author_id` | String | 是 | 作者ID |
| `action` | String | 是 | 动作: collect(收藏)/uncollect(取消收藏) |
| `collect_folder_id` | String | 否 | 收藏夹ID |
| `trigger_position` | String | 是 | 触发位置 |

**触发时机**: 用户点击收藏按钮时触发

#### 4.2.4 分享 (app_share_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 内容ID |
| `content_type` | String | 是 | 内容类型 |
| `author_id` | String | 是 | 作者ID |
| `share_channel` | String | 是 | 分享渠道: wechat_session(微信好友)/wechat_timeline(朋友圈)/qq/weibo/copy_link/system |
| `share_type` | String | 是 | 分享类型: card(卡片)/link(链接)/image(图片)/video(视频) |
| `trigger_position` | String | 是 | 触发位置 |

**触发时机**: 用户点击分享按钮并选择分享渠道时触发

#### 4.2.5 关注 (app_follow_user)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `target_user_id` | String | 是 | 被关注用户ID |
| `target_user_type` | String | 是 | 被关注用户类型: creator(创作者)/normal(普通用户)/official(官方账号) |
| `action` | String | 是 | 动作: follow(关注)/unfollow(取消关注) |
| `trigger_position` | String | 是 | 触发位置: feed/detail/profile/search/recommend |
| `follow_source` | String | 是 | 关注来源: content(内容页)/profile(主页)/list(推荐列表) |

**触发时机**: 用户点击关注/取消关注按钮时触发

---

### 4.3 内容发布事件

#### 4.3.1 发布内容 (app_publish_content)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content_id` | String | 是 | 发布内容ID |
| `content_type` | String | 是 | 内容类型: note/video |
| `image_count` | Int | 否 | 图片数量 |
| `video_duration` | Long | 否 | 视频时长(毫秒) |
| `has_text` | Boolean | 是 | 是否有文字描述 |
| `text_length` | Int | 否 | 文字描述长度 |
| `has_topic` | Boolean | 是 | 是否添加话题 |
| `topic_count` | Int | 否 | 话题数量 |
| `has_location` | Boolean | 是 | 是否添加定位 |
| `has_product` | Boolean | 是 | 是否挂载商品(v7.0新增) |
| `product_count` | Int | 否 | 挂载商品数量 |
| `publish_duration` | Long | 否 | 从进入发布页到发布成功的时长 |
| `edit_duration` | Long | 否 | 编辑内容时长 |
| `is_draft` | Boolean | 是 | 是否从草稿箱发布 |
| `publish_result` | String | 是 | 发布结果: success/fail |
| `fail_reason` | String | 否 | 失败原因 |

**触发时机**: 用户点击发布按钮，服务端返回成功/失败时触发

**漏斗定义**:
- 漏斗名称: 内容发布漏斗
- 步骤: 打开发布页 -> 选择图片/视频 -> 编辑内容 -> 添加话题/定位 -> 点击发布 -> 发布成功
- 计算方式: 会话级别去重

---

## 5. 商业指标事件埋点

### 5.1 广告曝光事件

#### 5.1.1 广告曝光 (ad_expose_ad)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ad_id` | String | 是 | 广告唯一ID |
| `ad_type` | String | 是 | 广告类型: feed_native(信息流原生)/feed_video(信息流视频)/splash(开屏)/banner(横幅)/interstitial(插屏) |
| `ad_position` | Int | 是 | 广告在列表中的位置 |
| `campaign_id` | String | 是 | 广告计划ID |
| `advertiser_id` | String | 是 | 广告主ID |
| `material_id` | String | 是 | 广告素材ID |
| `material_type` | String | 是 | 素材类型: image/video/carousel |
| `is_full_exposure` | Boolean | 是 | 是否完整曝光 |
| `exposure_duration` | Long | 否 | 曝光时长(毫秒) |
| `rec_trace_id` | String | 是 | 推荐追踪ID |

**触发时机**: 广告在屏幕50%以上可见且停留超过1秒时触发

**漏斗定义**:
- 漏斗名称: 广告转化漏斗
- 步骤: 广告曝光 -> 广告点击 -> 落地页加载 -> 转化行为
- 计算方式: 广告事件独立计算

### 5.2 广告点击事件

#### 5.2.1 广告点击 (ad_click_ad)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ad_id` | String | 是 | 广告ID |
| `ad_type` | String | 是 | 广告类型 |
| `click_position` | Int | 是 | 点击位置: 0-素材/1-标题/2-按钮 |
| `campaign_id` | String | 是 | 广告计划ID |
| `advertiser_id` | String | 是 | 广告主ID |
| `material_id` | String | 是 | 素材ID |
| `rec_trace_id` | String | 是 | 推荐追踪ID |
| `landing_url` | String | 是 | 落地页URL |

**触发时机**: 用户点击广告时触发

### 5.3 广告转化事件

#### 5.3.1 广告转化 (ad_convert_ad)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ad_id` | String | 是 | 广告ID |
| `campaign_id` | String | 是 | 广告计划ID |
| `advertiser_id` | String | 是 | 广告主ID |
| `convert_type` | String | 是 | 转化类型: download(下载)/activate(激活)/register(注册)/purchase(购买)/form_submit(表单提交) |
| `convert_value` | Double | 否 | 转化价值(金额) |
| `convert_time` | Long | 是 | 转化时间戳 |
| `attribution_window` | Int | 是 | 归因窗口(小时): 24/48/72 |

**触发时机**: 用户完成广告目标行为时触发（需与广告平台做归因对接）

---

## 6. 电商带货事件埋点(v7.0新增)

### 6.1 商品曝光事件

#### 6.1.1 商品曝光 (ecom_expose_product)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `product_id` | String | 是 | 商品唯一ID |
| `sku_id` | String | 否 | SKU ID |
| `content_id` | String | 否 | 关联内容ID(内容带货场景) |
| `author_id` | String | 否 | 带货创作者ID |
| `exposure_scene` | String | 是 | 曝光场景: content_detail(内容详情页)/shop_window(商品橱窗)/search_result(搜索结果)/recommend(推荐) |
| `position` | Int | 是 | 曝光位置 |
| `product_price` | Double | 是 | 商品售价 |
| `product_category` | String | 是 | 商品类目 |

**触发时机**: 商品在屏幕可见时触发

### 6.2 商品点击事件

#### 6.2.1 商品点击 (ecom_click_product)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `product_id` | String | 是 | 商品ID |
| `content_id` | String | 否 | 关联内容ID |
| `author_id` | String | 否 | 带货创作者ID |
| `click_scene` | String | 是 | 点击场景 |
| `source_page` | String | 是 | 来源页面 |

**触发时机**: 用户点击商品时触发

### 6.3 商品转化事件

#### 6.3.1 加入购物车 (ecom_add_cart)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `product_id` | String | 是 | 商品ID |
| `sku_id` | String | 是 | SKU ID |
| `quantity` | Int | 是 | 数量 |
| `product_price` | Double | 是 | 单价 |
| `content_id` | String | 否 | 关联内容ID |
| `author_id` | String | 否 | 带货创作者ID |

**触发时机**: 用户点击加入购物车时触发

#### 6.3.2 下单 (ecom_place_order)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `order_id` | String | 是 | 订单ID |
| `product_ids` | Array | 是 | 商品ID列表 |
| `total_amount` | Double | 是 | 订单总金额 |
| `discount_amount` | Double | 否 | 优惠金额 |
| `pay_amount` | Double | 是 | 实付金额 |
| `content_id` | String | 否 | 关联内容ID |
| `author_id` | String | 否 | 带货创作者ID |
| `order_source` | String | 是 | 订单来源: content(内容带货)/shop(店铺)/search(搜索) |

**触发时机**: 用户提交订单时触发

#### 6.3.3 支付成功 (ecom_pay_success)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `order_id` | String | 是 | 订单ID |
| `pay_amount` | Double | 是 | 实付金额 |
| `pay_channel` | String | 是 | 支付渠道: alipay/wechat/apple_pay |
| `pay_time` | Long | 是 | 支付时间戳 |
| `content_id` | String | 否 | 关联内容ID |
| `author_id` | String | 否 | 带货创作者ID |

**触发时机**: 支付成功回调时触发

**漏斗定义**:
- 漏斗名称: 电商转化漏斗
- 步骤: 商品曝光 -> 商品点击 -> 加入购物车 -> 提交订单 -> 支付成功
- 计算方式: 按用户去重计算各步骤转化率

---

## 7. 搜索事件埋点

### 7.1 搜索行为事件

#### 7.1.1 搜索发起 (app_search_query)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `search_keyword` | String | 是 | 搜索关键词 |
| `search_type` | String | 是 | 搜索类型: text(文本)/voice(语音)/image(以图搜图) |
| `suggest_keyword` | String | 否 | 用户点击的搜索建议词 |
| `search_request_id` | String | 是 | 搜索请求唯一ID |
| `result_count` | Int | 否 | 返回结果数量 |

**触发时机**: 用户发起搜索请求时触发

#### 7.1.2 搜索建议点击 (app_click_search_suggest)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `suggest_keyword` | String | 是 | 建议词 |
| `suggest_type` | String | 是 | 建议类型: hot(热搜)/history(历史)/recommend(推荐)/associate(联想) |
| `position` | Int | 是 | 建议词位置 |

**触发时机**: 用户点击搜索建议时触发

---

## 8. 创作者相关事件埋点

### 8.1 创作者数据事件

#### 8.1.1 创作者中心访问 (creator_visit_dashboard)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `creator_level` | String | 是 | 创作者等级 |
| `fan_count` | Int | 是 | 粉丝数 |
| `visit_page` | String | 是 | 访问页面: overview/data/content/fan/earning |

**触发时机**: 创作者进入创作者中心各页面时触发

#### 8.1.2 创作者数据查看 (creator_view_data)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `data_type` | String | 是 | 数据类型: play/like/comment/share/follow/earning |
| `time_range` | String | 是 | 时间范围: 7d/30d/90d |

**触发时机**: 创作者查看数据报表时触发

---

## 9. 系统与性能事件埋点

### 9.1 应用性能事件

#### 9.1.1 应用启动 (app_launch)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `launch_type` | String | 是 | 启动类型: cold(冷启动)/hot(热启动) |
| `launch_duration` | Long | 是 | 启动耗时(毫秒) |
| `is_first_launch` | Boolean | 是 | 是否首次启动 |
| `launch_source` | String | 是 | 启动来源: direct(直接)/push(推送)/deeplink(深度链接)/widget(小组件) |

**触发时机**: 应用启动完成时触发

#### 9.1.2 页面性能 (app_page_performance)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page_id` | String | 是 | 页面标识 |
| `ttfb` | Long | 否 | Time To First Byte(毫秒) |
| `fcp` | Long | 否 | First Contentful Paint(毫秒) |
| `lcp` | Long | 否 | Largest Contentful Paint(毫秒) |
| `fid` | Long | 否 | First Input Delay(毫秒) |
| `cls` | Float | 否 | Cumulative Layout Shift |

**触发时机**: 页面加载完成后触发

#### 9.1.3 错误上报 (app_error)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `error_type` | String | 是 | 错误类型: crash(崩溃)/js_error(JS错误)/api_error(接口错误)/network_error(网络错误) |
| `error_code` | String | 否 | 错误码 |
| `error_message` | String | 否 | 错误信息 |
| `error_stack` | String | 否 | 错误堆栈 |
| `page_id` | String | 是 | 发生错误的页面 |

**触发时机**: 发生错误时触发

---

## 10. 事件汇总表

### 10.1 事件清单总览

| 事件名 | 事件中文名 | 业务域 | 触发时机 | 优先级 |
|--------|-----------|--------|----------|--------|
| app_expose_page | 页面浏览曝光 | 用户行为 | 页面可见时 | P0 |
| app_expose_feed | 信息流内容曝光 | 用户行为 | 内容50%可见超1秒 | P0 |
| app_expose_search_result | 搜索结果曝光 | 用户行为 | 结果可见时 | P1 |
| app_click_content | 内容点击 | 用户行为 | 点击内容卡片 | P0 |
| app_click_button | 按钮点击 | 用户行为 | 点击按钮 | P1 |
| app_stay_content_detail | 内容详情页停留 | 用户行为 | 离开详情页 | P0 |
| app_play_video_start | 视频开始播放 | 内容消费 | 视频开始播放 | P0 |
| app_play_video_progress | 视频播放进度 | 内容消费 | 播放进度节点 | P0 |
| app_play_video_end | 视频播放结束 | 内容消费 | 视频播放结束 | P0 |
| app_like_content | 点赞 | 内容消费 | 点击点赞 | P0 |
| app_comment_content | 评论 | 内容消费 | 提交评论 | P0 |
| app_collect_content | 收藏 | 内容消费 | 点击收藏 | P0 |
| app_share_content | 分享 | 内容消费 | 选择分享渠道 | P0 |
| app_follow_user | 关注 | 内容消费 | 点击关注 | P0 |
| app_publish_content | 发布内容 | 内容消费 | 发布成功/失败 | P0 |
| app_search_query | 搜索发起 | 搜索 | 发起搜索 | P1 |
| app_click_search_suggest | 搜索建议点击 | 搜索 | 点击建议词 | P1 |
| ad_expose_ad | 广告曝光 | 商业 | 广告50%可见超1秒 | P0 |
| ad_click_ad | 广告点击 | 商业 | 点击广告 | P0 |
| ad_convert_ad | 广告转化 | 商业 | 完成转化行为 | P0 |
| ecom_expose_product | 商品曝光 | 电商 | 商品可见时 | P0 |
| ecom_click_product | 商品点击 | 电商 | 点击商品 | P0 |
| ecom_add_cart | 加入购物车 | 电商 | 点击加购 | P0 |
| ecom_place_order | 下单 | 电商 | 提交订单 | P0 |
| ecom_pay_success | 支付成功 | 电商 | 支付成功 | P0 |
| creator_visit_dashboard | 创作者中心访问 | 创作者 | 进入创作者中心 | P1 |
| creator_view_data | 创作者数据查看 | 创作者 | 查看数据报表 | P1 |
| app_launch | 应用启动 | 系统 | 应用启动完成 | P0 |
| app_page_performance | 页面性能 | 系统 | 页面加载完成 | P1 |
| app_error | 错误上报 | 系统 | 发生错误 | P0 |

### 10.2 关键漏斗定义汇总

| 漏斗名称 | 漏斗步骤 | 适用场景 |
|----------|----------|----------|
| 核心页面访问漏斗 | 启动App -> 首页曝光 -> 内容详情页曝光 -> 互动行为 | 用户活跃度分析 |
| 内容消费漏斗 | 内容曝光 -> 内容点击 -> 详情页加载 -> 内容消费 -> 互动 | 内容质量评估 |
| 内容深度消费漏斗 | 进入详情页 -> 停留>10秒 -> 停留>30秒 -> 消费完成 -> 互动 | 内容吸引力分析 |
| 视频完播漏斗 | 视频曝光 -> 开始播放 -> 25% -> 50% -> 75% -> 完成 | 视频质量评估 |
| 内容发布漏斗 | 打开发布页 -> 选择素材 -> 编辑 -> 添加标签 -> 点击发布 -> 成功 | 创作流程优化 |
| 广告转化漏斗 | 广告曝光 -> 广告点击 -> 落地页加载 -> 转化行为 | 广告效果评估 |
| 电商转化漏斗 | 商品曝光 -> 商品点击 -> 加购 -> 下单 -> 支付成功 | 电商转化分析 |

---

## 11. 数据上报接口规范

### 11.1 上报接口

```
POST /api/v1/track/batch
Content-Type: application/json
Authorization: Bearer {token}
```

### 11.2 请求体格式

```json
{
  "device_id": "dev_xyz789",
  "user_id": "u_123456",
  "session_id": "sess_abc789",
  "app_version": "7.0.0",
  "events": [
    {
      "event_id": "evt_abc123def456",
      "event_name": "app_expose_feed",
      "event_time": 1718185600000,
      "event_source": "client",
      "properties": {
        "content_id": "c_789012",
        "content_type": "video",
        "author_id": "u_345678",
        "position": 3,
        "rec_trace_id": "rec_20240612_001",
        "rec_algorithm": "vector",
        "is_full_exposure": true,
        "exposure_duration": 3200
      }
    }
  ]
}
```

### 11.3 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accepted_count": 1,
    "rejected_events": []
  }
}
```

### 11.4 服务端日志采集

服务端通过日志框架自动采集以下事件：

| 事件 | 采集方式 | 日志级别 |
|------|----------|----------|
| API请求 | 网关拦截器 | INFO |
| 用户注册/登录 | User服务日志 | INFO |
| 内容审核状态变更 | Content服务日志 | INFO |
| 订单状态变更 | 电商服务日志 | INFO |
| 系统异常 | 全局异常处理器 | ERROR |

---

## 12. 数据存储与计算

### 12.1 数据流架构

```
客户端埋点 / 服务端日志
  -> Kafka (消息队列)
    -> Flink (实时清洗/ETL)
      -> ClickHouse (实时OLAP查询)
      -> HDFS/S3 (离线数据湖)
        -> Spark (离线计算/T+1报表)
          -> MySQL/Redis (结果存储)
            -> BI报表 / 数据大屏 / 算法特征
```

### 12.2 数据分层

| 层级 | 名称 | 说明 | 存储 |
|------|------|------|------|
| ODS | 原始数据层 | 原始埋点数据，不做清洗 | Kafka/HDFS |
| DWD | 明细数据层 | 清洗后的标准事件明细 | HDFS/ClickHouse |
| DWS | 汇总数据层 | 按主题汇总的数据 | ClickHouse/MySQL |
| ADS | 应用数据层 | 面向应用的数据产品 | MySQL/Redis/ES |

### 12.3 数据保留策略

| 数据类型 | 保留时长 | 说明 |
|----------|----------|------|
| 原始埋点数据 | 90天 | 用于问题排查和数据回溯 |
| 清洗后明细数据 | 1年 | 用于灵活分析 |
| 汇总数据 | 3年 | 用于趋势分析 |
| 报表数据 | 永久 | 压缩存储 |

---

## 13. 数据质量保障

### 13.1 数据校验规则

| 校验项 | 规则 | 处理方式 |
|--------|------|----------|
| 必填字段 | 公共属性必须完整 | 缺失则标记异常，不入库 |
| 时间戳 | event_time不得晚于server_time超过5分钟 | 异常则标记，延迟处理 |
| 用户ID | user_id格式校验 | 格式错误则丢弃 |
| 事件名 | 必须在白名单内 | 未知事件进入待审核队列 |
| 去重 | event_id全局唯一 | 重复事件幂等处理 |

### 13.2 监控告警

| 监控项 | 阈值 | 告警方式 |
|--------|------|----------|
| 埋点丢失率 | > 1% | 钉钉告警 |
| 事件延迟上报率 | > 5% | 钉钉告警 |
| 异常事件占比 | > 0.1% | 钉钉告警 |
| 服务端日志丢失 | > 0.1% | 钉钉告警+电话 |
| 数据流积压 | > 10分钟 | 钉钉告警 |

---

## 14. 隐私合规

### 14.1 数据采集原则

- 最小必要原则: 仅采集业务必需的数据
- 明示同意原则: 敏感数据采集前需用户授权
- 数据安全原则: 传输加密，存储脱敏

### 14.2 用户权利支持

| 权利 | 实现方式 |
|------|----------|
| 知情权 | 隐私政策明确告知采集范围 |
| 访问权 | 提供个人数据导出功能 |
| 删除权 | 账号注销后删除个人数据 |
| 撤回同意 | 设置中可关闭个性化推荐/广告追踪 |

### 14.3 敏感数据处理

| 数据类型 | 处理方式 |
|----------|----------|
| 设备ID | 本地生成，不上传原始硬件标识 |
| 位置信息 | 用户授权后采集，精度降级到城市级 |
| 行为数据 | 关联用户ID，注销后匿名化 |

---

## 15. 附录

### 15.1 页面标识(Page ID)清单

| page_id | 页面名称 | 所属模块 |
|---------|----------|----------|
| home_feed | 首页信息流 | 首页 |
| discover | 发现页 | 发现 |
| search_result | 搜索结果页 | 搜索 |
| content_detail | 内容详情页 | 内容 |
| user_profile | 用户个人主页 | 用户 |
| message_list | 消息列表页 | 消息 |
| message_chat | 私信聊天页 | 消息 |
| publish_edit | 发布编辑页 | 发布 |
| mine | 我的页面 | 个人中心 |
| creator_dashboard | 创作者中心 | 创作者 |
| product_detail | 商品详情页 | 电商 |
| shop_window | 商品橱窗页 | 电商 |
| order_list | 订单列表页 | 电商 |
| ad_detail | 广告落地页 | 广告 |

### 15.2 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| v0.1 | 2026-06-12 | 初稿创建，包含v7.0新增电商埋点 | 数据团队 |

---

**文档结束**
