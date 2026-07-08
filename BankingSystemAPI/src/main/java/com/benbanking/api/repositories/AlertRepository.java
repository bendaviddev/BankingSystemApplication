package com.benbanking.api.repositories;

import com.benbanking.api.enums.AlertSeverity;
import com.benbanking.api.enums.AlertType;
import com.benbanking.api.models.Alert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AlertRepository {

    private final JdbcTemplate jdbcTemplate;

    public AlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Alert save(Alert alert) {
        String sql = """
                INSERT INTO alerts (user_id, alert_type, severity, message, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"alert_id"});
            statement.setInt(1, alert.getUserId());
            statement.setString(2, alert.getAlertType().name());
            statement.setString(3, alert.getSeverity().name());
            statement.setString(4, alert.getMessage());
            statement.setBoolean(5, alert.isRead());
            statement.setTimestamp(6, Timestamp.valueOf(alert.getCreatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            return null;
        }

        return new Alert(key.intValue(), alert.getUserId(), alert.getAlertType(), alert.getSeverity(),
                alert.getMessage(), alert.isRead(), alert.getCreatedAt());
    }

    public List<Alert> findByUserId(int userId, int limit) {
        String sql = "SELECT * FROM alerts WHERE user_id = ? ORDER BY created_at DESC, alert_id DESC LIMIT ?";
        return jdbcTemplate.query(sql, (result, rowNum) -> mapResultSetToAlert(result), userId, limit);
    }

    public long countUnread(int userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE user_id = ? AND is_read = FALSE", Long.class, userId);
        return count == null ? 0 : count;
    }

    /** Returns rows affected — 0 means not found or not owned by this user. */
    public int markRead(int alertId, int userId) {
        return jdbcTemplate.update(
                "UPDATE alerts SET is_read = TRUE WHERE alert_id = ? AND user_id = ?", alertId, userId);
    }

    public void markAllRead(int userId) {
        jdbcTemplate.update("UPDATE alerts SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE", userId);
    }

    private Alert mapResultSetToAlert(java.sql.ResultSet result) throws java.sql.SQLException {
        return new Alert(
                result.getInt("alert_id"),
                result.getInt("user_id"),
                AlertType.valueOf(result.getString("alert_type")),
                AlertSeverity.valueOf(result.getString("severity")),
                result.getString("message"),
                result.getBoolean("is_read"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
