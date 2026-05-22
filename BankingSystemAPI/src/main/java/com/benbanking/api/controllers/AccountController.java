package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.dto.DepositRequest;
import com.benbanking.api.dto.OpenAccountRequest;
import com.benbanking.api.dto.TransferRequest;
import com.benbanking.api.dto.WithdrawRequest;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.services.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<?> getAccounts(HttpServletRequest request) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> openAccount(
            HttpServletRequest request,
            @Valid @RequestBody OpenAccountRequest body
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        UserBankAccount account = accountService.openAccount(
                userId,
                body.getAccountType(),
                body.getOpeningBalance()
        );

        if (account == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Could not open account.")
            );
        }

        return ResponseEntity.ok(account);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            HttpServletRequest request,
            @Valid @RequestBody DepositRequest body
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        boolean success = accountService.deposit(userId, body.getAccountId(), body.getAmount());

        if (!success) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Deposit failed.")
            );
        }

        return ResponseEntity.ok(Map.of("message", "Deposit successful."));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            HttpServletRequest request,
            @Valid @RequestBody WithdrawRequest body
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        boolean success = accountService.withdraw(userId, body.getAccountId(), body.getAmount());

        if (!success) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Withdrawal failed.")
            );
        }

        return ResponseEntity.ok(Map.of("message", "Withdrawal successful."));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            HttpServletRequest request,
            @Valid @RequestBody TransferRequest body
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        boolean success = accountService.transfer(
                userId,
                body.getFromAccountId(),
                body.getToAccountId(),
                body.getAmount()
        );

        if (!success) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Transfer failed.")
            );
        }

        return ResponseEntity.ok(Map.of("message", "Transfer successful."));
    }
}
