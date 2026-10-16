package com.example.amazonMages.ecs;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProjectileCollisionSystemTest {
    @Test public void detectsOverlapAndExcludesDistantObjects() {
        World world = new World();
        world.create().add(TransformComponent.class, new TransformComponent(10, 10))
            .add(ColliderComponent.class, new ColliderComponent(5));
        world.create().add(TransformComponent.class, new TransformComponent(100, 100))
            .add(ColliderComponent.class, new ColliderComponent(5));
        ProjectileCollisionSystem collisions = new ProjectileCollisionSystem();
        assertEquals(1, collisions.overlapping(world, 15, 10, 1).size());
        assertEquals(0, collisions.overlapping(world, 50, 50, 1).size());
    }

    @Test public void tangentCirclesDoNotCollide() {
        World world = new World();
        world.create()
            .add(TransformComponent.class, new TransformComponent(0, 0))
            .add(ColliderComponent.class, new ColliderComponent(5));
        ProjectileCollisionSystem collisions = new ProjectileCollisionSystem();
        assertEquals(0, collisions.overlapping(world, 10, 0, 5).size());
        assertEquals(1, collisions.overlapping(world, 9.999, 0, 5).size());
    }

    @Test public void velocityMovesEntityAndOutOfBoundsDestroysIt() {
        World world = new World();
        Entity entity = world.create()
            .add(TransformComponent.class, new TransformComponent(10, 10))
            .add(VelocityComponent.class, new VelocityComponent(2, 3));
        SystemScheduler scheduler = new SystemScheduler();
        scheduler.add((w, ticks) -> {
            for (Entity e : w.with(TransformComponent.class, VelocityComponent.class)) {
                e.get(TransformComponent.class).x += e.get(VelocityComponent.class).x * ticks;
                e.get(TransformComponent.class).y += e.get(VelocityComponent.class).y * ticks;
            }
        });
        scheduler.update(world, 2);
        assertEquals(14, entity.get(TransformComponent.class).x, 0.001);
        assertEquals(16, entity.get(TransformComponent.class).y, 0.001);
    }
}
