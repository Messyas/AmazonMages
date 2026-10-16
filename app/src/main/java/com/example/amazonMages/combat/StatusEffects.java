package com.example.amazonMages.combat;

/** Tick-based temporary combat bonuses; no wall-clock timing dependencies. */
public final class StatusEffects {
    private int boostTicks;
    private double speedBonus;
    private double damageReduction;

    public void grantBoost(int ticks, double speedBonus, double damageReduction) {
        if (ticks <= 0 || !Double.isFinite(speedBonus) || !Double.isFinite(damageReduction)) return;
        boostTicks = Math.max(boostTicks, ticks);
        this.speedBonus = Math.max(this.speedBonus, Math.max(0, Math.min(1, speedBonus)));
        this.damageReduction = Math.max(this.damageReduction, Math.max(0, Math.min(0.8, damageReduction)));
    }
    public void update() {
        if (boostTicks > 0 && --boostTicks == 0) { speedBonus = 0; damageReduction = 0; }
    }
    public double speedMultiplier() { return 1 + speedBonus; }
    public double damageReduction() { return damageReduction; }
    public int remainingTicks() { return boostTicks; }
}
