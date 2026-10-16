package com.example.amazonMages.combat;

/** Tick-based temporary combat bonuses; no wall-clock timing dependencies. */
public final class StatusEffects {
    private int boostTicks;
    private int slowTicks;
    private double slow;
    private double speedBonus;
    private double damageReduction;

    public void grantBoost(int ticks, double speedBonus, double damageReduction) {
        if (ticks <= 0 || !Double.isFinite(speedBonus) || !Double.isFinite(damageReduction)) return;
        boostTicks = Math.max(boostTicks, ticks);
        this.speedBonus = Math.max(this.speedBonus, Math.max(0, Math.min(1, speedBonus)));
        this.damageReduction = Math.max(this.damageReduction, Math.max(0, Math.min(0.8, damageReduction)));
    }
    public void grantSlow(int ticks, double amount) {
        if (ticks<=0 || !Double.isFinite(amount)) return;
        slowTicks=Math.max(slowTicks,ticks); slow=Math.max(slow,Math.max(0,Math.min(.8,amount)));
    }
    public void update() {
        if (slowTicks>0 && --slowTicks==0) slow=0;
        if (boostTicks > 0 && --boostTicks == 0) { speedBonus = 0; damageReduction = 0; }
    }
    public double speedMultiplier() { return (1 + speedBonus)*(1-slow); }
    public double damageReduction() { return damageReduction; }
    public double boostBonus() { return speedBonus; }
    public int remainingTicks() { return boostTicks; }
}
