package com.example.interceptor;

import com.example.service.CacheStatisticsService;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.annotation.Priority;
import java.util.logging.Logger;

@Priority(Interceptor.Priority.APPLICATION)
@Interceptor
@CacheStatistics
public class CacheStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    private long totalHits = 0;
    private long totalMisses = 0;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext ctx) throws Exception {
        if (cacheStatisticsService == null || !cacheStatisticsService.isLoggingEnabled()) {
            return ctx.proceed();
        }

        String methodName = ctx.getMethod().getName();
        String className = ctx.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;

        long startTime = System.nanoTime();
        boolean[] hadSqlQuery = new boolean[1]; // Для определения был ли SQL


        try {
            Object result = ctx.proceed();
            long duration = System.nanoTime() - startTime;
            double durationMs = duration / 1_000_000.0;

            boolean isHit = determineIfCacheHit(methodName, durationMs);

            if (isHit) {
                totalHits++;
                logWithColor(String.format(
                        ANSI_GREEN + "[CACHE HIT]  " + ANSI_RESET +
                                ANSI_CYAN + "%-40s" + ANSI_RESET +
                                ANSI_YELLOW + " | Time: %6.2f ms" + ANSI_RESET +
                                ANSI_GREEN + " | Total Hits: %d" + ANSI_RESET,
                        fullMethodName, durationMs, totalHits
                ));
            } else {
                totalMisses++;
                logWithColor(String.format(
                        ANSI_RED + "[CACHE MISS] " + ANSI_RESET +
                                ANSI_CYAN + "%-40s" + ANSI_RESET +
                                ANSI_YELLOW + " | Time: %6.2f ms" + ANSI_RESET +
                                ANSI_RED + " | Total Misses: %d" + ANSI_RESET,
                        fullMethodName, durationMs, totalMisses
                ));
            }

            if ((totalHits + totalMisses) % 5 == 0) {
                printOverallStatistics();
            }

            return result;
        } catch (Exception e) {
            logWithColor(String.format(
                    ANSI_RED + "[CACHE ERROR] %s" + ANSI_RESET,
                    fullMethodName
            ));
            throw e;
        }
    }

    private boolean determineIfCacheHit(String methodName, double durationMs) {

        return durationMs < 5.0;
    }

    private void printOverallStatistics() {
        long total = totalHits + totalMisses;
        if (total > 0) {
            double hitRatio = (totalHits * 100.0) / total;
            String color = hitRatio > 70 ? ANSI_GREEN :
                    hitRatio > 30 ? ANSI_YELLOW : ANSI_RED;

            logWithColor(String.format(
                    color + "[CACHE STATS]" + ANSI_RESET +
                            " Hits: " + ANSI_GREEN + "%d" + ANSI_RESET +
                            ", Misses: " + ANSI_RED + "%d" + ANSI_RESET +
                            ", Total: %d, " + color + "Hit Ratio: %.1f%%" + ANSI_RESET,
                    totalHits, totalMisses, total, hitRatio
            ));
        }
    }

    private void logWithColor(String message) {
        System.out.println(message);

        String cleanMessage = message
                .replace(ANSI_RESET, "")
                .replace(ANSI_GREEN, "")
                .replace(ANSI_RED, "")
                .replace(ANSI_YELLOW, "")
                .replace(ANSI_CYAN, "");
        logger.info(cleanMessage);
    }
}