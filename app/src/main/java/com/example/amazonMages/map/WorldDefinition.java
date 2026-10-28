package com.example.amazonMages.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, Android-independent geography for the new Amazon world.
 * Coordinates in descriptors are tiles; movement and spawn APIs use world pixels.
 * Geography stays fixed when the detail seed changes. No world-sized allocation is needed.
 */
public final class WorldDefinition {
    public static final int COLUMNS = 1920, ROWS = 1920, TILE_SIZE = 64;
    public static final int CENTER = 960, RADIUS = 936, REVISION = 1;
    public static final long DEFAULT_SEED = 20261027L;
    public static final double SAFE_RADIUS = 48;

    public enum Terrain {
        WATER(0, false), LAVA(1, false), GROUND(2, true), GRASS(3, true),
        TREE(4, false), ACID_SWAMP(5, true), NEGRO(6, false), SOLIMOES(7, false),
        MEETING_DARK(8, false), MEETING_SILT(9, false), BURNED(10, true),
        PAVING(11, true), BRIDGE(12, true), BORDER(13, false), PATH(14, true), DEAD_TREE(15, false);

        public final int id;
        public final boolean walkable;
        Terrain(int id, boolean walkable) { this.id = id; this.walkable = walkable; }
        public boolean isWater() {
            return this == WATER || this == NEGRO || this == SOLIMOES
                || this == MEETING_DARK || this == MEETING_SILT;
        }
    }

    public enum Region { CENTRAL, INDIGENOUS, ANCIENT, DEAD_FOREST, MODERN, FOREST, OUTSIDE }

    public static final class Point {
        public final double col, row;
        public Point(double col, double row) { this.col = col; this.row = row; }
        public double worldX() { return col * TILE_SIZE; }
        public double worldY() { return row * TILE_SIZE; }
    }

    /** Movement result in world pixels, distinct from tile-space map points. */
    public static final class Position {
        public final double x, y;
        private Position(double x, double y) { this.x = x; this.y = y; }
    }

    public static final class Site {
        public final String id, displayName;
        public final Point center;
        public final Region region;
        public final double radius;
        private Site(String id, String name, double col, double row, Region region, double radius) {
            this.id = id; this.displayName = name; this.center = new Point(col, row);
            this.region = region; this.radius = radius;
        }
    }

    public static final class Building {
        public final String id, family;
        public final Point base;
        public final double width, depth;
        private Building(String id, String family, double col, double row, double width, double depth) {
            this.id = id; this.family = family; this.base = new Point(col, row);
            this.width = width; this.depth = depth;
        }
        boolean contains(double col, double row) {
            return Math.abs(col - base.col) < width / 2 && row > base.row - depth && row < base.row;
        }
    }

    public static final class Root {
        public final Point start, end;
        public final double baseWidth, tipWidth;
        private Root(double x, double y, double ex, double ey, double width, double tip) {
            start = p(x, y); end = p(ex, ey); baseWidth = width; tipWidth = tip;
        }
        private boolean blocks(double x, double y, double radius) {
            double dx = end.col - start.col, dy = end.row - start.row;
            double t = Math.max(0, Math.min(1, ((x - start.col) * dx + (y - start.row) * dy) / (dx * dx + dy * dy)));
            return distance(x, y, start.col + dx * t, start.row + dy * t)
                < baseWidth + (tipWidth - baseWidth) * t + radius;
        }
    }

    public static final class Route {
        public final List<Point> points;
        public final double width;
        private Route(double width, Point... points) {
            this.width = width;
            List<Point> copy = new ArrayList<>();
            Collections.addAll(copy, points);
            this.points = Collections.unmodifiableList(copy);
        }
    }

    private static final Point[] NEGRO = {
        p(1050, -20), p(850, 120), p(650, 270), p(430, 390), p(160, 510), p(-40, 620)
    };
    private static final Point[] NEGRO_ARM = {
        p(160, 510), p(130, 900), p(250, 1250), p(450, 1520), p(640, 1700), p(780, 1630)
    };
    private static final Point[] SOLIMOES = {
        p(1960, 740), p(1690, 880), p(1400, 1120), p(1180, 1280), p(1000, 1430), p(780, 1630)
    };
    private static final Point[] MEETING = { p(780, 1630), p(570, 1960) };
    private static final Point[] PORT_CHANNEL = {
        p(1690, 880), p(1670, 1120), p(1610, 1320), p(1588, 1440), p(1588, 1540)
    };
    private static final Point[] SWAMPS = {
        p(730, 1120), p(1230, 900), p(1150, 590), p(700, 650), p(1150, 1580), p(620, 1300)
    };

