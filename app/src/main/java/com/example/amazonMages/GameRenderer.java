package com.example.amazonMages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.ai.EnemyManager;
import com.example.amazonMages.combat.SurvivorArsenal;
import com.example.amazonMages.combat.XpCrystalPool;
import com.example.amazonMages.ecs.*;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.GameObject;
import com.example.amazonMages.items.ItemCatalog;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.ArrayList;
import java.util.List;

/** Owns world rendering and its reusable drawing state; never mutates simulation data. */
final class GameRenderer {
    public static final class DashEmber {
        public final double x, y;
        public int ticksRemaining;
        public DashEmber(double x, double y, int ticksRemaining) {
            this.x = x; this.y = y; this.ticksRemaining = ticksRemaining;
        }
    }

    public static final class CombatFxState {
        public double vortexX, vortexY, vortexRadius = 155.0;
        public int vortexTicks;
        public final List<DashEmber> embers = new ArrayList<>();
    }

    private final List<Entity> visiblePickups = new ArrayList<>();
    private final List<GameObject> actors = new ArrayList<>();
    private final Tilemap tiles;
    private final Player player;
    private final EnemyManager enemies;
    private final ProjectileWorld projectiles;
    private final World pickups;
    private final GameDisplay display;
    private final Paint pickupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pixelPaint = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private Bitmap pickupsSheet;
    private Bitmap craftIconsSheet;
    private Bitmap boitataLootSheet;
    private Bitmap orbsSheet;
    private Bitmap lightningSheet;
    private Bitmap vortexSheet;
    private Bitmap dashSheet;
    private SurvivorArsenal arsenal;
    private XpCrystalPool xpCrystals;
    private CombatFxState combatFx;
    private int animTicks;

    GameRenderer(Tilemap tiles, Player player, EnemyManager enemies,
            ProjectileWorld projectiles, World pickups, GameDisplay display) {
        this.tiles = tiles;
        this.player = player;
        this.enemies = enemies;
        this.projectiles = projectiles;
        this.pickups = pickups;
        this.display = display;
        pickupPaint.setColor(0xFF8CD87B);
        pixelPaint.setFilterBitmap(false);
        pixelPaint.setAntiAlias(false);
        pixelPaint.setDither(false);
    }

    void attachSurvivorFx(Context context, SurvivorArsenal arsenal, XpCrystalPool xpCrystals) {
        this.arsenal = arsenal;
        this.xpCrystals = xpCrystals;
        ResourceManager res = ResourceManager.shared(context);
        pickupsSheet = res.bitmap(R.drawable.emerald_pickups, false);
        craftIconsSheet = res.bitmap(R.drawable.ui_craft_icons, false);
        boitataLootSheet = res.bitmap(R.drawable.boitata_loot, false);
        orbsSheet = res.bitmap(R.drawable.emerald_vfx_orbs, false);
        lightningSheet = res.bitmap(R.drawable.emerald_vfx_lightning, false);
        vortexSheet = res.bitmap(R.drawable.emerald_vfx_vortex, false);
        dashSheet = res.bitmap(R.drawable.emerald_vfx_dash, false);
    }

    void attachCombatFx(CombatFxState combatFx) {
        this.combatFx = combatFx;
    }

    void draw(Canvas canvas) {
        animTicks++;
        tiles.draw(canvas, display);
        drawGroundCombatFx(canvas);
        enemies.drawArenaWarnings(canvas, display);
        drawXpCrystals(canvas);
        drawWorldPickups(canvas);

        tiles.prepareScenery(display, player.getPositionX(), player.getPositionY());
        actors.clear();
        actors.add(player);
        enemies.addActorsTo(actors);
        actors.sort(java.util.Comparator.comparingDouble(GameObject::getPositionY));
        for (GameObject actor : actors) {
            tiles.drawSceneryUntil(canvas, display, actor.getPositionY());
            actor.draw(canvas, display);
        }
        tiles.drawSceneryUntil(canvas, display, Double.POSITIVE_INFINITY);
        tiles.drawCanopy(canvas, display);

        enemies.drawProjectiles(canvas, display);
        projectiles.draw(canvas, display);
        drawSurvivorFx(canvas);
    }

    private void drawGroundCombatFx(Canvas canvas) {
        if (combatFx == null) return;
        if (combatFx.vortexTicks > 0 && vortexSheet != null) {
            int frame = (animTicks / 4) % 4;
            src.set(frame * 64, 0, (frame + 1) * 64, 64);
            float sx = (float) display.gameToDisplayCoordinatesX(combatFx.vortexX);
            float sy = (float) display.gameToDisplayCoordinatesY(combatFx.vortexY);
            float r = (float) combatFx.vortexRadius;
            pixelPaint.setAlpha(215);
            dst.set(sx - r, sy - r, sx + r, sy + r);
            canvas.drawBitmap(vortexSheet, src, dst, pixelPaint);
            pixelPaint.setAlpha(255);
        }
        if (dashSheet != null && !combatFx.embers.isEmpty()) {
            for (DashEmber ember : combatFx.embers) {
                int frame = Math.min(3, Math.max(0, (90 - ember.ticksRemaining) / 22));
                src.set(frame * 64, 0, (frame + 1) * 64, 64);
                float sx = (float) display.gameToDisplayCoordinatesX(ember.x);
                float sy = (float) display.gameToDisplayCoordinatesY(ember.y);
                dst.set(sx - 34f, sy - 34f, sx + 34f, sy + 34f);
                canvas.drawBitmap(dashSheet, src, dst, pixelPaint);
            }
        }
    }

