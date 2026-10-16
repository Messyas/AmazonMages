package com.example.amazonMages.ecs;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class SystemSchedulerTest {
    @Test public void executesSystemsInRegistrationOrder() {
        World world = new World();
        List<String> order = new ArrayList<>();
        SystemScheduler scheduler = new SystemScheduler();
        scheduler.add((w, ticks) -> order.add("movement:" + ticks));
        scheduler.add((w, ticks) -> order.add("collision:" + ticks));
        scheduler.update(world, 1);
        assertEquals("movement:1", order.get(0));
        assertEquals("collision:1", order.get(1));
    }

    @Test public void supportsRemovalDuringSnapshotQuery() {
        World world = new World();
        world.create().add(TransformComponent.class, new TransformComponent(1, 2));
        world.create().add(TransformComponent.class, new TransformComponent(3, 4));
        for (Entity entity : world.with(TransformComponent.class)) world.destroy(entity.id());
        assertEquals(0, world.size());
    }
}
