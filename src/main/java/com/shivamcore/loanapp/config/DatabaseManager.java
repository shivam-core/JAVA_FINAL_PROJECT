package com.shivamcore.loanapp.config;

import com.shivamcore.loanapp.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to MySQL using settings provided by ConfigLoader.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(ConfigLoader configLoader) {
        this.url = configLoader.get("db.url");
        this.user = configLoader.get("db.user");
        this.password = configLoader.getOrDefault("db.password", "");
        loadDriver();
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password != null ? password : "";
        loadDriver();
    }

    private void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found on classpath", e);
            throw new DataAccessException("MySQL JDBC Driver not found on classpath", e);
        }
    }

    /**
     * Obtains a new JDBC connection. Callers MUST manage lifecycle using try-with-resources.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            logger.error("Failed to establish database connection to: {}", url, e);
            throw new DataAccessException("Could not connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Tests whether database connection can be established.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            logger.warn("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUrl() {
        return url;
    }
}
