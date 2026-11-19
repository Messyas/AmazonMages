package com.example.amazonMages.map;

/** World-pixel geometry shared by movement, the encounter and the arena artwork. */
public final class BoitataArena {
    public static final String SITE_ID = "arena_forest";
    public static final double WALL_HALF_WIDTH = 19;
    public static final double GATE_HALF_ANGLE = 0.27;
    public final double x, y, radius;

    BoitataArena(WorldDefinition.Site site) {
        x = site.center.worldX();
        y = site.center.worldY();
        radius = site.radius * WorldDefinition.TILE_SIZE;
    }

    public boolean contains(double px, double py) {
        return distance(px, py) < radius;
    }

    public double distance(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    public boolean nearWall(double px, double py, double padding) {
        return Math.abs(distance(px, py) - radius) <= WALL_HALF_WIDTH + padding;
    }

    public boolean blocks(double px, double py, double padding) {
        if (!nearWall(px, py, padding)) return false;
        double dx = px - x, dy = py - y;
        return dy >= 0 || Math.abs(dx) + padding >= -dy * Math.tan(GATE_HALF_ANGLE);
    }
}
