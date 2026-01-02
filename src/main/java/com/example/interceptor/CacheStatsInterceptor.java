package com.example.interceptor;

import com.example.service.CacheStatsService;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;

@Interceptor
@CacheStatsLogging
public class CacheStatsInterceptor {
    private static final Logger logger = Logger.getLogger(CacheStatsInterceptor.class.getName());

    @Inject
    private CacheStatsService cacheStatsService;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext ctx) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();
            long endTime = System.currentTimeMillis();

            if (cacheStatsService.isLoggingEnabled()) {
                cacheStatsService.logCacheOperation(ctx.getMethod().getName(), endTime - startTime);
            }

            return result;
        } catch (Exception e) {
            logger.severe("Ошибка в методе " + ctx.getMethod().getName() + ": " + e.getMessage());
            throw e;
        }
    }
}