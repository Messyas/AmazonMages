package com.example.amazonMages.items;

import java.util.Random;

/** Immutable, configurable independent drops differentiated by Amazonian enemy archetype. */
public final class DropTable {
    public interface Sink { void drop(String item, int quantity); }

    public static final class Entry {
        public final String item;
        public final double chance;
        public final int minimum, maximum;

        public Entry(String item, double chance, int minimum, int maximum) {
            if (item == null || item.isEmpty() || !Double.isFinite(chance) || chance < 0 || chance > 1
                    || minimum < 1 || maximum < minimum || maximum > 9999) {
                throw new IllegalArgumentException("drop");
            }
            this.item = item;
            this.chance = chance;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    private final Entry[] entries;

    public DropTable(Entry... entries) { this.entries = entries.clone(); }

    public void roll(Random random, Sink sink) {
        for (Entry e : entries) {
            if (random.nextDouble() < e.chance) {
                sink.drop(e.item, e.minimum + random.nextInt(e.maximum - e.minimum + 1));
            }
        }
    }

    public static DropTable forEnemy(String id) {
        if ("swamp_sentinel".equals(id) || "ancient_wraith".equals(id)) return BOSS;
        if ("swarm_bat".equals(id)) return SWARM;
        if ("curupira_imp".equals(id) || "moss_golem".equals(id)) return FOREST;
        return ARCANE;
    }

    private static final DropTable SWARM = new DropTable(
        new Entry("coco", 0.28, 1, 2),
        new Entry("erva", 0.20, 1, 1),
        new Entry("mana", 0.18, 1, 1)
    );
    private static final DropTable FOREST = new DropTable(
        new Entry("coco", 0.42, 1, 3),
        new Entry("erva", 0.35, 1, 2),
        new Entry("bencao", 0.12, 1, 1)
    );
    private static final DropTable ARCANE = new DropTable(
        new Entry("erva", 0.35, 1, 2),
        new Entry("mana", 0.35, 1, 2),
        new Entry("coco", 0.25, 1, 2),
        new Entry("bencao", 0.10, 1, 1),
        new Entry("cajado", 0.05, 1, 1)
    );
    private static final DropTable BOSS = new DropTable(
        new Entry("coco", 1.0, 4, 6),
        new Entry("erva", 1.0, 3, 5),
        new Entry("mana", 1.0, 2, 4),
        new Entry("bencao", 1.0, 1, 2),
        new Entry("cajado", 1.0, 1, 1)
    );
}
