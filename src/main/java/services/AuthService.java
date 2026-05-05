package main.java.services;

import main.java.enums.UserRole;
import main.java.models.User;
import main.java.repositories.UserRepository;
import main.java.util.PasswordUtil;

public class AuthService {

    private UserRepository userRepository = new UserRepository();

    public User registerUser(String username, String password, String firstName,
            String lastName, String phoneNumber, String email, String address) {

        if (!PasswordUtil.isValidPassword(password)) {
            return null;
        }

        User existingUser = userRepository.findByUsername(username);

        if (existingUser != null) {
            return null;
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

        boolean correctPassword = PasswordUtil.verifyPassword(password, user.getPassword());

        if (!correctPassword) {
            return null;
        }

        return user;
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void updateUserProfile(User user, String firstName, String lastName,
            String phoneNumber, String email, String address) {

        user.updateProfile(firstName, lastName, phoneNumber, email, address);
        userRepository.update(user);
    }
}
