package com.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

@ApplicationScoped
public class CacheStatsService {

    private static final Logger logger = Logger.getLogger(CacheStatsService.class.getName());

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    private boolean loggingEnabled = false;

    @PostConstruct
    public void init() {
        String enabled = System.getProperty("cache.stats.logging.enabled", "false");
        loggingEnabled = Boolean.parseBoolean(enabled);
        logger.info("CacheStatsService инициализирован. Логирование статистики кэша " +
                (loggingEnabled ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО"));
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
        System.setProperty("cache.stats.logging.enabled", String.valueOf(enabled));
        logger.info("Логирование статистики кэша " + (enabled ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО"));
    }

    public Cache getCache() {
        return entityManager.getEntityManagerFactory().getCache();
    }

    public void logCacheOperation(String methodName, long duration) {
        if (loggingEnabled) {
            logger.info(String.format(
                    "📊 Метод %s выполнен за %d мс. L2 кэш используется: %s",
                    methodName,
                    duration,
                    getCache() != null ? "Да" : "Нет"
            ));
        }
    }
}