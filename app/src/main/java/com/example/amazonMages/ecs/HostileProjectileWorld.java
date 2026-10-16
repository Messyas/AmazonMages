package com.example.amazonMages.ecs;

import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.ai.EnemyProjectile;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.gameobject.Player;
import java.util.ArrayList;
import java.util.List;

/** ECS owns hostile movement, collision and expiry; the legacy shot only renders. */
public final class HostileProjectileWorld {
    private static final class Shot implements Component {
        final EnemyProjectile view;
        Shot(EnemyProjectile view) { this.view = view; }
    }
    private final World world = new World();
    private final MovementSystem movement = new MovementSystem();
    private final List<Entity> shots = new ArrayList<>();
    public int count() { return world.size(); }
    public void clear() { world.clear(); }
    public void spawn(EnemyProjectile shot) {
        world.create().add(TransformComponent.class, new TransformComponent(shot.getPositionX(), shot.getPositionY()))
            .add(VelocityComponent.class, new VelocityComponent(shot.getVelocityX(), shot.getVelocityY()))
            .add(ColliderComponent.class, new ColliderComponent(shot.getRadius()))
            .add(LifetimeComponent.class, new LifetimeComponent(180))
            .add(Shot.class, new Shot(shot));
    }
    public void update(Player player) {
        movement.update(world, 1);
        world.queryInto(shots, Shot.class);
        for (Entity entity : shots) {
            if (--entity.get(LifetimeComponent.class).remainingTicks <= 0) {
                world.destroy(entity.id());
                continue;
            }
            TransformComponent position = entity.get(TransformComponent.class);
            EnemyProjectile shot = entity.get(Shot.class).view;
            shot.syncPosition(position.x, position.y);
            double radius = player.getRadius() + entity.get(ColliderComponent.class).radius;
            if (Math.hypot(position.x - player.getPositionX(), position.y - player.getPositionY()) < radius
                    && world.destroy(entity.id())) {
                player.receiveHit(new DamageEvent(shot.getDamage(), shot.getDamage() * 12.0, DamageType.ACID));
            }
        }
    }
    public void draw(Canvas canvas, GameDisplay display) {
        world.queryInto(shots, Shot.class);
        for (Entity entity : shots) entity.get(Shot.class).view.draw(canvas, display);
    }
}
