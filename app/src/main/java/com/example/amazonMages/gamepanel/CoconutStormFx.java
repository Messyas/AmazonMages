package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Visual and timing layer for falling coconuts. Impacts are delivered on the landing tick. */
public final class CoconutStormFx {
    public interface ImpactHandler { void impact(double x, double y, double damageMultiplier); }

    private static final int FALL_TICKS = 12;
    private static final int IMPACT_TICKS = 24;
    private final Bitmap fruit, shadow, impact;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Impact> impacts = new ArrayList<>();

    public CoconutStormFx(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        fruit = resources.bitmap(R.drawable.coconut_fall, false);
        shadow = resources.bitmap(R.drawable.coconut_shadow, false);
        impact = resources.bitmap(R.drawable.coconut_impact, false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public void schedule(double x, double y, double damageMultiplier) {
        drops.add(new Drop(x, y, damageMultiplier));
    }

    public void clear() { drops.clear(); impacts.clear(); }

    public void update(ImpactHandler handler) {
        Iterator<Drop> dropIterator = drops.iterator();
        while (dropIterator.hasNext()) {
            Drop drop = dropIterator.next();
            drop.age++;
            if (drop.age == FALL_TICKS) {
                handler.impact(drop.x, drop.y, drop.damageMultiplier);
                impacts.add(new Impact(drop.x, drop.y));
            }
            if (drop.age >= FALL_TICKS + 3) dropIterator.remove();
        }
        Iterator<Impact> impactIterator = impacts.iterator();
        while (impactIterator.hasNext()) {
            Impact fx = impactIterator.next();
            fx.age++;
            if (fx.age >= IMPACT_TICKS) impactIterator.remove();
        }
    }

    public void draw(Canvas canvas, GameDisplay display) {
        for (Drop drop : drops) drawDrop(canvas, display, drop);
        for (Impact fx : impacts) {
            float x = (float) display.gameToDisplayCoordinatesX(fx.x);
            float y = (float) display.gameToDisplayCoordinatesY(fx.y);
            int frame = Math.min(5, fx.age / 4);
            drawFrame(canvas, impact, frame, 48, x, y, 48, 48);
        }
    }

    private void drawDrop(Canvas canvas, GameDisplay display, Drop drop) {
        float x = (float) display.gameToDisplayCoordinatesX(drop.x);
        float groundY = (float) display.gameToDisplayCoordinatesY(drop.y);
        int shadowFrame = (drop.age / 3 % 4) * 32;
        source.set(shadowFrame, 0, shadowFrame + 32, 32);
        int alpha = drop.age < 3 ? 120 : 190;
        paint.setAlpha(alpha);
        target.set(x - 16, groundY - 16, x + 16, groundY + 16);
        canvas.drawBitmap(shadow, source, target, paint);
        paint.setAlpha(255);

        if (drop.age < FALL_TICKS) {
            float progress = drop.age / (float) FALL_TICKS;
            float y = groundY - (1f - progress) * 112;
            int frame = (drop.age / 3 % 4);
            source.set(frame * 32, 0, (frame + 1) * 32, 40);
            target.set(x - 16, y - 20, x + 16, y + 20);
            canvas.drawBitmap(fruit, source, target, paint);
        }
    }

    private void drawFrame(Canvas canvas, Bitmap bitmap, int frame, int cell,
                           float x, float y, int width, int height) {
        source.set(frame * cell, 0, (frame + 1) * cell, cell);
        target.set(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f);
        canvas.drawBitmap(bitmap, source, target, paint);
    }

    private static final class Drop {
        final double x, y, damageMultiplier;
        int age;
        Drop(double x, double y, double damageMultiplier) {
            this.x = x; this.y = y; this.damageMultiplier = damageMultiplier;
        }
    }

    private static final class Impact {
        final double x, y;
        int age;
        Impact(double x, double y) { this.x = x; this.y = y; }
    }
}
