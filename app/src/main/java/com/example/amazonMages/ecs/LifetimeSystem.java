package com.example.amazonMages.ecs;
import java.util.ArrayList;
import java.util.List;
public final class LifetimeSystem implements GameSystem {
    private final List<Entity> expiring=new ArrayList<>();
    @Override public void update(World world,int ticks) {
        world.queryInto(expiring,LifetimeComponent.class);
        for(Entity entity:expiring) {
            LifetimeComponent lifetime=entity.get(LifetimeComponent.class);
            lifetime.revealTicks = Math.max(0, lifetime.revealTicks - ticks);
            if (lifetime.isPermanent()) continue;
            lifetime.remainingTicks-=ticks;
            if(lifetime.remainingTicks<=0)world.destroy(entity.id());
        }
    }
}
