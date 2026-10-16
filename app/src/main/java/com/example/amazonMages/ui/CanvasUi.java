package com.example.amazonMages.ui;
import android.graphics.Canvas;
import android.graphics.Paint;

/** Shared proportional Canvas primitives; one Paint, no temporary geometry per draw. */
public final class CanvasUi {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public void panel(Canvas canvas, float l, float t, float r, float b) {
        paint.setColor(0xEE13251E);
        canvas.drawRoundRect(l, t, r, b, 12, 12, paint);
    }
    public void text(Canvas canvas, String value, float x, float y, float size, int color) {
        paint.setColor(color); paint.setTextSize(size);
        canvas.drawText(value, x, y, paint);
    }
    public void button(Canvas c, String label, float l, float t, float r, float b, boolean selected) {
        paint.setColor(selected ? 0xFF74633A : 0xFF36594B);
        c.drawRoundRect(l, t, r, b, 8, 8, paint);
        text(c, label, l + (r-l)*.06f, t+(b-t)*.68f, (b-t)*.42f, 0xFFF5EACD);
    }
    public void bar(Canvas c, float l, float t, float r, float b, double ratio, int color) {
        paint.setColor(0xFF14221B); c.drawRect(l,t,r,b,paint);
        paint.setColor(color); c.drawRect(l,t,l+(r-l)*(float)Math.max(0, Math.min(1,ratio)),b,paint);
    }
    public static boolean hit(float x, float y, float l, float t, float r, float b) {
        return x >= l && x < r && y >= t && y < b;
    }
}
