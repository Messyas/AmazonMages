package com.example.amazonMages.map;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldDefinitionTest {
    private final WorldDefinition world = new WorldDefinition();

    @Test public void authoredSitesAndCompactScaleMatchTheDesign() {
        assertEquals(5, world.landmarks.size());
        assertEquals(4, world.arenas.size());
        assertEquals(12, world.discoveries.size());
        assertEquals(28.16, WorldDefinition.RADIUS * 2 * WorldDefinition.TILE_SIZE / 400.0, .001);
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

    @Test public void everyArenaReservesAClean12By12CombatFloor() {
        for (WorldDefinition.Site arena : world.arenas) {
            for (int y = -6; y <= 6; y++) for (int x = -6; x <= 6; x++) {
                assertTrue(arena.id + " blocked at " + x + "," + y,
                    world.isWalkable((arena.center.col + x) * 64, (arena.center.row + y) * 64, 32));
            }
        }
    }

    @Test public void movementCannotTunnelThroughTheTrunkOrLeaveTheCircle() {
        double x = 96 * 64, y = 93 * 64;
        assertTrue(world.isWalkable(x, y, 16));
        WorldDefinition.Position moved = world.resolveMovement(x, y, 0, -256, 16);
        assertTrue(world.isWalkable(moved.x, moved.y, 16));
        assertTrue(moved.y >= 92 * 64);
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
        for (int row = 40; row < 80; row += 2) for (int col = 80; col < 120; col += 2) {
            changed |= world.variation(row, col, 1000) != other.variation(row, col, 1000);
        }
        assertTrue(changed);
        for (WorldDefinition.Route route : world.routes) for (WorldDefinition.Point point : route.points) {
            int row = (int) point.row, col = (int) point.col;
            assertEquals(world.sampleTile(row, col), other.sampleTile(row, col));
        }
    }

    @Test public void riverColorsAndBridgeRemainDistinct() {
        assertEquals(WorldDefinition.Terrain.NEGRO, world.sampleTile(52, 72));
        assertEquals(WorldDefinition.Terrain.SOLIMOES, world.sampleTile(104, 128));
        assertEquals(WorldDefinition.Terrain.MEETING_DARK, world.sampleTile(172, 80));
        assertEquals(WorldDefinition.Terrain.MEETING_SILT, world.sampleTile(172, 84));
        int bridgeCount = 0;
        for (int row = 10; row < WorldDefinition.ROWS - 10; row++) {
            for (int col = 10; col < WorldDefinition.COLUMNS - 10; col++) {
                if (world.sampleTile(row, col) == WorldDefinition.Terrain.BRIDGE) bridgeCount++;
            }
        }
        assertTrue("Expected multiple river bridges, found " + bridgeCount, bridgeCount >= 20);
    }

    @Test public void regionalBuildingsArePlacedWithoutBlockingCrossings() {
        long urban = world.buildings.stream().filter(b -> b.family.equals("city_house")).count();
        long ruins = world.buildings.stream().filter(b -> b.family.equals("ruin")).count();
        assertTrue(urban >= 7);
        assertTrue(ruins >= 6);
    }

    @Test public void terrainCacheIsBoundedAndIndependentOfLoadOrder() {
        WorldTerrainCache forward = new WorldTerrainCache(world, 2);
        WorldTerrainCache backward = new WorldTerrainCache(world, 2);
        int[][] cells = {{100, 96}, {58, 58}, {124, 58}, {134, 134}, {36, 112}};
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
        WorldDefinition.Position invalid = world.safePosition(100, 100, 32);
        assertEquals(world.spawn.worldX(), invalid.x, 0);
        assertEquals(world.spawn.worldY(), invalid.y, 0);
        WorldDefinition.Position valid = world.safePosition(world.spawn.worldX() + 100, world.spawn.worldY(), 32);
        assertEquals(world.spawn.worldX() + 100, valid.x, 0);
        assertTrue(world.isWalkable(valid.x, valid.y, 32));
        WorldDefinition.Position trunk = world.safePosition(96 * 64, 91 * 64, 32);
        assertTrue(world.isWalkable(trunk.x, trunk.y, 32));
    }

    @Test public void solidRootsBlockMovementWhileAuthoredRoutesStayOpen() {
        assertEquals(4, world.roots.size());
        WorldDefinition.Root root = world.roots.get(0);
        double x = (root.start.col + root.end.col) / 2 * 64;
        double y = (root.start.row + root.end.row) / 2 * 64;
        assertFalse(world.isWalkable(x, y, 16));
    }

    @Test public void sanctuaryZoneHasZeroDangerAndBastionsHaveSolidCollision() {
        assertTrue(world.isInsideSanctuary(world.spawn.worldX(), world.spawn.worldY()));
        assertEquals(0.0, world.dangerFactorAt(world.spawn.worldX(), world.spawn.worldY()), 0.001);

        // Calm inner ring
        double calmDanger = world.dangerFactorAt((96 + 18) * 64, 100 * 64);
        assertTrue(calmDanger >= 0.35 && calmDanger <= 0.60);

        // Outer chaotic borders
        double borderDanger = world.dangerFactorAt((96 + 70) * 64, 100 * 64);
        assertTrue(borderDanger >= 0.90 && borderDanger <= 1.35);

        // Center courtyard is walkable while bastion towers block movement
        assertTrue(world.isWalkable(world.spawn.worldX(), world.spawn.worldY(), 32));
        assertFalse(world.isWalkable(88.5 * 64, (94.0 - 0.8) * 64, 16));
        assertFalse(world.isWalkable(103.5 * 64, (94.0 - 0.8) * 64, 16));
        assertFalse(world.isWalkable(88.5 * 64, (106.5 - 0.8) * 64, 16));
        assertFalse(world.isWalkable(103.5 * 64, (106.5 - 0.8) * 64, 16));
    }
}
