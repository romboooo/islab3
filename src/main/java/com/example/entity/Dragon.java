package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dragon")
public class Dragon {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dragon_seq")
    @SequenceGenerator(name = "dragon_seq", sequenceName = "dragon_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Name cannot be null or empty")
    @Column(nullable = false, unique = true)
    private String name;

    @Embedded
    @NotNull(message = "Coordinates cannot be null")
    @Valid
    private Coordinates coordinates;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Embedded
    @Valid
    private DragonCave cave;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "killer_id")
    private Person killer;

    @Min(value = 1, message = "Age must be greater than 0")
    @Column(nullable = false)
    private long age;

    @NotNull(message = "Weight cannot be null")
    @DecimalMin(value = "0.000001", inclusive = false, message = "Weight must be greater than 0")
    @Column(nullable = false)
    private Float weight;

    @Enumerated(EnumType.STRING)
    private Color color;

    @Enumerated(EnumType.STRING)
    @Column(name = "dragon_character")
    private DragonCharacter character;

    @Embedded
    @Valid
    private DragonHead head;

    public Dragon() {}

    public Dragon(String name, Coordinates coordinates, long age, Float weight) {
        this.name = name;
        this.coordinates = coordinates;
        this.age = age;
        this.weight = weight;
        this.creationDate = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    // Геттеры и сеттеры
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

    public long getAge() { return age; }
    public void setAge(long age) { this.age = age; }

    public Float getWeight() { return weight; }
    public void setWeight(Float weight) { this.weight = weight; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public DragonCharacter getCharacter() { return character; }
    public void setCharacter(DragonCharacter character) { this.character = character; }

    public DragonHead getHead() { return head; }
    public void setHead(DragonHead head) { this.head = head; }
}