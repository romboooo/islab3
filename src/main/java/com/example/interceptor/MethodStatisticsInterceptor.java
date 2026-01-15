package com.example.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

@Interceptor
@MethodStatistics
public class MethodStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(MethodStatisticsInterceptor.class.getName());
    private static final Map<String, MethodStats> statsMap = new ConcurrentHashMap<>();

    @AroundInvoke
    public Object collectStatistics(InvocationContext ctx) throws Exception {
        String methodName = ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();

            long duration = System.currentTimeMillis() - startTime;
            updateStats(methodName, duration, true);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            updateStats(methodName, duration, false);
            throw e;
        }
    }

    private synchronized void updateStats(String methodName, long duration, boolean success) {
        MethodStats stats = statsMap.getOrDefault(methodName, new MethodStats());
        stats.incrementCount();
        stats.addExecutionTime(duration);
        if (!success) {
            stats.incrementErrors();
        }
        statsMap.put(methodName, stats);
    }

    public static Map<String, Map<String, Object>> getStatistics() {
        Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();

        for (Map.Entry<String, MethodStats> entry : statsMap.entrySet()) {
            Map<String, Object> methodStats = new ConcurrentHashMap<>();
            MethodStats stats = entry.getValue();

            methodStats.put("invocationCount", stats.getCount());
            methodStats.put("errorCount", stats.getErrorCount());
            methodStats.put("totalTimeMs", stats.getTotalTime());
            methodStats.put("averageTimeMs", stats.getCount() > 0 ?
                    stats.getTotalTime() / stats.getCount() : 0);

            result.put(entry.getKey(), methodStats);
        }

        return result;
    }

    private static class MethodStats {
        private int count = 0;
        private long totalTime = 0;
        private int errorCount = 0;

        public void incrementCount() { count++; }
        public void addExecutionTime(long time) { totalTime += time; }
        public void incrementErrors() { errorCount++; }

        public int getCount() { return count; }
        public long getTotalTime() { return totalTime; }
        public int getErrorCount() { return errorCount; }
    }
}
