package com.example.amazonMages.ecs;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorldTest {
    @Test public void queryComponentsAndRemoveEntities() {
        World world = new World();
        Entity player = world.create().add(TransformComponent.class, new TransformComponent(10, 20))
            .add(HealthComponent.class, new HealthComponent(5));
        Entity decoration = world.create().add(TransformComponent.class, new TransformComponent(2, 3));
        assertEquals(2, world.with(TransformComponent.class).size());
        assertEquals(1, world.with(TransformComponent.class, HealthComponent.class).size());
        assertEquals(player.id(), world.with(HealthComponent.class).get(0).id());
        player.get(HealthComponent.class).damage(2);
        assertEquals(3, player.get(HealthComponent.class).current(), 0.001);
        player.get(HealthComponent.class).heal(20);
        assertEquals(5, player.get(HealthComponent.class).current(), 0.001);
        assertTrue(world.destroy(decoration.id()));
        assertFalse(world.destroy(decoration.id()));
        assertEquals(1, world.size());
    }
}
