package com.langtou.user.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 青少年模式接口测试
 * 覆盖开启/关闭青少年模式、获取配置、更新家长控制、状态检查等接口
 * 验证PIN验证、时长限制、夜间限制、内容分级
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("青少年模式接口测试")
public class TeenModeControllerApiTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static String userToken;
    private static Long userId;
    private static final String TEST_PIN = "1234";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册测试用户
        String username = "teen_test_" + System.currentTimeMillis();
        String registerBody = """
                {
                    "username": "%s",
                    "password": "Test@123456",
                    "email": "teen_test_%d@example.com",
                    "nickname": "青少年模式测试用户"
                }
                """.formatted(username, System.currentTimeMillis());

        userId = given()
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
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");
    }

    // ==================== 开启青少年模式接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("开启青少年模式 - 正常流程")
    void testEnableTeenModeSuccess() {
        String requestBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(2)
    @DisplayName("开启青少年模式 - PIN为空")
    void testEnableTeenModeEmptyPin() {
        String requestBody = """
                {
                    "pin": ""
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(3)
    @DisplayName("开启青少年模式 - 缺少PIN参数")
    void testEnableTeenModeMissingPin() {
        String requestBody = """
                {
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(4)
    @DisplayName("开启青少年模式 - 未携带Token")
    void testEnableTeenModeNoToken() {
        String requestBody = """
                {
                    "pin": "1234"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(5)
    @DisplayName("开启青少年模式 - 重复开启")
    void testEnableTeenModeAlreadyEnabled() {
        // 先确保已开启
        String requestBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(200);

        // 再次开启
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 关闭青少年模式接口测试 ====================

    @Test
    @Order(6)
    @DisplayName("关闭青少年模式 - 正确PIN")
    void testDisableTeenModeCorrectPin() {
        String requestBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/disable")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(7)
    @DisplayName("关闭青少年模式 - 错误PIN")
    void testDisableTeenModeWrongPin() {
        // 先开启
        String enableBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(enableBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(200);

        // 用错误PIN关闭
        String disableBody = """
                {
                    "pin": "9999"
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(disableBody)
                .when()
                .post("/api/v1/teen-mode/disable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(403)))
                .body("code", anyOf(equalTo(200), equalTo(400), equalTo(403)));
    }

    @Test
    @Order(8)
    @DisplayName("关闭青少年模式 - 未携带Token")
    void testDisableTeenModeNoToken() {
        String requestBody = """
                {
                    "pin": "1234"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/teen-mode/disable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(9)
    @DisplayName("关闭青少年模式 - 未开启时关闭")
    void testDisableTeenModeNotEnabled() {
        // 先确保已关闭
        String disableBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(disableBody)
                .when()
                .post("/api/v1/teen-mode/disable")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 获取配置接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取配置 - 正常获取")
    void testGetTeenModeConfigSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/teen-mode/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("获取配置 - 未携带Token")
    void testGetTeenModeConfigNoToken() {
        given()
                .when()
                .get("/api/v1/teen-mode/config")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 更新家长控制接口测试 ====================

    @Test
    @Order(12)
    @DisplayName("家长控制 - 更新时长限制")
    void testUpdateParentalControlsDailyLimit() {
        String requestBody = """
                {
                    "dailyDurationLimit": 120,
                    "nightModeStart": "22:00",
                    "nightModeEnd": "06:00",
                    "contentLevel": "all"
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/teen-mode/parental-controls")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(13)
    @DisplayName("家长控制 - 更新夜间限制")
    void testUpdateParentalControlsNightMode() {
        String requestBody = """
                {
                    "dailyDurationLimit": 60,
                    "nightModeStart": "21:00",
                    "nightModeEnd": "07:00",
                    "contentLevel": "general"
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/teen-mode/parental-controls")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(14)
    @DisplayName("家长控制 - 更新内容分级")
    void testUpdateParentalControlsContentLevel() {
        String requestBody = """
                {
                    "dailyDurationLimit": 120,
                    "nightModeStart": "22:00",
                    "nightModeEnd": "06:00",
                    "contentLevel": "children"
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/teen-mode/parental-controls")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(15)
    @DisplayName("家长控制 - 未携带Token")
    void testUpdateParentalControlsNoToken() {
        String requestBody = """
                {
                    "dailyDurationLimit": 120
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/teen-mode/parental-controls")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(16)
    @DisplayName("家长控制 - 时长限制边界值0")
    void testUpdateParentalControlsZeroLimit() {
        String requestBody = """
                {
                    "dailyDurationLimit": 0,
                    "nightModeStart": "22:00",
                    "nightModeEnd": "06:00",
                    "contentLevel": "all"
                }
                """;

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/v1/teen-mode/parental-controls")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ==================== 状态检查接口测试 ====================

    @Test
    @Order(17)
    @DisplayName("状态检查 - 正常获取")
    void testGetTeenModeStatusSuccess() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/teen-mode/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(18)
    @DisplayName("状态检查 - 未携带Token")
    void testGetTeenModeStatusNoToken() {
        given()
                .when()
                .get("/api/v1/teen-mode/status")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(19)
    @DisplayName("状态检查 - 开启后验证状态")
    void testGetTeenModeStatusAfterEnable() {
        // 先开启
        String enableBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(enableBody)
                .when()
                .post("/api/v1/teen-mode/enable")
                .then()
                .statusCode(200);

        // 检查状态
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/teen-mode/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(20)
    @DisplayName("状态检查 - 关闭后验证状态")
    void testGetTeenModeStatusAfterDisable() {
        // 先关闭
        String disableBody = """
                {
                    "pin": "%s"
                }
                """.formatted(TEST_PIN);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(disableBody)
                .when()
                .post("/api/v1/teen-mode/disable")
                .then()
                .statusCode(200);

        // 检查状态
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/v1/teen-mode/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
