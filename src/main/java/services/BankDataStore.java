package main.java.services;

import java.util.ArrayList;

import main.java.enums.ActivityType;
import main.java.enums.BankAccountStatus;
import main.java.enums.BankAccountType;
import main.java.enums.TransactionType;
import main.java.models.Log;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.repositories.AccountRepository;
import main.java.repositories.LogRepository;
import main.java.repositories.TransactionRepository;
import main.java.repositories.UserRepository;

public class BankDataStore {

    private static final AuthService authService = new AuthService();
    private static final AccountService accountService = new AccountService();
    private static final TransactionService transactionService = new TransactionService();
    private static final LogService logService = new LogService();

    private static final UserRepository userRepository = new UserRepository();
    private static final AccountRepository accountRepository = new AccountRepository();
    private static final TransactionRepository transactionRepository = new TransactionRepository();
    private static final LogRepository logRepository = new LogRepository();

    private BankDataStore() {
        // Prevent creating BankDataStore objects.
    }

    public static User registerUser(String username, String password, String firstName,
            String lastName, String phoneNumber, String email, String address) {
        return authService.registerUser(username, password, firstName, lastName, phoneNumber, email, address);
    }

    public static User loginUser(String username, String password) {
        User user = authService.loginUser(username, password);

        if (user != null) {
            addLog(user.getId(), ActivityType.LOGIN, "User logged in.");
        }

        return user;
    }

    public static User findUserByUsername(String username) {
        return authService.findUserByUsername(username);
    }

    public static void updateUserProfile(User user, String firstName, String lastName,
            String phoneNumber, String email, String address) {
        authService.updateUserProfile(user, firstName, lastName, phoneNumber, email, address);
        addLog(user.getId(), ActivityType.UPDATE_PERSONAL_INFORMATION, "User updated profile information.");
    }

    public static UserBankAccount openAccount(int userId, BankAccountType accountType, double openingBalance) {
        return accountService.openAccount(userId, accountType, openingBalance);
    }

    public static ArrayList<UserBankAccount> getAccountsByUserId(int userId) {
        return accountService.getAccountsByUserId(userId);
    }

    public static UserBankAccount getAccountByIdForUser(int accountId, int userId) {
        return accountService.getAccountByIdForUser(accountId, userId);
    }

    public static UserBankAccount getAccountById(int accountId) {
        return accountService.getAccountById(accountId);
    }

    public static boolean updateAccountStatus(int accountId, BankAccountStatus newStatus) {
        return accountService.updateAccountStatus(accountId, newStatus);
    }

    public static boolean deposit(int userId, int accountId, double amount) {
        return accountService.deposit(userId, accountId, amount);
    }

    public static boolean withdraw(int userId, int accountId, double amount) {
        return accountService.withdraw(userId, accountId, amount);
    }

    public static boolean transfer(int userId, int fromAccountId, int toAccountId, double amount) {
        return accountService.transfer(userId, fromAccountId, toAccountId, amount);
    }

    public static ArrayList<Transaction> getTransactionsForUser(int userId) {
        return transactionService.getTransactionsForUser(userId);
    }

    public static ArrayList<Transaction> getTransactionsForUserByType(int userId, TransactionType transactionType) {
        return transactionService.getTransactionsForUserByType(userId, transactionType);
    }

    public static ArrayList<Transaction> getTransactionsForUserByAccount(int userId, int accountId) {
        return transactionService.getTransactionsForUserByAccount(userId, accountId);
    }

    public static Transaction getLatestTransactionForAccount(int accountId) {
        return transactionService.getLatestTransactionForAccount(accountId);
    }

    public static void printTransactionReceipt(Transaction transaction, UserBankAccount account) {
        transactionService.printTransactionReceipt(transaction, account);
    }

    public static ArrayList<Log> getLogsByUserId(int userId) {
        return logService.getLogsByUserId(userId);
    }

    public static void addLog(int userId, ActivityType activityType, String message) {
        logService.addLog(userId, activityType, message);
    }

    public static void printAllUsers() {
        ArrayList<User> users = userRepository.findAll();

        System.out.println("==== All Users ====");

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        for (User user : users) {
            System.out.println("User ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Name: " + user.getFullName());
            System.out.println("Email: " + user.getEmail());
            System.out.println("Role: " + user.getRole());
            System.out.println("--------------------------");
        }
    }

    public static void printAllAccounts() {
        ArrayList<UserBankAccount> accounts = accountRepository.findAll();

        System.out.println("==== All Bank Accounts ====");

        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }

        for (UserBankAccount account : accounts) {
            account.printAccount();
            System.out.println("--------------------------");
        }
    }

    public static void printAllTransactions() {
        ArrayList<Transaction> transactions = transactionRepository.findAll();

        System.out.println("==== All Transactions ====");

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        for (Transaction transaction : transactions) {
            transaction.printTransaction();
            System.out.println("--------------------------");
        }
    }

    public static void printAllLogs() {
        ArrayList<Log> logs = logRepository.findAll();

        System.out.println("==== All Logs ====");

        if (logs.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }

        for (Log log : logs) {
            log.printLog();
            System.out.println("--------------------------");
        }
    }
}
