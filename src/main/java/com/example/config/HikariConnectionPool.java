package com.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import java.util.logging.Logger;

@Singleton
@Startup
public class HikariConnectionPool {

    private static final Logger logger = Logger.getLogger(HikariConnectionPool.class.getName());

    private HikariDataSource hikariDataSource;

    public void init() {
        try {
            logger.info("Инициализация HikariCP Connection Pool");

            String dbUrl = System.getenv("DB_URL");
            String dbUsername = System.getenv("DB_USERNAME");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                throw new RuntimeException("Database environment variables are not set. Please set DB_URL, DB_USERNAME, DB_PASSWORD");
            }

            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);

            config.setPoolName("DragonHikariPool");
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");

            config.setLeakDetectionThreshold(5000);
            config.setValidationTimeout(5000);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            hikariDataSource = new HikariDataSource(config);

            try (var conn = hikariDataSource.getConnection()) {
                logger.info("HikariCP успешно подключился к базе данных: "
                        + conn.getMetaData().getDatabaseProductName());
            }

            logger.info("HikariCP Pool инициализирован: " + config.getPoolName());

        } catch (Exception e) {
            logger.severe("Ошибка инициализации HikariCP: " + e.getMessage());
            throw new RuntimeException("HikariCP initialization failed", e);
        }
    }

    public DataSource getDataSource() {
        return hikariDataSource;
    }

    public String getPoolStats() {
        if (hikariDataSource == null || hikariDataSource.isClosed()) {
            return "Pool is not initialized or closed";
        }

        try {
            return String.format(
                    "HikariCP Pool Stats: Active=%d, Idle=%d, Total=%d, Waiting=%d",
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                    hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                    hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        } catch (Exception e) {
            return "Error getting pool stats: " + e.getMessage();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            logger.info("Завершение работы HikariCP пула соединений");
            hikariDataSource.close();
        }
    }
}