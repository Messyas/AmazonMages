package com.example.amazonMages.ecs;
import java.util.ArrayList;
import java.util.List;
public final class LifetimeSystem implements GameSystem {
    private final List<Entity> expiring=new ArrayList<>();
    @Override public void update(World world,int ticks) {
        world.queryInto(expiring,LifetimeComponent.class);
        for(Entity entity:expiring) {
            LifetimeComponent lifetime=entity.get(LifetimeComponent.class);
            lifetime.remainingTicks-=ticks;
            if(lifetime.remainingTicks<=0)world.destroy(entity.id());
        }
    }
}
