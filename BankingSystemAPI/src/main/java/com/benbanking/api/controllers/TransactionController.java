package com.benbanking.api.controllers;

import com.benbanking.api.auth.RequestAuth;
import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String sort
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(transactionService.query(
                userId, accountId, type, status, category, search,
                parseDate(from), parseDate(to), sort, page, size
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(HttpServletRequest request, @PathVariable("id") int transactionId) {
        int userId = RequestAuth.requireSession(request).getUserId();
        return ResponseEntity.ok(transactionService.getForUser(userId, transactionId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String sort
    ) {
        int userId = RequestAuth.requireSession(request).getUserId();
        List<Transaction> rows = transactionService.queryForExport(
                userId, accountId, type, status, category, search, parseDate(from), parseDate(to), sort);

        String csv = toCsv(rows);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String toCsv(List<Transaction> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("reference,accountId,counterpartyAccountId,type,status,amount,runningBalance,category,description,memo,createdAt\n");
        for (Transaction t : rows) {
            sb.append(csvField(t.getReference())).append(',')
              .append(t.getAccountId()).append(',')
              .append(t.getCounterpartyAccountId() == null ? "" : t.getCounterpartyAccountId()).append(',')
              .append(t.getTransactionType()).append(',')
              .append(t.getStatus()).append(',')
              .append(t.getAmount()).append(',')
              .append(t.getRunningBalance() == null ? "" : t.getRunningBalance()).append(',')
              .append(t.getCategory()).append(',')
              .append(csvField(t.getDescription())).append(',')
              .append(csvField(t.getMemo())).append(',')
              .append(t.getCreatedAt())
              .append('\n');
        }
        return sb.toString();
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
