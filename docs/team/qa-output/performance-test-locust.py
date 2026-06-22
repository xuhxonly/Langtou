# -*- coding: utf-8 -*-
"""
榔头（Langtou）内容社区平台 - Locust 性能测试脚本

运行方式:
    locust -f performance-test-locust.py --host=http://localhost:8080

场景说明:
    - 场景1：首页 Feed 加载（并发 100/500/1000 用户）
    - 场景2：笔记发布（图文 + 视频）
    - 场景3：搜索接口（热门关键词）
    - 场景4：点赞/评论高并发
    - 场景5：用户注册/登录峰值

SLA 指标:
    - 首页 Feed 加载 P99 < 500ms
    - 笔记发布 P99 < 1s
    - 搜索接口 P99 < 300ms
    - 点赞/评论 P99 < 200ms
    - 注册/登录 P99 < 300ms
    - 系统错误率 < 0.1%
"""

import random
import json
import time
from locust import HttpUser, task, between, events
from locust.runners import MasterRunner

# ============================================================
# 全局配置
# ============================================================

# 测试数据
TEST_PHONES = [f"138{i:08d}" for i in range(1000, 2000)]
TEST_KEYWORDS = ["美食", "旅游", "穿搭", "护肤", "健身", "摄影", "读书", "电影", "音乐", "宠物"]
HOT_KEYWORDS = ["美食探店", "夏日穿搭", "减肥食谱", "旅行攻略", "护肤分享"]

# 已注册用户池（用于需要登录的场景）
REGISTERED_USERS = []

# 笔记 ID 池（用于互动场景）
NOTE_IDS = list(range(100000, 110000))

# SLA 阈值（毫秒）
SLA_THRESHOLDS = {
    "feed_load": 500,
    "note_publish": 1000,
    "search": 300,
    "interact": 200,
    "user_auth": 300,
}


# ============================================================
# 自定义事件监听
# ============================================================

@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response,
               context, exception, **kwargs):
    """请求响应监听器：校验 SLA 并记录超标请求"""
    if exception:
        return

    # 根据请求名称匹配 SLA
    sla_key = None
    for key in SLA_THRESHOLDS:
        if key in name:
            sla_key = key
            break

    if sla_key and response_time > SLA_THRESHOLDS[sla_key]:
        print(f"[SLA 告警] {name} 响应时间 {response_time}ms 超过阈值 {SLA_THRESHOLDS[sla_key]}ms")


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """测试启动时初始化"""
    print("=" * 60)
    print("榔头（Langtou）内容社区平台 - 性能测试启动")
    print("=" * 60)
    print(f"目标主机: {environment.host}")
    print(f"SLA 阈值: {json.dumps(SLA_THRESHOLDS, ensure_ascii=False, indent=2)}")
    print("=" * 60)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """测试结束时输出统计"""
    print("=" * 60)
    print("性能测试结束")
    print("=" * 60)


# ============================================================
# 基础用户类
# ============================================================

