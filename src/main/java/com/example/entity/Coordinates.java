package com.example.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class Coordinates {
    @Column(name = "coordinates_x")
    private long x;

    @NotNull(message = "Y coordinate cannot be null")
    @Column(name = "coordinates_y")
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