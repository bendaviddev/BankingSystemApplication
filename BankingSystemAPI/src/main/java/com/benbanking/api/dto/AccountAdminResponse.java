package com.benbanking.api.dto;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.repositories.AccountRepository.AccountWithOwner;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountAdminResponse {

    private final int accountId;
    private final int userId;
    private final String ownerUsername;
    private final String accountNumber;
    private final BankAccountType accountType;
    private final Currency currency;
    private final BigDecimal balance;
    private final BankAccountStatus status;
    private final LocalDateTime createdAt;

    public AccountAdminResponse(int accountId, int userId, String ownerUsername, String accountNumber,
            BankAccountType accountType, Currency currency, BigDecimal balance, BankAccountStatus status,
            LocalDateTime createdAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.ownerUsername = ownerUsername;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AccountAdminResponse from(AccountWithOwner row) {
        var account = row.account();
        return new AccountAdminResponse(
                account.getAccountId(),
                account.getUserId(),
                row.ownerUsername(),
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

    public int getUserId() {
        return userId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
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
