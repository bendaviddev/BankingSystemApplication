package com.benbanking.api.controllers;

import com.benbanking.api.auth.AuthSession;
import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.dto.AccountAdminResponse;
import com.benbanking.api.dto.AdminStatsResponse;
import com.benbanking.api.dto.AuditLogResponse;
import com.benbanking.api.dto.PagedResponse;
import com.benbanking.api.dto.TransactionResponse;
import com.benbanking.api.dto.UpdateAccountStatusRequest;
import com.benbanking.api.dto.UserResponse;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.UserRole;
import com.benbanking.api.exceptions.ForbiddenException;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.TransactionRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.services.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LogRepository logRepository;
    private final AccountService accountService;

    public AdminController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LogRepository logRepository,
            AccountService accountService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.logRepository = logRepository;
        this.accountService = accountService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest request) {
        requireAdmin(request);

        long totalUsers = userRepository.countAll();
        long totalAccounts = accountRepository.countAll();
        long totalTransactions = transactionRepository.countAll();
        BigDecimal totalVolume = transactionRepository.sumCompletedAmount();
        long failedTransactions24h = transactionRepository.countFailedSince(LocalDateTime.now().minusHours(24));

        return ResponseEntity.ok(new AdminStatsResponse(totalUsers, totalAccounts, totalTransactions, totalVolume, failedTransactions24h));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request, @RequestParam(required = false) String search) {
        requireAdmin(request);
        List<UserResponse> users = userRepository.search(search).stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts(HttpServletRequest request, @RequestParam(required = false) String search) {
        requireAdmin(request);
        List<AccountAdminResponse> accounts = accountRepository.searchWithOwner(search).stream()
                .map(AccountAdminResponse::from)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Integer page
    ) {
        requireAdmin(request);
        int size = 20;
        int pageNum = page == null || page < 0 ? 0 : page;

        List<Transaction> rows = transactionRepository.findAllForAdmin(status, size, pageNum * size);
        long total = transactionRepository.countAllForAdmin(status);
        List<TransactionResponse> items = rows.stream().map(TransactionResponse::from).toList();

        return ResponseEntity.ok(new PagedResponse<>(items, pageNum, size, total));
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<?> updateAccountStatus(
            HttpServletRequest request,
            @PathVariable int accountId,
            @Valid @RequestBody UpdateAccountStatusRequest body
    ) {
        AuthSession admin = requireAdmin(request);
        accountService.updateAccountStatus(admin.getUserId(), accountId, body.getStatus());
        return ResponseEntity.ok(Map.of("message", "Account status updated successfully."));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> auditLogs(HttpServletRequest request, @RequestParam(required = false) Integer page) {
        requireAdmin(request);
        int size = 50;
        int pageNum = page == null || page < 0 ? 0 : page;

        List<AuditLogResponse> items = logRepository.findPaged(size, pageNum * size).stream()
                .map(AuditLogResponse::from)
                .toList();
        long total = logRepository.countAll();

        return ResponseEntity.ok(new PagedResponse<>(items, pageNum, size, total));
    }

    private AuthSession requireAdmin(HttpServletRequest request) {
        AuthSession session = RequestAuth.requireSession(request);
        if (session.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin access required.");
        }
        return session;
    }
}
