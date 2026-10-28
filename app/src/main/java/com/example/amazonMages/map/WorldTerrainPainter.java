package com.example.amazonMages.map;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

/** Generated HD materials, reduced by the art pipeline and mirrored at seams. */
final class WorldTerrainPainter {
    private final WorldTerrainCache terrain;
    private final Paint grass, earth, negro, silt, burned, paving, wood;
    private final Paint shore = new Paint(), swamp = new Paint(), outside = new Paint();
    private final Path[] earthCorners = new Path[16];
    WorldTerrainPainter(SpriteSheet sprites, WorldTerrainCache terrain) {
        this.terrain = terrain;
        grass = material(sprites.resourceBitmap(R.drawable.world_tile_grass_v1));
        earth = material(sprites.resourceBitmap(R.drawable.world_tile_earth_v1));
        negro = material(sprites.resourceBitmap(R.drawable.world_tile_negro_v1));
        silt = material(sprites.resourceBitmap(R.drawable.world_tile_solimoes_v1));
        burned = material(sprites.resourceBitmap(R.drawable.world_tile_burned_v1));
        paving = material(sprites.resourceBitmap(R.drawable.world_tile_paving_v1));
        wood = material(sprites.resourceBitmap(R.drawable.world_tile_bridge_v1));
        shore.setColor(0xffa68c56); swamp.setColor(0x6681b13d); outside.setColor(0xff142a20);
        for (int mask = 0; mask < earthCorners.length; mask++) {
            float[] radii = new float[8];
            for (int corner = 0; corner < 4; corner++)
                radii[corner * 2] = radii[corner * 2 + 1] = (mask & (1 << corner)) != 0 ? 22 : 0;
            earthCorners[mask] = new Path();
            earthCorners[mask].addRoundRect(new RectF(0, 0, 64, 64), radii, Path.Direction.CW);
        }
    }
    static Paint material(Bitmap bitmap) {
        Paint paint = new Paint();
        paint.setFilterBitmap(false); paint.setAntiAlias(false);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR));
        return paint;
    }
    void drawArea(Canvas canvas, int firstRow, int firstCol, int rows, int cols) {
        for (int dy = 0; dy < rows; dy++) for (int dx = 0; dx < cols; dx++) {
            int row = firstRow + dy, col = firstCol + dx;
            WorldDefinition.Terrain tile = terrain.sampleTile(row, col);
            float x = dx * 64, y = dy * 64;
            int corners = 0;
            if (tile == WorldDefinition.Terrain.PATH || tile == WorldDefinition.Terrain.GROUND) {
                boolean n = green(row - 1, col), s = green(row + 1, col);
                boolean w = green(row, col - 1), e = green(row, col + 1);
                corners = (n && w ? 1 : 0) | (n && e ? 2 : 0) | (s && e ? 4 : 0) | (s && w ? 8 : 0);
            }
            if (corners == 0) canvas.drawRect(x, y, x + 64, y + 64, paint(tile));
            else {
                canvas.drawRect(x, y, x + 64, y + 64, grass);
                canvas.save(); canvas.translate(x, y); canvas.drawPath(earthCorners[corners], earth); canvas.restore();
            }
            if (tile == WorldDefinition.Terrain.ACID_SWAMP) canvas.drawRect(x, y, x + 64, y + 64, swamp);
            if (tile.isWater()) {
                if (land(row - 1, col)) canvas.drawRect(x, y, x + 64, y + 5, shore);
                if (land(row + 1, col)) canvas.drawRect(x, y + 59, x + 64, y + 64, shore);
                if (land(row, col - 1)) canvas.drawRect(x, y, x + 5, y + 64, shore);
                if (land(row, col + 1)) canvas.drawRect(x + 59, y, x + 64, y + 64, shore);
            }
        }
    }
    private boolean land(int row, int col) {
        WorldDefinition.Terrain t = terrain.sampleTile(row, col);
        return !t.isWater() && t != WorldDefinition.Terrain.BRIDGE && t != WorldDefinition.Terrain.BORDER;
    }
    private boolean green(int row, int col) {
        WorldDefinition.Terrain t = terrain.sampleTile(row, col);
        return t == WorldDefinition.Terrain.GRASS || t == WorldDefinition.Terrain.TREE;
    }
    private Paint paint(WorldDefinition.Terrain tile) {
        switch (tile) {
            case NEGRO: case MEETING_DARK: case WATER: return negro;
            case SOLIMOES: case MEETING_SILT: return silt;
            case GROUND: case PATH: return earth;
            case BURNED: case DEAD_TREE: return burned;
            case PAVING: return paving;
            case BRIDGE: return wood;
            case BORDER: return outside;
            default: return grass;
        }
    }
}
