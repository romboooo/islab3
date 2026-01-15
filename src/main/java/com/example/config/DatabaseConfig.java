package com.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.logging.Logger;

@Singleton
@Startup
public class DatabaseConfig {
    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());
    private static DatabaseConfig instance;

    public DatabaseConfig() {
        instance = this;
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("=== Initializing Database Configuration ===");
            
            // Тестируем подключение к базе данных
            testDatabaseConnection();
            
            logger.info("=== Database Configuration Complete ===");
        } catch (Exception e) {
            logger.severe("✗ Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testDatabaseConnection() {
        try {
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:app/jdbc/postgres");
            
            try (Connection conn = dataSource.getConnection()) {
                logger.info("✓ Database connection successful!");
                logger.info("  Database: " + conn.getMetaData().getDatabaseProductName());
                logger.info("  URL: " + conn.getMetaData().getURL());
                logger.info("  Driver: " + conn.getMetaData().getDriverName());
            }
        } catch (Exception e) {
            logger.severe("✗ Cannot connect to database: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("DatabaseConfig cleanup");
    }

    public static DatabaseConfig getInstance() {
        return instance;
    }
}
