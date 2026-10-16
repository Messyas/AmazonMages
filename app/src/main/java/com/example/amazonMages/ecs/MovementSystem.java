package com.example.amazonMages.ecs;

import java.util.ArrayList;
import java.util.List;

/** Reusable query buffer; position components are the sole movement authority. */
public final class MovementSystem implements GameSystem {
    private final List<Entity> moving = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private static final Class<? extends Component>[] TYPES = new Class[] {
        TransformComponent.class, VelocityComponent.class
    };
    @Override public void update(World world, int ticks) {
        world.queryInto(moving, TYPES);
        for (Entity entity : moving) {
            if (world.find(entity.id()) != entity) continue;
            TransformComponent position = entity.get(TransformComponent.class);
            VelocityComponent velocity = entity.get(VelocityComponent.class);
            position.x += velocity.x * ticks;
            position.y += velocity.y * ticks;
        }
    }
}
