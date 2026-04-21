package com.otpservice.db;

import com.otpservice.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final HikariDataSource dataSource;

    private DatabaseManager() {
        AppConfig cfg = AppConfig.getInstance();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(cfg.getDbUrl());
        config.setUsername(cfg.getDbUsername());
        config.setPassword(cfg.getDbPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("otp-pool");
        this.dataSource = new HikariDataSource(config);
        log.info("Connection pool initialized: {}", config.getPoolName());
    }

    public static DatabaseManager getInstance() { return INSTANCE; }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initializeSchema() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) throw new RuntimeException("schema.sql not found in classpath");
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            log.info("Database schema initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException(e);
        }
    }
}
