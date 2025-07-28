package com.bryce.discord.services;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String URL = dotenv.get("DATABASE_PATH");
    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile boolean isInitialized = false;

    public static Connection getConnection() throws SQLException {

        if (!isInitialized) {
            initializeDatabase();
        }

        Connection conn = DriverManager.getConnection(URL);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 30000");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = memory");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Warning: Could not set PRAGMA settings: " + e.getMessage());

        }

        return conn;
    }

    private static void initializeDatabase() {
        lock.lock();
        try {
            if (isInitialized) return;

            try (Connection testConn = DriverManager.getConnection(URL)) {
                try (Statement stmt = testConn.createStatement()) {

                    stmt.execute("SELECT 1");
                    System.out.println("[DatabaseManager] Database connection established successfully");
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] Database connection test failed: " + e.getMessage());

                handleDatabaseLock();
            }

            isInitialized = true;
        } finally {
            lock.unlock();
        }
    }

    private static void handleDatabaseLock() {
        System.out.println("[DatabaseManager] Attempting to resolve database lock...");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (Connection conn = DriverManager.getConnection(URL + "?journal_mode=DELETE")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                System.out.println("[DatabaseManager] Database lock resolved");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Could not resolve database lock: " + e.getMessage());
            System.err.println("[DatabaseManager] You may need to restart the application or check for other processes using the database");
        }
    }

    public static <T> T executeWithRetry(DatabaseOperation<T> operation) throws SQLException {
        int maxRetries = 3;
        SQLException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = getConnection()) {
                return operation.execute(conn);
            } catch (SQLException e) {
                lastException = e;
                if (e.getMessage().contains("database is locked") && attempt < maxRetries) {
                    System.err.println("[DatabaseManager] Database locked, retrying... (attempt " + attempt + "/" + maxRetries + ")");
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted during retry", ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection conn) throws SQLException;
    }
}