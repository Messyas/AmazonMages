package com.example.amazonMages.gamepanel;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Player;

/**
 * In-world 2x pixel-art feedback for the aim stick: while the stick is dragged a row of crisp
 * pixel pips and a pixel crosshair show where the shot will go and transition from emerald to gold
 * as a Charged Attack builds up; otherwise a subtle 2x pip shows the last aim direction.
 */
public final class AimGuide {
    private static final float IDLE_DISTANCE = 78f;
    private static final float AIM_START = 44f;
    private static final float AIM_LENGTH = 220f;
    private static final int PIPS = 6;
    private final Paint paint = new Paint();

    public AimGuide() {
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        paint.setStyle(Paint.Style.FILL);
    }

    public void draw(Canvas canvas, GameDisplay display, Player player, boolean stickHeld, boolean aiming) {
        draw(canvas, display, player, stickHeld, aiming, 0f);
    }

    public void draw(Canvas canvas, GameDisplay display, Player player,
                     boolean stickHeld, boolean aiming, float chargeRatio) {
        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) return;
        float x = (float) display.gameToDisplayCoordinatesX(player.getPositionX());
        float y = (float) display.gameToDisplayCoordinatesY(player.getPositionY());
        float ux = (float) dx, uy = (float) dy;
        float charge = Math.max(0f, Math.min(1f, chargeRatio));
        if (!stickHeld) {
            float cx = snap2(x + ux * IDLE_DISTANCE);
            float cy = snap2(y + uy * IDLE_DISTANCE);
            drawPixelPip(canvas, cx, cy, 6f, 2f, 0x8814261C, 0xB89EE6A8);
            return;
        }
        if (!aiming) return;

        int fillColor = charge >= 1f ? 0xFFFFF3AA : charge >= 0.35f ? 0xFFF2D36B : 0xE09EE6A8;
        if (charge >= 0.35f) {
            float barW = 40f;
            float bx = snap2(x - barW * 0.5f);
            float by = snap2(y + 42f);
            paint.setColor(0xD814261C);
            canvas.drawRect(bx - 2f, by - 2f, bx + barW + 2f, by + 6f, paint);
            paint.setColor(charge >= 1f ? 0xFFFFF3AA : 0xFFF2D36B);
            canvas.drawRect(bx, by, bx + Math.round(barW * charge), by + 4f, paint);
            if (charge >= 1f) {
                paint.setColor(0xD8F2D36B);
                canvas.drawRect(snap2(x - 36f), snap2(y - 36f), snap2(x - 24f), snap2(y - 32f), paint);
                canvas.drawRect(snap2(x + 24f), snap2(y - 36f), snap2(x + 36f), snap2(y - 32f), paint);
            }
        }

        for (int i = 0; i < PIPS; i++) {
            float t = i / (float) (PIPS - 1);
            float distance = AIM_START + t * (AIM_LENGTH - AIM_START);
            float cx = snap2(x + ux * distance);
            float cy = snap2(y + uy * distance);
            float inner = charge >= 1f || i >= PIPS / 2 ? 4f : 2f;
            float outer = inner + 2f;
            drawPixelPip(canvas, cx, cy, outer, inner, 0xB014261C, fillColor);
        }
        float tipX = snap2(x + ux * (AIM_LENGTH + 10f));
        float tipY = snap2(y + uy * (AIM_LENGTH + 10f));
        paint.setColor(0xD814261C);
        canvas.drawRect(tipX - 6f, tipY - 2f, tipX + 6f, tipY + 2f, paint);
        canvas.drawRect(tipX - 2f, tipY - 6f, tipX + 2f, tipY + 6f, paint);
        paint.setColor(charge >= 1f ? 0xFFFFF3AA : 0xF0F2D36B);
        canvas.drawRect(tipX - 4f, tipY - 1f, tipX + 4f, tipY + 1f, paint);
        canvas.drawRect(tipX - 1f, tipY - 4f, tipX + 1f, tipY + 4f, paint);
    }

    private static float snap2(float value) {
        return Math.round(value * 0.5f) * 2f;
    }

    private void drawPixelPip(Canvas canvas, float cx, float cy, float outerSize, float innerSize,
                              int rimColor, int fillColor) {
        float halfOuter = outerSize * 0.5f;
        float halfInner = innerSize * 0.5f;
        paint.setColor(rimColor);
        canvas.drawRect(cx - halfOuter, cy - halfOuter, cx + halfOuter, cy + halfOuter, paint);
        paint.setColor(fillColor);
        canvas.drawRect(cx - halfInner, cy - halfInner, cx + halfInner, cy + halfInner, paint);
    }
}
