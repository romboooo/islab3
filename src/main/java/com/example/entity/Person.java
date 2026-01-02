package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq")
    @SequenceGenerator(name = "person_seq", sequenceName = "person_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Name cannot be null or empty")
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color")
    private Color eyeColor;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Hair color cannot be null")
    @Column(name = "hair_color", nullable = false)
    private Color hairColor;

    @Embedded
    @NotNull(message = "Location cannot be null")
    private Location location;

    @NotBlank(message = "Passport ID cannot be null or empty")
    @Size(min = 8, message = "Passport ID must be at least 8 characters long")
    @Column(name = "passport_id", nullable = false, unique = true)
    private String passportID;

    @Enumerated(EnumType.STRING)
    private Country nationality;

    public Person() {}

    public Person(String name, Color hairColor, Location location, String passportID) {
        this.name = name;
        this.hairColor = hairColor;
        this.location = location;
        this.passportID = passportID;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Color getEyeColor() { return eyeColor; }
    public void setEyeColor(Color eyeColor) { this.eyeColor = eyeColor; }

    public Color getHairColor() { return hairColor; }
    public void setHairColor(Color hairColor) { this.hairColor = hairColor; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getPassportID() { return passportID; }
    public void setPassportID(String passportID) { this.passportID = passportID; }

    public Country getNationality() { return nationality; }
    public void setNationality(Country nationality) { this.nationality = nationality; }
}