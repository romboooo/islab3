package com.example.service;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Сервис для управления состоянием логирования статистики L2 JPA Cache
 */
@Singleton
@Startup
public class CacheStatisticsService {

    private static final Logger logger = Logger.getLogger(CacheStatisticsService.class.getName());

    // Атомарная переменная для thread-safe доступа
    private final AtomicBoolean loggingEnabled = new AtomicBoolean(true);

    @jakarta.annotation.PostConstruct
    public void init() {
        // Проверяем системное свойство при старте
        String enabled = System.getProperty("cache.statistics.enabled", "true");
        loggingEnabled.set(Boolean.parseBoolean(enabled));
        logger.info("Cache statistics logging initialized: " + loggingEnabled.get());
    }

    /**
     * Включить логирование статистики кэша
     */
    public void enableLogging() {
        boolean previous = loggingEnabled.getAndSet(true);
        System.setProperty("cache.statistics.enabled", "true");
        logger.info("Cache statistics logging enabled (was: " + previous + ")");
    }

    /**
     * Отключить логирование статистики кэша
     */
    public void disableLogging() {
        boolean previous = loggingEnabled.getAndSet(false);
        System.setProperty("cache.statistics.enabled", "false");
        logger.info("Cache statistics logging disabled (was: " + previous + ")");
    }

    /**
     * Проверить, включено ли логирование
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled.get();
    }

    /**
     * Получить текущее состояние логирования
     */
    public String getStatus() {
        return loggingEnabled.get() ? "enabled" : "disabled";
    }
}
