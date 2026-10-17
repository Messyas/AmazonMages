package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;
import com.example.androidstudio2dgamedevelopment.R;

import com.example.amazonMages.graphics.SpriteSheet;

abstract class Tile {

    protected final Rect mapLocationRect;
    private final int variationSeed;

    public Tile(Rect mapLocationRect) {
        this(mapLocationRect, 0);
    }

    public Tile(Rect mapLocationRect, int variationSeed) {
        this.mapLocationRect = mapLocationRect;
        this.variationSeed = variationSeed;
    }

    protected int variant(int count) {
        return Math.floorMod(variationSeed, count);
    }

    public enum TileType {
        WATER_TILE,
        LAVA_TILE,
        GROUND_TILE,
        GRASS_TILE,
        TREE_TILE,
        ACID_SWAMP_TILE
    }

    public static Tile getTile(int idxTileType, SpriteSheet spriteSheet, Rect mapLocationRect) {
        return getTile(idxTileType, spriteSheet, mapLocationRect, 0, 0);
    }

    public static Tile getTile(int idxTileType, SpriteSheet spriteSheet, Rect mapLocationRect,
            int waterAtlasColumn, int variationSeed) {

        switch(TileType.values()[idxTileType]) {

            case WATER_TILE:
                return new WaterTile(spriteSheet, mapLocationRect, waterAtlasColumn, variationSeed);
            case LAVA_TILE:
                return new LavaTile(spriteSheet, mapLocationRect, variationSeed);
            case GROUND_TILE:
                return new GroundTile(spriteSheet, mapLocationRect, variationSeed);
            case GRASS_TILE:
                return new GrassTile(spriteSheet, mapLocationRect, variationSeed);
            case TREE_TILE:
                return new TreeTile(spriteSheet, mapLocationRect, variationSeed);
            case ACID_SWAMP_TILE:
                return new AcidSwampTile(spriteSheet, mapLocationRect, variationSeed);
            default:
                return null;
        }

    }

    public abstract void draw(Canvas canvas);
}
