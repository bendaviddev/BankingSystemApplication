package main.java.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import main.java.database.DatabaseConnection;
import main.java.enums.TransactionType;
import main.java.models.Transaction;

public class TransactionRepository {

    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions (account_id, transaction_type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, transaction.getAccountId());
            statement.setString(2, transaction.getTransactionType().name());
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setTimestamp(5, Timestamp.valueOf(transaction.getCreatedAt()));

            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();

            if (keys.next()) {
                int newId = keys.getInt(1);

                return new Transaction(
                        newId,
                        transaction.getAccountId(),
                        transaction.getTransactionType(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getCreatedAt()
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<Transaction> findByUserId(int userId) {
        ArrayList<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ?
                ORDER BY t.created_at DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                transactions.add(mapResultSetToTransaction(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public ArrayList<Transaction> findByUserIdAndType(int userId, TransactionType transactionType) {
        ArrayList<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.transaction_type = ?
                ORDER BY t.created_at DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, transactionType.name());

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                transactions.add(mapResultSetToTransaction(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public ArrayList<Transaction> findByUserIdAndAccountId(int userId, int accountId) {
        ArrayList<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.account_id = ?
                ORDER BY t.created_at DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, accountId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                transactions.add(mapResultSetToTransaction(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public Transaction findLatestByAccountId(int accountId) {
        String sql = """
                SELECT *
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC, transaction_id DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return mapResultSetToTransaction(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<Transaction> findAll() {
        ArrayList<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                transactions.add(mapResultSetToTransaction(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    private Transaction mapResultSetToTransaction(ResultSet result) throws SQLException {
        return new Transaction(
                result.getInt("transaction_id"),
                result.getInt("account_id"),
                TransactionType.valueOf(result.getString("transaction_type")),
                result.getDouble("amount"),
                result.getString("description"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}