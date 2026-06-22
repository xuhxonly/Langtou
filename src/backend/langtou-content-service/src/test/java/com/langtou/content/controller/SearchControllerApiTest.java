package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 内容搜索接口测试
 * 覆盖搜索笔记、搜索建议、热搜榜、搜索历史等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("内容搜索接口测试")
public class SearchControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static String accessToken;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 通过 user-service 登录获取 token
        String loginBody = """
                {
                    "username": "default_test_user",
                    "password": "Test@123456"
                }
                """;

        try {
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
        } catch (Exception e) {
            // 如果默认用户不存在，注册一个
            String registerBody = """
                    {
                        "username": "content_search_test_" + System.currentTimeMillis(),
                        "password": "Test@123456",
                        "email": "content_search_%d@example.com",
                        "nickname": "内容搜索测试"
                    }
                    """;

            given()
                    .baseUri("http://localhost:8081")
                    .contentType(ContentType.JSON)
                    .body(registerBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .statusCode(200);

            String username = "content_search_test_" + System.currentTimeMillis();
            String loginBody2 = """
                    {
                        "username": "%s",
                        "password": "Test@123456"
                    }
                    """.formatted(username);

            accessToken = given()
                    .baseUri("http://localhost:8081")
                    .contentType(ContentType.JSON)
                    .body(loginBody2)
                    .when()
                    .post("/api/v1/auth/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("data.token");
        }
    }

    // ==================== 搜索笔记接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("搜索笔记 - 正常关键字搜索")
    void testSearchNotesSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "测试")
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("搜索笔记 - 指定分页参数")
    void testSearchNotesWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "测试")
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("搜索笔记 - 无匹配结果")
    void testSearchNotesNoMatch() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "zzz_no_match_xyz_" + System.currentTimeMillis())
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("搜索笔记 - 缺少keyword参数")
    void testSearchNotesMissingKeyword() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    // ==================== 搜索建议接口测试 ====================

    @Test
    @Order(5)
    @DisplayName("搜索建议 - 正常前缀匹配")
    void testSearchSuggestSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("q", "测")
                .when()
                .get("/api/v1/search/suggest")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("搜索建议 - 指定limit参数")
    void testSearchSuggestWithLimit() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("q", "test")
                .queryParam("limit", 3)
                .when()
                .get("/api/v1/search/suggest")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(3));
    }

    @Test
    @Order(7)
    @DisplayName("搜索建议 - 无匹配建议")
    void testSearchSuggestNoMatch() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("q", "zzz_no_suggest_xyz_" + System.currentTimeMillis())
                .when()
                .get("/api/v1/search/suggest")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", equalTo(0));
    }

    @Test
    @Order(8)
    @DisplayName("搜索建议 - 缺少q参数")
    void testSearchSuggestMissingQ() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/search/suggest")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    // ==================== 热搜榜接口测试 ====================

    @Test
    @Order(9)
    @DisplayName("热搜榜 - 正常获取")
    void testGetHotSearchKeywords() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/search/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(10)
    @DisplayName("热搜榜 - 指定limit参数")
    void testGetHotSearchKeywordsWithLimit() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("limit", 5)
                .when()
                .get("/api/v1/search/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(5));
    }

    // ==================== 搜索历史接口测试 ====================

    @Test
    @Order(11)
    @DisplayName("搜索历史 - 正常获取")
    void testGetSearchHistory() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/search/history")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("搜索历史 - 未携带Token")
    void testGetSearchHistoryNoToken() {
        given()
                .when()
                .get("/api/v1/search/history")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(13)
    @DisplayName("清除搜索历史 - 正常流程")
    void testClearSearchHistory() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/search/history")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(14)
    @DisplayName("清除搜索历史 - 未携带Token")
    void testClearSearchHistoryNoToken() {
        given()
                .when()
                .delete("/api/v1/search/history")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("清除搜索历史 - 验证清除后历史为空")
    void testSearchHistoryAfterClear() {
        // 先清除
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/search/history")
                .then()
                .statusCode(200);

        // 再查询，验证历史为空
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/search/history")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", equalTo(0));
    }
}
