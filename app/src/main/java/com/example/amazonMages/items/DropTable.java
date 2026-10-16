package com.example.amazonMages.items;
import java.util.Random;

/** Immutable, configurable independent rolls. Only defeat code is allowed to invoke it. */
public final class DropTable {
    public interface Sink { void drop(String item, int quantity); }
    public static final class Entry {
        public final String item;
        public final double chance;
        public final int minimum, maximum;
        public Entry(String item, double chance, int minimum, int maximum) {
            if (item == null || item.isEmpty() || !Double.isFinite(chance) || chance < 0 || chance > 1 || minimum < 1 || maximum < minimum || maximum > 9999)
                throw new IllegalArgumentException("drop");
            this.item=item; this.chance=chance; this.minimum=minimum; this.maximum=maximum;
        }
    }
    private final Entry[] entries;
    public DropTable(Entry... entries) { this.entries=entries.clone(); }
    public void roll(Random random, Sink sink) {
        for (Entry e:entries) if (random.nextDouble() < e.chance)
            sink.drop(e.item, e.minimum+random.nextInt(e.maximum-e.minimum+1));
    }
    public static DropTable forEnemy(String id) {
        if ("swamp_sentinel".equals(id) || "ancient_wraith".equals(id)) return BOSS;
        return GHOST;
    }
    private static final DropTable GHOST = new DropTable(new Entry("erva",.4,1,2),
        new Entry("mana",.25,1,1),new Entry("cajado",.05,1,1));
    private static final DropTable BOSS = new DropTable(new Entry("erva",1,3,6),
        new Entry("mana",1,2,4),new Entry("cajado",1,1,1));
}
