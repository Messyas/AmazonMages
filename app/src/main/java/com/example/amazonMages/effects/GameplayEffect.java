package com.example.amazonMages.effects;

import com.example.amazonMages.combat.*;

/** Immutable effect definitions reused by consumables and attacks. Duplicate impacts are consumed by ECS. */
public final class GameplayEffect {
    public enum Kind { DAMAGE, HEAL, MANA, BOOST, SLOW }
    public interface Target {
        boolean damage(DamageEvent hit);
        boolean heal(double amount);
        boolean mana(double amount);
        StatusEffects statuses();
    }
    public final Kind kind;
    public final double amount;
    public final int duration;
    private final DamageType damageType;
    public GameplayEffect(Kind kind, double amount, int duration, DamageType damageType) {
        if (kind==null || !Double.isFinite(amount) || amount<=0 || duration<0 || damageType==null
                || ((kind==Kind.BOOST || kind==Kind.SLOW) && (duration<1 || amount>1))) throw new IllegalArgumentException("effect");
        this.kind=kind; this.amount=amount; this.duration=duration; this.damageType=damageType;
    }
    public boolean apply(Target target) {
        switch(kind) {
            case DAMAGE: return target.damage(new DamageEvent(amount,amount*12,damageType));
            case HEAL: return target.heal(amount);
            case MANA: return target.mana(amount);
            case BOOST:
                if (target.statuses().remainingTicks()>=duration && target.statuses().boostBonus()>=amount) return false;
                target.statuses().grantBoost(duration,amount,amount); return true;
            case SLOW: target.statuses().grantSlow(duration,amount); return true;
            default: return false;
        }
    }
}
