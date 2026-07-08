package com.benbanking.api.dto;

import java.math.BigDecimal;

public class AdminStatsResponse {

    private final long totalUsers;
    private final long totalAccounts;
    private final long totalTransactions;
    private final BigDecimal totalVolume;
    private final long failedTransactions24h;

    public AdminStatsResponse(long totalUsers, long totalAccounts, long totalTransactions,
            BigDecimal totalVolume, long failedTransactions24h) {
        this.totalUsers = totalUsers;
        this.totalAccounts = totalAccounts;
        this.totalTransactions = totalTransactions;
        this.totalVolume = totalVolume;
        this.failedTransactions24h = failedTransactions24h;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public long getFailedTransactions24h() {
        return failedTransactions24h;
    }
}
