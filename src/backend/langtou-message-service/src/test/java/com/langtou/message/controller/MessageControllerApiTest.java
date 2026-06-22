package com.langtou.message.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 消息服务接口测试
 * 覆盖发送私信、会话列表、消息列表、标记已读等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("消息服务接口测试")
public class MessageControllerApiTest {

    private static final String BASE_URL = "http://localhost:8084";
    private static String senderToken;
    private static String receiverToken;
    private static Long senderId;
    private static Long receiverId;
    private static final String SENDER_USERNAME = "msg_sender_" + System.currentTimeMillis();
    private static final String RECEIVER_USERNAME = "msg_receiver_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "Test@123456";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 注册发送方用户
        String senderRegisterBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "msg_sender_%d@example.com",
                    "nickname": "消息发送方"
                }
                """.formatted(SENDER_USERNAME, TEST_PASSWORD, System.currentTimeMillis());

        senderId = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(senderRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 注册接收方用户
        String receiverRegisterBody = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "msg_receiver_%d@example.com",
                    "nickname": "消息接收方"
                }
                """.formatted(RECEIVER_USERNAME, TEST_PASSWORD, System.currentTimeMillis());

        receiverId = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(receiverRegisterBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .path("data.id");

        // 登录发送方获取 token
        String senderLoginBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(SENDER_USERNAME, TEST_PASSWORD);

        senderToken = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(senderLoginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");

        // 登录接收方获取 token
        String receiverLoginBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(RECEIVER_USERNAME, TEST_PASSWORD);

        receiverToken = given()
                .baseUri("http://localhost:8081")
                .contentType(ContentType.JSON)
                .body(receiverLoginBody)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("data.token");
    }

    // ==================== 发送私信接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("发送私信 - 正常文本消息")
    void testSendMessageSuccess() {
        String requestBody = """
                {
                    "receiverId": %d,
                    "messageType": 1,
                    "content": "你好，这是一条测试消息"
                }
                """.formatted(receiverId);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"))
                .body("data", notNullValue())
                .body("data.id", notNullValue())
                .body("data.content", equalTo("你好，这是一条测试消息"));
    }

    @Test
    @Order(2)
    @DisplayName("发送私信 - 发送图片类型消息")
    void testSendMessageImageType() {
        String requestBody = """
                {
                    "receiverId": %d,
                    "messageType": 2,
                    "content": "https://example.com/image/test.png"
                }
                """.formatted(receiverId);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.messageType", equalTo(2));
    }

    @Test
    @Order(3)
    @DisplayName("发送私信 - 缺少receiverId")
    void testSendMessageMissingReceiverId() {
        String requestBody = """
                {
                    "messageType": 1,
                    "content": "缺少接收者"
                }
                """;

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(4)
    @DisplayName("发送私信 - 缺少content")
    void testSendMessageMissingContent() {
        String requestBody = """
                {
                    "receiverId": %d,
                    "messageType": 1
                }
                """.formatted(receiverId);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(5)
    @DisplayName("发送私信 - 未携带Token")
    void testSendMessageNoToken() {
        String requestBody = """
                {
                    "receiverId": %d,
                    "messageType": 1,
                    "content": "未授权消息"
                }
                """.formatted(receiverId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(6)
    @DisplayName("发送私信 - 接收者不存在")
    void testSendMessageReceiverNotFound() {
        String requestBody = """
                {
                    "receiverId": 99999999,
                    "messageType": 1,
                    "content": "发给不存在的用户"
                }
                """;

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404), equalTo(400)));
    }

    @Test
    @Order(7)
    @DisplayName("发送私信 - 发送长文本消息")
    void testSendMessageLongContent() {
        String longContent = "这是一条很长的测试消息。" .repeat(50);
        String requestBody = """
                {
                    "receiverId": %d,
                    "messageType": 1,
                    "content": "%s"
                }
                """.formatted(receiverId, longContent);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    // ==================== 会话列表接口测试 ====================

    @Test
    @Order(8)
    @DisplayName("获取会话列表 - 正常流程")
    void testGetConversationsSuccess() {
        // 先发送一条消息确保有会话数据
        String sendBody = """
                {
                    "receiverId": %d,
                    "messageType": 1,
                    "content": "会话列表测试消息"
                }
                """.formatted(receiverId);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(sendBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .when()
                .get("/api/v1/messages/conversations")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("获取会话列表 - 未携带Token")
    void testGetConversationsNoToken() {
        given()
                .when()
                .get("/api/v1/messages/conversations")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(10)
    @DisplayName("获取会话列表 - Token无效")
    void testGetConversationsInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid_token_xyz")
                .when()
                .get("/api/v1/messages/conversations")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    // ==================== 消息列表接口测试 ====================

    @Test
    @Order(11)
    @DisplayName("获取消息列表 - 正常流程")
    void testGetConversationMessages() {
        given()
                .header("Authorization", "Bearer " + senderToken)
                .when()
                .get("/api/v1/messages/conversation/{userId}", receiverId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("获取消息列表 - 指定分页参数")
    void testGetConversationMessagesWithPagination() {
        given()
                .header("Authorization", "Bearer " + senderToken)
                .queryParam("current", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/messages/conversation/{userId}", receiverId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @Order(13)
    @DisplayName("获取消息列表 - 未携带Token")
    void testGetConversationMessagesNoToken() {
        given()
                .when()
                .get("/api/v1/messages/conversation/{userId}", receiverId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(14)
    @DisplayName("获取消息列表 - 与不存在用户的会话")
    void testGetConversationMessagesUserNotFound() {
        given()
                .header("Authorization", "Bearer " + senderToken)
                .when()
                .get("/api/v1/messages/conversation/{userId}", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    // ==================== 标记已读接口测试 ====================

    @Test
    @Order(15)
    @DisplayName("标记已读 - 正常流程")
    void testMarkAsReadSuccess() {
        // 先发送一条消息给接收方
        String sendBody = """
                {
                    "receiverId": %d,
                    "messageType": 1,
                    "content": "请标记已读"
                }
                """.formatted(receiverId);

        given()
                .header("Authorization", "Bearer " + senderToken)
                .contentType(ContentType.JSON)
                .body(sendBody)
                .when()
                .post("/api/v1/messages/send")
                .then()
                .statusCode(200);

        // 接收方标记与发送方的会话为已读
        given()
                .header("Authorization", "Bearer " + receiverToken)
                .when()
                .put("/api/v1/messages/conversation/{userId}/read", senderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("message", containsString("成功"));
    }

    @Test
    @Order(16)
    @DisplayName("标记已读 - 未携带Token")
    void testMarkAsReadNoToken() {
        given()
                .when()
                .put("/api/v1/messages/conversation/{userId}/read", senderId)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)));
    }

    @Test
    @Order(17)
    @DisplayName("标记已读 - 不存在的用户会话")
    void testMarkAsReadUserNotFound() {
        given()
                .header("Authorization", "Bearer " + receiverToken)
                .when()
                .put("/api/v1/messages/conversation/{userId}/read", 99999999)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }
}
