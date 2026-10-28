package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Only visible forest cells and authored sites are considered. Large tree surfaces use raster materials. */
final class WorldSceneryRenderer {
    private final WorldDefinition world;
    private final Bitmap tree, burnedTree, maloca, temple, theater, house, ruin;
    private final Paint bitmapPaint = new Paint(), canopy;
    private final WorldTreeParts giant;
    private final RectF target = new RectF();
    private final Path crown = new Path();
    private final List<Placement> visible = new ArrayList<>();
    private final List<Placement> pool = new ArrayList<>();
    private double playerX, playerY;
    private int cursor;

    private static final class Placement {
        Bitmap bitmap;
        float left, top, width, height, baseY;
    }

    WorldSceneryRenderer(SpriteSheet sprites, WorldDefinition world) {
        this.world = world;
        giant = new WorldTreeParts(sprites);
        tree = sprites.resourceBitmap(R.drawable.world_tree_broadleaf_v1);
        burnedTree = sprites.resourceBitmap(R.drawable.world_burned_tree_v1);
        maloca = sprites.resourceBitmap(R.drawable.world_maloca_v1);
        temple = sprites.resourceBitmap(R.drawable.world_temple_v1);
        theater = sprites.resourceBitmap(R.drawable.world_theater_v1);
        house = sprites.resourceBitmap(R.drawable.world_city_house_v1);
        ruin = sprites.resourceBitmap(R.drawable.world_ruin_v1);
        Bitmap foliage = sprites.resourceBitmap(R.drawable.world_canopy_v1);
        // An opaque repeating canopy material permits one exact alpha pass over the player.
        Bitmap canopyMaterial = Bitmap.createBitmap(foliage.getWidth(), foliage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas materialCanvas = new Canvas(canopyMaterial);
        materialCanvas.drawColor(0xff284c31);
        materialCanvas.drawBitmap(foliage, 0, 0, null);
        canopy = WorldTerrainPainter.material(canopyMaterial);
        bitmapPaint.setFilterBitmap(false);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            double wave = 1 + .035 * Math.sin(i * 2.3);
            float x = (float) ((960 + 128 * Math.cos(angle) * wave) * 64);
            float y = (float) ((770 + 96 * Math.sin(angle) * wave) * 64);
            if (i == 0) crown.moveTo(x, y); else crown.lineTo(x, y);
        }
        crown.close();
    }

    void prepare(GameDisplay display, double playerX, double playerY) {
        this.playerX = playerX; this.playerY = playerY;
        visible.clear(); cursor = 0;
        Rect view = display.getGameRect();
        for (WorldDefinition.Point anchor : world.startingTrees)
            add(tree, anchor.worldX(), anchor.worldY(), 512, 768, view);
        int left = Math.max(0, (view.left - 512) / 512), right = (view.right + 512) / 512;
        int top = Math.max(0, (view.top - 128) / 512), bottom = (view.bottom + 768) / 512;
        for (int row = top; row <= bottom; row++) for (int col = left; col <= right; col++) {
            WorldDefinition.Point anchor = world.treeAtCell(row, col);
            if (anchor == null) continue;
            add(tree, anchor.worldX(), anchor.worldY(), 512, 768, view);
        }
        // Dead trees occupy the open scar, with enough space left between their silhouettes.
        for (int row = Math.max(0, (view.top - 128) / 768); row <= (view.bottom + 768) / 768; row++)
            for (int col = Math.max(0, (view.left - 512) / 768); col <= (view.right + 512) / 768; col++) {
                int tileRow = row * 12 + 5, tileCol = col * 12 + 5;
                if (world.sampleTile(tileRow, tileCol) == WorldDefinition.Terrain.DEAD_TREE)
                    add(burnedTree, tileCol * 64, tileRow * 64, 384, 576, view);
            }
        for (WorldDefinition.Building building : world.buildings) {
            if (building.family.equals("castanheira_trunk")) continue;
            Bitmap image = building.family.equals("maloca") ? maloca
                : building.family.equals("stepped_temple") ? temple
                : building.family.equals("amazon_theater") ? theater
                : building.family.equals("city_house") ? house
                : building.family.equals("burned_tree") ? burnedTree : ruin;
            float width = (float) (building.width * 64 + 96);
            float height = Math.max((float) building.depth * 64 + 128, width * image.getHeight() / image.getWidth());
            add(image, building.base.worldX(), building.base.worldY() + 24, width, height, view);
        }
        visible.sort(Comparator.comparingDouble(p -> p.baseY));
    }

    private void add(Bitmap bitmap, double x, double y, float width, float height, Rect view) {
        if (x + width / 2 < view.left || x - width / 2 > view.right || y < view.top || y - height > view.bottom) return;
        int index = visible.size();
        if (index == pool.size()) pool.add(new Placement());
        Placement p = pool.get(index);
        p.bitmap = bitmap; p.left = (float) x - width / 2; p.top = (float) y - height;
        p.width = width; p.height = height; p.baseY = (float) y;
        visible.add(p);
    }

    void drawGround(Canvas canvas, GameDisplay display) {
        Rect view = display.getGameRect();
        if (view.right < 830 * 64 || view.left > 1090 * 64 || view.bottom < 844 * 64 || view.top > 1010 * 64) return;
        canvas.save();
        canvas.translate((float) display.gameToDisplayCoordinatesX(0), (float) display.gameToDisplayCoordinatesY(0));
        canvas.clipRect(view);
        giant.draw(canvas, world);
        canvas.restore();
    }

    void drawUntil(Canvas canvas, GameDisplay display, double maxBaseY) {
        float offsetX = (float) display.gameToDisplayCoordinatesX(0), offsetY = (float) display.gameToDisplayCoordinatesY(0);
        while (cursor < visible.size() && visible.get(cursor).baseY <= maxBaseY) {
            Placement p = visible.get(cursor++);
            target.set(p.left + offsetX, p.top + offsetY, p.left + p.width + offsetX, p.baseY + offsetY);
            boolean coversPlayer = playerX > p.left + 24 && playerX < p.left + p.width - 24
                && playerY > p.top + 24 && playerY < p.baseY - 48;
            bitmapPaint.setAlpha(coversPlayer ? 90 : 255);
            canvas.drawBitmap(p.bitmap, null, target, bitmapPaint);
        }
    }

    void drawCanopy(Canvas canvas, GameDisplay display) {
        Rect view = display.getGameRect();
        if (view.right < 824 * 64 || view.left > 1096 * 64 || view.bottom < 668 * 64 || view.top > 872 * 64) return;
        double x = (playerX / 64 - 960) / 132, y = (playerY / 64 - 770) / 100;
        canopy.setAlpha(x * x + y * y < 1.1 ? 90 : 255);
        canvas.save();
        canvas.translate((float) display.gameToDisplayCoordinatesX(0), (float) display.gameToDisplayCoordinatesY(0));
        canvas.clipRect(view); canvas.drawPath(crown, canopy); canvas.restore();
    }
}
