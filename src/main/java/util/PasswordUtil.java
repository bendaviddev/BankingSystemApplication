package main.java.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static String readPassword(String prompt, Scanner scan) {
    System.out.print(prompt);
    return scan.nextLine();
}

    public static boolean isValidPassword(String password) {
        if (password.length() < 8) {
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

    public static void printPasswordRequirements() {
        System.out.println("Password must:");
        System.out.println("- Be at least 8 characters long");
        System.out.println("- Contain at least 1 number");
        System.out.println("- Contain at least 1 uppercase letter");
        System.out.println("- Contain at least 1 special character");
    }

    public static String hashPassword(String password) {
        try {
            byte[] salt = generateSalt();

            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hash);

            return saltString + ":" + hashString;

        } catch (Exception e) {
            throw new RuntimeException("Error hashing password.");
        }
    }

    public static boolean verifyPassword(String password, String storedPassword) {
        try {
            if (!storedPassword.contains(":")) {
                return password.equals(storedPassword);
            }

            String[] parts = storedPassword.split(":");

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] newHash = factory.generateSecret(spec).getEncoded();

            return slowEquals(storedHash, newHash);

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean needsHashUpdate(String storedPassword) {
        return !storedPassword.contains(":");
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length; i++) {
            result = result | (a[i] ^ b[i]);
        }

        return result == 0;
    }
}