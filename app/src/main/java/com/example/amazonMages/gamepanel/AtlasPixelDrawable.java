package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.example.amazonMages.resources.ResourceManager;

/** Nearest-neighbor frame from a UI atlas, optionally stretched through a 9-slice center. */
public final class AtlasPixelDrawable extends Drawable {
    private final Bitmap atlas;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final Rect target = new Rect();
    private final int cellWidth;
    private final int cellHeight;
    private final int frame;
    private final int leftCap;
    private final int topCap;
    private final int rightCap;
    private final int bottomCap;
    private final int[] sx = new int[4];
    private final int[] sy = new int[4];
    private final int[] dx = new int[4];
    private final int[] dy = new int[4];

    public AtlasPixelDrawable(Context context, int resourceId, int cellWidth, int cellHeight,
            int frame, int leftCap, int topCap, int rightCap, int bottomCap) {
        atlas = ResourceManager.shared(context).bitmap(resourceId, false);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.frame = frame;
        this.leftCap = leftCap;
        this.topCap = topCap;
        this.rightCap = rightCap;
        this.bottomCap = bottomCap;
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public static AtlasPixelDrawable frame(Context context, int resourceId,
            int cellWidth, int cellHeight, int frame) {
        return new AtlasPixelDrawable(context, resourceId, cellWidth, cellHeight,
            frame, 0, 0, 0, 0);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (atlas == null || bounds.isEmpty()) return;
        if (leftCap + rightCap + topCap + bottomCap == 0) {
            source.set(frame * cellWidth, 0, (frame + 1) * cellWidth, cellHeight);
            canvas.drawBitmap(atlas, source, bounds, paint);
            return;
        }
        int l = Math.min(leftCap, bounds.width() / 2);
        int r = Math.min(rightCap, bounds.width() / 2);
        int t = Math.min(topCap, bounds.height() / 2);
        int b = Math.min(bottomCap, bounds.height() / 2);
        sx[0] = frame * cellWidth; sx[1] = sx[0] + leftCap;
        sx[2] = (frame + 1) * cellWidth - rightCap; sx[3] = (frame + 1) * cellWidth;
        sy[0] = 0; sy[1] = topCap; sy[2] = cellHeight - bottomCap; sy[3] = cellHeight;
        dx[0] = bounds.left; dx[1] = bounds.left + l;
        dx[2] = bounds.right - r; dx[3] = bounds.right;
        dy[0] = bounds.top; dy[1] = bounds.top + t;
        dy[2] = bounds.bottom - b; dy[3] = bounds.bottom;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            source.set(sx[col], sy[row], sx[col + 1], sy[row + 1]);
            target.set(dx[col], dy[row], dx[col + 1], dy[row + 1]);
            canvas.drawBitmap(atlas, source, target, paint);
        }
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
