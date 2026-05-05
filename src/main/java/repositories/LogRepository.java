package main.java.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import main.java.database.DatabaseConnection;
import main.java.enums.ActivityType;
import main.java.models.Log;

public class LogRepository {

    public Log save(Log log) {
        String sql = """
                INSERT INTO logs (user_id, activity_type, message, created_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, log.getUserId());
            statement.setString(2, log.getActivityType().name());
            statement.setString(3, log.getMessage());
            statement.setTimestamp(4, Timestamp.valueOf(log.getCreatedAt()));

            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();

            if (keys.next()) {
                int newId = keys.getInt(1);

                return new Log(
                        newId,
                        log.getUserId(),
                        log.getActivityType(),
                        log.getMessage(),
                        log.getCreatedAt()
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<Log> findByUserId(int userId) {
        ArrayList<Log> logs = new ArrayList<>();

        String sql = "SELECT * FROM logs WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                logs.add(mapResultSetToLog(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    public ArrayList<Log> findAll() {
        ArrayList<Log> logs = new ArrayList<>();

        String sql = "SELECT * FROM logs ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                logs.add(mapResultSetToLog(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    private Log mapResultSetToLog(ResultSet result) throws SQLException {
        return new Log(
                result.getInt("log_id"),
                result.getInt("user_id"),
                ActivityType.valueOf(result.getString("activity_type")),
                result.getString("message"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
