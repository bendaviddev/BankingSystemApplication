package com.benbanking.api.models;

import com.benbanking.api.enums.TransactionType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    private int transactionId;
    private int accountId;
    private TransactionType transactionType;
    private double amount;
    private String description;
    private LocalDateTime createdAt;

    public Transaction(int transactionId, int accountId, TransactionType transactionType, double amount, String description) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Transaction(int transactionId, int accountId, TransactionType transactionType, double amount, String description, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void printTransaction() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Account ID: " + accountId);
        System.out.println("Type: " + transactionType);
        System.out.printf("Amount: $%.2f%n", amount);
        System.out.println("Description: " + description);
        System.out.println("Date: " + createdAt.format(formatter));
    }
}
