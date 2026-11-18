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
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.resources.ResourceManager;
import com.example.amazonMages.ui.PixelUiKit;

/** Pixel-art HUD renderer. All source rectangles stay on native atlas pixels. */
public final class ForestHud {
    public static final int NAV_ATTRIBUTES = 0;
    public static final int NAV_INVENTORY = 1;
    public static final int NAV_MAP = 2;
    public static final int NAV_QUESTS = 3;
    public static final int NAV_CHAT = 4;
    public static final int NAV_CRAFT = 5;

    private static final int ICON_ATTACK = 0, ICON_SPECIAL = 1, ICON_SWAP = 2,
        ICON_SHIELD = 3, ICON_POTION = 4;

    private final Bitmap hotbarFrame, slots, hearts, mana, xp, posture, buttons, icons, weaponIcons, panel, dashIcon;
    private final PixelUiKit uiKit;
    private final Paint paint = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final RectF panelRect = new RectF();
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
        dashIcon = resources.bitmap(R.drawable.emerald_vfx_dash, false);
        uiKit = new PixelUiKit(context);
        density = context.getResources().getDisplayMetrics().density;
        pixelScale = Math.max(2, Math.min(4, Math.round(density)));
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public int pixelScale() { return pixelScale; }
    public PixelUiKit uiKit() { return uiKit; }

    public void draw(Canvas c, int width, int height, Player player, PlayerProgression progression,
                     WeaponLoadout weapons, boolean specialAvailable,
                     int parryCooldownTicks, boolean developerChatAvailable) {
        draw(c, width, height, player, progression, weapons, specialAvailable,
            parryCooldownTicks, developerChatAvailable, 0, 1, 0, null, 0, 0);
    }

    public void draw(Canvas c, int width, int height, Player player, PlayerProgression progression,
                     WeaponLoadout weapons, boolean specialAvailable,
                     int parryCooldownTicks, boolean developerChatAvailable,
                     int survivalTicks, int waveNumber, int killCount, String bannerText) {
        draw(c, width, height, player, progression, weapons, specialAvailable,
            parryCooldownTicks, developerChatAvailable, survivalTicks, waveNumber, killCount,
            bannerText, 0, 0);
    }

    public void draw(Canvas c, int width, int height, Player player, PlayerProgression progression,
                     WeaponLoadout weapons, boolean specialAvailable,
                     int parryCooldownTicks, boolean developerChatAvailable,
                     int survivalTicks, int waveNumber, int killCount, String bannerText,
                     int coconutCount, int quickItemCount) {
        float statusRight = drawStatus(c, width, player, progression);
        drawHotbar(c, width, height, weapons, coconutCount);
        drawActions(c, width, height, player, weapons, specialAvailable, parryCooldownTicks, quickItemCount);
        boolean pendingSkill = progression != null && (progression.skillPoints > 0 || progression.attributePoints > 0);
        float navLeft = drawNavigation(c, width, developerChatAvailable, pendingSkill);
        drawSurvivorStrip(c, width, statusRight, navLeft, survivalTicks, waveNumber, killCount, bannerText);
    }

    private void drawSurvivorStrip(Canvas c, int width, float statusRight, float navLeft,
                                   int survivalTicks, int waveNumber, int killCount, String bannerText) {
        int s = pixelScale;
        int totalSec = Math.max(0, survivalTicks / 60);
        String timeStr = String.format(java.util.Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60);
        String summary = timeStr + "  ·  ONDA " + waveNumber + "  ·  " + killCount + " ABATES";
        float freeLeft = statusRight + 6f * s;
        float freeRight = navLeft - 6f * s;
        float freeW = freeRight - freeLeft;
        if (freeW >= 66f * s) {
            float panelW = Math.min(freeW, 134f * s);
            float cx = (freeLeft + freeRight) * 0.5f;
            float left = cx - panelW * 0.5f;
            float top = 6f * s;
            drawPanel(c, left, top, left + panelW, top + 18f * s, 236);
            float fontSize = Math.max(12.5f, Math.min(6.8f * s, panelW * 0.088f));
            uiKit.drawTextShadow(c, summary, cx, top + 11.8f * s, fontSize, 0xFFF4EBC8, true, Paint.Align.CENTER);
        }
        if (bannerText != null && !bannerText.isEmpty()) {
            float bw = Math.min(width * .56f, 168f * s);
            float bl = (width - bw) * .5f;
            float bt = 27f * s;
            drawPanel(c, bl, bt, bl + bw, bt + 16f * s, 240);
            uiKit.drawTextShadow(c, bannerText, width * .5f, bt + 10.4f * s, 6f * s, 0xFFFFE082, true, Paint.Align.CENTER);
        }
    }

