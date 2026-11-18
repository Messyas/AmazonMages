package com.example.amazonMages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Debug;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.amazonMages.ai.EnemyManager;
import com.example.amazonMages.ecs.ProjectileWorld;
import com.example.amazonMages.ecs.World;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.graphics.SpriteAnimator;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.map.WorldDefinition;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Uses the actual Android Canvas renderer. Optional worldStressSeconds enables the long soak. */
@RunWith(AndroidJUnit4.class)
public class WorldStreamingTest {
    @Test public void rendersLandmarksRiversAndArenasWithinBoundedCache() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        WorldDefinition world = new WorldDefinition();
        SpriteSheet sprites = new SpriteSheet(context);
        Joystick joystick = new Joystick(0, 0, 70, 40, context);
        Player player = new Player(context, joystick, world.spawn.worldX(), world.spawn.worldY(), 32,
            new SpriteAnimator(sprites.getPlayerAnimations()));
        player.setWorldGeometry(world);
        Tilemap tiles = new Tilemap(sprites, world);
        GameDisplay display = new GameDisplay(1280, 720, player);
        GameRenderer renderer = new GameRenderer(tiles, player, new EnemyManager(context, player, tiles),
            new ProjectileWorld(0, 0, WorldDefinition.COLUMNS * 64, WorldDefinition.ROWS * 64), new World(), display);
        Bitmap frame = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(frame);
        File directory = new File(context.getExternalFilesDir(null), "world-validation");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        String[] names = {"center", "castanheira", "malocas", "ancient", "burned", "modern", "negro", "meeting", "port", "forest", "north_crossing", "west_bank"};
        double[][] locations = {{96, 100}, {96, 94}, {58, 58}, {134, 58}, {58, 134},
            {134, 134}, {78, 36}, {84, 166}, {122, 128}, {96, 52}, {96, 28}, {32, 86}};
        JSONArray captures = new JSONArray();
        List<Long> frameNanos = new ArrayList<>();
        long maxNative = 0;
        int maxCache = 0;
        try {
            for (int i = 0; i < locations.length; i++) {
                WorldDefinition.Position safe = world.safePosition(locations[i][0] * 64, locations[i][1] * 64, 32);
                player.restorePosition(safe.x, safe.y); display.update();
                tiles.prefetchAround(safe.x, safe.y, 1280, 720);
                for (int sample = 0; sample < 4; sample++) {
                    canvas.drawColor(0xff142a20); renderer.draw(canvas); tiles.update();
                }
                assertTrue(tiles.cachedTerrainBytes() <= 36 * 1024 * 1024);
                assertTrue(world.isWalkable(player.getPositionX(), player.getPositionY(), 32));
                File image = new File(directory, names[i] + ".png");
                try (FileOutputStream output = new FileOutputStream(image)) {
                    assertTrue(frame.compress(Bitmap.CompressFormat.PNG, 100, output));
                }
                captures.put(new JSONObject().put("name", names[i]).put("x", safe.x).put("y", safe.y));
            }
            // All four reserved arenas are rendered through the same production path.
            for (WorldDefinition.Site arena : world.arenas) {
                player.restorePosition(arena.center.worldX(), arena.center.worldY()); display.update();
                canvas.drawColor(0xff142a20); renderer.draw(canvas);
                try (FileOutputStream output = new FileOutputStream(new File(directory, arena.id + ".png"))) {
                    assertTrue(frame.compress(Bitmap.CompressFormat.PNG, 100, output));
                }
            }
            int seconds = Integer.parseInt(InstrumentationRegistry.getArguments().getString("worldStressSeconds", "0"));
            long started = SystemClock.elapsedRealtime();
            long finish = started + seconds * 1000L;
            int frames = 0;
            while (SystemClock.elapsedRealtime() < finish) {
                long frameStart = System.nanoTime();
                long elapsed = SystemClock.elapsedRealtime() - started;
                int scene = (int) (elapsed / 15000 % locations.length);
                double dx = Math.sin(elapsed / 2000.0) * 150, dy = Math.cos(elapsed / 1700.0) * 100;
                WorldDefinition.Position safe = world.safePosition(locations[scene][0] * 64 + dx,
                    locations[scene][1] * 64 + dy, 32);
                player.restorePosition(safe.x, safe.y); display.update();
                canvas.drawColor(0xff142a20); renderer.draw(canvas); tiles.update();
                frameNanos.add(System.nanoTime() - frameStart);
                maxCache = Math.max(maxCache, tiles.cachedTerrainBytes());
                assertTrue(maxCache <= 36 * 1024 * 1024);
                if (++frames % 120 == 0) maxNative = Math.max(maxNative, Debug.getNativeHeapAllocatedSize());
                long usedMs = (System.nanoTime() - frameStart) / 1000000;
                if (usedMs < 33) SystemClock.sleep(33 - usedMs);
            }
            Collections.sort(frameNanos);
            JSONObject report = new JSONObject().put("renderer", "Production GameRenderer / Android Canvas 1280x720")
                .put("captures", captures).put("arenas", world.arenas.size()).put("soak_seconds", seconds)
                .put("soak_frames", frames).put("max_chunk_cache_bytes", maxCache)
                .put("max_sampled_native_heap_bytes", maxNative)
                .put("resident_java_heap_bytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            if (!frameNanos.isEmpty()) report.put("draw_p95_ms", frameNanos.get((int) ((frameNanos.size() - 1) * .95)) / 1000000.0);
            try (FileOutputStream output = new FileOutputStream(new File(directory, "renderer-report.json"))) {
                output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } finally { tiles.release(); frame.recycle(); }
    }
}
