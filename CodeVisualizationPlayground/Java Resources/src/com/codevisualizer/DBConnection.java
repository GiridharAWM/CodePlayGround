package com.codevisualizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager using JDBC and MySQL.
 * Strictly adheres to PreparedStatement guidelines and safe connection handling.
 * Supports both local development defaults and cloud deployment environment variables.
 */
public class DBConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private static String getUrl() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            return dbUrl.trim();
        }
        String host = System.getenv("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
            String name = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "code_visualizer";
            return "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        return "jdbc:mysql://localhost:3306/code_visualizer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String getUsername() {
        String user = System.getenv("DB_USER");
        if (user != null && !user.trim().isEmpty()) {
            return user.trim();
        }
        return "root";
    }

    private static String getPassword() {
        String pass = System.getenv("DB_PASSWORD");
        if (pass != null) {
            return pass;
        }
        return "root";
    }

    /**
     * Obtains a new database connection.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUsername(), getPassword());
    }

    /**
     * Safely closes a database connection.
     * @param conn the connection to close
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore during shutdown
            }
        }
    }
}
