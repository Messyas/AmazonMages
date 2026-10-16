package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.SpriteSheet;

import static com.example.amazonMages.map.MapLayout.*;

/** Pre-renders map in small chunks instead of a single 109 MiB bitmap. */
public class Tilemap {
    private static final int CHUNK_TILES = 8;
    private static final int CHUNK_WIDTH = CHUNK_TILES * TILE_WIDTH_PIXELS;
    private static final int CHUNK_HEIGHT = CHUNK_TILES * TILE_HEIGHT_PIXELS;
    private final int chunkRows = (NUMBER_OF_ROW_TILES + CHUNK_TILES - 1) / CHUNK_TILES;
    private final int chunkCols = (NUMBER_OF_COLUMN_TILES + CHUNK_TILES - 1) / CHUNK_TILES;
    private final Bitmap[][] chunks = new Bitmap[chunkRows][chunkCols];
    private final Rect source = new Rect();
    private final Rect destination = new Rect();

    public Tilemap(SpriteSheet spriteSheet) {
        int[][] layout = new MapLayout().getLayout();
        for (int row = 0; row < chunkRows; row++) {
            for (int col = 0; col < chunkCols; col++) {
                int width = Math.min(CHUNK_TILES, NUMBER_OF_COLUMN_TILES - col * CHUNK_TILES) * TILE_WIDTH_PIXELS;
                int height = Math.min(CHUNK_TILES, NUMBER_OF_ROW_TILES - row * CHUNK_TILES) * TILE_HEIGHT_PIXELS;
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas chunkCanvas = new Canvas(bitmap);
                for (int y = 0; y < height / TILE_HEIGHT_PIXELS; y++) {
                    for (int x = 0; x < width / TILE_WIDTH_PIXELS; x++) {
                        int tileRow = row * CHUNK_TILES + y;
                        int tileCol = col * CHUNK_TILES + x;
                        Tile tile = Tile.getTile(layout[tileRow][tileCol], spriteSheet,
                            new Rect(x * TILE_WIDTH_PIXELS, y * TILE_HEIGHT_PIXELS,
                                (x + 1) * TILE_WIDTH_PIXELS, (y + 1) * TILE_HEIGHT_PIXELS));
                        tile.draw(chunkCanvas);
                    }
                }
                chunks[row][col] = bitmap;
            }
        }
    }

    public void draw(Canvas canvas, GameDisplay display) {
        Rect visible = display.getGameRect();
        int left = Math.max(0, visible.left / CHUNK_WIDTH);
        int top = Math.max(0, visible.top / CHUNK_HEIGHT);
        int right = Math.min(chunkCols - 1, Math.floorDiv(visible.right - 1, CHUNK_WIDTH));
        int bottom = Math.min(chunkRows - 1, Math.floorDiv(visible.bottom - 1, CHUNK_HEIGHT));
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                int originX = col * CHUNK_WIDTH;
                int originY = row * CHUNK_HEIGHT;
                Bitmap bitmap = chunks[row][col];
                int l = Math.max(visible.left, originX);
                int t = Math.max(visible.top, originY);
                int r = Math.min(visible.right, originX + bitmap.getWidth());
                int b = Math.min(visible.bottom, originY + bitmap.getHeight());
                if (r <= l || b <= t) continue;
                source.set(l - originX, t - originY, r - originX, b - originY);
                destination.set(l - visible.left, t - visible.top, r - visible.left, b - visible.top);
                canvas.drawBitmap(bitmap, source, destination, null);
            }
        }
    }
}
