package com.example.amazonMages.ecs;

import java.util.ArrayList;
import java.util.List;

/** Pure-data circle collision queries; gameplay effects remain in the caller. */
public final class ProjectileCollisionSystem {
    public List<Entity> overlapping(World world, double x, double y, double radius) {
        if (!Double.isFinite(x) || !Double.isFinite(y) ||
            !Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("circle");
        List<Entity> hits = new ArrayList<>();
        for (Entity entity : world.with(TransformComponent.class, ColliderComponent.class)) {
            TransformComponent position = entity.get(TransformComponent.class);
            double combined = radius + entity.get(ColliderComponent.class).radius;
            double dx = position.x - x, dy = position.y - y;
            if (dx * dx + dy * dy <= combined * combined) hits.add(entity);
        }
        return hits;
    }
}
