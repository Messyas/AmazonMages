package com.example.amazonMages.ai;

/** Tunable behavior profile for each enemy species, in world pixels and game ticks. */
public final class EnemyConfig {
    public final double noticeRadius, attackRadius, preferredDistance, retreatRadius;
    public final double speed, projectileSpeed, aimErrorRadians;
    public final int maxHealth, alertTicks, attackCooldown, enragedCooldown;

    public EnemyConfig(double noticeRadius, double attackRadius, double preferredDistance,
                       double retreatRadius, double speed, double projectileSpeed,
                       double aimErrorRadians, int maxHealth, int alertTicks,
                       int attackCooldown, int enragedCooldown) {
        this.noticeRadius = noticeRadius;
        this.attackRadius = attackRadius;
        this.preferredDistance = preferredDistance;
        this.retreatRadius = retreatRadius;
        this.speed = speed;
        this.projectileSpeed = projectileSpeed;
        this.aimErrorRadians = aimErrorRadians;
        this.maxHealth = maxHealth;
        this.alertTicks = alertTicks;
        this.attackCooldown = attackCooldown;
        this.enragedCooldown = enragedCooldown;
    }

    public static EnemyConfig acidGhost() {
        return new EnemyConfig(650, 470, 270, 145, 140, 350,
            Math.toRadians(12), 4, 45, 110, 65);
    }
}
