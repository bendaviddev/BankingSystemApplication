package com.benbanking.api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtil {

    private static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasNumber = false;
        boolean hasUppercase = false;
        boolean hasSpecialCharacter = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if (Character.isDigit(c)) {
                hasNumber = true;
            }

            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            }

            if (!Character.isLetterOrDigit(c)) {
                hasSpecialCharacter = true;
            }
        }

        return hasNumber && hasUppercase && hasSpecialCharacter;
    }

    public static String hashPassword(String password) {
        return BCRYPT.encode(password);
    }

    public static boolean checkPassword(String password, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return BCRYPT.matches(password, storedPassword);
        }

        return verifyLegacyPbkdf2(password, storedPassword);
    }

    /** Supports passwords hashed before the BCrypt migration. */
    private static boolean verifyLegacyPbkdf2(String password, String storedPassword) {
        try {
            if (!storedPassword.contains(":")) {
                return false;
            }

            String[] parts = storedPassword.split(":");
            if (parts.length < 2) {
                return false;
            }

            byte[] salt = java.util.Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = java.util.Base64.getDecoder().decode(parts[1]);

            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    65536,
                    256
            );

            javax.crypto.SecretKeyFactory factory =
                    javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] newHash = factory.generateSecret(spec).getEncoded();

            return slowEquals(storedHash, newHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBcryptHash(String storedPassword) {
        return storedPassword != null
                && (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }
        return result == 0;
    }
}
