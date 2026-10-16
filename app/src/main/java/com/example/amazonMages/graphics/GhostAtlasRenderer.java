package com.example.amazonMages.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.ai.EnemyState;

/**
 * Fixed grid: eight direction rows (S,SE,E,NE,N,NW,W,SW), four frame columns.
 * Works with 64x64 art (256x512 atlas) or compact 32x32 (128x256 atlas).
 * Missing optional atlases fall back to the original ghost.png.
 */
public final class GhostAtlasRenderer {
    private static Bitmap normal;
    private static Bitmap fused;
    private static Bitmap fallback;
    private static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    private final Rect destination = new Rect();

    public GhostAtlasRenderer(Context context) {
        if (fallback == null) fallback = BitmapFactory.decodeResource(
            context.getResources(), com.example.androidstudio2dgamedevelopment.R.drawable.ghost);
        if (normal == null) normal = loadOptional(context, "acid_ghost_atlas");
        if (fused == null) fused = loadOptional(context, "acid_ghost_fused_atlas");
    }

    private static Bitmap loadOptional(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(context.getResources(), id, options);
    }

    public void draw(Canvas canvas, GameDisplay display, double x, double y,
                     Direction facing, EnemyState state, boolean merged, int animationTick) {
        Bitmap atlas = merged ? fused : normal;
        int size = merged ? 96 : 64;
        int cx = (int) display.gameToDisplayCoordinatesX(x);
        int cy = (int) display.gameToDisplayCoordinatesY(y);
        destination.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        if (atlas == null) {
            if (fallback != null) canvas.drawBitmap(fallback, null, destination, PAINT);
            return;
        }
        int frameWidth = atlas.getWidth() / 4;
        int frameHeight = atlas.getHeight() / 8;
        int column;
        if (state == EnemyState.ALERT) column = 2;
        else if (state == EnemyState.ATTACK || state == EnemyState.ENRAGED) column = 3;
        else if (state == EnemyState.CHASE) column = 1;
        else column = (animationTick / 16) % 2;
        int row = facing.ordinal();
        source.set(column * frameWidth, row * frameHeight,
            (column + 1) * frameWidth, (row + 1) * frameHeight);
        canvas.drawBitmap(atlas, source, destination, PAINT);
    }
}
