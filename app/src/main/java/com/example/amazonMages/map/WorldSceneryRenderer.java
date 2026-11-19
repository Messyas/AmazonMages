package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Depth-sorted GBA 2x scenery renderer: trees, buildings, Castanheira and micro-props. */
final class WorldSceneryRenderer {
    private final WorldDefinition world;
    private final Bitmap treesSheet, castanheira, maloca, temple, theater, house, ruin, burnedTree;
    private final Bitmap flowersSheet, bushSheet, rockSheet;
    private final Bitmap sanctuaryNw, sanctuaryNe, sanctuarySw, sanctuarySe, sanctuarySigil;
    private final Bitmap boitataProps;
    private final Paint bitmapPaint = new Paint();
    private final Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect src = new Rect();
    private final RectF target = new RectF();
    private final Paint arenaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Placement> visible = new ArrayList<>();
    private final List<Placement> pool = new ArrayList<>();
    private double playerX, playerY;
    private int cursor;

    private static final class Placement {
        Bitmap bitmap;
        int srcLeft, srcTop, srcRight, srcBottom;
        float left, top, width, height, baseY;
        boolean fadeBehind;
    }

    WorldSceneryRenderer(SpriteSheet sprites, WorldDefinition world) {
        this.world = world;
        treesSheet = sprites.resourceBitmap(R.drawable.emerald_scenery_trees);
        castanheira = sprites.resourceBitmap(R.drawable.emerald_castanheira);
        maloca = sprites.resourceBitmap(R.drawable.emerald_struct_maloca);
        temple = sprites.resourceBitmap(R.drawable.emerald_struct_temple);
        theater = sprites.resourceBitmap(R.drawable.emerald_struct_theater);
        house = sprites.resourceBitmap(R.drawable.emerald_struct_house);
        ruin = sprites.resourceBitmap(R.drawable.emerald_struct_ruin);
        burnedTree = sprites.resourceBitmap(R.drawable.emerald_struct_burned_tree);
        flowersSheet = sprites.resourceBitmap(R.drawable.obj_flowers);
        bushSheet = sprites.resourceBitmap(R.drawable.obj_bush);
        rockSheet = sprites.resourceBitmap(R.drawable.obj_rock);
        sanctuaryNw = sprites.resourceBitmap(R.drawable.emerald_struct_sanctuary_nw);
        sanctuaryNe = sprites.resourceBitmap(R.drawable.emerald_struct_sanctuary_ne);
        sanctuarySw = sprites.resourceBitmap(R.drawable.emerald_struct_sanctuary_sw);
        sanctuarySe = sprites.resourceBitmap(R.drawable.emerald_struct_sanctuary_se);
        sanctuarySigil = sprites.resourceBitmap(R.drawable.emerald_sanctuary_sigil);
        boitataProps = sprites.resourceBitmap(R.drawable.boitata_arena_props);
        bitmapPaint.setFilterBitmap(false);
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setDither(false);
    }

