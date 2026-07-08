package com.benbanking.api.models;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserBankAccount {

    private int accountId;
    private int userId;
    private String accountNumber;
    private BankAccountType accountType;
    private Currency currency;
    private BigDecimal balance;
    private BankAccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserBankAccount(
            int accountId,
            int userId,
            String accountNumber,
            BankAccountType accountType,
            Currency currency,
            BigDecimal balance,
            BankAccountStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getUserId() {
        return userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BankAccountType getAccountType() {
        return accountType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(BankAccountStatus status) {
        this.status = status;
    }
}
