package com.benbanking.api.dto;

import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.models.Log;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private final int logId;
    private final int userId;
    private final ActivityType activityType;
    private final String message;
    private final LocalDateTime createdAt;

    public AuditLogResponse(int logId, int userId, ActivityType activityType, String message, LocalDateTime createdAt) {
        this.logId = logId;
        this.userId = userId;
        this.activityType = activityType;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static AuditLogResponse from(Log log) {
        return new AuditLogResponse(log.getLogId(), log.getUserId(), log.getActivityType(), log.getMessage(), log.getCreatedAt());
    }

    public int getLogId() {
        return logId;
    }

    public int getUserId() {
        return userId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
