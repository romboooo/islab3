package com.example.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class DragonHead {
    @Column(name = "tooth_count")
    private Float toothCount;

    public DragonHead() {}

    public DragonHead(Float toothCount) {
        this.toothCount = toothCount;
    }

    public Float getToothCount() { return toothCount; }
    public void setToothCount(Float toothCount) { this.toothCount = toothCount; }
}