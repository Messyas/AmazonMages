package com.example.amazonMages.graphics;

/** Eight compass directions in screen coordinates (positive Y points down). */
public enum Direction {
    SOUTH, SOUTH_WEST, WEST, NORTH_WEST, NORTH, NORTH_EAST, EAST, SOUTH_EAST;

    public static Direction fromVector(double x, double y, Direction fallback) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || (x == 0 && y == 0)) return fallback;
        double angle = Math.atan2(x, y);
        int sector = ((int) Math.round(angle / (Math.PI / 4)) + 8) % 8;
        return values()[sector];
    }
}
