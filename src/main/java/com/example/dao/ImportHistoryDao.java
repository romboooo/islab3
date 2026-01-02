package com.example.dao;

import com.example.entity.ImportHistory;
import com.example.entity.ImportStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ImportHistoryDao {

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    public ImportHistory save(ImportHistory importHistory) {
        if (importHistory.getId() == null) {
            entityManager.persist(importHistory);
            return importHistory;
        } else {
            return entityManager.merge(importHistory);
        }
    }

    public List<ImportHistory> findAll() {
        return entityManager.createQuery("SELECT ih FROM ImportHistory ih ORDER BY ih.startTime DESC", ImportHistory.class)
                .getResultList();
    }

    public ImportHistory findById(Long id) {
        return entityManager.find(ImportHistory.class, id);
    }

    public List<ImportHistory> findByStatus(ImportStatus status) {
        return entityManager.createQuery("SELECT ih FROM ImportHistory ih WHERE ih.status = :status ORDER BY ih.startTime DESC", ImportHistory.class)
                .setParameter("status", status)
                .getResultList();
    }
}