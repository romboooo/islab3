package com.example.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;

@Embeddable
public class DragonCave {
    @Min(value = 1, message = "Number of treasures must be greater than 0")
    private long numberOfTreasures;

    public DragonCave() {}

    public DragonCave(long numberOfTreasures) {
        this.numberOfTreasures = numberOfTreasures;
    }

    public long getNumberOfTreasures() { return numberOfTreasures; }
    public void setNumberOfTreasures(long numberOfTreasures) {
        this.numberOfTreasures = numberOfTreasures;
    }
}