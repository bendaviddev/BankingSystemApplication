package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.dto.AccountLookupResponse;
import com.benbanking.api.dto.AccountResponse;
import com.benbanking.api.dto.DepositRequest;
import com.benbanking.api.dto.ExternalTransferRequest;
import com.benbanking.api.dto.OpenAccountRequest;
import com.benbanking.api.dto.TransactionResponse;
import com.benbanking.api.dto.TransferRequest;
import com.benbanking.api.dto.WithdrawRequest;
import com.benbanking.api.exceptions.InsufficientFundsException;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.services.AccountService;
import com.benbanking.api.services.AccountService.ExternalTransferResult;
import com.benbanking.api.services.AccountService.TransferResult;
import com.benbanking.api.services.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AlertService alertService;

    public AccountController(AccountService accountService, AlertService alertService) {
        this.accountService = accountService;
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<?> getAccounts(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId).stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<?> openAccount(HttpServletRequest request, @Valid @RequestBody OpenAccountRequest body) {
        int userId = RequestAuth.requireSession(request).getUserId();
        UserBankAccount account = accountService.openAccount(userId, body.getAccountType(), body.getOpeningBalance());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccount(HttpServletRequest request, @PathVariable("id") int accountId) {
        int userId = RequestAuth.requireSession(request).getUserId();
        UserBankAccount account = accountService.getAccountForUser(accountId, userId);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam("accountNumber") String accountNumber) {
        AccountLookupResponse response = accountService.lookupAccount(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(HttpServletRequest request, @Valid @RequestBody DepositRequest body) {
        int userId = RequestAuth.requireSession(request).getUserId();
        Transaction txn = accountService.deposit(userId, body.getAccountId(), body.getAmount(), body.getCategory(), body.getMemo());
        alertService.fireForTransaction(userId, txn);
        return ResponseEntity.ok(TransactionResponse.from(txn));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(HttpServletRequest request, @Valid @RequestBody WithdrawRequest body) {
        int userId = RequestAuth.requireSession(request).getUserId();
        try {
            Transaction txn = accountService.withdraw(userId, body.getAccountId(), body.getAmount(), body.getCategory(), body.getMemo());
            alertService.fireForTransaction(userId, txn);
            return ResponseEntity.ok(TransactionResponse.from(txn));
        } catch (InsufficientFundsException e) {
            alertService.fireFailedTransaction(userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(HttpServletRequest request, @Valid @RequestBody TransferRequest body) {
        int userId = RequestAuth.requireSession(request).getUserId();
        try {
            TransferResult result = accountService.transferInternal(
                    userId, body.getFromAccountId(), body.getToAccountId(), body.getAmount(), body.getMemo());
            alertService.fireForTransaction(userId, result.outLeg());
            return ResponseEntity.ok(Map.of(
                    "outLeg", TransactionResponse.from(result.outLeg()),
                    "inLeg", TransactionResponse.from(result.inLeg())
            ));
        } catch (InsufficientFundsException e) {
            alertService.fireFailedTransaction(userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/transfer/external")
    public ResponseEntity<?> transferExternal(HttpServletRequest request, @Valid @RequestBody ExternalTransferRequest body) {
        int userId = RequestAuth.requireSession(request).getUserId();
        try {
            ExternalTransferResult result = accountService.transferExternal(
                    userId, body.getFromAccountId(), body.getToAccountNumber(), body.getAmount(), body.getMemo());
            alertService.fireForTransaction(userId, result.outLeg());
            alertService.fireTransferReceived(result.recipientUserId(), result.outLeg().getAmount(), result.senderDescription());
            return ResponseEntity.ok(Map.of(
                    "outLeg", TransactionResponse.from(result.outLeg()),
                    "inLeg", TransactionResponse.from(result.inLeg())
            ));
        } catch (InsufficientFundsException e) {
            alertService.fireFailedTransaction(userId, e.getMessage());
            throw e;
        }
    }
}
