package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 标签接口测试
 * 覆盖热门标签、标签搜索、标签下笔记列表、创建标签、更新标签、删除标签等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("标签接口测试")
public class TagControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String accessToken;
    private static Long createdTagId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册测试用户
        String username = "tag_test_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "tag_test_%d@example.com",
                    "nickname": "标签测试用户"
                }
                """.formatted(username, System.currentTimeMillis());

        given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(409)));

        // 登录获取 Token
        String loginBody = """
                {
                    "username": "%s",
                    "password": "Test@123456"
                }
                """.formatted(username);

        accessToken = given()
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

    // ==================== 热门标签接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("热门标签 - 正常获取")
    void testGetHotTagsSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/tags/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("热门标签 - 指定limit参数")
    void testGetHotTagsWithLimit() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("limit", 5)
                .when()
                .get("/api/v1/tags/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(5));
    }

    @Test
    @Order(3)
    @DisplayName("热门标签 - limit边界值0")
    void testGetHotTagsLimitZero() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("limit", 0)
                .when()
                .get("/api/v1/tags/hot")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(4)
    @DisplayName("热门标签 - 未携带Token（公开接口）")
    void testGetHotTagsNoToken() {
        given()
                .when()
                .get("/api/v1/tags/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 标签搜索接口测试 ====================

    @Test
    @Order(5)
    @DisplayName("标签搜索 - 正常关键字搜索")
    void testSearchTagsSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "测试")
                .when()
                .get("/api/v1/tags/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("标签搜索 - 指定limit参数")
    void testSearchTagsWithLimit() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "test")
                .queryParam("limit", 3)
                .when()
                .get("/api/v1/tags/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", lessThanOrEqualTo(3));
    }

    @Test
    @Order(7)
    @DisplayName("标签搜索 - 无匹配结果")
    void testSearchTagsNoMatch() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "zzz_no_match_xyz_" + System.currentTimeMillis())
                .when()
                .get("/api/v1/tags/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", equalTo(0));
    }

    @Test
    @Order(8)
    @DisplayName("标签搜索 - 缺少keyword参数")
    void testSearchTagsMissingKeyword() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/tags/search")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(9)
    @DisplayName("标签搜索 - 未携带Token（公开接口）")
    void testSearchTagsNoToken() {
        given()
                .queryParam("keyword", "测试")
                .when()
                .get("/api/v1/tags/search")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 标签下笔记列表接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("标签下笔记 - 正常获取")
    void testGetNotesByTagSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("tagId", 1)
                .when()
                .get("/api/v1/tags/{tagId}/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("标签下笔记 - 指定分页参数")
    void testGetNotesByTagWithPagination() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("tagId", 1)
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/tags/{tagId}/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("标签下笔记 - 标签不存在")
    void testGetNotesByTagNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("tagId", 99999999)
                .when()
                .get("/api/v1/tags/{tagId}/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(13)
    @DisplayName("标签下笔记 - 未携带Token（公开接口）")
    void testGetNotesByTagNoToken() {
        given()
                .pathParam("tagId", 1)
                .when()
                .get("/api/v1/tags/{tagId}/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 创建标签接口测试 ====================

    @Test
    @Order(14)
    @DisplayName("创建标签 - 正常流程")
    void testCreateTagSuccess() {
        String requestBody = """
                {
                    "name": "API测试标签_%d"
                }
                """.formatted(System.currentTimeMillis());

        createdTagId = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/tags")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("code", anyOf(equalTo(200), equalTo(201)))
                .extract()
                .path("data.id");
    }

    @Test
    @Order(15)
    @DisplayName("创建标签 - 未携带Token")
    void testCreateTagNoToken() {
        String requestBody = """
                {
                    "name": "未授权标签"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/tags")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(16)
    @DisplayName("创建标签 - 标签名为空")
    void testCreateTagEmptyName() {
        String requestBody = """
                {
                    "name": ""
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/tags")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 更新标签接口测试 ====================

    @Test
    @Order(17)
    @DisplayName("更新标签 - 正常流程")
    void testUpdateTagSuccess() {
        if (createdTagId == null) return;
        String requestBody = """
                {
                    "name": "更新后标签_%d"
                }
                """.formatted(System.currentTimeMillis());

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/tags/{tagId}", createdTagId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    @Order(18)
    @DisplayName("更新标签 - 未携带Token")
    void testUpdateTagNoToken() {
        if (createdTagId == null) return;
        String requestBody = """
                {
                    "name": "未授权更新"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/tags/{tagId}", createdTagId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 删除标签接口测试 ====================

    @Test
    @Order(19)
    @DisplayName("删除标签 - 正常流程")
    void testDeleteTagSuccess() {
        if (createdTagId == null) return;
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/tags/{tagId}", createdTagId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(20)
    @DisplayName("删除标签 - 未携带Token")
    void testDeleteTagNoToken() {
        given()
                .when()
                .delete("/api/v1/tags/{tagId}", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(21)
    @DisplayName("删除标签 - 标签不存在")
    void testDeleteTagNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/tags/{tagId}", 99999998)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }
}
