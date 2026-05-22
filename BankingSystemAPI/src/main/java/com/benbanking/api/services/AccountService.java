package com.benbanking.api.services;

import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LogRepository logRepository;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LogRepository logRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.logRepository = logRepository;
    }

    public UserBankAccount openAccount(int userId, BankAccountType accountType, double openingBalance) {
        if (openingBalance < 0) {
            return null;
        }

        String accountNumber = "ACCT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

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
            addLog(userId, ActivityType.OPEN_NEW_BANK_ACCOUNT, "User opened a new bank account.");
        }

        return savedAccount;
    }

    public ArrayList<UserBankAccount> getAccountsByUserId(int userId) {
        return accountRepository.findByUserId(userId);
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

        if (account == null || amount <= 0 || account.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        account.deposit(amount);
        accountRepository.updateBalanceAndStatus(account);

        transactionRepository.save(new Transaction(
                0,
                accountId,
                TransactionType.DEPOSIT,
                amount,
                "Deposit into account"
        ));

        addLog(userId, ActivityType.DEPOSIT, "User deposited money.");
        return true;
    }

    public boolean withdraw(int userId, int accountId, double amount) {
        UserBankAccount account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account == null || amount <= 0 || account.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        if (!account.withdraw(amount)) {
            return false;
        }

        accountRepository.updateBalanceAndStatus(account);

        transactionRepository.save(new Transaction(
                0,
                accountId,
                TransactionType.WITHDRAWAL,
                amount,
                "Withdrawal from account"
        ));

        addLog(userId, ActivityType.WITHDRAWAL, "User withdrew money.");
        return true;
    }

    @Transactional
    public boolean transfer(int userId, int fromAccountId, int toAccountId, double amount) {
        UserBankAccount fromAccount = accountRepository.findByIdAndUserId(fromAccountId, userId);
        UserBankAccount toAccount = accountRepository.findById(toAccountId);

        if (fromAccount == null || toAccount == null || amount <= 0) {
            return false;
        }

        if (fromAccount.getStatus() != BankAccountStatus.ACTIVE || toAccount.getStatus() != BankAccountStatus.ACTIVE) {
            return false;
        }

        if (!fromAccount.withdraw(amount)) {
            return false;
        }

        toAccount.deposit(amount);

        accountRepository.updateBalanceAndStatus(fromAccount);
        accountRepository.updateBalanceAndStatus(toAccount);

        transactionRepository.save(new Transaction(
                0,
                fromAccountId,
                TransactionType.TRANSFER,
                amount,
                "Transfer sent to account ID " + toAccountId
        ));

        transactionRepository.save(new Transaction(
                0,
                toAccountId,
                TransactionType.TRANSFER,
                amount,
                "Transfer received from account ID " + fromAccountId
        ));

        addLog(userId, ActivityType.TRANSFER, "User transferred money.");
        return true;
    }

    private void addLog(int userId, ActivityType activityType, String message) {
        logRepository.save(new Log(0, userId, activityType, message));
    }
}
