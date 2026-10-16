package com.example.amazonMages.combat;

/** Derived combat values. Defaults preserve current gameplay until progression is wired in. */
public final class CombatStats {
    private double physicalResistance;
    private double magicResistance;
    private double acidResistance;
    private double outgoingPhysicalMultiplier = 1.0;
    private double outgoingMagicMultiplier = 1.0;
    private double movementMultiplier = 1.0;

    public double resistance(DamageType type) {
        switch (type) {
            case PHYSICAL: return physicalResistance;
            case ACID: return acidResistance;
            default: return magicResistance;
        }
    }
    public void setResistance(DamageType type, double value) {
        double safe = clamp(value, 0, 0.8);
        switch (type) {
            case PHYSICAL: physicalResistance = safe; break;
            case ACID: acidResistance = safe; break;
            default: magicResistance = safe;
        }
    }
    public double outgoingMultiplier(DamageType type) {
        return type == DamageType.PHYSICAL ? outgoingPhysicalMultiplier : outgoingMagicMultiplier;
    }
    public void setOutgoingMultiplier(DamageType type, double value) {
        double safe = clamp(value, 0, 10);
        if (type == DamageType.PHYSICAL) outgoingPhysicalMultiplier = safe;
        else outgoingMagicMultiplier = safe;
    }
    public double getMovementMultiplier() { return movementMultiplier; }
    public void setMovementMultiplier(double value) { movementMultiplier = clamp(value, 0.1, 3); }
    private static double clamp(double v, double min, double max) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite stat");
        return Math.max(min, Math.min(max, v));
    }
}
