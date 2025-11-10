package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Утилита для хеширования
 */
public final class HashUtil {
    private HashUtil() {}

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось хешировать строку", e);
        }
    }
}

