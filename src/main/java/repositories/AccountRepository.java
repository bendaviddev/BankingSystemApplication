package main.java.repositories;

import main.java.database.DatabaseConnection;
import main.java.enums.BankAccountStatus;
import main.java.enums.BankAccountType;
import main.java.enums.Currency;
import main.java.models.UserBankAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class AccountRepository {

    public UserBankAccount save(UserBankAccount account) {
        String sql = """
                INSERT INTO accounts (user_id, account_number, account_type, currency, balance, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, account.getUserId());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getAccountType().name());
            statement.setString(4, account.getCurrency().name());
            statement.setDouble(5, account.getBalance());
            statement.setString(6, account.getStatus().name());

            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();

            if (keys.next()) {
                int newId = keys.getInt(1);

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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<UserBankAccount> findByUserId(int userId) {
        ArrayList<UserBankAccount> accounts = new ArrayList<>();

        String sql = "SELECT * FROM accounts WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                accounts.add(mapResultSetToAccount(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    public UserBankAccount findById(int accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return mapResultSetToAccount(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public UserBankAccount findByIdAndUserId(int accountId, int userId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ? AND user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountId);
            statement.setInt(2, userId);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return mapResultSetToAccount(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<UserBankAccount> findAll() {
        ArrayList<UserBankAccount> accounts = new ArrayList<>();

        String sql = "SELECT * FROM accounts";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                accounts.add(mapResultSetToAccount(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    public void updateBalanceAndStatus(UserBankAccount account) {
        String sql = """
                UPDATE accounts
                SET balance = ?, status = ?
                WHERE account_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, account.getBalance());
            statement.setString(2, account.getStatus().name());
            statement.setInt(3, account.getAccountId());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private UserBankAccount mapResultSetToAccount(ResultSet result) throws SQLException {
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
