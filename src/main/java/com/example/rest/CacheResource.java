package com.example.rest;

import com.example.service.CacheStatsService;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
public class CacheResource {

    @Inject
    private CacheStatsService cacheStatsService;

    @GET
    @Path("/stats")
    public Response getCacheStats() {
        try {
            Cache cache = cacheStatsService.getCache();
            Map<String, Object> stats = new HashMap<>();

            stats.put("status", "OK");
            stats.put("cacheProvider", "EclipseLink L2 Cache");
            stats.put("loggingEnabled", cacheStatsService.isLoggingEnabled());
            stats.put("cacheEnabled", cache != null);

            if (cache != null) {
                stats.put("cacheClass", cache.getClass().getName());
                stats.put("timestamp", System.currentTimeMillis());

                Map<String, Boolean> entityCacheStatus = new HashMap<>();
                entityCacheStatus.put("Dragon", true);
                entityCacheStatus.put("Person", true);
                entityCacheStatus.put("ImportHistory", true);

                stats.put("entityCacheStatus", entityCacheStatus);
            }

            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logging/{enabled}")
    public Response setLoggingEnabled(@PathParam("enabled") boolean enabled) {
        try {
            cacheStatsService.setLoggingEnabled(enabled);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("message", "Логирование статистики кэша " +
                    (enabled ? "включено" : "выключено"));
            response.put("timestamp", System.currentTimeMillis());
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/clear")
    public Response clearCache() {
        try {
            Cache cache = cacheStatsService.getCache();
            if (cache != null) {
                cache.evictAll();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("message", "Весь L2 кэш очищен");
            response.put("timestamp", System.currentTimeMillis());
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/clear/{entity}")
    public Response clearEntityCache(@PathParam("entity") String entityName) {
        try {
            Cache cache = cacheStatsService.getCache();
            if (cache == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Кэш не доступен"))
                        .build();
            }

            Class<?> entityClass = null;
            switch (entityName.toLowerCase()) {
                case "dragon":
                    entityClass = com.example.entity.Dragon.class;
                    break;
                case "person":
                    entityClass = com.example.entity.Person.class;
                    break;
                case "importhistory":
                    entityClass = com.example.entity.ImportHistory.class;
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Неизвестная сущность: " + entityName))
                            .build();
            }

            cache.evict(entityClass);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("message", "Кэш для сущности " + entityName + " очищен");
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}