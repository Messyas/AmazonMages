package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;

import static com.example.amazonMages.map.MapLayout.*;

/**
 * Pre-renders the map in small chunks. Chunks around the camera are built ahead of time on a
 * background thread (and uploaded to the GPU there), so crossing a chunk border never builds
 * bitmaps on the render thread. Drawing only falls back to building synchronously when a chunk is
 * needed before the worker finished it (first frame, teleport).
 */
public class Tilemap {
    private static final int CHUNK_TILES = 8;
    private static final int CHUNK_WIDTH = CHUNK_TILES * TILE_WIDTH_PIXELS;
    private static final int CHUNK_HEIGHT = CHUNK_TILES * TILE_HEIGHT_PIXELS;
    private final int chunkRows = (NUMBER_OF_ROW_TILES + CHUNK_TILES - 1) / CHUNK_TILES;
    private final int chunkCols = (NUMBER_OF_COLUMN_TILES + CHUNK_TILES - 1) / CHUNK_TILES;
    /** Chunks built beyond the visible area, in every direction (~1 MiB each; ~50 MiB resident). */
    private static final int PREFETCH_MARGIN = 1;
    /** Chunks farther than this from the visible area are dropped; the gap avoids rebuild flicker. */
    private static final int KEEP_MARGIN = 2;

    // Player spawn (see Game: 2*500, 500 px). Acid-swamp patches, and therefore ambient enemies,
    // are kept away from it so the beginning of the game is calm.
    private static final int START_COLUMN = 15, START_ROW = 7;
    private static final int SAFE_RADIUS_TILES = 32;

    private final int[][] layout;
    private final SpriteSheet spriteSheet;
    private final com.example.amazonMages.graphics.Sprite[] waterFrames = new com.example.amazonMages.graphics.Sprite[4];
    private final com.example.amazonMages.graphics.Sprite[] lavaFrames = new com.example.amazonMages.graphics.Sprite[4];
    private final com.example.amazonMages.graphics.Sprite[] acidFrames = new com.example.amazonMages.graphics.Sprite[4];
    private final java.util.List<TreePlacement> trees = new java.util.ArrayList<>();
    private int animationTicks;

    private static final byte ANIM_NONE = 0, ANIM_WATER = 1, ANIM_LAVA = 2, ANIM_ACID = 3;
    private final Sprite[][] animationFrames = {null, waterFrames, lavaFrames, acidFrames};
    private final int[] animationSpeed = {1, 12, 9, 14};
    /** Which tiles animate, precomputed so drawing does no neighbour checks. */
    private final byte[][] animated = new byte[NUMBER_OF_ROW_TILES][NUMBER_OF_COLUMN_TILES];
    /** Tile indices (row * columns + column) per tile type, built lazily for spawn queries. */
    private final int[][] tilesByType = new int[6][];