    public final long detailSeed;
    public final Point spawn = p(960, 1010);
    public final List<Site> landmarks, arenas, discoveries;
    public final List<Building> buildings;
    public final List<Route> routes;
    public final List<Root> roots;
    public final List<Point> startingTrees;

    public WorldDefinition() { this(DEFAULT_SEED); }

    public WorldDefinition(long seed) {
        detailSeed = seed;
        List<Point> entryTrees = new ArrayList<>();
        entryTrees.add(p(947.5, 1017.5)); entryTrees.add(p(943.5, 1021.5));
        entryTrees.add(p(973.5, 1017.5)); entryTrees.add(p(977.5, 1021.5));
        startingTrees = Collections.unmodifiableList(entryTrees);
        List<Root> rootParts = new ArrayList<>();
        rootParts.add(new Root(946, 938, 952, 1006, 7, 1));
        rootParts.add(new Root(974, 938, 968, 1006, 7, 1));
        rootParts.add(new Root(939, 932, 885, 970, 8, 1));
        rootParts.add(new Root(981, 932, 1035, 970, 8, 1));
        roots = Collections.unmodifiableList(rootParts);
        List<Site> major = new ArrayList<>();
        major.add(site("castanheira", "Castanheira Ancestral", 960, 940, Region.CENTRAL, 115));
        major.add(site("malocas", "Malocas da Floresta", 440, 440, Region.INDIGENOUS, 80));
        major.add(site("cidade_antiga", "Cidade Antiga", 1480, 440, Region.ANCIENT, 80));
        major.add(site("mata_morta", "Mata Morta", 440, 1480, Region.DEAD_FOREST, 160));
        major.add(site("cidade_moderna", "Cidade das Águas", 1480, 1480, Region.MODERN, 90));
        landmarks = Collections.unmodifiableList(major);

        List<Site> reserved = new ArrayList<>();
        reserved.add(site("arena_storm", "Clareira das Tempestades", 380, 330, Region.INDIGENOUS, 36));
        reserved.add(site("arena_ancient", "Pátio Ancestral", 1550, 370, Region.ANCIENT, 36));
        reserved.add(site("arena_forest", "Clareira da Mata", 370, 1510, Region.DEAD_FOREST, 36));
        reserved.add(site("arena_river", "Praça do Porto", 1550, 1510, Region.MODERN, 36));
        arenas = Collections.unmodifiableList(reserved);

        List<Building> structures = new ArrayList<>();
        structures.add(building("central_trunk", "castanheira_trunk", 960, 940, 48, 80));
        structures.add(building("maloca_west", "maloca", 400, 470, 28, 16));
        structures.add(building("maloca_east", "maloca", 480, 470, 24, 15));
        structures.add(building("maloca_north", "maloca", 445, 407, 30, 18));
        structures.add(building("ancient_temple", "stepped_temple", 1480, 405, 40, 32));
        structures.add(building("ancient_east", "ruin", 1530, 480, 18, 14));
        structures.add(building("ancient_west", "ruin", 1442, 453, 20, 12));
        structures.add(building("burned_tree", "burned_tree", 440, 1440, 24, 20));
        structures.add(building("theater", "amazon_theater", 1460, 1425, 40, 24));
        structures.add(building("city_west", "city_house", 1440, 1494, 18, 14));
        structures.add(building("city_east", "city_house", 1540, 1455, 18, 14));
        buildings = Collections.unmodifiableList(structures);

        List<Route> paths = new ArrayList<>();
        // Regional roads start on the south side of the trunk; avoid every building footprint.
        paths.add(route(4, spawn, p(870, 1000), p(750, 800), p(600, 640), p(440, 440)));
        paths.add(route(4, spawn, p(1050, 1000), p(1190, 820), p(1320, 650), p(1480, 440)));
        paths.add(route(4, spawn, p(870, 1050), p(720, 1210), p(590, 1370), p(440, 1480)));
        paths.add(route(4, spawn, p(1050, 1050), p(1200, 1210), p(1350, 1370), p(1480, 1480)));
        paths.add(route(4, p(440, 440), p(650, 290), p(960, 210), p(1260, 290), p(1480, 440),
            p(1650, 650), p(1710, 960), p(1650, 1260), p(1480, 1480), p(1260, 1650),
            p(960, 1710), p(650, 1650), p(440, 1480), p(290, 1260), p(210, 960),
            p(290, 650), p(440, 440)));
        paths.add(route(4, spawn, p(860, 1020), p(800, 940), p(850, 810), p(960, 780),
            p(1070, 810), p(1120, 940), p(1060, 1020), spawn));
        for (int i = 0; i < arenas.size(); i++) {
            Point region = landmarks.get(i + 1).center;
            Point arena = arenas.get(i).center;
            if (i == 1) paths.add(route(4, region, p(1530, 450), arena));
            else if (i == 3) paths.add(route(4, region, p(1480, 1530), arena));
            else paths.add(route(4, region, arena));
        }

        paths.add(route(6, arenas.get(3).center, p(1588, 1510)));
        // Reach the northern and western banks of the Negro instead of leaving forest
        // wedges isolated between the river and the circular boundary.
        paths.add(route(4, p(440, 440), p(380, 430), p(350, 300), p(400, 230),
            p(650, 180), p(850, 180), p(960, 210)));
        paths.add(route(4, p(440, 440), p(440, 490), p(350, 530), p(250, 650),
            p(190, 750), p(100, 900), p(90, 1020), p(160, 1200), p(330, 1390), p(440, 1480)));
        List<Site> secondary = new ArrayList<>();
        for (int sector = 0; sector < 4; sector++) {
            Site destination = landmarks.get(sector + 1);
            // Fixed authored positions offset from the main approaches. No random POI count.
            for (int i = 0; i < 8; i++) {
                double t = .23 + i * .085;
                double cx = spawn.col + (destination.center.col - spawn.col) * t;
                double cy = spawn.row + (destination.center.row - spawn.row) * t;
                double side = i % 2 == 0 ? -44 : 44;
                Point location = p(cx + side, cy - side * .4);
                secondary.add(site("discovery_" + sector + "_" + i,
                    discoveryName(sector, i), location.col, location.row, destination.region, 9));
                // Link each discovery to a main-road waypoint instead of a disconnected clearing.
                Route approach = paths.get(sector);
                Point nearest = nearestOnRoute(approach, location);
                paths.add(route(3, nearest, location));
                double dx = location.col - nearest.col, dy = location.row - nearest.row;
                double length = Math.max(1, Math.hypot(dx, dy));
                String family = sector == 0 ? "maloca" : sector == 1 ? "ruin"
                    : sector == 2 ? "burned_tree" : "city_house";
                structures.add(building("discovery_" + sector + "_" + i + "_structure", family,
                    location.col - dy / length * 6, location.row + dx / length * 6 + 1, 3, 2));
            }
        }
        discoveries = Collections.unmodifiableList(secondary);
        routes = Collections.unmodifiableList(paths);
        // Keep each small authored discovery building clear of every approach, including paths
        // added after it. Positions remain deterministic and within the discovery clearing.
        for (int i = 0; i < secondary.size(); i++) {
            Site site = secondary.get(i);
            Building original = structures.get(11 + i);
            boolean placed = false;
            for (int ring = 6; ring <= 10 && !placed; ring += 2) {
                for (int angle = 0; angle < 16; angle++) {
                    double direction = angle * Math.PI / 8;
                    double x = site.center.col + ring * Math.cos(direction);
                    double y = site.center.row + ring * Math.sin(direction);
                    if (roadDistance(x, y) <= 5) continue;
                    Terrain floor = sampleTile((int) y, (int) x);
                    if (floor.isWater() || floor == Terrain.BORDER) continue;
                    structures.set(11 + i, building(original.id, original.family, x, y + 1, 3, 2));
                    placed = true; break;
                }
            }
            if (!placed) throw new IllegalStateException("No clear location for " + original.id);
        }
        addRegionalBuildings(structures, landmarks.get(4), "city_house", 8);
        addRegionalBuildings(structures, landmarks.get(2), "ruin", 6);
    }

