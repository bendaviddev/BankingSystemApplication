package com.benbanking.api.services;

import com.benbanking.api.dto.PagedResponse;
import com.benbanking.api.dto.TransactionResponse;
import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.repositories.TransactionRepository;
import com.benbanking.api.repositories.TransactionRepository.TransactionFilter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TransactionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public PagedResponse<TransactionResponse> query(
            int userId, Integer accountId, TransactionType type, TransactionStatus status,
            TransactionCategory category, String search, LocalDate from, LocalDate to,
            String sort, Integer rawPage, Integer rawSize
    ) {
        int page = rawPage == null || rawPage < 0 ? 0 : rawPage;
        int size = clampSize(rawSize);

        TransactionFilter filter = new TransactionFilter(
                userId, accountId, type, status, category, search,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(LocalTime.MAX),
                sort, size, page * size
        );

        List<Transaction> rows = transactionRepository.findFiltered(filter);
        long total = transactionRepository.countFiltered(filter);

        List<TransactionResponse> items = rows.stream().map(TransactionResponse::from).toList();
        return new PagedResponse<>(items, page, size, total);
    }

    /** Same filters as query(), no pagination — used for CSV export. */
    public List<Transaction> queryForExport(
            int userId, Integer accountId, TransactionType type, TransactionStatus status,
            TransactionCategory category, String search, LocalDate from, LocalDate to, String sort
    ) {
        TransactionFilter filter = new TransactionFilter(
                userId, accountId, type, status, category, search,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(LocalTime.MAX),
                sort, 10_000, 0
        );
        return transactionRepository.findFiltered(filter);
    }

    public TransactionResponse getForUser(int userId, int transactionId) {
        Transaction transaction = transactionRepository.findByIdForUser(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found."));
        return TransactionResponse.from(transaction);
    }

    private int clampSize(Integer rawSize) {
        if (rawSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.max(1, Math.min(MAX_PAGE_SIZE, rawSize));
    }
}
