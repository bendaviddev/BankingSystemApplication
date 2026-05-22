package com.benbanking.api.services;

import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public ArrayList<Transaction> getTransactionsForUser(int userId) {
        return transactionRepository.findByUserId(userId);
    }

    public ArrayList<Transaction> getTransactionsByAccountId(int userId, int accountId) {
        UserBankAccount account = accountRepository.findByIdAndUserId(accountId, userId);
        if (account == null) {
            return new ArrayList<>();
        }
        return transactionRepository.findByUserIdAndAccountId(userId, accountId);
    }
}