    public Site landmark(String id) { return find(landmarks, id); }
    public Site arena(String id) { return find(arenas, id); }

    public Region regionAt(double x, double y) {
        double col = x / TILE_SIZE, row = y / TILE_SIZE;
        if (!inside(col, row)) return Region.OUTSIDE;
        if (distance(col, row, CENTER, CENTER) < 180) return Region.CENTRAL;
        if (distance(col, row, 440, 1480) < 245) return Region.DEAD_FOREST;
        for (Site site : landmarks) {
            if (distance(col, row, site.center.col, site.center.row) < site.radius + 100) return site.region;
        }
        return Region.FOREST;
    }

    /** Pure sampling: fixed geometry first, seed-dependent forest details last. */
    public Terrain sampleTile(int row, int col) {
        if (row < 0 || col < 0 || row >= ROWS || col >= COLUMNS) return Terrain.BORDER;
        double x = col + .5, y = row + .5;
        if (!inside(x, y)) return Terrain.BORDER;
        for (Point tree : startingTrees)
            if (col == (int) tree.col && row == (int) tree.row) return Terrain.TREE;
        double road = roadDistance(x, y);
        // Fixed discoveries reserve small clearings or river islands before the water pass.
        // Their approaches become bridges automatically outside these dry footprints.
        for (Site site : discoveries) {
            if (distance(x, y, site.center.col, site.center.row) < site.radius)
                return road <= 2 ? Terrain.PATH : Terrain.GROUND;
        }
        Terrain river = riverAt(x, y);
        if (river != null) return road <= 3 ? Terrain.BRIDGE : river;
        if (road <= 2) return Terrain.PATH;
        for (Site arena : arenas) {
            if (distance(x, y, arena.center.col, arena.center.row) < arena.radius)
                return arena.region == Region.MODERN ? Terrain.PAVING : Terrain.GROUND;
        }
        double fromSpawn = distance(x, y, spawn.col, spawn.row);
        if (fromSpawn < SAFE_RADIUS) return Terrain.GRASS;
        for (Site site : landmarks) {
            if (distance(x, y, site.center.col, site.center.row) < site.radius) {
                if (site.region == Region.DEAD_FOREST) return burnedTerrain(row, col);
                if (site.region == Region.MODERN || site.region == Region.ANCIENT) return Terrain.PAVING;
                return Terrain.GROUND;
            }
        }
        double deadDistance = distance(x, y, 440, 1480);
        if (deadDistance < 225 + 12 * Math.sin(x / 24) * Math.cos(y / 31)) return burnedTerrain(row, col);
        for (Point swamp : SWAMPS) {
            if (distance(x, y, swamp.col, swamp.row) < 18) return Terrain.ACID_SWAMP;
        }
        double radial = distance(x, y, CENTER, CENTER);
        if (road > 5 && radial > 160 && hash(col / 64, row / 64, 77) < .15 + .45 * radial / RADIUS
                && distance(x, y, col / 64 * 64 + 32, row / 64 * 64 + 32) < 9) return Terrain.ACID_SWAMP;
        if (road <= 5) return Terrain.GRASS;
        // One trunk per 8x8 area; crowns are separate drawable objects, not solid rectangles.
        int cellCol = col / 8, cellRow = row / 8;
        double roll = hash(cellCol, cellRow, 1);
        int anchorCol = cellCol * 8 + 2 + (int) (hash(cellCol, cellRow, 2) * 4);
        int anchorRow = cellRow * 8 + 2 + (int) (hash(cellCol, cellRow, 3) * 4);
        return roll < .78 && col == anchorCol && row == anchorRow
            ? Terrain.TREE : Terrain.GRASS;
    }

