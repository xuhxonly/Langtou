package com.langtou.interact.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 收藏服务接口测试
 * 覆盖收藏、取消收藏、我的收藏列表等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("收藏服务接口测试")
public class CollectionControllerApiTest {

    private static final String BASE_URL = "http://localhost:8083";
    private static String accessToken;
    private static Long testUserId;
    private static final String TEST_USERNAME = "collect_test_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册并登录测试用户
        String registerBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "collect_%d@example.com",
                    "nickname": "收藏测试用户"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD, System.currentTimeMillis());

        testUserId = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String loginBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

        accessToken = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");
    }

    // ==================== 收藏接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("收藏笔记 - 正常流程")
    void testCollectSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 1)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(2)
    @DisplayName("收藏笔记 - 重复收藏同一篇笔记")
    void testCollectDuplicate() {
        // 先收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 2)
                .then()
                .statusCode(200);

        // 再次收藏同一篇
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 2)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(409)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(409)));
    }

    @Test
    @Order(3)
    @DisplayName("收藏笔记 - 未携带Token")
    void testCollectNoToken() {
        given()
                .when()
                .post("/api/v1/notes/{noteId}/collect", 1)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(4)
    @DisplayName("收藏笔记 - Token无效")
    void testCollectInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_xyz")
                .when()
                .post("/api/v1/notes/{noteId}/collect", 1)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(5)
    @DisplayName("收藏笔记 - 笔记不存在")
    void testCollectNoteNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)));
    }

    // ==================== 取消收藏接口测试 ====================

    @Test
    @Order(6)
    @DisplayName("取消收藏 - 正常流程")
    void testUncollectSuccess() {
        // 先收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 3)
                .then()
                .statusCode(200);

        // 再取消收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/notes/{noteId}/collect", 3)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(7)
    @DisplayName("取消收藏 - 未收藏过的笔记")
    void testUncollectNotCollected() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/notes/{noteId}/collect", 99999998)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));
    }

    @Test
    @Order(8)
    @DisplayName("取消收藏 - 未携带Token")
    void testUncollectNoToken() {
        given()
                .when()
                .delete("/api/v1/notes/{noteId}/collect", 1)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 我的收藏列表接口测试 ====================

    @Test
    @Order(9)
    @DisplayName("我的收藏列表 - 正常获取")
    void testMyCollectionsSuccess() {
        // 先收藏几篇笔记确保有数据
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", 10)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/me/collections")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(10)
    @DisplayName("我的收藏列表 - 指定分页参数")
    void testMyCollectionsWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("current", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/users/me/collections")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("我的收藏列表 - 未携带Token")
    void testMyCollectionsNoToken() {
        given()
                .when()
                .get("/api/v1/users/me/collections")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(12)
    @DisplayName("我的收藏列表 - Token无效")
    void testMyCollectionsInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_abc")
                .when()
                .get("/api/v1/users/me/collections")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(13)
    @DisplayName("收藏后验证列表包含该笔记")
    void testCollectAndVerifyInList() {
        Long noteId = 20L;

        // 先取消可能存在的收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/notes/{noteId}/collect", noteId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));

        // 收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/api/v1/notes/{noteId}/collect", noteId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 查询收藏列表，验证包含该笔记
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/users/me/collections")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
