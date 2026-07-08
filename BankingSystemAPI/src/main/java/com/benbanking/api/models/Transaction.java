package com.benbanking.api.models;

import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private int transactionId;
    private String reference;
    private int accountId;
    private Integer counterpartyAccountId;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private TransactionCategory category;
    private String description;
    private String memo;
    private LocalDateTime createdAt;

    public Transaction(
            int transactionId,
            String reference,
            int accountId,
            Integer counterpartyAccountId,
            TransactionType transactionType,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal runningBalance,
            TransactionCategory category,
            String description,
            String memo,
            LocalDateTime createdAt
    ) {
        this.transactionId = transactionId;
        this.reference = reference;
        this.accountId = accountId;
        this.counterpartyAccountId = counterpartyAccountId;
        this.transactionType = transactionType;
        this.status = status;
        this.amount = amount;
        this.runningBalance = runningBalance;
        this.category = category;
        this.description = description;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getReference() {
        return reference;
    }

    public int getAccountId() {
        return accountId;
    }

    public Integer getCounterpartyAccountId() {
        return counterpartyAccountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
