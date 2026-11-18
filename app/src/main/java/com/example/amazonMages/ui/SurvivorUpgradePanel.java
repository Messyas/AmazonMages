package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.combat.SurvivorArsenal;
import com.example.amazonMages.gamepanel.ForestHud;
import java.util.Collections;
import java.util.List;

/** GBA Pokemon-Emerald pixel-art 3-card level-up selection modal backed by PixelUiKit. */
public final class SurvivorUpgradePanel {
    private final PixelUiKit fallbackUi;
    private final Paint shapePaint = new Paint();
    private final RectF panelRect = new RectF();
    private final RectF slotRect = new RectF();
    private final RectF btnRect = new RectF();
    private final RectF[] cardRects = {new RectF(), new RectF(), new RectF()};
    private List<SurvivorArsenal.UpgradeOption> options = Collections.emptyList();
    private int pendingLevels;

    public SurvivorUpgradePanel(Context context) {
        fallbackUi = new PixelUiKit(context);
        shapePaint.setAntiAlias(false);
        shapePaint.setDither(false);
    }

    public boolean isOpen() { return !options.isEmpty(); }
    public int pendingLevels() { return pendingLevels; }

    public void queueLevelUps(int levels, SurvivorArsenal arsenal, java.util.Random random) {
        if (levels <= 0) return;
        pendingLevels += levels;
        if (options.isEmpty()) options = arsenal.generateChoices(random);
    }

    public SurvivorArsenal.UpgradeOption touch(float x, float y, int width, int height) {
        if (!isOpen()) return null;
        layoutCards(width, height);
        for (int i = 0; i < options.size() && i < cardRects.length; i++) {
            if (cardRects[i].contains(x, y)) return options.get(i);
        }
        return null;
    }

    public void advance(SurvivorArsenal arsenal, java.util.Random random) {
        if (pendingLevels > 0) pendingLevels--;
        options = pendingLevels > 0 ? arsenal.generateChoices(random) : Collections.emptyList();
    }

    public void clear() {
        pendingLevels = 0;
        options = Collections.emptyList();
    }

    public void draw(Canvas canvas, int width, int height, int currentLevel, ForestHud hud) {
        if (!isOpen() || width <= 0 || height <= 0) return;
        PixelUiKit ui = hud != null ? hud.uiKit() : fallbackUi;
        layoutCards(width, height);

        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(0xA806100A);
        canvas.drawRect(0, 0, width, height, shapePaint);

        panelRect.set(width * .05f, height * .09f,
            Math.min(width * .905f, width - Math.max(150f, width * .10f)), height * .94f);
        ui.drawPanel(canvas, panelRect);

        float ribbonW = Math.min(panelRect.width() * .44f, 300f * Math.max(2, ui.scale()));
        float ribbonH = Math.max(46f, Math.min(58f, panelRect.height() * .095f));
        ui.drawTitleRibbon(canvas, panelRect.centerX(), panelRect.top - ribbonH * 0.42f,
            ribbonW, ribbonH, "NIVEL " + currentLevel + "!");

        float shortSide = Math.min(width, height);
        String hint = "Escolha uma evolucao magica para continuar";
        if (pendingLevels > 1) hint += "  (+" + (pendingLevels - 1) + " pendente)";
        ui.drawTextShadow(canvas, hint, panelRect.centerX(), panelRect.top + ribbonH * .58f + 22f,
            Math.max(12f, shortSide * .028f), 0xFFD8ECC8, false, Paint.Align.CENTER);

        for (int i = 0; i < options.size() && i < cardRects.length; i++) {
            drawCard(canvas, ui, cardRects[i], options.get(i), shortSide);
        }
    }

    private void drawCard(Canvas canvas, PixelUiKit ui, RectF rect, SurvivorArsenal.UpgradeOption opt, float shortSide) {
        ui.drawSubPanel(canvas, rect);
        boolean isNew = "NOVO!".equals(opt.badge);

        float cx = rect.centerX();
        float slotSize = Math.min(rect.width() * .38f, rect.height() * .28f);
        slotRect.set(cx - slotSize * .5f, rect.top + 12f, cx + slotSize * .5f, rect.top + 12f + slotSize);
        ui.drawSlot(canvas, slotRect, isNew ? 1 : 0, isNew);
        ui.drawPickupIcon(canvas, opt.pickupIconIndex, slotRect.centerX(), slotRect.centerY(), slotSize * .64f, 255);

        float badgeY = slotRect.bottom + 15f;
        ui.drawTextShadow(canvas, opt.badge, cx, badgeY, Math.max(10.5f, shortSide * .025f),
            isNew ? 0xFFF2D36B : 0xFF9BF06A, true, Paint.Align.CENTER);

        float titleY = badgeY + Math.max(18f, shortSide * .045f);
        ui.drawTextShadow(canvas, opt.title, cx, titleY, Math.max(12.5f, shortSide * .031f),
            0xFFF4EBC8, true, Paint.Align.CENTER);

        float descY = titleY + Math.max(18f, shortSide * .042f);
        ui.drawWrappedText(canvas, opt.description, cx, descY, rect.width() * .86f,
            Math.max(10f, shortSide * .023f), 0xFFCDE2BA, false, Paint.Align.CENTER);

        float btnH = Math.max(44f, Math.min(56f, rect.height() * .13f));
        float btnW = Math.min(220f, rect.width() - 28f);
        btnRect.set(cx - btnW * .5f, rect.bottom - btnH - 12f, cx + btnW * .5f, rect.bottom - 12f);
        ui.drawButton(canvas, btnRect, "ESCOLHER", 0, isNew ? 0xFFFFF1B0 : 0);
    }

    private void layoutCards(int width, int height) {
        float panelRight = Math.min(width * .905f, width - Math.max(150f, width * .10f));
        float left = width * .075f, right = panelRight - width * .025f;
        float top = height * .25f, bottom = height * .89f;
        float gap = (right - left) * .030f;
        float cardW = ((right - left) - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            float cl = left + i * (cardW + gap);
            cardRects[i].set(cl, top, cl + cardW, bottom);
        }
    }
}
