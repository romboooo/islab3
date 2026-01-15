// com/example/interceptor/CacheStatisticsInterceptor.java
package com.example.interceptor;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Method;
import java.util.logging.Logger;

@Interceptor
@CacheStatistics
public class CacheStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    @PersistenceContext(unitName = "myPU")
    private EntityManager em;

    private boolean loggingEnabled = true;

    @PostConstruct
    public void init() {
        // Можно получать настройки из конфигурационного файла
        String enableLogging = System.getProperty("cache.statistics.enabled", "true");
        loggingEnabled = Boolean.parseBoolean(enableLogging);
    }

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext ctx) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();

            if (loggingEnabled && em != null) {
                logCacheStats();
            }

            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.fine(() -> String.format("Method %s executed in %d ms",
                    ctx.getMethod().getName(), duration));
        }
    }

    private void logCacheStats() {
        try {
            Cache cache = em.getEntityManagerFactory().getCache();

            // Используем reflection для доступа к EclipseLink API
            // потому что эти классы могут быть недоступны напрямую в зависимости от версии

            if (cache != null) {
                Class<?> cacheClass = cache.getClass();

                // Проверяем, это EclipseLink JpaCache?
                if (cacheClass.getName().contains("JpaCache")) {
                    // Получаем сессию через reflection
                    Method getSessionMethod = cacheClass.getMethod("getSession");
                    Object session = getSessionMethod.invoke(cache);

                    // Получаем IdentityMapAccessor
                    Method getIdentityMapAccessorMethod = session.getClass().getMethod("getIdentityMapAccessorInstance");
                    Object identityMapAccessor = getIdentityMapAccessorMethod.invoke(session);

                    // Получаем статистику
                    Method getStatsMethod = identityMapAccessor.getClass().getMethod("getCacheStatistics");
                    Object stats = getStatsMethod.invoke(identityMapAccessor);

                    // Получаем значения через reflection
                    Class<?> statsClass = stats.getClass();
                    Method getHitCountMethod = statsClass.getMethod("getHitCount");
                    Method getMissCountMethod = statsClass.getMethod("getMissCount");
                    Method getObjectCountMethod = statsClass.getMethod("getObjectCount");

                    Long hits = (Long) getHitCountMethod.invoke(stats);
                    Long misses = (Long) getMissCountMethod.invoke(stats);
                    Long objectCount = (Long) getObjectCountMethod.invoke(stats);

                    logger.info(() -> String.format(
                            "L2 Cache Statistics - Hits: %d, Misses: %d, Total Objects: %d, Hit Ratio: %.2f%%",
                            hits,
                            misses,
                            objectCount,
                            hits + misses > 0 ? (hits * 100.0 / (hits + misses)) : 0
                    ));
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки - возможно, статистика недоступна
            logger.fine("Cache statistics not available or disabled: " + e.getMessage());
        }
    }
}