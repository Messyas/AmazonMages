package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.resources.ResourceManager;

/**
 * Directional leaf ward, Stage-2 Vine Guard, Living Bark Aegis ring,
 * Curupira Dash trail, Parry Riposte nova, and posture feedback sprites for the player.
 */
public final class PlayerDefenseFx {
    private static final int CELL = 64;
    private static final int NOVA_CELL = 128;
    private final Bitmap shield;
    private final Bitmap parryFlash;
    private final Bitmap parryNova;
    private final Bitmap postureBreak;
    private final Bitmap aegisRing;
    private final Bitmap dashTrail;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private int animTicks;

    public PlayerDefenseFx(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        shield = resources.bitmap(R.drawable.leaf_shield, false);
        parryFlash = resources.bitmap(R.drawable.parry_flash, false);
        parryNova = resources.bitmap(R.drawable.emerald_parry_nova, false);
        postureBreak = resources.bitmap(R.drawable.posture_break, false);
        aegisRing = resources.bitmap(R.drawable.emerald_aegis_ring, false);
        dashTrail = resources.bitmap(R.drawable.emerald_vfx_dash, false);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    public void draw(Canvas canvas, GameDisplay display, Player player) {
        animTicks++;
        float centerX = (float) display.gameToDisplayCoordinatesX(player.getPositionX());
        float centerY = (float) display.gameToDisplayCoordinatesY(player.getPositionY());
        float directionX = (float) player.getAttackDirectionX();
        float directionY = (float) player.getAttackDirectionY();

        if (player.isAegisCharged() && aegisRing != null) {
            paint.setAlpha(215);
            int frame = (animTicks / 8) % 4;
            drawFrame(canvas, aegisRing, frame, 0, CELL,
                centerX - 36f, centerY - 36f, 72f, 72f);
            paint.setAlpha(255);
        }

        if (player.isDashing() && dashTrail != null) {
            int frame = Math.min(3, Math.max(0, (Player.DASH_DURATION_TICKS - player.getDashTicks()) / 3));
            paint.setAlpha(210);
            drawFrame(canvas, dashTrail, frame, 0, CELL,
                centerX - directionX * 18f - 32f, centerY - directionY * 18f - 32f, 64f, 64f);
            paint.setAlpha(255);
        }

        if (player.getParryTicks() > 0 && shield != null) {
            paint.setAlpha(255);
            int frame = Math.min(3, Math.max(0,
                (Player.PARRY_WINDOW_TICKS - player.getParryTicks()) * 4 / Math.max(1, Player.PARRY_WINDOW_TICKS)));
            int row = player.getAttackFacing().ordinal();
            float x = centerX + directionX * 34 - CELL / 2f;
            float y = centerY + directionY * 34 - CELL / 2f;
            drawFrame(canvas, shield, frame, row, CELL, x, y, CELL, CELL);
        } else if (player.isGuarding() && shield != null) {
            paint.setAlpha(player.getGuardBlockFlashTicks() > 0 ? 235 : 155);
            int row = player.getAttackFacing().ordinal();
            float x = centerX + directionX * 30 - CELL / 2f;
            float y = centerY + directionY * 30 - CELL / 2f;
            drawFrame(canvas, shield, 2, row, CELL, x, y, CELL, CELL);
            paint.setAlpha(255);
        }

        if (player.getParryFlashTicks() > 0) {
            if (parryNova != null) {
                int novaFrame = Math.min(4, Math.max(0, (18 - player.getParryFlashTicks()) / 4));
                drawFrame(canvas, parryNova, novaFrame, 0, NOVA_CELL,
                    centerX - NOVA_CELL * 1.15f, centerY - NOVA_CELL * 1.15f,
                    NOVA_CELL * 2.3f, NOVA_CELL * 2.3f);
            }
            if (parryFlash != null) {
                int frame = Math.min(4, Math.max(0, (18 - player.getParryFlashTicks()) / 4));
                float x = centerX + directionX * 32 - CELL / 2f;
                float y = centerY + directionY * 32 - CELL / 2f;
                drawFrame(canvas, parryFlash, frame, 0, CELL, x, y, CELL, CELL);
            }
        }
        if (player.getPostureBreakTicks() > 0 && postureBreak != null) {
            int frame = Math.min(5, (18 - player.getPostureBreakTicks()) / 3);
            drawFrame(canvas, postureBreak, frame, 0, CELL, centerX - CELL / 2f,
                centerY - CELL * .8f, CELL, CELL);
        }
    }

    private void drawFrame(Canvas canvas, Bitmap bitmap, int column, int row, int cellSize,
                           float x, float y, float width, float height) {
        source.set(column * cellSize, row * cellSize, (column + 1) * cellSize, (row + 1) * cellSize);
        destination.set(x, y, x + width, y + height);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }
}
