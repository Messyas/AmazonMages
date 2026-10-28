package com.example.amazonMages.map;

/** Compatibility constants; terrain is streamed from WorldDefinition. */
public final class MapLayout {
    public static final int ACID_SWAMP = WorldDefinition.Terrain.ACID_SWAMP.id;
    public static final int TILE_WIDTH_PIXELS = WorldDefinition.TILE_SIZE;
    public static final int TILE_HEIGHT_PIXELS = WorldDefinition.TILE_SIZE;
    public static final int NUMBER_OF_ROW_TILES = WorldDefinition.ROWS;
    public static final int NUMBER_OF_COLUMN_TILES = WorldDefinition.COLUMNS;
    public static final long WORLD_SEED = WorldDefinition.DEFAULT_SEED;
    private MapLayout() {}
}