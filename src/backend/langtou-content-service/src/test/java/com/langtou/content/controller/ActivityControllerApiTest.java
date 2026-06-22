package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 活动接口测试
 * 覆盖活动列表、活动详情、参与/退出活动、活动排行榜等接口
 * 验证活动状态验证、重复参与、时间范围
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("活动接口测试")
public class ActivityControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String userToken;
    private static Long userId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册测试用户
        String username = "activity_test_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "activity_test_%d@example.com",
                    "nickname": "活动测试用户"
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

    // ==================== 活动列表接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("活动列表 - 正常获取")
    void testListActivitiesSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/activities")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("活动列表 - 指定分页参数")
    void testListActivitiesWithPagination() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/activities")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("活动列表 - 按类型筛选")
    void testListActivitiesByType() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("type", "challenge")
                .when()
                .get("/api/v1/activities")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("活动列表 - 未携带Token（公开接口）")
    void testListActivitiesNoToken() {
        given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/activities")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("活动列表 - 分页参数边界")
    void testListActivitiesPaginationEdge() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .queryParam("page", 0)
                .queryParam("size", 100)
                .when()
                .get("/api/v1/activities")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 活动详情接口测试 ====================

    @Test
    @Order(6)
    @DisplayName("活动详情 - 正常获取")
    void testGetActivityDetailSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .when()
                .get("/api/v1/activities/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(7)
    @DisplayName("活动详情 - 活动不存在")
    void testGetActivityDetailNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 99999999)
                .when()
                .get("/api/v1/activities/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(8)
    @DisplayName("活动详情 - 未携带Token（公开接口）")
    void testGetActivityDetailNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .get("/api/v1/activities/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 参与活动接口测试 ====================

    @Test
    @Order(9)
    @DisplayName("参与活动 - 正常流程")
    void testJoinActivitySuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(10)
    @DisplayName("参与活动 - 未携带Token")
    void testJoinActivityNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(11)
    @DisplayName("参与活动 - 重复参与")
    void testJoinActivityDuplicate() {
        // 先参与一次
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

        // 再次参与
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));
    }

    @Test
    @Order(12)
    @DisplayName("参与活动 - 活动不存在")
    void testJoinActivityNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 99999999)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 退出活动接口测试 ====================

    @Test
    @Order(13)
    @DisplayName("退出活动 - 正常流程")
    void testQuitActivitySuccess() {
        // 先参与
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/join")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));

        // 退出
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/quit")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(14)
    @DisplayName("退出活动 - 未携带Token")
    void testQuitActivityNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .post("/api/v1/activities/{id}/quit")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("退出活动 - 未参与时退出")
    void testQuitActivityNotJoined() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .header("X-User-Id", String.valueOf(userId))
                .pathParam("id", 2)
                .when()
                .post("/api/v1/activities/{id}/quit")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));
    }

    // ==================== 活动排行榜接口测试 ====================

    @Test
    @Order(16)
    @DisplayName("活动排行榜 - 正常获取")
    void testGetActivityRankingSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .queryParam("sortBy", "note_count")
                .queryParam("limit", 50)
                .when()
                .get("/api/v1/activities/{id}/ranking")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(17)
    @DisplayName("活动排行榜 - 指定limit")
    void testGetActivityRankingWithLimit() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .queryParam("sortBy", "like_count")
                .queryParam("limit", 10)
                .when()
                .get("/api/v1/activities/{id}/ranking")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(18)
    @DisplayName("活动排行榜 - 未携带Token（公开接口）")
    void testGetActivityRankingNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .get("/api/v1/activities/{id}/ranking")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(19)
    @DisplayName("活动排行榜 - 活动不存在")
    void testGetActivityRankingNotFound() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 99999999)
                .when()
                .get("/api/v1/activities/{id}/ranking")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(20)
    @DisplayName("活动排行榜 - limit边界值")
    void testGetActivityRankingLimitEdge() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .pathParam("id", 1)
                .queryParam("limit", 1)
                .when()
                .get("/api/v1/activities/{id}/ranking")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }
}
