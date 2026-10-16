package com.example.amazonMages.combat;

import org.junit.Test;
import static org.junit.Assert.*;

public class DamageResolverTest {
    @Test public void resistanceAndTemporaryReductionPreserveFractionalDamage() {
        CombatStats stats = new CombatStats();
        stats.setResistance(DamageType.ACID, .25);
        StatusEffects effects = new StatusEffects();
        effects.grantBoost(2, .5, .2);
        DamageEvent hit = new DamageEvent(1, 10, DamageType.ACID);
        assertEquals(.6, DamageResolver.resolve(hit, stats, effects), .00001);
        assertEquals(1.5, effects.speedMultiplier(), 0);
        effects.update();
        assertEquals(.6, DamageResolver.resolve(hit, stats, effects), .00001);
        effects.update();
        assertEquals(.75, DamageResolver.resolve(hit, stats, effects), .00001);
        assertEquals(1, effects.speedMultiplier(), 0);
        assertEquals(0, effects.remainingTicks());
    }

    @Test public void physicalResistanceDoesNotReduceMagicDamage() {
        CombatStats stats = new CombatStats();
        stats.setResistance(DamageType.PHYSICAL, .8);
        assertEquals(2, DamageResolver.resolve(new DamageEvent(10, 0, DamageType.PHYSICAL), stats, null), .00001);
        assertEquals(10, DamageResolver.resolve(new DamageEvent(10, 0, DamageType.MAGIC), stats, null), 0);
    }
}
