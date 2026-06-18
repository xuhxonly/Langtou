# -*- coding: utf-8 -*-
"""
榔头(Langtou)性能压测脚本

使用Locust框架模拟用户行为，包括：
- 登录
- 浏览Feed流
- 查看笔记详情
- 点赞
- 评论
- 搜索
- 附近笔记查询

使用方式：
    locust -f locustfile.py --host=http://localhost:8080
    或使用 run-test.sh 脚本启动
"""

import json
import random
import uuid

from locust import HttpUser, TaskSet, task, between, events
from locust.runners import MasterRunner


# ============================================================
# 测试数据
# ============================================================

# 预注册的测试用户
TEST_USERS = [
    {"username": "testuser1", "password": "Test123456"},
    {"username": "testuser2", "password": "Test123456"},
    {"username": "testuser3", "password": "Test123456"},
    {"username": "testuser4", "password": "Test123456"},
    {"username": "testuser5", "password": "Test123456"},
]

# 搜索关键词
SEARCH_KEYWORDS = [
    "美食", "旅行", "时尚", "健身", "摄影",
    "美食推荐", "旅行攻略", "穿搭分享", "健身打卡",
    "咖啡", "蛋糕", "日落", "城市", "风景",
]

# 评论内容池
COMMENT_CONTENTS = [
    "写得太好了，收藏了！",
    "很有帮助，谢谢分享！",
    "太赞了，学习了！",
    "这也太好看了吧！",
    "同感！",
    "收藏收藏！",
    "马上去试试！",
    "好详细的经验分享！",
    "学到了很多！",
    "已关注，期待更多分享！",
]

# 笔记ID范围（用于查看详情和点赞）
NOTE_ID_RANGE = range(1, 100)

# LBS测试坐标（北京、上海、广州、深圳）
LBS_LOCATIONS = [
    {"lat": 39.9042, "lng": 116.4074, "name": "北京"},
    {"lat": 31.2304, "lng": 121.4737, "name": "上海"},
    {"lat": 23.1291, "lng": 113.2644, "name": "广州"},
    {"lat": 22.5431, "lng": 114.0579, "name": "深圳"},
]


# ============================================================
# 用户行为定义
# ============================================================

