package com.example.amazonMages.items;

/** Pure gameplay rules for consuming inventory items. */
public final class ConsumableService {
    public static final String HEALING_HERB = "erva";
    private static final int HERB_HEAL = 2;

    public int useHealingHerb(Inventory inventory, int health, int maximumHealth) {
        if (inventory == null || maximumHealth < 1 || health < 0)
            throw new IllegalArgumentException("health/inventory");
        if (health >= maximumHealth || inventory.count(HEALING_HERB) == 0)
            return health;
        if (!inventory.remove(HEALING_HERB, 1)) return health;
        return (int) Math.min(maximumHealth, (long) health + HERB_HEAL);
    }
}
