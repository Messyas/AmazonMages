package com.example.amazonMages.ai;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.gameobject.Circle;

/** World-space, non-homing hostile projectile with a finite lifetime. */
public class EnemyProjectile extends Circle {
    private final double vx, vy;
    private final int damage;
    private int ticksRemaining = 180;

    public EnemyProjectile(double x, double y, double directionX, double directionY,
                           double speedPixelsPerSecond, int damage) {
        super(0xFF75E040, x, y, 13);
        this.damage = damage;
        vx = directionX * speedPixelsPerSecond / 60.0;
        vy = directionY * speedPixelsPerSecond / 60.0;
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override public void update() {
        positionX += vx;
        positionY += vy;
        ticksRemaining--;
    }

    public double getVelocityX() { return vx; }
    public double getVelocityY() { return vy; }
    public void syncPosition(double x, double y) { positionX = x; positionY = y; }

    public int getDamage() { return damage; }
    public DamageType getDamageType() { return DamageType.ACID; }
    public boolean canTravel(double x, double y) { return true; }

    public boolean expired() { return ticksRemaining <= 0; }

    @Override public void draw(Canvas canvas, GameDisplay display) {
        float cx = Math.round(display.gameToDisplayCoordinatesX(positionX) * 0.5f) * 2f;
        float cy = Math.round(display.gameToDisplayCoordinatesY(positionY) * 0.5f) * 2f;
        paint.setColor(0xFF14261C);
        canvas.drawRect(cx - 8f, cy - 6f, cx + 8f, cy + 6f, paint);
        canvas.drawRect(cx - 6f, cy - 8f, cx + 6f, cy + 8f, paint);
        paint.setColor(0xFF389828);
        canvas.drawRect(cx - 6f, cy - 6f, cx + 6f, cy + 6f, paint);
        paint.setColor((ticksRemaining / 6) % 2 == 0 ? 0xFFB8F244 : 0xFF8FD12A);
        canvas.drawRect(cx - 4f, cy - 4f, cx + 4f, cy + 4f, paint);
        paint.setColor(0xFFF4F7A0);
        canvas.drawRect(cx - 4f, cy - 4f, cx, cy, paint);
    }
}
