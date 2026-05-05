package main.java.services;

import main.java.enums.ActivityType;
import main.java.enums.BankAccountStatus;
import main.java.enums.BankAccountType;
import main.java.enums.Currency;
import main.java.enums.TransactionType;
import main.java.models.Transaction;
import main.java.models.UserBankAccount;
import main.java.repositories.AccountRepository;
import main.java.repositories.TransactionRepository;

import java.util.ArrayList;

public class AccountService {

    private AccountRepository accountRepository = new AccountRepository();
    private TransactionRepository transactionRepository = new TransactionRepository();
    private LogService logService = new LogService();

    public UserBankAccount openAccount(int userId, BankAccountType accountType, double openingBalance) {
        if (openingBalance < 0) {
            return null;
        }

        String accountNumber = "ACCT-" + System.currentTimeMillis();

        UserBankAccount account = new UserBankAccount(
                0,
                userId,
                accountNumber,
                accountType,
                Currency.DOLLAR,
                openingBalance,
                BankAccountStatus.ACTIVE
        );

        UserBankAccount savedAccount = accountRepository.save(account);

        if (savedAccount != null) {
            logService.addLog(userId, ActivityType.OPEN_NEW_BANK_ACCOUNT, "User opened a new bank account.");
        }

        return savedAccount;
    }

    public ArrayList<UserBankAccount> getAccountsByUserId(int userId) {
        return accountRepository.findByUserId(userId);
    }

    public UserBankAccount getAccountByIdForUser(int accountId, int userId) {
        return accountRepository.findByIdAndUserId(accountId, userId);
    }

    public UserBankAccount getAccountById(int accountId) {
        return accountRepository.findById(accountId);
    }

    public boolean updateAccountStatus(int accountId, BankAccountStatus newStatus) {
        UserBankAccount account = accountRepository.findById(accountId);

        if (account == null) {
            return false;
        }

        account.setStatus(newStatus);
        accountRepository.updateBalanceAndStatus(account);

        return true;
    }

    public boolean deposit(int userId, int accountId, double amount) {
        UserBankAccount account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account == null || amount <= 0) {
            return false;
        }

        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        account.deposit(amount);
        accountRepository.updateBalanceAndStatus(account);

        Transaction transaction = new Transaction(
                0,
                accountId,
                TransactionType.DEPOSIT,
                amount,
                "Deposit into account"
        );

        transactionRepository.save(transaction);
        logService.addLog(userId, ActivityType.DEPOSIT, "User deposited money.");

        return true;
    }

    public boolean withdraw(int userId, int accountId, double amount) {
        UserBankAccount account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account == null || amount <= 0) {
            return false;
        }

        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        boolean success = account.withdraw(amount);

        if (!success) {
            return false;
        }

        accountRepository.updateBalanceAndStatus(account);

        Transaction transaction = new Transaction(
                0,
                accountId,
                TransactionType.WITHDRAWAL,
                amount,
                "Withdrawal from account"
        );

        transactionRepository.save(transaction);
        logService.addLog(userId, ActivityType.WITHDRAWAL, "User withdrew money.");

        return true;
    }

    public boolean transfer(int userId, int fromAccountId, int toAccountId, double amount) {
        UserBankAccount fromAccount = accountRepository.findByIdAndUserId(fromAccountId, userId);
        UserBankAccount toAccount = accountRepository.findById(toAccountId);

        if (fromAccount == null || toAccount == null || amount <= 0) {
            return false;
        }

        if (fromAccount.getStatus() != BankAccountStatus.ACTIVE || toAccount.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        boolean withdrawn = fromAccount.withdraw(amount);

        if (!withdrawn) {
            return false;
        }

        toAccount.deposit(amount);

        accountRepository.updateBalanceAndStatus(fromAccount);
        accountRepository.updateBalanceAndStatus(toAccount);

        Transaction fromTransaction = new Transaction(
                0,
                fromAccountId,
                TransactionType.TRANSFER,
                amount,
                "Transfer sent to account ID " + toAccountId
        );

        Transaction toTransaction = new Transaction(
                0,
                toAccountId,
                TransactionType.TRANSFER,
                amount,
                "Transfer received from account ID " + fromAccountId
        );

        transactionRepository.save(fromTransaction);
        transactionRepository.save(toTransaction);

        logService.addLog(userId, ActivityType.TRANSFER, "User transferred money.");

        return true;
    }
}