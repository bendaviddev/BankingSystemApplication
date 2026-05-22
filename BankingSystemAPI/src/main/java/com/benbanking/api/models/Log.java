package com.benbanking.api.models;

import com.benbanking.api.enums.ActivityType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private int logId;
    private int userId;
    private ActivityType activityType;
    private String message;
    private LocalDateTime createdAt;

    public Log(int logId, int userId, ActivityType activityType, String message) {
        this.logId = logId;
        this.userId = userId;
        this.activityType = activityType;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Log(int logId, int userId, ActivityType activityType, String message, LocalDateTime createdAt) {
        this.logId = logId;
        this.userId = userId;
        this.activityType = activityType;
        this.message = message;
        this.createdAt = createdAt;
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

    public void printLog() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Log ID: " + logId);
        System.out.println("User ID: " + userId);
        System.out.println("Activity: " + activityType);
        System.out.println("Message: " + message);
        System.out.println("Date: " + createdAt.format(formatter));
    }
}
