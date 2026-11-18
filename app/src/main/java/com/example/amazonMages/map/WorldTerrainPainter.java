package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

/** Renders GBA Pokemon-Emerald-style 64px autotiles without stretching or mirror-tiling artifacts. */
final class WorldTerrainPainter {
    private static final int TILE = WorldDefinition.TILE_SIZE;
    private final WorldTerrainCache terrain;
    private final Bitmap grassSheet, pathSheet, pavingSheet, swampSheet, negroSheet, solimoesSheet, bridgeSheet;
    private final Paint pixelPaint = new Paint();
    private final Paint outsidePaint = new Paint();
    private final Paint acidHighlight = new Paint();
    private final Paint meetingFoam = new Paint();

    WorldTerrainPainter(SpriteSheet sprites, WorldTerrainCache terrain) {
        this.terrain = terrain;
        grassSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_grass);
        pathSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_path);
        pavingSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_paving);
        swampSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_swamp);
        negroSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_water_negro);
        solimoesSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_water_solimoes);
        bridgeSheet = sprites.resourceBitmap(R.drawable.emerald_tiles_bridge);
        pixelPaint.setFilterBitmap(false);
        pixelPaint.setAntiAlias(false);
        pixelPaint.setDither(false);
        outsidePaint.setColor(0xff142a20);
        acidHighlight.setColor(0x44a8e838);
        meetingFoam.setColor(0xaa9ad4c8);
    }

    void drawArea(Canvas canvas, int firstRow, int firstCol, int rows, int cols) {
        Rect src = new Rect();
        RectF dst = new RectF();
        canvas.drawRect(0, 0, cols * TILE, rows * TILE, outsidePaint);
        for (int dy = 0; dy < rows; dy++) {
            for (int dx = 0; dx < cols; dx++) {
                int row = firstRow + dy;
                int col = firstCol + dx;
                float x = dx * TILE;
                float y = dy * TILE;
                WorldDefinition.Terrain tile = terrain.sampleTile(row, col);
                if (tile == WorldDefinition.Terrain.BORDER) {
                    canvas.drawRect(x, y, x + TILE, y + TILE, outsidePaint);
                    continue;
                }

                // Always start with deterministic Hoenn emerald grass underneath transitions.
                int hash = Math.abs(row * 73856093 ^ col * 19349663);
                int grassFrame = (hash % 7 == 0) ? 4 + (hash / 7) % 4 : hash % 4;
                drawFrame(canvas, grassSheet, grassFrame, x, y, src, dst);

                if (tile == WorldDefinition.Terrain.PATH || tile == WorldDefinition.Terrain.GROUND) {
                    drawAutotile(canvas, pathSheet, row, col, x, y, src, dst, t ->
                        t == WorldDefinition.Terrain.PATH || t == WorldDefinition.Terrain.GROUND
                            || t == WorldDefinition.Terrain.BRIDGE || t == WorldDefinition.Terrain.PAVING);
                } else if (tile == WorldDefinition.Terrain.PAVING) {
                    if (isDirt(row - 1, col) || isDirt(row + 1, col)
                            || isDirt(row, col - 1) || isDirt(row, col + 1)) {
                        drawFrame(canvas, pathSheet, 0, x, y, src, dst);
                    }
                    drawAutotile(canvas, pavingSheet, row, col, x, y, src, dst, t ->
                        t == WorldDefinition.Terrain.PAVING || t == WorldDefinition.Terrain.BRIDGE);
                } else if (tile == WorldDefinition.Terrain.BURNED
                        || tile == WorldDefinition.Terrain.DEAD_TREE
                        || tile == WorldDefinition.Terrain.ACID_SWAMP) {
                    drawAutotile(canvas, swampSheet, row, col, x, y, src, dst, t ->
                        t == WorldDefinition.Terrain.BURNED
                            || t == WorldDefinition.Terrain.DEAD_TREE
                            || t == WorldDefinition.Terrain.ACID_SWAMP);
                    if (tile == WorldDefinition.Terrain.ACID_SWAMP) {
                        canvas.drawRect(x + 8, y + 8, x + TILE - 8, y + TILE - 8, acidHighlight);
                    }
                } else if (tile.isWater()) {
                    boolean silt = tile == WorldDefinition.Terrain.SOLIMOES
                        || tile == WorldDefinition.Terrain.MEETING_SILT;
                    Bitmap waterSheet = silt ? solimoesSheet : negroSheet;
                    int bankMask = (isLand(row - 1, col) ? 1 : 0)
                        | (isLand(row, col + 1) ? 2 : 0)
                        | (isLand(row + 1, col) ? 4 : 0)
                        | (isLand(row, col - 1) ? 8 : 0);
                    drawFrame(canvas, waterSheet, bankMask, x, y, src, dst);
                    if (tile == WorldDefinition.Terrain.MEETING_DARK
                            && terrain.sampleTile(row, col + 1) == WorldDefinition.Terrain.MEETING_SILT) {
                        canvas.drawRect(x + TILE - 6, y + 4, x + TILE, y + TILE - 4, meetingFoam);
                    } else if (tile == WorldDefinition.Terrain.MEETING_SILT
                            && terrain.sampleTile(row, col - 1) == WorldDefinition.Terrain.MEETING_DARK) {
                        canvas.drawRect(x, y + 4, x + 6, y + TILE - 4, meetingFoam);
                    }
                } else if (tile == WorldDefinition.Terrain.BRIDGE) {
                    drawFrame(canvas, bridgeSheet, 0, x, y, src, dst);
                    if (terrain.sampleTile(row - 1, col).isWater()) drawFrame(canvas, bridgeSheet, 1, x, y, src, dst);
                    if (terrain.sampleTile(row + 1, col).isWater()) drawFrame(canvas, bridgeSheet, 2, x, y, src, dst);
                    if (terrain.sampleTile(row, col - 1).isWater()) drawFrame(canvas, bridgeSheet, 3, x, y, src, dst);
                    if (terrain.sampleTile(row, col + 1).isWater()) drawFrame(canvas, bridgeSheet, 4, x, y, src, dst);
                }
            }
        }
    }

    private interface TerrainMatch {
        boolean matches(WorldDefinition.Terrain t);
    }

    private void drawAutotile(Canvas canvas, Bitmap sheet, int row, int col, float x, float y,
                              Rect src, RectF dst, TerrainMatch match) {
        if (sheet == null) return;
        boolean sameN = match.matches(terrain.sampleTile(row - 1, col));
        boolean sameE = match.matches(terrain.sampleTile(row, col + 1));
        boolean sameS = match.matches(terrain.sampleTile(row + 1, col));
        boolean sameW = match.matches(terrain.sampleTile(row, col - 1));
        int mask = (!sameN ? 1 : 0) | (!sameE ? 2 : 0) | (!sameS ? 4 : 0) | (!sameW ? 8 : 0);
        drawFrame(canvas, sheet, mask, x, y, src, dst);
        // Inner diagonal corners (frames 16..19 = NW, NE, SE, SW)
        if (sameN && sameW && !match.matches(terrain.sampleTile(row - 1, col - 1))) drawFrame(canvas, sheet, 16, x, y, src, dst);
        if (sameN && sameE && !match.matches(terrain.sampleTile(row - 1, col + 1))) drawFrame(canvas, sheet, 17, x, y, src, dst);
        if (sameS && sameE && !match.matches(terrain.sampleTile(row + 1, col + 1))) drawFrame(canvas, sheet, 18, x, y, src, dst);
        if (sameS && sameW && !match.matches(terrain.sampleTile(row + 1, col - 1))) drawFrame(canvas, sheet, 19, x, y, src, dst);
    }

    private boolean isDirt(int row, int col) {
        WorldDefinition.Terrain t = terrain.sampleTile(row, col);
        return t == WorldDefinition.Terrain.PATH || t == WorldDefinition.Terrain.GROUND;
    }

    private boolean isLand(int row, int col) {
        WorldDefinition.Terrain t = terrain.sampleTile(row, col);
        return !t.isWater() && t != WorldDefinition.Terrain.BRIDGE && t != WorldDefinition.Terrain.BORDER;
    }

    private void drawFrame(Canvas canvas, Bitmap sheet, int frame, float x, float y, Rect src, RectF dst) {
        if (sheet == null) return;
        int sx = frame * TILE;
        if (sx + TILE > sheet.getWidth()) return;
        src.set(sx, 0, sx + TILE, TILE);
        dst.set(x, y, x + TILE, y + TILE);
        canvas.drawBitmap(sheet, src, dst, pixelPaint);
    }
}
