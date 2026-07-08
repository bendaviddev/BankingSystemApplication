package com.benbanking.api.dto;

public class AccountLookupResponse {

    private final String maskedAccountNumber;
    private final String ownerFirstName;
    private final String ownerLastInitial;

    public AccountLookupResponse(String maskedAccountNumber, String ownerFirstName, String ownerLastInitial) {
        this.maskedAccountNumber = maskedAccountNumber;
        this.ownerFirstName = ownerFirstName;
        this.ownerLastInitial = ownerLastInitial;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public String getOwnerLastInitial() {
        return ownerLastInitial;
    }
}
