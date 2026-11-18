package com.example.amazonMages.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Magnetic XP & mana crystals dropped by defeated enemies in Vampire Survivors style. */
public final class XpCrystalPool {
    public static final int MAX_GEMS = 180;

    public static final class Gem {
        public double x, y;
        public double speed;
        public int xp;
        public final int tier; // 0 = blue, 1 = emerald, 2 = gold

        Gem(double x, double y, int xp, int tier) {
            this.x = x;
            this.y = y;
            this.xp = xp;
            this.tier = tier;
        }
    }

    public interface Collector {
        void onCollect(int xp, int tier);
    }

    private final List<Gem> gems = new ArrayList<>();

    public void spawn(double x, double y, int xp, int tier) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || xp <= 0) return;
        if (gems.size() >= MAX_GEMS) {
            // Consolidate into oldest non-boss gem so XP is never lost during huge waves.
            Gem oldest = gems.get(0);
            oldest.xp += xp;
            return;
        }
        gems.add(new Gem(x, y, xp, Math.max(0, Math.min(2, tier))));
    }

    public int updateAndCollect(double playerX, double playerY, double playerRadius,
                                double magnetRadius, Collector collector) {
        int collected = 0;
        double pickupDist = playerRadius + 14.0;
        for (int i = gems.size() - 1; i >= 0; i--) {
            Gem gem = gems.get(i);
            double dx = playerX - gem.x;
            double dy = playerY - gem.y;
            double dist = Math.hypot(dx, dy);
            if (dist <= pickupDist) {
                gems.remove(i);
                collected++;
                if (collector != null) collector.onCollect(gem.xp, gem.tier);
                continue;
            }
            if (dist <= magnetRadius || gem.speed > 0) {
                gem.speed = Math.min(14.5, Math.max(3.5, gem.speed + 0.75));
                double step = Math.min(dist, gem.speed);
                gem.x += dx / dist * step;
                gem.y += dy / dist * step;
            }
        }
        return collected;
    }

    public List<Gem> gems() { return Collections.unmodifiableList(gems); }
    public int size() { return gems.size(); }
    public void clear() { gems.clear(); }
}

