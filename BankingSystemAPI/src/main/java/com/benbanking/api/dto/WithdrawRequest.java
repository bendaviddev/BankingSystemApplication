package com.benbanking.api.dto;

import com.benbanking.api.enums.TransactionCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class WithdrawRequest {

    @NotNull
    private Integer accountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000.00.")
    @Digits(integer = 17, fraction = 2, message = "Amount may have at most 2 decimal places.")
    private BigDecimal amount;

    private TransactionCategory category;

    @Size(max = 140)
    private String memo;

    public Integer getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public String getMemo() {
        return memo;
    }
}
