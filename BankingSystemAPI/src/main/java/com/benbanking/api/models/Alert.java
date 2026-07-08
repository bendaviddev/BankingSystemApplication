package com.benbanking.api.models;

import com.benbanking.api.enums.AlertSeverity;
import com.benbanking.api.enums.AlertType;

import java.time.LocalDateTime;

public class Alert {

    private int alertId;
    private int userId;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Alert(int alertId, int userId, AlertType alertType, AlertSeverity severity,
            String message, boolean read, LocalDateTime createdAt) {
        this.alertId = alertId;
        this.userId = userId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getAlertId() {
        return alertId;
    }

    public int getUserId() {
        return userId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
