package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.ForestHud;
import com.example.amazonMages.map.MapLayout;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.map.WorldDefinition;
import com.example.amazonMages.quest.QuestJournal;
import com.example.amazonMages.quest.QuestState;

/**
 * Pixel-art Map and Quest Journal modal with zero-allocation hardware zoom (1.0x - 3.0x),
 * drag-panning, and Sanctuary safe zone markers.
 */
public final class ExplorationPanel {
    private enum Page { CLOSED, MAP, QUESTS }

    private Page page = Page.CLOSED;
    private PixelUiKit fallbackUiKit;
    private final Paint paint = new Paint();
    private final RectF destination = new RectF();
    private final RectF mapImage = new RectF();
    private final RectF panelBounds = new RectF();
    private final RectF closeBounds = new RectF();
    private final RectF cardRect = new RectF();
    private final RectF barRect = new RectF();

    // Zoom & pan controls
    private float zoomLevel = 1f;
    private boolean firstMapLayout = true;
    private float panCenterX = 0.5f;
    private float panCenterY = 0.5f;
    private float gestureX, gestureY, gestureSpan;
    private float fittedWidth, fittedHeight;
    private float density = 1f;
    private int primaryPointer = -1;
    private int secondaryPointer = -1;

    private final RectF btnZoomIn = new RectF();
    private final RectF btnZoomOut = new RectF();
    private final RectF btnZoomReset = new RectF();

    public void attachContext(Context context) {
        if (context != null && fallbackUiKit == null) {
            fallbackUiKit = new PixelUiKit(context);
            density = context.getResources().getDisplayMetrics().density;
        }
    }

    public boolean isOpen() { return page != Page.CLOSED; }
    public void openMap() { page = Page.MAP; }
    public void openQuests() { page = Page.QUESTS; }
    public void close() {
        page = Page.CLOSED;
        resetGesture();
    }

    public boolean touch(float x, float y, int width, int height) {
        if (!isOpen()) return false;
        bounds(width, height);
        if (!panelBounds.contains(x, y) || closeBounds.contains(x, y)) {
            close();
            return true;
        }
        if (page == Page.MAP) {
            if (btnZoomIn.contains(x, y)) {
                zoomLevel = Math.min(3f, zoomLevel + .5f);
                clampCenter();
                return true;
            }
            if (btnZoomOut.contains(x, y)) {
                zoomLevel = Math.max(1f, zoomLevel - .5f);
                clampCenter();
                return true;
            }
            if (btnZoomReset.contains(x, y)) {
                zoomLevel = 1.0f;
                panCenterX = 0.5f;
                panCenterY = 0.5f;
                return true;
            }
        }
        return true;
    }

