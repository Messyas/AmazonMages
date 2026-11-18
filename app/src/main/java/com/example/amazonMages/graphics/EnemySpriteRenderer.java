package com.example.amazonMages.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.resources.ResourceManager;
import java.util.HashMap;
import java.util.Map;

/** Renders 4-col x 8-direction GBA enemy atlases, hurt flashes and stun markers. */
public final class EnemySpriteRenderer {
    private static final String[] KEYS = {"bat", "imp", "golem", "shaman", "sentinel", "wraith"};
    private static final Map<String, Bitmap> ATLASES = new HashMap<>();
    private static Bitmap rootedIcon;
    private static boolean loaded;
    private static final PorterDuffColorFilter HURT_TINT =
        new PorterDuffColorFilter(0xCCFFF2B0, PorterDuff.Mode.SRC_ATOP);
    private final GhostAtlasRenderer ghostRenderer;
    private final Paint paint = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();

    public EnemySpriteRenderer(Context context) {
        this.ghostRenderer = new GhostAtlasRenderer(context);
        preload(context);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public static void preload(Context context) {
        synchronized (EnemySpriteRenderer.class) {
            if (loaded) return;
            GhostAtlasRenderer.preload(context);
            ResourceManager res = ResourceManager.shared(context);
            for (String key : KEYS) {
                int id = context.getResources().getIdentifier(
                    "enemy_" + key + "_atlas", "drawable", context.getPackageName());
                if (id != 0) {
                    Bitmap atlas = res.bitmap(id, false);
                    if (atlas != null && !atlas.isRecycled()) {
                        atlas.prepareToDraw();
                        ATLASES.put(key, atlas);
                    }
                }
            }
            int rootId = context.getResources().getIdentifier("status_rooted", "drawable", context.getPackageName());
            if (rootId != 0) {
                rootedIcon = res.bitmap(rootId, false);
                if (rootedIcon != null && !rootedIcon.isRecycled()) rootedIcon.prepareToDraw();
            }
            loaded = true;
        }
    }

    public void draw(Canvas canvas, GameDisplay display, String spriteKey,
                     double x, double y, Direction facing, GhostAnimation action,
                     boolean fused, int animTicks, int hurtFlashTicks, int stunTicks) {
        if (spriteKey == null || "acid_ghost".equals(spriteKey)) {
            ghostRenderer.draw(canvas, display, x, y, facing, action, fused, animTicks);
            drawStunOverlay(canvas, display, x, y, stunTicks, animTicks);
            return;
        }
        Bitmap atlas = ATLASES.get(spriteKey);
        if (atlas == null) {
            ghostRenderer.draw(canvas, display, x, y, facing, action, fused, animTicks);
            return;
        }
        int cellW = atlas.getWidth() / 4;
        int cellH = atlas.getHeight() / 8;
        int frame = (Math.max(0, animTicks) / 7) % 4;
        int row = facing.ordinal();
        src.set(frame * cellW, row * cellH, (frame + 1) * cellW, (row + 1) * cellH);
        float cx = (float) display.gameToDisplayCoordinatesX(x);
        float cy = (float) display.gameToDisplayCoordinatesY(y);
        dst.set(cx - cellW / 2f, cy - cellH / 2f, cx + cellW / 2f, cy + cellH / 2f);
        paint.setColorFilter(hurtFlashTicks > 0 ? HURT_TINT : null);
        canvas.drawBitmap(atlas, src, dst, paint);
        paint.setColorFilter(null);
        drawStunOverlay(canvas, display, x, y, stunTicks, animTicks);
    }

    private void drawStunOverlay(Canvas canvas, GameDisplay display, double x, double y, int stunTicks, int ticks) {
        if (stunTicks <= 0 || rootedIcon == null) return;
        int frame = (ticks / 6) % 4;
        int cell = rootedIcon.getHeight();
        src.set(frame * cell, 0, (frame + 1) * cell, cell);
        float cx = (float) display.gameToDisplayCoordinatesX(x);
        float cy = (float) display.gameToDisplayCoordinatesY(y + 18);
        dst.set(cx - cell / 2f, cy - cell / 2f, cx + cell / 2f, cy + cell / 2f);
        canvas.drawBitmap(rootedIcon, src, dst, paint);
    }
}

