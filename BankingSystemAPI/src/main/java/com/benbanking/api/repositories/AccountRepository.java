package com.benbanking.api.repositories;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.models.UserBankAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserBankAccount save(UserBankAccount account) {
        String sql = """
                INSERT INTO accounts (user_id, account_number, account_type, currency, balance, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            statement.setInt(1, account.getUserId());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getAccountType().name());
            statement.setString(4, account.getCurrency().name());
            statement.setDouble(5, account.getBalance());
            statement.setString(6, account.getStatus().name());

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
                account.getStatus()
        );
    }

    public ArrayList<UserBankAccount> findByUserId(int userId) {
        String sql = "SELECT * FROM accounts WHERE user_id = ?";

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

    public ArrayList<UserBankAccount> findAll() {
        String sql = "SELECT * FROM accounts";

        List<UserBankAccount> accounts = jdbcTemplate.query(
                sql,
                (result, rowNum) -> mapResultSetToAccount(result)
        );

        return new ArrayList<>(accounts);
    }

    public void updateBalanceAndStatus(UserBankAccount account) {
        String sql = """
                UPDATE accounts
                SET balance = ?, status = ?
                WHERE account_id = ?
                """;

        jdbcTemplate.update(
                sql,
                account.getBalance(),
                account.getStatus().name(),
                account.getAccountId()
        );
    }

    private UserBankAccount mapResultSetToAccount(java.sql.ResultSet result) throws java.sql.SQLException {
        return new UserBankAccount(
                result.getInt("account_id"),
                result.getInt("user_id"),
                result.getString("account_number"),
                BankAccountType.valueOf(result.getString("account_type")),
                Currency.valueOf(result.getString("currency")),
                result.getDouble("balance"),
                BankAccountStatus.valueOf(result.getString("status"))
        );
    }
}