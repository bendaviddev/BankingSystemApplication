package com.benbanking.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class DepositRequest {

    @NotNull
    private Integer accountId;

    @DecimalMin(value = "0.01")
    private double amount;

    public Integer getAccountId() {
        return accountId;
    }

    public double getAmount() {
        return amount;
    }
}
