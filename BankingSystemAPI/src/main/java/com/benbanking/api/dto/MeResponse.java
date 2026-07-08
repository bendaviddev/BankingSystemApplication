package com.benbanking.api.dto;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.User;

import java.time.LocalDateTime;

public class MeResponse {

    private final int userId;
    private final String username;
    private final UserRole role;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final String address;
    private final LocalDateTime createdAt;

    public MeResponse(int userId, String username, UserRole role, String firstName, String lastName,
            String email, String phoneNumber, String address, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.createdAt = createdAt;
    }

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getCreatedAt()
        );
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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
