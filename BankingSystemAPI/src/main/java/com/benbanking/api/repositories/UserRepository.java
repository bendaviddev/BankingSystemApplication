package com.benbanking.api.repositories;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_MAPPER = (result, rowNum) -> new User(
            result.getInt("id"),
            result.getString("username"),
            result.getString("password"),
            result.getString("first_name"),
            result.getString("last_name"),
            result.getString("phone_number"),
            result.getString("email"),
            result.getString("address"),
            UserRole.valueOf(result.getString("role")),
            result.getTimestamp("created_at").toLocalDateTime(),
            result.getTimestamp("updated_at").toLocalDateTime()
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        String sql = """
                INSERT INTO users
                (username, password, first_name, last_name, phone_number, email, address, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getRole().name(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );

        return findByUsername(user.getUsername());
    }

    public User findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users WHERE username = ?", USER_MAPPER, username);
        return users.isEmpty() ? null : users.get(0);
    }

    public Optional<User> findById(int id) {
        List<User> users = jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_MAPPER, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY id", USER_MAPPER);
    }

    public List<User> search(String search) {
        if (search == null || search.isBlank()) {
            return findAll();
        }
        String like = "%" + search.toLowerCase() + "%";
        return jdbcTemplate.query(
                """
                SELECT * FROM users
                WHERE LOWER(username) LIKE ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(email) LIKE ?
                ORDER BY id
                """,
                USER_MAPPER, like, like, like, like
        );
    }

    public int countAll() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return count == null ? 0 : count;
    }

    public void updatePassword(int userId, String hashedPassword) {
        jdbcTemplate.update(
                "UPDATE users SET password = ?, updated_at = ? WHERE id = ?",
                hashedPassword, Timestamp.valueOf(LocalDateTime.now()), userId
        );
    }

    public void updateProfile(int userId, String firstName, String lastName, String phoneNumber, String email, String address) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET first_name = ?, last_name = ?, phone_number = ?, email = ?, address = ?, updated_at = ?
                WHERE id = ?
                """,
                firstName, lastName, phoneNumber, email, address, Timestamp.valueOf(LocalDateTime.now()), userId
        );
    }

    public ArrayList<User> findAllMutable() {
        return new ArrayList<>(findAll());
    }
}
