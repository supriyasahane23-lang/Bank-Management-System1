package com.bank.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connections to MySQL database.
 * Reads settings from db.properties with sensible defaults.
 */
public class DatabaseConfig {
    private static String dbUrl = "jdbc:mysql://localhost:3306/bank_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String dbUser = "root";
    private static String dbPassword = "root";

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found in classpath! Make sure mysql-connector-j.jar is in lib folder.");
        }
    }

    private static void loadProperties() {
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream input = new FileInputStream(propFile)) {
                Properties prop = new Properties();
                prop.load(input);
                if (prop.getProperty("db.url") != null) dbUrl = prop.getProperty("db.url").trim();
                if (prop.getProperty("db.user") != null) dbUser = prop.getProperty("db.user").trim();
                if (prop.getProperty("db.password") != null) dbPassword = prop.getProperty("db.password").trim();
            } catch (Exception e) {
                System.err.println("⚠️ Could not read db.properties, using default database connection settings: " + e.getMessage());
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}
