package com.example.amazonMages.ecs;

public final class TransformComponent implements Component {
    public double x, y;
    public TransformComponent(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("position");
        this.x = x;
        this.y = y;
    }
}
