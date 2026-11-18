package com.example.amazonMages.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compact, connected GBA-scale geography for Amazon Mages.
 * Coordinates in descriptors are tiles (64 px); movement and spawn APIs use world pixels.
 * Geography stays fixed when the detail seed changes. No world-sized allocation is needed.
 */
public final class WorldDefinition {
    public static final int COLUMNS = 192, ROWS = 192, TILE_SIZE = 64;
    public static final int CENTER = 96, RADIUS = 88, REVISION = 2;
    public static final long DEFAULT_SEED = 20261027L;
    public static final double SAFE_RADIUS = 7.5;

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
            double length2 = dx * dx + dy * dy;
            double t = length2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - start.col) * dx + (y - start.row) * dy) / length2));
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
        p(90, 4), p(80, 26), p(72, 52), p(68, 78), p(68, 108), p(76, 138), p(86, 158)
    };
    private static final Point[] NEGRO_ARM = {
        p(68, 78), p(38, 86), p(12, 100)
    };
    private static final Point[] SOLIMOES = {
        p(186, 72), p(156, 86), p(128, 104), p(108, 136), p(86, 158)
    };
    private static final Point[] MEETING = { p(86, 158), p(78, 186) };
    private static final Point[] SWAMPS = {
        p(58, 124), p(64, 150), p(40, 124), p(118, 82)
    };

    public final long detailSeed;
    public final Point spawn = p(96, 100);
    public final List<Site> landmarks, arenas, discoveries;
    public final List<Building> buildings;
    public final List<Route> routes;
    public final List<Root> roots;
    public final List<Point> startingTrees;

    public WorldDefinition() { this(DEFAULT_SEED); }

    public WorldDefinition(long seed) {
        detailSeed = seed;
        List<Point> entryTrees = new ArrayList<>();
        entryTrees.add(p(87.5, 93.5)); entryTrees.add(p(104.5, 93.5));
        entryTrees.add(p(92.5, 108.5)); entryTrees.add(p(99.5, 108.5));
        startingTrees = Collections.unmodifiableList(entryTrees);

        List<Root> rootParts = new ArrayList<>();
        rootParts.add(new Root(94.4, 91.6, 91.4, 93.8, 0.65, 0.25));
        rootParts.add(new Root(97.6, 91.6, 100.6, 93.8, 0.65, 0.25));
        rootParts.add(new Root(94.0, 90.0, 90.8, 90.6, 0.65, 0.25));
        rootParts.add(new Root(98.0, 90.0, 101.2, 90.6, 0.65, 0.25));
        roots = Collections.unmodifiableList(rootParts);

        List<Site> major = new ArrayList<>();
        major.add(site("castanheira", "Castanheira Ancestral", 96, 90, Region.CENTRAL, 12));
        major.add(site("malocas", "Malocas da Floresta", 58, 58, Region.INDIGENOUS, 10.5));
        major.add(site("cidade_antiga", "Cidade Antiga", 134, 58, Region.ANCIENT, 10.5));
        major.add(site("mata_morta", "Mata Morta", 58, 134, Region.DEAD_FOREST, 13.5));
        major.add(site("cidade_moderna", "Cidade das Águas", 134, 134, Region.MODERN, 11.5));
        landmarks = Collections.unmodifiableList(major);

        List<Site> reserved = new ArrayList<>();
        reserved.add(site("arena_storm", "Clareira das Tempestades", 42, 44, Region.INDIGENOUS, 7.5));
        reserved.add(site("arena_ancient", "Pátio Ancestral", 150, 44, Region.ANCIENT, 7.5));
        reserved.add(site("arena_forest", "Clareira da Mata", 42, 148, Region.DEAD_FOREST, 7.5));
        reserved.add(site("arena_river", "Praça do Porto", 150, 148, Region.MODERN, 7.5));
        arenas = Collections.unmodifiableList(reserved);

        List<Building> structures = new ArrayList<>();
        structures.add(building("central_trunk", "castanheira_trunk", 96, 92, 3.6, 2.5));
        structures.add(building("maloca_north", "maloca", 58, 51, 3.2, 2.0));
        structures.add(building("maloca_west", "maloca", 50, 57, 2.8, 1.8));
        structures.add(building("maloca_east", "maloca", 65, 56, 2.6, 1.8));
        structures.add(building("ancient_temple", "stepped_temple", 134, 50, 3.8, 2.6));
        structures.add(building("ancient_west", "ruin", 125, 56, 2.2, 1.5));
        structures.add(building("ancient_east", "ruin", 143, 56, 2.2, 1.5));
        structures.add(building("burned_tree", "burned_tree", 58, 128, 2.2, 2.0));
        structures.add(building("theater", "amazon_theater", 134, 127, 4.2, 2.6));
        structures.add(building("city_west", "city_house", 124, 133, 2.2, 1.6));
        structures.add(building("city_east", "city_house", 144, 133, 2.2, 1.6));
        buildings = Collections.unmodifiableList(structures);

        List<Route> paths = new ArrayList<>();
        // 0..3: Regional radial roads connecting spawn to the 4 landmarks (crossing rivers with bridges)
        paths.add(route(3.2, spawn, p(84, 98), p(70, 76), p(62, 64), p(58, 58)));
        paths.add(route(3.2, spawn, p(108, 98), p(120, 76), p(128, 64), p(134, 58)));
        paths.add(route(3.2, spawn, p(84, 104), p(72, 122), p(64, 130), p(58, 134)));
        paths.add(route(3.2, spawn, p(108, 104), p(120, 122), p(128, 130), p(134, 134)));
        // Outer perimeter ring connecting all 4 landmarks + Negro/Solimoes/Meeting bridges
        paths.add(route(3.2,
            p(58, 58), p(78, 36), p(96, 30), p(116, 36), p(134, 58),
            p(154, 86), p(152, 114), p(134, 134), p(112, 160), p(84, 164),
            p(58, 134), p(36, 112), p(32, 86), p(38, 66), p(58, 58)));
        // Central clearing loop around the Castanheira
        paths.add(route(3.0,
            spawn, p(84, 98), p(82, 82), p(96, 78), p(110, 82), p(108, 98), spawn));
        // Routes linking each landmark to its combat arena
        for (int i = 0; i < arenas.size(); i++) {
            Point region = landmarks.get(i + 1).center;
            Point arena = arenas.get(i).center;
            if (i == 0) paths.add(route(3.0, region, p(48, 52), arena));
            else if (i == 1) paths.add(route(3.0, region, p(144, 52), arena));
            else if (i == 2) paths.add(route(3.0, region, p(48, 142), arena));
            else paths.add(route(3.0, region, p(144, 142), arena));
        }

        // 12 authored discoveries (3 per sector) connected to main roads
        List<Site> secondary = new ArrayList<>();
        for (int sector = 0; sector < 4; sector++) {
            Site destination = landmarks.get(sector + 1);
            for (int i = 0; i < 3; i++) {
                double t = 0.44 + i * 0.17;
                double cx = spawn.col + (destination.center.col - spawn.col) * t;
                double cy = spawn.row + (destination.center.row - spawn.row) * t;
                double side = (i % 2 == 0 ? -7.5 : 7.5);
                Point location = p(cx + side, cy - side * 0.35);
                secondary.add(site("discovery_" + sector + "_" + i,
                    discoveryName(sector, i), location.col, location.row, destination.region, 2.8));
                Route approach = paths.get(sector);
                Point nearest = nearestOnRoute(approach, location);
                paths.add(route(2.6, nearest, location));
                String family = sector == 0 ? "maloca" : sector == 1 ? "ruin"
                    : sector == 2 ? "burned_tree" : "city_house";
                structures.add(building("discovery_" + sector + "_" + i + "_structure", family,
                    location.col, location.row - 2.0, 1.8, 1.4));
            }
        }
        discoveries = Collections.unmodifiableList(secondary);
        routes = Collections.unmodifiableList(paths);

        // Relocate each discovery structure onto a clear spot beside its clearing, off all roads & rivers
        for (int i = 0; i < secondary.size(); i++) {
            Site site = secondary.get(i);
            Building original = structures.get(11 + i);
            boolean placed = false;
            for (int ring = 3; ring <= 7 && !placed; ring++) {
                for (int angle = 0; angle < 16; angle++) {
                    double direction = angle * Math.PI / 8;
                    double x = site.center.col + ring * Math.cos(direction);
                    double y = site.center.row + ring * Math.sin(direction);
                    if (roadDistance(x, y) <= 3.0) continue;
                    Terrain floor = sampleTile((int) y, (int) x);
                    if (floor.isWater() || floor == Terrain.BORDER) continue;
                    boolean safe = true;
                    for (Site arena : arenas) if (distance(x, y, arena.center.col, arena.center.row) < arena.radius + 3.0) safe = false;
                    for (int b = 0; b < structures.size(); b++) {
                        if (b == 11 + i) continue;
                        Building other = structures.get(b);
                        if (Math.abs(x - other.base.col) < other.width / 2 + 2.5
                            && Math.abs(y - other.base.row + other.depth / 2) < other.depth / 2 + 2.5) safe = false;
                    }
                    if (!safe) continue;
                    structures.set(11 + i, building(original.id, original.family, x, y + 0.8, 1.8, 1.4));
                    placed = true;
                    break;
                }
            }
            if (!placed) throw new IllegalStateException("No clear location for " + original.id);
        }

        addRegionalBuildings(structures, landmarks.get(4), "city_house", 5);
        addRegionalBuildings(structures, landmarks.get(2), "ruin", 4);
    }

    public Site landmark(String id) { return find(landmarks, id); }
    public Site arena(String id) { return find(arenas, id); }

    public Region regionAt(double x, double y) {
        double col = x / TILE_SIZE, row = y / TILE_SIZE;
        if (!inside(col, row)) return Region.OUTSIDE;
        if (distance(col, row, CENTER, CENTER) < 22) return Region.CENTRAL;
        if (distance(col, row, 58, 134) < 24) return Region.DEAD_FOREST;
        for (Site site : landmarks) {
            if (distance(col, row, site.center.col, site.center.row) < site.radius + 14) return site.region;
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
        for (Site site : discoveries) {
            if (distance(x, y, site.center.col, site.center.row) < site.radius)
                return road <= 1.6 ? Terrain.PATH : Terrain.GROUND;
        }
        Terrain river = riverAt(x, y);
        if (river != null) return road <= 2.2 ? Terrain.BRIDGE : river;
        if (road <= 1.6) return Terrain.PATH;
        for (Site arena : arenas) {
            if (distance(x, y, arena.center.col, arena.center.row) < arena.radius + 1.0)
                return arena.region == Region.MODERN || arena.region == Region.ANCIENT
                    ? Terrain.PAVING : Terrain.GROUND;
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
        double deadDistance = distance(x, y, 58, 134);
        if (deadDistance < 19 + 2.0 * Math.sin(x / 5.0) * Math.cos(y / 6.0)) return burnedTerrain(row, col);
        for (Point swamp : SWAMPS) {
            if (distance(x, y, swamp.col, swamp.row) < 4.5) return Terrain.ACID_SWAMP;
        }
        if (road <= 3.0) return Terrain.GRASS;
        // Dense GBA forest clusters: one tree per 4x4 cell outside clearings & road shoulders.
        int cellCol = col / 4, cellRow = row / 4;
        double radial = distance(x, y, CENTER, CENTER);
        double density = radial > 56 ? 0.78 : 0.62;
        double roll = hash(cellCol, cellRow, 1);
        int anchorCol = cellCol * 4 + 1 + (int) (hash(cellCol, cellRow, 2) * 2);
        int anchorRow = cellRow * 4 + 1 + (int) (hash(cellCol, cellRow, 3) * 2);
        return roll < density && col == anchorCol && row == anchorRow
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
        if (col > 86 && col < 106 && row > 86 && row < 98 && roadDistance(col, row) > 2.0) {
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
        if (length > TILE_SIZE * 4) return new Position(x, y);
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
        if (cellRow < 0 || cellCol < 0) return null;
        int col = cellCol * 4 + 1 + (int) (hash(cellCol, cellRow, 2) * 2);
        int row = cellRow * 4 + 1 + (int) (hash(cellCol, cellRow, 3) * 2);
        return sampleTile(row, col) == Terrain.TREE ? p(col + .5, row + .5) : null;
    }

    /** Validate load/teleport/drop positions, searching only a bounded neighborhood. */
    public Position safePosition(double x, double y, double radius) {
        if (isWalkable(x, y, radius)) return new Position(x, y);
        if (Double.isFinite(x) && Double.isFinite(y)
                && Math.abs(x / TILE_SIZE - CENTER) < RADIUS + 12
                && Math.abs(y / TILE_SIZE - CENTER) < RADIUS + 12) {
            int col = (int) Math.floor(x / TILE_SIZE), row = (int) Math.floor(y / TILE_SIZE);
            for (int ring = 1; ring <= 16; ring++) {
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

    public double roadDistance(double x, double y) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Route route : routes) {
            for (int i = 1; i < route.points.size(); i++) {
                double d = segmentDistance(x, y, route.points.get(i - 1), route.points.get(i));
                nearest = Math.min(nearest, d + (3.2 - route.width) / 2);
            }
        }
        return nearest;
    }

    private static Terrain riverAt(double x, double y) {
        if (polylineDistance(x, y, MEETING) < 3.2) {
            Point a = MEETING[0], b = MEETING[1];
            double cross = (b.col - a.col) * (y - a.row) - (b.row - a.row) * (x - a.col);
            return cross > 0 ? Terrain.MEETING_DARK : Terrain.MEETING_SILT;
        }
        if (polylineDistance(x, y, NEGRO) < 2.3 || polylineDistance(x, y, NEGRO_ARM) < 1.8)
            return Terrain.NEGRO;
        if (polylineDistance(x, y, SOLIMOES) < 2.5) return Terrain.SOLIMOES;
        return null;
    }

    private Terrain burnedTerrain(int row, int col) {
        if (row % 5 == 2 && col % 5 == 2 && variation(row, col, 3) == 0 && roadDistance(col + .5, row + .5) > 2.8) {
            return Terrain.DEAD_TREE;
        }
        return Terrain.BURNED;
    }

    private void addRegionalBuildings(List<Building> structures, Site site, String family, int count) {
        int added = 0;
        for (int dy = -14; dy <= 14 && added < count; dy += 5) {
            for (int dx = -14; dx <= 14 && added < count; dx += 5) {
                double x = site.center.col + dx, y = site.center.row + dy;
                if (Math.hypot(dx, dy) > 15 || roadDistance(x, y) < 3.4) continue;
                Terrain floor = sampleTile((int) y, (int) x);
                if (floor.isWater() || floor == Terrain.BORDER) continue;
                boolean clear = true;
                for (Site arena : arenas) {
                    if (distance(x, y, arena.center.col, arena.center.row) < arena.radius + 3.0) clear = false;
                }
                for (Building building : structures) {
                    if (Math.abs(x - building.base.col) < building.width / 2 + 2.6
                        && Math.abs(y - building.base.row + building.depth / 2) < building.depth / 2 + 2.4) clear = false;
                }
                if (!clear) continue;
                structures.add(building(site.id + "_block_" + added, family, x, y + 0.8, 2.2, 1.5));
                added++;
            }
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
            {"Passarela", "Cerâmica Antiga", "Mirante"},
            {"Muro Antigo", "Pátio Coberto", "Escadaria"},
            {"Clareira Cinzenta", "Tronco Caído", "Última Sombra"},
            {"Jardim", "Pequeno Porto", "Mirante das Águas"}
        };
        return names[sector][index % 3];
    }
}
