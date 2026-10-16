package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private static final int MAX_CACHED_CHUNKS = 24;
    private final int[][] layout;
    private final SpriteSheet spriteSheet;
    private final LinkedHashMap<Integer, Bitmap> cache = new LinkedHashMap<Integer, Bitmap>(32, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Bitmap> eldest) {
            if (size() <= MAX_CACHED_CHUNKS) return false;
            eldest.getValue().recycle();
            return true;
        }
    };
    private final Rect source = new Rect();
    private final Rect destination = new Rect();

    public Tilemap(SpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
        this.layout = new MapLayout().getLayout();
        // Stable patches of magical acid swamp, distributed through the world.
        for (int row = 7; row < NUMBER_OF_ROW_TILES - 6; row += 13) {
            for (int col = 10; col < NUMBER_OF_COLUMN_TILES - 6; col += 17) {
                for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++)
                    layout[row + dy][col + dx] = MapLayout.ACID_SWAMP;
            }
        }
    }

    /** World coordinates of a random swamp tile, or null when none is suitable. */
    public double[] randomSwampSpawn(double playerX, double playerY, java.util.Random random) {
        for (int attempt = 0; attempt < 150; attempt++) {
            int row = random.nextInt(NUMBER_OF_ROW_TILES);
            int col = random.nextInt(NUMBER_OF_COLUMN_TILES);
            if (layout[row][col] != MapLayout.ACID_SWAMP) continue;
            double x = (col + 0.5) * TILE_WIDTH_PIXELS;
            double y = (row + 0.5) * TILE_HEIGHT_PIXELS;
            double distance = Math.hypot(x - playerX, y - playerY);
            if (distance >= 300 && distance <= 1400) return new double[]{x, y};
        }
        return null;
    }

    private Bitmap getChunk(int row, int col) {
        int key = row * chunkCols + col;
        Bitmap cached = cache.get(key);
        if (cached != null) return cached;
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
        cache.put(key, bitmap);
        return bitmap;
    }

    private static int floorDiv(int value, int divisor) {
        int quotient = value / divisor;
        return value % divisor < 0 ? quotient - 1 : quotient;
    }

    public void draw(Canvas canvas, GameDisplay display) {
        Rect visible = display.getGameRect();
        int left = Math.max(0, floorDiv(visible.left, CHUNK_WIDTH));
        int top = Math.max(0, floorDiv(visible.top, CHUNK_HEIGHT));
        int right = Math.min(chunkCols - 1, floorDiv(visible.right - 1, CHUNK_WIDTH));
        int bottom = Math.min(chunkRows - 1, floorDiv(visible.bottom - 1, CHUNK_HEIGHT));
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                int originX = col * CHUNK_WIDTH;
                int originY = row * CHUNK_HEIGHT;
                Bitmap bitmap = getChunk(row, col);
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
