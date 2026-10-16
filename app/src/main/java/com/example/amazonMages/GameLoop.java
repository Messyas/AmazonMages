package com.example.amazonMages;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

/** Fixed-timestep updates with a bounded catch-up budget and independently paced rendering. */
public class GameLoop extends Thread {
    public static final double MAX_UPS = 60.0;
    private static final long STEP_NANOS = 1_000_000_000L / 60L;
    private static final int MAX_CATCH_UP_UPDATES = 5;
    private final Game game;
    private final SurfaceHolder surfaceHolder;
    private volatile boolean isRunning;
    private volatile double averageUPS;
    private volatile double averageFPS;
    private volatile double averageUpdateMs;
    private volatile double averageDrawMs;

    public GameLoop(Game game, SurfaceHolder surfaceHolder) {
        this.game = game;
        this.surfaceHolder = surfaceHolder;
    }

    public double getAverageUPS() { return averageUPS; }
    public double getAverageFPS() { return averageFPS; }
    public double getAverageUpdateMs() { return averageUpdateMs; }
    public double getAverageDrawMs() { return averageDrawMs; }

    public void startLoop() {
        if (getState() != State.NEW) return;
        isRunning = true;
        start();
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        long statsStart = previous;
        long accumulator = 0L;
        int updates = 0;
        int frames = 0;
        long updateNanos = 0L;
        long drawNanos = 0L;
        while (isRunning) {
            long now = System.nanoTime();
            long elapsed = Math.max(0L, Math.min(now - previous, STEP_NANOS * MAX_CATCH_UP_UPDATES));
            previous = now;
            accumulator += elapsed;

            int catchUp = 0;
            while (isRunning && accumulator >= STEP_NANOS && catchUp < MAX_CATCH_UP_UPDATES) {
                long updateStart = System.nanoTime();
                game.update();
                updateNanos += System.nanoTime() - updateStart;
                accumulator -= STEP_NANOS;
                catchUp++;
                updates++;
            }
            if (catchUp == MAX_CATCH_UP_UPDATES && accumulator >= STEP_NANOS) {
                accumulator %= STEP_NANOS;
            }

            if (catchUp > 0 && isRunning && surfaceHolder.getSurface().isValid()) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        long drawStart = System.nanoTime();
                        game.draw(canvas);
                        drawNanos += System.nanoTime() - drawStart;
                        frames++;
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    Log.w("GameLoop", "Surface unavailable", e);
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (IllegalArgumentException | IllegalStateException e) {
                            Log.w("GameLoop", "Unable to post frame", e);
                        }
                    }
                }
            }

            now = System.nanoTime();
            long statsElapsed = now - statsStart;
            if (statsElapsed >= 1_000_000_000L) {
                averageUPS = updates * 1_000_000_000.0 / statsElapsed;
                averageFPS = frames * 1_000_000_000.0 / statsElapsed;
                averageUpdateMs = updates == 0 ? 0 : updateNanos / (1_000_000.0 * updates);
                averageDrawMs = frames == 0 ? 0 : drawNanos / (1_000_000.0 * frames);
                updates = 0;
                frames = 0;
                updateNanos = 0L;
                drawNanos = 0L;
                statsStart = now;
            }
            long waitNanos = STEP_NANOS - accumulator;
            if (waitNanos > 1_000_000L && isRunning) {
                SystemClock.sleep(Math.max(1L, (waitNanos - 500_000L) / 1_000_000L));
            } else {
                Thread.yield();
            }
        }
    }

    public void stopLoop() {
        isRunning = false;
        if (Thread.currentThread() == this || getState() == State.NEW) return;
        interrupt();
        try {
            join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
