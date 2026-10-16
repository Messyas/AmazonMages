package com.example.amazonMages.progression;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Canvas modal with seven tappable attributes, no image assets required. */
public final class AttributesPanel {
    private static final String[] KEYS = {"vigor", "mente", "resistencia", "inteligencia", "destreza", "arcano", "forca"};
    private static final String[] LABELS = {"Vigor", "Mente", "Resistencia", "Inteligencia", "Destreza", "Arcano", "Forca"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean open;
    public boolean isOpen() { return open; }
    public void toggle() { open = !open; }
    public void close() { open = false; }

    public boolean touch(float x, float y, int width, int height, PlayerProgression stats) {
        if (!open) return false;
        float left = width * .12f, top = height * .08f;
        float panelWidth = width * .76f, panelHeight = height * .84f;
        if (x > left + panelWidth * .67f && y >= top && y < top + 58) { close(); return true; }
        float rowTop = top + panelHeight * .29f;
        float rowHeight = panelHeight * .083f;
        int index = (int)((y - rowTop) / rowHeight);
        if (index >= 0 && index < KEYS.length && x > left + panelWidth * .62f && x < left + panelWidth && y >= rowTop) {
            return stats.allocate(KEYS[index]);
        }
        return false;
    }

    public void draw(Canvas canvas, int width, int height, PlayerProgression stats) {
        if (!open) return;
        float left = width * .12f, top = height * .08f;
        float panelWidth = width * .76f, panelHeight = height * .84f;
        paint.setColor(0xE913211E);
        canvas.drawRoundRect(new RectF(left, top, left + panelWidth, top + panelHeight), 18, 18, paint);
        paint.setColor(0xFFD3B977);
        paint.setTextSize(Math.max(18, Math.min(width, height) * .045f));
        canvas.drawText("ATRIBUTOS   X", left + 18, top + 38, paint);
        paint.setTextSize(Math.max(13, Math.min(width, height) * .029f));
        canvas.drawText("Nivel " + stats.level + "  XP " + stats.experience + "  Pontos " + stats.attributePoints,
            left + 18, top + panelHeight * .21f, paint);
        float rowTop = top + panelHeight * .29f;
        float rowHeight = panelHeight * .083f;
        int[] values = {stats.vigor, stats.mente, stats.resistencia, stats.inteligencia,
            stats.destreza, stats.arcano, stats.forca};
        for (int i = 0; i < KEYS.length; i++) {
            float y = rowTop + i * rowHeight + rowHeight * .62f;
            paint.setColor(0xFFF3EAD6);
            canvas.drawText(LABELS[i] + "  " + values[i], left + 18, y, paint);
            paint.setColor(stats.attributePoints > 0 ? 0xFFCCB578 : 0xFF777777);
            canvas.drawText("+", left + panelWidth * .78f, y, paint);
        }
        paint.setColor(0xFFB9C8BB);
        canvas.drawText("Toque no + para distribuir pontos", left + 18, top + panelHeight * .94f, paint);
    }
}
