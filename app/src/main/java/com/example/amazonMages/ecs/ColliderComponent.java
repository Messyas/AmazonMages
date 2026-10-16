package com.example.amazonMages.ecs;

/** Circular hitbox independent from Android rendering. */
public final class ColliderComponent implements Component {
    public final double radius;
    public ColliderComponent(double radius) {
        if (!Double.isFinite(radius) || radius <= 0) throw new IllegalArgumentException("radius");
        this.radius = radius;
    }
}
