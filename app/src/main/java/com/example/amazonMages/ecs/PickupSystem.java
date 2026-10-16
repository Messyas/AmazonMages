package com.example.amazonMages.ecs;

import com.example.amazonMages.items.Inventory;

/** Collects overlapping pickups, retaining any unaccepted quantity. */
public final class PickupSystem {
    public interface Listener { void collected(String itemId,long entityId,int quantity); }
    private Listener listener;
    public void setListener(Listener listener) { this.listener=listener; }
    private final java.util.List<Entity> hits = new java.util.ArrayList<>();
    private boolean blocked;
    public boolean blockedByCapacity() { return blocked; }
    private final ProjectileCollisionSystem collision = new ProjectileCollisionSystem();
    public int collect(World world, double x, double y, double radius, Inventory inventory) {
        int collected = 0; blocked=false;
        collision.overlappingInto(world,x,y,radius,hits);
        for (Entity entity : hits) {
            PickupComponent pickup = entity.get(PickupComponent.class);
            if (pickup == null) continue;
            int accepted = inventory.add(pickup.itemId, pickup.quantity);
            if (accepted < pickup.quantity) blocked=true;
            if (accepted > 0) {
                collected += accepted;
                if (accepted == pickup.quantity) world.destroy(entity.id());
                else entity.add(PickupComponent.class,
                    new PickupComponent(pickup.itemId, pickup.quantity - accepted));
                if (listener!=null) listener.collected(pickup.itemId,entity.id(),accepted);
            }
        }
        return collected;
    }
}
