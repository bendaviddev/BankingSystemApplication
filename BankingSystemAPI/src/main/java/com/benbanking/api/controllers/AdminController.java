package com.benbanking.api.controllers;

import com.benbanking.api.dto.UpdateAccountStatusRequest;
import com.benbanking.api.dto.UserResponse;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public AdminController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream().map(UserResponse::from).toList()
        );
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<?> updateAccountStatus(
            @PathVariable int accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        boolean success = accountService.updateAccountStatus(accountId, request.getStatus());

        if (!success) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Could not update account status.")
            );
        }

        return ResponseEntity.ok(Map.of("message", "Account status updated successfully."));
    }
}