class LangtouUserBehavior(TaskSet):
    """
    榔头APP用户行为模拟

    模拟真实用户的使用场景，按权重分配各行为：
    - 浏览Feed（最高频）
    - 查看笔记详情（高频）
    - 搜索（中频）
    - 点赞（中低频）
    - 评论（低频）
    - 附近笔记查询（低频）
    """

    def on_start(self):
        """用户开始时先登录获取token。"""
        self.token = None
        self.user_id = None
        self.note_ids_seen = []
        self._login()

    def _login(self):
        """登录获取JWT token。"""
        user = random.choice(TEST_USERS)
        with self.client.post(
            "/api/v1/users/login",
            json=user,
            name="用户登录",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if data.get("code") == 200 and data.get("data"):
                        self.token = data["data"].get("token")
                        self.user_id = data["data"].get("user", {}).get("id")
                        response.success()
                    else:
                        response.failure(f"登录返回异常: {data.get('message', 'unknown')}")
                except json.JSONDecodeError:
                    response.failure("登录响应解析失败")
            else:
                response.failure(f"登录HTTP错误: {response.status_code}")

    def _get_headers(self):
        """获取带认证token的请求头。"""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

    @task(weight=30)
    def browse_feed(self):
        """浏览Feed流（最高频行为）。"""
        page = random.randint(1, 5)
        with self.client.get(
            "/api/v1/notes",
            params={"page": page, "size": 20},
            headers=self._get_headers(),
            name="浏览Feed流",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if data.get("code") == 200 and data.get("data"):
                        records = data["data"].get("list", [])
                        # 收集笔记ID用于后续操作
                        for record in records:
                            note_id = record.get("id")
                            if note_id and note_id not in self.note_ids_seen:
                                self.note_ids_seen.append(note_id)
                        # 限制缓存数量
                        if len(self.note_ids_seen) > 200:
                            self.note_ids_seen = self.note_ids_seen[-100:]
                        response.success()
                    else:
                        response.failure(f"Feed返回异常: {data.get('message', 'unknown')}")
                except json.JSONDecodeError:
                    response.failure("Feed响应解析失败")
            else:
                response.failure(f"Feed HTTP错误: {response.status_code}")

    @task(weight=20)
    def view_note_detail(self):
        """查看笔记详情。"""
        if not self.note_ids_seen:
            note_id = random.choice(NOTE_ID_RANGE)
        else:
            note_id = random.choice(self.note_ids_seen)

        with self.client.get(
            f"/api/v1/notes/{note_id}",
            headers=self._get_headers(),
            name="查看笔记详情",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                # 笔记不存在，从缓存中移除
                if note_id in self.note_ids_seen:
                    self.note_ids_seen.remove(note_id)
                response.success()
            else:
                response.failure(f"笔记详情HTTP错误: {response.status_code}")

    @task(weight=15)
    def search_notes(self):
        """搜索笔记。"""
        keyword = random.choice(SEARCH_KEYWORDS)
        page = random.randint(1, 3)
        with self.client.get(
            "/api/v1/search",
            params={"keyword": keyword, "type": "note", "page": page, "size": 20},
            headers=self._get_headers(),
            name="搜索笔记",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"搜索HTTP错误: {response.status_code}")

    @task(weight=10)
    def like_note(self):
        """点赞笔记。"""
        if not self.note_ids_seen:
            return

        note_id = random.choice(self.note_ids_seen)
        with self.client.post(
            f"/api/v1/notes/{note_id}/like",
            headers=self._get_headers(),
            name="点赞笔记",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code in (400, 401):
                response.success()  # 已点赞或未登录，不算失败
            else:
                response.failure(f"点赞HTTP错误: {response.status_code}")

    @task(weight=5)
    def comment_note(self):
        """评论笔记。"""
        if not self.note_ids_seen:
            return

        note_id = random.choice(self.note_ids_seen)
        content = random.choice(COMMENT_CONTENTS)
        with self.client.post(
            f"/api/v1/notes/{note_id}/comments",
            json={"content": content},
            headers=self._get_headers(),
            name="评论笔记",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code in (400, 401):
                response.success()
            else:
                response.failure(f"评论HTTP错误: {response.status_code}")

    @task(weight=5)
    def collect_note(self):
        """收藏笔记。"""
        if not self.note_ids_seen:
            return

        note_id = random.choice(self.note_ids_seen)
        with self.client.post(
            f"/api/v1/notes/{note_id}/collect",
            headers=self._get_headers(),
            name="收藏笔记",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code in (400, 401):
                response.success()
            else:
                response.failure(f"收藏HTTP错误: {response.status_code}")

    @task(weight=8)
    def search_nearby_notes(self):
        """查询附近笔记（LBS）。"""
        location = random.choice(LBS_LOCATIONS)
        radius = random.choice([1000, 3000, 5000, 10000])
        with self.client.get(
            "/api/v1/notes/nearby",
            params={
                "lat": location["lat"],
                "lng": location["lng"],
                "radius": radius,
                "page": 1,
                "size": 20,
            },
            headers=self._get_headers(),
            name="附近笔记查询",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"附近笔记HTTP错误: {response.status_code}")

    @task(weight=5)
    def browse_following_feed(self):
        """浏览关注用户的Feed流。"""
        with self.client.get(
            "/api/v1/notes/following",
            params={"page": 1, "size": 20},
            headers=self._get_headers(),
            name="关注Feed流",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 401:
                response.success()
            else:
                response.failure(f"关注Feed HTTP错误: {response.status_code}")

    @task(weight=2)
    def get_related_notes(self):
        """查看相关推荐笔记。"""
        if not self.note_ids_seen:
            return

        note_id = random.choice(self.note_ids_seen)
        with self.client.get(
            f"/api/v1/notes/{note_id}/related",
            params={"page": 1, "size": 10},
            headers=self._get_headers(),
            name="相关推荐笔记",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"相关推荐HTTP错误: {response.status_code}")


class LangtouWebsiteUser(HttpUser):
    """
    榔头APP虚拟用户

    模拟用户行为，请求间隔1-3秒（模拟真实用户操作节奏）。
    """
    tasks = [LangtouUserBehavior]
    # 模拟用户操作间隔（秒）
    wait_time = between(1, 3)


# ============================================================
# 自定义事件监听（压测统计增强）
# ============================================================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """压测开始时的初始化。"""
    print("\n" + "=" * 60)
    print("  榔头(Langtou)性能压测启动")
    print("  目标服务: " + str(environment.host))
    print("=" * 60)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """压测结束时的统计输出。"""
    print("\n" + "=" * 60)
    print("  榔头(Langtou)性能压测结束")
    print("=" * 60)