    void prepare(GameDisplay display, double playerX, double playerY) {
        this.playerX = playerX;
        this.playerY = playerY;
        visible.clear();
        cursor = 0;
        Rect view = display.getGameRect();
        if (arenaVisible(view)) addArenaProps(view);

        for (WorldDefinition.Point anchor : world.startingTrees) {
            addTreeFrame(0, anchor.worldX(), anchor.worldY() + 18, view);
        }

        int cellSizePx = 4 * WorldDefinition.TILE_SIZE;
        int leftCell = Math.max(0, (view.left - 192) / cellSizePx);
        int rightCell = Math.min(WorldDefinition.COLUMNS / 4 - 1, (view.right + 192) / cellSizePx);
        int topCell = Math.max(0, (view.top - 128) / cellSizePx);
        int bottomCell = Math.min(WorldDefinition.ROWS / 4 - 1, (view.bottom + 256) / cellSizePx);
        for (int row = topCell; row <= bottomCell; row++) {
            for (int col = leftCell; col <= rightCell; col++) {
                WorldDefinition.Point anchor = world.treeAtCell(row, col);
                if (anchor != null) {
                    int variant = world.variation(row, col, 6);
                    int frame = variant == 5 ? 2 : (variant % 2);
                    addTreeFrame(frame, anchor.worldX(), anchor.worldY() + 16, view);
                    continue;
                }
                // Non-blocking micro-props on open grass shoulders to enrich Hoenn exploration feel.
                int propCol = col * 4 + 2;
                int propRow = row * 4 + 2;
                WorldDefinition.Terrain t = world.sampleTile(propRow, propCol);
                if (t == WorldDefinition.Terrain.GRASS && world.variation(propRow, propCol, 3) == 0) {
                    int kind = world.variation(propRow + 7, propCol + 13, 5);
                    double px = (propCol + 0.5) * WorldDefinition.TILE_SIZE;
                    double py = (propRow + 0.85) * WorldDefinition.TILE_SIZE;
                    if (kind <= 2 && flowersSheet != null) {
                        int f = world.variation(propRow, propCol, 6);
                        addSheet(flowersSheet, f * 64, 0, 64, 64, px, py, 56, 56, false, view);
                    } else if (kind == 3 && bushSheet != null) {
                        int b = world.variation(propRow, propCol, 4);
                        addSheet(bushSheet, b * 64, 0, 64, 64, px, py, 54, 54, false, view);
                    } else if (rockSheet != null && world.roadDistance(propCol + 0.5, propRow + 0.5) > 2.6) {
                        int r = world.variation(propRow, propCol, 4);
                        addSheet(rockSheet, r * 64, 0, 64, 64, px, py, 48, 48, false, view);
                    }
                }
            }
        }

        // Dead trees inside Mata Morta scar
        int startTileRow = Math.max(0, (view.top - 128) / WorldDefinition.TILE_SIZE);
        int endTileRow = Math.min(WorldDefinition.ROWS - 1, (view.bottom + 256) / WorldDefinition.TILE_SIZE);
        int startTileCol = Math.max(0, (view.left - 128) / WorldDefinition.TILE_SIZE);
        int endTileCol = Math.min(WorldDefinition.COLUMNS - 1, (view.right + 128) / WorldDefinition.TILE_SIZE);
        for (int row = startTileRow; row <= endTileRow; row++) {
            for (int col = startTileCol; col <= endTileCol; col++) {
                if (world.sampleTile(row, col) == WorldDefinition.Terrain.DEAD_TREE) {
                    addTreeFrame(3, (col + 0.5) * WorldDefinition.TILE_SIZE, (row + 0.85) * WorldDefinition.TILE_SIZE, view);
                }
            }
        }

        for (WorldDefinition.Building building : world.buildings) {
            Bitmap image;
            float width, height;
            if (building.family.equals("castanheira_trunk")) {
                image = castanheira;
                width = 384f;
                height = 448f;
                addSheet(image, 0, 0, image.getWidth(), image.getHeight(),
                    building.base.worldX(), building.base.worldY() + 28, width, height, true, view);
                continue;
            }
            boolean small = building.width < 2.1;
            if (building.family.equals("maloca")) {
                image = maloca;
                width = small ? 144f : 224f;
            } else if (building.family.equals("stepped_temple")) {
                image = temple;
                width = 256f;
            } else if (building.family.equals("amazon_theater")) {
                image = theater;
                width = 288f;
            } else if (building.family.equals("city_house")) {
                image = house;
                width = small ? 120f : 160f;
            } else if (building.family.equals("burned_tree")) {
                image = burnedTree;
                width = small ? 112f : 160f;
            } else if (building.family.equals("sanctuary_bastion_nw")) {
                image = sanctuaryNw;
                width = 168f;
            } else if (building.family.equals("sanctuary_bastion_ne")) {
                image = sanctuaryNe;
                width = 168f;
            } else if (building.family.equals("sanctuary_bastion_sw")) {
                image = sanctuarySw;
                width = 168f;
            } else if (building.family.equals("sanctuary_bastion_se")) {
                image = sanctuarySe;
                width = 168f;
            } else {
                image = ruin;
                width = small ? 112f : 160f;
            }
            height = width * image.getHeight() / image.getWidth();
            addSheet(image, 0, 0, image.getWidth(), image.getHeight(),
                building.base.worldX(), building.base.worldY() + 18, width, height, true, view);
        }
        visible.sort(Comparator.comparingDouble(p -> p.baseY));
    }

