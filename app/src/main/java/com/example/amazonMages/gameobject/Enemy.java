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
import com.example.amazonMages.ai.EnemyTemplate;
import com.example.amazonMages.ai.EnemyRank;
import com.example.amazonMages.ai.EnemyConfig;
import com.example.amazonMages.ai.EnemyProjectile;
import com.example.amazonMages.ai.EnemyState;
import com.example.amazonMages.ai.EnemyPersonality;
import com.example.amazonMages.graphics.Direction;
import com.example.amazonMages.graphics.EnemySpriteRenderer;
import com.example.amazonMages.graphics.GhostAnimation;
import com.example.amazonMages.graphics.GhostAnimationController;
import com.example.amazonMages.map.WorldDefinition;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.concurrent.atomic.AtomicLong;

/** Enemy entity supporting multiple GBA species, horde knockback/stun, and acid-ghost fusion. */
public class Enemy extends Circle {
    private static final AtomicLong NEXT_UID = new AtomicLong(1);
    public final long uid = NEXT_UID.getAndIncrement();

    private final Player player;
    private final EnemySpriteRenderer spriteRenderer;
    private final GhostAnimationController animation = new GhostAnimationController();
    private boolean hasSeenPlayer;
    private boolean sawAlly;
    private boolean wasRetreating;
    private int deathTicks = -1;
    private int hurtFlashTicks;
    private int stunTicks;
    private double impulseX, impulseY;
    private final EnemyConfig config;
    private EnemyTemplate template;
    private WorldDefinition worldGeometry;
    private int phase;
    private final EnemyBrain brain;
    private int health;
    private double preciseHealth;
    private final CombatStats combatStats = new CombatStats();
    private final Posture posture = new Posture(100);
    private final EnemyPersonality personality;
    private int damage = 1;
    private int meleeDamage = 1;
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
        this.spriteRenderer = new EnemySpriteRenderer(context);
        this.config = config;
        this.personality = personality;
        this.brain = new EnemyBrain(config);
        this.health = config.maxHealth;
        this.preciseHealth = health;
    }

    /** Instantiate a configured rank/species while reusing the species' movement and renderer. */
    public Enemy(Context context, Player player, EnemyTemplate template,
                 double x, double y) {
        this(context, player, template, x, y, EnemyPersonality.AGGRESSIVE);
    }

    public Enemy(Context context, Player player, EnemyTemplate template,
                 double x, double y, EnemyPersonality personality) {
        this(context, player, template.behavior, x, y, personality);
        this.template = template;
        this.health = template.maxHealth;
        this.preciseHealth = template.maxHealth;
        this.damage = template.projectileDamage;
        this.meleeDamage = template.meleeDamage;
        this.radius = template.radius;
    }

    public void setWorldGeometry(WorldDefinition world) { this.worldGeometry = world; }
    public EnemyTemplate getTemplate() { return template; }
    public EnemyRank getRank() { return template == null ? EnemyRank.NORMAL : template.rank; }
    public int getPhase() { return phase; }
    public int getMaximumHealth() { return template == null ? config.maxHealth : template.maxHealth; }
    public double getHealthRatio() { return Math.max(0, health) / (double) getMaximumHealth(); }
    public int getMeleeDamage() { return meleeDamage; }
    public boolean isTargetable() { return !isDead(); }
    public boolean isFlying() { return template == null || template.flying; }
    public boolean isStunned() { return stunTicks > 0; }

    public void applyImpulse(double vx, double vy) {
        if (!Double.isFinite(vx) || !Double.isFinite(vy)) return;
        double cap = 16.0;
        impulseX = Math.max(-cap, Math.min(cap, impulseX + vx));
        impulseY = Math.max(-cap, Math.min(cap, impulseY + vy));
    }

    public void stun(int ticks) {
        if (ticks > stunTicks) stunTicks = ticks;
    }

    public void nudge(double dx, double dy) {
        if (deathTicks >= 0 || !Double.isFinite(dx) || !Double.isFinite(dy)) return;
        moveBy(dx, dy);
    }

    @Override public void update() { update(1); }

    public void update(int elapsedTicks) {
        if (hurtFlashTicks > 0) hurtFlashTicks = Math.max(0, hurtFlashTicks - elapsedTicks);
        if (deathTicks >= 0) {
            deathTicks += elapsedTicks;
            animation.update(elapsedTicks, GhostAnimation.DEATH);
            return;
        }

        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        if (stunTicks > 0) {
            stunTicks = Math.max(0, stunTicks - elapsedTicks);
            velocityX = impulseX * elapsedTicks;
            velocityY = impulseY * elapsedTicks;
            impulseX *= 0.78;
            impulseY *= 0.78;
            moveBy(velocityX, velocityY);
            animation.update(elapsedTicks, GhostAnimation.HURT);
            return;
        }

        EnemyState previousState = brain.getState();
        brain.update(dx, dy, health, elapsedTicks);
        if (template != null) phase = Math.max(phase, template.phaseFor(health));
        velocityX = brain.getMoveX();
        velocityY = brain.getMoveY();
        boolean retreating = personality == EnemyPersonality.CAUTIOUS && health > 1 &&
            health < config.maxHealth && Math.hypot(dx, dy) < 340;
        if (retreating) {
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
        if (template != null && template.isBossEncounter() && phase > 0) {
            double speedBonus = 1 + .12 * phase;
            velocityX *= speedBonus;
            velocityY *= speedBonus;
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

        double moveX = velocityX + impulseX * elapsedTicks;
        double moveY = velocityY + impulseY * elapsedTicks;
        impulseX *= 0.78;
        impulseY *= 0.78;
        moveBy(moveX, moveY);

        facing = Direction.fromVector(velocityX, velocityY, facing);
        attackFacing = Direction.fromVector(dx, dy, attackFacing);
    }

    private void moveBy(double dx, double dy) {
        if (worldGeometry != null && !isFlying()) {
            WorldDefinition.Position resolved = worldGeometry.resolveMovement(
                positionX, positionY, dx, dy, Math.min(26, radius));
            if (resolved.x == positionX && resolved.y == positionY && (dx != 0 || dy != 0)
                    && !worldGeometry.isWalkable(positionX, positionY, Math.min(26, radius))) {
                positionX += dx;
                positionY += dy;
            } else {
                positionX = resolved.x;
                positionY = resolved.y;
            }
        } else {
            positionX += dx;
            positionY += dy;
        }
    }

    public EnemyProjectile fireProjectile() {
        if (deathTicks >= 0 || stunTicks > 0 || damage <= 0 || config.attackRadius <= 0 || !brain.shouldFire()) {
            return null;
        }
        animation.react(GhostAnimation.ATTACK);
        brain.onShotFired(health);
        return new EnemyProjectile(positionX, positionY,
            brain.getShotX(), brain.getShotY(), config.projectileSpeed,
            template != null && template.isBossEncounter() ? damage + phase : damage);
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
        meleeDamage = 2;
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
        preciseHealth = Math.max(minimumHealthAfterHit(),
            preciseHealth - DamageResolver.resolve(hit, combatStats, null));
        health = (int) Math.ceil(preciseHealth);
        hurtFlashTicks = 6;
        if (template != null) phase = Math.max(phase, template.phaseFor(health));
        posture.applyPressure(hit.postureDamage);
        if (health == 0) {
            deathTicks = 0;
            animation.react(GhostAnimation.DEATH);
        } else animation.react(GhostAnimation.HURT);
    }

    protected double minimumHealthAfterHit() { return 0; }

    protected void restoreFullHealth() {
        preciseHealth = getMaximumHealth();
        health = getMaximumHealth();
        phase = 0;
        deathTicks = -1;
        hurtFlashTicks = 0;
        stunTicks = 0;
        posture.restore(100);
    }

    public boolean finishedDeath() {
        if (deathTicks < 0) return false;
        if (template != null && !"acid_ghost".equals(template.spriteKey)) {
            return deathTicks >= 16;
        }
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
        String key = template == null ? "acid_ghost" : template.spriteKey;
        spriteRenderer.draw(canvas, display, key, positionX, positionY,
            facing, animation.getCurrent(), fused, animation.getTicks(), hurtFlashTicks, stunTicks);
    }
}
