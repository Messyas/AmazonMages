package com.example.amazonMages.gameobject;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.combat.CombatStats;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageResolver;
import com.example.amazonMages.combat.Posture;
import com.example.amazonMages.ai.EnemyBrain;
import com.example.amazonMages.ai.EnemyConfig;
import com.example.amazonMages.ai.EnemyProjectile;
import com.example.amazonMages.ai.EnemyState;
import com.example.amazonMages.ai.EnemyPersonality;
import com.example.amazonMages.graphics.Direction;
import com.example.amazonMages.graphics.GhostAtlasRenderer;
import com.example.amazonMages.graphics.GhostAnimation;
import com.example.amazonMages.graphics.GhostAnimationController;
import com.example.androidstudio2dgamedevelopment.R;

/** Acid ghost entity: visuals, health and a reusable behavior controller. */
public class Enemy extends Circle {

    private final Player player;
    private final GhostAtlasRenderer spriteRenderer;
    private final GhostAnimationController animation = new GhostAnimationController();
    private boolean hasSeenPlayer;
    private boolean sawAlly;
    private boolean wasRetreating;
    private int deathTicks = -1;
    private final EnemyConfig config;
    private final EnemyBrain brain;
    private int health;
    private double preciseHealth;
    private final CombatStats combatStats = new CombatStats();
    private final Posture posture = new Posture(100);
    private final EnemyPersonality personality;
    private int damage = 1;
    private boolean fused;
    private double groupTargetX = Double.NaN, groupTargetY = Double.NaN;
    private final int updateOffset = (int) (Math.random() * 60);
    private Direction facing = Direction.SOUTH;
    private Direction attackFacing = Direction.SOUTH;

    public Enemy(Context context, Player player) {
        this(context, player, EnemyConfig.acidGhost());
    }

    public Enemy(Context context, Player player, EnemyConfig config) {
        this(context, player, config, player.getPositionX() + (Math.random() - 0.5) * 1100,
            player.getPositionY() + (Math.random() - 0.5) * 1100,
            EnemyPersonality.random(new java.util.Random()));
    }

    public Enemy(Context context, Player player, EnemyConfig config,
                 double spawnX, double spawnY, EnemyPersonality personality) {
        super(getColor(context, R.color.enemy), spawnX, spawnY, 30);
        this.player = player;
        this.spriteRenderer = new GhostAtlasRenderer(context);
        this.config = config;
        this.personality = personality;
        this.brain = new EnemyBrain(config);
        this.health = config.maxHealth;
        this.preciseHealth = health;
    }

    @Override public void update() { update(1); }

    public void update(int elapsedTicks) {
        if (deathTicks >= 0) {
            deathTicks += elapsedTicks;
            animation.update(elapsedTicks, GhostAnimation.DEATH);
            return;
        }
        EnemyState previousState = brain.getState();
        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        brain.update(dx, dy, health, elapsedTicks);
        velocityX = brain.getMoveX();
        velocityY = brain.getMoveY();
        boolean retreating = personality == EnemyPersonality.CAUTIOUS && health > 1 &&
            health < config.maxHealth && Math.hypot(dx, dy) < 380;
        if (retreating) {
            // Wounded cautious ghosts withdraw before their final enraged phase.
            double distance = Math.hypot(dx, dy);
            if (distance > 0) {
                velocityX = -dx / distance * config.speed / 60.0 * elapsedTicks;
                velocityY = -dy / distance * config.speed / 60.0 * elapsedTicks;
            }
        } else if (personality == EnemyPersonality.SOCIAL &&
                   Double.isFinite(groupTargetX) && Double.isFinite(groupTargetY) && !fused) {
            double gx = groupTargetX - positionX, gy = groupTargetY - positionY;
            double distance = Math.hypot(gx, gy);
            if (distance > 0) {
                velocityX = gx / distance * config.speed / 60.0 * elapsedTicks;
                velocityY = gy / distance * config.speed / 60.0 * elapsedTicks;
            }
        } else if (personality == EnemyPersonality.AGGRESSIVE &&
                   brain.getState() != EnemyState.IDLE && brain.getState() != EnemyState.ALERT) {
            double distance = Math.hypot(dx, dy);
            if (distance > 0) {
                velocityX = dx / distance * config.speed / 60.0 * elapsedTicks;
                velocityY = dy / distance * config.speed / 60.0 * elapsedTicks;
            }
        }
        if (previousState == EnemyState.IDLE && brain.getState() == EnemyState.ALERT) {
            animation.react(hasSeenPlayer ? GhostAnimation.STARTLED : GhostAnimation.SPOTTED_PLAYER);
            hasSeenPlayer = true;
        }
        if (retreating && !wasRetreating) animation.react(GhostAnimation.STARTLED);
        wasRetreating = retreating;
        GhostAnimation ambient = brain.getState() == EnemyState.IDLE ? GhostAnimation.IDLE :
            brain.getState() == EnemyState.ALERT ? GhostAnimation.ALERT :
            retreating ? GhostAnimation.FLEE :
            brain.getState() == EnemyState.ENRAGED ? GhostAnimation.ENRAGED :
            personality == EnemyPersonality.SOCIAL && Double.isFinite(groupTargetX) ? GhostAnimation.SOCIAL :
            brain.getState() == EnemyState.CHASE ? GhostAnimation.CHASE :
            Math.hypot(velocityX, velocityY) > 0 ? GhostAnimation.MOVE : GhostAnimation.IDLE;
        animation.update(elapsedTicks, ambient);
        posture.update();
        positionX += velocityX;
        positionY += velocityY;
        facing = Direction.fromVector(velocityX, velocityY, facing);
        attackFacing = Direction.fromVector(dx, dy, attackFacing);
    }

