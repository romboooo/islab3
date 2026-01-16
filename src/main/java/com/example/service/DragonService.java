package com.example.service;

import com.example.dao.DragonDao;
import com.example.dao.PersonDao;
import com.example.dto.DragonDto;
import com.example.entity.Dragon;
import com.example.entity.Person;
import com.example.interceptor.CacheStatistics;
import com.example.interceptor.MethodStatistics;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@MethodStatistics
public class DragonService {

    @Inject
    private DragonDao dragonDao;

    @Inject
    private PersonDao personDao;

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;
    @CacheStatistics
    @MethodStatistics
    public DragonDto findById(Long id) {
        Dragon dragon = dragonDao.findById(id);
        return dragon != null ? new DragonDto(dragon) : null;
    }
    @CacheStatistics
    @MethodStatistics
    public List<DragonDto> findAll() {
        return dragonDao.findAll().stream()
                .map(DragonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public List<DragonDto> findAllPaginated(int page, int size) {
        return dragonDao.findAllPaginated(page, size).stream()
                .map(DragonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public List<DragonDto> findWithFilters(Map<String, Object> filters, int page, int size, String sortBy, String sortOrder) {
        return dragonDao.findWithFilters(filters, page, size, sortBy, sortOrder).stream()
                .map(DragonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public Long countWithFilters(Map<String, Object> filters) {
        return dragonDao.countWithFilters(filters);
    }
    @CacheStatistics
    @MethodStatistics
    public DragonDto save(@Valid DragonDto dragonDto) {
        if ("true".equals(System.getProperty("db.fail"))) {
            throw new RuntimeException("Database is unavailable");
        }
        if (dragonDto.getId() == null && !isDragonNameUnique(dragonDto.getName())) {
            throw new RuntimeException("Dragon with name '" + dragonDto.getName() + "' already exists");
        }

        if (dragonDto.getId() != null && !isDragonNameUnique(dragonDto.getName(), dragonDto.getId())) {
            throw new RuntimeException("Dragon with name '" + dragonDto.getName() + "' already exists");
        }

        Dragon dragon = convertToEntity(dragonDto);

        if (dragonDto.getKiller() != null && dragonDto.getKiller().getId() != null) {
            Person killer = personDao.findById(dragonDto.getKiller().getId());
            dragon.setKiller(killer);
        }

        Dragon saved = dragonDao.save(dragon);
        return new DragonDto(saved);
    }
    @CacheStatistics
    @MethodStatistics
    public void delete(Long id, Long newKillerId) {
        Dragon dragon = dragonDao.findById(id);
        if (dragon != null) {
            if (newKillerId != null) {
                List<Dragon> relatedDragons = dragonDao.findAll().stream()
                        .filter(d -> d.getKiller() != null && d.getKiller().getId().equals(id))
                        .collect(Collectors.toList());

                Person newKiller = personDao.findById(newKillerId);
                for (Dragon related : relatedDragons) {
                    related.setKiller(newKiller);
                    dragonDao.save(related);
                }
            }
            dragonDao.delete(id);
        }
    }
    @CacheStatistics
    @MethodStatistics
    private Dragon convertToEntity(DragonDto dto) {
        Dragon dragon = new Dragon();
        dragon.setId(dto.getId());
        dragon.setName(dto.getName());
        dragon.setCoordinates(dto.getCoordinates());
        dragon.setCave(dto.getCave());
        dragon.setAge(dto.getAge());
        dragon.setWeight(dto.getWeight());
        dragon.setColor(dto.getColor());
        dragon.setCharacter(dto.getCharacter());
        dragon.setHead(dto.getHead());
        return dragon;
    }
    @CacheStatistics
    @MethodStatistics
    public Long getSumOfAges() {
        return entityManager.createQuery("SELECT SUM(d.age) FROM Dragon d", Long.class)
                .getSingleResult();
    }


    @CacheStatistics
    @MethodStatistics
    public List<DragonDto> findByWeightGreaterThan(Float weight) {
        return dragonDao.findByWeightGreaterThan(weight).stream()
                .map(DragonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public List<Long> findUniqueAges() {
        return entityManager.createQuery("SELECT DISTINCT d.age FROM Dragon d ORDER BY d.age", Long.class)
                .getResultList();
    }
    @CacheStatistics
    @MethodStatistics
    public boolean isDragonNameUnique(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        Long count = entityManager.createQuery(
                        "SELECT COUNT(d) FROM Dragon d WHERE LOWER(d.name) = LOWER(:name)", Long.class)
                .setParameter("name", name.trim())
                .getSingleResult();
        return count == 0;
    }
    @CacheStatistics
    @MethodStatistics
    public boolean isDragonNameUnique(String name, Long excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        Long count = entityManager.createQuery(
                        "SELECT COUNT(d) FROM Dragon d WHERE LOWER(d.name) = LOWER(:name) AND d.id != :excludeId", Long.class)
                .setParameter("name", name.trim())
                .setParameter("excludeId", excludeId)
                .getSingleResult();
        return count == 0;
    }
}