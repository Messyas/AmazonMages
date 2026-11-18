package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.content.res.ResourcesCompat;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;

/**
 * Round pixel-art HUD camera zoom toggle using ui_button_round + golden magnifier (ui_icons[0])
 * and a wooden/gold PixelifySans zoom factor badge ("1×", "2×", "3×") at the bottom rim.
 */
public final class HudZoomDrawable extends Drawable {
    private final Bitmap roundButtons;
    private final Bitmap uiIcons;
    private final Paint bitmapPaint = new Paint();
    private final Paint badgePaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final RectF badgeRect = new RectF();
    private final int scale;
    private String zoomLabel;
    private boolean pressed;
    private boolean enabled = true;

    public HudZoomDrawable(Context context, String initialLabel) {
        ResourceManager resources = ResourceManager.shared(context);
        roundButtons = resources.bitmap(R.drawable.ui_button_round, false);
        uiIcons = resources.bitmap(R.drawable.ui_icons, false);
        scale = Math.max(2, Math.min(4, Math.round(context.getResources().getDisplayMetrics().density)));
        zoomLabel = initialLabel == null ? "2×" : initialLabel;

        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setFilterBitmap(false);
        bitmapPaint.setDither(false);
        badgePaint.setAntiAlias(false);
        badgePaint.setDither(false);

        Typeface pixelFont = ResourcesCompat.getFont(context, R.font.pixelify_sans);
        textPaint.setTypeface(pixelFont != null ? Typeface.create(pixelFont, Typeface.BOLD)
            : Typeface.create("monospace", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setZoomLabel(String label) {
        if (label != null && !label.equals(zoomLabel)) {
            zoomLabel = label;
            invalidateSelf();
        }
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        int s = scale;
        float cx = bounds.exactCenterX();
        int maxBtn = Math.min(bounds.width(), bounds.height() - 4 * s);
        float btnSize = Math.max(24 * s, Math.min(32 * s, maxBtn));
        float pressShift = pressed ? s : 0f;
        float top = bounds.top + Math.max(0f, (bounds.height() - btnSize - 4 * s) * 0.5f);

        int frame = !enabled ? 2 : pressed ? 1 : 0;
        if (roundButtons != null) {
            src.set(frame * 32, 0, (frame + 1) * 32, 32);
            dst.set(cx - btnSize * 0.5f, top, cx + btnSize * 0.5f, top + btnSize);
            canvas.drawBitmap(roundButtons, src, dst, bitmapPaint);
        }

        float iconSize = btnSize * 0.5f;
        float iconCenterY = top + btnSize * 0.44f + pressShift;
        if (uiIcons != null) {
            src.set(0, 0, 16, 16); // index 0 = golden magnifier (lupa)
            dst.set(cx - iconSize * 0.5f, iconCenterY - iconSize * 0.5f,
                cx + iconSize * 0.5f, iconCenterY + iconSize * 0.5f);
            bitmapPaint.setAlpha(enabled ? 255 : 140);
            canvas.drawBitmap(uiIcons, src, dst, bitmapPaint);
            bitmapPaint.setAlpha(255);
        }

        // Crisp wooden/gold pixel factor badge ("1×", "2×", "3×") docked cleanly on lower rim
        float textSize = Math.max(11f, btnSize * 0.28f);
        textPaint.setTextSize(textSize);
        float badgeW = Math.max(btnSize * 0.56f, textPaint.measureText(zoomLabel) + 6 * s);
        float badgeH = Math.max(10 * s, textSize + 3.5f * s);
        float badgeBottom = Math.min(bounds.bottom, top + btnSize + 3 * s);
        float badgeTop = badgeBottom - badgeH + pressShift * 0.5f;
        badgeRect.set(cx - badgeW * 0.5f, badgeTop, cx + badgeW * 0.5f, badgeTop + badgeH);

        badgePaint.setColor(0xFF0B1A12);
        canvas.drawRect(badgeRect, badgePaint);
        badgePaint.setColor(0xFFC99A2E);
        canvas.drawRect(badgeRect.left + s * 0.5f, badgeRect.top + s * 0.5f,
            badgeRect.right - s * 0.5f, badgeRect.bottom - s * 0.5f, badgePaint);
        badgePaint.setColor(0xFF2B1A12);
        canvas.drawRect(badgeRect.left + s, badgeRect.top + s,
            badgeRect.right - s, badgeRect.bottom - s, badgePaint);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = badgeRect.centerY() - (fm.ascent + fm.descent) * 0.5f;
        textPaint.setColor(0xFF0B1A12);
        canvas.drawText(zoomLabel, cx + s * 0.5f, baseline + s * 0.5f, textPaint);
        textPaint.setColor(0xFFFFF1B0);
        canvas.drawText(zoomLabel, cx, baseline, textPaint);
    }

    @Override protected boolean onStateChange(int[] state) {
        boolean nextPressed = false, nextEnabled = true;
        for (int value : state) {
            if (value == android.R.attr.state_pressed) nextPressed = true;
            else if (value == -android.R.attr.state_enabled) nextEnabled = false;
        }
        if (pressed == nextPressed && enabled == nextEnabled) return false;
        pressed = nextPressed;
        enabled = nextEnabled;
        invalidateSelf();
        return true;
    }

    @Override public boolean isStateful() { return true; }
    @Override public void setAlpha(int alpha) {
        bitmapPaint.setAlpha(alpha);
        badgePaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter colorFilter) {
        bitmapPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

