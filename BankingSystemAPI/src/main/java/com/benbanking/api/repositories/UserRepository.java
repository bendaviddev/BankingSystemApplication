package com.benbanking.api.repositories;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        String sql = """
                INSERT INTO users 
                (username, password, first_name, last_name, phone_number, email, address, role)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getRole().name()
        );

        return findByUsername(user.getUsername());
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        List<User> users = jdbcTemplate.query(
                sql,
                (result, rowNum) -> new User(
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("password"),
                        result.getString("first_name"),
                        result.getString("last_name"),
                        result.getString("phone_number"),
                        result.getString("email"),
                        result.getString("address"),
                        UserRole.valueOf(result.getString("role"))
                ),
                username
        );

        if (users.isEmpty()) {
            return null;
        }

        return users.get(0);
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users";

        return jdbcTemplate.query(
                sql,
                (result, rowNum) -> new User(
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("password"),
                        result.getString("first_name"),
                        result.getString("last_name"),
                        result.getString("phone_number"),
                        result.getString("email"),
                        result.getString("address"),
                        UserRole.valueOf(result.getString("role"))
                )
        );
    }

    public void updatePassword(int userId, String hashedPassword) {
        jdbcTemplate.update("UPDATE users SET password = ? WHERE id = ?", hashedPassword, userId);
    }
}
