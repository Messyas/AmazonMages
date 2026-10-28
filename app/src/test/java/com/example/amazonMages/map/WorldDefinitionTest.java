package com.example.amazonMages.map;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldDefinitionTest {
    private final WorldDefinition world = new WorldDefinition();

    @Test public void authoredSitesAndScaleMatchTheDesign() {
        assertEquals(5, world.landmarks.size());
        assertEquals(4, world.arenas.size());
        assertEquals(32, world.discoveries.size());
        assertEquals(299.52, WorldDefinition.RADIUS * 2 * WorldDefinition.TILE_SIZE / 400.0, .001);
        assertTrue(world.isWalkable(world.spawn.worldX(), world.spawn.worldY(), 32));
        assertEquals(WorldDefinition.Region.CENTRAL,
            world.regionAt(world.spawn.worldX(), world.spawn.worldY()));
        assertEquals(WorldDefinition.Region.INDIGENOUS, world.landmark("malocas").region);
        assertEquals(WorldDefinition.Region.ANCIENT, world.landmark("cidade_antiga").region);
        assertEquals(WorldDefinition.Region.DEAD_FOREST, world.landmark("mata_morta").region);
        assertEquals(WorldDefinition.Region.MODERN, world.landmark("cidade_moderna").region);
        for (WorldDefinition.Site site : world.landmarks) assertTrue(world.inside(site.center.col, site.center.row));
        for (WorldDefinition.Site site : world.discoveries) assertTrue(world.inside(site.center.col, site.center.row));
    }

    @Test public void everyAuthoredRouteIsWalkableWithPlayerClearance() {
        StringBuilder blocked = new StringBuilder();
        int blockedCount = 0;
        for (int routeIndex = 0; routeIndex < world.routes.size(); routeIndex++) {
            WorldDefinition.Route route = world.routes.get(routeIndex);
            for (int segment = 1; segment < route.points.size(); segment++) {
                WorldDefinition.Point a = route.points.get(segment - 1), b = route.points.get(segment);
                int samples = (int) Math.ceil(Math.hypot(b.col - a.col, b.row - a.row) * 4);
                for (int i = 0; i <= samples; i++) {
                    double t = i / (double) Math.max(1, samples);
                    double x = (a.col + (b.col - a.col) * t) * 64;
                    double y = (a.row + (b.row - a.row) * t) * 64;
                    if (!world.isWalkable(x, y, 32)) {
                        blockedCount++;
                        if (blockedCount <= 16) blocked.append("Route ").append(routeIndex)
                            .append(" at ").append(x / 64).append(',').append(y / 64).append('\n');
                    }
                }
            }
        }
        assertEquals(blocked.toString(), 0, blockedCount);
    }

    @Test public void everyArenaReservesA48By48TileCombatFloor() {
        for (WorldDefinition.Site arena : world.arenas) {
            for (int y = -24; y <= 24; y++) for (int x = -24; x <= 24; x++) {
                assertTrue(arena.id + " blocked at " + x + "," + y,
                    world.isWalkable((arena.center.col + x) * 64, (arena.center.row + y) * 64, 32));
            }
        }
    }

    @Test public void movementCannotTunnelThroughTheTrunkOrLeaveTheCircle() {
        double x = 960 * 64, y = 943 * 64;
        assertTrue(world.isWalkable(x, y, 16));
        WorldDefinition.Position moved = world.resolveMovement(x, y, 0, -256, 16);
        assertTrue(world.isWalkable(moved.x, moved.y, 16));
        assertTrue(moved.y > 940 * 64);
        assertFalse(world.isWalkable(0, 0, 32));
        assertFalse(world.isWalkable(Double.NaN, y, 32));
        assertFalse(world.isWalkable(x, y, -1));
        WorldDefinition.Position invalid = world.resolveMovement(x, y, Double.POSITIVE_INFINITY, 0, 16);
        assertEquals(x, invalid.x, 0);
        assertEquals(y, invalid.y, 0);
        WorldDefinition.Position teleport = world.resolveMovement(x, y, 100000, 0, 16);
        assertEquals(x, teleport.x, 0);
    }

    @Test public void seedChangesDetailsButLeavesAuthoredGeographyFixed() {
        WorldDefinition other = new WorldDefinition(1234);
        assertEquals(world.landmark("cidade_antiga").center.col, other.landmark("cidade_antiga").center.col, 0);
        boolean changed = false;
        for (int row = 500; row < 600; row += 2) for (int col = 800; col < 900; col += 2) {
            changed |= world.variation(row, col, 1000) != other.variation(row, col, 1000);
        }
        assertTrue(changed);
        for (WorldDefinition.Route route : world.routes) for (WorldDefinition.Point point : route.points) {
            int row = (int) point.row, col = (int) point.col;
            assertEquals(world.sampleTile(row, col), other.sampleTile(row, col));
        }
    }

    @Test public void riverColorsAndBridgeRemainDistinct() {
        assertEquals(WorldDefinition.Terrain.NEGRO, world.sampleTile(510, 160));
        assertEquals(WorldDefinition.Terrain.SOLIMOES, world.sampleTile(1120, 1400));
        assertEquals(WorldDefinition.Terrain.MEETING_DARK, world.sampleTile(1800, 660));
        assertEquals(WorldDefinition.Terrain.MEETING_SILT, world.sampleTile(1800, 680));
        boolean bridge = false;
        for (int row = 1530; row < 1740; row += 2) for (int col = 700; col < 1000; col += 2)
            bridge |= world.sampleTile(row, col) == WorldDefinition.Terrain.BRIDGE;
        assertTrue(bridge);
    }

    @Test public void negroBanksAndNorthernForestHaveMultipleGuaranteedCrossings() {
        assertEquals(WorldDefinition.Terrain.BRIDGE, world.sampleTile(413, 376));
        assertEquals(WorldDefinition.Terrain.BRIDGE, world.sampleTile(180, 770));
        assertEquals(WorldDefinition.Terrain.BRIDGE, world.sampleTile(842, 134));
        assertTrue(world.isWalkable(350 * 64, 300 * 64, 32));
        assertTrue(world.isWalkable(100 * 64, 900 * 64, 32));
        long urban = world.buildings.stream().filter(b -> b.family.equals("city_house")).count();
        long ruins = world.buildings.stream().filter(b -> b.family.equals("ruin")).count();
        assertTrue(urban >= 10); assertTrue(ruins >= 8);
    }

    @Test public void terrainCacheIsBoundedAndIndependentOfLoadOrder() {
        WorldTerrainCache forward = new WorldTerrainCache(world, 2);
        WorldTerrainCache backward = new WorldTerrainCache(world, 2);
        int[][] cells = {{1010, 960}, {440, 440}, {800, 1130}, {1480, 1480}, {330, 380}};
        for (int[] cell : cells) forward.sampleTile(cell[0], cell[1]);
        for (int i = cells.length - 1; i >= 0; i--) backward.sampleTile(cells[i][0], cells[i][1]);
        for (int[] cell : cells) {
            assertEquals(world.sampleTile(cell[0], cell[1]), forward.sampleTile(cell[0], cell[1]));
            assertEquals(forward.sampleTile(cell[0], cell[1]), backward.sampleTile(cell[0], cell[1]));
        }
        assertEquals(2, forward.residentBlocks());
        assertEquals(2048, forward.residentBytes());
        assertEquals(WorldDefinition.Terrain.BORDER, forward.sampleTile(-1, 4));
        forward.clear();
        assertEquals(0, forward.residentBlocks());
    }

    @Test public void invalidSavedPositionsAreRelocatedAndValidPositionsRemainStable() {
        WorldDefinition.Position invalid = world.safePosition(1000, 500, 32);
        assertEquals(world.spawn.worldX(), invalid.x, 0);
        assertEquals(world.spawn.worldY(), invalid.y, 0);
        WorldDefinition.Position valid = world.safePosition(world.spawn.worldX() + 100, world.spawn.worldY(), 32);
        assertEquals(world.spawn.worldX() + 100, valid.x, 0);
        assertTrue(world.isWalkable(valid.x, valid.y, 32));
        WorldDefinition.Position trunk = world.safePosition(960 * 64, 900 * 64, 32);
        assertTrue(world.isWalkable(trunk.x, trunk.y, 32));
    }

    @Test public void solidRootsBlockMovementWhileAuthoredRoutesStayOpen() {
        assertEquals(4, world.roots.size());
        WorldDefinition.Root root = world.roots.get(0);
        double x = (root.start.col + root.end.col) / 2 * 64;
        double y = (root.start.row + root.end.row) / 2 * 64;
        assertFalse(world.isWalkable(x, y, 16));
    }
}
