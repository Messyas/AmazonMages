package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/** Procedural placeholder for the magical acid swamp; no new art dependency. */
final class AcidSwampTile extends Tile {
    private static final Paint BASE = new Paint();
    private static final Paint POOL = new Paint(3);
    private static final Paint GLOW = new Paint(3);
    static {
        BASE.setColor(0xFF233D2C);
        POOL.setColor(0xFF4E9B37);
        GLOW.setColor(0xFFB6ED69);
    }

    AcidSwampTile(Rect location) { super(location); }

    @Override public void draw(Canvas canvas) {
        canvas.drawRect(mapLocationRect, BASE);
        float x = mapLocationRect.left, y = mapLocationRect.top;
        canvas.drawOval(x + 5, y + 13, x + 57, y + 52, POOL);
        canvas.drawCircle(x + 18, y + 28, 4, GLOW);
        canvas.drawCircle(x + 43, y + 39, 3, GLOW);
        canvas.drawCircle(x + 33, y + 18, 2, GLOW);
    }
}
