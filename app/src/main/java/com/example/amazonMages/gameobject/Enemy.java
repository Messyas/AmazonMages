package com.example.amazonMages.gameobject;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.GameLoop;
import com.example.amazonMages.ai.EnemyBrain;
import com.example.amazonMages.ai.EnemyConfig;
import com.example.amazonMages.ai.EnemyProjectile;
import com.example.amazonMages.ai.EnemyState;
import com.example.amazonMages.ai.EnemyPersonality;
import com.example.amazonMages.graphics.Direction;
import com.example.androidstudio2dgamedevelopment.R;

/** Acid ghost entity: visuals, health and a reusable behavior controller. */
public class Enemy extends Circle {
    private static final double SPAWNS_PER_MINUTE = 20;
    private static final double UPDATES_PER_SPAWN = GameLoop.MAX_UPS / (SPAWNS_PER_MINUTE / 60.0);
    private static double updatesUntilNextSpawn = UPDATES_PER_SPAWN;
    private static Bitmap enemyBitmap;

    private final Player player;
    private final EnemyConfig config;
    private final EnemyBrain brain;
    private int health;
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
        this.config = config;
        this.personality = personality;
        this.brain = new EnemyBrain(config);
        this.health = config.maxHealth;
        if (enemyBitmap == null) enemyBitmap =
            BitmapFactory.decodeResource(context.getResources(), R.drawable.ghost);
    }

    public static boolean readyToSpawn() {
        if (updatesUntilNextSpawn <= 0) {
            updatesUntilNextSpawn += UPDATES_PER_SPAWN;
            return true;
        }
        updatesUntilNextSpawn--;
        return false;
    }

    @Override public void update() { update(1); }

    public void update(int elapsedTicks) {
        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        brain.update(dx, dy, health, elapsedTicks);
        velocityX = brain.getMoveX();
        velocityY = brain.getMoveY();
        if (personality == EnemyPersonality.CAUTIOUS && health > 1 &&
            health < config.maxHealth && Math.hypot(dx, dy) < 380) {
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
        positionX += velocityX;
        positionY += velocityY;
        facing = Direction.fromVector(velocityX, velocityY, facing);
        attackFacing = Direction.fromVector(dx, dy, attackFacing);
    }

    public EnemyProjectile fireProjectile() {
        if (!brain.shouldFire()) return null;
        brain.onShotFired(health);
        return new EnemyProjectile(positionX, positionY,
            brain.getShotX(), brain.getShotY(), config.projectileSpeed, damage);
    }

    public void setGroupTarget(Enemy other) {
        groupTargetX = other == null ? Double.NaN : other.getPositionX();
        groupTargetY = other == null ? Double.NaN : other.getPositionY();
    }

    /** Only one fusion per resulting mob; prevents exponential health/damage stacking. */
    public void fuseWith(Enemy other) {
        health += other.health;
        damage = 2;
        fused = true;
        radius = 42;
    }

    public boolean isFused() { return fused; }
    public int getDamage() { return damage; }
    public EnemyPersonality getPersonality() { return personality; }

    public void takeDamage(int amount) { if (amount > 0) health = Math.max(0, health - amount); }
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
        if (enemyBitmap != null) {
            float x = (float) display.gameToDisplayCoordinatesX(positionX);
            float y = (float) display.gameToDisplayCoordinatesY(positionY);
            if (fused) {
                android.graphics.Rect dst = new android.graphics.Rect(
                    (int)x - 48, (int)y - 48, (int)x + 48, (int)y + 48);
                canvas.drawBitmap(enemyBitmap, null, dst, null);
            } else canvas.drawBitmap(enemyBitmap,
                x - enemyBitmap.getWidth() / 2f,
                y - enemyBitmap.getHeight() / 2f, null);
        }
    }
}
