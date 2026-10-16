package com.example.amazonMages.gameobject;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.combat.CombatStats;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageResolver;
import com.example.amazonMages.combat.Posture;
import com.example.amazonMages.combat.StatusEffects;
import com.example.amazonMages.GameLoop;
import com.example.amazonMages.gamepanel.HealthBar;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.Utils;
import com.example.amazonMages.graphics.SpriteAnimator;
import com.example.amazonMages.graphics.AnimationAction;
import com.example.amazonMages.graphics.Direction;

import com.example.androidstudio2dgamedevelopment.R;

/**
 * Player is the main character of the game, which the user can control with a touch joystick.
 * The player class is an extension of a Circle, which is an extension of a GameObject
 */
public class Player extends Circle {
    public static final double SPEED_PIXELS_PER_SECOND = 400.0;
    private static final double MAX_SPEED = SPEED_PIXELS_PER_SECOND / GameLoop.MAX_UPS;
    public static final int MAX_HEALTH_POINTS = 5;
    private final Joystick joystick;
    private final CombatStats combatStats = new CombatStats();
    private final StatusEffects statusEffects = new StatusEffects();
    private final Posture posture = new Posture(100);
    private double preciseHealth = MAX_HEALTH_POINTS;
    private boolean invulnerable;
    public void setInvulnerable(boolean enabled) { invulnerable = enabled; }
    public boolean isInvulnerable() { return invulnerable; }
    private final HealthBar healthBar;
    private int healthPoints = MAX_HEALTH_POINTS;
    private int maximumHealth = MAX_HEALTH_POINTS;
    private final SpriteAnimator animator;
    private Direction facing = Direction.SOUTH;
    private Direction attackFacing = Direction.SOUTH;
    private double attackDirectionX = 0;
    private double attackDirectionY = 1;
    private final PlayerState playerState;

    public Player(Context context, Joystick joystick, double positionX, double positionY, double radius, SpriteAnimator animator) {
        super(getColor(context, R.color.player), positionX, positionY, radius);
        this.joystick = joystick;
        this.healthBar = new HealthBar(context, this);
        this.animator = animator;
        this.playerState = new PlayerState(this);
    }

    public void update() {

        // Update velocity based on actuator of joystick
        double movement = MAX_SPEED * combatStats.getMovementMultiplier() * statusEffects.speedMultiplier();
        velocityX = joystick.getActuatorX()*movement;
        velocityY = joystick.getActuatorY()*movement;
        statusEffects.update();
        posture.update();

        // Update position
        positionX += velocityX;
        positionY += velocityY;

        // Update direction
        if (velocityX != 0 || velocityY != 0) {
            // Normalize velocity to get direction (unit vector of velocity)
            double distance = Utils.getDistanceBetweenPoints(0, 0, velocityX, velocityY);
            directionX = velocityX/distance;
            directionY = velocityY/distance;
            facing = Direction.fromVector(directionX, directionY, facing);
        }

        playerState.update();
        animator.setState(velocityX == 0 && velocityY == 0 ? AnimationAction.IDLE : AnimationAction.MOVE, facing);
        animator.update();
    }

    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        animator.draw(canvas, gameDisplay, positionX, positionY);

        healthBar.draw(canvas, gameDisplay);
    }

    /** Used only before the game loop starts, when restoring a save. */
    public void restorePosition(double x, double y) {
        positionX = x;
        positionY = y;
    }

    public int getMaximumHealth() { return maximumHealth; }
    public void setMaximumHealth(int value) {
        maximumHealth = Math.max(1, value);
        healthPoints = Math.min(healthPoints, maximumHealth);
        preciseHealth = Math.min(preciseHealth, maximumHealth);
    }

    public int getHealthPoint() {
        return healthPoints;
    }

    public void setHealthPoint(int healthPoints) {
        // Only allow positive values
        if (healthPoints >= 0) {
            this.healthPoints = Math.min(maximumHealth, healthPoints);
            preciseHealth = this.healthPoints;
        }
    }

    /** Apply typed damage without rounding away fractional resistance. */
    public void receiveHit(DamageEvent hit) {
        if (healthPoints <= 0 || invulnerable) return;
        preciseHealth = Math.max(0, preciseHealth - DamageResolver.resolve(hit, combatStats, statusEffects));
        healthPoints = (int) Math.ceil(preciseHealth);
        posture.applyPressure(hit.postureDamage);
    }

    public CombatStats getCombatStats() { return combatStats; }
    public StatusEffects getStatusEffects() { return statusEffects; }
    public Posture getPosture() { return posture; }

    /** Aim can be set separately from movement by future combat controls. */
    public void aim(double x, double y) {
        double length = Math.hypot(x, y);
        if (length < 0.001 || !Double.isFinite(length)) return;
        attackDirectionX = x / length;
        attackDirectionY = y / length;
        attackFacing = Direction.fromVector(x, y, attackFacing);
    }
    public double getAttackDirectionX() { return attackDirectionX; }
    public double getAttackDirectionY() { return attackDirectionY; }

    public Direction getFacing() { return facing; }
    public Direction getAttackFacing() { return attackFacing; }

    public PlayerState getPlayerState() {
        return playerState;
    }
}
