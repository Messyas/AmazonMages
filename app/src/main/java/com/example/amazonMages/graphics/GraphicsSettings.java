package com.example.amazonMages.graphics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * User-selectable trade-off between render resolution and frame rate, persisted across sessions.
 *
 * <p>Resolution is the size of the render buffer relative to the screen; the compositor scales it
 * up, so the game world, HUD and touch input keep their native coordinates. The frame cap divides
 * the fixed 60 Hz simulation (60 or 30 FPS), which keeps frame pacing even.
 */
public final class GraphicsSettings {
    public static final int[] RESOLUTION_PERCENTS = {100, 85, 75, 60, 50};
    public static final int[] FPS_CAPS = {60, 30};
    private static final int SIMULATION_HZ = 60;
    private static final String PREFS = "graphics_settings";
    private static final String KEY_RESOLUTION = "resolution_percent";
    private static final String KEY_FPS = "fps_cap";

    private final SharedPreferences prefs;
    private int resolutionPercent;
    private int fpsCap;

    private GraphicsSettings(SharedPreferences prefs) {
        this.prefs = prefs;
        resolutionPercent = nearest(RESOLUTION_PERCENTS, prefs.getInt(KEY_RESOLUTION, 100));
        fpsCap = nearest(FPS_CAPS, prefs.getInt(KEY_FPS, 60));
    }

    public static GraphicsSettings load(Context context) {
        return new GraphicsSettings(context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    /** Render buffer size relative to the view: 1.0 is native. */
    public float scale() { return resolutionPercent / 100f; }
    public int resolutionPercent() { return resolutionPercent; }
    public int fpsCap() { return fpsCap; }
    /** Simulation ticks between two presented frames (1 for 60 FPS, 2 for 30 FPS). */
    public int frameInterval() { return Math.max(1, SIMULATION_HZ / fpsCap); }

    public void cycleResolution() {
        resolutionPercent = next(RESOLUTION_PERCENTS, resolutionPercent);
        save();
    }

    public void cycleFps() {
        fpsCap = next(FPS_CAPS, fpsCap);
        save();
    }

    private void save() {
        prefs.edit().putInt(KEY_RESOLUTION, resolutionPercent).putInt(KEY_FPS, fpsCap).apply();
    }

    private static int next(int[] options, int current) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) return options[(i + 1) % options.length];
        }
        return options[0];
    }

    private static int nearest(int[] options, int value) {
        int best = options[0];
        for (int option : options) {
            if (Math.abs(option - value) < Math.abs(best - value)) best = option;
        }
        return best;
    }
}
