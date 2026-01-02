package com.example.dao;

import com.example.entity.Dragon;
import com.example.entity.Person;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Stateless
public class DragonDao {

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    public Dragon findById(Long id) {
        return entityManager.find(Dragon.class, id);
    }

    public List<Dragon> findAll() {
        return entityManager.createQuery("SELECT d FROM Dragon d", Dragon.class).getResultList();
    }

    public List<Dragon> findAllPaginated(int page, int size) {
        return entityManager.createQuery("SELECT d FROM Dragon d ORDER BY d.id", Dragon.class)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();
    }
    public boolean existsByKillerId(Long killerId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(d) FROM Dragon d WHERE d.killer.id = :killerId", Long.class);
        query.setParameter("killerId", killerId);
        Long count = query.getSingleResult();
        return count > 0;
    }
    public List<Dragon> findWithFilters(Map<String, Object> filters, int page, int size, String sortBy, String sortOrder) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Dragon> query = cb.createQuery(Dragon.class);
        Root<Dragon> root = query.from(Dragon.class);

        List<Predicate> predicates = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get(entry.getKey())),
                        "%" + entry.getValue().toString().toLowerCase() + "%"));
            }
        }

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        if (sortBy != null && !sortBy.isEmpty()) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                query.orderBy(cb.asc(root.get(sortBy)));
            } else {
                query.orderBy(cb.desc(root.get(sortBy)));
            }
        }

        return entityManager.createQuery(query)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();
    }

    public Long countWithFilters(Map<String, Object> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Dragon> root = query.from(Dragon.class);

        List<Predicate> predicates = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get(entry.getKey())),
                        "%" + entry.getValue().toString().toLowerCase() + "%"));
            }
        }

        query.select(cb.count(root));
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(query).getSingleResult();
    }

    public Dragon save(Dragon dragon) {
        if (dragon.getId() == null) {
            entityManager.persist(dragon);
            return dragon;
        } else {
            return entityManager.merge(dragon);
        }
    }

    public void delete(Long id) {
        Dragon dragon = findById(id);
        if (dragon != null) {
            entityManager.remove(dragon);
        }
    }

    public Long getSumOfAges() {
        return entityManager.createQuery("SELECT SUM(d.age) FROM Dragon d", Long.class)
                .getSingleResult();
    }

    public List<Dragon> findByWeightGreaterThan(Float weight) {
        return entityManager.createQuery("SELECT d FROM Dragon d WHERE d.weight > :weight", Dragon.class)
                .setParameter("weight", weight)
                .getResultList();
    }

    public List<Long> findUniqueAges() {
        return entityManager.createQuery("SELECT DISTINCT d.age FROM Dragon d ORDER BY d.age", Long.class)
                .getResultList();
    }
}