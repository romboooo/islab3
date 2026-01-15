package com.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

@Singleton
@Startup
public class CacheMonitorService {
    private static final Logger logger = Logger.getLogger(CacheMonitorService.class.getName());

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    @PostConstruct
    public void init() {
        try {
            Cache cache = entityManager.getEntityManagerFactory().getCache();
            logger.info("=== L2 Cache Initialized ===");
            // Проверяем, что кэш доступен (не null)
            logger.info("Cache is available: " + (cache != null));
            logger.info("Cache implementation: " + (cache != null ? cache.getClass().getName() : "null"));
            
            // Включим статистику, если это возможно
            if (cache != null) {
                org.eclipse.persistence.jpa.JpaEntityManager em = 
                    entityManager.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.sessions.Session session = em.getActiveSession();
                if (session instanceof org.eclipse.persistence.internal.sessions.AbstractSession) {
                    org.eclipse.persistence.internal.sessions.AbstractSession abstractSession = 
                        (org.eclipse.persistence.internal.sessions.AbstractSession) session;
                    abstractSession.getIdentityMapAccessor().initializeAllIdentityMaps();
                    logger.info("✓ L2 Cache ready for use");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Could not initialize cache monitoring: " + e.getMessage());
        }
    }
    
    public void clearCache() {
        entityManager.getEntityManagerFactory().getCache().evictAll();
        logger.info("L2 Cache cleared");
    }

    public String getCacheStatistics() {
        try {
            org.eclipse.persistence.jpa.JpaEntityManager em =
                    entityManager.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
            org.eclipse.persistence.sessions.Session session = em.getActiveSession();

            StringBuilder stats = new StringBuilder();
            stats.append("=== L2 Cache Statistics ===\n");

            // Получаем все дескрипторы классов
            for (org.eclipse.persistence.descriptors.ClassDescriptor descriptor :
                    session.getDescriptors().values()) {
                // Используем IdentityMapAccessor для получения IdentityMap по классу
                org.eclipse.persistence.internal.identitymaps.IdentityMap cache =
                        ((org.eclipse.persistence.internal.sessions.AbstractSession) session)
                                .getIdentityMapAccessorInstance().getIdentityMap(descriptor.getJavaClass());
                if (cache != null) {
                    stats.append(String.format("Class: %s, Size: %d\n",
                            descriptor.getJavaClass().getSimpleName(),
                            cache.getSize()));
                }
            }

            return stats.toString();
        } catch (Exception e) {
            return "Error getting cache statistics: " + e.getMessage();
        }
    }
}
