package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import android.util.LruCache;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Streams the authored circular world; memory and per-frame work depend on the camera, not world size. */
public final class Tilemap {
    private static final int CHUNK = 8, PIXELS = CHUNK * 64;
    private static final int CHUNK_BUDGET = 36 * 1024 * 1024;
    private final WorldDefinition world;
    private final WorldTerrainCache terrain;
    private final WorldTerrainPainter painter;
    private final WorldSceneryRenderer scenery;
    private final Bitmap overview;
    private final Object cacheLock = new Object();
    private final Set<Long> pending = new HashSet<>();
    private final LruCache<Long, Bitmap> chunks = new LruCache<Long, Bitmap>(CHUNK_BUDGET) {
        @Override protected int sizeOf(Long key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private final ThreadPoolExecutor builder = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(), task -> {
            Thread thread = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
                task.run();
            }, "amazon-terrain");
            thread.setDaemon(true);
            return thread;
        });
    private volatile int wantedLeft, wantedTop, wantedRight = -1, wantedBottom = -1;
    private volatile boolean closed;
    private int ticks;
    private final Paint ripple = new Paint();

    public Tilemap(SpriteSheet sprites) { this(sprites, new WorldDefinition()); }
    public Tilemap(SpriteSheet sprites, WorldDefinition world) {
        this.world = world;
        terrain = new WorldTerrainCache(world);
        painter = new WorldTerrainPainter(sprites, terrain);
        scenery = new WorldSceneryRenderer(sprites, world);
        overview = sprites.resourceBitmap(R.drawable.world_overview_v1);
        builder.allowCoreThreadTimeOut(true);
        ripple.setColor(0x558b9b99); ripple.setStrokeWidth(2);
    }
    public WorldDefinition world() { return world; }
    public Bitmap overview() { return overview; }
    public void update() { ticks = ticks == Integer.MAX_VALUE ? 0 : ticks + 1; }
    public int cachedTerrainBytes() { synchronized (cacheLock) { return chunks.size(); } }
    private static long key(int row, int col) { return ((long) row << 32) | (col & 0xffffffffL); }
    private boolean wanted(int row, int col) {
        return !closed && col >= wantedLeft && col <= wantedRight && row >= wantedTop && row <= wantedBottom;
    }
    private Bitmap build(int row, int col) {
        Bitmap bitmap = Bitmap.createBitmap(PIXELS, PIXELS, Bitmap.Config.ARGB_8888);
        painter.drawArea(new Canvas(bitmap), row * CHUNK, col * CHUNK, CHUNK, CHUNK);
        bitmap.prepareToDraw();
        return bitmap;
    }
    private synchronized void prefetch(int left, int top, int right, int bottom) {
        if (closed) return;
        wantedLeft = left - 1; wantedTop = top - 1; wantedRight = right + 1; wantedBottom = bottom + 1;
        int rows = WorldDefinition.ROWS / CHUNK, cols = WorldDefinition.COLUMNS / CHUNK;
        // Prioritize the visible rectangle and cap speculative work to the remaining cache slots.
        for (int row = top; row <= bottom; row++) for (int col = left; col <= right; col++) request(row, col);
        int extras = Math.max(0, CHUNK_BUDGET / (PIXELS * PIXELS * 4) - (right - left + 1) * (bottom - top + 1));
        for (int row = Math.max(0, wantedTop); row <= Math.min(rows - 1, wantedBottom); row++)
            for (int col = Math.max(0, wantedLeft); col <= Math.min(cols - 1, wantedRight); col++) {
                if (row >= top && row <= bottom && col >= left && col <= right) continue;
                if (extras-- > 0) request(row, col);
            }
    }
    private void request(int row, int col) {
        final long id = key(row, col);
        synchronized (cacheLock) {
            if (closed || chunks.get(id) != null || !pending.add(id)) return;
            builder.execute(() -> {
                try {
                    if (!wanted(row, col)) return;
                    Bitmap bitmap = build(row, col);
                    synchronized (cacheLock) { if (wanted(row, col)) chunks.put(id, bitmap); }
                } finally { synchronized (cacheLock) { pending.remove(id); } }
            });
        }
    }
    public void prefetchAround(double x, double y, int width, int height) {
        prefetch(Math.max(0, (int) Math.floor((x - width / 2.0) / PIXELS)),
            Math.max(0, (int) Math.floor((y - height / 2.0) / PIXELS)),
            Math.min(WorldDefinition.COLUMNS / CHUNK - 1, (int) Math.floor((x + width / 2.0) / PIXELS)),
            Math.min(WorldDefinition.ROWS / CHUNK - 1, (int) Math.floor((y + height / 2.0) / PIXELS)));
    }
    public void draw(Canvas canvas, GameDisplay display) {
        Rect view = display.getGameRect();
        int left = Math.max(0, Math.floorDiv(view.left, PIXELS));
        int top = Math.max(0, Math.floorDiv(view.top, PIXELS));
        int right = Math.min(WorldDefinition.COLUMNS / CHUNK - 1, Math.floorDiv(view.right - 1, PIXELS));
        int bottom = Math.min(WorldDefinition.ROWS / CHUNK - 1, Math.floorDiv(view.bottom - 1, PIXELS));
        prefetch(left, top, right, bottom);
        for (int row = top; row <= bottom; row++) for (int col = left; col <= right; col++) {
            Bitmap bitmap;
            synchronized (cacheLock) { bitmap = chunks.get(key(row, col)); }
            float x = (float) display.gameToDisplayCoordinatesX(col * PIXELS);
            float y = (float) display.gameToDisplayCoordinatesY(row * PIXELS);
            if (bitmap != null) canvas.drawBitmap(bitmap, x, y, null);
            else {
                // A first frame or teleport can precede prefetch. Render the same terrain directly,
                // without racing the worker to allocate a second chunk bitmap or displaying a hole.
                canvas.save(); canvas.translate(x, y);
                painter.drawArea(canvas, row * CHUNK, col * CHUNK, CHUNK, CHUNK);
                canvas.restore();
            }
        }
        // Sparse animated ripples, limited to the visible water, over the cached material.
        for (int row = Math.max(0, view.top / 64); row <= view.bottom / 64; row += 4)
            for (int col = Math.max(0, view.left / 64); col <= view.right / 64; col += 4) {
                if (!terrain.sampleTile(row, col).isWater()) continue;
                float x = (float) display.gameToDisplayCoordinatesX(col * 64 + (ticks / 10 + row * 7) % 28);
                float y = (float) display.gameToDisplayCoordinatesY(row * 64 + 28);
                canvas.drawLine(x, y, x + 12, y, ripple);
            }
        scenery.drawGround(canvas, display);
    }
    public void prepareScenery(GameDisplay display, double playerX, double playerY) {
        scenery.prepare(display, playerX, playerY);
    }
    public void drawSceneryUntil(Canvas canvas, GameDisplay display, double maxBaseY) {
        scenery.drawUntil(canvas, display, maxBaseY);
    }
    public void drawCanopy(Canvas canvas, GameDisplay display) { scenery.drawCanopy(canvas, display); }
    public double[] randomSwampSpawn(double x, double y, Random random) {
        return randomRegionSpawn(x, y, random, MapLayout.ACID_SWAMP, 300, 1400);
    }
    public double[] randomRegionSpawn(double x, double y, Random random, int tileType, double min, double max) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || min < 0 || max < min) return null;
        for (int attempt = 0; attempt < 96; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(min * min + random.nextDouble() * (max * max - min * min));
            double sx = x + Math.cos(angle) * radius, sy = y + Math.sin(angle) * radius;
            if (Math.hypot(sx - world.spawn.worldX(), sy - world.spawn.worldY()) < WorldDefinition.SAFE_RADIUS * 64) continue;
            if (terrain.sampleTile((int) Math.floor(sy / 64), (int) Math.floor(sx / 64)).id != tileType) continue;
            if (world.isWalkable(sx, sy, 32)) return new double[]{sx, sy};
        }
        return null;
    }
    public void release() {
        synchronized (cacheLock) {
            closed = true; builder.shutdownNow(); pending.clear(); chunks.evictAll();
        }
        terrain.clear();
    }
}
