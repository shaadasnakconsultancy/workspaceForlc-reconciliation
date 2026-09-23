package com.smipl.lcrecon.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DbUtil {
    private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);
    private static HikariDataSource dataSource;

    public static void initialize() {
        try {
            Properties props = new Properties();
            InputStream is = DbUtil.class.getClassLoader().getResourceAsStream("application.properties");
            if (is != null) {
                props.load(is);
                is.close();
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.size", "10")));
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized. Call DbUtil.initialize() first.");
        }
        return dataSource.getConnection();
    }

    public static void runScript(String resourcePath) {
        try (Connection conn = getConnection();
             InputStream is = DbUtil.class.getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sb.append(line).append("\n");
            }

            // Split on GO or semicolons for SQL Server batch execution
            String[] statements = sb.toString().split("(?i)\\bGO\\b|;\\s*\n");
            Statement stmt = conn.createStatement();
            for (String sql : statements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    try {
                        stmt.execute(trimmedSql);
                    } catch (SQLException e) {
                        logger.warn("SQL statement warning: {}", e.getMessage());
                    }
                }
            }
            stmt.close();
            logger.info("SQL script executed: {}", resourcePath);
        } catch (Exception e) {
            logger.error("Failed to execute SQL script: {}", resourcePath, e);
        }
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool shut down");
        }
    }
}
