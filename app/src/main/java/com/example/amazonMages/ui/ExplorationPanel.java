package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.ForestHud;
import com.example.amazonMages.map.MapLayout;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.quest.QuestJournal;
import com.example.amazonMages.quest.QuestState;

/** Pixel-art Map and Quest Journal modal opened from the forest HUD. */
public final class ExplorationPanel {
    private enum Page { CLOSED, MAP, QUESTS }

    private Page page = Page.CLOSED;
    private PixelUiKit fallbackUiKit;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final RectF panelBounds = new RectF();
    private final RectF closeBounds = new RectF();
    private final RectF cardRect = new RectF();
    private final RectF barRect = new RectF();

    public void attachContext(Context context) {
        if (context != null && fallbackUiKit == null) fallbackUiKit = new PixelUiKit(context);
    }

    public boolean isOpen() { return page != Page.CLOSED; }
    public void openMap() { page = Page.MAP; }
    public void openQuests() { page = Page.QUESTS; }
    public void close() { page = Page.CLOSED; }

    public boolean touch(float x, float y, int width, int height) {
        if (!isOpen()) return false;
        bounds(width, height);
        if (!panelBounds.contains(x, y) || closeBounds.contains(x, y)) close();
        return true;
    }

    public void draw(Canvas canvas, int width, int height, Tilemap tilemap, Player player,
                     QuestJournal quests, String introId, String sentinelId) {
        draw(canvas, width, height, tilemap, player, quests, introId, sentinelId, null);
    }

    public void draw(Canvas canvas, int width, int height, Tilemap tilemap, Player player,
                     QuestJournal quests, String introId, String sentinelId, ForestHud hud) {
        if (!isOpen() || width <= 0 || height <= 0) return;
        PixelUiKit ui = hud != null ? hud.uiKit() : fallbackUiKit;
        if (ui == null) return;
        bounds(width, height);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xA406100A);
        canvas.drawRect(0, 0, width, height, paint);

        ui.drawPanel(canvas, panelBounds);
        float ribbonW = Math.min(panelBounds.width() * .42f, 280f * Math.max(2, ui.scale()));
        float ribbonH = Math.max(46f, Math.min(58f, panelBounds.height() * .095f));
        ui.drawTitleRibbon(canvas, panelBounds.centerX(), panelBounds.top - ribbonH * 0.42f, ribbonW, ribbonH,
            page == Page.MAP ? "MAPA DA FLORESTA" : "DIARIO DE MISSOES");
        ui.drawCloseButton(canvas, closeBounds, false);

