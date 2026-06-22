package com.langtou.interact.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 互动服务接口测试
 * 覆盖点赞、取消点赞、评论、收藏、取消收藏等核心接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("互动服务接口测试")
public class InteractControllerApiTest {

    private static final String BASE_URL = "http://localhost:8083";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static final String CONTENT_SERVICE_URL = "http://localhost:8082";
    private static String accessToken;
    private static Long targetNoteId;
    private static Long createdCommentId;
    private static final String TEST_USERNAME = "interact_test_" + System.currentTimeMillis();
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
                    "email": "interact_test_" + System.currentTimeMillis() + "@example.com",
                    "nickname": "互动测试用户"
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

        // 创建一条笔记作为互动目标
        String noteBody = """
                {
                    "title": "互动测试目标笔记",
                    "content": "这是一条用于测试点赞、评论、收藏功能的笔记。",
                    "tags": ["互动", "测试"],
                    "category": "测试"
                }
                """;

        targetNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");
    }

    // ==================== 点赞相关测试 ====================

    @Test
    @Order(1)
    @DisplayName("点赞笔记 - 正常流程")
    void testLikeNoteSuccess() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", anyOf(nullValue(), notNullValue()));
    }

    @Test
    @Order(2)
    @DisplayName("点赞笔记 - 重复点赞")
    void testLikeNoteDuplicate() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(409)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(409), equalTo(1009)))
                .body("message", anyOf(containsString("已点赞"), containsString("重复"), containsString("成功")));
    }

    @Test
    @Order(3)
    @DisplayName("点赞笔记 - 目标不存在")
    void testLikeNoteNotFound() {
        String requestBody = """
                {
                    "targetId": 99999999,
                    "targetType": "NOTE"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)))
                .body("code", anyOf(equalTo(404), equalTo(400), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("目标")));
    }

    @Test
    @Order(4)
    @DisplayName("点赞笔记 - 未携带Token")
    void testLikeNoteNoToken() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(5)
    @DisplayName("点赞笔记 - 缺少targetType参数")
    void testLikeNoteMissingType() {
        String requestBody = """
                {
                    "targetId": %d
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("类型"), containsString("不能为空"), containsString("参数")));
    }

    // ==================== 取消点赞相关测试 ====================

    @Test
    @Order(6)
    @DisplayName("取消点赞 - 正常流程")
    void testUnlikeNoteSuccess() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/unlike")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(7)
    @DisplayName("取消点赞 - 未点赞过")
    void testUnlikeNoteNotLiked() {
        // 创建一个新笔记，确保没有点赞过
        String noteBody = """
                {
                    "title": "未点赞的笔记",
                    "content": "这条笔记从未被点赞"
                }
                """;

        Integer newNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(newNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/unlike")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(404), equalTo(1010)))
                .body("message", anyOf(containsString("未点赞"), containsString("不存在"), containsString("成功"), containsString("无需")));
    }

    @Test
    @Order(8)
    @DisplayName("取消点赞 - 未携带Token")
    void testUnlikeNoteNoToken() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/unlike")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    // ==================== 评论相关测试 ====================

    @Test
    @Order(9)
    @DisplayName("评论笔记 - 正常流程")
    void testCommentNoteSuccess() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE",
                    "content": "这是一条通过API测试添加的评论内容。"
                }
                """.formatted(targetNoteId);

        createdCommentId = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.id", notNullValue())
                .body("data.content", equalTo("这是一条通过API测试添加的评论内容。"))
                .body("data.targetId", equalTo(targetNoteId.intValue()))
                .body("data.author", notNullValue())
                .extract()
                .path("data.id");
    }

    @Test
    @Order(10)
    @DisplayName("评论笔记 - 内容为空")
    void testCommentNoteEmptyContent() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE",
                    "content": ""
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("内容"), containsString("不能为空"), containsString("参数")));
    }

    @Test
    @Order(11)
    @DisplayName("评论笔记 - 目标不存在")
    void testCommentNoteNotFound() {
        String requestBody = """
                {
                    "targetId": 99999998,
                    "targetType": "NOTE",
                    "content": "评论不存在的笔记"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)))
                .body("code", anyOf(equalTo(404), equalTo(400), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("目标")));
    }

    @Test
    @Order(12)
    @DisplayName("评论笔记 - 未携带Token")
    void testCommentNoteNoToken() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE",
                    "content": "未授权评论"
                }
                """.formatted(targetNoteId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    @Test
    @Order(13)
    @DisplayName("评论笔记 - 内容超长")
    void testCommentNoteContentTooLong() {
        String longContent = "超长评论".repeat(500);
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE",
                    "content": "%s"
                }
                """.formatted(targetNoteId, longContent);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .body("code", anyOf(equalTo(400), equalTo(1002)))
                .body("message", anyOf(containsString("内容"), containsString("长度"), containsString("参数")));
    }

    // ==================== 收藏相关测试 ====================

    @Test
    @Order(14)
    @DisplayName("收藏笔记 - 正常流程")
    void testCollectNoteSuccess() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", anyOf(nullValue(), notNullValue()));
    }

    @Test
    @Order(15)
    @DisplayName("收藏笔记 - 重复收藏")
    void testCollectNoteDuplicate() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(409)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(409), equalTo(1011)))
                .body("message", anyOf(containsString("已收藏"), containsString("重复"), containsString("成功")));
    }

    @Test
    @Order(16)
    @DisplayName("收藏笔记 - 目标不存在")
    void testCollectNoteNotFound() {
        String requestBody = """
                {
                    "targetId": 99999997,
                    "targetType": "NOTE"
                }
                """;

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)))
                .body("code", anyOf(equalTo(404), equalTo(400), equalTo(1004)))
                .body("message", anyOf(containsString("不存在"), containsString("未找到"), containsString("目标")));
    }

    @Test
    @Order(17)
    @DisplayName("收藏笔记 - 未携带Token")
    void testCollectNoteNoToken() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    // ==================== 取消收藏相关测试 ====================

    @Test
    @Order(18)
    @DisplayName("取消收藏 - 正常流程")
    void testUncollectNoteSuccess() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/uncollect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(19)
    @DisplayName("取消收藏 - 未收藏过")
    void testUncollectNoteNotCollected() {
        // 创建一个新笔记，确保没有收藏过
        String noteBody = """
                {
                    "title": "未收藏的笔记",
                    "content": "这条笔记从未被收藏"
                }
                """;

        Integer newNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(newNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/uncollect")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(404), equalTo(1012)))
                .body("message", anyOf(containsString("未收藏"), containsString("不存在"), containsString("成功"), containsString("无需")));
    }

    @Test
    @Order(20)
    @DisplayName("取消收藏 - 未携带Token")
    void testUncollectNoteNoToken() {
        String requestBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(targetNoteId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/interact/uncollect")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
                .body("code", anyOf(equalTo(401), equalTo(403), equalTo(1005)))
                .body("message", anyOf(containsString("Token"), containsString("认证"), containsString("未登录"), containsString("权限")));
    }

    // ==================== 完整流程测试 ====================

    @Test
    @Order(21)
    @DisplayName("完整互动流程 - 点赞后取消点赞")
    void testFullLikeUnlikeFlow() {
        // 创建新笔记用于完整流程测试
        String noteBody = """
                {
                    "title": "完整流程测试笔记",
                    "content": "用于测试完整互动流程"
                }
                """;

        Integer flowNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String likeBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(flowNoteId);

        // 点赞
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(likeBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 取消点赞
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(likeBody)
                .when()
                .post("/api/v1/interact/unlike")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(22)
    @DisplayName("完整互动流程 - 收藏后取消收藏")
    void testFullCollectUncollectFlow() {
        // 创建新笔记用于完整流程测试
        String noteBody = """
                {
                    "title": "收藏流程测试笔记",
                    "content": "用于测试收藏完整流程"
                }
                """;

        Integer flowNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String collectBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(flowNoteId);

        // 收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(collectBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 取消收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(collectBody)
                .when()
                .post("/api/v1/interact/uncollect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(23)
    @DisplayName("完整互动流程 - 点赞+评论+收藏")
    void testFullInteractFlow() {
        // 创建新笔记用于完整流程测试
        String noteBody = """
                {
                    "title": "综合互动测试笔记",
                    "content": "用于测试点赞评论收藏综合流程"
                }
                """;

        Integer flowNoteId = given()
                .baseUri(CONTENT_SERVICE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String targetBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE"
                }
                """.formatted(flowNoteId);

        // 点赞
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(targetBody)
                .when()
                .post("/api/v1/interact/like")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 评论
        String commentBody = """
                {
                    "targetId": %d,
                    "targetType": "NOTE",
                    "content": "综合测试评论"
                }
                """.formatted(flowNoteId);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(commentBody)
                .when()
                .post("/api/v1/interact/comment")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.content", equalTo("综合测试评论"));

        // 收藏
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(targetBody)
                .when()
                .post("/api/v1/interact/collect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }
}
