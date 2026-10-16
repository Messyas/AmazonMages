package com.example.amazonMages.combat;

/** Shared posture meter for future shield, physical and boss interactions. */
public final class Posture {
    private final double maximum;
    private double current;
    private int recoveryDelayTicks;

    public Posture(double maximum) {
        if (!Double.isFinite(maximum) || maximum <= 0) throw new IllegalArgumentException("maximum");
        this.maximum = maximum;
        current = maximum;
    }
    public boolean applyPressure(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return false;
        current = Math.max(0, current - amount);
        recoveryDelayTicks = 120;
        return current == 0;
    }
    public void update() {
        if (recoveryDelayTicks > 0) recoveryDelayTicks--;
        else current = Math.min(maximum, current + maximum / 180.0);
    }
    public void restore(double amount) {
        if (Double.isFinite(amount) && amount > 0) current = Math.min(maximum, current + amount);
    }
    public double getCurrent() { return current; }
    public double getMaximum() { return maximum; }
    public void reset() { current = maximum; recoveryDelayTicks = 0; }
}
