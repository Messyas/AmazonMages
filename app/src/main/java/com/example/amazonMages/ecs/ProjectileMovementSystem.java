package com.example.amazonMages.ecs;

/** Position and velocity components are authoritative for projectile motion. */
public final class ProjectileMovementSystem implements GameSystem {
    private final double minX, minY, maxX, maxY;
    public ProjectileMovementSystem(double minX, double minY, double maxX, double maxY) {
        this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
    }
    @Override public void update(World world, int ticks) {
        for (Entity entity : world.with(ProjectileComponent.class, TransformComponent.class, VelocityComponent.class)) {
            TransformComponent transform = entity.get(TransformComponent.class);
            VelocityComponent velocity = entity.get(VelocityComponent.class);
            transform.x += velocity.x * ticks;
            transform.y += velocity.y * ticks;
            if (transform.x < minX || transform.y < minY ||
                transform.x > maxX || transform.y > maxY) {
                world.destroy(entity.id());
                continue;
            }
            entity.get(ProjectileComponent.class).spell.syncPosition(transform.x, transform.y);
        }
    }
}
