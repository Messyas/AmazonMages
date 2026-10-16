package com.example.amazonMages.ecs;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class MovementSystemTest {
    @Test public void authoritativePositionMovesAndQueryBufferDoesNotRetainDestroyedEntities() {
        World world = new World();
        Entity entity = world.create().add(TransformComponent.class, new TransformComponent(2, 3))
            .add(VelocityComponent.class, new VelocityComponent(4, -1));
        MovementSystem movement = new MovementSystem();
        movement.update(world, 3);
        assertEquals(14, entity.get(TransformComponent.class).x, 0);
        assertEquals(0, entity.get(TransformComponent.class).y, 0);
        List<Entity> query = new ArrayList<>();
        world.queryInto(query, TransformComponent.class);
        assertEquals(1, query.size());
        assertTrue(world.destroy(entity.id()));
        assertFalse(world.destroy(entity.id()));
        world.queryInto(query, TransformComponent.class);
        assertTrue(query.isEmpty());
        world.create();
        world.clear();
        assertEquals(0, world.size());
    }
}
