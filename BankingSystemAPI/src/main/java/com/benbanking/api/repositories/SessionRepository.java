package com.benbanking.api.repositories;

import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String tokenHash, int userId, LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime lastSeenAt) {
        jdbcTemplate.update(
                "INSERT INTO sessions (token_hash, user_id, created_at, expires_at, last_seen_at) VALUES (?, ?, ?, ?, ?)",
                tokenHash, userId, Timestamp.valueOf(createdAt), Timestamp.valueOf(expiresAt), Timestamp.valueOf(lastSeenAt)
        );
    }

    /** Joined with users so role/username changes (e.g. an admin freeze) apply on the very next request. */
    public Optional<Session> findByTokenHash(String tokenHash) {
        String sql = """
                SELECT s.session_id, s.token_hash, s.user_id, s.expires_at, s.last_seen_at, u.username, u.role
                FROM sessions s
                JOIN users u ON s.user_id = u.id
                WHERE s.token_hash = ?
                """;
        List<Session> rows = jdbcTemplate.query(sql, (result, rowNum) -> new Session(
                result.getInt("session_id"),
                result.getString("token_hash"),
                result.getInt("user_id"),
                result.getString("username"),
                UserRole.valueOf(result.getString("role")),
                result.getTimestamp("expires_at").toLocalDateTime(),
                result.getTimestamp("last_seen_at").toLocalDateTime()
        ), tokenHash);

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void touchLastSeen(String tokenHash, LocalDateTime now) {
        jdbcTemplate.update(
                "UPDATE sessions SET last_seen_at = ? WHERE token_hash = ?",
                Timestamp.valueOf(now), tokenHash
        );
    }

    public void deleteByTokenHash(String tokenHash) {
        jdbcTemplate.update("DELETE FROM sessions WHERE token_hash = ?", tokenHash);
    }

    /** Used on password change: revoke every other session for this user, keep the caller's own token alive. */
    public void deleteOtherSessions(int userId, String currentTokenHash) {
        jdbcTemplate.update(
                "DELETE FROM sessions WHERE user_id = ? AND token_hash <> ?",
                userId, currentTokenHash
        );
    }

    public void deleteExpired(LocalDateTime now) {
        jdbcTemplate.update("DELETE FROM sessions WHERE expires_at < ?", Timestamp.valueOf(now));
    }
}