    public boolean isWalkable(double x, double y, double radius) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(radius) || radius < 0) return false;
        double col = x / TILE_SIZE, row = y / TILE_SIZE, r = radius / TILE_SIZE;
        if (distance(col, row, CENTER, CENTER) + r >= RADIUS) return false;
        for (Building building : buildings) {
            double nearestX = Math.max(building.base.col - building.width / 2,
                Math.min(col, building.base.col + building.width / 2));
            double nearestY = Math.max(building.base.row - building.depth, Math.min(row, building.base.row));
            if (building.contains(col, row) || distance(col, row, nearestX, nearestY) < r) return false;
        }
        // Authored paths cross low, worn sections of the roots; solid sections remain obstacles.
        if (col > 875 && col < 1045 && row > 920 && row < 1010 && roadDistance(col, row) > 3) {
            for (Root root : roots) if (root.blocks(col, row, r)) return false;
        }
        for (int tr = (int) Math.floor(row - r); tr <= (int) Math.floor(row + r); tr++) {
            for (int tc = (int) Math.floor(col - r); tc <= (int) Math.floor(col + r); tc++) {
                double nearestX = Math.max(tc, Math.min(col, tc + 1));
                double nearestY = Math.max(tr, Math.min(row, tr + 1));
                if (distance(col, row, nearestX, nearestY) <= r && !sampleTile(tr, tc).walkable) return false;
            }
        }
        return true;
    }

    /** Bounded movement, with axis sliding and substeps to prevent tunneling. */
    public Position resolveMovement(double x, double y, double dx, double dy, double radius) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(radius) || radius < 0) return new Position(x, y);
        double length = Math.hypot(dx, dy);
        if (length > TILE_SIZE * 4) return new Position(x, y); // teleport validates its destination
        int steps = Math.max(1, (int) Math.ceil(length / 16));
        for (int i = 0; i < steps; i++) {
            if (isWalkable(x + dx / steps, y, radius)) x += dx / steps;
            if (isWalkable(x, y + dy / steps, radius)) y += dy / steps;
        }
        return new Position(x, y);
    }

    public int variation(int row, int col, int count) {
        if (count < 1) throw new IllegalArgumentException("count must be positive");
        return Math.min(count - 1, (int) (hash(col, row, 17) * count));
    }

    /** Local decoration query; no global list of trees or scan of the entire world. */
    public Point treeAtCell(int cellRow, int cellCol) {
        if (cellRow < 0 || cellCol < 0 || hash(cellCol, cellRow, 1) >= .78) return null;
        int col = cellCol * 8 + 2 + (int) (hash(cellCol, cellRow, 2) * 4);
        int row = cellRow * 8 + 2 + (int) (hash(cellCol, cellRow, 3) * 4);
        return sampleTile(row, col) == Terrain.TREE ? p(col + .5, row + .5) : null;
    }

    /** Validate load/teleport/drop positions, searching only a bounded neighborhood. */
    public Position safePosition(double x, double y, double radius) {
        if (isWalkable(x, y, radius)) return new Position(x, y);
        if (Double.isFinite(x) && Double.isFinite(y)
                && Math.abs(x / TILE_SIZE - CENTER) < RADIUS + 32
                && Math.abs(y / TILE_SIZE - CENTER) < RADIUS + 32) {
            int col = (int) Math.floor(x / TILE_SIZE), row = (int) Math.floor(y / TILE_SIZE);
            for (int ring = 1; ring <= 24; ring++) {
                for (int step = -ring; step <= ring; step++) {
                    double[][] candidates = {
                        {(col + step + .5) * TILE_SIZE, (row - ring + .5) * TILE_SIZE},
                        {(col + step + .5) * TILE_SIZE, (row + ring + .5) * TILE_SIZE},
                        {(col - ring + .5) * TILE_SIZE, (row + step + .5) * TILE_SIZE},
                        {(col + ring + .5) * TILE_SIZE, (row + step + .5) * TILE_SIZE}
                    };
                    for (double[] candidate : candidates)
                        if (isWalkable(candidate[0], candidate[1], radius))
                            return new Position(candidate[0], candidate[1]);
                }
            }
        }
        return new Position(spawn.worldX(), spawn.worldY());
    }

    public boolean inside(double col, double row) { return distance(col, row, CENTER, CENTER) < RADIUS; }

    private double roadDistance(double x, double y) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Route route : routes) {
            for (int i = 1; i < route.points.size(); i++) {
                double d = segmentDistance(x, y, route.points.get(i - 1), route.points.get(i));
                // The base threshold is 2 tiles. Narrow discovery paths have a 1.5-tile half width.
                nearest = Math.min(nearest, d + (4 - route.width) / 2);
            }
        }
        return nearest;
    }

    private static Terrain riverAt(double x, double y) {
        if (polylineDistance(x, y, MEETING) < 30) {
            Point a = MEETING[0], b = MEETING[1];
            double cross = (b.col - a.col) * (y - a.row) - (b.row - a.row) * (x - a.col);
            return cross > 0 ? Terrain.MEETING_DARK : Terrain.MEETING_SILT;
        }
        if (polylineDistance(x, y, NEGRO) < 19 || polylineDistance(x, y, NEGRO_ARM) < 13)
            return Terrain.NEGRO;
        if (polylineDistance(x, y, SOLIMOES) < 25 || polylineDistance(x, y, PORT_CHANNEL) < 10) return Terrain.SOLIMOES;
        return null;
    }

    private Terrain burnedTerrain(int row, int col) {
        return row % 12 == 5 && col % 12 == 5 && variation(row, col, 4) == 0 ? Terrain.DEAD_TREE : Terrain.BURNED;
    }

    private void addRegionalBuildings(List<Building> structures, Site site, String family, int count) {
        int added = 0;
        for (int dy = -60; dy <= 60 && added < count; dy += 20) for (int dx = -60; dx <= 60 && added < count; dx += 20) {
            double x = site.center.col + dx, y = site.center.row + dy;
            if (Math.hypot(dx, dy) > 72 || roadDistance(x, y) < 11) continue;
            Terrain floor = sampleTile((int) y, (int) x);
            if (floor.isWater() || floor == Terrain.BORDER) continue;
            boolean clear = true;
            for (Site arena : arenas) if (distance(x, y, arena.center.col, arena.center.row) < arena.radius + 12) clear = false;
            for (Building building : structures) {
                if (Math.abs(x - building.base.col) < building.width / 2 + 9
                    && Math.abs(y - building.base.row + building.depth / 2) < building.depth / 2 + 8) clear = false;
            }
            if (!clear) continue;
            structures.add(building(site.id + "_block_" + added, family, x, y + 4, 10, 8));
            added++;
        }
        if (added < count) throw new IllegalStateException("Regional building count not satisfied: " + site.id);
    }

    private double hash(int col, int row, int salt) {
        long value = detailSeed ^ (col * 0x9E3779B97F4A7C15L) ^ (row * 0xC2B2AE3D27D4EB4FL)
            ^ (salt * 0x165667B19E3779F9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return ((value ^ (value >>> 31)) >>> 11) * 0x1.0p-53;
    }

    private static Point nearestOnRoute(Route route, Point point) {
        Point nearest = route.points.get(0);
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < route.points.size(); i++) {
            Point a = route.points.get(i - 1), b = route.points.get(i);
            double dx = b.col - a.col, dy = b.row - a.row;
            double length2 = dx * dx + dy * dy;
            double t = length2 == 0 ? 0 : Math.max(0, Math.min(1,
                ((point.col - a.col) * dx + (point.row - a.row) * dy) / length2));
            Point candidate = p(a.col + t * dx, a.row + t * dy);
            double d = distance(point.col, point.row, candidate.col, candidate.row);
            if (d < best) { best = d; nearest = candidate; }
        }
        return nearest;
    }

    private static double segmentDistance(double x, double y, Point a, Point b) {
        double dx = b.col - a.col, dy = b.row - a.row;
        double length2 = dx * dx + dy * dy;
        double t = length2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - a.col) * dx + (y - a.row) * dy) / length2));
        return distance(x, y, a.col + t * dx, a.row + t * dy);
    }
    private static double polylineDistance(double x, double y, Point[] points) {
        double d = Double.POSITIVE_INFINITY;
        for (int i = 1; i < points.length; i++) d = Math.min(d, segmentDistance(x, y, points[i - 1], points[i]));
        return d;
    }
    private static double distance(double x, double y, double ax, double ay) { return Math.hypot(x - ax, y - ay); }
    private static Point p(double col, double row) { return new Point(col, row); }
    private static Route route(double width, Point... points) { return new Route(width, points); }
    private static Building building(String id, String family, double x, double y, double w, double h) {
        return new Building(id, family, x, y, w, h);
    }
    private static Site site(String id, String name, double x, double y, Region region, double radius) {
        return new Site(id, name, x, y, region, radius);
    }
    private static Site find(List<Site> sites, String id) {
        for (Site site : sites) if (site.id.equals(id)) return site;
        return null;
    }
    private static String discoveryName(int sector, int index) {
        String[][] names = {
            {"Passarela", "Cerâmica Antiga", "Mirante", "Porto da Mata"},
            {"Muro Antigo", "Pátio Coberto", "Ruínas", "Escadaria"},
            {"Clareira Cinzenta", "Tronco Caído", "Última Sombra", "Mata Viva"},
            {"Jardim", "Pequeno Porto", "Praça", "Mirante das Águas"}
        };
        return names[sector][index % 4];
    }
}
