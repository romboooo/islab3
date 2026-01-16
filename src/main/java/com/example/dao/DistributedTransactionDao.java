// src/main/java/com/example/dao/DistributedTransactionDao.java
package com.example.dao;

import com.example.entity.DistributedTransaction;
import com.example.entity.TransactionStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class DistributedTransactionDao {
    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    public DistributedTransaction save(DistributedTransaction transaction) {
        if (transaction.getId() == null) {
            entityManager.persist(transaction);
            return transaction;
        } else {
            return entityManager.merge(transaction);
        }
    }

    public DistributedTransaction findById(Long id) {
        return entityManager.find(DistributedTransaction.class, id);
    }

    public DistributedTransaction findByTransactionId(String transactionId) {
        TypedQuery<DistributedTransaction> query = entityManager.createQuery(
                "SELECT dt FROM DistributedTransaction dt WHERE dt.transactionId = :transactionId",
                DistributedTransaction.class);
        query.setParameter("transactionId", transactionId);
        List<DistributedTransaction> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<DistributedTransaction> findPreparedTransactionsOlderThan(LocalDateTime timestamp) {
        return entityManager.createQuery(
                        "SELECT dt FROM DistributedTransaction dt WHERE dt.status = :status AND dt.createdAt < :timestamp",
                        DistributedTransaction.class)
                .setParameter("status", TransactionStatus.PREPARED)
                .setParameter("timestamp", timestamp)
                .getResultList();
    }

    public List<DistributedTransaction> findAll() {
        return entityManager.createQuery(
                "SELECT dt FROM DistributedTransaction dt ORDER BY dt.createdAt DESC",
                DistributedTransaction.class
        ).getResultList();
    }
}