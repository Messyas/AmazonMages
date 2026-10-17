package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.combat.WeaponDefinition;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.combat.WeaponType;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.resources.ResourceManager;

/** Pixel-art HUD renderer. All source rectangles stay on native atlas pixels. */
public final class ForestHud {
    public static final int NAV_ATTRIBUTES = 0;
    public static final int NAV_INVENTORY = 1;
    public static final int NAV_MAP = 2;
    public static final int NAV_QUESTS = 3;
    public static final int NAV_CHAT = 4;
    private static final int ICON_ATTACK = 0, ICON_SPECIAL = 1, ICON_SWAP = 2,
        ICON_SHIELD = 3, ICON_POTION = 4, ICON_TALK = 5, ICON_ATTRIBUTES = 6,
        ICON_BAG = 7, ICON_MAP = 8, ICON_QUEST = 9;

    private final Bitmap hotbarFrame, slots, hearts, mana, xp, posture, buttons, icons, weaponIcons, panel;
    private final Paint paint = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final int pixelScale;
    private final float density;

    public ForestHud(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        hotbarFrame = resources.bitmap(R.drawable.hud_hotbar_frame, false);
        slots = resources.bitmap(R.drawable.hud_slot, false);
        hearts = resources.bitmap(R.drawable.hud_heart, false);
        mana = resources.bitmap(R.drawable.hud_mana, false);
        xp = resources.bitmap(R.drawable.hud_xp_bar, false);
        posture = resources.bitmap(R.drawable.hud_posture_bar, false);
        buttons = resources.bitmap(R.drawable.hud_button, false);
        icons = resources.bitmap(R.drawable.hud_icons, false);
        weaponIcons = resources.bitmap(R.drawable.hud_weapon_icons, false);
        panel = resources.bitmap(R.drawable.hud_panel, false);
        density = context.getResources().getDisplayMetrics().density;
        pixelScale = Math.max(2, Math.min(4, Math.round(density)));
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public int pixelScale() { return pixelScale; }

    public void draw(Canvas c, int width, int height, Player player, PlayerProgression progression,
                     WeaponLoadout weapons, boolean specialAvailable,
                     int parryCooldownTicks, boolean developerChatAvailable) {
        drawStatus(c, width, player, progression);
        drawHotbar(c, width, height, weapons);
        drawActions(c, width, height, weapons, specialAvailable, parryCooldownTicks);
        drawNavigation(c, width, developerChatAvailable);
    }

    private void drawStatus(Canvas c, int width, Player player, PlayerProgression progression) {
        int s = pixelScale;
        int left = 7 * s, top = 6 * s;
        float panelWidth = Math.min(96 * s, Math.max(88 * s, width * .18f));
        drawPanel(c, left - 4 * s, top - 3 * s, left + panelWidth, top + 33 * s);
        int maxHearts = 5;
        double perHeart = Math.max(1.0, player.getMaximumHealth() / (double) maxHearts);
        for (int i = 0; i < maxHearts; i++) {
            double amount = player.getHealthPoint() - i * perHeart;
            int frame = amount >= perHeart ? 0 : amount >= perHeart * .5 ? 1 : 2;
            if (player.getHealthPoint() <= 1 && (System.currentTimeMillis() / 180 % 2 == 0)) frame = 3;
            sprite(c, hearts, frame * 9, 0, 9, 9, left + i * 11 * s, top, 9 * s, 9 * s);
        }
        int manaSlots = 7;
        double manaPer = progression.maxMana / manaSlots;
        for (int i = 0; i < manaSlots; i++) {
            double amount = progression.mana - i * manaPer;
            int frame = amount >= manaPer ? 0 : amount >= manaPer * .5 ? 1 : 2;
            sprite(c, mana, frame * 9, 0, 9, 9, left + i * 11 * s, top + 12 * s, 9 * s, 9 * s);
        }
        float barX = left, barY = top + 25 * s;
        float barWidth = Math.min(150 * s, Math.max(88 * s, width * .19f));
        drawNineSliceBar(c, xp, barX, barY, barWidth, 5 * s,
            progression.experience / (double) PlayerProgression.experienceToNextLevel(progression.level),
            new Rect(10, 0, 23, 13), progression.level);
    }

    private void drawHotbar(Canvas c, int width, int height, WeaponLoadout weapons) {
        int s = pixelScale;
        int x = 3 * s;
        int frameY = (height - 112 * s) / 2;
        sprite(c, hotbarFrame, 0, 0, 28, 112, x, frameY, 28 * s, 112 * s);
        for (int i = 0; i < 4; i++) {
            int state = weapons.at(i) == null ? 3 : (i == weapons.selectedSlot() ? 1 : 0);
            int sy = state * 24;
            int y = frameY + (4 + i * 25) * s;
            sprite(c, slots, 0, sy, 24, 24, x + 2 * s, y, 24 * s, 24 * s);
            WeaponDefinition weapon = weapons.at(i);
            if (weapon != null) {
                int iconIndex = weaponIconIndex(weapon);
                sprite(c, weaponIcons, iconIndex * 16, 0, 16, 16,
                    x + 6 * s, y + 4 * s, 16 * s, 16 * s);
            }
            if (i == weapons.selectedSlot() && weapons.specialCooldown() > 0 && weapon == WeaponLoadout.COCONUT) {
                float ratio = Math.min(1f, weapons.specialCooldown() / 1080f);
                paint.setColor(0xA8202520);
                c.drawRect(x + 2 * s, y + 24 * s * (1 - ratio), x + 26 * s, y + 24 * s, paint);
            }
        }
    }

    private void drawActions(Canvas c, int width, int height, WeaponLoadout weapons,
                             boolean specialAvailable, int parryCooldownTicks) {
        drawParryButton(c, width * .64f, height * .86f, parryCooldownTicks);
        // The attack button is now the knob of the aim stick (see drawAimKnob).
        drawButton(c, width * .77f, height * .86f, 38, ICON_SPECIAL, !specialAvailable);
        drawButton(c, width * .88f, height * .60f, 38, ICON_SWAP, false);
    }

    /** Attack button drawn at the aim stick's knob, so it moves with the finger. */
    public void drawAimKnob(Canvas c, float cx, float cy, float diameter) {
        float icon = diameter * 16f / 38f;
        sprite(c, buttons, 0, 0, 32, 32, cx - diameter / 2, cy - diameter / 2, diameter, diameter);
        sprite(c, icons, ICON_ATTACK * 16, 0, 16, 16, cx - icon / 2, cy - icon / 2, icon, icon);
    }

    private void drawButton(Canvas c, float cx, float cy, int size, int iconIndex, boolean disabled) {
        int s = pixelScale;
        int state = disabled ? 2 : 0;
        float extent = size * s;
        sprite(c, buttons, state * 32, 0, 32, 32,
            cx - extent / 2, cy - extent / 2, extent, extent);
        sprite(c, icons, iconIndex * 16, 0, 16, 16,
            cx - 8 * s, cy - 8 * s, 16 * s, 16 * s);
    }

    private void drawParryButton(Canvas c, float cx, float cy, int cooldownTicks) {
        int s = pixelScale;
        int cooldown = Math.max(0, Math.min(Player.PARRY_COOLDOWN_TICKS, cooldownTicks));
        drawButton(c, cx, cy, 38, ICON_SHIELD, cooldown > 0);
        if (cooldown == 0) return;
        float radius = 22 * s;
        float stroke = Math.max(2 * density, 2.5f * s);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setColor(0xC51B382A);
        c.drawCircle(cx, cy, radius, paint);
        paint.setColor(0xFF9EE6A8);
        c.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
            -90, 360 * cooldown / (float) Player.PARRY_COOLDOWN_TICKS, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawNavigation(Canvas c, int width, boolean developerChatAvailable) {
        int s = pixelScale;
        int target = Math.max(48, Math.round(48 * density));
        int gap = target;
        // Reserve the Android pause control in the far top-right corner.
        int count = developerChatAvailable ? 5 : 4;
        int left = width - count * gap - Math.round(72 * density);
        int top = Math.round(5 * density);
        int[] ids = developerChatAvailable ?
            new int[]{ICON_TALK, ICON_ATTRIBUTES, ICON_BAG, ICON_MAP, ICON_QUEST} :
            new int[]{ICON_ATTRIBUTES, ICON_BAG, ICON_MAP, ICON_QUEST};
        for (int i = 0; i < ids.length; i++) {
            float x = left + i * gap;
            drawPanel(c, x + target * .13f, top, x + target * .87f, top + target * .87f);
            sprite(c, icons, ids[i] * 16, 0, 16, 16,
                x + (target - 16 * s) / 2f, top + (target - 16 * s) / 2f,
                16 * s, 16 * s);
        }
    }

    public int navigationAt(float x, float y, int width, boolean developerChatAvailable) {
        int target = Math.max(48, Math.round(48 * density));
        int gap = target;
        int count = developerChatAvailable ? 5 : 4;
        int left = width - count * gap - Math.round(72 * density);
        int top = Math.round(5 * density);
        if (y < top || y >= top + target) return -1;
        int index = (int) ((x - left) / gap);
        if (x < left || index < 0 || index >= count) return -1;
        if (developerChatAvailable) return index == 0 ? NAV_CHAT : index - 1;
        return index;
    }

    public int hotbarSlotAt(float x, float y, int height) {
        int s = pixelScale;
        int left = 3 * s;
        int top = (height - 112 * s) / 2;
        if (x < left || x >= left + 28 * s || y < top + 4 * s || y >= top + 108 * s) return -1;
        int slot = (int) ((y - (top + 4 * s)) / (25f * s));
        return slot >= 0 && slot < 4 ? slot : -1;
    }

    public void drawBossBar(Canvas c, float left, float top, float width, float ratio) {
        drawNineSliceBar(c, posture, left, top, width, 8 * pixelScale, ratio, null, 0);
    }

    public void drawPanel(Canvas c, float left, float top, float right, float bottom) {
        drawPanel(c, left, top, right, bottom, 255);
    }

    public void drawPanel(Canvas c, float left, float top, float right, float bottom, int alpha) {
        if (panel == null) return;
        paint.setAlpha(Math.max(0, Math.min(255, alpha)));
        nineSlice(c, panel, left, top, right, bottom, 8 * pixelScale);
        paint.setAlpha(255);
    }

    private void drawNineSliceBar(Canvas c, Bitmap atlas, float left, float top, float width,
                                  float height, double ratio, Rect badge, int level) {
        float badgeW = badge == null ? 0 : 13 * pixelScale;
        float barW = Math.max(9 * pixelScale, width - badgeW + (badgeW == 0 ? 0 : 2 * pixelScale));
        // The first 9x5 pixels are left/middle/right caps. Scale the middle region horizontally.
        slice(c, atlas, new Rect(0,0,4,5), left, top, 4*pixelScale, height);
        slice(c, atlas, new Rect(4,0,5,5), left+4*pixelScale, top,
            barW-8*pixelScale, height);
        slice(c, atlas, new Rect(5,0,9,5), left+barW-4*pixelScale, top, 4*pixelScale, height);
        float innerX = left + 3 * pixelScale;
        float innerWidth = Math.max(0, (barW - 6 * pixelScale) * (float)Math.max(0,Math.min(1,ratio)));
        if (innerWidth > 0) slice(c, atlas, new Rect(9,0,10,3), innerX, top + pixelScale,
            innerWidth, Math.max(1, height - 2 * pixelScale));
        if (badge != null) slice(c, atlas, badge, left + barW - pixelScale, top - 4 * pixelScale,
            badgeW, 13 * pixelScale);
        if (badge != null && level > 0) {
            paint.setColor(0xFFFFE99A);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(5 * pixelScale);
            paint.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD));
            c.drawText(Integer.toString(level), left + barW + 5.5f * pixelScale,
                top + 8.8f * pixelScale, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(null);
        }
    }

    private void nineSlice(Canvas c, Bitmap image, float l, float t, float r, float b, int corner) {
        float cs = Math.min(corner, Math.min((r-l)/2, (b-t)/2));
        int k = 8;
        // 3x3 source grid, with 8px corners and 8px edges; center stretches.
        float[] xs = {l,l+cs,r-cs,r}; float[] ys = {t,t+cs,b-cs,b};
        int[] sx = {0,k,24-k,24}; int[] sy = {0,k,24-k,24};
        for (int row=0;row<3;row++) for(int col=0;col<3;col++)
            slice(c,image,new Rect(sx[col],sy[row],sx[col+1],sy[row+1]),
                xs[col],ys[row],xs[col+1]-xs[col],ys[row+1]-ys[row]);
    }

    private void sprite(Canvas c, Bitmap bitmap, int sx, int sy, int sw, int sh,
                        float x, float y, float w, float h) {
        if (bitmap == null) return;
        src.set(sx, sy, sx + sw, sy + sh); dst.set(x, y, x + w, y + h);
        c.drawBitmap(bitmap, src, dst, paint);
    }

    private void slice(Canvas c, Bitmap bitmap, Rect source, float x, float y, float w, float h) {
        if (bitmap == null || w <= 0 || h <= 0) return;
        dst.set(x,y,x+w,y+h); c.drawBitmap(bitmap, source, dst, paint);
    }

    private static int weaponIconIndex(WeaponDefinition weapon) {
        if (weapon == WeaponLoadout.COCONUT) return 1;
        WeaponType type = weapon.type;
        if (type == WeaponType.CATALYST) return 3;
        if (type == WeaponType.PHYSICAL) return 4;
        return 0;
    }
}
