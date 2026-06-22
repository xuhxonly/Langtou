package com.langtou.user.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 用户搜索与关注关系接口测试
 * 覆盖搜索用户、粉丝列表、关注列表等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("用户搜索与关注关系接口测试")
public class SearchControllerApiTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static String accessToken;
    private static Long testUserId;
    private static Long targetUserId;
    private static final String TEST_USERNAME = "search_test_user_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String TEST_EMAIL = "search_test_" + System.currentTimeMillis() + "@example.com";
    private static final String TARGET_USERNAME = "search_target_" + System.currentTimeMillis();
    private static final String TARGET_NICKNAME = "搜索目标用户";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册并登录测试用户
        String registerBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "%s",
                    "nickname": "搜索测试用户"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);

        testUserId = given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 登录获取token
        String loginBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

        accessToken = given()
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 注册目标用户（用于关注和搜索测试）
        String targetRegisterBody = """
                {
                    "username": "%s",
                    "password": "Target@123456",
                    "email": "search_target_%d@example.com",
                    "nickname": "%s"
                }
                """.formatted(TARGET_USERNAME, System.currentTimeMillis(), TARGET_NICKNAME);

        targetUserId = given()
                .contentType(ContentType.JSON)
                .body(targetRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 关注目标用户，确保有粉丝/关注数据
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/users/{id}/follow", targetUserId)
                .then()
                .statusCode(200);
    }

    // ==================== 搜索用户接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("搜索用户 - 按用户名关键字搜索")
    void testSearchUsersByUsername() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", TARGET_USERNAME)
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(2)
    @DisplayName("搜索用户 - 按昵称关键字搜索")
    void testSearchUsersByNickname() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "搜索目标")
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("搜索用户 - 关键字无匹配结果")
    void testSearchUsersNoMatch() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "zzz_no_match_xyz_" + System.currentTimeMillis())
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", equalTo(0));
    }

    @Test
    @Order(4)
    @DisplayName("搜索用户 - 指定limit参数")
    void testSearchUsersWithLimit() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "test")
                .queryParam("limit", 5)
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(5));
    }

    @Test
    @Order(5)
    @DisplayName("搜索用户 - 缺少keyword参数")
    void testSearchUsersMissingKeyword() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(6)
    @DisplayName("搜索用户 - 未携带Token")
    void testSearchUsersNoToken() {
        given()
                .queryParam("keyword", "test")
                .when()
                .get("/api/v1/users/search")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 粉丝列表接口测试 ====================

    @Test
    @Order(7)
    @DisplayName("获取粉丝列表 - 正常流程")
    void testGetFollowersSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/{userId}/followers", targetUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("获取粉丝列表 - 指定分页参数")
    void testGetFollowersWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/users/{userId}/followers", targetUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("获取粉丝列表 - 用户不存在")
    void testGetFollowersUserNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/{userId}/followers", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 关注列表接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取关注列表 - 正常流程")
    void testGetFollowingSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/{userId}/following", testUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("获取关注列表 - 指定分页参数")
    void testGetFollowingWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/users/{userId}/following", testUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("获取关注列表 - 用户不存在")
    void testGetFollowingUserNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/{userId}/following", 99999998)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(13)
    @DisplayName("获取关注列表 - 无关注记录")
    void testGetFollowingEmptyList() {
        // 注册一个新用户，没有任何关注
        String emptyUsername = "empty_follow_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Empty@123456",
                    "email": "empty_%d@example.com",
                    "nickname": "无关注用户"
                }
                """.formatted(emptyUsername, System.currentTimeMillis());

        Integer emptyUserId = given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/{userId}/following", emptyUserId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
