package com.example.dto;

import com.example.entity.*;
import jakarta.validation.constraints.*;

public class PersonDto {
    private Long id;

    @NotBlank(message = "Name cannot be null or empty")
    private String name;

    private Color eyeColor;

    @NotNull(message = "Hair color cannot be null")
    private Color hairColor;

    @NotNull(message = "Location cannot be null")
    private Location location;

    @NotBlank(message = "Passport ID cannot be null or empty")
    @Size(min = 8, message = "Passport ID must be at least 8 characters long")
    private String passportID;

    private Country nationality;

    public PersonDto() {}

    public PersonDto(Person person) {
        this.id = person.getId();
        this.name = person.getName();
        this.eyeColor = person.getEyeColor();
        this.hairColor = person.getHairColor();
        this.location = person.getLocation();
        this.passportID = person.getPassportID();
        this.nationality = person.getNationality();
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