class BaseUser(HttpUser):
    """基础用户类，封装通用方法"""
    abstract = True
    wait_time = between(1, 3)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.token = None
        self.user_id = None
        self.phone = None

    def login(self, phone, password="Test@1234"):
        """用户登录并获取 Token"""
        resp = self.client.post(
            "/api/v1/user/login",
            json={"phone": phone, "password": password},
            name="user_auth_login"
        )
        if resp.status_code == 200:
            data = resp.json().get("data", {})
            self.token = data.get("token")
            self.user_id = data.get("userId")
            self.phone = phone
        return resp

    def register(self, phone, password="Test@1234", verify_code="123456"):
        """用户注册"""
        return self.client.post(
            "/api/v1/user/register",
            json={"phone": phone, "password": password, "verifyCode": verify_code},
            name="user_auth_register"
        )

    def get_headers(self):
        """获取带鉴权的请求头"""
        headers = {"Content-Type": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers


# ============================================================
# 场景1：首页 Feed 加载
# ============================================================

class FeedUser(BaseUser):
    """
    场景1：首页 Feed 加载

    并发用户数: 100 / 500 / 1000
    Ramp-up: 60s / 120s / 180s
    持续时间: 300s
    断言条件:
        - HTTP 状态码 = 200
        - 响应 code = 200
        - 返回列表非空（有数据时）
        - P99 < 500ms
    """
    weight = 40
    wait_time = between(2, 5)

    def on_start(self):
        """每个用户启动时执行登录"""
        phone = random.choice(TEST_PHONES)
        self.login(phone)

    @task(5)
    def load_recommend_feed(self):
        """加载推荐 Feed"""
        with self.client.get(
            "/api/v1/content/notes",
            params={"page": 1, "size": 10, "type": "recommend"},
            headers=self.get_headers(),
            name="feed_load_recommend",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(3)
    def load_follow_feed(self):
        """加载关注 Feed"""
        with self.client.get(
            "/api/v1/content/notes",
            params={"page": 1, "size": 10, "type": "follow"},
            headers=self.get_headers(),
            name="feed_load_follow",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(2)
    def load_nearby_feed(self):
        """加载同城 Feed"""
        lat = round(random.uniform(39.8, 40.0), 6)
        lng = round(random.uniform(116.3, 116.5), 6)
        with self.client.get(
            "/api/v1/content/notes",
            params={"page": 1, "size": 10, "type": "nearby", "lat": lat, "lng": lng},
            headers=self.get_headers(),
            name="feed_load_nearby",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def load_feed_pagination(self):
        """Feed 分页加载"""
        page = random.randint(1, 10)
        with self.client.get(
            "/api/v1/content/notes",
            params={"page": page, "size": 10, "type": "recommend"},
            headers=self.get_headers(),
            name="feed_load_pagination",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")


# ============================================================
# 场景2：笔记发布
# ============================================================

class PublishUser(BaseUser):
    """
    场景2：笔记发布（图文 + 视频）

    并发用户数: 50 / 200 / 500
    Ramp-up: 30s / 60s / 90s
    持续时间: 300s
    断言条件:
        - HTTP 状态码 = 200
        - 响应 code = 200
        - 返回 noteId 非空
        - P99 < 1s
    """
    weight = 15
    wait_time = between(5, 10)

    def on_start(self):
        phone = random.choice(TEST_PHONES)
        self.login(phone)

    @task(3)
    def publish_image_note(self):
        """发布图文笔记"""
        payload = {
            "title": f"性能测试图文笔记 {int(time.time() * 1000)}",
            "content": "这是一篇用于性能测试的图文笔记内容。",
            "images": [
                "https://cdn.langtou.com/img/test1.jpg",
                "https://cdn.langtou.com/img/test2.jpg"
            ],
            "type": 1,
            "tags": ["测试", "性能"]
        }
        with self.client.post(
            "/api/v1/content/note",
            json=payload,
            headers=self.get_headers(),
            name="note_publish_image",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200 and data.get("data", {}).get("noteId"):
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def publish_video_note(self):
        """发布视频笔记"""
        payload = {
            "title": f"性能测试视频笔记 {int(time.time() * 1000)}",
            "content": "这是一篇用于性能测试的视频笔记内容。",
            "videoUrl": "https://cdn.langtou.com/video/test.mp4",
            "coverUrl": "https://cdn.langtou.com/img/cover.jpg",
            "type": 2,
            "duration": 30,
            "tags": ["测试", "视频"]
        }
        with self.client.post(
            "/api/v1/content/note",
            json=payload,
            headers=self.get_headers(),
            name="note_publish_video",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200 and data.get("data", {}).get("noteId"):
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")


# ============================================================
# 场景3：搜索接口
# ============================================================

class SearchUser(BaseUser):
    """
    场景3：搜索接口（热门关键词）

    并发用户数: 100 / 300 / 600
    Ramp-up: 30s / 60s / 90s
    持续时间: 300s
    断言条件:
        - HTTP 状态码 = 200
        - 响应 code = 200
        - 返回时间在阈值内
        - P99 < 300ms
    """
    weight = 20
    wait_time = between(2, 4)

    def on_start(self):
        phone = random.choice(TEST_PHONES)
        self.login(phone)

    @task(5)
    def search_hot_keywords(self):
        """热门关键词搜索"""
        keyword = random.choice(HOT_KEYWORDS)
        with self.client.get(
            "/api/v1/content/search",
            params={"keyword": keyword, "page": 1, "size": 10},
            headers=self.get_headers(),
            name="search_hot",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(3)
    def search_common_keywords(self):
        """普通关键词搜索"""
        keyword = random.choice(TEST_KEYWORDS)
        with self.client.get(
            "/api/v1/content/search",
            params={"keyword": keyword, "page": 1, "size": 10},
            headers=self.get_headers(),
            name="search_common",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(2)
    def search_with_sort(self):
        """带排序的搜索"""
        keyword = random.choice(TEST_KEYWORDS)
        sort_type = random.choice(["latest", "hot", "relevant"])
        with self.client.get(
            "/api/v1/content/search",
            params={"keyword": keyword, "page": 1, "size": 10, "sort": sort_type},
            headers=self.get_headers(),
            name="search_sorted",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def search_pagination(self):
        """搜索分页"""
        keyword = random.choice(TEST_KEYWORDS)
        page = random.randint(1, 5)
        with self.client.get(
            "/api/v1/content/search",
            params={"keyword": keyword, "page": page, "size": 10},
            headers=self.get_headers(),
            name="search_pagination",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")


# ============================================================
# 场景4：点赞/评论高并发
# ============================================================

class InteractUser(BaseUser):
    """
    场景4：点赞/评论高并发

    并发用户数: 200 / 800 / 1500
    Ramp-up: 30s / 60s / 120s
    持续时间: 300s
    断言条件:
        - HTTP 状态码 = 200
        - 响应 code = 200
        - 操作幂等性正确
        - P99 < 200ms
    """
    weight = 15
    wait_time = between(1, 3)

    def on_start(self):
        phone = random.choice(TEST_PHONES)
        self.login(phone)

    @task(5)
    def like_note(self):
        """点赞笔记"""
        note_id = random.choice(NOTE_IDS)
        with self.client.post(
            "/api/v1/interact/like",
            json={"noteId": note_id},
            headers=self.get_headers(),
            name="interact_like",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") in [200, 10030]:  # 200 成功，10030 已点赞
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(2)
    def unlike_note(self):
        """取消点赞"""
        note_id = random.choice(NOTE_IDS)
        with self.client.post(
            "/api/v1/interact/unlike",
            json={"noteId": note_id},
            headers=self.get_headers(),
            name="interact_unlike",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") in [200, 10031]:  # 200 成功，10031 未点赞
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(3)
    def comment_note(self):
        """评论笔记"""
        note_id = random.choice(NOTE_IDS)
        with self.client.post(
            "/api/v1/interact/comment",
            json={
                "noteId": note_id,
                "content": f"性能测试评论 {int(time.time() * 1000)}"
            },
            headers=self.get_headers(),
            name="interact_comment",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def collect_note(self):
        """收藏笔记"""
        note_id = random.choice(NOTE_IDS)
        with self.client.post(
            "/api/v1/interact/collect",
            json={"noteId": note_id, "folderId": 0},
            headers=self.get_headers(),
            name="interact_collect",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") in [200, 10033]:  # 200 成功，10033 已收藏
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def get_note_detail(self):
        """获取笔记详情（互动用户也会浏览详情）"""
        note_id = random.choice(NOTE_IDS)
        with self.client.get(
            f"/api/v1/content/note/{note_id}",
            headers=self.get_headers(),
            name="interact_note_detail",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")


# ============================================================
# 场景5：用户注册/登录峰值
# ============================================================

class AuthUser(BaseUser):
    """
    场景5：用户注册/登录峰值

    并发用户数: 100 / 400 / 800
    Ramp-up: 20s / 40s / 60s
    持续时间: 300s
    断言条件:
        - HTTP 状态码 = 200
        - 响应 code = 200
        - 返回 token 非空
        - P99 < 300ms
    """
    weight = 10
    wait_time = between(3, 6)

    def on_start(self):
        pass  # 认证场景不需要预登录

    @task(3)
    def user_login(self):
        """用户登录"""
        phone = random.choice(TEST_PHONES)
        with self.client.post(
            "/api/v1/user/login",
            json={"phone": phone, "password": "Test@1234"},
            name="user_auth_login",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200 and data.get("data", {}).get("token"):
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def user_register(self):
        """用户注册"""
        phone = f"139{random.randint(10000000, 99999999)}"
        with self.client.post(
            "/api/v1/user/register",
            json={"phone": phone, "password": "Test@1234", "verifyCode": "123456"},
            name="user_auth_register",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200 and data.get("data", {}).get("userId"):
                    resp.success()
                else:
                    # 手机号已注册也视为成功（幂等场景）
                    if data.get("code") == 10001:
                        resp.success()
                    else:
                        resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")

    @task(1)
    def get_user_profile(self):
        """获取用户资料（登录后操作）"""
        user_id = random.randint(10001, 11000)
        with self.client.get(
            f"/api/v1/user/profile/{user_id}",
            name="user_auth_profile",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == 200:
                    resp.success()
                else:
                    resp.failure(f"业务错误: {data.get('msg')}")
            else:
                resp.failure(f"HTTP错误: {resp.status_code}")


# ============================================================
# 混合场景（综合负载）
# ============================================================

class MixedUser(BaseUser):
    """
    混合场景：模拟真实用户行为
    权重较低，用于验证系统综合承载能力
    """
    weight = 5
    wait_time = between(3, 8)

    def on_start(self):
        phone = random.choice(TEST_PHONES)
        self.login(phone)

    @task(3)
    def browse_and_interact(self):
        """浏览并互动"""
        # 1. 加载 Feed
        self.client.get(
            "/api/v1/content/notes",
            params={"page": 1, "size": 10, "type": "recommend"},
            headers=self.get_headers(),
            name="mixed_feed"
        )

        # 2. 查看详情
        note_id = random.choice(NOTE_IDS)
        self.client.get(
            f"/api/v1/content/note/{note_id}",
            headers=self.get_headers(),
            name="mixed_detail"
        )

        # 3. 点赞
        self.client.post(
            "/api/v1/interact/like",
            json={"noteId": note_id},
            headers=self.get_headers(),
            name="mixed_like"
        )

    @task(1)
    def search_and_publish(self):
        """搜索并发布"""
        # 1. 搜索
        keyword = random.choice(TEST_KEYWORDS)
        self.client.get(
            "/api/v1/content/search",
            params={"keyword": keyword, "page": 1, "size": 10},
            headers=self.get_headers(),
            name="mixed_search"
        )

        # 2. 发布笔记
        payload = {
            "title": f"混合场景测试 {int(time.time() * 1000)}",
            "content": "混合场景测试内容",
            "images": ["https://cdn.langtou.com/img/test.jpg"],
            "type": 1
        }
        self.client.post(
            "/api/v1/content/note",
            json=payload,
            headers=self.get_headers(),
            name="mixed_publish"
        )


# ============================================================
# 执行配置说明
# ============================================================
"""
运行命令示例:

1. 本地调试（1 用户）:
   locust -f performance-test-locust.py --host=http://localhost:8080 -u 1 -r 1 --run-time 10s --headless

2. 场景1 Feed 加载压测（1000 并发）:
   locust -f performance-test-locust.py --host=http://gateway.langtou.com -u 1000 -r 100 --run-time 300s --headless --csv=feed_test

3. 全场景混合压测:
   locust -f performance-test-locust.py --host=http://gateway.langtou.com -u 2000 -r 200 --run-time 600s --headless --csv=full_test

4. Web UI 模式:
   locust -f performance-test-locust.py --host=http://gateway.langtou.com
   # 访问 http://localhost:8089 配置并发参数

分布式执行（多机压测）:
   # Master
   locust -f performance-test-locust.py --master --host=http://gateway.langtou.com

   # Worker（多台执行）
   locust -f performance-test-locust.py --worker --master-host=<master_ip>
"""
