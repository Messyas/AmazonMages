package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

/** Decodes only visible 512px parts of the generated monumental tree. */
final class WorldTreeParts {
    private static final int[] TRUNK = {R.drawable.world_castanheira_trunk_v1_p0,
        R.drawable.world_castanheira_trunk_v1_p1, R.drawable.world_castanheira_trunk_v1_p2,
        R.drawable.world_castanheira_trunk_v1_p3, R.drawable.world_castanheira_trunk_v1_p4,
        R.drawable.world_castanheira_trunk_v1_p5};
    private static final int[] ROOT = {R.drawable.world_root_v1_p0, R.drawable.world_root_v1_p1, R.drawable.world_root_v1_p2};
    private final SpriteSheet sprites;
    private final Paint paint = new Paint();
    private final Rect clip = new Rect();
    private final RectF target = new RectF();
    WorldTreeParts(SpriteSheet sprites) { this.sprites = sprites; paint.setFilterBitmap(false); }

    void draw(Canvas canvas, WorldDefinition world) {
        for (WorldDefinition.Root root : world.roots) {
            double dx = root.end.col - root.start.col, dy = root.end.row - root.start.row;
            canvas.save(); canvas.translate((float) root.start.worldX(), (float) root.start.worldY());
            canvas.rotate((float) -Math.toDegrees(Math.atan2(dx, dy)));
            float width = (float) root.baseWidth * 128;
            parts(canvas, ROOT, 1, 384, 1536, -width / 2, 0, width, (float) Math.hypot(dx, dy) * 64);
            canvas.restore();
        }
        parts(canvas, TRUNK, 2, 1024, 1536, 928 * 64, 844 * 64, 64 * 64, 96 * 64);
    }
    private void parts(Canvas canvas, int[] ids, int columns, int sourceWidth, int sourceHeight,
            float left, float top, float width, float height) {
        canvas.getClipBounds(clip);
        for (int i = 0; i < ids.length; i++) {
            int sx = i % columns * 512, sy = i / columns * 512;
            target.set(left + sx * width / sourceWidth, top + sy * height / sourceHeight,
                left + Math.min(sourceWidth, sx + 512) * width / sourceWidth,
                top + Math.min(sourceHeight, sy + 512) * height / sourceHeight);
            if (target.right < clip.left || target.left > clip.right || target.bottom < clip.top || target.top > clip.bottom) continue;
            Bitmap bitmap = sprites.resourceBitmap(ids[i]);
            canvas.drawBitmap(bitmap, null, target, paint);
        }
    }
}
