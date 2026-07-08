package com.benbanking.api.models;

import com.benbanking.api.enums.UserRole;

import java.time.LocalDateTime;

/** Row shape returned when a session is looked up joined against users, for fresh role/username. */
public class Session {

    private final int sessionId;
    private final String tokenHash;
    private final int userId;
    private final String username;
    private final UserRole role;
    private final LocalDateTime expiresAt;
    private final LocalDateTime lastSeenAt;

    public Session(int sessionId, String tokenHash, int userId, String username, UserRole role,
            LocalDateTime expiresAt, LocalDateTime lastSeenAt) {
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
        this.lastSeenAt = lastSeenAt;
    }

    public int getSessionId() {
        return sessionId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
