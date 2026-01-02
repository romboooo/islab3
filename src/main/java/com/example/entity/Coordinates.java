package com.example.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class Coordinates {
    private long x;

    @NotNull(message = "Y coordinate cannot be null")
    private double y;

    public Coordinates() {}

    public Coordinates(long x, double y) {
        this.x = x;
        this.y = y;
    }

    public long getX() { return x; }
    public void setX(long x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}