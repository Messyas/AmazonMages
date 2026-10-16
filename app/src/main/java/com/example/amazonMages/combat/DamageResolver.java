package com.example.amazonMages.combat;

/** Central damage math, usable by enemies, spells and future equipment. */
public final class DamageResolver {
    private DamageResolver() {}

    public static double resolve(DamageEvent hit, CombatStats defender, StatusEffects effects) {
        double resistance = defender == null ? 0 : defender.resistance(hit.type);
        double temporary = effects == null ? 0 : effects.damageReduction();
        // Multiplicative stacking prevents full immunity from modest bonuses.
        return Math.max(0, hit.amount * (1 - resistance) * (1 - temporary));
    }
}
