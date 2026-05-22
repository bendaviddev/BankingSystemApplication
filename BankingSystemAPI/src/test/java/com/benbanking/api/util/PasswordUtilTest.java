package com.benbanking.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void validPasswordPassesRules() {
        assertTrue(PasswordUtil.isValidPassword("Demo123!"));
    }

    @Test
    void weakPasswordFailsRules() {
        assertFalse(PasswordUtil.isValidPassword("password"));
        assertFalse(PasswordUtil.isValidPassword("short1!"));
    }

    @Test
    void bcryptHashAndVerify() {
        String hash = PasswordUtil.hashPassword("Demo123!");
        assertTrue(PasswordUtil.isBcryptHash(hash));
        assertTrue(PasswordUtil.checkPassword("Demo123!", hash));
        assertFalse(PasswordUtil.checkPassword("WrongPass1!", hash));
    }
}
