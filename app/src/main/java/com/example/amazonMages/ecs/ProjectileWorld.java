package com.example.amazonMages.ecs;

import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Spell;
import java.util.ArrayList;
import java.util.List;

/** Bridges ECS projectiles to the existing collision interface. */
public final class ProjectileWorld {
    private final List<Entity> renderEntities = new ArrayList<>();
    private final List<Spell> impacts = new ArrayList<>();
    private final World world = new World();
    private final SystemScheduler scheduler = new SystemScheduler();
    private final ProjectileCollisionSystem collisions = new ProjectileCollisionSystem();

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

    public void update() {
        scheduler.update(world, 1);
        for (int i = impacts.size() - 1; i >= 0; i--)
            if (!impacts.get(i).updateImpact()) impacts.remove(i);
    }

    /** Returns live projectiles overlapping a target, in deterministic spawn order. */
    public List<Entity> overlapping(double x, double y, double radius) {
        return collisions.overlapping(world, x, y, radius);
    }

    public void overlappingInto(double x,double y,double radius,List<Entity> hits) {
        collisions.overlappingInto(world,x,y,radius,hits);
    }

    public boolean consume(long entityId) {
        Entity entity = world.find(entityId);
        if (entity == null) return false;
        Spell spell = entity.get(ProjectileComponent.class).spell;
        if (!world.destroy(entityId)) return false;
        if (spell.beginImpact()) impacts.add(spell);
        return true;
    }

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
        world.queryInto(renderEntities,ProjectileComponent.class);
        for (Entity entity : renderEntities) entity.get(ProjectileComponent.class).spell.draw(canvas, display);
        for (Spell impact : impacts) impact.drawImpact(canvas, display);
    }

    public int count() { return world.size(); }
}
