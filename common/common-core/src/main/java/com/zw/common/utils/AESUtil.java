package com.zw.common.utils;

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
     * 解密前端加密的数据（完全配套）
     */
    public static String decrypt(String cipherTextB64) {
        // 1. 解码Base64
        byte[] combined = Base64.getDecoder().decode(cipherTextB64);

        // 2. 提取IV（前12字节）
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // 3. 提取密文（剩余部分）
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // 4. 解码主密钥
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyB64key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        try{
            // 5. 解密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        }catch (Exception e){
            // 处理异常
            return null;
        }

    }

    /**
     * 加密（后端加密，用于存储）
     */
    public static String encrypt(String plainText, String masterKeyB64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyB64);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);

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