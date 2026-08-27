package com.campus.common.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2-SHA256 密码哈希。零外部依赖（仅 JDK 自带）。
 * 存储格式：salt_b64$hash_b64，迭代 100k。
 */
public final class PasswordHasher {
    private static final int ITERATIONS = 100_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        return hashWithSalt(password, salt);
    }

    public static boolean verify(String password, String stored) {
        int sep = stored.indexOf('$');
        if (sep <= 0) return false;
        byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
        String expected = stored.substring(sep + 1);
        String actual = hashWithSalt(password, salt).substring(sep + 1);
        return constantTimeEquals(expected, actual);
    }

    private static String hashWithSalt(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            throw new IllegalStateException("hash failure", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
