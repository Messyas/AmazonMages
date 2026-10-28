package com.example.amazonMages.map;

import java.util.LinkedHashMap;

/** Bounded, thread-safe cache of immutable 32x32 terrain blocks for the new world. */
public final class WorldTerrainCache {
    public static final int BLOCK_SIZE = 32;
    public static final int DEFAULT_CAPACITY = 128;
    private static final WorldDefinition.Terrain[] TYPES = WorldDefinition.Terrain.values();
    private final WorldDefinition world;
    private final int capacity;
    private final LinkedHashMap<Long, byte[]> blocks = new LinkedHashMap<>(16, .75f, true);

    public WorldTerrainCache(WorldDefinition world) { this(world, DEFAULT_CAPACITY); }

    public WorldTerrainCache(WorldDefinition world, int capacity) {
        if (world == null || capacity < 1) throw new IllegalArgumentException("world and positive capacity required");
        this.world = world;
        this.capacity = capacity;
    }

    public synchronized WorldDefinition.Terrain sampleTile(int row, int col) {
        if (row < 0 || col < 0 || row >= WorldDefinition.ROWS || col >= WorldDefinition.COLUMNS)
            return WorldDefinition.Terrain.BORDER;
        int blockRow = row / BLOCK_SIZE, blockCol = col / BLOCK_SIZE;
        long key = ((long) blockRow << 32) | (blockCol & 0xffffffffL);
        byte[] block = blocks.get(key);
        if (block == null) {
            block = new byte[BLOCK_SIZE * BLOCK_SIZE];
            for (int y = 0; y < BLOCK_SIZE; y++) {
                for (int x = 0; x < BLOCK_SIZE; x++) {
                    block[y * BLOCK_SIZE + x] = (byte) world.sampleTile(
                        blockRow * BLOCK_SIZE + y, blockCol * BLOCK_SIZE + x).id;
                }
            }
            if (blocks.size() >= capacity) {
                Long oldest = blocks.keySet().iterator().next();
                blocks.remove(oldest);
            }
            blocks.put(key, block);
        }
        int id = block[(row % BLOCK_SIZE) * BLOCK_SIZE + col % BLOCK_SIZE] & 0xff;
        for (WorldDefinition.Terrain terrain : TYPES) if (terrain.id == id) return terrain;
        throw new IllegalStateException("Unknown terrain ID " + id);
    }

    public synchronized int residentBlocks() { return blocks.size(); }
    public synchronized int residentBytes() { return blocks.size() * BLOCK_SIZE * BLOCK_SIZE; }
    public synchronized void clear() { blocks.clear(); }
}
