package com.langtou.common.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试
 * 通过反射设置静态密钥，不依赖 Spring 容器
 */
class JwtUtilsTest {

    private static final String TEST_SECRET = "this-is-a-very-long-test-secret-key-for-jwt-signing-at-least-32-chars";

    @BeforeAll
    static void setUp() {
        try {
            // 通过反射设置 secret 字段
            Field secretField = JwtUtils.class.getDeclaredField("secret");
            secretField.setAccessible(true);

            // JwtUtils 是 Spring 组件，需要创建实例来设置实例字段
            // 但 STATIC_SECRET/STATIC_KEY 是 static 字段，需要通过 init() 方法设置
            // 先创建一个实例来注入 secret
            Object instance = org.springframework.beans.BeanUtils.instantiateClass(JwtUtils.class);
            secretField.set(instance, TEST_SECRET);

            // 设置 expiration 字段
            Field expirationField = JwtUtils.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(instance, 86400000L);

            // 调用 init() 方法初始化静态密钥
            Method initMethod = JwtUtils.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(instance);
        } catch (Exception e) {
            fail("Failed to initialize JwtUtils: " + e.getMessage());
        }
    }

    // ==================== Token 生成与验证测试 ====================

    @Test
    void testGenerateAndValidateToken() {
        String token = JwtUtils.generateToken(1L, "testuser");
        assertNotNull(token);
        assertTrue(JwtUtils.validateToken(token));
    }

    @Test
    void testGetUserIdFromToken() {
        String token = JwtUtils.generateToken(1L, "testuser");
        assertEquals(1L, JwtUtils.getUserId(token));
    }

    @Test
    void testGetUsernameFromToken() {
        String token = JwtUtils.generateToken(1L, "testuser");
        assertEquals("testuser", JwtUtils.getUsername(token));
    }

    // ==================== Token 角色测试 ====================

    @Test
    void testTokenWithRole() {
        String token = JwtUtils.generateToken(1L, "admin", "ADMIN");
        assertNotNull(token);
        assertEquals("ADMIN", JwtUtils.getRole(token));
    }

    @Test
    void testTokenDefaultRole() {
        String token = JwtUtils.generateToken(1L, "user");
        assertNotNull(token);
        assertEquals("USER", JwtUtils.getRole(token));
    }

    // ==================== 无效 Token 测试 ====================

    @Test
    void testInvalidToken() {
        assertFalse(JwtUtils.validateToken("invalid.token.here"));
        assertNull(JwtUtils.getUserId("invalid.token.here"));
        assertNull(JwtUtils.getUsername("invalid.token.here"));
    }

    @Test
    void testEmptyToken() {
        assertFalse(JwtUtils.validateToken(""));
        assertNull(JwtUtils.getUserId(""));
    }

    @Test
    void testNullToken() {
        assertFalse(JwtUtils.validateToken(null));
        assertNull(JwtUtils.getUserId(null));
    }

    // ==================== Token 过期测试 ====================

    @Test
    void testExpiredToken() {
        try {
            // 通过反射临时设置短过期时间
            Field expirationField = JwtUtils.class.getDeclaredField("STATIC_EXPIRATION");
            expirationField.setAccessible(true);
            expirationField.set(null, 1L); // 1ms 过期

            // 重新生成密钥（因为过期时间变了）
            Field secretField = JwtUtils.class.getDeclaredField("STATIC_SECRET");
            secretField.setAccessible(true);
            secretField.set(null, TEST_SECRET);

            // 重新初始化 STATIC_KEY
            Method initKeyMethod = JwtUtils.class.getDeclaredMethod("init");
            initKeyMethod.setAccessible(true);

            Object instance = org.springframework.beans.BeanUtils.instantiateClass(JwtUtils.class);
            Field sField = JwtUtils.class.getDeclaredField("secret");
            sField.setAccessible(true);
            sField.set(instance, TEST_SECRET);
            Field eField = JwtUtils.class.getDeclaredField("expiration");
            eField.setAccessible(true);
            eField.set(instance, 1L);
            initKeyMethod.invoke(instance);

            String token = JwtUtils.generateToken(1L, "testuser");
            Thread.sleep(100);

            assertFalse(JwtUtils.validateToken(token));
            assertNull(JwtUtils.getUserId(token));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail("Failed to test expired token: " + e.getMessage());
        }
    }

    // ==================== Token 解析测试 ====================

    @Test
    void testParseToken() {
        String token = JwtUtils.generateToken(1L, "testuser", "ADMIN");
        var claims = JwtUtils.parseToken(token);
        assertNotNull(claims);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("testuser", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void testParseExpiredToken() {
        try {
            // 生成短期 token
            Field expirationField = JwtUtils.class.getDeclaredField("STATIC_EXPIRATION");
            expirationField.setAccessible(true);
            expirationField.set(null, 1L);

            Field secretField = JwtUtils.class.getDeclaredField("STATIC_SECRET");
            secretField.setAccessible(true);
            secretField.set(null, TEST_SECRET);

            Object instance = org.springframework.beans.BeanUtils.instantiateClass(JwtUtils.class);
            Field sField = JwtUtils.class.getDeclaredField("secret");
            sField.setAccessible(true);
            sField.set(instance, TEST_SECRET);
            Field eField = JwtUtils.class.getDeclaredField("expiration");
            eField.setAccessible(true);
            eField.set(instance, 1L);

            Method initKeyMethod = JwtUtils.class.getDeclaredMethod("init");
            initKeyMethod.setAccessible(true);
            initKeyMethod.invoke(instance);

            String token = JwtUtils.generateToken(1L, "testuser");
            Thread.sleep(100);

            var claims = JwtUtils.parseToken(token);
            assertNull(claims); // 过期 token 解析应返回 null
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail("Failed to test parse expired token: " + e.getMessage());
        }
    }

    // ==================== Token 过期状态检查 ====================

    @Test
    void testIsTokenExpired() {
        String token = JwtUtils.generateToken(1L, "testuser");
        assertFalse(JwtUtils.isTokenExpired(token));
    }

    // ==================== 不同用户 Token 隔离测试 ====================

    @Test
    void testDifferentUserTokens() {
        String token1 = JwtUtils.generateToken(1L, "user1");
        String token2 = JwtUtils.generateToken(2L, "user2");

        assertNotEquals(token1, token2);
        assertEquals(1L, JwtUtils.getUserId(token1));
        assertEquals(2L, JwtUtils.getUserId(token2));
        assertEquals("user1", JwtUtils.getUsername(token1));
        assertEquals("user2", JwtUtils.getUsername(token2));
    }
}
