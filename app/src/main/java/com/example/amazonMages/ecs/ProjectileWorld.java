package com.example.amazonMages.ecs;

import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Spell;
import java.util.ArrayList;
import java.util.List;

/** Bridges ECS projectiles to the existing collision interface. */
public final class ProjectileWorld {
    private final World world = new World();
    private final SystemScheduler scheduler = new SystemScheduler();

    public ProjectileWorld(double minX, double minY, double maxX, double maxY) {
        scheduler.add(new ProjectileMovementSystem(minX, minY, maxX, maxY));
    }

    public void spawn(Spell spell) {
        world.create()
            .add(ProjectileComponent.class, new ProjectileComponent(spell))
            .add(TransformComponent.class,
                new TransformComponent(spell.getPositionX(), spell.getPositionY()))
            .add(VelocityComponent.class,
                new VelocityComponent(spell.getVelocityX(), spell.getVelocityY()))
            .add(ColliderComponent.class, new ColliderComponent(spell.getRadius()));
    }

    public void update() { scheduler.update(world, 1); }

    public List<Spell> collisionSnapshot() {
        List<Spell> result = new ArrayList<>();
        for (Entity entity : world.with(ProjectileComponent.class))
            result.add(entity.get(ProjectileComponent.class).spell);
        return result;
    }

    public void reconcileCollisions(List<Spell> remaining) {
        for (Entity entity : world.with(ProjectileComponent.class)) {
            if (!remaining.contains(entity.get(ProjectileComponent.class).spell))
                world.destroy(entity.id());
        }
    }

    public void draw(Canvas canvas, GameDisplay display) {
        for (Entity entity : world.with(ProjectileComponent.class))
            entity.get(ProjectileComponent.class).spell.draw(canvas, display);
    }

    public int count() { return world.size(); }
}
