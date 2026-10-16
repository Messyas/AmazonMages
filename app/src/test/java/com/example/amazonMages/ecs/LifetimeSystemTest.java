package com.example.amazonMages.ecs;
import org.junit.Test;
import static org.junit.Assert.*;
public class LifetimeSystemTest {
    @Test public void expiryRemovesOnlyExpiredEntitiesAndHandlesMultiTickUpdates() {
        World world=new World();
        Entity early=world.create().add(LifetimeComponent.class,new LifetimeComponent(2));
        Entity later=world.create().add(LifetimeComponent.class,new LifetimeComponent(5));
        Entity permanent=world.create();
        LifetimeSystem expiry=new LifetimeSystem();expiry.update(world,2);
        assertNull(world.find(early.id()));assertNotNull(world.find(later.id()));
        expiry.update(world,3);assertNull(world.find(later.id()));assertNotNull(world.find(permanent.id()));
    }
}
