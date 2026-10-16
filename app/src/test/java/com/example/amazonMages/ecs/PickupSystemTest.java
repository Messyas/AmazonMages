package com.example.amazonMages.ecs;
import com.example.amazonMages.items.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PickupSystemTest {
    @Test public void collectsNearbyAndRetainsOverflow() {
        Inventory bag = new Inventory(new ItemRegistry().register(new ItemDefinition("herb", "Herb", 5)));
        bag.add("herb", 4);
        World world = new World();
        Entity pickup = world.create()
            .add(TransformComponent.class, new TransformComponent(10, 10))
            .add(ColliderComponent.class, new ColliderComponent(4))
            .add(PickupComponent.class, new PickupComponent("herb", 3));
        PickupSystem system = new PickupSystem();
        assertEquals(0, system.collect(world, 100, 100, 2, bag));
        assertEquals(1, system.collect(world, 10, 10, 2, bag));
        assertEquals(2, pickup.get(PickupComponent.class).quantity);
        assertEquals(5, bag.count("herb"));
    }
}
