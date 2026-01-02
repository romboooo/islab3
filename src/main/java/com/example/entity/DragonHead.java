package com.example.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class DragonHead {
    private Float toothCount;

    public DragonHead() {}

    public DragonHead(Float toothCount) {
        this.toothCount = toothCount;
    }

    public Float getToothCount() { return toothCount; }
    public void setToothCount(Float toothCount) { this.toothCount = toothCount; }
}