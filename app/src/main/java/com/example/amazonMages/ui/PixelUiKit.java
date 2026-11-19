package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import com.example.amazonMages.combat.WeaponDefinition;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.combat.WeaponType;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;

/**
 * Shared pixel-art UI component renderer backed by ui-kit-v3 and v4 PixelifySans + downsampled atlases.
 * Keeps all bitmap draws unfiltered and provides crisp integer-scaled 9-slice, slot, medallion,
 * tab, button, divider, progress bar, and icon primitives with zero draw-loop allocations.
 */
public final class PixelUiKit {
    public static final int ICON_MAGNIFIER = 0;
    public static final int ICON_BAG = 1;
    public static final int ICON_ATTRIBUTES = 2;
    public static final int ICON_MAP = 3;
    public static final int ICON_JOURNAL = 4;
    public static final int ICON_CHAT = 5;
    public static final int ICON_MENU = 6;
    public static final int ICON_PAUSE = 7;
    public static final int ICON_SAVE = 10;
    public static final int ICON_HEART = 12;
    public static final int ICON_MANA = 13;
    public static final int ICON_SHIELD = 14;
    public static final int ICON_SWORD = 15;
    public static final int ICON_LOCK = 17;

    public static final int CRAFT_ICON_ELIXIR = 0;
    public static final int CRAFT_ICON_BOMB = 1;
    public static final int CRAFT_ICON_TONIC = 2;
    public static final int CRAFT_ICON_RUNE = 3;
    public static final int CRAFT_ICON_CAULDRON = 4;
    public static final int CRAFT_ICON_CHALICE = 5;

    private static final int[] PANEL_X = {0, 10, 22, 32};
    private static final int[] PANEL_Y = {0, 10, 22, 32};
    private static final int[] TITLE_X = {0, 10, 38, 48};
    private static final int[] TITLE_Y = {2, 7, 13, 18};
    private static final int[] BTN_X = {1, 10, 22, 31};
    private static final int[] BTN_Y = {2, 6, 13, 17};
    private static final int[] TAB_X = {0, 8, 32, 40};
    private static final int[] TAB_Y = {0, 5, 12, 16};
    private static final int[] BAR_X = {0, 12, 50, 64};
    private static final int[] BAR_Y = {1, 4, 8, 11};
    private static final int[] PERF_X = {0, 4, 12, 16};
    private static final int[] PERF_Y = {0, 4, 12, 16};

    private final Context context;
    private Bitmap panel;
    private Bitmap panelTitle;
    private Bitmap button;
    private Bitmap closeBtn;
    private final Bitmap slot;
    private Bitmap tab;
    private Bitmap dividerH;
    private Bitmap dividerV;
    private Bitmap barFrame;
    private Bitmap barFill;
    private Bitmap stepper;
    private final Bitmap icons;
    private Bitmap cursorSelect;
    private final Bitmap perfOverlay;
    private Bitmap skillMedallion;
    private Bitmap skillIcons;
    private final Bitmap craftIcons;
    private final Bitmap boitataLootIcons;
    private Bitmap craftCauldron;
    private final Bitmap pickupIcons;
    private final Bitmap weaponIcons;

    private final Typeface pixelFontRegular;
    private final Typeface pixelFontBold;
    private final int scale;
    private final float density;

    private final Paint pixelPaint = new Paint();
    private final Paint shapePaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final RectF tmp = new RectF();
    private final float[] dx = new float[4];
    private final float[] dy = new float[4];

    public PixelUiKit(Context context) {
        this.context = context.getApplicationContext();
        ResourceManager res = ResourceManager.shared(context);
        slot = res.bitmap(R.drawable.ui_slot, false);
        icons = res.bitmap(R.drawable.ui_icons, false);
        perfOverlay = res.bitmap(R.drawable.ui_perf_overlay, false);
        craftIcons = res.bitmap(R.drawable.ui_craft_icons, false);
        boitataLootIcons = res.bitmap(R.drawable.boitata_loot, false);
        pickupIcons = res.bitmap(R.drawable.emerald_pickups, false);
        weaponIcons = res.bitmap(R.drawable.hud_weapon_icons, false);

        density = context.getResources().getDisplayMetrics().density;
        scale = Math.max(2, Math.min(4, Math.round(density)));
        pixelPaint.setAntiAlias(false);
        pixelPaint.setFilterBitmap(false);
        pixelPaint.setDither(false);
        shapePaint.setAntiAlias(false);
        shapePaint.setDither(false);

        Typeface font = ResourcesCompat.getFont(context, R.font.pixelify_sans);
        pixelFontRegular = font != null ? font : Typeface.create("monospace", Typeface.NORMAL);
        pixelFontBold = Typeface.create(pixelFontRegular, Typeface.BOLD);
        textPaint.setTypeface(pixelFontRegular);
    }

