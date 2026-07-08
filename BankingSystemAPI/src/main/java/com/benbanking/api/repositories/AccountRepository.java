package com.benbanking.api.repositories;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.models.UserBankAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserBankAccount save(UserBankAccount account) {
        String sql = """
                INSERT INTO accounts
                    (user_id, account_number, account_type, currency, balance, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"account_id"});

            statement.setInt(1, account.getUserId());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getAccountType().name());
            statement.setString(4, account.getCurrency().name());
            statement.setBigDecimal(5, account.getBalance());
            statement.setString(6, account.getStatus().name());
            statement.setTimestamp(7, Timestamp.valueOf(account.getCreatedAt()));
            statement.setTimestamp(8, Timestamp.valueOf(account.getUpdatedAt()));

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            return null;
        }

        int newId = key.intValue();

        return new UserBankAccount(
                newId,
                account.getUserId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public ArrayList<UserBankAccount> findByUserId(int userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = ? ORDER BY account_id";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result),
                userId
        );

        return new ArrayList<>(accounts);
    }

    public UserBankAccount findById(int accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result),
                accountId
        );

        if (accounts.isEmpty()) {
            return null;
        }

        return accounts.get(0);
    }

    public UserBankAccount findByIdAndUserId(int accountId, int userId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ? AND user_id = ?";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result),
                accountId,
                userId
        );

        if (accounts.isEmpty()) {
            return null;
        }

        return accounts.get(0);
    }

    public Optional<UserBankAccount> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result),
                accountNumber
        );

        return accounts.isEmpty() ? Optional.empty() : Optional.of(accounts.get(0));
    }

    public ArrayList<UserBankAccount> findAll() {
        String sql = "SELECT * FROM accounts ORDER BY account_id";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result)
        );

        return new ArrayList<>(accounts);
    }

    public int countByUserId(int userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public int countAll() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Admin search: accounts joined with owner username. Case-insensitive match on
     * account number or owner username; empty/null search returns everything.
     */
    public List<AccountWithOwner> searchWithOwner(String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.*, u.username AS owner_username
                FROM accounts a
                JOIN users u ON a.user_id = u.id
                """);
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" WHERE LOWER(a.account_number) LIKE ? OR LOWER(u.username) LIKE ?");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY a.account_id");

        return jdbcTemplate.query(sql.toString(), (result, rowNum) -> new AccountWithOwner(
                mapResultSetToAccount(result),
                result.getString("owner_username")
        ), params.toArray());
    }

    /**
     * Atomic conditional debit: only succeeds if the account is ACTIVE and has sufficient
     * balance. Returns the number of rows affected (0 or 1) — callers must check this rather
     * than reading balance first (no read-modify-write races).
     */
    public int debitIfSufficient(int accountId, BigDecimal amount, LocalDateTime updatedAt) {
        String sql = """
                UPDATE accounts
                SET balance = balance - ?, updated_at = ?
                WHERE account_id = ? AND status = 'ACTIVE' AND balance >= ?
                """;
        return jdbcTemplate.update(sql, amount, Timestamp.valueOf(updatedAt), accountId, amount);
    }

    /** Atomic conditional credit: only succeeds if the account is ACTIVE. */
    public int creditIfActive(int accountId, BigDecimal amount, LocalDateTime updatedAt) {
        String sql = """
                UPDATE accounts
                SET balance = balance + ?, updated_at = ?
                WHERE account_id = ? AND status = 'ACTIVE'
                """;
        return jdbcTemplate.update(sql, amount, Timestamp.valueOf(updatedAt), accountId);
    }

    /** Re-read balance within the same transaction, immediately after a conditional update. */
    public BigDecimal getBalance(int accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
    }

    public void updateStatus(int accountId, BankAccountStatus status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE accounts SET status = ?, updated_at = ? WHERE account_id = ?",
                status.name(), Timestamp.valueOf(updatedAt), accountId
        );
    }

    /** Absolute balance set — seeding/administrative use only; money operations must use the atomic methods above. */
    public void setBalance(int accountId, BigDecimal balance, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE accounts SET balance = ?, updated_at = ? WHERE account_id = ?",
                balance, Timestamp.valueOf(updatedAt), accountId
        );
    }

    private UserBankAccount mapResultSetToAccount(ResultSet result) throws SQLException {
        return new UserBankAccount(
                result.getInt("account_id"),
                result.getInt("user_id"),
                result.getString("account_number"),
                BankAccountType.valueOf(result.getString("account_type")),
                Currency.valueOf(result.getString("currency")),
                result.getBigDecimal("balance"),
                BankAccountStatus.valueOf(result.getString("status")),
                result.getTimestamp("created_at").toLocalDateTime(),
                result.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record AccountWithOwner(UserBankAccount account, String ownerUsername) {
    }
}