    private boolean arenaVisible(Rect view) {
        BoitataArena arena = world.boitataArena;
        double extent = arena.radius + 220;
        return arena.x + extent > view.left && arena.x - extent < view.right
            && arena.y + extent > view.top && arena.y - extent < view.bottom;
    }

    private void addArenaProps(Rect view) {
        if (boitataProps == null) return;
        BoitataArena arena = world.boitataArena;
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16 - Math.PI / 2;
            double fromGate = Math.min(i, 32 - i) * Math.PI / 16;
            if (fromGate <= BoitataArena.GATE_HALF_ANGLE) continue;
            addSheet(boitataProps, 0, 0, 128, 128, arena.x + Math.cos(angle) * arena.radius,
                arena.y + Math.sin(angle) * arena.radius + 34, 118, 86, true, view);
        }
        for (int side = -1; side <= 1; side += 2) {
            addSheet(boitataProps, 128, 0, 128, 128,
                arena.x + side * Math.sin(BoitataArena.GATE_HALF_ANGLE) * arena.radius,
                arena.y - Math.cos(BoitataArena.GATE_HALF_ANGLE) * arena.radius + 38,
                118, 118, true, view);
        }
        for (int i = 0; i < 7; i++) {
            double angle = i * .86 - .27;
            double radius = arena.radius + 52 + (i % 3) * 22;
            addSheet(boitataProps, 256, 0, 128, 128, arena.x + Math.cos(angle) * radius,
                arena.y + Math.sin(angle) * radius + 20, 102, 94, true, view);
        }
    }

    private void addTreeFrame(int frame, double x, double y, Rect view) {
        if (treesSheet == null) return;
        addSheet(treesSheet, frame * 128, 0, 128, 160, x, y, 128f, 160f, true, view);
    }

    private void addSheet(Bitmap bitmap, int sx, int sy, int sw, int sh,
            double x, double y, float width, float height, boolean fadeBehind, Rect view) {
        if (bitmap == null) return;
        if (x + width / 2 < view.left || x - width / 2 > view.right
                || y < view.top || y - height > view.bottom) return;
        int index = visible.size();
        if (index == pool.size()) pool.add(new Placement());
        Placement p = pool.get(index);
        p.bitmap = bitmap;
        p.srcLeft = sx;
        p.srcTop = sy;
        p.srcRight = sx + sw;
        p.srcBottom = sy + sh;
        p.left = (float) x - width / 2;
        p.top = (float) y - height;
        p.width = width;
        p.height = height;
        p.baseY = (float) y;
        p.fadeBehind = fadeBehind;
        visible.add(p);
    }

    void drawGround(Canvas canvas, GameDisplay display) {
        if (arenaVisible(display.getGameRect())) drawArenaGround(canvas, display);
        if (sanctuarySigil != null) {
            float sx = (float) display.gameToDisplayCoordinatesX(world.spawn.worldX());
            float sy = (float) display.gameToDisplayCoordinatesY(world.spawn.worldY());
            float r = 128f;
            target.set(sx - r, sy - r, sx + r, sy + r);
            src.set(0, 0, sanctuarySigil.getWidth(), sanctuarySigil.getHeight());
            canvas.drawBitmap(sanctuarySigil, src, target, bitmapPaint);
        }
        drawSanctuaryAura(canvas, display);
    }

    private void drawArenaGround(Canvas canvas, GameDisplay display) {
        BoitataArena arena = world.boitataArena;
        float x = (float) display.gameToDisplayCoordinatesX(arena.x);
        float y = (float) display.gameToDisplayCoordinatesY(arena.y);
        float radius = (float) arena.radius;
        float gate = (float) Math.toDegrees(BoitataArena.GATE_HALF_ANGLE);
        target.set(x - radius, y - radius, x + radius, y + radius);
        arenaPaint.setStyle(Paint.Style.STROKE);
        arenaPaint.setStrokeWidth((float) BoitataArena.WALL_HALF_WIDTH * 2);
        arenaPaint.setColor(0xFF24272A);
        canvas.drawArc(target, -90 + gate, 360 - 2 * gate, false, arenaPaint);
        arenaPaint.setStrokeWidth(17);
        arenaPaint.setColor(0xFF505050);
        canvas.drawArc(target, -90 + gate, 360 - 2 * gate, false, arenaPaint);
        arenaPaint.setStrokeWidth(2);
        for (int i = 0; i < 22; i++) {
            double angle = i * 2.399963;
            float distance = radius * (.23f + (i % 8) * .085f);
            float px = x + (float) Math.cos(angle) * distance;
            float py = y + (float) Math.sin(angle) * distance;
            arenaPaint.setColor(i % 4 == 0 ? 0xD0A24B29 : 0xB65A392F);
            float width = 12 + i % 5 * 5;
            canvas.drawLine(px - width, py + 7, px, py, arenaPaint);
            canvas.drawLine(px, py, px + width * .65f, py + 6, arenaPaint);
        }
    }

    private void drawSanctuaryAura(Canvas canvas, GameDisplay display) {
        long time = System.currentTimeMillis();
        float sx = (float) display.gameToDisplayCoordinatesX(world.spawn.worldX());
        float sy = (float) display.gameToDisplayCoordinatesY(world.spawn.worldY());
        float radiusPx = (float) (WorldDefinition.SAFE_RADIUS * WorldDefinition.TILE_SIZE);
        float pulse = (float) (Math.sin(time / 420.0) * 4.0);

        // Glowing emerald leaf magic boundary ring
        auraPaint.setStyle(Paint.Style.STROKE);
        auraPaint.setStrokeWidth(3.5f);
        auraPaint.setColor(0x803DC45E);
        canvas.drawCircle(sx, sy, radiusPx + pulse, auraPaint);

        auraPaint.setStrokeWidth(1.5f);
        auraPaint.setColor(0xC09BF06A);
        canvas.drawCircle(sx, sy, radiusPx + pulse * 0.5f, auraPaint);

        // Orbiting/floating leaf rune motes around perimeter
        auraPaint.setStyle(Paint.Style.FILL);
        auraPaint.setColor(0xD0FFF1B0);
        for (int i = 0; i < 8; i++) {
            double angle = (time / 1800.0) + (i * Math.PI / 4.0);
            float lx = sx + (float) (Math.cos(angle) * (radiusPx + pulse));
            float ly = sy + (float) (Math.sin(angle) * (radiusPx + pulse));
            canvas.drawCircle(lx, ly, 3.5f, auraPaint);
        }
    }

    void drawUntil(Canvas canvas, GameDisplay display, double maxBaseY) {
        float offsetX = (float) display.gameToDisplayCoordinatesX(0);
        float offsetY = (float) display.gameToDisplayCoordinatesY(0);
        while (cursor < visible.size() && visible.get(cursor).baseY <= maxBaseY) {
            Placement p = visible.get(cursor++);
            src.set(p.srcLeft, p.srcTop, p.srcRight, p.srcBottom);
            target.set(p.left + offsetX, p.top + offsetY, p.left + p.width + offsetX, p.baseY + offsetY);
            boolean coversPlayer = p.fadeBehind
                && playerX > p.left + 16 && playerX < p.left + p.width - 16
                && playerY > p.top + 12 && playerY < p.baseY - 36;
            bitmapPaint.setAlpha(coversPlayer ? 115 : 255);
            canvas.drawBitmap(p.bitmap, src, target, bitmapPaint);
        }
    }

    void drawCanopy(Canvas canvas, GameDisplay display) {
        // No separate giant canopy overlay needed on compact GBA scale.
    }
}
