package com.example.dto;

import com.example.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class DragonDto {
    private Long id;

    @NotBlank(message = "Name cannot be null or empty")
    private String name;

    @NotNull(message = "Coordinates cannot be null")
    @Valid
    private Coordinates coordinates;

    private LocalDateTime creationDate;

    @Valid
    private DragonCave cave;

    @Valid
    private Person killer;

    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 2000, message = "Age must be at most 2000")
    private int age;

    @NotNull(message = "Weight cannot be null")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1")
    private Float weight;

    private Color color;
    private DragonCharacter character;

    @Valid
    private DragonHead head;

    public DragonDto() {}

    public DragonDto(Dragon dragon) {
        this.id = dragon.getId();
        this.name = dragon.getName();
        this.coordinates = dragon.getCoordinates();
        this.creationDate = dragon.getCreationDate();
        this.cave = dragon.getCave();
        this.killer = dragon.getKiller();
        this.age = (int)dragon.getAge();
        this.weight = dragon.getWeight();
        this.color = dragon.getColor();
        this.character = dragon.getCharacter();
        this.head = dragon.getHead();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public DragonCave getCave() { return cave; }
    public void setCave(DragonCave cave) { this.cave = cave; }

    public Person getKiller() { return killer; }
    public void setKiller(Person killer) { this.killer = killer; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Float getWeight() { return weight; }
    public void setWeight(Float weight) { this.weight = weight; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public DragonCharacter getCharacter() { return character; }
    public void setCharacter(DragonCharacter character) { this.character = character; }

    public DragonHead getHead() { return head; }
    public void setHead(DragonHead head) { this.head = head; }
}