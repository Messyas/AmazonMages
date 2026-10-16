package com.example.amazonMages.ecs;

import java.util.ArrayList;
import java.util.List;

/** Pure-data circle collision queries; gameplay effects remain in the caller. */
public final class ProjectileCollisionSystem {
    private final List<Entity> candidates = new ArrayList<>();
    public List<Entity> overlapping(World world, double x, double y, double radius) {
        List<Entity> hits = new ArrayList<>();
        overlappingInto(world,x,y,radius,hits);
        return hits;
    }
    public void overlappingInto(World world,double x,double y,double radius,List<Entity> hits) {
        if (!Double.isFinite(x) || !Double.isFinite(y) ||
            !Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("circle");
        hits.clear();
        world.queryInto(candidates,TransformComponent.class,ColliderComponent.class);
        for (Entity entity : candidates) {
            TransformComponent position = entity.get(TransformComponent.class);
            double combined = radius + entity.get(ColliderComponent.class).radius;
            double dx = position.x - x, dy = position.y - y;
            if (Math.hypot(dx, dy) < combined) hits.add(entity);
        }
    }
}
