package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Location {
    @Column(name = "location_x")
    private long x;

    @Column(name = "location_y")
    private double y;

    @Column(name = "location_z")
    private long z;
    public Location() {}

    public Location(long x, double y, long z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getX() { return x; }
    public void setX(long x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public long getZ() { return z; }
    public void setZ(long z) { this.z = z; }
}