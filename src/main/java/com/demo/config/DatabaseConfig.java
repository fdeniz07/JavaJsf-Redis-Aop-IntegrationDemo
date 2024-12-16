package com.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String DB_DRIVER = AppConfig.getProperty("db.driver");
    private static final String DB_URL = AppConfig.getProperty("db.url");
    private static final String DB_USER = AppConfig.getProperty("db.username");
    private static final String DB_PASSWORD = AppConfig.getProperty("db.password");

    static {
        try {
            Class.forName(DB_DRIVER);
            logger.info("Database driver {} loaded successfully", DB_DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error("Database driver {} not found", DB_DRIVER, e);
            throw new RuntimeException("Database driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.debug("Database connection established successfully");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to establish database connection", e);
            throw e;
        }
    }
}