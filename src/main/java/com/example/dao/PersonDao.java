package com.example.dao;

import com.example.entity.Person;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class PersonDao {

    @PersistenceContext(unitName = "myPU")
    private EntityManager entityManager;

    public Person findById(Long id) {
        return entityManager.find(Person.class, id);
    }

    public List<Person> findAll() {
        return entityManager.createQuery("SELECT p FROM Person p", Person.class).getResultList();
    }

    public Person save(Person person) {
        if (person.getId() == null) {
            entityManager.persist(person);
            return person;
        } else {
            return entityManager.merge(person);
        }
    }

    public void delete(Long id) {
        Person person = findById(id);
        if (person != null) {
            entityManager.remove(person);
        }
    }
}