    public EnemyProjectile fireProjectile() {
        if (deathTicks >= 0 || !brain.shouldFire()) return null;
        animation.react(GhostAnimation.ATTACK);
        brain.onShotFired(health);
        return new EnemyProjectile(positionX, positionY,
            brain.getShotX(), brain.getShotY(), config.projectileSpeed, damage);
    }

    public void setGroupTarget(Enemy other) {
        if (other != null && !sawAlly) {
            animation.react(GhostAnimation.SPOTTED_ALLY);
            sawAlly = true;
        }
        if (other == null) sawAlly = false;
        groupTargetX = other == null ? Double.NaN : other.getPositionX();
        groupTargetY = other == null ? Double.NaN : other.getPositionY();
    }

    /** Only one fusion per resulting mob; prevents exponential health/damage stacking. */
    public void fuseWith(Enemy other) {
        health += other.health;
        preciseHealth += other.preciseHealth;
        damage = 2;
        fused = true;
        radius = 42;
        animation.react(GhostAnimation.FUSION);
    }

    public CombatStats getCombatStats() { return combatStats; }
    public Posture getPosture() { return posture; }
    public boolean isFused() { return fused; }
    public int getDamage() { return damage; }
    public EnemyPersonality getPersonality() { return personality; }

    public void takeDamage(int amount) {
        receiveHit(new DamageEvent(Math.max(0, amount), 10, com.example.amazonMages.combat.DamageType.MAGIC));
    }

    public void receiveHit(DamageEvent hit) {
        if (deathTicks >= 0 || hit.amount <= 0) return;
        preciseHealth = Math.max(0, preciseHealth - DamageResolver.resolve(hit, combatStats, null));
        health = (int) Math.ceil(preciseHealth);
        posture.applyPressure(hit.postureDamage);
        if (health == 0) {
            deathTicks = 0;
            animation.react(GhostAnimation.DEATH);
        } else animation.react(GhostAnimation.HURT);
    }
    public boolean finishedDeath() {
        return deathTicks >= GhostAnimation.DEATH.durationTicks();
    }
    public boolean isDead() { return health <= 0; }
    public int getHealth() { return health; }
    public int getUpdateOffset() { return updateOffset; }
    public EnemyState getAiState() { return brain.getState(); }
    public Direction getFacing() { return facing; }
    public Direction getAttackFacing() { return attackFacing; }
    public void aim(double x, double y) {
        attackFacing = Direction.fromVector(x, y, attackFacing);
    }

    @Override public void draw(Canvas canvas, GameDisplay display) {
        spriteRenderer.draw(canvas, display, positionX, positionY,
            facing, animation.getCurrent(), fused, animation.getTicks());
    }
}
