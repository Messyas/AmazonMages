package com.example.amazonMages.ecs;

/** Position and velocity components are authoritative for projectile motion. */
public final class ProjectileMovementSystem implements GameSystem {
    private final java.util.List<Entity> moving = new java.util.ArrayList<>();
    @SuppressWarnings("unchecked") private static final Class<? extends Component>[] TYPES = new Class[] {
        ProjectileComponent.class,TransformComponent.class,VelocityComponent.class
    };
    private final double minX, minY, maxX, maxY;
    public ProjectileMovementSystem(double minX, double minY, double maxX, double maxY) {
        this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
    }
    @Override public void update(World world, int ticks) {
        world.queryInto(moving,TYPES);
        for (Entity entity : moving) {
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
