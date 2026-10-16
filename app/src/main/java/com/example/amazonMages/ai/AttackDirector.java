package com.example.amazonMages.ai;

/** Shared attack budget: no more than one hostile shot per 24 game ticks. */
public final class AttackDirector {
    private static final int MIN_TICKS_BETWEEN_SHOTS = 24;
    private static final int MAX_PROJECTILES = 14;
    private int nextAllowedTick;
    private int cursor;

    public void beginTick(int tick) {
        if (tick == Integer.MAX_VALUE) { nextAllowedTick = 0; cursor = 0; }
    }

    public int firstCandidate(int enemyCount) {
        return enemyCount == 0 ? 0 : cursor % enemyCount;
    }

    public boolean canFire(int tick, int activeProjectiles) {
        return tick >= nextAllowedTick && activeProjectiles < MAX_PROJECTILES;
    }

    public void recordShot(int tick, int enemyIndex, int enemyCount) {
        nextAllowedTick = tick + MIN_TICKS_BETWEEN_SHOTS;
        cursor = enemyCount == 0 ? 0 : (enemyIndex + 1) % enemyCount;
    }

    public int getMaxProjectiles() { return MAX_PROJECTILES; }
}
