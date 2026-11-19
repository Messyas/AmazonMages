package com.example.amazonMages.ai;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpatialGridTest {
    @Test
    public void queriesLocalNeighborsAndClearsDirtyCellsCleanly() {
        SpatialGrid grid = new SpatialGrid(12288, 12288, 16);
        grid.insert(0, 1000, 1000);
        grid.insert(1, 1040, 1020);
        grid.insert(2, 6000, 6000);
        grid.insert(3, -50, -50); // boundary safety

        int[] buf = new int[16];
        int near = grid.queryRadius(1010, 1010, 80, buf);
        Set<Integer> found = new HashSet<>();
        for (int i = 0; i < near; i++) found.add(buf[i]);
        assertTrue(found.contains(0));
        assertTrue(found.contains(1));
        assertFalse(found.contains(2));

        grid.clear();
        assertEquals(0, grid.activeCellCount());
        assertEquals(0, grid.queryRadius(1010, 1010, 500, buf));

        // Reuse across multiple ticks and dynamic capacity growth
        for (int i = 0; i < 40; i++) {
            grid.insert(i, 2000 + i * 3, 2000 + i * 2);
        }
        int cluster = grid.queryRadius(2050, 2040, 250, new int[64]);
        assertEquals(40, cluster);
    }
}
