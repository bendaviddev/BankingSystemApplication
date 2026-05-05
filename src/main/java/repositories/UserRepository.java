package main.java.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import main.java.database.DatabaseConnection;
import main.java.enums.UserRole;
import main.java.models.User;

public class UserRepository {

    public User save(User user) {
        String sql = """
        INSERT INTO users (username, password, first_name, last_name, phone_number, email, address, role)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            statement.setString(5, user.getPhoneNumber());
            statement.setString(6, user.getEmail());
            statement.setString(7, user.getAddress());
            statement.setString(8, user.getRole().name());

            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();

            if (keys.next()) {
                int newId = keys.getInt(1);

                return new User(
                        newId,
                        user.getUsername(),
                        user.getPassword(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getPhoneNumber(),
                        user.getEmail(),
                        user.getAddress(),
                        user.getRole()
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return mapResultSetToUser(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return mapResultSetToUser(result);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<User> findAll() {
        ArrayList<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet result = statement.executeQuery();

            while (result.next()) {
                users.add(mapResultSetToUser(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public void update(User user) {
        String sql = """
        UPDATE users
        SET first_name = ?, last_name = ?, phone_number = ?, email = ?, address = ?, password = ?, role = ?
        WHERE id = ?
        """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, user.getPhoneNumber());
            statement.setString(4, user.getEmail());

            statement.setString(5, user.getAddress());
            statement.setString(6, user.getPassword());
            statement.setString(7, user.getRole().name());
            statement.setInt(8, user.getId());

            statement.executeUpdate();

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }

    private User mapResultSetToUser(ResultSet result) throws SQLException {
        return new User(
                result.getInt("id"),
                result.getString("username"),
                result.getString("password"),
                result.getString("first_name"),
                result.getString("last_name"),
                result.getString("phone_number"),
                result.getString("email"),
                result.getString("address"),
                UserRole.valueOf(result.getString("role"))
        );
    }
}
