package com.example.amazonMages.combat;

import com.example.amazonMages.progression.PlayerProgression;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class SurvivorArsenalTest {
    @Test public void autoCastFiresUnlockedWeaponsAndUpgradesScalePower() {
        SurvivorArsenal arsenal = new SurvivorArsenal();
        PlayerProgression prog = new PlayerProgression();
        int[] coconuts = {0}, leaves = {0}, orbs = {0}, lightning = {0};

        SurvivorArsenal.AutoCastSink sink = new SurvivorArsenal.AutoCastSink() {
            @Override public void fireCoconutFan(int count, double dmg) { coconuts[0] += count; }
            @Override public void fireLeafBurst(int count, int pierce, double dmg) { leaves[0] += count; }
            @Override public void pulseOrbDamage(double x, double y, double r, double dmg) { orbs[0]++; }
            @Override public void strikeLightningTarget(double dmg, double area) { lightning[0]++; }
        };

        for (int t = 0; t < 60; t++) arsenal.update(0, 0, true, prog, sink);
        assertTrue(coconuts[0] > 0);
        assertEquals(0, leaves[0]);
        assertEquals(0, orbs[0]);
        assertEquals(0, lightning[0]);

        arsenal.apply(SurvivorArsenal.UpgradeKind.LEAF, prog);
        arsenal.apply(SurvivorArsenal.UpgradeKind.ORBS, prog);
        arsenal.apply(SurvivorArsenal.UpgradeKind.LIGHTNING, prog);
        for (int t = 0; t < 120; t++) arsenal.update(0, 0, true, prog, sink);
        assertTrue(leaves[0] > 0);
        assertTrue(orbs[0] > 0);
        assertTrue(lightning[0] > 0);
        assertEquals(2, arsenal.orbCount());
    }

    @Test public void levelUpChoicesOfferDistinctOptionsAndScaleParryAndMagnet() {
        SurvivorArsenal arsenal = new SurvivorArsenal();
        PlayerProgression prog = new PlayerProgression();
        double baseParryRadius = arsenal.parryRadius();
        double baseMagnet = arsenal.magnetRadius(prog);

        List<SurvivorArsenal.UpgradeOption> choices = arsenal.generateChoices(new Random(7));
        assertEquals(3, choices.size());
        assertNotSame(choices.get(0).kind, choices.get(1).kind);
        assertNotSame(choices.get(1).kind, choices.get(2).kind);

        arsenal.apply(SurvivorArsenal.UpgradeKind.PARRY, prog);
        arsenal.apply(SurvivorArsenal.UpgradeKind.MAGNET, prog);
        assertTrue(arsenal.parryRadius() > baseParryRadius);
        assertTrue(arsenal.magnetRadius(prog) > baseMagnet);
        assertEquals(2, arsenal.apply(SurvivorArsenal.UpgradeKind.VIGOR, prog));
    }

    @Test public void xpCrystalPoolAttractsAndConsolidatesWithoutLosingXp() {
        XpCrystalPool pool = new XpCrystalPool();
        for (int i = 0; i < XpCrystalPool.MAX_GEMS + 10; i++) {
            pool.spawn(200 + i, 0, 10, 0);
        }
        assertEquals(XpCrystalPool.MAX_GEMS, pool.size());

        XpCrystalPool magnetTest = new XpCrystalPool();
        magnetTest.spawn(100, 0, 25, 1);
        int[] collectedXp = {0};
        for (int tick = 0; tick < 30; tick++) {
            magnetTest.updateAndCollect(0, 0, 32, 160, (xp, tier) -> collectedXp[0] += xp);
        }
        assertEquals(25, collectedXp[0]);
        assertEquals(0, magnetTest.size());
    }
}