    public synchronized void ensureModalBitmaps() {
        if (panel != null) return;
        ResourceManager res = ResourceManager.shared(context);
        panel = res.bitmap(R.drawable.ui_panel, false);
        panelTitle = res.bitmap(R.drawable.ui_panel_title, false);
        button = res.bitmap(R.drawable.ui_button, false);
        closeBtn = res.bitmap(R.drawable.ui_close, false);
        tab = res.bitmap(R.drawable.ui_tab, false);
        dividerH = res.bitmap(R.drawable.ui_divider_h, false);
        dividerV = res.bitmap(R.drawable.ui_divider_v, false);
        barFrame = res.bitmap(R.drawable.ui_bar_frame, false);
        barFill = res.bitmap(R.drawable.ui_bar_fill, false);
        stepper = res.bitmap(R.drawable.ui_stepper, false);
        cursorSelect = res.bitmap(R.drawable.ui_cursor_select, false);
        skillMedallion = res.bitmap(R.drawable.ui_skill_medallion, false);
        skillIcons = res.bitmap(R.drawable.ui_skill_icons, false);
        craftCauldron = res.bitmap(R.drawable.ui_craft_cauldron, false);
    }

    public synchronized void releaseModalBitmaps() {
        panel = null;
        panelTitle = null;
        button = null;
        closeBtn = null;
        tab = null;
        dividerH = null;
        dividerV = null;
        barFrame = null;
        barFill = null;
        stepper = null;
        cursorSelect = null;
        skillMedallion = null;
        skillIcons = null;
        craftCauldron = null;
    }

    public int scale() { return scale; }
    public float modalRight(int viewWidth) {
        return Math.min(viewWidth * .94f, viewWidth - 78f * density);
    }

    public Paint textPaint(float sizePx, int color, boolean bold, Paint.Align align) {
        textPaint.setTextSize(sizePx);
        textPaint.setColor(color);
        textPaint.setTypeface(bold ? pixelFontBold : pixelFontRegular);
        textPaint.setTextAlign(align);
        return textPaint;
    }

    public void drawTextShadow(Canvas c, String text, float x, float baseline,
                               float sizePx, int color, boolean bold, Paint.Align align) {
        if (text == null || text.isEmpty()) return;
        float off = Math.max(1f, scale * 0.55f);
        textPaint(sizePx, 0xFF0B1A12, bold, align);
        c.drawText(text, x + off, baseline + off, textPaint);
        textPaint.setColor(color);
        c.drawText(text, x, baseline, textPaint);
    }

    public float drawWrappedText(Canvas c, String text, float cx, float startY,
                                 float maxWidth, float sizePx, int color, boolean bold, Paint.Align align) {
        if (text == null || text.isEmpty()) return startY;
        textPaint(sizePx, color, bold, align);
        float lineHeight = sizePx * 1.26f;
        float shadowOff = Math.max(1f, scale * 0.5f);
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float y = startY;
        for (String w : words) {
            String next = line.length() == 0 ? w : line + " " + w;
            if (textPaint.measureText(next) > maxWidth && line.length() > 0) {
                String out = line.toString();
                textPaint.setColor(0xFF0B1A12);
                c.drawText(out, cx + shadowOff, y + shadowOff, textPaint);
                textPaint.setColor(color);
                c.drawText(out, cx, y, textPaint);
                line.setLength(0);
                line.append(w);
                y += lineHeight;
            } else {
                line.setLength(0);
                line.append(next);
            }
        }
        if (line.length() > 0) {
            String out = line.toString();
            textPaint.setColor(0xFF0B1A12);
            c.drawText(out, cx + shadowOff, y + shadowOff, textPaint);
            textPaint.setColor(color);
            c.drawText(out, cx, y, textPaint);
            y += lineHeight;
        }
        return y;
    }

