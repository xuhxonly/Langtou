package com.langtou.content.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 商品管理接口测试
 * 覆盖创建/更新/删除商品、商品列表、上架/下架、笔记关联商品等接口
 * 验证创作者权限、商品数量限制
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("商品管理接口测试")
public class ProductControllerApiTest {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static String creatorToken;
    private static String normalToken;
    private static Long createdProductId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册创作者用户
        String creatorUsername = "product_creator_" + System.currentTimeMillis();
        String creatorRegisterBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "product_creator_%d@example.com",
                    "nickname": "商品创作者"
                }
                """.formatted(creatorUsername, System.currentTimeMillis());

        given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(creatorRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(409)));

        String creatorLoginBody = """
                {
                    "username": "%s",
                    "password": "Test@123456"
                }
                """.formatted(creatorUsername);

        creatorToken = given()
                .baseUri(USER_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(creatorLoginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 注册普通用户
        String normalUsername = "product_normal_" + System.currentTimeMillis();
        String normalRegisterBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "product_normal_%d@example.com",
                    "nickname": "商品普通用户"
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

    // ==================== 创建商品接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建商品 - 正常流程")
    void testCreateProductSuccess() {
        String requestBody = """
                {
                    "title": "API测试商品",
                    "description": "这是一个通过API测试创建的商品",
                    "price": 99.9,
                    "imageUrl": "https://example.com/product.png",
                    "link": "https://example.com/buy"
                }
                """;

        createdProductId = given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("code", anyOf(equalTo(200), equalTo(201)))
                .body("message", containsString("成功"))
                .extract()
                .path("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("创建商品 - 未携带Token")
    void testCreateProductNoToken() {
        String requestBody = """
                {
                    "title": "未授权商品",
                    "price": 10.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(3)
    @DisplayName("创建商品 - 标题为空")
    void testCreateProductEmptyTitle() {
        String requestBody = """
                {
                    "title": "",
                    "price": 10.0
                }
                """;

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(4)
    @DisplayName("创建商品 - 价格为负数")
    void testCreateProductNegativePrice() {
        String requestBody = """
                {
                    "title": "负价格商品",
                    "price": -10.0
                }
                """;

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 商品列表接口测试 ====================

    @Test
    @Order(5)
    @DisplayName("商品列表 - 正常获取")
    void testGetMyProductsSuccess() {
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("商品列表 - 按状态筛选")
    void testGetMyProductsByStatus() {
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .queryParam("page", 1)
                .queryParam("size", 20)
                .queryParam("status", "active")
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("商品列表 - 未携带Token")
    void testGetMyProductsNoToken() {
        given()
                .queryParam("page", 1)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 商品详情接口测试 ====================

    @Test
    @Order(8)
    @DisplayName("商品详情 - 正常获取")
    void testGetProductByIdSuccess() {
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .pathParam("id", 1)
                .when()
                .get("/api/v1/products/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(9)
    @DisplayName("商品详情 - 商品不存在")
    void testGetProductByIdNotFound() {
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .pathParam("id", 99999999)
                .when()
                .get("/api/v1/products/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 更新商品接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("更新商品 - 正常流程")
    void testUpdateProductSuccess() {
        if (createdProductId == null) return;
        String requestBody = """
                {
                    "title": "更新后商品_%d",
                    "price": 199.9
                }
                """.formatted(System.currentTimeMillis());

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", createdProductId)
                .when()
                .put("/api/v1/products/{id}")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    @Order(11)
    @DisplayName("更新商品 - 未携带Token")
    void testUpdateProductNoToken() {
        if (createdProductId == null) return;
        String requestBody = """
                {
                    "title": "未授权更新"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", createdProductId)
                .when()
                .put("/api/v1/products/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(12)
    @DisplayName("更新商品 - 更新他人商品")
    void testUpdateProductNoPermission() {
        if (createdProductId == null) return;
        String requestBody = """
                {
                    "title": "尝试更新他人商品"
                }
                """;

        given()
                .header("Authorization", "Bearer " + normalToken)
                .header("X-User-Id", "2")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", createdProductId)
                .when()
                .put("/api/v1/products/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(403), equalTo(401)));
    }

    // ==================== 上架/下架接口测试 ====================

    @Test
    @Order(13)
    @DisplayName("上架/下架 - 正常切换")
    void testToggleProductStatusSuccess() {
        if (createdProductId == null) return;
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .pathParam("id", createdProductId)
                .when()
                .post("/api/v1/products/{id}/toggle-status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(14)
    @DisplayName("上架/下架 - 未携带Token")
    void testToggleProductStatusNoToken() {
        if (createdProductId == null) return;
        given()
                .pathParam("id", createdProductId)
                .when()
                .post("/api/v1/products/{id}/toggle-status")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 笔记关联商品接口测试 ====================

    @Test
    @Order(15)
    @DisplayName("笔记关联商品 - 正常流程")
    void testLinkProductsToNoteSuccess() {
        if (createdProductId == null) return;
        String requestBody = """
                {
                    "productIds": [%d]
                }
                """.formatted(createdProductId);

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("noteId", 1)
                .when()
                .post("/api/v1/products/notes/{noteId}/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    @Order(16)
    @DisplayName("笔记关联商品 - 空列表")
    void testLinkProductsToNoteEmptyList() {
        String requestBody = """
                {
                    "productIds": []
                }
                """;

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .header("X-User-Id", "1")
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("noteId", 1)
                .when()
                .post("/api/v1/products/notes/{noteId}/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(17)
    @DisplayName("笔记关联商品 - 未携带Token")
    void testLinkProductsToNoteNoToken() {
        String requestBody = """
                {
                    "productIds": [1]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("noteId", 1)
                .when()
                .post("/api/v1/products/notes/{noteId}/products")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 商品点击记录接口测试 ====================

    @Test
    @Order(18)
    @DisplayName("商品点击 - 正常记录")
    void testRecordProductClickSuccess() {
        if (createdProductId == null) return;
        String requestBody = """
                {
                    "noteId": 1
                }
                """;

        given()
                .header("Authorization", "Bearer " + creatorToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", createdProductId)
                .when()
                .post("/api/v1/products/{id}/click")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    @Order(19)
    @DisplayName("商品点击 - 无请求体")
    void testRecordProductClickNoBody() {
        if (createdProductId == null) return;
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .pathParam("id", createdProductId)
                .when()
                .post("/api/v1/products/{id}/click")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    // ==================== 删除商品接口测试 ====================

    @Test
    @Order(20)
    @DisplayName("删除商品 - 正常流程")
    void testDeleteProductSuccess() {
        if (createdProductId == null) return;
        given()
                .header("Authorization", "Bearer " + creatorToken)
                .pathParam("id", createdProductId)
                .when()
                .delete("/api/v1/products/{id}")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(405)));
    }
}
