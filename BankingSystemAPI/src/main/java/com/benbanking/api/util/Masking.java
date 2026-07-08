package com.benbanking.api.util;

public final class Masking {

    private Masking() {
    }

    /** "ACCT-1234ABCD5678" -> "•••• 5678" (last 4 characters). */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "•••• " + (accountNumber == null ? "" : accountNumber);
        }
        return "•••• " + accountNumber.substring(accountNumber.length() - 4);
    }

    public static String lastInitial(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return "";
        }
        return lastName.substring(0, 1).toUpperCase() + ".";
    }
}