    // Indexed by chunk (row * chunkCols + col): no boxing, so the per-frame lookups allocate nothing.
    private final AtomicReferenceArray<Bitmap> chunks = new AtomicReferenceArray<>(chunkRows * chunkCols);
    private final AtomicIntegerArray pending = new AtomicIntegerArray(chunkRows * chunkCols);
    private final ThreadPoolExecutor builder = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(), runnable -> {
            Thread thread = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                runnable.run();
            }, "tilemap-chunks");
            thread.setDaemon(true);
            return thread;
        });
    // Chunk window the camera currently cares about; stale queued work outside it is skipped.
    private volatile int wantedLeft, wantedTop, wantedRight = -1, wantedBottom = -1;
    private int lastLeft = Integer.MIN_VALUE, lastTop, lastRight, lastBottom;
    private final Rect source = new Rect();
    private final Rect destination = new Rect();
    private Bitmap overviewBitmap;

    public Tilemap(SpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
        this.layout = new MapLayout().getLayout();
        for (int frame = 0; frame < 4; frame++) {
            waterFrames[frame] = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.tile_water, frame, 64, 64);
            lavaFrames[frame] = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.tile_lava, frame, 64, 64);
            acidFrames[frame] = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.tile_acid_swamp, frame, 64, 64);
        }
        builder.allowCoreThreadTimeOut(true);
        placeAcidSwamps();
        buildAnimationMask();
        buildTreePlacements();
    }

    /**
     * Stable patches of magical acid swamp (the only place ambient enemies spawn). Nothing is
     * placed near the player's start, and density grows toward the right edge and the borders of
     * the world, so enemies live in the outskirts. Deterministic for a given world seed.
     */
    private void placeAcidSwamps() {
        Random random = new Random(MapLayout.WORLD_SEED ^ 0x5A17CAFEL);
        for (int row = 4; row < NUMBER_OF_ROW_TILES - 4; row += 9) {
            for (int col = 6; col < NUMBER_OF_COLUMN_TILES - 4; col += 11) {
                int r = Math.max(1, Math.min(NUMBER_OF_ROW_TILES - 5, row + random.nextInt(5) - 2));
                int c = Math.max(1, Math.min(NUMBER_OF_COLUMN_TILES - 5, col + random.nextInt(7) - 3));
                double roll = random.nextDouble();
                if (Math.hypot(c - START_COLUMN, r - START_ROW) < SAFE_RADIUS_TILES) continue;
                double rightness = c / (double) NUMBER_OF_COLUMN_TILES;
                int borderDistance = Math.min(Math.min(c, NUMBER_OF_COLUMN_TILES - 1 - c),
                    Math.min(r, NUMBER_OF_ROW_TILES - 1 - r));
                // Rare on the left, common on the right (cubic ramp), a little extra on every border.
                double chance = 0.02 + 0.50 * rightness * rightness * rightness + (borderDistance < 15 ? 0.06 : 0.0);
                if (roll > chance || touchesLiquid(r, c)) continue;
                for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++)
                    layout[r + dy][c + dx] = MapLayout.ACID_SWAMP;
            }
        }
    }

    private boolean touchesLiquid(int row, int col) {
        for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++) {
            int type = layout[row + dy][col + dx];
            if (type == Tile.TileType.WATER_TILE.ordinal() || type == Tile.TileType.LAVA_TILE.ordinal())
                return true;
        }
        return false;
    }

    private void buildAnimationMask() {
        for (int row = 0; row < NUMBER_OF_ROW_TILES; row++) {
            for (int col = 0; col < NUMBER_OF_COLUMN_TILES; col++) {
                int type = layout[row][col];
                if (type == Tile.TileType.LAVA_TILE.ordinal()) animated[row][col] = ANIM_LAVA;
                else if (type == Tile.TileType.ACID_SWAMP_TILE.ordinal()) animated[row][col] = ANIM_ACID;
                else if (type == Tile.TileType.WATER_TILE.ordinal() && isWaterInterior(row, col))
                    animated[row][col] = ANIM_WATER;
            }
        }
    }

    /** Small cached color map used by the exploration screen. */
    public Bitmap overview() {
        if (overviewBitmap != null && !overviewBitmap.isRecycled()) return overviewBitmap;
        final int scale = 3;
        overviewBitmap = Bitmap.createBitmap(NUMBER_OF_COLUMN_TILES * scale,
            NUMBER_OF_ROW_TILES * scale, Bitmap.Config.ARGB_8888);
        Canvas overviewCanvas = new Canvas(overviewBitmap);
        Paint tilePaint = new Paint();
        tilePaint.setStyle(Paint.Style.FILL);
        tilePaint.setAntiAlias(false);
        for (int row = 0; row < NUMBER_OF_ROW_TILES; row++) {
            for (int col = 0; col < NUMBER_OF_COLUMN_TILES; col++) {
                tilePaint.setColor(overviewColor(layout[row][col]));
                overviewCanvas.drawRect(col * scale, row * scale,
                    (col + 1) * scale, (row + 1) * scale, tilePaint);
            }
        }
        return overviewBitmap;
    }

    private static int overviewColor(int tileType) {
        switch (tileType) {
            case 0: return 0xFF347A9B; // water
            case 1: return 0xFFC45D36; // lava
            case 2: return 0xFFB99A62; // ground
            case 4: return 0xFF245A35; // trees
            case 5: return 0xFFB5C94B; // acid swamp
            case 3:
            default: return 0xFF65A65A; // grass
        }
    }

    private void buildTreePlacements() {
        java.util.List<int[]> anchors = new java.util.ArrayList<>();
        boolean[][] anchored = new boolean[NUMBER_OF_ROW_TILES][NUMBER_OF_COLUMN_TILES];
        // The hand-made area goes first, in its original scan order, so its giant/palm trees keep
        // their positions; the generated terrain is scanned afterwards.
        for (int row = 0; row < NUMBER_OF_ROW_TILES - 2; row++) {
            for (int col = 0; col < NUMBER_OF_COLUMN_TILES - 2; col++) {
                if (isHandMade(row, col)) tryAnchor(row, col, anchors, anchored);
            }
        }
        for (int row = 0; row < NUMBER_OF_ROW_TILES - 2; row++) {
            for (int col = 0; col < NUMBER_OF_COLUMN_TILES - 2; col++) {
                if (!isHandMade(row, col)) tryAnchor(row, col, anchors, anchored);
            }
        }
        for (int i = 0; i < anchors.size(); i++) {
            int row = anchors.get(i)[0], col = anchors.get(i)[1];
            Sprite sprite;
            int width, height;
            if (i == 10) {
                sprite = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.obj_tree_giant, 0, 192, 192);
                width = height = 192;
            } else if (i % 6 == 3) {
                int variant = Math.floorMod(row * 7 + col, 2);
                sprite = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.obj_tree_palm,
                    variant, 128, 192);
                width = 128; height = 192;
            } else {
                int variant = Math.floorMod(row * 17 + col * 11, 3);
                sprite = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.obj_tree_round,
                    variant, 128, 128);
                width = height = 128;
            }
            float centerX = (col + 1) * TILE_WIDTH_PIXELS;
            float baseY = (row + 2) * TILE_HEIGHT_PIXELS;
            trees.add(new TreePlacement(sprite, Math.round(centerX - width / 2f),
                Math.round(baseY - height), Math.round(baseY)));
        }
    }

    private static boolean isHandMade(int row, int col) {
        return row < MapLayout.HAND_MADE_ROWS - 2 && col < MapLayout.HAND_MADE_COLUMNS - 2;
    }

    /** Adds a tree anchor unless another anchor is within one tile (same rule as before, O(1)). */
    private void tryAnchor(int row, int col, java.util.List<int[]> anchors, boolean[][] anchored) {
        if (layout[row][col] != Tile.TileType.TREE_TILE.ordinal()) return;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int r = row + dy, c = col + dx;
            if (r >= 0 && c >= 0 && r < NUMBER_OF_ROW_TILES && c < NUMBER_OF_COLUMN_TILES && anchored[r][c])
                return;
        }
        anchored[row][col] = true;
        anchors.add(new int[]{row, col});
    }

    private static final class TreePlacement {
        final Sprite sprite;
        final int x, y, baseY;
        TreePlacement(Sprite sprite, int x, int y, int baseY) {
            this.sprite = sprite; this.x = x; this.y = y; this.baseY = baseY;
        }
    }

    /** Draws trees that are behind the player before the player, or foreground trees after it. */
    public void drawTrees(Canvas canvas, GameDisplay display, double playerY, boolean behindPlayer) {
        Rect visible = display.getGameRect();
        for (TreePlacement tree : trees) {
            if ((tree.baseY <= playerY) != behindPlayer) continue;
            if (tree.x + tree.sprite.getWidth() < visible.left || tree.x > visible.right ||
                    tree.y + tree.sprite.getHeight() < visible.top || tree.y > visible.bottom) continue;
            tree.sprite.draw(canvas,
                (int) display.gameToDisplayCoordinatesX(tree.x),
                (int) display.gameToDisplayCoordinatesY(tree.y));
        }
    }

    /** World coordinates of a random swamp tile, or null when none is suitable. */
    public double[] randomSwampSpawn(double playerX, double playerY, java.util.Random random) {
        return randomRegionSpawn(playerX,playerY,random,MapLayout.ACID_SWAMP,300,1400);
    }

    public double[] randomRegionSpawn(double playerX,double playerY,java.util.Random random,
            int tileType,double minDistance,double maxDistance) {
        int[] candidates = tilesOfType(tileType);
        double minSquared = minDistance * minDistance, maxSquared = maxDistance * maxDistance;
        int chosen = -1, seen = 0;
        for (int index : candidates) {
            double x = (index % NUMBER_OF_COLUMN_TILES + 0.5) * TILE_WIDTH_PIXELS;
            double y = (index / NUMBER_OF_COLUMN_TILES + 0.5) * TILE_HEIGHT_PIXELS;
            double dx = x - playerX, dy = y - playerY;
            double squared = dx * dx + dy * dy;
            if (squared < minSquared || squared > maxSquared) continue;
            if (random.nextInt(++seen) == 0) chosen = index; // uniform pick without a second pass
        }
        if (chosen < 0) return null;
        return new double[]{(chosen % NUMBER_OF_COLUMN_TILES + 0.5) * TILE_WIDTH_PIXELS,
            (chosen / NUMBER_OF_COLUMN_TILES + 0.5) * TILE_HEIGHT_PIXELS};
    }

    private int[] tilesOfType(int tileType) {
        if (tileType < 0 || tileType >= tilesByType.length) return new int[0];
        int[] cached = tilesByType[tileType];
        if (cached != null) return cached;
        int count = 0;
        for (int[] row : layout) for (int type : row) if (type == tileType) count++;
        int[] result = new int[count];
        int next = 0;
        for (int row = 0; row < NUMBER_OF_ROW_TILES; row++)
            for (int col = 0; col < NUMBER_OF_COLUMN_TILES; col++)
                if (layout[row][col] == tileType) result[next++] = row * NUMBER_OF_COLUMN_TILES + col;
        tilesByType[tileType] = result;
        return result;
    }

    public void update() { animationTicks++; }

    /** Renders one chunk. Safe off the render thread: it only reads immutable layout data. */
    private Bitmap buildChunk(int row, int col) {
        int width = Math.min(CHUNK_TILES, NUMBER_OF_COLUMN_TILES - col * CHUNK_TILES) * TILE_WIDTH_PIXELS;
        int height = Math.min(CHUNK_TILES, NUMBER_OF_ROW_TILES - row * CHUNK_TILES) * TILE_HEIGHT_PIXELS;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas chunkCanvas = new Canvas(bitmap);
        for (int y = 0; y < height / TILE_HEIGHT_PIXELS; y++) {
            for (int x = 0; x < width / TILE_WIDTH_PIXELS; x++) {
                int tileRow = row * CHUNK_TILES + y;
                int tileCol = col * CHUNK_TILES + x;
                int tileType = layout[tileRow][tileCol];
                int waterColumn = tileType == Tile.TileType.WATER_TILE.ordinal()
                    ? waterAtlasColumn(tileRow, tileCol) : 0;
                int variationSeed = Math.floorMod(tileRow * 31 + tileCol * 17, 360);
                Tile tile = Tile.getTile(tileType, spriteSheet,
                    new Rect(x * TILE_WIDTH_PIXELS, y * TILE_HEIGHT_PIXELS,
                        (x + 1) * TILE_WIDTH_PIXELS, (y + 1) * TILE_HEIGHT_PIXELS),
                    waterColumn, variationSeed);
                tile.draw(chunkCanvas);
            }
        }
        bitmap.prepareToDraw(); // start the GPU upload here, not in the first frame that shows it
        return bitmap;
    }

    private Bitmap getChunk(int row, int col) {
        int key = row * chunkCols + col;
        Bitmap ready = chunks.get(key);
        if (ready != null) return ready;
        // Not prefetched in time (first frame, teleport): build now rather than draw a hole.
        Bitmap built = buildChunk(row, col);
        return chunks.compareAndSet(key, null, built) ? built : chunks.get(key);
    }

    /** Queues the visible chunks plus a margin; drops chunks that are far from the camera. */
    private void prefetch(int left, int top, int right, int bottom) {
        // Nothing to do while the camera stays inside the same chunk window (almost every frame).
        if (left == lastLeft && top == lastTop && right == lastRight && bottom == lastBottom) return;
        lastLeft = left; lastTop = top; lastRight = right; lastBottom = bottom;
        wantedLeft = left - KEEP_MARGIN; wantedTop = top - KEEP_MARGIN;
        wantedRight = right + KEEP_MARGIN; wantedBottom = bottom + KEEP_MARGIN;
        int fromRow = Math.max(0, top - PREFETCH_MARGIN), toRow = Math.min(chunkRows - 1, bottom + PREFETCH_MARGIN);
        int fromCol = Math.max(0, left - PREFETCH_MARGIN), toCol = Math.min(chunkCols - 1, right + PREFETCH_MARGIN);
        for (int row = fromRow; row <= toRow; row++) {
            for (int col = fromCol; col <= toCol; col++) {
                final int key = row * chunkCols + col;
                if (chunks.get(key) != null || !pending.compareAndSet(key, 0, 1)) continue;
                final int chunkRow = row, chunkCol = col;
                builder.execute(() -> {
                    try {
                        if (chunks.get(key) == null && isWanted(chunkRow, chunkCol))
                            chunks.compareAndSet(key, null, buildChunk(chunkRow, chunkCol));
                    } catch (RuntimeException | OutOfMemoryError e) {
                        android.util.Log.w("Tilemap", "Chunk prefetch failed", e);
                    } finally {
                        pending.set(key, 0);
                    }
                });
            }
        }
        // Drop chunks that fell out of the keep window. No recycle(): the GPU may still hold the
        // bitmap; the GC frees it once unreferenced.
        for (int key = 0; key < chunks.length(); key++) {
            if (chunks.get(key) != null && !isWanted(key / chunkCols, key % chunkCols)) chunks.set(key, null);
        }
    }

    private boolean isWanted(int row, int col) {
        return col >= wantedLeft && col <= wantedRight && row >= wantedTop && row <= wantedBottom;
    }

    /** Asynchronously builds the chunks around a world position (start-up, loaded save, teleport). */
    public void prefetchAround(double worldX, double worldY, int viewWidth, int viewHeight) {
        int left = Math.max(0, floorDiv((int) (worldX - viewWidth / 2.0), CHUNK_WIDTH));
        int right = Math.min(chunkCols - 1, floorDiv((int) (worldX + viewWidth / 2.0), CHUNK_WIDTH));
        int top = Math.max(0, floorDiv((int) (worldY - viewHeight / 2.0), CHUNK_HEIGHT));
        int bottom = Math.min(chunkRows - 1, floorDiv((int) (worldY + viewHeight / 2.0), CHUNK_HEIGHT));
        prefetch(left, top, right, bottom);
    }

    /** Stops the background builder; pending work is discarded. */
    public void release() {
        builder.shutdownNow();
        for (int key = 0; key < chunks.length(); key++) chunks.set(key, null);
    }

    /** Selects the corresponding shore/corner piece from the water autotile strip. */
    private int waterAtlasColumn(int row, int col) {
        boolean north = !isWater(row - 1, col);
        boolean east = !isWater(row, col + 1);
        boolean south = !isWater(row + 1, col);
        boolean west = !isWater(row, col - 1);
        if (north && east) return 9;   // outer north-east corner
        if (east && south) return 10;  // outer south-east corner
        if (south && west) return 11;  // outer south-west corner
        if (west && north) return 8;   // outer north-west corner
        if (north) return 4;
        if (south) return 5;
        if (west) return 6;
        if (east) return 7;

        boolean nw = !isWater(row - 1, col - 1);
        boolean ne = !isWater(row - 1, col + 1);
        boolean se = !isWater(row + 1, col + 1);
        boolean sw = !isWater(row + 1, col - 1);
        if (nw) return 12;
        if (ne) return 13;
        if (se) return 14;
        if (sw) return 15;
        return Math.floorMod(row * 13 + col * 7, 4);
    }

    private boolean isWater(int row, int col) {
        return row >= 0 && row < NUMBER_OF_ROW_TILES && col >= 0 &&
            col < NUMBER_OF_COLUMN_TILES && layout[row][col] == Tile.TileType.WATER_TILE.ordinal();
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
        prefetch(left, top, right, bottom);
        // Screen position of world (0,0). The caller scales the canvas about the screen centre when
        // zoomed, so terrain must use the same centre-relative origin as entities and trees; using
        // the visible rect's corner instead shifts the ground and leaves part of the screen empty.
        int offsetX = (int) Math.floor(display.gameToDisplayCoordinatesX(0));
        int offsetY = (int) Math.floor(display.gameToDisplayCoordinatesY(0));
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
                destination.set(l + offsetX, t + offsetY, r + offsetX, b + offsetY);
                canvas.drawBitmap(bitmap, source, destination, null);
            }
        }
        drawAnimatedTerrain(canvas, visible, offsetX, offsetY);
    }

    private void drawAnimatedTerrain(Canvas canvas, Rect visible, int offsetX, int offsetY) {
        int left = Math.max(0, (int) Math.floor(visible.left / (double) TILE_WIDTH_PIXELS));
        int top = Math.max(0, (int) Math.floor(visible.top / (double) TILE_HEIGHT_PIXELS));
        int right = Math.min(NUMBER_OF_COLUMN_TILES - 1,
            (int) Math.floor((visible.right - 1) / (double) TILE_WIDTH_PIXELS));
        int bottom = Math.min(NUMBER_OF_ROW_TILES - 1,
            (int) Math.floor((visible.bottom - 1) / (double) TILE_HEIGHT_PIXELS));
        for (int row = top; row <= bottom; row++) {
            byte[] kinds = animated[row];
            for (int col = left; col <= right; col++) {
                byte kind = kinds[col];
                if (kind == ANIM_NONE) continue;
                int phase = Math.floorMod(row * 7 + col * 11, 4);
                int frame = (animationTicks / animationSpeed[kind] + phase) % 4;
                animationFrames[kind][frame].draw(canvas,
                    col * TILE_WIDTH_PIXELS + offsetX, row * TILE_HEIGHT_PIXELS + offsetY);
            }
        }
    }

    private boolean isWaterInterior(int row, int col) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            if (!isWater(row + dy, col + dx)) return false;
        }
        return true;
    }
}
