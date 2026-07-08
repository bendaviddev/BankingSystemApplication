package com.benbanking.api.services;

import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.enums.UserRole;
import com.benbanking.api.exceptions.BadRequestException;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    /**
     * Pre-computed BCrypt hash used only for a dummy comparison when the username doesn't
     * exist, so unknown-username and wrong-password paths take the same amount of time
     * (invariant #12 — avoids leaking which usernames are registered via response timing).
     */
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa2z2t2j2Xz2f0kQKQ2h2h2h2h2h2h2u";

    private final UserRepository userRepository;
    private final LogRepository logRepository;

    public AuthService(UserRepository userRepository, LogRepository logRepository) {
        this.userRepository = userRepository;
        this.logRepository = logRepository;
    }

    public User registerUser(String username, String password, String firstName,
            String lastName, String phoneNumber, String email, String address) {

        if (!PasswordUtil.isValidPassword(password)) {
            throw new BadRequestException(
                    "Password must be at least 8 characters and include an uppercase letter, a number, and a special character.");
        }

        if (userRepository.findByUsername(username) != null) {
            throw new BadRequestException("Username is already taken. Please choose a different one.");
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        User newUser = new User(
                0, username, hashedPassword, firstName, lastName, phoneNumber, email, address,
                UserRole.USER, null, null
        );

        return userRepository.save(newUser);
    }

    /** Returns null on any authentication failure — callers turn that into a generic 401. */
    public User loginUser(String username, String password) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            // Timing hardening: run a BCrypt comparison even though there's no user to check
            // against, so "unknown username" and "wrong password" take about the same time.
            PasswordUtil.checkPassword(password, DUMMY_HASH);
            return null;
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            logRepository.save(new Log(0, user.getId(), ActivityType.LOGIN_FAILED, "Incorrect password.", LocalDateTime.now()));
            return null;
        }

        if (!PasswordUtil.isBcryptHash(user.getPassword())) {
            user.setPassword(PasswordUtil.hashPassword(password));
            userRepository.updatePassword(user.getId(), user.getPassword());
        }

        logRepository.save(new Log(0, user.getId(), ActivityType.LOGIN, "User logged in.", LocalDateTime.now()));
        return user;
    }

    public User requireUser(int userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
    }

    public void updatePassword(int userId, String hashedPassword) {
        userRepository.updatePassword(userId, hashedPassword);
    }
}
