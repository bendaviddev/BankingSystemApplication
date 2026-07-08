package com.benbanking.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AnalyticsSummaryResponse {

    private final BigDecimal totalBalance;
    private final BigDecimal totalIn30d;
    private final BigDecimal totalOut30d;
    private final List<CategoryTotal> spendingByCategory;
    private final List<MonthlyFlow> monthlyFlow;
    private final List<TransactionResponse> largestTransactions;
    private final List<BalancePoint> balanceHistory;

    public AnalyticsSummaryResponse(BigDecimal totalBalance, BigDecimal totalIn30d, BigDecimal totalOut30d,
            List<CategoryTotal> spendingByCategory, List<MonthlyFlow> monthlyFlow,
            List<TransactionResponse> largestTransactions, List<BalancePoint> balanceHistory) {
        this.totalBalance = totalBalance;
        this.totalIn30d = totalIn30d;
        this.totalOut30d = totalOut30d;
        this.spendingByCategory = spendingByCategory;
        this.monthlyFlow = monthlyFlow;
        this.largestTransactions = largestTransactions;
        this.balanceHistory = balanceHistory;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public BigDecimal getTotalIn30d() {
        return totalIn30d;
    }

    public BigDecimal getTotalOut30d() {
        return totalOut30d;
    }

    public List<CategoryTotal> getSpendingByCategory() {
        return spendingByCategory;
    }

    public List<MonthlyFlow> getMonthlyFlow() {
        return monthlyFlow;
    }

    public List<TransactionResponse> getLargestTransactions() {
        return largestTransactions;
    }

    public List<BalancePoint> getBalanceHistory() {
        return balanceHistory;
    }

    public record CategoryTotal(String category, BigDecimal total) {
    }

    public record MonthlyFlow(String month, BigDecimal inflow, BigDecimal outflow) {
    }

    public record BalancePoint(LocalDate date, BigDecimal balance) {
    }
}
