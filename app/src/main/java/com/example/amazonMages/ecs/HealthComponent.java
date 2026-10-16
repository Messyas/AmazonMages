package com.example.amazonMages.ecs;

public final class HealthComponent implements Component {
    public final double maximum;
    private double current;
    public HealthComponent(double maximum) {
        if (!Double.isFinite(maximum) || maximum <= 0) throw new IllegalArgumentException("maximum");
        this.maximum = maximum;
        this.current = maximum;
    }
    public double current() { return current; }
    public boolean alive() { return current > 0; }
    public void damage(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("damage");
        current = Math.max(0, current - amount);
    }
    public void heal(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("heal");
        current = Math.min(maximum, current + amount);
    }
}
