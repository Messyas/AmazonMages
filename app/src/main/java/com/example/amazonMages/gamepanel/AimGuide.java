package com.example.amazonMages.gamepanel;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Player;

/**
 * In-world feedback for the aim stick: while the stick is dragged a dotted line with an arrow head
 * points where the shot will go; otherwise a faint chevron next to the player shows the last aim
 * (the direction a quick tap will fire). Reuses its paints and path, so drawing allocates nothing.
 */
public final class AimGuide {
    private static final float IDLE_DISTANCE = 78f;
    private static final float AIM_START = 44f;
    private static final float AIM_LENGTH = 220f;
    private static final int DOTS = 7;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    public void draw(Canvas canvas, GameDisplay display, Player player, boolean stickHeld, boolean aiming) {
        double dx = player.getAttackDirectionX(), dy = player.getAttackDirectionY();
        if (dx == 0 && dy == 0) return;
        float x = (float) display.gameToDisplayCoordinatesX(player.getPositionX());
        float y = (float) display.gameToDisplayCoordinatesY(player.getPositionY());
        float ux = (float) dx, uy = (float) dy;
        if (!stickHeld) {
            drawChevron(canvas, x + ux * IDLE_DISTANCE, y + uy * IDLE_DISTANCE, ux, uy, 11f, 0x669EE6A8);
            return;
        }
        if (!aiming) return; // still inside the dead zone: nothing to show yet
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < DOTS; i++) {
            float t = i / (float) DOTS;
            float distance = AIM_START + t * (AIM_LENGTH - AIM_START);
            paint.setColor(((int) (80 + 150 * t) << 24) | 0x9EE6A8);
            canvas.drawCircle(x + ux * distance, y + uy * distance, 3f + 3f * t, paint);
        }
        drawChevron(canvas, x + ux * (AIM_LENGTH + 10f), y + uy * (AIM_LENGTH + 10f), ux, uy, 20f, 0xE69EE6A8);
    }

    /** Filled triangle at (cx,cy) pointing along (ux,uy). */
    private void drawChevron(Canvas canvas, float cx, float cy, float ux, float uy, float size, int color) {
        float px = -uy, py = ux; // perpendicular
        arrow.reset();
        arrow.moveTo(cx + ux * size, cy + uy * size);
        arrow.lineTo(cx - ux * size * .6f + px * size * .75f, cy - uy * size * .6f + py * size * .75f);
        arrow.lineTo(cx - ux * size * .6f - px * size * .75f, cy - uy * size * .6f - py * size * .75f);
        arrow.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(arrow, paint);
    }
}
