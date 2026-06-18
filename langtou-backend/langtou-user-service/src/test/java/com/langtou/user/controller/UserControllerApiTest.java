package com.langtou.user.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 用户服务接口测试
 * 覆盖注册、登录、用户信息、关注等核心接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户服务接口测试")
public class UserControllerApiTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static String accessToken;
    private static Long testUserId;
    private static final String TEST_USERNAME = "apitest_user_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String TEST_EMAIL = "apitest_" + System.currentTimeMillis() + "@example.com";
    private static final String TEST_NICKNAME = "API测试用户";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ==================== 注册相关测试 ====================

    @Test
    @Order(1)
    @DisplayName("正常用户注册")
    void testRegisterSuccess() {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "%s",
                    "nickname": "%s"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL, TEST_NICKNAME);

        testUserId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.id", notNullValue())
                .body("data.username", equalTo(TEST_USERNAME))
                .body("data.email", equalTo(TEST_EMAIL))
                .extract()
                .path("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("注册失败 - 用户名已存在")
    void testRegisterDuplicateUsername() {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "Another@123",
                    "email": "another_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "另一个用户"
                }
                """.formatted(TEST_USERNAME);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(409)))
                .body("code", anyOf(equalTo(400), equalTo(409), equalTo(1001)))
                .body("message", anyOf(containsString("已存在"), containsString("重复"), containsString("存在")));
    }

    @Test
    @Order(3)
    @DisplayName("注册失败 - 参数校验失败（密码太短）")
    void testRegisterInvalidPassword() {
        String requestBody = """
                {
                    "username": "short_pwd_user",
                    "password": "123",
                    "email": "short_pwd@example.com",
                    "nickname": "密码太短"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("密码"), containsString("格式"), containsString("参数")));
    }

    @Test
    @Order(4)
    @DisplayName("注册失败 - 邮箱格式错误")
    void testRegisterInvalidEmail() {
        String requestBody = """
                {
                    "username": "bad_email_user_" + System.currentTimeMillis(),
                    "password": "Test@123456",
                    "email": "not-an-email",
                    "nickname": "邮箱错误"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("邮箱"), containsString("格式"), containsString("参数")));
    }

    // ==================== 登录相关测试 ====================

    @Test
    @Order(5)
    @DisplayName("正常用户登录")
    void testLoginSuccess() {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

        accessToken = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.token", notNullValue())
                .body("data.tokenType", anyOf(equalTo("Bearer"), nullValue()))
                .extract()
                .path("data.token");
    }

    @Test
    @Order(6)
    @DisplayName("登录失败 - 密码错误")
    void testLoginWrongPassword() {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "WrongPassword123!"
                }
                """.formatted(TEST_USERNAME);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1003)))
                .body("message", anyOf(containsString("密码"), containsString("认证"), containsString("失败")));
    }

    @Test
    @Order(7)
    @DisplayName("登录失败 - 用户不存在")
    void testLoginUserNotFound() {
        String requestBody = """
                {
                    "username": "non_existent_user_99999",
                    "password": "AnyPassword123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(404)))
                .body("code", anyOf(equalTo(401), equalTo(404), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("失败")));
    }

    // ==================== 用户信息相关测试 ====================

    @Test
    @Order(8)
    @DisplayName("获取当前用户信息 - 正常流程")
    void testGetCurrentUserSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.id", equalTo(testUserId.intValue()))
                .body("data.username", equalTo(TEST_USERNAME))
                .body("data.email", equalTo(TEST_EMAIL))
                .body("data.nickname", equalTo(TEST_NICKNAME));
    }

    @Test
    @Order(9)
    @DisplayName("获取当前用户信息 - 未携带Token")
    void testGetCurrentUserNoToken() {
        given()
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(10)
    @DisplayName("获取当前用户信息 - Token无效")
    void testGetCurrentUserInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_here")
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("无效"), containsString("认证"), containsString("过期")));
    }

    @Test
    @Order(11)
    @DisplayName("更新用户资料 - 正常流程")
    void testUpdateUserProfileSuccess() {
        String newNickname = "更新后的昵称_" + System.currentTimeMillis();
        String requestBody = """
                {
                    "nickname": "%s",
                    "bio": "这是通过API测试更新的个人简介",
                    "avatar": "https://example.com/avatar.png"
                }
                """.formatted(newNickname);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.nickname", equalTo(newNickname))
                .body("data.bio", equalTo("这是通过API测试更新的个人简介"));
    }

    @Test
    @Order(12)
    @DisplayName("更新用户资料 - 未携带Token")
    void testUpdateUserProfileNoToken() {
        String requestBody = """
                {
                    "nickname": "未授权更新"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/users/me")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(13)
    @DisplayName("更新用户资料 - 昵称超长")
    void testUpdateUserProfileNicknameTooLong() {
        String longNickname = "a".repeat(51);
        String requestBody = """
                {
                    "nickname": "%s"
                }
                """.formatted(longNickname);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/users/me")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("昵称"), containsString("长度"), containsString("参数")));
    }

    // ==================== 关注相关测试 ====================

    @Test
    @Order(14)
    @DisplayName("关注用户 - 正常流程")
    void testFollowUserSuccess() {
        // 先创建另一个用户用于关注测试
        String targetUsername = "target_user_" + System.currentTimeMillis();
        String requestBody = """
                {
                    "username": "%s",
                    "password": "Target@123456",
                    "email": "target_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "被关注用户"
                }
                """.formatted(targetUsername);

        Integer targetUserId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/follow", targetUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", anyOf(nullValue(), notNullValue()));
    }

    @Test
    @Order(15)
    @DisplayName("关注用户 - 关注自己（异常场景）")
    void testFollowSelf() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/follow", testUserId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(409)))
                .body("code", anyOf(equalTo(400), equalTo(409), equalTo(1006)))
                .body("message", anyOf(containsString("自己"), containsString("不能"), containsString("关注")));
    }

    @Test
    @Order(16)
    @DisplayName("关注用户 - 用户不存在")
    void testFollowNonExistentUser() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/follow", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)))
                .body("code", anyOf(equalTo(404), equalTo(400), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("用户")));
    }

    @Test
    @Order(17)
    @DisplayName("关注用户 - 未携带Token")
    void testFollowNoToken() {
        given()
                .when()
                .post("/api/v1/users/{id}/follow", 1)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(18)
    @DisplayName("取消关注 - 正常流程")
    void testUnfollowUserSuccess() {
        // 创建并关注一个用户，然后取消关注
        String targetUsername = "unfollow_target_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Target@123456",
                    "email": "unfollow_target_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "取消关注目标"
                }
                """.formatted(targetUsername);

        Integer targetUserId = given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 先关注
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/follow", targetUserId)
                .then()
                .statusCode(200);

        // 再取消关注
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/unfollow", targetUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(19)
    @DisplayName("取消关注 - 未关注过的用户")
    void testUnfollowNotFollowedUser() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/unfollow", 99999998)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(404), equalTo(1007)))
                .body("message", anyOf(containsString("未关注"), containsString("不存在"), containsString("成功"), containsString("无需")));
    }

    @Test
    @Order(20)
    @DisplayName("取消关注 - 未携带Token")
    void testUnfollowNoToken() {
        given()
                .when()
                .post("/api/v1/users/{id}/unfollow", 1)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }
}
