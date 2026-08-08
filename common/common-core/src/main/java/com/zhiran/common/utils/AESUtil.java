package com.zhiran.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 对称加密工具
 * 特点：加密后包含随机IV，每次加密结果都不同，更安全
 */
public class AESUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;  // bits
    private static final int GCM_IV_LENGTH = 12;     // bytes
    private static final String masterKeyB64key = "SzdnWThuUDJzWDltUTR2RjN3UjZ0RTFhTDVvWjhjVTA=";     // bytes

    /**
     * 解密
     */
    public static String decrypt(String cipherTextB64) {
        if (cipherTextB64 == null || cipherTextB64.isEmpty()) {
            throw new IllegalArgumentException("Cipher text cannot be null or empty");
        }
        // 1. 解码Base64
        byte[] combined = Base64.getDecoder().decode(cipherTextB64);

        // 2. 提取IV（前12字节）
        if (combined.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid cipher text: too short");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // 3. 提取密文（剩余部分）
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // 4. 解码主密钥
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyB64key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        try {
            // 5. 解密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new IllegalStateException("Decryption failed: authentication tag mismatch (possible tampering or wrong key)", e);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed: " + e.getMessage(), e);
        }

    }

    /**
     * 加密
     */
    public static String encrypt(String plainText, String masterKeyB64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyB64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Invalid key size: expected 32 bytes for AES-256, got " + keyBytes.length + " bytes");
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }
}