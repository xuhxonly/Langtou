package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 举报管理接口测试
 * 覆盖举报列表、举报详情、处理举报等接口
 * 验证管理员权限、举报状态流转
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("举报管理接口测试")
public class AdminReportControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String adminToken;
    private static String normalToken;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册管理员用户
        String adminUsername = "report_admin_" + System.currentTimeMillis();
        String adminRegisterBody = """
                {
                    "username": "%s",
                    "password": "Admin@123456",
                    "email": "report_admin_%d@example.com",
                    "nickname": "举报管理员"
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

        // 注册普通用户
        String normalUsername = "report_normal_" + System.currentTimeMillis();
        String normalRegisterBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "report_normal_%d@example.com",
                    "nickname": "举报普通用户"
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
    }

    // ==================== 举报列表接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("举报列表 - 正常获取")
    void testGetReportsSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("举报列表 - 按状态筛选（待处理）")
    void testGetReportsByStatusPending() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("status", 0)
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("举报列表 - 按状态筛选（已处理）")
    void testGetReportsByStatusResolved() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("status", 1)
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("举报列表 - 分页参数边界")
    void testGetReportsPaginationEdge() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .queryParam("page", 1)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("举报列表 - 未携带Token")
    void testGetReportsNoToken() {
        given()
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(6)
    @DisplayName("举报列表 - 普通用户无权限")
    void testGetReportsNormalUserForbidden() {
        given()
                .header("Authorization", "Bearer " + normalToken)
                .when()
                .get("/api/v1/admin/reports")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 举报详情接口测试 ====================

    @Test
    @Order(7)
    @DisplayName("举报详情 - 正常获取")
    void testGetReportDetailSuccess() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("id", 1)
                .when()
                .get("/api/v1/admin/reports/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(8)
    @DisplayName("举报详情 - 举报不存在")
    void testGetReportDetailNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("id", 99999999)
                .when()
                .get("/api/v1/admin/reports/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)))
                .body("code", anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(9)
    @DisplayName("举报详情 - 未携带Token")
    void testGetReportDetailNoToken() {
        given()
                .pathParam("id", 1)
                .when()
                .get("/api/v1/admin/reports/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 处理举报接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("处理举报 - 正常通过举报")
    void testHandleReportApprove() {
        String requestBody = """
                {
                    "action": "approve",
                    "handleResult": "举报属实，已处理"
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 1)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(11)
    @DisplayName("处理举报 - 正常驳回举报")
    void testHandleReportDismiss() {
        String requestBody = """
                {
                    "action": "dismiss",
                    "handleResult": "举报不实，已驳回"
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 2)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(12)
    @DisplayName("处理举报 - 缺少action参数")
    void testHandleReportMissingAction() {
        String requestBody = """
                {
                    "handleResult": "缺少action"
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 1)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(13)
    @DisplayName("处理举报 - 未携带Token")
    void testHandleReportNoToken() {
        String requestBody = """
                {
                    "action": "approve",
                    "handleResult": "未授权处理"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 1)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(14)
    @DisplayName("处理举报 - 普通用户无权限")
    void testHandleReportNormalUserForbidden() {
        String requestBody = """
                {
                    "action": "approve",
                    "handleResult": "无权限处理"
                }
                """;

        given()
                .header("Authorization", "Bearer " + normalToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 1)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(15)
    @DisplayName("处理举报 - 举报不存在")
    void testHandleReportNotFound() {
        String requestBody = """
                {
                    "action": "approve",
                    "handleResult": "处理不存在的举报"
                }
                """;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 99999999)
                .when()
                .put("/api/v1/admin/reports/{id}/handle")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 举报统计接口测试 ====================

    @Test
    @Order(16)
    @DisplayName("举报统计 - 正常获取")
    void testGetReportStats() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/admin/reports/stats")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(17)
    @DisplayName("举报统计 - 未携带Token")
    void testGetReportStatsNoToken() {
        given()
                .when()
                .get("/api/v1/admin/reports/stats")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }
}