    private void drawXpCrystals(Canvas canvas) {
        if (xpCrystals == null || pickupsSheet == null) return;
        for (XpCrystalPool.Gem gem : xpCrystals.gems()) {
            float sx = (float) display.gameToDisplayCoordinatesX(gem.x);
            float sy = (float) display.gameToDisplayCoordinatesY(gem.y);
            int idx = Math.max(0, Math.min(2, gem.tier));
            src.set(idx * 32, 0, (idx + 1) * 32, 32);
            dst.set(sx - 14f, sy - 14f, sx + 14f, sy + 14f);
            canvas.drawBitmap(pickupsSheet, src, dst, pixelPaint);
        }
    }

    private void drawWorldPickups(Canvas canvas) {
        pickups.queryInto(visiblePickups, PickupComponent.class, TransformComponent.class);
        for (Entity entity : visiblePickups) {
            LifetimeComponent lifetime = entity.get(LifetimeComponent.class);
            if (lifetime != null && !lifetime.isVisible()) continue;
            TransformComponent position = entity.get(TransformComponent.class);
            float sx = (float) display.gameToDisplayCoordinatesX(position.x);
            float sy = (float) display.gameToDisplayCoordinatesY(position.y);
            String pickupId = entity.get(PickupComponent.class).itemId;
            int craftIdx = craftIconFor(pickupId);
            int boitataIdx = ItemCatalog.boitataLootFrame(pickupId);
            if (boitataIdx >= 0 && boitataLootSheet != null) {
                src.set(boitataIdx * 48, 0, (boitataIdx + 1) * 48, 48);
                dst.set(sx - 22f, sy - 22f, sx + 22f, sy + 22f);
                canvas.drawBitmap(boitataLootSheet, src, dst, pixelPaint);
            } else if (craftIdx >= 0 && craftIconsSheet != null) {
                src.set(craftIdx * 20, 0, (craftIdx + 1) * 20, 20);
                dst.set(sx - 15f, sy - 15f, sx + 15f, sy + 15f);
                canvas.drawBitmap(craftIconsSheet, src, dst, pixelPaint);
            } else if (pickupsSheet != null) {
                int idx = pickupIconFor(pickupId);
                src.set(idx * 32, 0, (idx + 1) * 32, 32);
                dst.set(sx - 16f, sy - 16f, sx + 16f, sy + 16f);
                canvas.drawBitmap(pickupsSheet, src, dst, pixelPaint);
            } else {
                canvas.drawCircle(sx, sy, 13, pickupPaint);
            }
        }
    }

    private void drawSurvivorFx(Canvas canvas) {
        if (arsenal == null) return;
        if (arsenal.orbLevel() > 0 && orbsSheet != null) {
            int count = arsenal.orbCount();
            double orbitR = arsenal.orbOrbitRadius();
            double baseAngle = arsenal.orbAngle();
            int orbFrame = (animTicks / 5) % 4;
            src.set(orbFrame * 48, 0, (orbFrame + 1) * 48, 48);
            for (int i = 0; i < count; i++) {
                double ang = baseAngle + i * (Math.PI * 2 / count);
                double wx = player.getPositionX() + Math.cos(ang) * orbitR;
                double wy = player.getPositionY() + Math.sin(ang) * orbitR;
                float sx = (float) display.gameToDisplayCoordinatesX(wx);
                float sy = (float) display.gameToDisplayCoordinatesY(wy);
                dst.set(sx - 24f, sy - 24f, sx + 24f, sy + 24f);
                canvas.drawBitmap(orbsSheet, src, dst, pixelPaint);
            }
        }
        if (lightningSheet != null) {
            for (SurvivorArsenal.LightningStrikeFx strike : arsenal.strikes()) {
                int frame = Math.min(3, Math.max(0, (20 - strike.ticksRemaining) / 5));
                src.set(frame * 64, 0, (frame + 1) * 64, 128);
                float sx = (float) display.gameToDisplayCoordinatesX(strike.x);
                float sy = (float) display.gameToDisplayCoordinatesY(strike.y);
                dst.set(sx - 32f, sy - 112f, sx + 32f, sy + 16f);
                canvas.drawBitmap(lightningSheet, src, dst, pixelPaint);
            }
        }
    }

    private static int craftIconFor(String itemId) {
        if ("elixir".equals(itemId)) return 0;
        if ("bomba_coco".equals(itemId)) return 1;
        if ("tonico".equals(itemId)) return 2;
        if ("runa_ancestral".equals(itemId)) return 3;
        return -1;
    }

    private static int pickupIconFor(String itemId) {
        if ("mana".equals(itemId)) return 4;
        if ("bencao".equals(itemId)) return 5;
        if ("cajado".equals(itemId)) return 6;
        if ("coco".equals(itemId)) return 7;
        return 3; // erva
    }
}
