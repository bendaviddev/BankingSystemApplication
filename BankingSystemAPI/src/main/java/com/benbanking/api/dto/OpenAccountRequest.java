package com.benbanking.api.dto;

import com.benbanking.api.enums.BankAccountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OpenAccountRequest {

    @NotNull
    private BankAccountType accountType;

    @NotNull
    @DecimalMin(value = "0.00", message = "Opening balance cannot be negative.")
    @DecimalMax(value = "1000000.00", message = "Opening balance cannot exceed 1,000,000.00.")
    @Digits(integer = 17, fraction = 2, message = "Opening balance may have at most 2 decimal places.")
    private BigDecimal openingBalance;

    public BankAccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
}
