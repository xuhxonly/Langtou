package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 内容服务接口测试
 * 覆盖笔记的发布、查询、更新、删除、搜索等核心接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("内容服务接口测试")
public class ContentControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String accessToken;
    private static Long createdNoteId;
    private static final String TEST_USERNAME = "content_test_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册测试用户
        String registerBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "content_test_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "内容测试用户"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

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
                    "password": "%s"
                }
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

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

    // ==================== 发布笔记相关测试 ====================

    @Test
    @Order(1)
    @DisplayName("发布笔记 - 正常流程")
    void testCreateNoteSuccess() {
        String requestBody = """
                {
                    "title": "API测试笔记标题",
                    "content": "这是通过API测试发布的笔记内容，包含一些测试文本。",
                    "images": [
                        "https://example.com/image1.png",
                        "https://example.com/image2.png"
                    ],
                    "tags": ["测试", "API", "自动化"],
                    "category": "技术"
                }
                """;

        createdNoteId = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.id", notNullValue())
                .body("data.title", equalTo("API测试笔记标题"))
                .body("data.content", containsString("API测试"))
                .body("data.author", notNullValue())
                .extract()
                .path("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("发布笔记 - 标题为空")
    void testCreateNoteEmptyTitle() {
        String requestBody = """
                {
                    "title": "",
                    "content": "内容不为空",
                    "tags": []
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("标题"), containsString("不能为空"), containsString("参数")));
    }

    @Test
    @Order(3)
    @DisplayName("发布笔记 - 内容为空")
    void testCreateNoteEmptyContent() {
        String requestBody = """
                {
                    "title": "有标题无内容",
                    "content": "",
                    "tags": []
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("内容"), containsString("不能为空"), containsString("参数")));
    }

    @Test
    @Order(4)
    @DisplayName("发布笔记 - 未携带Token")
    void testCreateNoteNoToken() {
        String requestBody = """
                {
                    "title": "未授权发布",
                    "content": "这条笔记不应该被发布"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(5)
    @DisplayName("发布笔记 - 标题超长")
    void testCreateNoteTitleTooLong() {
        String longTitle = "超长标题".repeat(50);
        String requestBody = """
                {
                    "title": "%s",
                    "content": "正常内容"
                }
                """.formatted(longTitle);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("标题"), containsString("长度"), containsString("参数")));
    }

    // ==================== 笔记列表相关测试 ====================

    @Test
    @Order(6)
    @DisplayName("获取笔记列表 - 正常流程")
    void testGetNoteListSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.list", notNullValue())
                .body("data.total", greaterThanOrEqualTo(0))
                .body("data.pageNum", equalTo(1))
                .body("data.pageSize", equalTo(10));
    }

    @Test
    @Order(7)
    @DisplayName("获取笔记列表 - 无Token（公开列表）")
    void testGetNoteListPublic() {
        given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.list", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("获取笔记列表 - 分页参数边界")
    void testGetNoteListPaginationEdge() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 0)
                .queryParam("size", 1000)
                .when()
                .get("/api/v1/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(1002)));
    }

    // ==================== 笔记详情相关测试 ====================

    @Test
    @Order(9)
    @DisplayName("获取笔记详情 - 正常流程")
    void testGetNoteDetailSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.id", equalTo(createdNoteId.intValue()))
                .body("data.title", equalTo("API测试笔记标题"))
                .body("data.content", containsString("API测试"))
                .body("data.author", notNullValue())
                .body("data.createTime", notNullValue());
    }

    @Test
    @Order(10)
    @DisplayName("获取笔记详情 - 笔记不存在")
    void testGetNoteDetailNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/notes/{noteId}", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(404), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("笔记")));
    }

    @Test
    @Order(11)
    @DisplayName("获取笔记详情 - 无Token访问公开笔记")
    void testGetNoteDetailPublic() {
        given()
                .when()
                .get("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.id", equalTo(createdNoteId.intValue()));
    }

    // ==================== 更新笔记相关测试 ====================

    @Test
    @Order(12)
    @DisplayName("更新笔记 - 正常流程")
    void testUpdateNoteSuccess() {
        String requestBody = """
                {
                    "title": "更新后的笔记标题",
                    "content": "这是更新后的笔记内容，通过API测试修改。",
                    "images": ["https://example.com/updated.png"],
                    "tags": ["更新", "测试"],
                    "category": "生活"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.title", equalTo("更新后的笔记标题"))
                .body("data.content", equalTo("这是更新后的笔记内容，通过API测试修改。"))
                .body("data.category", equalTo("生活"));
    }

    @Test
    @Order(13)
    @DisplayName("更新笔记 - 未携带Token")
    void testUpdateNoteNoToken() {
        String requestBody = """
                {
                    "title": "未授权更新"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(14)
    @DisplayName("更新笔记 - 更新不存在的笔记")
    void testUpdateNoteNotFound() {
        String requestBody = """
                {
                    "title": "更新不存在"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/notes/{noteId}", 99999998)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(404), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("笔记")));
    }

    @Test
    @Order(15)
    @DisplayName("更新笔记 - 更新他人笔记（无权限）")
    void testUpdateNoteNoPermission() {
        // 注册另一个用户
        String otherUsername = "other_user_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Other@123456",
                    "email": "other_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "另一个用户"
                }
                """.formatted(otherUsername);

        String otherToken = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 如果注册未返回token，则登录获取
        if (otherToken == null) {
            String loginBody = """
                    {
                        "username": "%s",
                        "password": "Other@123456"
                    }
                    """.formatted(otherUsername);
            otherToken = given()
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

        String requestBody = """
                {
                    "title": "尝试更新他人笔记"
                }
                """;

        given()
                .header("Authorization", "Bearer " + otherToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(403), equalTo(401)))
                .body("code", anyOf(equalTo(403), equalTo(401), equalTo(1008)))
                .body("message", anyOf(containsString("权限"), containsString("无权"), containsString("不能"), containsString("作者")));
    }

    // ==================== 删除笔记相关测试 ====================

    @Test
    @Order(16)
    @DisplayName("删除笔记 - 正常流程")
    void testDeleteNoteSuccess() {
        // 先创建一条待删除的笔记
        String requestBody = """
                {
                    "title": "待删除的笔记",
                    "content": "这条笔记将被删除"
                }
                """;

        Integer noteToDelete = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/notes/{noteId}", noteToDelete)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));

        // 验证笔记已被删除
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/notes/{noteId}", noteToDelete)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(404), equalTo(1004)));
    }

    @Test
    @Order(17)
    @DisplayName("删除笔记 - 未携带Token")
    void testDeleteNoteNoToken() {
        given()
                .when()
                .delete("/api/v1/notes/{noteId}", createdNoteId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(18)
    @DisplayName("删除笔记 - 删除不存在的笔记")
    void testDeleteNoteNotFound() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/notes/{noteId}", 99999997)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(404), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("笔记")));
    }

    // ==================== 搜索笔记相关测试 ====================

    @Test
    @Order(19)
    @DisplayName("搜索笔记 - 正常流程")
    void testSearchNotesSuccess() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "API测试")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.list", notNullValue())
                .body("data.total", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(20)
    @DisplayName("搜索笔记 - 空关键词")
    void testSearchNotesEmptyKeyword() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("关键词"), containsString("不能为空"), containsString("参数")));
    }

    @Test
    @Order(21)
    @DisplayName("搜索笔记 - 无结果")
    void testSearchNotesNoResult() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("keyword", "xyzabc123456不存在的词")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.list", empty());
    }

    @Test
    @Order(22)
    @DisplayName("搜索笔记 - 无Token（公开搜索）")
    void testSearchNotesPublic() {
        given()
                .queryParam("keyword", "测试")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/search/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.list", notNullValue());
    }
}
