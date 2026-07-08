package com.benbanking.api.services;

import com.benbanking.api.dto.AlertListResponse;
import com.benbanking.api.dto.AlertResponse;
import com.benbanking.api.enums.AlertSeverity;
import com.benbanking.api.enums.AlertType;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Alert;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.repositories.AlertRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert rules. Every method here must be called AFTER the triggering money operation's
 * @Transactional method has already returned/committed (invariant #11) — an alert-insert
 * failure must never roll back a deposit/withdrawal/transfer. Callers are controllers or
 * non-transactional service wrappers, never the money-movement methods themselves.
 */
@Service
public class AlertService {

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000.00");

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Fires LOW_BALANCE / LARGE_TRANSACTION rules for a completed transaction on the given user's account. */
    public void fireForTransaction(int userId, Transaction transaction) {
        if (transaction == null) {
            return;
        }

        if (transaction.getRunningBalance() != null
                && transaction.getRunningBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            create(userId, AlertType.LOW_BALANCE, AlertSeverity.WARNING,
                    "Balance on account ending in "
                            + lastFour(transaction.getAccountId())
                            + " dropped below $100 (now $" + transaction.getRunningBalance() + ").");
        }

        if (transaction.getAmount() != null
                && transaction.getAmount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            create(userId, AlertType.LARGE_TRANSACTION, AlertSeverity.INFO,
                    "Large transaction of $" + transaction.getAmount() + " recorded.");
        }
    }

    public void fireFailedTransaction(int userId, String message) {
        create(userId, AlertType.TRANSACTION_FAILED, AlertSeverity.WARNING, message);
    }

    public void fireTransferReceived(int recipientUserId, BigDecimal amount, String fromDescription) {
        create(recipientUserId, AlertType.TRANSFER_RECEIVED, AlertSeverity.INFO,
                "You received $" + amount + " from " + fromDescription + ".");
    }

    public void firePasswordChanged(int userId) {
        create(userId, AlertType.SECURITY, AlertSeverity.INFO,
                "Your password was changed. If this wasn't you, contact support immediately.");
    }

    private static final int MAX_ALERTS = 50;

    public AlertListResponse getAlertsForUser(int userId) {
        List<AlertResponse> items = alertRepository.findByUserId(userId, MAX_ALERTS).stream()
                .map(AlertResponse::from)
                .toList();
        long unreadCount = alertRepository.countUnread(userId);
        return new AlertListResponse(items, unreadCount);
    }

    public void markRead(int alertId, int userId) {
        int rows = alertRepository.markRead(alertId, userId);
        if (rows == 0) {
            throw new NotFoundException("Alert not found.");
        }
    }

    public void markAllRead(int userId) {
        alertRepository.markAllRead(userId);
    }

    private void create(int userId, AlertType type, AlertSeverity severity, String message) {
        alertRepository.save(new Alert(0, userId, type, severity, message, false, LocalDateTime.now()));
    }

    private String lastFour(int accountId) {
        // Account id isn't the account number, but keeps the message concrete without another lookup.
        return "#" + accountId;
    }
}
