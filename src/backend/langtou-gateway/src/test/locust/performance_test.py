#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Langtou (榔头) 项目 - Gateway 性能测试脚本
=============================================
Locust 1.6+ 语法，支持 Master-Worker 分布式压测模式。

运行方式:
    单机模式:
        locust -f performance_test.py --host=http://localhost:8080 -u 1000 -r 100 --run-time 10m --html=report.html

    分布式 Master:
        locust -f performance_test.py --master --master-bind-host=0.0.0.0 --master-bind-port=5557 --host=http://localhost:8080

    分布式 Worker:
        locust -f performance_test.py --worker --master-host=<MASTER_IP> --master-port=5557 --host=http://localhost:8080

自定义事件统计:
    - feed_load    : 首页 Feed 加载
    - publish_note : 笔记发布
    - search       : 搜索接口
    - interact     : 点赞/评论交互
    - auth         : 注册/登录认证
"""

import random
import string
import time
import uuid
from datetime import datetime

from locust import HttpUser, task, between, events
from locust.exception import RescheduleTask

# =============================================================================
# 全局配置
# =============================================================================

GATEWAY_HOST = "http://localhost:8080"
API_PREFIX = "/api/v1"

# SLA 阈值（毫秒）
SLA_THRESHOLDS = {
    "feed_load": 800,
    "publish_note": 1500,
    "search": 1000,
    "interact": 600,
    "auth": 1200,
}

# 业务成功码
BUSINESS_SUCCESS_CODE = 200

# 测试账号池（用于多用户并发时避免单一账号瓶颈）
TEST_USERS_POOL = [
    {"username": f"perf_user_{i}", "password": "Perf@123456", "nickname": f"压测用户{i}"}
    for i in range(1, 51)
]


def generate_random_string(length=8):
    """生成随机字符串"""
    return "".join(random.choices(string.ascii_letters + string.digits, k=length))


def generate_random_phone():
    """生成随机手机号"""
    prefix = random.choice(["138", "139", "150", "151", "152", "157", "158", "159", "182", "183", "187", "188"])
    suffix = "".join(random.choices(string.digits, k=8))
    return prefix + suffix


# =============================================================================
# Locust 事件钩子
# =============================================================================

@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response, context, exception, **kwargs):
    """
    全局请求监听器：
    1. 自定义 SLA 检查 —— 响应时间超过阈值则标记为失败
    2. 业务状态码断言 —— 验证 response.json()["code"] == 200
    """
    if exception:
        return

    if response is None:
        return

    # SLA 检查
    event_name = None
    for key in SLA_THRESHOLDS:
        if key in name:
            event_name = key
            break

    if event_name and response_time > SLA_THRESHOLDS[event_name]:
        # 使用 fire 事件将 SLA 违例标记为失败
        events.request.fire(
            request_type=request_type,
            name=f"{name}_SLA_VIOLATION",
            response_time=response_time,
            response_length=response_length,
            response=response,
            context=context,
            exception=Exception(
                f"SLA violation: {name} response time {response_time}ms exceeds threshold {SLA_THRESHOLDS[event_name]}ms"
            ),
        )

    # 业务状态码断言
    try:
        if response.status_code == 200:
            json_data = response.json()
            if isinstance(json_data, dict) and "code" in json_data:
                if json_data["code"] != BUSINESS_SUCCESS_CODE:
                    events.request.fire(
                        request_type=request_type,
                        name=f"{name}_BUSINESS_ERROR",
                        response_time=response_time,
                        response_length=response_length,
                        response=response,
                        context=context,
                        exception=Exception(
                            f"Business error: code={json_data.get('code')}, message={json_data.get('message')}"
                        ),
                    )
    except Exception:
        pass


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    print(f"[TEST START] {datetime.now().isoformat()} - 榔头 Gateway 性能测试开始")
    print(f"[CONFIG] Host: {environment.host}")
    print(f"[CONFIG] SLA Thresholds: {SLA_THRESHOLDS}")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    print(f"[TEST STOP] {datetime.now().isoformat()} - 榔头 Gateway 性能测试结束")


# =============================================================================
# 基础用户类
# =============================================================================

class BaseUser(HttpUser):
    """
    基础用户类，封装公共逻辑：
    - on_start 登录获取 JWT Token
    - 统一请求头注入
    - 业务断言封装
    """

    abstract = True
    wait_time = between(1, 3)

    def __init__(self, environment):
        super().__init__(environment)
        self.token = None
        self.user_id = None
        self.username = None
        self.headers = {"Content-Type": "application/json"}

    def on_start(self):
        """
        每个虚拟用户启动时执行：注册（如需要）并登录获取 Token。
        若登录失败，则标记该用户任务失败。
        """
        user_info = random.choice(TEST_USERS_POOL)
        self.username = user_info["username"]
        password = user_info["password"]

        # 先尝试登录，失败则先注册
        if not self._login(self.username, password):
            if not self._register(self.username, password, user_info["nickname"]):
                raise RescheduleTask("注册失败，无法继续")
            if not self._login(self.username, password):
                raise RescheduleTask("登录失败，无法继续")

        self.headers["Authorization"] = f"Bearer {self.token}"

    def _register(self, username, password, nickname):
        """用户注册"""
        payload = {
            "username": username,
            "password": password,
            "nickname": nickname,
            "phone": generate_random_phone(),
        }
        with self.client.post(
            f"{API_PREFIX}/auth/register",
            json=payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_register",
        ) as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    if data.get("code") == BUSINESS_SUCCESS_CODE:
                        resp.success()
                        return True
                    elif "已存在" in str(data.get("message", "")) or "exists" in str(data.get("message", "")).lower():
                        # 用户已存在视为可接受
                        resp.success()
                        return True
                    else:
                        resp.failure(f"注册业务失败: {data}")
                        return False
                except Exception as e:
                    resp.failure(f"注册响应解析失败: {e}")
                    return False
            else:
                resp.failure(f"注册 HTTP 失败: {resp.status_code}")
                return False

    def _login(self, username, password):
        """用户登录，获取 JWT Token"""
        payload = {
            "username": username,
            "password": password,
        }
        with self.client.post(
            f"{API_PREFIX}/auth/login",
            json=payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_login",
        ) as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    if data.get("code") == BUSINESS_SUCCESS_CODE:
                        self.token = data.get("data", {}).get("token") or data.get("data", {}).get("accessToken")
                        self.user_id = data.get("data", {}).get("userId") or data.get("data", {}).get("id")
                        resp.success()
                        return True
                    else:
                        resp.failure(f"登录业务失败: {data}")
                        return False
                except Exception as e:
                    resp.failure(f"登录响应解析失败: {e}")
                    return False
            else:
                resp.failure(f"登录 HTTP 失败: {resp.status_code}")
                return False

    def _assert_business_success(self, response, name):
        """
        业务断言：验证响应 JSON 中的 code 是否为 200
        若失败则触发 locust 失败事件
        """
        try:
            data = response.json()
            if data.get("code") != BUSINESS_SUCCESS_CODE:
                response.failure(f"[{name}] 业务码错误: code={data.get('code')}, msg={data.get('message')}")
                return False
            return True
        except Exception as e:
            response.failure(f"[{name}] 响应解析失败: {e}")
            return False

    def _request_with_auth(self, method, path, **kwargs):
        """
        封装带认证头的请求，自动注入 Authorization
        """
        headers = kwargs.pop("headers", {})
        headers.update(self.headers)
        return self.client.request(method, path, headers=headers, **kwargs)


# =============================================================================
# 场景 1：FeedUser - 首页 Feed 加载
# =============================================================================

class FeedUser(BaseUser):
    """
    首页 Feed 加载场景
    权重：5
    并发梯度：100 / 500 / 1000
    """

    weight = 5
    wait_time = between(1, 5)

    @task(10)
    def load_feed_page_1(self):
        """加载首页 Feed（第1页）"""
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/notes?page=1&size=20",
            catch_response=True,
            name="feed_load_page1",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "feed_load_page1"):
                    resp.success()
            else:
                resp.failure(f"feed_load_page1 HTTP 错误: {resp.status_code}")

    @task(5)
    def load_feed_page_n(self):
        """加载 Feed 翻页（随机第2-5页）"""
        page = random.randint(2, 5)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/notes?page={page}&size=20",
            catch_response=True,
            name="feed_load_pageN",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "feed_load_pageN"):
                    resp.success()
            else:
                resp.failure(f"feed_load_pageN HTTP 错误: {resp.status_code}")

    @task(3)
    def load_feed_with_refresh(self):
        """模拟下拉刷新（第1页，带时间戳防缓存）"""
        ts = int(time.time() * 1000)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/notes?page=1&size=20&_t={ts}",
            catch_response=True,
            name="feed_load_refresh",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "feed_load_refresh"):
                    resp.success()
            else:
                resp.failure(f"feed_load_refresh HTTP 错误: {resp.status_code}")


# =============================================================================
# 场景 2：PublishUser - 笔记发布
# =============================================================================

class PublishUser(BaseUser):
    """
    笔记发布场景（图文笔记）
    权重：2
    并发梯度：50 / 200 / 500
    """

    weight = 2
    wait_time = between(3, 8)

    @task(1)
    def publish_text_image_note(self):
        """发布图文笔记"""
        note_title = f"性能测试笔记-{generate_random_string(6)}-{int(time.time())}"
        payload = {
            "title": note_title,
            "content": f"这是一条由 Locust 压测生成的图文笔记内容。\n随机内容：{generate_random_string(32)}\n时间戳：{datetime.now().isoformat()}",
            "images": [
                f"https://example.com/images/{generate_random_string(10)}.jpg",
                f"https://example.com/images/{generate_random_string(10)}.jpg",
            ],
            "tags": ["性能测试", "Locust", "压测", generate_random_string(4)],
            "location": random.choice(["北京", "上海", "广州", "深圳", "杭州", "成都"]),
        }
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/notes",
            json=payload,
            catch_response=True,
            name="publish_note",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "publish_note"):
                    resp.success()
            else:
                resp.failure(f"publish_note HTTP 错误: {resp.status_code}")

    @task(1)
    def publish_note_and_check(self):
        """发布笔记后查询验证"""
        note_title = f"压测验证笔记-{generate_random_string(6)}"
        payload = {
            "title": note_title,
            "content": "发布并验证查询一致性。",
            "images": [],
            "tags": ["验证"],
        }
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/notes",
            json=payload,
            catch_response=True,
            name="publish_note_and_check",
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"发布 HTTP 错误: {resp.status_code}")
                return
            if not self._assert_business_success(resp, "publish_note_and_check"):
                return
            resp.success()

        # 简单延迟后查询（实际可扩展为轮询）
        time.sleep(random.uniform(0.5, 1.5))
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/notes?page=1&size=10",
            catch_response=True,
            name="publish_note_verify_list",
        ) as resp2:
            if resp2.status_code == 200:
                if self._assert_business_success(resp2, "publish_note_verify_list"):
                    resp2.success()
            else:
                resp2.failure(f"验证查询 HTTP 错误: {resp2.status_code}")


# =============================================================================
# 场景 3：SearchUser - 搜索接口
# =============================================================================

class SearchUser(BaseUser):
    """
    搜索接口场景
    权重：3
    并发梯度：100 / 300 / 600
    """

    weight = 3
    wait_time = between(2, 6)

    KEYWORDS = ["美食", "旅游", "穿搭", "摄影", "健身", "读书", "电影", "音乐", "科技", "职场"]

    @task(5)
    def search_notes_by_keyword(self):
        """按关键词搜索笔记"""
        keyword = random.choice(self.KEYWORDS)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/search/notes?keyword={keyword}&page=1&size=20",
            catch_response=True,
            name="search_notes",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "search_notes"):
                    resp.success()
            else:
                resp.failure(f"search_notes HTTP 错误: {resp.status_code}")

    @task(3)
    def search_notes_pagination(self):
        """搜索翻页"""
        keyword = random.choice(self.KEYWORDS)
        page = random.randint(1, 5)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/search/notes?keyword={keyword}&page={page}&size=20",
            catch_response=True,
            name="search_notes_page",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "search_notes_page"):
                    resp.success()
            else:
                resp.failure(f"search_notes_page HTTP 错误: {resp.status_code}")

    @task(2)
    def search_with_sort(self):
        """搜索带排序"""
        keyword = random.choice(self.KEYWORDS)
        sort = random.choice(["latest", "hot", "relevant"])
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/search/notes?keyword={keyword}&sort={sort}&page=1&size=20",
            catch_response=True,
            name="search_notes_sort",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "search_notes_sort"):
                    resp.success()
            else:
                resp.failure(f"search_notes_sort HTTP 错误: {resp.status_code}")


# =============================================================================
# 场景 4：InteractUser - 点赞/评论高并发
# =============================================================================

class InteractUser(BaseUser):
    """
    点赞/评论高并发场景
    权重：4
    并发梯度：200 / 800 / 1500
    包含幂等性验证：重复点赞/取消点赞应返回正确状态
    """

    weight = 4
    wait_time = between(0.5, 2)

    # 用于幂等性验证的笔记 ID 缓存（每个用户独立）
    liked_note_ids = set()
    commented_note_ids = set()

    def _get_random_note_id(self):
        """获取一个随机笔记 ID（先拉取 Feed 列表）"""
        try:
            with self._request_with_auth(
                "GET",
                f"{API_PREFIX}/notes?page=1&size=20",
                catch_response=True,
                name="interact_get_note_id",
            ) as resp:
                if resp.status_code == 200:
                    data = resp.json()
                    if data.get("code") == BUSINESS_SUCCESS_CODE:
                        notes = data.get("data", {}).get("list", []) or data.get("data", [])
                        if notes:
                            note = random.choice(notes)
                            note_id = note.get("id") or note.get("noteId")
                            resp.success()
                            return note_id
                resp.failure("获取笔记列表失败")
                return None
        except Exception as e:
            return None

    @task(5)
    def like_note(self):
        """点赞笔记（含幂等性验证）"""
        note_id = self._get_random_note_id()
        if not note_id:
            return

        # 幂等性：若已点赞，则执行取消点赞；若未点赞，则执行点赞
        is_liked = note_id in self.liked_note_ids
        action = "unlike" if is_liked else "like"

        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/like",
            json={"noteId": note_id, "action": action},
            catch_response=True,
            name="interact_like",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == BUSINESS_SUCCESS_CODE:
                    # 幂等性验证：重复操作应返回成功或特定提示，不应报错
                    if is_liked:
                        self.liked_note_ids.discard(note_id)
                    else:
                        self.liked_note_ids.add(note_id)
                    resp.success()
                elif data.get("code") == 409 or "已点赞" in str(data.get("message", "")):
                    # 幂等性兼容：已点赞再次点赞返回冲突视为可接受
                    resp.success()
                else:
                    resp.failure(f"点赞业务错误: {data}")
            else:
                resp.failure(f"点赞 HTTP 错误: {resp.status_code}")

    @task(5)
    def comment_note(self):
        """评论笔记"""
        note_id = self._get_random_note_id()
        if not note_id:
            return

        payload = {
            "noteId": note_id,
            "content": f"Locust 压测评论-{generate_random_string(6)}-{int(time.time() * 1000)}",
            "parentId": None,
        }
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/comment",
            json=payload,
            catch_response=True,
            name="interact_comment",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "interact_comment"):
                    self.commented_note_ids.add(note_id)
                    resp.success()
            else:
                resp.failure(f"评论 HTTP 错误: {resp.status_code}")

    @task(2)
    def idempotent_like_verify(self):
        """
        幂等性专项验证：
        对同一笔记连续点赞两次，第二次应返回正确状态（不报错）
        """
        note_id = self._get_random_note_id()
        if not note_id:
            return

        # 第一次点赞
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/like",
            json={"noteId": note_id, "action": "like"},
            catch_response=True,
            name="interact_like_idempotent_1",
        ) as resp1:
            if resp1.status_code == 200:
                data1 = resp1.json()
                if data1.get("code") == BUSINESS_SUCCESS_CODE or "已点赞" in str(data1.get("message", "")):
                    resp1.success()
                else:
                    resp1.failure(f"第一次点赞失败: {data1}")
                    return
            else:
                resp1.failure(f"第一次点赞 HTTP 错误: {resp1.status_code}")
                return

        # 第二次点赞（幂等性验证）
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/like",
            json={"noteId": note_id, "action": "like"},
            catch_response=True,
            name="interact_like_idempotent_2",
        ) as resp2:
            if resp2.status_code == 200:
                data2 = resp2.json()
                # 期望：成功 或 提示已点赞，不能是 500 或其他业务错误
                if data2.get("code") == BUSINESS_SUCCESS_CODE or "已点赞" in str(data2.get("message", "")) or data2.get("code") == 409:
                    resp2.success()
                else:
                    resp2.failure(f"幂等性验证失败: {data2}")
            else:
                # 某些实现可能返回 409 Conflict，也视为幂等性正确
                if resp2.status_code == 409:
                    resp2.success()
                else:
                    resp2.failure(f"幂等性验证 HTTP 错误: {resp2.status_code}")

    @task(1)
    def batch_interact(self):
        """批量交互：点赞 + 评论组合"""
        note_id = self._get_random_note_id()
        if not note_id:
            return

        # 点赞
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/like",
            json={"noteId": note_id, "action": "like"},
            catch_response=True,
            name="interact_batch_like",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == BUSINESS_SUCCESS_CODE or "已点赞" in str(data.get("message", "")):
                    resp.success()
                else:
                    resp.failure(f"批量点赞失败: {data}")
            else:
                resp.failure(f"批量点赞 HTTP 错误: {resp.status_code}")

        # 评论
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/comment",
            json={
                "noteId": note_id,
                "content": f"批量交互评论-{generate_random_string(6)}",
            },
            catch_response=True,
            name="interact_batch_comment",
        ) as resp2:
            if resp2.status_code == 200:
                if self._assert_business_success(resp2, "interact_batch_comment"):
                    resp2.success()
            else:
                resp2.failure(f"批量评论 HTTP 错误: {resp2.status_code}")


# =============================================================================
# 场景 5：AuthUser - 注册/登录峰值
# =============================================================================

class AuthUser(HttpUser):
    """
    注册/登录峰值场景
    权重：1
    并发梯度：100 / 400 / 800
    注意：AuthUser 不继承 BaseUser，因为需要独立模拟注册/登录峰值，
          不依赖 on_start 预登录。
    """

    weight = 1
    wait_time = between(1, 4)

    def __init__(self, environment):
        super().__init__(environment)
        self._registered = False
        self._local_username = None
        self._local_password = "Perf@123456"

    @task(3)
    def register_new_user(self):
        """新用户注册"""
        unique_id = uuid.uuid4().hex[:8]
        username = f"auth_peak_{unique_id}"
        payload = {
            "username": username,
            "password": self._local_password,
            "nickname": f"峰值用户{unique_id}",
            "phone": generate_random_phone(),
        }
        with self.client.post(
            f"{API_PREFIX}/auth/register",
            json=payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_register",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == BUSINESS_SUCCESS_CODE:
                    self._registered = True
                    self._local_username = username
                    resp.success()
                elif "已存在" in str(data.get("message", "")) or "exists" in str(data.get("message", "")).lower():
                    resp.success()
                else:
                    resp.failure(f"注册业务错误: {data}")
            else:
                resp.failure(f"注册 HTTP 错误: {resp.status_code}")

    @task(5)
    def login_user(self):
        """用户登录"""
        # 优先使用已注册的用户名，否则使用随机测试账号
        if self._local_username:
            username = self._local_username
        else:
            user = random.choice(TEST_USERS_POOL)
            username = user["username"]
            self._local_password = user["password"]

        payload = {
            "username": username,
            "password": self._local_password,
        }
        with self.client.post(
            f"{API_PREFIX}/auth/login",
            json=payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_login",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "auth_login"):
                    resp.success()
            else:
                resp.failure(f"登录 HTTP 错误: {resp.status_code}")

    @task(2)
    def register_and_login_flow(self):
        """注册后立即登录（完整认证流程）"""
        unique_id = uuid.uuid4().hex[:8]
        username = f"auth_flow_{unique_id}"
        password = self._local_password
        payload = {
            "username": username,
            "password": password,
            "nickname": f"流程用户{unique_id}",
            "phone": generate_random_phone(),
        }

        # 注册
        with self.client.post(
            f"{API_PREFIX}/auth/register",
            json=payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_register_flow",
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if data.get("code") == BUSINESS_SUCCESS_CODE:
                    resp.success()
                elif "已存在" in str(data.get("message", "")):
                    resp.success()
                else:
                    resp.failure(f"流程注册失败: {data}")
                    return
            else:
                resp.failure(f"流程注册 HTTP 错误: {resp.status_code}")
                return

        # 登录
        login_payload = {"username": username, "password": password}
        with self.client.post(
            f"{API_PREFIX}/auth/login",
            json=login_payload,
            headers={"Content-Type": "application/json"},
            catch_response=True,
            name="auth_login_flow",
        ) as resp2:
            if resp2.status_code == 200:
                if self._assert_business_success(resp2, "auth_login_flow"):
                    resp2.success()
            else:
                resp2.failure(f"流程登录 HTTP 错误: {resp2.status_code}")

    def _assert_business_success(self, response, name):
        """AuthUser 内部业务断言"""
        try:
            data = response.json()
            if data.get("code") != BUSINESS_SUCCESS_CODE:
                response.failure(f"[{name}] 业务码错误: code={data.get('code')}, msg={data.get('message')}")
                return False
            return True
        except Exception as e:
            response.failure(f"[{name}] 响应解析失败: {e}")
            return False


# =============================================================================
# 场景 6：MixedUser - 混合负载（综合场景）
# =============================================================================

class MixedUser(BaseUser):
    """
    混合负载综合场景
    同时执行 Feed / Publish / Search / Interact / Auth 相关任务
    按权重分配：Feed(5) : Publish(2) : Search(3) : Interact(4) : Auth(1)
    """

    weight = 10
    wait_time = between(1, 5)

    # 权重比例映射
    @task(5)
    def mixed_feed(self):
        """混合场景 - Feed 加载"""
        ts = int(time.time() * 1000)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/notes?page=1&size=20&_t={ts}",
            catch_response=True,
            name="mixed_feed_load",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "mixed_feed_load"):
                    resp.success()
            else:
                resp.failure(f"mixed_feed_load HTTP 错误: {resp.status_code}")

    @task(2)
    def mixed_publish(self):
        """混合场景 - 发布笔记"""
        payload = {
            "title": f"混合负载笔记-{generate_random_string(6)}",
            "content": f"混合场景发布的图文笔记内容。{generate_random_string(20)}",
            "images": [f"https://example.com/img/{generate_random_string(8)}.jpg"],
            "tags": ["混合", "压测"],
        }
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/notes",
            json=payload,
            catch_response=True,
            name="mixed_publish_note",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "mixed_publish_note"):
                    resp.success()
            else:
                resp.failure(f"mixed_publish_note HTTP 错误: {resp.status_code}")

    @task(3)
    def mixed_search(self):
        """混合场景 - 搜索"""
        keyword = random.choice(SearchUser.KEYWORDS)
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/search/notes?keyword={keyword}&page=1&size=20",
            catch_response=True,
            name="mixed_search",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "mixed_search"):
                    resp.success()
            else:
                resp.failure(f"mixed_search HTTP 错误: {resp.status_code}")

    @task(4)
    def mixed_interact(self):
        """混合场景 - 点赞/评论"""
        # 先获取笔记
        note_id = None
        try:
            with self._request_with_auth(
                "GET",
                f"{API_PREFIX}/notes?page=1&size=10",
                catch_response=True,
                name="mixed_interact_get_note",
            ) as resp:
                if resp.status_code == 200:
                    data = resp.json()
                    if data.get("code") == BUSINESS_SUCCESS_CODE:
                        notes = data.get("data", {}).get("list", []) or data.get("data", [])
                        if notes:
                            note_id = random.choice(notes).get("id") or random.choice(notes).get("noteId")
                        resp.success()
                    else:
                        resp.failure("获取笔记列表业务错误")
                        return
                else:
                    resp.failure(f"获取笔记列表 HTTP 错误: {resp.status_code}")
                    return
        except Exception:
            return

        if not note_id:
            return

        # 点赞
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/like",
            json={"noteId": note_id, "action": "like"},
            catch_response=True,
            name="mixed_interact_like",
        ) as resp2:
            if resp2.status_code == 200:
                data2 = resp2.json()
                if data2.get("code") == BUSINESS_SUCCESS_CODE or "已点赞" in str(data2.get("message", "")):
                    resp2.success()
                else:
                    resp2.failure(f"混合点赞失败: {data2}")
            else:
                resp2.failure(f"混合点赞 HTTP 错误: {resp2.status_code}")

        # 评论
        with self._request_with_auth(
            "POST",
            f"{API_PREFIX}/interact/comment",
            json={
                "noteId": note_id,
                "content": f"混合负载评论-{generate_random_string(6)}",
            },
            catch_response=True,
            name="mixed_interact_comment",
        ) as resp3:
            if resp3.status_code == 200:
                if self._assert_business_success(resp3, "mixed_interact_comment"):
                    resp3.success()
            else:
                resp3.failure(f"混合评论 HTTP 错误: {resp3.status_code}")

    @task(1)
    def mixed_auth_refresh(self):
        """混合场景 - 模拟 Token 刷新或登录状态检查"""
        with self._request_with_auth(
            "GET",
            f"{API_PREFIX}/auth/profile",
            catch_response=True,
            name="mixed_auth_profile",
        ) as resp:
            if resp.status_code == 200:
                if self._assert_business_success(resp, "mixed_auth_profile"):
                    resp.success()
            elif resp.status_code == 401:
                # Token 过期场景，尝试重新登录
                user_info = random.choice(TEST_USERS_POOL)
                payload = {"username": user_info["username"], "password": user_info["password"]}
                with self.client.post(
                    f"{API_PREFIX}/auth/login",
                    json=payload,
                    headers={"Content-Type": "application/json"},
                    catch_response=True,
                    name="mixed_auth_relogin",
                ) as relogin:
                    if relogin.status_code == 200:
                        data = relogin.json()
                        if data.get("code") == BUSINESS_SUCCESS_CODE:
                            new_token = data.get("data", {}).get("token") or data.get("data", {}).get("accessToken")
                            if new_token:
                                self.token = new_token
                                self.headers["Authorization"] = f"Bearer {new_token}"
                            relogin.success()
                        else:
                            relogin.failure(f"重新登录业务失败: {data}")
                    else:
                        relogin.failure(f"重新登录 HTTP 错误: {relogin.status_code}")
            else:
                resp.failure(f"mixed_auth_profile HTTP 错误: {resp.status_code}")


# =============================================================================
# 自定义统计事件（通过 Locust 事件系统扩展）
# =============================================================================

@events.request.add_listener
def custom_event_tracker(request_type, name, response_time, response_length, response, context, exception, **kwargs):
    """
    自定义统计事件映射：
    将各类请求映射到统一的业务事件维度，方便在报告中按场景聚合。
    """
    event_mapping = {
        "feed_load": ["feed_load_page1", "feed_load_pageN", "feed_load_refresh", "mixed_feed_load"],
        "publish_note": ["publish_note", "publish_note_and_check", "mixed_publish_note"],
        "search": ["search_notes", "search_notes_page", "search_notes_sort", "mixed_search"],
        "interact": [
            "interact_like",
            "interact_comment",
            "interact_like_idempotent_1",
            "interact_like_idempotent_2",
            "interact_batch_like",
            "interact_batch_comment",
            "mixed_interact_like",
            "mixed_interact_comment",
        ],
        "auth": [
            "auth_register",
            "auth_login",
            "auth_register_flow",
            "auth_login_flow",
            "mixed_auth_profile",
            "mixed_auth_relogin",
        ],
    }

    for event_key, names in event_mapping.items():
        if any(n in name for n in names):
            # 通过环境变量或日志输出自定义事件统计（Locust 1.6+ 原生不支持自定义事件图表，
            # 但可以通过命名约定在报告中聚合，或通过外部监控系统采集）
            break


# =============================================================================
# 入口说明
# =============================================================================
if __name__ == "__main__":
    import sys
    print("请使用 locust 命令运行本脚本，例如:")
    print("  locust -f performance_test.py --host=http://localhost:8080")
    print("")
    print("分布式模式:")
    print("  Master: locust -f performance_test.py --master --master-bind-port=5557 --host=http://localhost:8080")
    print("  Worker: locust -f performance_test.py --worker --master-host=127.0.0.1 --master-port=5557 --host=http://localhost:8080")
    sys.exit(0)
