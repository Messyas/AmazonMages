package com.example.amazonMages.combat;

/** Immutable hit description; posture pressure is independent from HP damage. */
public final class DamageEvent {
    public final double amount;
    public final double postureDamage;
    public final DamageType type;

    public DamageEvent(double amount, double postureDamage, DamageType type) {
        if (!Double.isFinite(amount) || amount < 0 || !Double.isFinite(postureDamage) || postureDamage < 0 || type == null)
            throw new IllegalArgumentException("Invalid damage event");
        this.amount = amount;
        this.postureDamage = postureDamage;
        this.type = type;
    }
}
