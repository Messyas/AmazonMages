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
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.gamepanel.PlayerDefenseFx;
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
    public static final int PARRY_WINDOW_TICKS = 12;
    public static final int PARRY_COOLDOWN_TICKS = 36;
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
    private final PlayerDefenseFx defenseFx;
    private int healthPoints = MAX_HEALTH_POINTS;
    private int maximumHealth = MAX_HEALTH_POINTS;
    private final SpriteAnimator animator;
    private AnimationAction scriptedAction;
    private int scriptedActionTicks;
    private Direction facing = Direction.SOUTH;
    private Direction attackFacing = Direction.SOUTH;
    private double attackDirectionX = 0;
    private double attackDirectionY = 1;
    private final PlayerState playerState;
    private int parryTicks, parryCooldownTicks, parryFlashTicks, postureBreakTicks;

    public Player(Context context, Joystick joystick, double positionX, double positionY, double radius, SpriteAnimator animator) {
        super(getColor(context, R.color.player), positionX, positionY, radius);
        this.joystick = joystick;
        this.defenseFx = new PlayerDefenseFx(context);
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
        if (parryTicks > 0) parryTicks--;
        if (parryCooldownTicks > 0) parryCooldownTicks--;
        if (parryFlashTicks > 0) parryFlashTicks--;
        if (postureBreakTicks > 0) postureBreakTicks--;

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
        AnimationAction action = scriptedAction != null ? scriptedAction :
            (velocityX == 0 && velocityY == 0 ? AnimationAction.IDLE : AnimationAction.MOVE);
        Direction animationFacing = action == AnimationAction.ATTACK || action == AnimationAction.PARRY
            ? attackFacing : facing;
        animator.setState(action, animationFacing);
        animator.update();
        if (scriptedActionTicks > 0 && --scriptedActionTicks == 0 &&
                scriptedAction != AnimationAction.DEATH) scriptedAction = null;
    }

    /** Play a non-locomotion action for its authored atlas duration. */
    public void playAction(AnimationAction action) {
        if (action == null || action == AnimationAction.IDLE || action == AnimationAction.MOVE) return;
        scriptedAction = action;
        switch (action) {
            case ATTACK: scriptedActionTicks = 6 * 5; break;
            case HURT: scriptedActionTicks = 3 * 6; break;
            case DEATH: scriptedActionTicks = 8 * 8; break;
            case SPECIAL: scriptedActionTicks = 8 * 6; break;
            case PARRY: scriptedActionTicks = 4 * 4; break;
            case DODGE: scriptedActionTicks = 5 * 4; break;
            default: scriptedActionTicks = 1;
        }
        animator.setState(action, action == AnimationAction.ATTACK || action == AnimationAction.PARRY
            ? attackFacing : facing);
    }

    /** Game over stops normal simulation, but the collapse animation keeps advancing. */
    public void advanceDeathAnimation() {
        if (scriptedAction != AnimationAction.DEATH) playAction(AnimationAction.DEATH);
        animator.setState(AnimationAction.DEATH, facing);
        animator.update();
        if (scriptedActionTicks > 0) scriptedActionTicks--;
    }

    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        animator.draw(canvas, gameDisplay, positionX, positionY);
        defenseFx.draw(canvas, gameDisplay, this);
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

    public double getPreciseHealth() { return preciseHealth; }
    public boolean heal(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || preciseHealth <= 0 || preciseHealth >= maximumHealth) return false;
        preciseHealth = Math.min(maximumHealth, preciseHealth + amount);
        healthPoints = (int) Math.ceil(preciseHealth);
        return true;
    }

    public int getHealthPoint() {
        return healthPoints;
    }

    public void setHealthPoint(int healthPoints) {
        // Only allow positive values
        if (healthPoints >= 0) {
            this.healthPoints = Math.min(maximumHealth, healthPoints);
            preciseHealth = this.healthPoints;
            if (this.healthPoints > 0 && scriptedAction == AnimationAction.DEATH) {
                scriptedAction = null;
                scriptedActionTicks = 0;
            } else if (this.healthPoints == 0) playAction(AnimationAction.DEATH);
        }
    }

    /** Apply typed damage without rounding away fractional resistance. */
    public void receiveHit(DamageEvent hit) {
        if (healthPoints <= 0 || invulnerable) return;
        double previousHealth = preciseHealth;
        preciseHealth = Math.max(0, preciseHealth - DamageResolver.resolve(hit, combatStats, statusEffects));
        healthPoints = (int) Math.ceil(preciseHealth);
        if (posture.applyPressure(hit.postureDamage)) postureBreakTicks = 18;
        if (preciseHealth < previousHealth)
            playAction(healthPoints <= 0 ? AnimationAction.DEATH : AnimationAction.HURT);
    }

    /** Opens a short directional parry window with a brief recovery. */
    public boolean beginParry() {
        if (healthPoints <= 0 || parryCooldownTicks > 0 || scriptedAction == AnimationAction.DEATH) return false;
        parryTicks = PARRY_WINDOW_TICKS;
        parryCooldownTicks = PARRY_COOLDOWN_TICKS;
        playAction(AnimationAction.PARRY);
        return true;
    }

    /** True when an incoming hit falls inside the frontal 140-degree parry arc. */
    public boolean tryParry(double sourceX, double sourceY) {
        if (parryTicks <= 0 || invulnerable) return false;
        double dx = sourceX - positionX, dy = sourceY - positionY;
        double length = Math.hypot(dx, dy);
        if (length < 0.001) return false;
        double dot = (dx * attackDirectionX + dy * attackDirectionY) / length;
        if (dot < 0.3420) return false;
        parryTicks = 0;
        parryFlashTicks = 15;
        return true;
    }

    public boolean isParryReady() { return parryCooldownTicks == 0; }
    public int getParryCooldownTicks() { return parryCooldownTicks; }
    public int getParryTicks() { return parryTicks; }
    public int getParryFlashTicks() { return parryFlashTicks; }
    public int getPostureBreakTicks() { return postureBreakTicks; }

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
