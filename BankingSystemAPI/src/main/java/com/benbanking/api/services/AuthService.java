package com.benbanking.api.services;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.User;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String username, String password, String firstName,
            String lastName, String phoneNumber, String email, String address) {

        if (!PasswordUtil.isValidPassword(password)) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include an uppercase letter, a number, and a special character.");
        }

        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username is already taken. Please choose a different one.");
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        User newUser = new User(
                0,
                username,
                hashedPassword,
                firstName,
                lastName,
                phoneNumber,
                email,
                address,
                UserRole.USER
        );

        return userRepository.save(newUser);
    }

    public User loginUser(String username, String password) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return null;
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            return null;
        }

        if (!PasswordUtil.isBcryptHash(user.getPassword())) {
            user.setPassword(PasswordUtil.hashPassword(password));
            userRepository.updatePassword(user.getId(), user.getPassword());
        }

        return user;
    }
}
