package com.example.amazonMages.ai;

import com.example.amazonMages.map.WorldDefinition;
import java.util.Random;

/**
 * Vampire-Survivors-style wave pacing director: scales spawn cadence, enemy cap,
 * biome-weighted enemy variety, periodic horde surges, and timed boss arrivals.
 */
public final class WaveDirector {
    public static final int MAX_CAP = 100;
    private static final int SURGE_PERIOD_TICKS = 2700; // 45s
    private static final int SURGE_DURATION_TICKS = 300; // 5s
    private static final int SENTINEL_TIME_TICKS = 5400; // 1:30
    private static final int WRAITH_TIME_TICKS = 10800; // 3:00

    private int survivalTicks;
    private int spawnCooldown;
    private int surgeTicksRemaining;
    private boolean sentinelSpawned;
    private boolean wraithSpawned;
    private String pendingAnnouncement;

    public int survivalTicks() { return survivalTicks; }
    public int waveNumber() { return 1 + survivalTicks / SURGE_PERIOD_TICKS; }
    public boolean isSurgeActive() { return surgeTicksRemaining > 0; }

    public int targetCap() {
        int seconds = survivalTicks / 60;
        if (seconds < 30) return 28;
        if (seconds < 60) return 42;
        if (seconds < 120) return 60;
        if (seconds < 180) return 80;
        return MAX_CAP;
    }

    public int spawnIntervalTicks() {
        int seconds = survivalTicks / 60;
        int base = seconds < 25 ? 42 : seconds < 75 ? 28 : seconds < 135 ? 18 : 12;
        return surgeTicksRemaining > 0 ? Math.max(7, base / 2) : base;
    }

    /** Returns how many enemies to attempt spawning this tick (0 when waiting). */
    public int step(int currentEnemies) {
        survivalTicks++;
        if (survivalTicks > 60 && survivalTicks % SURGE_PERIOD_TICKS == 0) {
            surgeTicksRemaining = SURGE_DURATION_TICKS;
            pendingAnnouncement = "Horda da Floresta! Onda " + waveNumber();
        }
        if (surgeTicksRemaining > 0) surgeTicksRemaining--;

        if (spawnCooldown > 0) {
            spawnCooldown--;
            return 0;
        }
        if (currentEnemies >= targetCap()) return 0;
        spawnCooldown = spawnIntervalTicks();
        if (surgeTicksRemaining > 0) return 3;
        return survivalTicks < 1800 ? 1 : 2;
    }

    /** Picks a species weighted by survival progression and current biome. */
    public EnemyTemplate pickAmbient(WorldDefinition.Region region, Random random) {
        int seconds = survivalTicks / 60;
        double roll = random.nextDouble();
        if (seconds < 20) {
            return roll < 0.65 ? EnemyTemplates.SWARM_BAT : EnemyTemplates.ACID_GHOST;
        }
        if (seconds < 55) {
            if (region == WorldDefinition.Region.INDIGENOUS && roll < 0.45) return EnemyTemplates.CURUPIRA_IMP;
            if (region == WorldDefinition.Region.DEAD_FOREST && roll < 0.45) return EnemyTemplates.ACID_GHOST;
            if (roll < 0.40) return EnemyTemplates.SWARM_BAT;
            if (roll < 0.68) return EnemyTemplates.ACID_GHOST;
            if (roll < 0.88) return EnemyTemplates.CURUPIRA_IMP;
            return EnemyTemplates.VINE_SHAMAN;
        }
        if ((region == WorldDefinition.Region.ANCIENT || region == WorldDefinition.Region.MODERN) && roll < 0.28) {
            return EnemyTemplates.MOSS_GOLEM;
        }
        if (region == WorldDefinition.Region.DEAD_FOREST && roll < 0.30) {
            return EnemyTemplates.VINE_SHAMAN;
        }
        if (roll < 0.30) return EnemyTemplates.SWARM_BAT;
        if (roll < 0.50) return EnemyTemplates.ACID_GHOST;
        if (roll < 0.72) return EnemyTemplates.CURUPIRA_IMP;
        if (roll < 0.86) return EnemyTemplates.VINE_SHAMAN;
        return EnemyTemplates.MOSS_GOLEM;
    }

    public EnemyTemplate checkTimedBoss() {
        if (!sentinelSpawned && survivalTicks >= SENTINEL_TIME_TICKS) {
            sentinelSpawned = true;
            pendingAnnouncement = "Sentinela do Pantano surgiu!";
            return EnemyTemplates.SWAMP_SENTINEL;
        }
        if (!wraithSpawned && survivalTicks >= WRAITH_TIME_TICKS) {
            wraithSpawned = true;
            pendingAnnouncement = "Espectro Ancestral despertou!";
            return EnemyTemplates.ANCIENT_WRAITH;
        }
        return null;
    }

    public String pollAnnouncement() {
        String msg = pendingAnnouncement;
        pendingAnnouncement = null;
        return msg;
    }

    public void resetTimer() {
        survivalTicks = 0;
        spawnCooldown = 15;
        surgeTicksRemaining = 0;
    }
}

