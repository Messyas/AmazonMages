package com.example.amazonMages.ai;

import java.util.Random;

/** Distance-aware finite state machine; outputs movement and optional projectile aim. */
public final class EnemyBrain {
    private final EnemyConfig config;
    private final Random random = new Random();
    private EnemyState state = EnemyState.IDLE;
    private int alertRemaining;
    private int cooldown;
    private double moveX, moveY, shotX, shotY;
    private boolean fire;

    public EnemyBrain(EnemyConfig config) { this.config = config; }

    public void update(double dx, double dy, int health) {
        fire = false;
        moveX = moveY = 0;
        if (cooldown > 0) cooldown--;
        double distance = Math.hypot(dx, dy);
        boolean enraged = health > 0 && health * 3 <= config.maxHealth;
        if (distance > config.noticeRadius) {
            state = EnemyState.IDLE;
            alertRemaining = 0;
            return;
        }
        if (state == EnemyState.IDLE) {
            state = EnemyState.ALERT;
            alertRemaining = config.alertTicks;
        }
        if (alertRemaining > 0) {
            alertRemaining--;
            state = EnemyState.ALERT;
            return;
        }
        state = enraged ? EnemyState.ENRAGED :
            distance <= config.attackRadius ? EnemyState.ATTACK : EnemyState.CHASE;
        if (distance > 0) {
            double ux = dx / distance, uy = dy / distance;
            double speed = config.speed / 60.0 * (enraged ? 1.4 : 1);
            if (distance > config.preferredDistance) {
                moveX = ux * speed;
                moveY = uy * speed;
            } else if (distance < config.retreatRadius) {
                moveX = -ux * speed * 0.65;
                moveY = -uy * speed * 0.65;
            }
            if (distance <= config.attackRadius && cooldown == 0) {
                // One aim decision at firing time. Projectile never homes.
                double error = (random.nextDouble() * 2 - 1) *
                    config.aimErrorRadians * (enraged ? 0.75 : 1);
                double angle = Math.atan2(dy, dx) + error;
                shotX = Math.cos(angle);
                shotY = Math.sin(angle);
                fire = true;
                cooldown = enraged ? config.enragedCooldown : config.attackCooldown;
            }
        }
    }

    public EnemyState getState() { return state; }
    public double getMoveX() { return moveX; }
    public double getMoveY() { return moveY; }
    public boolean shouldFire() { return fire; }
    public double getShotX() { return shotX; }
    public double getShotY() { return shotY; }
}
