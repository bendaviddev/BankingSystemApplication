package com.benbanking.api.dto;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.models.UserBankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private final int accountId;
    private final String accountNumber;
    private final BankAccountType accountType;
    private final Currency currency;
    private final BigDecimal balance;
    private final BankAccountStatus status;
    private final LocalDateTime createdAt;

    public AccountResponse(int accountId, String accountNumber, BankAccountType accountType, Currency currency,
            BigDecimal balance, BankAccountStatus status, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AccountResponse from(UserBankAccount account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public int getAccountId() {
        return accountId;
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
}
