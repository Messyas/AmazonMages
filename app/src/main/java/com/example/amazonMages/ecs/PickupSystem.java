package com.example.amazonMages.ecs;

import com.example.amazonMages.items.Inventory;

/** Collects overlapping pickups, retaining any unaccepted quantity. */
public final class PickupSystem {
    private final ProjectileCollisionSystem collision = new ProjectileCollisionSystem();
    public int collect(World world, double x, double y, double radius, Inventory inventory) {
        int collected = 0;
        for (Entity entity : collision.overlapping(world, x, y, radius)) {
            PickupComponent pickup = entity.get(PickupComponent.class);
            if (pickup == null) continue;
            int accepted = inventory.add(pickup.itemId, pickup.quantity);
            if (accepted > 0) {
                collected += accepted;
                if (accepted == pickup.quantity) world.destroy(entity.id());
                else entity.add(PickupComponent.class,
                    new PickupComponent(pickup.itemId, pickup.quantity - accepted));
            }
        }
        return collected;
    }
}
