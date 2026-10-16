package com.example.amazonMages.ecs;

public final class VelocityComponent implements Component {
    public double x, y;
    public VelocityComponent(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("velocity");
        this.x = x;
        this.y = y;
    }
}
