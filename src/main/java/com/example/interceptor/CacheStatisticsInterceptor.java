// com/example/interceptor/CacheStatisticsInterceptor.java
package com.example.interceptor;

import com.example.service.CacheStatisticsService;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * CDI Interceptor для логирования статистики использования L2 JPA Cache
 * Использует CacheStatisticsService для управления состоянием логирования
 */
@Interceptor
@CacheStatistics
public class CacheStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    @PersistenceContext(unitName = "myPU")
    private EntityManager em;

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext ctx) throws Exception {
        try {
            Object result = ctx.proceed();

            // Логируем статистику только если включено через сервис
            if (cacheStatisticsService != null && 
                cacheStatisticsService.isLoggingEnabled() && 
                em != null) {
                logCacheStats(ctx.getMethod().getName());
            }

            return result;
        } catch (Exception e) {
            // Пробрасываем исключение дальше
            throw e;
        }
    }

    /**
     * Логирует статистику использования L2 JPA Cache (cache hits, cache misses)
     * @param methodName имя метода, для которого логируется статистика
     */
    private void logCacheStats(String methodName) {
        try {
            // Используем EntityManagerFactory для получения кэша
            jakarta.persistence.EntityManagerFactory emf = em.getEntityManagerFactory();
            Cache cache = emf.getCache();

            if (cache == null) {
                logger.warning("Cache is null - L2 cache may not be enabled");
                return;
            }

            Class<?> cacheClass = cache.getClass();
            logger.fine("Cache class: " + cacheClass.getName());

            // Проверяем, это EclipseLink JpaCache?
            if (cacheClass.getName().contains("JpaCache")) {
                // Получаем сессию через reflection
                Method getSessionMethod = cacheClass.getMethod("getSession");
                Object session = getSessionMethod.invoke(cache);

                if (session == null) {
                    logger.warning("Session is null");
                    return;
                }

                // Получаем IdentityMapAccessor
                Method getIdentityMapAccessorMethod = session.getClass().getMethod("getIdentityMapAccessorInstance");
                Object identityMapAccessor = getIdentityMapAccessorMethod.invoke(session);

                if (identityMapAccessor == null) {
                    logger.warning("IdentityMapAccessor is null");
                    return;
                }

                // Получаем статистику кэша
                Method getStatsMethod = identityMapAccessor.getClass().getMethod("getCacheStatistics");
                Object stats = getStatsMethod.invoke(identityMapAccessor);

                if (stats == null) {
                    logger.warning("Cache statistics is null - statistics may not be enabled");
                    return;
                }

                // Получаем значения через reflection
                Class<?> statsClass = stats.getClass();
                Method getHitCountMethod = statsClass.getMethod("getHitCount");
                Method getMissCountMethod = statsClass.getMethod("getMissCount");
                Method getObjectCountMethod = statsClass.getMethod("getObjectCount");

                Long hits = (Long) getHitCountMethod.invoke(stats);
                Long misses = (Long) getMissCountMethod.invoke(stats);
                Long objectCount = (Long) getObjectCountMethod.invoke(stats);

                // Вычисляем hit ratio
                double hitRatio = (hits + misses) > 0 ? (hits * 100.0 / (hits + misses)) : 0;

                // Логируем статистику L2 Cache с информацией о методе
                logger.info(String.format(
                        "[L2 Cache Statistics] Method: %s | Hits: %d, Misses: %d, Total Objects: %d, Hit Ratio: %.2f%%",
                        methodName,
                        hits,
                        misses,
                        objectCount,
                        hitRatio
                ));
            } else {
                logger.warning("Cache implementation is not EclipseLink JpaCache: " + cacheClass.getName());
            }
        } catch (NoSuchMethodException e) {
            logger.warning("Cache statistics API not available: " + e.getMessage() + 
                    ". Make sure 'eclipselink.cache.statistics' is set to 'true' in persistence.xml");
        } catch (Exception e) {
            // Логируем на уровне WARNING для отладки
            logger.warning("Error retrieving cache statistics: " + e.getMessage());
            logger.fine("Stack trace: ", e);
        }
    }
}