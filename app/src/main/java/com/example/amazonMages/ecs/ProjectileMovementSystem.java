package com.example.amazonMages.ecs;

/** ECS owns the lifetime of player projectiles; legacy Spell still handles motion. */
public final class ProjectileMovementSystem implements GameSystem {
    private final double minX, minY, maxX, maxY;
    public ProjectileMovementSystem(double minX, double minY, double maxX, double maxY) {
        this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
    }
    @Override public void update(World world, int ticks) {
        for (Entity entity : world.with(ProjectileComponent.class, TransformComponent.class)) {
            ProjectileComponent projectile = entity.get(ProjectileComponent.class);
            for (int i = 0; i < ticks; i++) projectile.spell.update();
            TransformComponent transform = entity.get(TransformComponent.class);
            transform.x = projectile.spell.getPositionX();
            transform.y = projectile.spell.getPositionY();
            if (transform.x < minX || transform.y < minY ||
                transform.x > maxX || transform.y > maxY) world.destroy(entity.id());
        }
    }
}
