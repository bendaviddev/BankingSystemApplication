package com.benbanking.api.auth;

import com.benbanking.api.enums.UserRole;

public class AuthSession {

    private final int userId;
    private final String username;
    private final UserRole role;

    public AuthSession(int userId, String username, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
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
}
