package main.java.services;

import main.java.enums.TransactionType;
import main.java.models.Transaction;
import main.java.models.UserBankAccount;
import main.java.repositories.TransactionRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class TransactionService {

    private TransactionRepository transactionRepository = new TransactionRepository();

    public ArrayList<Transaction> getTransactionsForUser(int userId) {
        return transactionRepository.findByUserId(userId);
    }

    public ArrayList<Transaction> getTransactionsForUserByType(int userId, TransactionType transactionType) {
        return transactionRepository.findByUserIdAndType(userId, transactionType);
    }

    public ArrayList<Transaction> getTransactionsForUserByAccount(int userId, int accountId) {
        return transactionRepository.findByUserIdAndAccountId(userId, accountId);
    }

    public Transaction getLatestTransactionForAccount(int accountId) {
        return transactionRepository.findLatestByAccountId(accountId);
    }

    public void printTransactionReceipt(Transaction transaction, UserBankAccount account) {
        if (transaction == null || account == null) {
            System.out.println("Unable to print receipt.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("===== Transaction Receipt =====");
        System.out.println("Transaction ID: " + transaction.getTransactionId());
        System.out.println("Type: " + transaction.getTransactionType());
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.printf("Amount: $%.2f%n", transaction.getAmount());
        System.out.printf("New Balance: $%.2f%n", account.getBalance());
        System.out.println("Date: " + transaction.getCreatedAt().format(formatter));
        System.out.println("===============================");
    }
}
