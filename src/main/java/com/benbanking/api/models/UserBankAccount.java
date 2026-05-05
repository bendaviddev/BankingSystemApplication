package main.java.models;

import main.java.enums.BankAccountStatus;
import main.java.enums.BankAccountType;
import main.java.enums.Currency;

public class UserBankAccount {
    private int accountId;
    private int userId;
    public String accountNumber;
    private BankAccountType accountType;
    private Currency currency;
    private double balance;
    private BankAccountStatus status;

    public UserBankAccount(int accountId, int userId, String accountNumber,BankAccountType accountType,
                           Currency currency, double balance, BankAccountStatus status) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
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

    public double getBalance() {
        return balance;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public void setStatus(BankAccountStatus status) {
    this.status = status;
}

    public void deposit(double amount) {
        balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount > balance || amount <= 0) {
            return false;
        }

        balance -= amount;
        return true;
    }

    public void printAccount() {
        System.out.println("Account ID: " + accountId);
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Type: " + accountType);
        System.out.println("Currency: " + currency);
        System.out.printf("Balance: $%.2f%n", balance);
        System.out.println("Status: " + status);
    }
}