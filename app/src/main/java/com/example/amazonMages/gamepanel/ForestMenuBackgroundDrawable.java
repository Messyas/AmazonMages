package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.resources.ResourceManager;

/** Static, nearest-neighbor forest scene for the title screen. */
public final class ForestMenuBackgroundDrawable extends Drawable {
    private final Bitmap grass;
    private final Bitmap trees;
    private final Bitmap player;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF target = new RectF();

    public ForestMenuBackgroundDrawable(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        grass = resources.bitmap(R.drawable.tile_grass, false);
        trees = resources.bitmap(R.drawable.obj_tree_round, false);
        player = resources.bitmap(R.drawable.player_idle, false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        canvas.save();
        canvas.clipRect(bounds);

        if (grass != null) {
            for (int y = bounds.top; y < bounds.bottom; y += 64) {
                int row = Math.floorDiv(y - bounds.top, 64);
                for (int x = bounds.left; x < bounds.right; x += 64) {
                    int col = Math.floorDiv(x - bounds.left, 64);
                    int variant = Math.floorMod(col * 3 + row * 5, Math.max(1, grass.getWidth() / 64));
                    source.set(variant * 64, 0, variant * 64 + 64, Math.min(64, grass.getHeight()));
                    target.set(x, y, x + 64, y + 64);
                    canvas.drawBitmap(grass, source, target, paint);
                }
            }
        } else canvas.drawColor(0xFF1E6436);

        if (trees != null) {
            int cellCount = Math.max(1, trees.getWidth() / 128);
            int[][] anchors = {
                {bounds.left - 44, bounds.top - 48},
                {bounds.right - 220, bounds.top - 55},
                {bounds.left - 58, bounds.bottom - 190},
                {bounds.right - 196, bounds.bottom - 170}
            };
            for (int i = 0; i < anchors.length; i++) {
                int variant = i % cellCount;
                source.set(variant * 128, 0, variant * 128 + 128, Math.min(128, trees.getHeight()));
                target.set(anchors[i][0], anchors[i][1], anchors[i][0] + 230, anchors[i][1] + 230);
                canvas.drawBitmap(trees, source, target, paint);
            }
        }

        paint.setColor(0x4D07140C);
        canvas.drawRect(bounds, paint);

        if (player != null && player.getWidth() >= 64 && player.getHeight() >= 64) {
            source.set(0, 0, 64, 64);
            float size = Math.min(112, bounds.height() * .24f);
            float x = bounds.centerX() - size * .5f;
            float y = bounds.centerY() + bounds.height() * .18f - size * .5f;
            target.set(x, y, x + size, y + size);
            paint.setColor(0xFFFFFFFF);
            canvas.drawBitmap(player, source, target, paint);
        }
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
}