    public boolean onTouch(MotionEvent event, int width, int height) {
        if (!isOpen()) return false;
        bounds(width, height);
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetGesture();
                touch(x, y, width, height);
                if (page == Page.MAP && destination.contains(x, y)) {
                    primaryPointer = event.getPointerId(0);
                    sampleGesture(event, false);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int added = event.getActionIndex();
                if (primaryPointer != -1 && secondaryPointer == -1
                        && destination.contains(event.getX(added), event.getY(added))) {
                    secondaryPointer = event.getPointerId(added);
                    sampleGesture(event, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (primaryPointer != -1) sampleGesture(event, true);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int removed = event.getPointerId(event.getActionIndex());
                if (removed == primaryPointer || removed == secondaryPointer) {
                    primaryPointer = removed == primaryPointer ? secondaryPointer : primaryPointer;
                    secondaryPointer = -1;
                    sampleGesture(event, false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetGesture();
                break;
        }
        return true;
    }

    private void resetGesture() {
        primaryPointer = secondaryPointer = -1;
        gestureSpan = 0;
    }

    private void sampleGesture(MotionEvent event, boolean apply) {
        int first = event.findPointerIndex(primaryPointer);
        if (first < 0 || fittedWidth <= 0 || fittedHeight <= 0) return;
        int second = event.findPointerIndex(secondaryPointer);
        float x = event.getX(first), y = event.getY(first), span = 0f;
        if (second >= 0) {
            float x2 = event.getX(second), y2 = event.getY(second);
            span = (float) Math.hypot(x2 - x, y2 - y);
            x = (x + x2) * .5f;
            y = (y + y2) * .5f;
        }
        if (apply) {
            float oldZoom = zoomLevel;
            if (span >= 1f && gestureSpan >= 1f) zoomLevel = clamp(oldZoom * span / gestureSpan, 1f, 3f);
            float centerX = destination.centerX(), centerY = destination.centerY();
            panCenterX += ((gestureX - centerX) / oldZoom - (x - centerX) / zoomLevel) / fittedWidth;
            panCenterY += ((gestureY - centerY) / oldZoom - (y - centerY) / zoomLevel) / fittedHeight;
            clampCenter();
        }
        gestureX = x;
        gestureY = y;
        gestureSpan = span;
    }

    private void clampCenter() {
        if (fittedWidth <= 0 || fittedHeight <= 0) return;
        float halfX = Math.min(.5f, destination.width() / (2f * fittedWidth * zoomLevel));
        float halfY = Math.min(.5f, destination.height() / (2f * fittedHeight * zoomLevel));
        panCenterX = clamp(panCenterX, halfX, 1f - halfX);
        panCenterY = clamp(panCenterY, halfY, 1f - halfY);
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
        if (map == null) return;

        float top = topOffset + 4f;
        float bottom = panel.bottom - pad * 1.9f;
        float gap = 8f * density;
        float button = Math.min(52f * density, (bottom - top - 2f * gap) / 3f);
        float controlsX = panel.right - pad - button;
        float left = panel.left + pad;
        float right = controlsX - gap - 6f;
        destination.set(left, top, right, bottom);
        float mapWidth = destination.width();
        float fit = Math.min(mapWidth / map.getWidth(), destination.height() / map.getHeight());
        fittedWidth = map.getWidth() * fit;
        fittedHeight = map.getHeight() * fit;
        if (firstMapLayout) {
            zoomLevel = clamp(Math.max(mapWidth / fittedWidth, destination.height() / fittedHeight), 1f, 3f);
            panCenterX = (float) (player.getPositionX() / (WorldDefinition.COLUMNS * WorldDefinition.TILE_SIZE));
            panCenterY = (float) (player.getPositionY() / (WorldDefinition.ROWS * WorldDefinition.TILE_SIZE));
            firstMapLayout = false;
        }
        clampCenter();
        float imageWidth = fittedWidth * zoomLevel, imageHeight = fittedHeight * zoomLevel;
        float imageX = destination.centerX() - panCenterX * imageWidth;
        float imageY = destination.centerY() - panCenterY * imageHeight;
        mapImage.set(imageX, imageY, imageX + imageWidth, imageY + imageHeight);

        cardRect.set(left - 6f, top - 6f, right + 6f, bottom + 6f);
        ui.drawSubPanel(canvas, cardRect);
        paint.setFilterBitmap(false);
        canvas.save();
        canvas.clipRect(destination);
        paint.setColor(0xFF0B1A12);
        canvas.drawRect(destination, paint);
        canvas.drawBitmap(map, null, mapImage, paint);

        // Draw Sanctuary location marker (Spawn 96, 100)
        float sancNormX = 96f / MapLayout.NUMBER_OF_COLUMN_TILES;
        float sancNormY = 100f / MapLayout.NUMBER_OF_ROW_TILES;
        float sancScreenX = imageX + sancNormX * imageWidth;
        float sancScreenY = imageY + sancNormY * imageHeight;
        if (destination.contains(sancScreenX, sancScreenY)) {
            paint.setColor(0xFF0B1A12);
            canvas.drawCircle(sancScreenX, sancScreenY, Math.max(7f, mapWidth * .022f), paint);
            paint.setColor(0xFF38B764);
            canvas.drawCircle(sancScreenX, sancScreenY, Math.max(4.5f, mapWidth * .014f), paint);
            paint.setColor(0xFFF2D36B);
            canvas.drawCircle(sancScreenX, sancScreenY, Math.max(2.0f, mapWidth * .006f), paint);
        }

        com.example.amazonMages.map.BoitataArena arena = tilemap.world().boitataArena;
        float arenaX = (float) (arena.x / (MapLayout.NUMBER_OF_COLUMN_TILES * MapLayout.TILE_WIDTH_PIXELS));
        float arenaY = (float) (arena.y / (MapLayout.NUMBER_OF_ROW_TILES * MapLayout.TILE_HEIGHT_PIXELS));
        float arenaScreenX = imageX + arenaX * imageWidth;
        float arenaScreenY = imageY + arenaY * imageHeight;
        if (destination.contains(arenaScreenX, arenaScreenY)) {
            float marker = Math.max(5.5f, mapWidth * .016f);
            paint.setColor(0xFF3A1610);
            canvas.drawCircle(arenaScreenX, arenaScreenY, marker + 2, paint);
            paint.setColor(0xFFFF9B50);
            canvas.drawCircle(arenaScreenX, arenaScreenY, marker, paint);
            ui.drawTextShadow(canvas, "Arena do Boitatá", arenaScreenX, arenaScreenY + marker + 12f,
                Math.max(10f, mapWidth * .038f), 0xFFFFC386, true, Paint.Align.CENTER);
        }

        // Draw Player location marker
        float px = clamp((float) player.getPositionX() /
            (MapLayout.NUMBER_OF_COLUMN_TILES * MapLayout.TILE_WIDTH_PIXELS), 0f, .999f);
        float py = clamp((float) player.getPositionY() /
            (MapLayout.NUMBER_OF_ROW_TILES * MapLayout.TILE_HEIGHT_PIXELS), 0f, .999f);

        float playerScreenX = imageX + px * imageWidth;
        float playerScreenY = imageY + py * imageHeight;
        if (destination.contains(playerScreenX, playerScreenY)) {
            paint.setColor(0xFF0B1A12);
            canvas.drawCircle(playerScreenX, playerScreenY, Math.max(6f, mapWidth * .020f), paint);
            paint.setColor(0xFFFFF17A);
            canvas.drawCircle(playerScreenX, playerScreenY, Math.max(3.5f, mapWidth * .011f), paint);
        }
        canvas.restore();

        float btnY = destination.centerY() - (3f * button + 2f * gap) * .5f;
        btnZoomIn.set(controlsX, btnY, controlsX + button, btnY + button);
        btnY += button + gap;
        btnZoomOut.set(controlsX, btnY, controlsX + button, btnY + button);
        btnY += button + gap;
        btnZoomReset.set(controlsX, btnY, controlsX + button, btnY + button);
        ui.drawButton(canvas, btnZoomReset, "1x", zoomLevel <= 1.0f ? 2 : 0);
        ui.drawButton(canvas, btnZoomOut, "-", zoomLevel <= 1.0f ? 2 : 0);
        ui.drawButton(canvas, btnZoomIn, "+", zoomLevel >= 3.0f ? 2 : 0);

        float legendY = panel.bottom - pad * .85f;
        float legendText = Math.max(10f, Math.min(panel.width() * .024f, panel.height() * .042f));
        legend(canvas, ui, panel.left + pad, legendY, 0xFF38B764, "Santuario", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .24f, legendY, 0xFFFFF17A, "Voce", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .48f, legendY, 0xFF347A9B, "Agua", legendText);
        legend(canvas, ui, panel.left + pad + panel.width() * .72f, legendY, 0xFFB5C94B, "Acido", legendText);
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
        float panelRight = page == Page.MAP ? width * .973f
            : Math.min(width * .905f, width - Math.max(150f, width * .10f));
        panelBounds.set(width * (page == Page.MAP ? .027f : .05f), height * .08f, panelRight, height * .94f);
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