    private float drawStatus(Canvas c, int width, Player player, PlayerProgression progression) {
        int s = pixelScale;
        int left = 7 * s, top = 6 * s;
        float panelRight = left + Math.min(96 * s, Math.max(88 * s, width * .18f));
        drawPanel(c, left - 4 * s, top - 3 * s, panelRight, top + 40 * s);
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
        float barX = left, barY = top + 24 * s;
        float barWidth = Math.min(150 * s, Math.max(88 * s, width * .19f));
        drawNineSliceBar(c, xp, barX, barY, barWidth, 5 * s,
            progression.experience / (double) PlayerProgression.experienceToNextLevel(progression.level),
            new Rect(10, 0, 23, 13), progression.level);
        double postureRatio = player.getPosture().getCurrent() / Math.max(1.0, player.getPosture().getMaximum());
        drawNineSliceBar(c, posture, barX, barY + 8 * s, barWidth * .78f, 4 * s, postureRatio, null, 0);
        return panelRight;
    }

    private void drawHotbar(Canvas c, int width, int height, WeaponLoadout weapons, int coconutCount) {
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
                if (weapon == WeaponLoadout.COCONUT && coconutCount > 0) {
                    drawCountBadge(c, x + 24 * s, y + 22 * s, "x" + coconutCount, s);
                }
            }
            if (i == weapons.selectedSlot() && weapons.specialCooldown() > 0 && weapon != null) {
                float ratio = Math.min(1f, weapons.specialCooldown() / (float) WeaponLoadout.SPECIAL_COOLDOWN_TICKS);
                paint.setColor(0xA8202520);
                c.drawRect(x + 2 * s, y + 24 * s * (1 - ratio), x + 26 * s, y + 24 * s, paint);
            }
        }
    }

    private void drawActions(Canvas c, int width, int height, Player player, WeaponLoadout weapons,
                             boolean specialAvailable, int parryCooldownTicks, int quickItemCount) {
        int s = pixelScale;
        float qx = width * .53f, qy = height * .86f;
        drawButton(c, qx, qy, 34, ICON_POTION, quickItemCount <= 0);
        if (quickItemCount > 0) {
            drawCountBadge(c, qx + 14 * s, qy + 15 * s, Integer.toString(quickItemCount), s);
        }

        drawParryButton(c, width * .64f, height * .86f, parryCooldownTicks);
        drawDashButton(c, width * .71f, height * .68f, player);
        drawButton(c, width * .77f, height * .86f, 38, ICON_SPECIAL, !specialAvailable);
        drawButton(c, width * .88f, height * .58f, 38, ICON_SWAP, false);
    }

    private void drawDashButton(Canvas c, float cx, float cy, Player player) {
        int s = pixelScale;
        int cd = player.getDashCooldownTicks();
        int max = Math.max(1, player.getDashCooldownMax());
        float extent = 34 * s;
        sprite(c, buttons, (cd > 0 ? 2 : 0) * 32, 0, 32, 32,
            cx - extent / 2, cy - extent / 2, extent, extent);
        if (dashIcon != null) {
            src.set(64, 0, 128, 64);
            dst.set(cx - 10 * s, cy - 10 * s, cx + 10 * s, cy + 10 * s);
            c.drawBitmap(dashIcon, src, dst, paint);
        } else {
            sprite(c, icons, ICON_SWAP * 16, 0, 16, 16, cx - 8 * s, cy - 8 * s, 16 * s, 16 * s);
        }
        if (cd > 0) {
            float radius = 19 * s;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2 * density, 2.2f * s));
            paint.setStrokeCap(Paint.Cap.SQUARE);
            paint.setColor(0xC51B382A);
            c.drawCircle(cx, cy, radius, paint);
            paint.setColor(0xFFF2D36B);
            c.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                -90, 360 * cd / (float) max, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
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

    private void drawCountBadge(Canvas c, float rightX, float bottomY, String text, int s) {
        uiKit.drawTextShadow(c, text, rightX, bottomY, 4.9f * s, 0xFFFFF1B0, true, Paint.Align.RIGHT);
    }

    private int navTargetSize() {
        return Math.max(44, Math.round(44 * density));
    }

    private int navLeftEdge(int width, int count) {
        int gap = navTargetSize() + Math.round(3 * density);
        return width - count * gap - Math.round(74 * density);
    }

    private int[] navOrder(boolean developerChatAvailable) {
        return developerChatAvailable
            ? new int[]{NAV_CHAT, NAV_ATTRIBUTES, NAV_INVENTORY, NAV_CRAFT, NAV_MAP, NAV_QUESTS}
            : new int[]{NAV_ATTRIBUTES, NAV_INVENTORY, NAV_CRAFT, NAV_MAP, NAV_QUESTS};
    }

    private float drawNavigation(Canvas c, int width, boolean developerChatAvailable, boolean pendingSkill) {
        int target = navTargetSize();
        int gap = target + Math.round(3 * density);
        int[] order = navOrder(developerChatAvailable);
        int left = navLeftEdge(width, order.length);
        int top = Math.round(6 * density);
        for (int i = 0; i < order.length; i++) {
            float x = left + i * gap;
            float pad = target * 0.06f;
            panelRect.set(x + pad, top, x + target - pad, top + target - pad * 2f);
            uiKit.drawSlot(c, panelRect, 0, false);
            float iconSize = panelRect.width() * 0.58f;
            int action = order[i];
            if (action == NAV_CRAFT) {
                uiKit.drawCraftIcon(c, PixelUiKit.CRAFT_ICON_CAULDRON,
                    panelRect.centerX(), panelRect.centerY(), iconSize, 255);
            } else {
                int uiIcon;
                if (action == NAV_CHAT) uiIcon = PixelUiKit.ICON_CHAT;
                else if (action == NAV_ATTRIBUTES) uiIcon = PixelUiKit.ICON_ATTRIBUTES;
                else if (action == NAV_INVENTORY) uiIcon = PixelUiKit.ICON_BAG;
                else if (action == NAV_MAP) uiIcon = PixelUiKit.ICON_MAP;
                else uiIcon = PixelUiKit.ICON_JOURNAL;
                uiKit.drawUiIcon(c, uiIcon, panelRect.centerX(), panelRect.centerY(), iconSize, 255);
            }
            if (action == NAV_ATTRIBUTES && pendingSkill && (System.currentTimeMillis() / 300) % 2 == 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF0B1A12);
                c.drawRect(panelRect.right - target * .26f, panelRect.top + 1f,
                    panelRect.right - 1f, panelRect.top + target * .26f, paint);
                paint.setColor(0xFFF2D36B);
                c.drawRect(panelRect.right - target * .23f, panelRect.top + 3f,
                    panelRect.right - 3f, panelRect.top + target * .23f, paint);
            }
        }
        return left;
    }

    public int navigationAt(float x, float y, int width, boolean developerChatAvailable) {
        int target = navTargetSize();
        int gap = target + Math.round(3 * density);
        int[] order = navOrder(developerChatAvailable);
        int left = navLeftEdge(width, order.length);
        int top = Math.round(6 * density);
        if (y < top || y >= top + target) return -1;
        int index = (int) ((x - left) / gap);
        if (x < left || index < 0 || index >= order.length) return -1;
        return order[index];
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
        slice(c, atlas, new Rect(0, 0, 4, 5), left, top, 4 * pixelScale, height);
        slice(c, atlas, new Rect(4, 0, 5, 5), left + 4 * pixelScale, top,
            barW - 8 * pixelScale, height);
        slice(c, atlas, new Rect(5, 0, 9, 5), left + barW - 4 * pixelScale, top, 4 * pixelScale, height);
        float innerX = left + 3 * pixelScale;
        float innerWidth = Math.max(0, (barW - 6 * pixelScale) * (float) Math.max(0, Math.min(1, ratio)));
        if (innerWidth > 0) slice(c, atlas, new Rect(9, 0, 10, 3), innerX, top + pixelScale,
            innerWidth, Math.max(1, height - 2 * pixelScale));
        if (badge != null) slice(c, atlas, badge, left + barW - pixelScale, top - 4 * pixelScale,
            badgeW, 13 * pixelScale);
        if (badge != null && level > 0) {
            uiKit.drawTextShadow(c, Integer.toString(level), left + barW + 5.5f * pixelScale,
                top + 8.8f * pixelScale, 5.2f * pixelScale, 0xFFFFE99A, true, Paint.Align.CENTER);
        }
    }

    private void nineSlice(Canvas c, Bitmap image, float l, float t, float r, float b, int corner) {
        float cs = Math.min(corner, Math.min((r - l) / 2, (b - t) / 2));
        int k = 8;
        float[] xs = {l, l + cs, r - cs, r}; float[] ys = {t, t + cs, b - cs, b};
        int[] sx = {0, k, 24 - k, 24}; int[] sy = {0, k, 24 - k, 24};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            slice(c, image, new Rect(sx[col], sy[row], sx[col + 1], sy[row + 1]),
                xs[col], ys[row], xs[col + 1] - xs[col], ys[row + 1] - ys[row]);
    }

    private void sprite(Canvas c, Bitmap bitmap, int sx, int sy, int sw, int sh,
                        float x, float y, float w, float h) {
        if (bitmap == null) return;
        src.set(sx, sy, sx + sw, sy + sh); dst.set(x, y, x + w, y + h);
        c.drawBitmap(bitmap, src, dst, paint);
    }

    private void slice(Canvas c, Bitmap bitmap, Rect source, float x, float y, float w, float h) {
        if (bitmap == null || w <= 0 || h <= 0) return;
        dst.set(x, y, x + w, y + h); c.drawBitmap(bitmap, source, dst, paint);
    }

    private static int weaponIconIndex(WeaponDefinition weapon) {
        if (weapon == WeaponLoadout.COCONUT) return 1;
        WeaponType type = weapon.type;
        if (type == WeaponType.CATALYST) return 3;
        if (type == WeaponType.PHYSICAL) return 4;
        return 0;
    }
}
