package com.benbanking.api.dto;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.User;

import java.time.LocalDateTime;

public class UserResponse {

    private final int id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String email;
    private final String address;
    private final UserRole role;
    private final LocalDateTime createdAt;

    public UserResponse(
            int id,
            String username,
            String firstName,
            String lastName,
            String phoneNumber,
            String email,
            String address,
            UserRole role,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
