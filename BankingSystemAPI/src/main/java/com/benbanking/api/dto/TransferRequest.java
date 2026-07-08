package com.benbanking.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TransferRequest {

    @NotNull
    private Integer fromAccountId;

    @NotNull
    private Integer toAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000.00.")
    @Digits(integer = 17, fraction = 2, message = "Amount may have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(max = 140)
    private String memo;

    public Integer getFromAccountId() {
        return fromAccountId;
    }

    public Integer getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMemo() {
        return memo;
    }
}