    public float centerBaseline(RectF r, float sizePx, boolean bold) {
        textPaint(sizePx, 0xFFFFFFFF, bold, Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        return r.centerY() - (fm.ascent + fm.descent) * 0.5f;
    }

    public void drawPanel(Canvas c, RectF r) {
        ensureModalBitmaps();
        nineSlice(c, panel, 0, PANEL_X, PANEL_Y, 10 * scale, r, 255);
    }

    public void drawSubPanel(Canvas c, RectF r) {
        float b = Math.max(2f, scale * 0.75f);
        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(0xFF0B1A12);
        c.drawRect(r, shapePaint);
        shapePaint.setColor(0xFF3A5E42);
        c.drawRect(r.left + b, r.top + b, r.right - b, r.bottom - b, shapePaint);
        shapePaint.setColor(0xFF10241A);
        c.drawRect(r.left + b * 2f, r.top + b * 2f, r.right - b * 2f, r.bottom - b * 2f, shapePaint);
    }

    public void drawPerfOverlay(Canvas c, RectF r, int alpha) {
        nineSlice(c, perfOverlay, 0, PERF_X, PERF_Y, 4 * scale, r, alpha);
    }

    public void drawTitleRibbon(Canvas c, float centerX, float top, float width, float height, String title) {
        ensureModalBitmaps();
        tmp.set(centerX - width * 0.5f, top, centerX + width * 0.5f, top + height);
        int capY = Math.round(Math.min(5f * scale, height * 0.27f));
        nineSlice(c, panelTitle, 0, TITLE_X, TITLE_Y, 10 * scale, capY, tmp, 255);
        float textSize = Math.max(14f, Math.min(height * 0.44f, width * 0.085f));
        float base = centerBaseline(tmp, textSize, true);
        drawTextShadow(c, title, tmp.centerX(), base, textSize, 0xFFFFF1B0, true, Paint.Align.CENTER);
    }

    public void drawTab(Canvas c, RectF r, int uiIconIdx, int craftIconIdx, String label, boolean active) {
        ensureModalBitmaps();
        int frame = active ? 0 : 1;
        int capY = Math.round(Math.min(5f * scale, r.height() * 0.25f));
        nineSlice(c, tab, frame * 40, TAB_X, TAB_Y, 8 * scale, capY, r, 255);
        boolean hasIcon = uiIconIdx >= 0 || craftIconIdx >= 0;
        float iconSize = hasIcon ? Math.min(r.height() * 0.56f, 14f * scale) : 0f;
        float textSize = Math.max(12f, Math.min(r.height() * 0.42f, 6.2f * scale));
        int textColor = active ? 0xFFFFF1B0 : 0xFFB8C99A;
        if (!hasIcon) {
            drawTextShadow(c, label, r.centerX(), centerBaseline(r, textSize, active),
                textSize, textColor, active, Paint.Align.CENTER);
            return;
        }
        textPaint(textSize, textColor, active, Paint.Align.LEFT);
        float textW = textPaint.measureText(label);
        float gap = 3.5f * scale;
        float totalW = iconSize + gap + textW;
        float startX = r.centerX() - totalW * 0.5f;
        float iy = r.centerY() - iconSize * 0.5f + (active ? 0f : scale * 0.5f);
        if (craftIconIdx >= 0) {
            drawCraftIcon(c, craftIconIdx, startX + iconSize * 0.5f, iy + iconSize * 0.5f, iconSize, 255);
        } else {
            drawUiIcon(c, uiIconIdx, startX + iconSize * 0.5f, iy + iconSize * 0.5f, iconSize, 255);
        }
        float base = centerBaseline(r, textSize, active) + (active ? 0f : scale * 0.5f);
        drawTextShadow(c, label, startX + iconSize + gap, base, textSize, textColor, active, Paint.Align.LEFT);
    }

    /** state: 0=normal, 1=pressed, 2=disabled */
    public void drawButton(Canvas c, RectF r, String label, int state) {
        drawButton(c, r, label, state, 0);
    }

    /** state: 0=normal, 1=pressed, 2=disabled */
    public void drawButton(Canvas c, RectF r, String label, int state, int textColorOverride) {
        ensureModalBitmaps();
        int clamped = Math.max(0, Math.min(2, state));
        int capX = Math.round(Math.min(9f * scale, r.width() * 0.22f));
        int capY = Math.round(Math.min(4f * scale, r.height() * 0.24f));
        nineSlice(c, button, clamped * 32, BTN_X, BTN_Y, capX, capY, r, 255);
        if (label == null || label.isEmpty()) return;
        int color = textColorOverride != 0 ? textColorOverride
            : clamped == 2 ? 0xFF7D8C78 : (clamped == 1 ? 0xFFFFF1B0 : 0xFFF4EBC8);
        float textSize = Math.max(13f, Math.min(r.height() * 0.44f, 6.8f * scale));
        float shift = clamped == 1 ? scale * 0.5f : 0f;
        drawTextShadow(c, label, r.centerX(), centerBaseline(r, textSize, true) + shift,
            textSize, color, true, Paint.Align.CENTER);
    }

    public void drawCloseButton(Canvas c, RectF r, boolean pressed) {
        ensureModalBitmaps();
        if (closeBtn == null) return;
        int frame = pressed ? 1 : 0;
        src.set(frame * 20, 0, (frame + 1) * 20, 20);
        c.drawBitmap(closeBtn, src, r, pixelPaint);
    }

    /** frame: 0=normal, 1=gold, 2=bright gold, 3=locked */
    public void drawSlot(Canvas c, RectF r, int frame, boolean cursorSelected) {
        if (slot != null) {
            int clamped = Math.max(0, Math.min(3, frame));
            src.set(clamped * 24, 0, (clamped + 1) * 24, 24);
            c.drawBitmap(slot, src, r, pixelPaint);
        }
        if (cursorSelected) drawCursor(c, r);
    }

    public void drawCursor(Canvas c, RectF r) {
        ensureModalBitmaps();
        if (cursorSelect == null) return;
        int f = (int) ((System.currentTimeMillis() / 280) % 2);
        src.set(f * 28, 0, (f + 1) * 28, 28);
        float pad = Math.max(2f, r.width() * 0.08f);
        dst.set(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad);
        c.drawBitmap(cursorSelect, src, dst, pixelPaint);
    }

    /** medallionState: 0=locked, 1=available, 2=unlocked, 3=maxed */
    public void drawSkillMedallion(Canvas c, RectF r, int medallionState, int skillIconIndex, boolean cursorSelected) {
        ensureModalBitmaps();
        if (skillMedallion != null) {
            int m = Math.max(0, Math.min(3, medallionState));
            src.set(m * 28, 0, (m + 1) * 28, 28);
            c.drawBitmap(skillMedallion, src, r, pixelPaint);
        }
        float iconSize = r.width() * 0.56f;
        int alpha = medallionState == 0 ? 150 : 255;
        drawSkillIcon(c, skillIconIndex, r.centerX(), r.centerY(), iconSize, alpha);
        if (cursorSelected) drawCursor(c, r);
    }

    public void drawStepper(Canvas c, RectF r, boolean enabled) {
        ensureModalBitmaps();
        if (stepper == null) return;
        int frame = enabled ? 0 : 2;
        src.set(frame * 20, 0, (frame + 1) * 20, 20);
        c.drawBitmap(stepper, src, r, pixelPaint);
    }

    /** tone: 0=green, 1=gold, 2=amber */
    public void drawBar(Canvas c, RectF r, float ratio, int tone) {
        ensureModalBitmaps();
        float clampedRatio = Math.max(0f, Math.min(1f, ratio));
        float capX = Math.min(12f * scale, r.width() * 0.22f);
        float capY = Math.min(3f * scale, r.height() * 0.26f);
        nineSlice(c, barFrame, 0, BAR_X, BAR_Y, Math.round(capX), Math.round(capY), r, 255);
        float fillLeft = r.left + Math.min(10f * scale, r.width() * 0.14f);
        float fillRight = r.right - Math.min(10f * scale, r.width() * 0.14f);
        float fillTop = r.top + capY;
        float fillBottom = r.bottom - capY;
        if (barFill != null && clampedRatio > 0f && fillRight > fillLeft && fillBottom > fillTop) {
            int f = Math.max(0, Math.min(2, tone));
            src.set(f * 16 + 1, 0, (f + 1) * 16 - 1, 6);
            dst.set(fillLeft, fillTop, fillLeft + (fillRight - fillLeft) * clampedRatio, fillBottom);
            c.drawBitmap(barFill, src, dst, pixelPaint);
        }
    }

    public void drawDividerH(Canvas c, float left, float right, float centerY) {
        ensureModalBitmaps();
        if (dividerH == null || right <= left) return;
        int s = Math.max(1, scale - 1);
        float h = 6f * s;
        float top = centerY - h * 0.5f;
        float leafW = 16f * s;
        if (right - left <= leafW * 1.4f) {
            src.set(0, 0, 64, 6);
            dst.set(left, top, right, top + h);
            c.drawBitmap(dividerH, src, dst, pixelPaint);
            return;
        }
        float midL = (left + right) * 0.5f - leafW * 0.5f;
        float midR = midL + leafW;
        tileHorizontal(c, dividerH, 10, 0, 22, 6, left, top, midL, top + h, s);
        src.set(24, 0, 40, 6);
        dst.set(midL, top, midR, top + h);
        c.drawBitmap(dividerH, src, dst, pixelPaint);
        tileHorizontal(c, dividerH, 42, 0, 54, 6, midR, top, right, top + h, s);
    }

    public void drawDividerV(Canvas c, float centerX, float top, float bottom) {
        ensureModalBitmaps();
        if (dividerV == null || bottom <= top) return;
        int s = Math.max(1, scale - 1);
        float w = 6f * s;
        float left = centerX - w * 0.5f;
        float leafH = 16f * s;
        if (bottom - top <= leafH * 1.4f) {
            src.set(0, 0, 6, 64);
            dst.set(left, top, left + w, bottom);
            c.drawBitmap(dividerV, src, dst, pixelPaint);
            return;
        }
        float midT = (top + bottom) * 0.5f - leafH * 0.5f;
        float midB = midT + leafH;
        tileVertical(c, dividerV, 0, 10, 6, 22, left, top, left + w, midT, s);
        src.set(0, 24, 6, 40);
        dst.set(left, midT, left + w, midB);
        c.drawBitmap(dividerV, src, dst, pixelPaint);
        tileVertical(c, dividerV, 0, 42, 6, 54, left, midB, left + w, bottom, s);
    }

    public void drawUiIcon(Canvas c, int index, float cx, float cy, float size, int alpha) {
        if (icons == null) return;
        int idx = Math.max(0, Math.min(17, index));
        src.set(idx * 16, 0, (idx + 1) * 16, 16);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        pixelPaint.setAlpha(alpha);
        c.drawBitmap(icons, src, dst, pixelPaint);
        pixelPaint.setAlpha(255);
    }

    public void drawSkillIcon(Canvas c, int index, float cx, float cy, float size, int alpha) {
        ensureModalBitmaps();
        if (skillIcons == null) return;
        int idx = Math.max(0, Math.min(8, index));
        src.set(idx * 20, 0, (idx + 1) * 20, 20);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        pixelPaint.setAlpha(alpha);
        c.drawBitmap(skillIcons, src, dst, pixelPaint);
        pixelPaint.setAlpha(255);
    }

    public void drawCraftIcon(Canvas c, int index, float cx, float cy, float size, int alpha) {
        if (craftIcons == null) return;
        int idx = Math.max(0, Math.min(5, index));
        src.set(idx * 20, 0, (idx + 1) * 20, 20);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        pixelPaint.setAlpha(alpha);
        c.drawBitmap(craftIcons, src, dst, pixelPaint);
        pixelPaint.setAlpha(255);
    }

    public void drawCauldron(Canvas c, boolean activeBurst, float cx, float cy, float size) {
        ensureModalBitmaps();
        if (craftCauldron == null) return;
        int frame = activeBurst ? 1 : 0;
        src.set(frame * 48, 0, (frame + 1) * 48, 48);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        c.drawBitmap(craftCauldron, src, dst, pixelPaint);
    }

    public void drawPickupIcon(Canvas c, int index, float cx, float cy, float size, int alpha) {
        if (pickupIcons == null) return;
        int idx = Math.max(0, Math.min(7, index));
        src.set(idx * 32, 0, (idx + 1) * 32, 32);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        pixelPaint.setAlpha(alpha);
        c.drawBitmap(pickupIcons, src, dst, pixelPaint);
        pixelPaint.setAlpha(255);
    }

    public void drawWeaponIcon(Canvas c, WeaponDefinition weapon, float cx, float cy, float size) {
        if (weaponIcons == null || weapon == null) return;
        int idx = 0;
        if (weapon == WeaponLoadout.COCONUT) idx = 1;
        else if (weapon.type == WeaponType.CATALYST) idx = 3;
        else if (weapon.type == WeaponType.PHYSICAL) idx = 4;
        src.set(idx * 16, 0, (idx + 1) * 16, 16);
        dst.set(cx - size * 0.5f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.5f);
        c.drawBitmap(weaponIcons, src, dst, pixelPaint);
    }

    /** Renders any registered catalog item ID using ui_craft_icons or emerald_pickups. */
    public void drawItemIcon(Canvas c, String itemId, float cx, float cy, float size, int alpha) {
        int lootFrame = com.example.amazonMages.items.ItemCatalog.boitataLootFrame(itemId);
        if (lootFrame >= 0 && boitataLootIcons != null) {
            src.set(lootFrame * 48, 0, (lootFrame + 1) * 48, 48);
            dst.set(cx - size * .5f, cy - size * .5f, cx + size * .5f, cy + size * .5f);
            pixelPaint.setAlpha(alpha);
            c.drawBitmap(boitataLootIcons, src, dst, pixelPaint);
            pixelPaint.setAlpha(255);
            return;
        }
        if ("elixir".equals(itemId)) { drawCraftIcon(c, CRAFT_ICON_ELIXIR, cx, cy, size, alpha); return; }
        if ("bomba_coco".equals(itemId)) { drawCraftIcon(c, CRAFT_ICON_BOMB, cx, cy, size, alpha); return; }
        if ("tonico".equals(itemId)) { drawCraftIcon(c, CRAFT_ICON_TONIC, cx, cy, size, alpha); return; }
        if ("runa_ancestral".equals(itemId)) { drawCraftIcon(c, CRAFT_ICON_RUNE, cx, cy, size, alpha); return; }
        if ("mana".equals(itemId)) { drawPickupIcon(c, 4, cx, cy, size, alpha); return; }
        if ("bencao".equals(itemId)) { drawPickupIcon(c, 5, cx, cy, size, alpha); return; }
        if ("cajado".equals(itemId)) { drawPickupIcon(c, 6, cx, cy, size, alpha); return; }
        if ("coco".equals(itemId)) { drawPickupIcon(c, 7, cx, cy, size, alpha); return; }
        drawPickupIcon(c, 3, cx, cy, size, alpha); // erva
    }

    private void nineSlice(Canvas c, Bitmap bmp, int offsetX, int[] sx, int[] sy,
                           int corner, RectF r, int alpha) {
        nineSlice(c, bmp, offsetX, sx, sy, corner, corner, r, alpha);
    }

    private void nineSlice(Canvas c, Bitmap bmp, int offsetX, int[] sx, int[] sy,
                           int cornerX, int cornerY, RectF r, int alpha) {
        if (bmp == null || r.isEmpty()) return;
        float cx = Math.min(cornerX, r.width() * 0.5f);
        float cy = Math.min(cornerY, r.height() * 0.5f);
        dx[0] = r.left; dx[1] = r.left + cx; dx[2] = r.right - cx; dx[3] = r.right;
        dy[0] = r.top; dy[1] = r.top + cy; dy[2] = r.bottom - cy; dy[3] = r.bottom;
        pixelPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float dl = dx[col], dt = dy[row], dr = dx[col + 1], db = dy[row + 1];
                if (dr - dl <= 0.5f || db - dt <= 0.5f) continue;
                int sl = offsetX + sx[col], st = sy[row], sr = offsetX + sx[col + 1], sb = sy[row + 1];
                if (col == 1 && row != 1) {
                    tileHorizontal(c, bmp, sl, st, sr, sb, dl, dt, dr, db, scale);
                } else if (row == 1 && col != 1) {
                    tileVertical(c, bmp, sl, st, sr, sb, dl, dt, dr, db, scale);
                } else {
                    src.set(sl, st, sr, sb);
                    dst.set(dl, dt, dr, db);
                    c.drawBitmap(bmp, src, dst, pixelPaint);
                }
            }
        }
        pixelPaint.setAlpha(255);
    }

    private void tileHorizontal(Canvas c, Bitmap bmp, int sl, int st, int sr, int sb,
                                float dl, float dt, float dr, float db, int tileScale) {
        int sw = sr - sl;
        float step = Math.max(4f, sw * tileScale);
        float x = dl;
        while (x < dr - 0.5f) {
            float segW = Math.min(step, dr - x);
            int srcStop = sl + Math.max(1, Math.round(sw * (segW / step)));
            src.set(sl, st, Math.min(sr, srcStop), sb);
            dst.set(x, dt, x + segW, db);
            c.drawBitmap(bmp, src, dst, pixelPaint);
            x += segW;
        }
    }

    private void tileVertical(Canvas c, Bitmap bmp, int sl, int st, int sr, int sb,
                              float dl, float dt, float dr, float db, int tileScale) {
        int sh = sb - st;
        float step = Math.max(4f, sh * tileScale);
        float y = dt;
        while (y < db - 0.5f) {
            float segH = Math.min(step, db - y);
            int srcStop = st + Math.max(1, Math.round(sh * (segH / step)));
            src.set(sl, st, sr, Math.min(sb, srcStop));
            dst.set(dl, y, dr, y + segH);
            c.drawBitmap(bmp, src, dst, pixelPaint);
            y += segH;
        }
    }
}

