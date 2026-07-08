package com.benbanking.api.repositories;

import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.models.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Transaction save(Transaction transaction) {
        String sql = """
                INSERT INTO transactions
                    (reference, account_id, counterparty_account_id, transaction_type, status,
                     amount, running_balance, category, description, memo, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"transaction_id"});

            statement.setString(1, transaction.getReference());
            statement.setInt(2, transaction.getAccountId());
            if (transaction.getCounterpartyAccountId() != null) {
                statement.setInt(3, transaction.getCounterpartyAccountId());
            } else {
                statement.setNull(3, Types.INTEGER);
            }
            statement.setString(4, transaction.getTransactionType().name());
            statement.setString(5, transaction.getStatus().name());
            statement.setBigDecimal(6, transaction.getAmount());
            if (transaction.getRunningBalance() != null) {
                statement.setBigDecimal(7, transaction.getRunningBalance());
            } else {
                statement.setNull(7, Types.DECIMAL);
            }
            statement.setString(8, transaction.getCategory().name());
            statement.setString(9, transaction.getDescription());
            statement.setString(10, transaction.getMemo());
            statement.setTimestamp(11, Timestamp.valueOf(transaction.getCreatedAt()));

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            return null;
        }
        int newId = key.intValue();

        return new Transaction(
                newId,
                transaction.getReference(),
                transaction.getAccountId(),
                transaction.getCounterpartyAccountId(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getRunningBalance(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getMemo(),
                transaction.getCreatedAt()
        );
    }

    public Optional<Transaction> findById(int transactionId) {
        List<Transaction> rows = jdbcTemplate.query(
                "SELECT * FROM transactions WHERE transaction_id = ?",
                (result, rowNum) -> mapResultSetToTransaction(result),
                transactionId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Transaction> findByIdForUser(int transactionId, int userId) {
        String sql = """
                SELECT t.* FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE t.transaction_id = ? AND a.user_id = ?
                """;
        List<Transaction> rows = jdbcTemplate.query(
                sql, (result, rowNum) -> mapResultSetToTransaction(result), transactionId, userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Ownership-scoped, filtered, sorted, paginated transaction history for GET /api/transactions. */
    public List<Transaction> findFiltered(TransactionFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.account_id ");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, filter, params);
        sql.append(" ORDER BY ").append(orderBy(filter.sort()));
        if (filter.limit() != null) {
            sql.append(" LIMIT ").append(filter.limit()).append(" OFFSET ").append(filter.offset());
        }

        return jdbcTemplate.query(sql.toString(), (result, rowNum) -> mapResultSetToTransaction(result), params.toArray());
    }

    public long countFiltered(TransactionFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transactions t JOIN accounts a ON t.account_id = a.account_id ");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, filter, params);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    private void appendWhere(StringBuilder sql, TransactionFilter filter, List<Object> params) {
        List<String> clauses = new ArrayList<>();

        if (filter.userId() != null) {
            clauses.add("a.user_id = ?");
            params.add(filter.userId());
        }
        if (filter.accountId() != null) {
            clauses.add("t.account_id = ?");
            params.add(filter.accountId());
        }
        if (filter.type() != null) {
            clauses.add("t.transaction_type = ?");
            params.add(filter.type().name());
        }
        if (filter.status() != null) {
            clauses.add("t.status = ?");
            params.add(filter.status().name());
        }
        if (filter.category() != null) {
            clauses.add("t.category = ?");
            params.add(filter.category().name());
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            clauses.add("(LOWER(t.description) LIKE ? OR LOWER(t.memo) LIKE ? OR LOWER(t.reference) LIKE ?)");
            String like = "%" + filter.search().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.from() != null) {
            clauses.add("t.created_at >= ?");
            params.add(Timestamp.valueOf(filter.from()));
        }
        if (filter.to() != null) {
            clauses.add("t.created_at <= ?");
            params.add(Timestamp.valueOf(filter.to()));
        }

        if (!clauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", clauses));
        }
    }

    private String orderBy(String sort) {
        return switch (sort == null ? "date_desc" : sort) {
            case "date_asc" -> "t.created_at ASC, t.transaction_id ASC";
            case "amount_desc" -> "t.amount DESC, t.transaction_id DESC";
            case "amount_asc" -> "t.amount ASC, t.transaction_id ASC";
            default -> "t.created_at DESC, t.transaction_id DESC";
        };
    }

    /** All COMPLETED transactions for a user since a given instant, oldest first — feeds analytics aggregation in Java. */
    public List<Transaction> findCompletedByUserSince(int userId, LocalDateTime since) {
        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.status = 'COMPLETED' AND t.created_at >= ?
                ORDER BY t.created_at ASC, t.transaction_id ASC
                """;
        return jdbcTemplate.query(sql, (result, rowNum) -> mapResultSetToTransaction(result),
                userId, Timestamp.valueOf(since));
    }

    public List<Transaction> findTopCompletedByUser(int userId, int limit) {
        String sql = """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.account_id
                WHERE a.user_id = ? AND t.status = 'COMPLETED'
                ORDER BY t.amount DESC, t.transaction_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (result, rowNum) -> mapResultSetToTransaction(result), userId, limit);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long.class);
        return count == null ? 0 : count;
    }

    public BigDecimal sumCompletedAmount() {
        BigDecimal sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE status = 'COMPLETED'", BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public long countFailedSince(LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE status = 'FAILED' AND created_at >= ?",
                Long.class, Timestamp.valueOf(since));
        return count == null ? 0 : count;
    }

    /** Admin transaction listing: all users, optional status filter, paginated. */
    public List<Transaction> findAllForAdmin(TransactionStatus status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" WHERE status = ?");
            params.add(status.name());
        }
        sql.append(" ORDER BY created_at DESC, transaction_id DESC LIMIT ").append(limit).append(" OFFSET ").append(offset);
        return jdbcTemplate.query(sql.toString(), (result, rowNum) -> mapResultSetToTransaction(result), params.toArray());
    }

    public long countAllForAdmin(TransactionStatus status) {
        if (status == null) {
            return countAll();
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE status = ?", Long.class, status.name());
        return count == null ? 0 : count;
    }

    private Transaction mapResultSetToTransaction(ResultSet result) throws SQLException {
        int counterparty = result.getInt("counterparty_account_id");
        Integer counterpartyAccountId = result.wasNull() ? null : counterparty;

        return new Transaction(
                result.getInt("transaction_id"),
                result.getString("reference"),
                result.getInt("account_id"),
                counterpartyAccountId,
                TransactionType.valueOf(result.getString("transaction_type")),
                TransactionStatus.valueOf(result.getString("status")),
                result.getBigDecimal("amount"),
                result.getBigDecimal("running_balance"),
                TransactionCategory.valueOf(result.getString("category")),
                result.getString("description"),
                result.getString("memo"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }

    public record TransactionFilter(
            Integer userId,
            Integer accountId,
            TransactionType type,
            TransactionStatus status,
            TransactionCategory category,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            String sort,
            Integer limit,
            Integer offset
    ) {
    }
}
