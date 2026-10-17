package com.example.amazonMages.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.map.MapLayout;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.quest.QuestJournal;
import com.example.amazonMages.quest.QuestState;

/** Map and quest journal opened from the forest HUD. */
public final class ExplorationPanel {
    private enum Page { CLOSED, MAP, QUESTS }

    private Page page = Page.CLOSED;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final RectF panelBounds = new RectF();
    private final RectF closeBounds = new RectF();

    public boolean isOpen() { return page != Page.CLOSED; }
    public void openMap() { page = Page.MAP; }
    public void openQuests() { page = Page.QUESTS; }
    public void close() { page = Page.CLOSED; }

    /** Returns true while the panel owns the touch. Outside taps and the close button dismiss it. */
    public boolean touch(float x, float y, int width, int height) {
        if (!isOpen()) return false;
        bounds(width, height);
        if (!panelBounds.contains(x, y) || closeBounds.contains(x, y)) close();
        return true;
    }

    public void draw(Canvas canvas, int width, int height, Tilemap tilemap, Player player,
                     QuestJournal quests, String introId, String sentinelId) {
        if (!isOpen() || width <= 0 || height <= 0) return;
        bounds(width, height);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB9000000);
        canvas.drawColor(0x99090F0B);
        paint.setColor(0xFF18291F);
        canvas.drawRect(panelBounds, paint);
        paint.setColor(0xFF52754E);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, Math.min(width, height) * .004f));
        canvas.drawRect(panelBounds, paint);
        paint.setStyle(Paint.Style.FILL);

        float pad = panelPad();
        float headerY = panelBounds.top + pad * 1.5f;
        text(canvas, page == Page.MAP ? "MAPA DA FLORESTA" : "DIARIO DE MISSOES",
            panelBounds.left + pad, headerY,
            Math.max(16f, Math.min(panelBounds.width() * .045f, panelBounds.height() * .085f)),
            0xFFFFD98A, true);
        closeBounds.set(panelBounds.right - pad - 34, panelBounds.top + pad * .6f,
            panelBounds.right - pad, panelBounds.top + pad * .6f + 34);
        paint.setColor(0xFF75413C);
        canvas.drawRect(closeBounds, paint);
        text(canvas, "X", closeBounds.centerX(), closeBounds.centerY() + 6,
            19, 0xFFFFE9D4, true, true);

        if (page == Page.MAP) drawMap(canvas, panelBounds, pad, tilemap, player);
        else drawQuests(canvas, panelBounds, pad, quests, introId, sentinelId);
    }

    private void drawMap(Canvas canvas, RectF panel, float pad, Tilemap tilemap, Player player) {
        text(canvas, "Posicao atual", panel.left + pad, panel.top + pad * 3.2f,
            Math.max(12f, Math.min(panel.width() * .027f, panel.height() * .06f)), 0xFFD8E4C8, false);
        Bitmap map = tilemap.overview();
        float availableTop = panel.top + pad * 3.75f;
        float availableBottom = panel.bottom - pad * 2.0f;
        float availableWidth = panel.width() - pad * 2;
        float mapWidth = Math.min(availableWidth, (availableBottom - availableTop) * 1.42f);
        float mapHeight = mapWidth * map.getHeight() / map.getWidth();
        float left = panel.centerX() - mapWidth * .5f;
        float top = availableTop + (availableBottom - availableTop - mapHeight) * .5f;
        paint.setColor(0xFF0C1710);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(left - 4, top - 4, left + mapWidth + 4, top + mapHeight + 4, paint);
        paint.setFilterBitmap(false);
        source.set(0, 0, map.getWidth(), map.getHeight());
        destination.set(left, top, left + mapWidth, top + mapHeight);
        canvas.drawBitmap(map, source, destination, paint);

        float px = clamp((float) player.getPositionX() /
            (MapLayout.NUMBER_OF_COLUMN_TILES * MapLayout.TILE_WIDTH_PIXELS), 0f, .999f);
        float py = clamp((float) player.getPositionY() /
            (MapLayout.NUMBER_OF_ROW_TILES * MapLayout.TILE_HEIGHT_PIXELS), 0f, .999f);
        paint.setColor(0xFF162016);
        canvas.drawCircle(left + px * mapWidth, top + py * mapHeight, Math.max(6, mapWidth * .018f), paint);
        paint.setColor(0xFFFFF17A);
        canvas.drawCircle(left + px * mapWidth, top + py * mapHeight, Math.max(3, mapWidth * .009f), paint);

        float legendY = panel.bottom - pad * 1.15f;
        float legendText = Math.max(10f, Math.min(panel.width() * .022f, panel.height() * .04f));
        legend(canvas, panel.left + pad, legendY, 0xFF65A65A, "Mata", legendText);
        legend(canvas, panel.left + pad + panel.width() * .23f, legendY, 0xFF347A9B, "Agua", legendText);
        legend(canvas, panel.left + pad + panel.width() * .46f, legendY, 0xFFB5C94B, "Acido", legendText);
        legend(canvas, panel.left + pad + panel.width() * .70f, legendY, 0xFF245A35, "Arvores", legendText);
    }

    private void drawQuests(Canvas canvas, RectF panel, float pad, QuestJournal quests,
                            String introId, String sentinelId) {
        float top = panel.top + pad * 2.8f;
        float cardGap = pad * .55f;
        float cardHeight = (panel.bottom - pad - top - cardGap) * .5f;
        questCard(canvas, panel.left + pad, top, panel.right - pad, top + cardHeight,
            "Ecos do pantano", "Derrote fantasmas acidos", quests, introId, 3);
        float secondTop = top + cardHeight + cardGap;
        questCard(canvas, panel.left + pad, secondTop, panel.right - pad,
            secondTop + cardHeight, "A Sentinela", "Enfrente a guardia da floresta",
            quests, sentinelId, 1);
    }

    private void questCard(Canvas canvas, float left, float top, float right, float bottom,
                           String title, String objective, QuestJournal quests, String id, int target) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF263B2B);
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setColor(0xFF638054);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        float inset = (right - left) * .035f;
        float titleSize = Math.max(14f, Math.min((right - left) * .036f,
            panelBounds.height() * .045f));
        text(canvas, title, left + inset, top + inset + titleSize, titleSize, 0xFFFFD98A, true);
        text(canvas, objective, left + inset, top + inset + titleSize * 2.25f,
            Math.max(11f, titleSize * .72f), 0xFFE0E8D7, false);
        QuestState state = quests.state(id);
        String stateLabel = stateLabel(state);
        String amount = quests.progress(id) + "/" + target;
        String status = state == QuestState.LOCKED || state == QuestState.AVAILABLE
            ? stateLabel : stateLabel + "  " + amount;
        text(canvas, status, left + inset, bottom - inset, Math.max(11f, titleSize * .70f),
            state == QuestState.COMPLETED || state == QuestState.REWARDED ? 0xFFBCEB8D : 0xFFC9D5B8, true);
    }

    private static String stateLabel(QuestState state) {
        if (state == null) return "Indisponivel";
        switch (state) {
            case AVAILABLE: return "Disponivel";
            case ACTIVE: return "Em andamento";
            case COMPLETED: return "Concluida";
            case REWARDED: return "Recompensa recebida";
            case LOCKED:
            default: return "Bloqueada";
        }
    }

    private void legend(Canvas canvas, float x, float y, int color, String label, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(x, y - size + 1, x + size, y + 1, paint);
        text(canvas, label, x + size + 4, y, size, 0xFFE3E9D9, false);
    }

    private void bounds(int width, int height) {
        float marginX = width * .06f, marginY = height * .08f;
        panelBounds.set(marginX, marginY, width - marginX, height - marginY);
        float pad = panelPad();
        closeBounds.set(panelBounds.right - pad - 34, panelBounds.top + pad * .6f,
            panelBounds.right - pad, panelBounds.top + pad * .6f + 34);
    }

    private float panelPad() {
        return Math.min(panelBounds.width() * .045f, panelBounds.height() * .07f);
    }

    private void text(Canvas canvas, String value, float x, float baseline, float size, int color, boolean bold) {
        text(canvas, value, x, baseline, size, color, bold, false);
    }

    private void text(Canvas canvas, String value, float x, float baseline, float size, int color,
                      boolean bold, boolean centered) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(bold ? android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            : android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setAntiAlias(true);
        if (centered) x -= paint.measureText(value) * .5f;
        canvas.drawText(value, x, baseline, paint);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
