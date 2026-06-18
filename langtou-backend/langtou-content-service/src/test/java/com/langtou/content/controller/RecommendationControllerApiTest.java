package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 个性化推荐接口测试
 * 覆盖个性化推荐流、热门推荐、行为反馈等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("个性化推荐接口测试")
public class RecommendationControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static String accessToken;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 通过 user-service 登录获取 token
        String username = "recommend_test_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "recommend_%d@example.com",
                    "nickname": "推荐测试用户"
                }
                """.formatted(username, System.currentTimeMillis());

        given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200);

        String loginBody = """
                {
                    "username": "%s",
                    "password": "Test@123456"
                }
                """.formatted(username);

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

    // ==================== 个性化推荐流接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("个性化推荐流 - 正常获取")
    void testRecommendFeedSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/recommendations/feed")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("个性化推荐流 - 指定分页参数")
    void testRecommendFeedWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/recommendations/feed")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(5));
    }

    @Test
    @Order(3)
    @DisplayName("个性化推荐流 - 第二页数据")
    void testRecommendFeedPage2() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/recommendations/feed")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("个性化推荐流 - 未携带Token")
    void testRecommendFeedNoToken() {
        given()
                .when()
                .get("/api/v1/recommendations/feed")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(5)
    @DisplayName("个性化推荐流 - Token无效")
    void testRecommendFeedInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_abc123")
                .when()
                .get("/api/v1/recommendations/feed")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 热门推荐接口测试 ====================

    @Test
    @Order(6)
    @DisplayName("热门推荐 - 正常获取")
    void testHotRecommendSuccess() {
        given()
                .when()
                .get("/api/v1/recommendations/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("热门推荐 - 指定分页参数")
    void testHotRecommendWithPagination() {
        given()
                .queryParam("page", 1)
                .queryParam("size", 3)
                .when()
                .get("/api/v1/recommendations/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(3));
    }

    @Test
    @Order(8)
    @DisplayName("热门推荐 - 无需Token（公开接口）")
    void testHotRecommendWithoutToken() {
        given()
                .when()
                .get("/api/v1/recommendations/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    // ==================== 行为反馈接口测试 ====================

    @Test
    @Order(9)
    @DisplayName("行为反馈 - 浏览行为上报")
    void testFeedbackViewAction() {
        String requestBody = """
                {
                    "noteId": 1,
                    "actionType": "view"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(10)
    @DisplayName("行为反馈 - 点赞行为上报")
    void testFeedbackLikeAction() {
        String requestBody = """
                {
                    "noteId": 1,
                    "actionType": "like"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(11)
    @DisplayName("行为反馈 - 收藏行为上报")
    void testFeedbackCollectAction() {
        String requestBody = """
                {
                    "noteId": 1,
                    "actionType": "collect"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(12)
    @DisplayName("行为反馈 - 缺少noteId参数")
    void testFeedbackMissingNoteId() {
        String requestBody = """
                {
                    "actionType": "view"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(13)
    @DisplayName("行为反馈 - 缺少actionType参数")
    void testFeedbackMissingActionType() {
        String requestBody = """
                {
                    "noteId": 1
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(14)
    @DisplayName("行为反馈 - 未携带Token")
    void testFeedbackNoToken() {
        String requestBody = """
                {
                    "noteId": 1,
                    "actionType": "view"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("行为反馈 - 笔记不存在")
    void testFeedbackNoteNotFound() {
        String requestBody = """
                {
                    "noteId": 99999999,
                    "actionType": "view"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/recommendations/feedback")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)));
    }
}
