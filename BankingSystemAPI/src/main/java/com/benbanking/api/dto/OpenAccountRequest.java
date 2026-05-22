package com.benbanking.api.dto;

import com.benbanking.api.enums.BankAccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OpenAccountRequest {

    @NotNull
    private BankAccountType accountType;

    @Min(0)
    private double openingBalance;

    public BankAccountType getAccountType() {
        return accountType;
    }

    public double getOpeningBalance() {
        return openingBalance;
    }
}
