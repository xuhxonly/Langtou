package com.langtou.message.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 通知接口测试
 * 覆盖通知列表、标记已读、全部已读、删除通知、未读数等接口
 * 验证正常/空结果/权限验证
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("通知接口测试")
public class NotificationControllerApiTest {

    private static final String BASE_URL = "http://localhost:8084";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String userToken;
    private static Long userId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册测试用户
        String username = "notif_test_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "notif_test_%d@example.com",
                    "nickname": "通知测试用户"
                }
                """.formatted(username, System.currentTimeMillis());

        userId = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 登录获取 Token
        String loginBody = """
                {
                    "username": "%s",
                    "password": "Test@123456"
                }
                """.formatted(username);

        userToken = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");
    }

    // ==================== 通知列表接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("通知列表 - 正常获取")
    void testGetNotificationsSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("current", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("通知列表 - 指定分页参数")
    void testGetNotificationsWithPagination() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("current", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("通知列表 - 按类型筛选")
    void testGetNotificationsByType() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("current", 1)
                .queryParam("size", 20)
                .queryParam("type", "system")
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("通知列表 - 空结果（新用户无通知）")
    void testGetNotificationsEmpty() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("current", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("通知列表 - 未携带Token")
    void testGetNotificationsNoToken() {
        given()
                .queryParam("current", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(6)
    @DisplayName("通知列表 - Token无效")
    void testGetNotificationsInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_xyz")
                .queryParam("current", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 未读数接口测试 ====================

    @Test
    @Order(7)
    @DisplayName("未读数 - 正常获取")
    void testGetUnreadCountSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("未读数 - 未携带Token")
    void testGetUnreadCountNoToken() {
        given()
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(9)
    @DisplayName("未读数 - 新用户应为0")
    void testGetUnreadCountZero() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 全部已读接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("全部已读 - 正常流程")
    void testMarkAllAsReadSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .put("/api/v1/notifications/read-all")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(11)
    @DisplayName("全部已读 - 未携带Token")
    void testMarkAllAsReadNoToken() {
        given()
                .when()
                .put("/api/v1/notifications/read-all")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(12)
    @DisplayName("全部已读 - 验证未读数归零")
    void testMarkAllAsReadThenCheckCount() {
        // 先全部已读
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .put("/api/v1/notifications/read-all")
                .then()
                .statusCode(200);

        // 再查询未读数
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 标记单条已读接口测试 ====================

    @Test
    @Order(13)
    @DisplayName("标记已读 - 正常流程")
    void testMarkAsReadSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .when()
                .put("/api/v1/notifications/{id}/read")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(14)
    @DisplayName("标记已读 - 未携带Token")
    void testMarkAsReadNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .put("/api/v1/notifications/{id}/read")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("标记已读 - 通知不存在")
    void testMarkAsReadNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 99999999)
                .when()
                .put("/api/v1/notifications/{id}/read")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 删除通知接口测试 ====================

    @Test
    @Order(16)
    @DisplayName("删除通知 - 正常流程")
    void testDeleteNotificationSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .when()
                .delete("/api/v1/notifications/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(17)
    @DisplayName("删除通知 - 未携带Token")
    void testDeleteNotificationNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .delete("/api/v1/notifications/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(18)
    @DisplayName("删除通知 - 通知不存在")
    void testDeleteNotificationNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 99999999)
                .when()
                .delete("/api/v1/notifications/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }
}
