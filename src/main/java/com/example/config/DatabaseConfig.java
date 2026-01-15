package com.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

@Singleton
@Startup
public class DatabaseConfig {

    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());

    @Resource(lookup = "jdbc/postgres")
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        logger.info("=== Инициализация базы данных ===");

        try {
            // Тест через DataSource
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                logger.info("✅ Подключение к PostgreSQL установлено!");
                logger.info("✓ URL: " + conn.getMetaData().getURL());
                logger.info("✓ Database: " + conn.getMetaData().getDatabaseProductName());
                logger.info("✓ Version: " + conn.getMetaData().getDatabaseProductVersion());

                // Создаем таблицы если нужно
                createTablesIfNeeded(conn);
            }

        } catch (Exception e) {
            logger.warning("⚠️ Внимание: " + e.getMessage());
            logger.info("⚠️ Но приложение продолжает работу...");
        }

        logger.info("=== Инициализация завершена ===");
    }

    private void createTablesIfNeeded(Connection conn) throws Exception {
        // Проверяем существование таблиц
        String checkTables = """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_name = 'person'
            ) as person_exists,
            EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_name = 'dragon'
            ) as dragon_exists
            """;

        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery(checkTables)) {

            if (rs.next()) {
                boolean personExists = rs.getBoolean("person_exists");
                boolean dragonExists = rs.getBoolean("dragon_exists");

                logger.info("✓ Person table exists: " + personExists);
                logger.info("✓ Dragon table exists: " + dragonExists);
            }
        }
    }
}