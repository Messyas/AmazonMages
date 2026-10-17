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

/** Directional leaf ward and compact combat feedback sprites for the player. */
public final class PlayerDefenseFx {
    private static final int CELL = 64;
    private final Bitmap shield;
    private final Bitmap parryFlash;
    private final Bitmap postureBreak;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    public PlayerDefenseFx(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        shield = resources.bitmap(R.drawable.leaf_shield, false);
        parryFlash = resources.bitmap(R.drawable.parry_flash, false);
        postureBreak = resources.bitmap(R.drawable.posture_break, false);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    public void draw(Canvas canvas, GameDisplay display, Player player) {
        float centerX = (float) display.gameToDisplayCoordinatesX(player.getPositionX());
        float centerY = (float) display.gameToDisplayCoordinatesY(player.getPositionY());
        float directionX = (float) player.getAttackDirectionX();
        float directionY = (float) player.getAttackDirectionY();

        if (player.getParryTicks() > 0 && shield != null) {
            int frame = (Player.PARRY_WINDOW_TICKS - player.getParryTicks()) / 3;
            int row = player.getAttackFacing().ordinal();
            float x = centerX + directionX * 34 - CELL / 2f;
            float y = centerY + directionY * 34 - CELL / 2f;
            drawFrame(canvas, shield, frame, row, x, y, CELL, CELL);
        }
        if (player.getParryFlashTicks() > 0 && parryFlash != null) {
            int frame = Math.min(4, (15 - player.getParryFlashTicks()) / 3);
            float x = centerX + directionX * 32 - CELL / 2f;
            float y = centerY + directionY * 32 - CELL / 2f;
            drawFrame(canvas, parryFlash, frame, 0, x, y, CELL, CELL);
        }
        if (player.getPostureBreakTicks() > 0 && postureBreak != null) {
            int frame = Math.min(5, (18 - player.getPostureBreakTicks()) / 3);
            drawFrame(canvas, postureBreak, frame, 0, centerX - CELL / 2f,
                centerY - CELL * .8f, CELL, CELL);
        }
    }

    private void drawFrame(Canvas canvas, Bitmap bitmap, int column, int row,
                           float x, float y, float width, float height) {
        source.set(column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL);
        destination.set(x, y, x + width, y + height);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }
}
