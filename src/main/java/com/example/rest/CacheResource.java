package com.example.rest;

import com.example.entity.Person;
import com.example.service.CacheStatisticsService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.example.entity.Dragon;
import java.util.HashMap;
import java.util.Map;

@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CacheResource {

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    @GET
    @Path("/test")
    public Response testCache() {
        Map<String, Object> result = new HashMap<>();

        long startTime = System.currentTimeMillis();

        entityManager.createQuery("SELECT d FROM Dragon d", Object.class).getResultList();
        long time1 = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        entityManager.createQuery("SELECT d FROM Dragon d", Object.class).getResultList();
        long time2 = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        entityManager.createQuery("SELECT d FROM Dragon d", Object.class).getResultList();
        long time3 = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        entityManager.createNamedQuery("Dragon.findByWeightGreaterThan", Object.class)
                .setParameter("weight", 100.0f)
                .getResultList();
        long time4 = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        entityManager.createNamedQuery("Dragon.findByWeightGreaterThan", Object.class)
                .setParameter("weight", 100.0f)
                .getResultList();
        long time5 = System.currentTimeMillis() - startTime;

        result.put("cache_enabled", true);
        result.put("query_times_ms", Map.of(
                "first_dragon_query", time1,
                "second_dragon_query", time2,
                "third_dragon_query", time3,
                "first_named_query", time4,
                "second_named_query", time5
        ));

        result.put("performance_gain", Map.of(
                "dragon_query_improvement", String.format("%.1f%%", ((double)(time1 - time2) / time1 * 100)),
                "named_query_improvement", String.format("%.1f%%", ((double)(time4 - time5) / time4 * 100))
        ));

        result.put("analysis",
                time2 < time1 && time5 < time4 ?
                        "Кэш работает: повторные запросы выполняются быстрее" :
                        "Кэш может не работать оптимально или данные уже в кэше"
        );

        return Response.ok(result).build();
    }

    @GET
    @Path("/clear")
    public Response clearCache() {
        try {
            entityManager.clear();

            jakarta.persistence.Cache cache = entityManager.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
                return Response.ok(Map.of(
                        "message", "Кэш первого и второго уровня очищен",
                        "status", "success"
                )).build();
            }

            return Response.ok(Map.of(
                    "message", "Кэш первого уровня очищен",
                    "status", "success"
            )).build();

        } catch (Exception e) {
            return Response.ok(Map.of(
                    "message", "Ошибка при очистке кэша: " + e.getMessage(),
                    "status", "error"
            )).build();
        }
    }

    @GET
    @Path("/info")
    public Response getCacheInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            jakarta.persistence.Cache cache = entityManager.getEntityManagerFactory().getCache();

            info.put("jpa_cache_available", cache != null);

            if (cache != null) {
                info.put("cache_implementation", cache.getClass().getName());

                try {
                    Object dragonId = entityManager.createQuery("SELECT MIN(d.id) FROM Dragon d", Object.class)
                            .getSingleResult();
                    if (dragonId != null) {
                        info.put("sample_dragon_id_in_cache", cache.contains(Dragon.class, dragonId));
                    }

                    Object personId = entityManager.createQuery("SELECT MIN(p.id) FROM Person p", Object.class)
                            .getSingleResult();
                    if (personId != null) {
                        info.put("sample_person_id_in_cache", cache.contains(Person.class, personId));
                    }
                } catch (Exception e) {
                    info.put("cache_check_error", e.getMessage());
                }
            }

            info.put("query_cache_enabled", true);
            info.put("second_level_cache_enabled", true);
            info.put("cache_expiry_ms", 600000);

        } catch (Exception e) {
            info.put("error", "Не удалось получить информацию о кэше: " + e.getMessage());
        }

        return Response.ok(info).build();
    }


    @GET
    @Path("/statistics")
    public Response getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            jakarta.persistence.Cache cache = entityManager.getEntityManagerFactory().getCache();

            if (cache != null && cache.getClass().getName().contains("JpaCache")) {
                Class<?> cacheClass = cache.getClass();

                java.lang.reflect.Method getSessionMethod = cacheClass.getMethod("getSession");
                Object session = getSessionMethod.invoke(cache);

                java.lang.reflect.Method getIdentityMapAccessorMethod = session.getClass().getMethod("getIdentityMapAccessorInstance");
                Object identityMapAccessor = getIdentityMapAccessorMethod.invoke(session);

                java.lang.reflect.Method getStatsMethod = identityMapAccessor.getClass().getMethod("getCacheStatistics");
                Object cacheStats = getStatsMethod.invoke(identityMapAccessor);

                Class<?> statsClass = cacheStats.getClass();
                java.lang.reflect.Method getHitCountMethod = statsClass.getMethod("getHitCount");
                java.lang.reflect.Method getMissCountMethod = statsClass.getMethod("getMissCount");
                java.lang.reflect.Method getObjectCountMethod = statsClass.getMethod("getObjectCount");

                Long hits = (Long) getHitCountMethod.invoke(cacheStats);
                Long misses = (Long) getMissCountMethod.invoke(cacheStats);
                Long objectCount = (Long) getObjectCountMethod.invoke(cacheStats);

                double hitRatio = (hits + misses) > 0 ? (hits * 100.0 / (hits + misses)) : 0;

                stats.put("status", "success");
                stats.put("hits", hits);
                stats.put("misses", misses);
                stats.put("objectCount", objectCount);
                stats.put("hitRatio", String.format("%.2f%%", hitRatio));
                stats.put("cacheImplementation", "EclipseLink L2 Cache");
            } else {
                stats.put("status", "info");
                stats.put("message", "EclipseLink cache statistics not available");
            }
        } catch (Exception e) {
            stats.put("status", "error");
            stats.put("message", "Cannot retrieve cache statistics: " + e.getMessage());
        }

        return Response.ok(stats).build();
    }

    @POST
    @Path("/statistics/enable")
    public Response enableStatistics() {
        if (cacheStatisticsService != null) {
            cacheStatisticsService.enableLogging();
            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Cache statistics logging enabled",
                    "loggingEnabled", true
            )).build();
        }
        return Response.ok(Map.of(
                "status", "error",
                "message", "CacheStatisticsService not available"
        )).build();
    }

    @POST
    @Path("/statistics/disable")
    public Response disableStatistics() {
        if (cacheStatisticsService != null) {
            cacheStatisticsService.disableLogging();
            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Cache statistics logging disabled",
                    "loggingEnabled", false
            )).build();
        }
        return Response.ok(Map.of(
                "status", "error",
                "message", "CacheStatisticsService not available"
        )).build();
    }

    @GET
    @Path("/statistics/status")
    public Response getStatisticsStatus() {
        if (cacheStatisticsService != null) {
            return Response.ok(Map.of(
                    "status", "success",
                    "loggingEnabled", cacheStatisticsService.isLoggingEnabled(),
                    "currentStatus", cacheStatisticsService.getStatus()
            )).build();
        }
        return Response.ok(Map.of(
                "status", "error",
                "message", "CacheStatisticsService not available"
        )).build();
    }

    @GET
    @Path("/method/stats")
    public Response getMethodStatistics() {
        try {
            Class<?> interceptorClass = Class.forName("com.example.interceptor.MethodStatisticsInterceptor");
            java.lang.reflect.Method getStatsMethod = interceptorClass.getMethod("getStatistics");

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> stats =
                    (Map<String, Map<String, Object>>) getStatsMethod.invoke(null);

            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.ok(Map.of(
                    "error", "Cannot retrieve method statistics: " + e.getMessage()
            )).build();
        }
    }

    @POST
    @Path("/method/stats/reset")
    public Response resetMethodStatistics() {
        try {
            Class<?> interceptorClass = Class.forName("com.example.interceptor.MethodStatisticsInterceptor");
            java.lang.reflect.Field statsMapField = interceptorClass.getDeclaredField("statsMap");
            statsMapField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, ?> statsMap = (Map<String, ?>) statsMapField.get(null);
            statsMap.clear();

            return Response.ok(Map.of(
                    "status", "success",
                    "message", "Method statistics reset"
            )).build();
        } catch (Exception e) {
            return Response.ok(Map.of(
                    "status", "error",
                    "message", "Cannot reset statistics: " + e.getMessage()
            )).build();
        }
    }
}
