package com.maitong.visitor.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CryptoUtils {

    private static final String DEFAULT_AES_KEY = "MTMTMTMTMTMTMTMTMTMTMTMTMTMTMTMT"; // 32 字节

    /**
     * AES-256 加密
     */
    public static String encryptAES(String data) {
        if (data == null || data.trim().isEmpty()) return data;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(DEFAULT_AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

    /**
     * AES-256 解密
     */
    public static String decryptAES(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.trim().isEmpty()) return encryptedBase64;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(DEFAULT_AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedBase64;
        }
    }

    /**
     * 身份证脱敏 (掩码倒数第 5 位到第 8 位，如需求说明书定义 3301021234****1234)
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) return idCard;
        int len = idCard.length();
        if (len == 18) {
            // 倒数第8位到倒数第5位 (索引 len-8 到 len-4)
            return idCard.substring(0, len - 8) + "****" + idCard.substring(len - 4);
        }
        return idCard.substring(0, 6) + "****" + idCard.substring(len - 4);
    }

    /**
     * 生成保密协议防篡改 SHA-256 审计链 Hash
     */
    public static String generateHashChain(String name, String idCardEnc, String timestampStr, String ip, String device, String version) {
        try {
            String raw = name + "|" + idCardEnc + "|" + timestampStr + "|" + ip + "|" + device + "|" + version;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "HASH_ERR_" + System.currentTimeMillis();
        }
    }
}
