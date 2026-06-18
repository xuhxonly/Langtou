package com.langtou.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private static String STATIC_SECRET;
    private static long STATIC_EXPIRATION;
    private static SecretKey STATIC_KEY;

    private static final int MIN_SECRET_LENGTH = 32;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT密钥未配置，请在配置文件中设置 jwt.secret 属性");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT密钥长度不足，当前长度: " + secret.length() + "，最小要求: " + MIN_SECRET_LENGTH);
        }
        STATIC_SECRET = secret;
        STATIC_EXPIRATION = expiration;
        STATIC_KEY = Keys.hmacShaKeyFor(STATIC_SECRET.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 密钥已初始化，长度: {} 字符", secret.length());
    }

    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, "USER");
    }

    public static String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return generateToken(claims);
    }

    public static String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + STATIC_EXPIRATION);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(STATIC_KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(STATIC_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return null;
        } catch (JwtException e) {
            log.error("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(STATIC_KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("username", String.class);
    }

    public static String getRole(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return "USER";
        }
        String role = claims.get("role", String.class);
        return role != null ? role : "USER";
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }
}
