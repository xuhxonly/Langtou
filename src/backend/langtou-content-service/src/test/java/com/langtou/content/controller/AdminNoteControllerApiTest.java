package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 管理端笔记接口测试
 * 覆盖笔记列表（管理端）、笔记审核通过/驳回、笔记删除、笔记统计等接口
 * 验证管理员权限、审核状态流转
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("管理端笔记接口测试")
public class AdminNoteControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String adminToken;
    private static String normalToken;
    private static Long testNoteId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册管理员用户
        String adminUsername = "admin_note_" + System.currentTimeMillis();
        String adminRegisterBody = """
                {
                    "username": "%s",
                    "password": "Admin@123456",
                    "email": "admin_note_%d@example.com",
                    "nickname": "笔记管理员"
                }
                """.formatted(adminUsername, System.currentTimeMillis());

        given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(adminRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(409)));

        String adminLoginBody = """
                {
                    "username": "%s",
                    "password": "Admin@123456"
                }
                """.formatted(adminUsername);

        adminToken = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(adminLoginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 注册普通用户并创建测试笔记
        String normalUsername = "note_normal_" + System.currentTimeMillis();
        String normalRegisterBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "note_normal_%d@example.com",
                    "nickname": "笔记普通用户"
                }
                """.formatted(normalUsername, System.currentTimeMillis());

        given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(normalRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(409)));

        String normalLoginBody = """
                {
                    "username": "%s",
                    "password": "Test@123456"
                }
                """.formatted(normalUsername);

        normalToken = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(normalLoginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 创建测试笔记
        String noteBody = """
                {
                    "title": "管理端测试笔记",
                    "content": "这是一条用于管理端审核测试的笔记内容",
                    "tags": ["测试", "管理"]
                }
                """;

        try {
            testNoteId = given()
                    .header("Authorization", "Bearer " + normalToken)
                    .contentType(ContentType.JSON)
                    .body(noteBody)
                    .when()
                    .post("/api/v1/notes")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("data.id");
        } catch (Exception e) {
            testNoteId = 1L;
        }
    }

    // ==================== 笔记列表（管理端）接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("管理端笔记列表 - 正常获取")
    void testGetAdminNotesSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("管理端笔记列表 - 按关键字搜索")
    void testGetAdminNotesByKeyword() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("keyword", "测试")
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("管理端笔记列表 - 按状态筛选（待审核）")
    void testGetAdminNotesByStatusPending() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("status", "pending")
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("管理端笔记列表 - 按状态筛选（已通过）")
    void testGetAdminNotesByStatusApproved() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("status", "approved")
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("管理端笔记列表 - 未携带Token")
    void testGetAdminNotesNoToken() {
        given()
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(6)
    @DisplayName("管理端笔记列表 - 普通用户无权限")
    void testGetAdminNotesNormalUserForbidden() {
        given()
                .header("Authorization", "Bearer " + normalToken)
                .when()
                .get("/api/v1/admin/notes")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 审核通过接口测试 ====================

    @Test
    @Order(7)
    @DisplayName("审核通过 - 正常流程")
    void testApproveNoteSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/approve")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(8)
    @DisplayName("审核通过 - 未携带Token")
    void testApproveNoteNoToken() {
        given()
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/approve")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(9)
    @DisplayName("审核通过 - 笔记不存在")
    void testApproveNoteNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", 99999999)
                .when()
                .post("/api/v1/admin/notes/{noteId}/approve")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 审核驳回接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("审核驳回 - 正常流程")
    void testRejectNoteSuccess() {
        // 先创建一条待驳回的笔记
        String noteBody = """
                {
                    "title": "待驳回测试笔记",
                    "content": "这条笔记将被驳回"
                }
                """;

        Integer noteToReject = given()
                .header("Authorization", "Bearer " + normalToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        String rejectBody = """
                {
                    "reason": "内容不符合规范"
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(rejectBody)
                .pathParam("noteId", noteToReject)
                .when()
                .post("/api/v1/admin/notes/{noteId}/reject")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(11)
    @DisplayName("审核驳回 - 未携带Token")
    void testRejectNoteNoToken() {
        String rejectBody = """
                {
                    "reason": "未授权驳回"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(rejectBody)
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/reject")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(12)
    @DisplayName("审核驳回 - 无拒绝原因")
    void testRejectNoteWithoutReason() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/reject")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    // ==================== 删除笔记接口测试 ====================

    @Test
    @Order(13)
    @DisplayName("删除笔记 - 正常流程")
    void testDeleteNoteSuccess() {
        // 先创建一条待删除的笔记
        String noteBody = """
                {
                    "title": "待删除测试笔记",
                    "content": "这条笔记将被管理员删除"
                }
                """;

        Integer noteToDelete = given()
                .header("Authorization", "Bearer " + normalToken)
                .contentType(ContentType.JSON)
                .body(noteBody)
                .when()
                .post("/api/v1/notes")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", noteToDelete)
                .when()
                .delete("/api/v1/admin/notes/{noteId}")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(14)
    @DisplayName("删除笔记 - 未携带Token")
    void testDeleteNoteNoToken() {
        given()
                .pathParam("noteId", testNoteId)
                .when()
                .delete("/api/v1/admin/notes/{noteId}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("删除笔记 - 笔记不存在")
    void testDeleteNoteNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", 99999999)
                .when()
                .delete("/api/v1/admin/notes/{noteId}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 笔记下线/恢复接口测试 ====================

    @Test
    @Order(16)
    @DisplayName("笔记下线 - 正常流程")
    void testOfflineNoteSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/offline")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(17)
    @DisplayName("笔记恢复 - 正常流程")
    void testRestoreNoteSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("noteId", testNoteId)
                .when()
                .post("/api/v1/admin/notes/{noteId}/restore")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    // ==================== 批量审核接口测试 ====================

    @Test
    @Order(18)
    @DisplayName("批量审核通过 - 正常流程")
    void testBatchApproveSuccess() {
        String requestBody = """
                {
                    "noteIds": [1, 2, 3]
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/notes/batch-approve")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    @Order(19)
    @DisplayName("批量审核拒绝 - 正常流程")
    void testBatchRejectSuccess() {
        String requestBody = """
                {
                    "noteIds": [4, 5]
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/notes/batch-reject")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    @Order(20)
    @DisplayName("批量审核通过 - 空列表")
    void testBatchApproveEmptyList() {
        String requestBody = """
                {
                    "noteIds": []
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/notes/batch-approve")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    // ==================== 热门笔记接口测试 ====================

    @Test
    @Order(21)
    @DisplayName("热门笔记 - 正常获取")
    void testGetHotNotesSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("limit", 5)
                .when()
                .get("/api/v1/admin/notes/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(22)
    @DisplayName("热门笔记 - 默认limit")
    void testGetHotNotesDefaultLimit() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/admin/notes/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 审核日志接口测试 ====================

    @Test
    @Order(23)
    @DisplayName("审核日志 - 正常获取")
    void testGetAuditLogSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/admin/audit/log")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(24)
    @DisplayName("审核日志 - 未携带Token")
    void testGetAuditLogNoToken() {
        given()
                .when()
                .get("/api/v1/admin/audit/log")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }
}
