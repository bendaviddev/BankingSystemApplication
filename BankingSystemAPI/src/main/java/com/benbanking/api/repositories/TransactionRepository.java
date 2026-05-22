package com.benbanking.api.repositories;

import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions (account_id, transaction_type, amount, description, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            statement.setInt(1, transaction.getAccountId());
            statement.setString(2, transaction.getTransactionType().name());
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setTimestamp(5, Timestamp.valueOf(transaction.getCreatedAt()));

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            return null;
        }

        int newId = key.intValue();

        return new Transaction(
                newId,
                transaction.getAccountId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    public ArrayList<Transaction> findByAccountId(int accountId) {
        String sql = """
                SELECT *
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC
                """;

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result),
                accountId
        );

        return new ArrayList<>(transactions);
    }

    public ArrayList<Transaction> findByUserId(int userId) {
        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ?
                ORDER BY t.created_at DESC
                """;

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result),
                userId
        );

        return new ArrayList<>(transactions);
    }

    public ArrayList<Transaction> findByUserIdAndType(int userId, TransactionType transactionType) {
        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.transaction_type = ?
                ORDER BY t.created_at DESC
                """;

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result),
                userId,
                transactionType.name()
        );

        return new ArrayList<>(transactions);
    }

    public ArrayList<Transaction> findByUserIdAndAccountId(int userId, int accountId) {
        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.account_id = ?
                ORDER BY t.created_at DESC
                """;

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result),
                userId,
                accountId
        );

        return new ArrayList<>(transactions);
    }

    public Transaction findLatestByAccountId(int accountId) {
        String sql = """
                SELECT *
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC, transaction_id DESC
                LIMIT 1
                """;

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result),
                accountId
        );

        if (transactions.isEmpty()) {
            return null;
        }

        return transactions.get(0);
    }

    public ArrayList<Transaction> findAll() {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";

        List<Transaction> transactions = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToTransaction(result)
        );

        return new ArrayList<>(transactions);
    }

    private Transaction mapResultSetToTransaction(java.sql.ResultSet result) throws java.sql.SQLException {
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