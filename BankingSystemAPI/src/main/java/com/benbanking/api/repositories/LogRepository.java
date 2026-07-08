package com.benbanking.api.repositories;

import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.models.Log;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LogRepository {

    private final JdbcTemplate jdbcTemplate;

    public LogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Log save(Log log) {
        String sql = """
                INSERT INTO logs (user_id, activity_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"log_id"});

            statement.setInt(1, log.getUserId());
            statement.setString(2, log.getActivityType().name());
            statement.setString(3, log.getMessage());
            statement.setTimestamp(4, Timestamp.valueOf(log.getCreatedAt()));

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            return null;
        }

        int newId = key.intValue();

        return new Log(
                newId,
                log.getUserId(),
                log.getActivityType(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }

    public ArrayList<Log> findByUserId(int userId) {
        String sql = "SELECT * FROM logs WHERE user_id = ? ORDER BY created_at DESC";

        List<Log> logs = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToLog(result),
                userId
        );

        return new ArrayList<>(logs);
    }

    public ArrayList<Log> findAll() {
        String sql = "SELECT * FROM logs ORDER BY created_at DESC";

        List<Log> logs = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToLog(result)
        );

        return new ArrayList<>(logs);
    }

    public List<Log> findPaged(int limit, int offset) {
        String sql = "SELECT * FROM logs ORDER BY created_at DESC, log_id DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, (result, rowNum) -> mapResultSetToLog(result), limit, offset);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs", Long.class);
        return count == null ? 0 : count;
    }

    private Log mapResultSetToLog(java.sql.ResultSet result) throws java.sql.SQLException {
        return new Log(
                result.getInt("log_id"),
                result.getInt("user_id"),
                ActivityType.valueOf(result.getString("activity_type")),
                result.getString("message"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
