package com.benbanking.api.services;

import com.benbanking.api.dto.AnalyticsSummaryResponse;
import com.benbanking.api.dto.AnalyticsSummaryResponse.BalancePoint;
import com.benbanking.api.dto.AnalyticsSummaryResponse.CategoryTotal;
import com.benbanking.api.dto.AnalyticsSummaryResponse.MonthlyFlow;
import com.benbanking.api.dto.TransactionResponse;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All bucketing here is done against the JVM's wall-clock "now" (invariant #14 calls for
 * UTC bucketing; timestamps are stored as naive LocalDateTime with no zone info, so — as a
 * documented simplification for this demo — "now" and stored timestamps are compared using
 * the same clock rather than converting through java.time.ZoneOffset.UTC).
 */
@Service
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int BALANCE_HISTORY_DAYS = 30;
    private static final int MONTHLY_FLOW_MONTHS = 6;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AnalyticsService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public AnalyticsSummaryResponse getSummary(int userId) {
        List<UserBankAccount> accounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalance = accounts.stream()
                .map(UserBankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixMonthsAgo = now.minusMonths(MONTHLY_FLOW_MONTHS).withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime thirtyDaysAgo = now.minusDays(BALANCE_HISTORY_DAYS);

        List<Transaction> history = transactionRepository.findCompletedByUserSince(userId, sixMonthsAgo);

        BigDecimal totalIn30d = BigDecimal.ZERO;
        BigDecimal totalOut30d = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<String, BigDecimal[]> monthly = initMonthlyBuckets(now);

        for (Transaction t : history) {
            boolean credit = isCredit(t.getTransactionType());
            boolean debit = !credit;

            String monthKey = t.getCreatedAt().format(MONTH_KEY);
            BigDecimal[] bucket = monthly.get(monthKey);
            if (bucket != null) {
                if (credit) {
                    bucket[0] = bucket[0].add(t.getAmount());
                } else {
                    bucket[1] = bucket[1].add(t.getAmount());
                }
            }

            if (!t.getCreatedAt().isBefore(thirtyDaysAgo)) {
                if (credit) {
                    totalIn30d = totalIn30d.add(t.getAmount());
                } else {
                    totalOut30d = totalOut30d.add(t.getAmount());
                    categoryTotals.merge(t.getCategory().name(), t.getAmount(), BigDecimal::add);
                }
            }
        }

        List<CategoryTotal> spendingByCategory = categoryTotals.entrySet().stream()
                .map(e -> new CategoryTotal(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                .toList();

        List<MonthlyFlow> monthlyFlow = monthly.entrySet().stream()
                .map(e -> new MonthlyFlow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        List<TransactionResponse> largestTransactions = transactionRepository.findTopCompletedByUser(userId, 5)
                .stream().map(TransactionResponse::from).toList();

        List<BalancePoint> balanceHistory = computeBalanceHistory(accounts, history, now.toLocalDate());

        return new AnalyticsSummaryResponse(
                totalBalance, totalIn30d, totalOut30d, spendingByCategory, monthlyFlow, largestTransactions, balanceHistory
        );
    }

    private boolean isCredit(TransactionType type) {
        return type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN;
    }

    private Map<String, BigDecimal[]> initMonthlyBuckets(LocalDateTime now) {
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        YearMonth start = YearMonth.from(now).minusMonths(MONTHLY_FLOW_MONTHS - 1L);
        for (int i = 0; i < MONTHLY_FLOW_MONTHS; i++) {
            buckets.put(start.plusMonths(i).format(MONTH_KEY), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        return buckets;
    }

    /**
     * Per calendar day (last 30), sum across accounts of each account's balance as of the end
     * of that day, carrying forward on quiet days. Reconstructed by walking each account's
     * COMPLETED transactions backward from its current balance (no synthetic "opening balance"
     * transaction is recorded, so this is the only way to derive historical EOD balances).
     */
    private List<BalancePoint> computeBalanceHistory(List<UserBankAccount> accounts, List<Transaction> history, LocalDate today) {
        Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = BALANCE_HISTORY_DAYS - 1; i >= 0; i--) {
            totals.put(today.minusDays(i), BigDecimal.ZERO);
        }

        for (UserBankAccount account : accounts) {
            List<Transaction> forAccount = history.stream()
                    .filter(t -> t.getAccountId() == account.getAccountId())
                    .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                    .toList();

            BigDecimal pointer = account.getBalance();
            int idx = 0;

            for (int dayOffset = 0; dayOffset < BALANCE_HISTORY_DAYS; dayOffset++) {
                LocalDate day = today.minusDays(dayOffset);
                LocalDateTime dayEnd = day.atTime(LocalTime.MAX);

                while (idx < forAccount.size() && forAccount.get(idx).getCreatedAt().isAfter(dayEnd)) {
                    Transaction t = forAccount.get(idx);
                    if (isCredit(t.getTransactionType())) {
                        pointer = pointer.subtract(t.getAmount());
                    } else {
                        pointer = pointer.add(t.getAmount());
                    }
                    idx++;
                }

                totals.merge(day, pointer, BigDecimal::add);
            }
        }

        List<BalancePoint> points = new ArrayList<>();
        totals.forEach((date, balance) -> points.add(new BalancePoint(date, balance)));
        points.sort(Comparator.comparing(BalancePoint::date));
        return points;
    }
}
