package com.benbanking.api.dto;

import com.benbanking.api.enums.AlertSeverity;
import com.benbanking.api.enums.AlertType;
import com.benbanking.api.models.Alert;

import java.time.LocalDateTime;

public class AlertResponse {

    private final int alertId;
    private final AlertType alertType;
    private final AlertSeverity severity;
    private final String message;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public AlertResponse(int alertId, AlertType alertType, AlertSeverity severity, String message,
            boolean isRead, LocalDateTime createdAt) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getAlertId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.isRead(),
                alert.getCreatedAt()
        );
    }

    public int getAlertId() {
        return alertId;
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
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
