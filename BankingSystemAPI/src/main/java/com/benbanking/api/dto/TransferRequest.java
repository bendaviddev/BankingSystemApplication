package com.benbanking.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {

    @NotNull
    private Integer fromAccountId;

    @NotNull
    private Integer toAccountId;

    @DecimalMin(value = "0.01")
    private double amount;

    public Integer getFromAccountId() {
        return fromAccountId;
    }

    public Integer getToAccountId() {
        return toAccountId;
    }

    public double getAmount() {
        return amount;
    }
}
