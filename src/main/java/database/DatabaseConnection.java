// package main.java.database;

// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

// public class DatabaseConnection {

//     private static final String URL = System.getenv("DB_URL");
//     private static final String USERNAME = System.getenv("DB_USERNAME");
//     private static final String PASSWORD = System.getenv("DB_PASSWORD");

//     public static Connection getConnection() throws SQLException {
//         return DriverManager.getConnection(URL, USERNAME, PASSWORD);
//     }
// }


package main.java.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = System.getenv("DB_URL");
    private static final String USERNAME = System.getenv("DB_USERNAME");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        if (URL == null || USERNAME == null || PASSWORD == null) {
            throw new IllegalStateException(
                "Database environment variables are missing. " +
                "Make sure DB_URL, DB_USERNAME, and DB_PASSWORD are set."
            );
        }

        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
