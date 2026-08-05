package com.maitong.visitor.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class PasswordUtils {

    /**
     * 对管理员密码进行 SHA-256 加密存证
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("SALT_MAITONG_2026_" + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return rawPassword;
        }
    }

    /**
     * 校验初始密码或修改后的密码
     */
    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        // 支持明文直接比对（兼容模式）或 哈希比对
        if (storedHash.equals(rawPassword)) return true;
        String calculated = hashPassword(rawPassword);
        return calculated.equalsIgnoreCase(storedHash);
    }
}
