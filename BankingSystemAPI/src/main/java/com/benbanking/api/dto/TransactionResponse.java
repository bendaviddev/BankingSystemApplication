package com.benbanking.api.dto;

import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private final int transactionId;
    private final String reference;
    private final int accountId;
    private final Integer counterpartyAccountId;
    private final TransactionType transactionType;
    private final TransactionStatus status;
    private final BigDecimal amount;
    private final BigDecimal runningBalance;
    private final TransactionCategory category;
    private final String description;
    private final String memo;
    private final LocalDateTime createdAt;

    public TransactionResponse(int transactionId, String reference, int accountId, Integer counterpartyAccountId,
            TransactionType transactionType, TransactionStatus status, BigDecimal amount, BigDecimal runningBalance,
            TransactionCategory category, String description, String memo, LocalDateTime createdAt) {
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

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getReference(),
                transaction.getAccountId(),
                transaction.getCounterpartyAccountId(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getRunningBalance(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getMemo(),
                transaction.getCreatedAt()
        );
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