        float pad = panelPad();
        if (page == Page.MAP) drawMap(canvas, ui, panelBounds, pad, ribbonH, tilemap, player);
        else drawQuests(canvas, ui, panelBounds, pad, ribbonH, quests, introId, sentinelId);
    }

    private void drawMap(Canvas canvas, PixelUiKit ui, RectF panel, float pad, float ribbonH,
                         Tilemap tilemap, Player player) {
        float topOffset = panel.top + ribbonH * .58f + 10f;
        Bitmap map = tilemap.overview();
        float availableTop = topOffset + 4f;
        float availableBottom = panel.bottom - pad * 1.9f;
        float availableWidth = panel.width() - pad * 2f;
        float mapWidth = Math.min(availableWidth, (availableBottom - availableTop) * map.getWidth() / map.getHeight());
        float mapHeight = mapWidth * map.getHeight() / map.getWidth();
        float left = panel.centerX() - mapWidth * .5f;
        float top = availableTop + (availableBottom - availableTop - mapHeight) * .5f;

        cardRect.set(left - 6f, top - 6f, left + mapWidth + 6f, top + mapHeight + 6f);
        ui.drawSubPanel(canvas, cardRect);
        paint.setFilterBitmap(false);
        source.set(0, 0, map.getWidth(), map.getHeight());
        destination.set(left, top, left + mapWidth, top + mapHeight);
        canvas.drawBitmap(map, source, destination, paint);

        float px = clamp((float) player.getPositionX() /
            (MapLayout.NUMBER_OF_COLUMN_TILES * MapLayout.TILE_WIDTH_PIXELS), 0f, .999f);
        float py = clamp((float) player.getPositionY() /
            (MapLayout.NUMBER_OF_ROW_TILES * MapLayout.TILE_HEIGHT_PIXELS), 0f, .999f);
        paint.setColor(0xFF0B1A12);
        canvas.drawCircle(left + px * mapWidth, top + py * mapHeight, Math.max(6f, mapWidth * .020f), paint);
        paint.setColor(0xFFFFF17A);
        canvas.drawCircle(left + px * mapWidth, top + py * mapHeight, Math.max(3.5f, mapWidth * .011f), paint);

        float legendY = panel.bottom - pad * .85f;
        float legendText = Math.max(10f, Math.min(panel.width() * .024f, panel.height() * .042f));
        legend(canvas, ui, panel.left + pad, legendY, 0xFF65A65A, "Mata", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .23f, legendY, 0xFF347A9B, "Agua", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .46f, legendY, 0xFFB5C94B, "Acido", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .69f, legendY, 0xFF245A35, "Arvores", legendText);
    }

    private void drawQuests(Canvas canvas, PixelUiKit ui, RectF panel, float pad, float ribbonH,
                            QuestJournal quests, String introId, String sentinelId) {
        float top = panel.top + ribbonH * .58f + 16f;
        float cardGap = pad * .55f;
        float cardHeight = (panel.bottom - pad - top - cardGap) * .5f;
        questCard(canvas, ui, panel.left + pad, top, panel.right - pad, top + cardHeight,
            "Ecos do Pantano", "Derrote fantasmas acidos na floresta", quests, introId, 3);
        float secondTop = top + cardHeight + cardGap;
        questCard(canvas, ui, panel.left + pad, secondTop, panel.right - pad, secondTop + cardHeight,
            "A Sentinela", "Enfrente a guardia ancestral da floresta", quests, sentinelId, 1);
    }

    private void questCard(Canvas canvas, PixelUiKit ui, float left, float top, float right, float bottom,
                           String title, String objective, QuestJournal quests, String id, int target) {
        cardRect.set(left, top, right, bottom);
        ui.drawSubPanel(canvas, cardRect);
        float inset = (right - left) * .04f;
        float titleSize = Math.max(13f, Math.min((right - left) * .038f, panelBounds.height() * .050f));
        ui.drawTextShadow(canvas, title, left + inset, top + inset + titleSize, titleSize, 0xFFFFD98A, true, Paint.Align.LEFT);
        ui.drawTextShadow(canvas, objective, left + inset, top + inset + titleSize * 2.1f,
            Math.max(10.5f, titleSize * .74f), 0xFFE0E8D7, false, Paint.Align.LEFT);

        QuestState state = quests.state(id);
        int prog = Math.max(0, Math.min(target, quests.progress(id)));
        float ratio = target > 0 ? prog / (float) target : 0f;
        barRect.set(left + inset, bottom - inset - 16f, right - inset * 6f, bottom - inset);
        ui.drawBar(canvas, barRect, ratio, state == QuestState.COMPLETED || state == QuestState.REWARDED ? 1 : 0);

        String stateLabel = stateLabel(state);
        String amount = prog + "/" + target;
        String status = state == QuestState.LOCKED || state == QuestState.AVAILABLE ? stateLabel : stateLabel + "  " + amount;
        ui.drawTextShadow(canvas, status, right - inset, bottom - inset - 2f,
            Math.max(10.5f, titleSize * .72f),
            state == QuestState.COMPLETED || state == QuestState.REWARDED ? 0xFF9BF06A : 0xFFC9D5B8,
            true, Paint.Align.RIGHT);
    }

    private static String stateLabel(QuestState state) {
        if (state == null) return "Indisponivel";
        switch (state) {
            case AVAILABLE: return "Disponivel";
            case ACTIVE: return "Em andamento";
            case COMPLETED: return "Concluida";
            case REWARDED: return "Recompensada";
            case LOCKED:
            default: return "Bloqueada";
        }
    }

    private void legend(Canvas canvas, PixelUiKit ui, float x, float y, int color, String label, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0B1A12);
        canvas.drawRect(x - 1, y - size, x + size + 1, y + 2, paint);
        paint.setColor(color);
        canvas.drawRect(x, y - size + 1, x + size, y + 1, paint);
        ui.drawTextShadow(canvas, label, x + size + 5, y, size, 0xFFF4EBC8, false, Paint.Align.LEFT);
    }

    private void bounds(int width, int height) {
        float panelRight = Math.min(width * .905f, width - Math.max(150f, width * .10f));
        panelBounds.set(width * .05f, height * .08f, panelRight, height * .94f);
        float closeSize = Math.max(38f, Math.min(52f, panelBounds.height() * .09f));
        closeBounds.set(panelBounds.right - closeSize - 10f, panelBounds.top + 8f,
            panelBounds.right - 10f, panelBounds.top + 8f + closeSize);
    }

    private float panelPad() {
        return Math.min(panelBounds.width() * .045f, panelBounds.height() * .07f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
