package com.example.amazonMages.ai;

import java.util.Arrays;

/**
 * Zero-allocation uniform spatial hash grid over the 2D game world.
 *
 * <p>Uses flat primitive linked lists ({@code cellHead} + {@code nextInCell}) and dirty-cell
 * tracking so {@link #clear()}, {@link #insert(int, double, double)}, and
 * {@link #queryRadius(double, double, double, int[])} allocate zero heap objects on the 60 Hz
 * simulation thread and turn O(N^2) horde separation, fusion, targeting, and AoE queries into O(1)
 * neighborhood lookups.
 */
public final class SpatialGrid {
    public static final int CELL_SHIFT = 7; // 128 px per cell
    public static final int CELL_SIZE = 1 << CELL_SHIFT;
    private static final int ORIGIN_PAD = 256;

    private final int cols;
    private final int rows;
    private final int[] cellHead;
    private int[] dirtyCells;
    private int dirtyCount;
    private int[] nextInCell;

    public SpatialGrid() {
        this(192 * 64, 192 * 64, 256);
    }

    public SpatialGrid(int worldWidthPx, int worldHeightPx, int initialEntityCapacity) {
        this.cols = Math.max(8, ((worldWidthPx + ORIGIN_PAD * 2) >> CELL_SHIFT) + 2);
        this.rows = Math.max(8, ((worldHeightPx + ORIGIN_PAD * 2) >> CELL_SHIFT) + 2);
        this.cellHead = new int[cols * rows];
        Arrays.fill(this.cellHead, -1);
        int cap = Math.max(64, initialEntityCapacity);
        this.dirtyCells = new int[cap];
        this.nextInCell = new int[cap];
    }

    /** Resets only cells touched since the last clear — O(active entities), zero allocations. */
    public void clear() {
        for (int i = 0; i < dirtyCount; i++) {
            cellHead[dirtyCells[i]] = -1;
        }
        dirtyCount = 0;
    }

    public void insert(int entityIndex, double worldX, double worldY) {
        if (entityIndex < 0 || !Double.isFinite(worldX) || !Double.isFinite(worldY)) return;
        ensureEntityCapacity(entityIndex + 1);
        int col = clampCell(((int) Math.floor(worldX) + ORIGIN_PAD) >> CELL_SHIFT, cols);
        int row = clampCell(((int) Math.floor(worldY) + ORIGIN_PAD) >> CELL_SHIFT, rows);
        int cell = row * cols + col;
        if (cellHead[cell] == -1) {
            if (dirtyCount == dirtyCells.length) {
                dirtyCells = Arrays.copyOf(dirtyCells, dirtyCells.length * 2);
            }
            dirtyCells[dirtyCount++] = cell;
        }
        nextInCell[entityIndex] = cellHead[cell];
        cellHead[cell] = entityIndex;
    }

    /**
     * Writes indices of entities in cells overlapping {@code [cx - radius, cx + radius] x [cy - radius, cy + radius]}
     * into {@code outIndices} and returns how many candidates were written.
     */
    public int queryRadius(double cx, double cy, double radius, int[] outIndices) {
        if (outIndices == null || outIndices.length == 0 || dirtyCount == 0
                || !Double.isFinite(cx) || !Double.isFinite(cy) || !Double.isFinite(radius) || radius < 0) {
            return 0;
        }
        int minCol = clampCell(((int) Math.floor(cx - radius) + ORIGIN_PAD) >> CELL_SHIFT, cols);
        int maxCol = clampCell(((int) Math.floor(cx + radius) + ORIGIN_PAD) >> CELL_SHIFT, cols);
        int minRow = clampCell(((int) Math.floor(cy - radius) + ORIGIN_PAD) >> CELL_SHIFT, rows);
        int maxRow = clampCell(((int) Math.floor(cy + radius) + ORIGIN_PAD) >> CELL_SHIFT, rows);

        int count = 0;
        int limit = outIndices.length;
        for (int r = minRow; r <= maxRow; r++) {
            int rowOffset = r * cols;
            for (int c = minCol; c <= maxCol; c++) {
                int idx = cellHead[rowOffset + c];
                while (idx != -1) {
                    if (count >= limit) return count;
                    outIndices[count++] = idx;
                    idx = nextInCell[idx];
                }
            }
        }
        return count;
    }

    public int activeCellCount() {
        return dirtyCount;
    }

    private void ensureEntityCapacity(int needed) {
        if (needed <= nextInCell.length) return;
        int nextCap = Math.max(needed, nextInCell.length * 2);
        nextInCell = Arrays.copyOf(nextInCell, nextCap);
    }

    private static int clampCell(int value, int maxExclusive) {
        return value < 0 ? 0 : (value >= maxExclusive ? maxExclusive - 1 : value);
    }
}
