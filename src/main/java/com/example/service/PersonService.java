package com.example.service;

import com.example.dao.DragonDao;
import com.example.dao.PersonDao;
import com.example.dto.PersonDto;
import com.example.entity.Person;
import com.example.entity.Country;
import com.example.interceptor.CacheStatistics;
import com.example.interceptor.MethodStatistics;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@MethodStatistics
public class PersonService {

    @Inject
    private PersonDao personDao;

    @Inject
    private DragonDao dragonDao;
    @CacheStatistics
    @MethodStatistics
    public PersonDto findById(Long id) {
        Person person = personDao.findById(id);
        return person != null ? new PersonDto(person) : null;
    }
    @CacheStatistics
    @MethodStatistics
    public List<PersonDto> findAll() {
        return personDao.findAll().stream()
                .map(PersonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public PersonDto save(@Valid PersonDto personDto) {
        Person person = convertToEntity(personDto);
        Person saved = personDao.save(person);
        return new PersonDto(saved);
    }
    @CacheStatistics
    @MethodStatistics
    public void delete(Long id) {
        boolean hasDragonReferences = dragonDao.existsByKillerId(id);

        if (hasDragonReferences) {
            throw new RuntimeException("Cannot delete person with id " + id +
                    " because there are dragons that reference this person as their killer.");
        }

        personDao.delete(id);
    }
    @CacheStatistics
    @MethodStatistics
    private Person convertToEntity(PersonDto dto) {
        Person person = new Person();
        person.setId(dto.getId());
        person.setName(dto.getName());
        person.setEyeColor(dto.getEyeColor());
        person.setHairColor(dto.getHairColor());
        person.setLocation(dto.getLocation());
        person.setPassportID(dto.getPassportID());
        person.setNationality(dto.getNationality());
        return person;
    }

    @CacheStatistics
    @MethodStatistics
    public List<PersonDto> findByName(String name) {
        return personDao.findAll().stream()
                .filter(person -> person.getName().toLowerCase().contains(name.toLowerCase()))
                .map(PersonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public List<PersonDto> findByNationality(Country nationality) {
        return personDao.findAll().stream()
                .filter(person -> nationality.equals(person.getNationality()))
                .map(PersonDto::new)
                .collect(Collectors.toList());
    }
    @CacheStatistics
    @MethodStatistics
    public boolean isPassportIdUnique(String passportId) {
        return personDao.findAll().stream()
                .noneMatch(person -> person.getPassportID().equals(passportId));
    }
    @CacheStatistics
    @MethodStatistics
    public boolean isPassportIdUnique(String passportId, Long excludeId) {
        return personDao.findAll().stream()
                .filter(person -> !person.getId().equals(excludeId))
                .noneMatch(person -> person.getPassportID().equals(passportId));
    }
}