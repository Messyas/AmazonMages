package com.example.amazonMages.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.example.amazonMages.GameDisplay;
import java.util.EnumMap;

/**
 * Optional PNG atlas convention:
 * drawable-nodpi/acid_ghost_<action>.png and acid_ghost_fused_<action>.png.
 * Each atlas: 8 direction rows in Direction enum order, N columns from
 * GhostAnimation.frames; each cell must be square and equally sized.
 * Existing 4x8 generic atlases and ghost.png remain compatible fallbacks.
 */
public final class GhostAtlasRenderer {
    // Atlases are immutable and identical for every ghost, so they are loaded once and shared.
    // (They used to be per-instance: every spawn re-ran ~30 resource lookups and, once the
    // bitmap cache was full, re-decoded PNGs on the game thread.)
    private static final EnumMap<GhostAnimation, Bitmap> NORMAL =
        new EnumMap<>(GhostAnimation.class);
    private static final EnumMap<GhostAnimation, Bitmap> FUSED =
        new EnumMap<>(GhostAnimation.class);
    private static Bitmap genericNormal, genericFused, fallback;
    private static boolean loaded;
    private static final Paint PAINT = new Paint();
    static {
        PAINT.setAntiAlias(false);
        PAINT.setFilterBitmap(false);
        PAINT.setDither(false);
    }
    private final Rect source = new Rect();
    private final Rect destination = new Rect();

    public GhostAtlasRenderer(Context context) {
        preload(context);
    }

    /** Idempotent and thread-safe; call from a background thread at start-up to avoid spawn hitches. */
    public static void preload(Context context) {
        synchronized (GhostAtlasRenderer.class) {
            if (loaded) return;
            fallback = com.example.amazonMages.resources.ResourceManager.shared(context).bitmap(
                com.example.androidstudio2dgamedevelopment.R.drawable.ghost, false);
            genericNormal = loadOptional(context, "acid_ghost_atlas");
            genericFused = loadOptional(context, "acid_ghost_fused_atlas");
            if (!valid(genericNormal, 4)) genericNormal = null;
            if (!valid(genericFused, 4)) genericFused = null;
            for (GhostAnimation action : GhostAnimation.values()) {
                Bitmap normal = loadOptional(context, "acid_ghost_" + action.fileSuffix);
                Bitmap merged = loadOptional(context, "acid_ghost_fused_" + action.fileSuffix);
                if (valid(normal, action.frames)) NORMAL.put(action, normal);
                if (valid(merged, action.frames)) FUSED.put(action, merged);
            }
            // Hint the GPU upload now instead of during the first frame that shows each atlas.
            warm(fallback); warm(genericNormal); warm(genericFused);
            for (Bitmap bitmap : NORMAL.values()) warm(bitmap);
            for (Bitmap bitmap : FUSED.values()) warm(bitmap);
            loaded = true;
        }
    }

    private static void warm(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.prepareToDraw();
    }

    private static Bitmap loadOptional(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return com.example.amazonMages.resources.ResourceManager.shared(context).bitmap(id, false);
    }

    private static boolean valid(Bitmap bitmap, int columns) {
        return bitmap != null && bitmap.getWidth() % columns == 0 &&
            bitmap.getHeight() % 8 == 0 &&
            bitmap.getWidth() / columns == bitmap.getHeight() / 8;
    }

    public void draw(Canvas canvas, GameDisplay display, double x, double y,
                     Direction facing, GhostAnimation action, boolean merged, int ticks) {
        Bitmap atlas = merged ? FUSED.get(action) : NORMAL.get(action);
        boolean generic = atlas == null;
        if (generic) atlas = merged ? genericFused : genericNormal;
        int size = merged ? 96 : 64;
        int cx = (int) display.gameToDisplayCoordinatesX(x);
        int cy = (int) display.gameToDisplayCoordinatesY(y);
        destination.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        if (atlas == null) {
            if (fallback != null) canvas.drawBitmap(fallback, null, destination, PAINT);
            return;
        }
        int columns = generic ? 4 : action.frames;
        int width = atlas.getWidth() / columns;
        int height = atlas.getHeight() / 8;
        if (width <= 0 || height <= 0) return;
        int frame;
        if (generic) {
            if (action == GhostAnimation.ALERT || action == GhostAnimation.SPOTTED_PLAYER ||
                action == GhostAnimation.STARTLED || action == GhostAnimation.SPOTTED_ALLY)
                frame = 2;
            else if (action == GhostAnimation.ATTACK || action == GhostAnimation.ENRAGED ||
                     action == GhostAnimation.FUSION) frame = 3;
            else if (action == GhostAnimation.MOVE || action == GhostAnimation.CHASE ||
                     action == GhostAnimation.FLEE || action == GhostAnimation.SOCIAL)
                frame = 1;
            else frame = (ticks / 16) % 2;
        } else {
            int index = Math.max(0, ticks) / action.ticksPerFrame;
            frame = action.loops ? index % columns : Math.min(index, columns - 1);
        }
        int row = facing.ordinal();
        source.set(frame * width, row * height, (frame + 1) * width, (row + 1) * height);
        canvas.drawBitmap(atlas, source, destination, PAINT);
    }
}
