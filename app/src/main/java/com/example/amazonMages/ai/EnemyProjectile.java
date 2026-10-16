package com.example.amazonMages.ai;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Circle;

/** World-space, non-homing hostile projectile with a finite lifetime. */
public final class EnemyProjectile extends Circle {
    private final double vx, vy;
    private final int damage;
    private int ticksRemaining = 180;

    public EnemyProjectile(double x, double y, double directionX, double directionY,
                           double speedPixelsPerSecond, int damage) {
        super(0xFF75E040, x, y, 13);
        this.damage = damage;
        vx = directionX * speedPixelsPerSecond / 60.0;
        vy = directionY * speedPixelsPerSecond / 60.0;
        paint.setAntiAlias(true);
    }

    @Override public void update() {
        positionX += vx;
        positionY += vy;
        ticksRemaining--;
    }

    public int getDamage() { return damage; }

    public boolean expired() { return ticksRemaining <= 0; }

    @Override public void draw(Canvas canvas, GameDisplay display) {
        super.draw(canvas, display);
    }
}
