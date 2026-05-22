package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(transactionService.getTransactionsForUser(userId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionsByAccount(
            HttpServletRequest request,
            @PathVariable int accountId
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(userId, accountId));
    }
}
