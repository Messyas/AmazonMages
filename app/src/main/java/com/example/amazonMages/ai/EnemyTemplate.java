package com.example.amazonMages.ai;

/** Immutable reusable archetype: species AI + rank + phase thresholds + rewards. */
public final class EnemyTemplate {
    public final String id, displayName, defeatEvent, spriteKey;
    public final EnemyRank rank;
    public final EnemyConfig behavior;
    public final int maxHealth, projectileDamage, meleeDamage, experienceReward, xpTier;
    public final double radius;
    public final boolean flying;
    public final double[] phaseThresholds;

    public EnemyTemplate(String id, String displayName, EnemyRank rank, EnemyConfig behavior,
        int maxHealth, int projectileDamage, double radius, int experienceReward,
        String defeatEvent, double... phaseThresholds) {
        this(id, displayName, rank, behavior, maxHealth, projectileDamage,
            Math.max(1, projectileDamage), radius, experienceReward, defeatEvent,
            "acid_ghost", rank == EnemyRank.NORMAL ? 0 : 2, true, phaseThresholds);
    }

    public EnemyTemplate(String id, String displayName, EnemyRank rank, EnemyConfig behavior,
        int maxHealth, int projectileDamage, int meleeDamage, double radius, int experienceReward,
        String defeatEvent, String spriteKey, int xpTier, boolean flying, double... phaseThresholds) {
        if (id == null || id.isEmpty() || displayName == null || displayName.isEmpty()
            || rank == null || behavior == null || maxHealth < 1 || projectileDamage < 0 || meleeDamage < 0
            || !Double.isFinite(radius) || radius <= 0 || experienceReward < 0
            || defeatEvent == null || defeatEvent.isEmpty())
            throw new IllegalArgumentException("Invalid enemy template");
        double previous = 1;
        for (double threshold : phaseThresholds) {
            if (!Double.isFinite(threshold) || threshold <= 0 || threshold >= previous)
                throw new IllegalArgumentException("Phases must descend within (0,1)");
            previous = threshold;
        }
        this.id = id;
        this.displayName = displayName;
        this.rank = rank;
        this.behavior = behavior;
        this.maxHealth = maxHealth;
        this.projectileDamage = projectileDamage;
        this.meleeDamage = meleeDamage;
        this.radius = radius;
        this.experienceReward = experienceReward;
        this.defeatEvent = defeatEvent;
        this.spriteKey = spriteKey == null || spriteKey.isEmpty() ? "acid_ghost" : spriteKey;
        this.xpTier = Math.max(0, Math.min(2, xpTier));
        this.flying = flying;
        this.phaseThresholds = phaseThresholds.clone();
    }

    /** 0 is initial phase; later phases unlock at descending HP ratios. */
    public int phaseFor(int health) {
        double ratio = Math.max(0, health) / (double) maxHealth;
        int phase = 0;
        for (double threshold : phaseThresholds) if (ratio <= threshold) phase++;
        return phase;
    }

    public boolean isBossEncounter() {
        return rank == EnemyRank.BOSS || rank == EnemyRank.MINI_BOSS;
    }
}
