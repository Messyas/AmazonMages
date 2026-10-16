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
    private Direction facing = Direction.SOUTH;
    private Direction attackFacing = Direction.SOUTH;

    public Enemy(Context context, Player player) {
        this(context, player, EnemyConfig.acidGhost());
    }

    public Enemy(Context context, Player player, EnemyConfig config) {
        super(getColor(context, R.color.enemy),
            player.getPositionX() + (Math.random() - 0.5) * 1100,
            player.getPositionY() + (Math.random() - 0.5) * 1100, 30);
        this.player = player;
        this.config = config;
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

    @Override public void update() {
        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        brain.update(dx, dy, health);
        velocityX = brain.getMoveX();
        velocityY = brain.getMoveY();
        positionX += velocityX;
        positionY += velocityY;
        facing = Direction.fromVector(velocityX, velocityY, facing);
        attackFacing = Direction.fromVector(dx, dy, attackFacing);
    }

    public EnemyProjectile fireProjectile() {
        if (!brain.shouldFire()) return null;
        return new EnemyProjectile(positionX, positionY,
            brain.getShotX(), brain.getShotY(), config.projectileSpeed);
    }

    public void takeDamage(int amount) { if (amount > 0) health = Math.max(0, health - amount); }
    public boolean isDead() { return health <= 0; }
    public int getHealth() { return health; }
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
            canvas.drawBitmap(enemyBitmap,
                x - enemyBitmap.getWidth() / 2f,
                y - enemyBitmap.getHeight() / 2f, null);
        }
    }
}
