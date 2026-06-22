package com.langtou.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 *
 * 提供手机号 AES-256-GCM 加密/解密及脱敏展示功能。
 * 密钥从环境变量 CRYPTO_KEY 读取，长度必须为 32 字节（256 位）。
 *
 * 密文格式（Base64 编码）：
 * [12字节 IV] + [密文] + [16字节 GCM Tag]
 */
public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /** GCM IV 长度（12字节 / 96位） */
    private static final int GCM_IV_LENGTH = 12;

    /** GCM Tag 长度（16字节 / 128位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** AES 密钥长度（32字节 / 256位） */
    private static final int AES_KEY_LENGTH = 32;

    /** 环境变量名 */
    private static final String ENV_CRYPTO_KEY = "CRYPTO_KEY";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 加密手机号（AES-256-GCM）
     *
     * @param plaintext 明文手机号
     * @param key       加密密钥（32字节 Base64 编码或原始字符串，不足32字节自动补零）
     * @return Base64 编码的密文（格式：IV + 密文 + Tag）
     * @throws IllegalArgumentException 如果明文为空或密钥无效
     */
    public static String encrypt(String plaintext, String key) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("加密密钥不能为空");
        }

        try {
            byte[] keyBytes = normalizeKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // 加密
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 组合 IV + 密文（GCM 模式下 cipherText 已包含 Tag）
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密手机号
     *
     * @param ciphertext Base64 编码的密文
     * @param key        加密密钥（与加密时使用的密钥一致）
     * @return 明文手机号
     * @throws IllegalArgumentException 如果密文为空或密钥无效
     */
    public static String decrypt(String ciphertext, String key) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("密文不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("解密密钥不能为空");
        }

        try {
            byte[] keyBytes = normalizeKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            // Base64 解码
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // 提取 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // 解密
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 手机号脱敏展示（138****1234）
     *
     * 规则：保留前3位和后4位，中间用 **** 替代。
     * 如果手机号格式不合法（长度不足7位），则返回原值。
     *
     * @param phone 手机号（明文或密文均可，建议传入明文）
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 从环境变量获取加密密钥
     *
     * @return 加密密钥字符串
     * @throws IllegalStateException 如果环境变量未配置
     */
    public static String getKeyFromEnv() {
        String key = System.getenv(ENV_CRYPTO_KEY);
        if (key == null || key.isEmpty()) {
            key = System.getProperty(ENV_CRYPTO_KEY);
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("加密密钥未配置，请设置环境变量 " + ENV_CRYPTO_KEY);
        }
        return key;
    }

    /**
     * 标准化密钥：确保密钥长度为 32 字节
     *
     * 如果 key 已经是 32 字节的 Base64 编码，先尝试 Base64 解码。
     * 否则截取或补零到 32 字节。
     */
    private static byte[] normalizeKey(String key) {
        byte[] keyBytes;

        // 先尝试 Base64 解码
        try {
            byte[] decoded = Base64.getDecoder().decode(key);
            if (decoded.length == AES_KEY_LENGTH) {
                return decoded;
            }
            keyBytes = decoded;
        } catch (Exception e) {
            // 非 Base64，使用原始字符串
            keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        // 标准化到 32 字节
        byte[] normalized = new byte[AES_KEY_LENGTH];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, AES_KEY_LENGTH));
        return normalized;
    }
}
