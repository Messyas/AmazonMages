package com.example.amazonMages.combat;

/** Shared posture meter for shield guard, parry recovery, physical and boss interactions. */
public final class Posture {
    private final double baseMaximum;
    private double bonusMaximum;
    private double current;
    private int recoveryDelayTicks;

    public Posture(double maximum) {
        if (!Double.isFinite(maximum) || maximum <= 0) throw new IllegalArgumentException("maximum");
        this.baseMaximum = maximum;
        this.current = maximum;
    }

    public void setBonusMaximum(double bonus) {
        double prevMax = getMaximum();
        bonusMaximum = Double.isFinite(bonus) ? Math.max(0.0, bonus) : 0.0;
        double delta = getMaximum() - prevMax;
        if (delta > 0) current = Math.min(getMaximum(), current + delta);
        else current = Math.min(getMaximum(), current);
    }

    public boolean applyPressure(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return false;
        current = Math.max(0, current - amount);
        recoveryDelayTicks = 120;
        return current == 0;
    }

    public void update() {
        double max = getMaximum();
        if (recoveryDelayTicks > 0) recoveryDelayTicks--;
        else current = Math.min(max, current + max / 180.0);
    }

    public void restore(double amount) {
        if (Double.isFinite(amount) && amount > 0) current = Math.min(getMaximum(), current + amount);
    }

    public double getCurrent() { return current; }
    public double getMaximum() { return baseMaximum + bonusMaximum; }
    public void reset() { current = getMaximum(); recoveryDelayTicks = 0; }
}
