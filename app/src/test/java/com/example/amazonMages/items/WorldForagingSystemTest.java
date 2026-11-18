package com.example.amazonMages.items;

import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.combat.StatusEffects;
import com.example.amazonMages.ecs.ColliderComponent;
import com.example.amazonMages.ecs.Entity;
import com.example.amazonMages.ecs.LifetimeComponent;
import com.example.amazonMages.ecs.PickupComponent;
import com.example.amazonMages.ecs.TransformComponent;
import com.example.amazonMages.ecs.World;
import com.example.amazonMages.effects.GameplayEffect;
import com.example.amazonMages.map.WorldDefinition;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.progression.SkillTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldForagingSystemTest {
    @Test public void coconutReserveRegrowsAndTreesSpawnReplenishments() {
        ItemRegistry registry = ItemCatalog.create();
        Inventory bag = new Inventory(registry);
        bag.add("coco", 1);
        PlayerProgression stats = new PlayerProgression();
        SkillTree skills = new SkillTree();
        World ground = new World();
        WorldDefinition world = new WorldDefinition();
        WorldForagingSystem foraging = new WorldForagingSystem();
        Random rng = new Random(42);
        List<String> spawned = new ArrayList<>();

        int ticks = Math.max(skills.coconutRegrowTicks(), skills.foragingIntervalTicks());
        for (int i = 0; i < ticks; i++) {
            foraging.update(world.spawn.worldX(), world.spawn.worldY(), 5, 5,
                bag, stats, skills, ground, world, rng, (item, qty, x, y) -> spawned.add(item));
        }
        assertTrue(bag.count("coco") >= 2);
        assertFalse(spawned.isEmpty());
        assertEquals("coco", spawned.get(0));
    }

    @Test public void recycleIfCrowdedAndTransmutePreventMapClutter() {
        ItemRegistry registry = ItemCatalog.create();
        Inventory bag = new Inventory(registry);
        bag.add("cajado", 1);
        World ground = new World();

        for (int i = 0; i < WorldForagingSystem.MAX_GROUND_PICKUPS; i++) {
            ground.create()
                .add(TransformComponent.class, new TransformComponent(100 + i * 25.0, 100))
                .add(ColliderComponent.class, new ColliderComponent(13))
                .add(PickupComponent.class, new PickupComponent("erva", 1))
                .add(LifetimeComponent.class, new LifetimeComponent(30000 - i));
        }
        assertEquals(WorldForagingSystem.MAX_GROUND_PICKUPS, ground.size());
        WorldForagingSystem.recycleIfCrowded(ground, 100, 100);
        assertEquals(WorldForagingSystem.MAX_GROUND_PICKUPS - 1, ground.size());

        // Nearby duplicate staff (already at maxStack=1) is transmuted instead of stuck on ground
        Entity dupStaff = ground.create()
            .add(TransformComponent.class, new TransformComponent(105, 100))
            .add(ColliderComponent.class, new ColliderComponent(13))
            .add(PickupComponent.class, new PickupComponent("cajado", 1));
        WorldForagingSystem foraging = new WorldForagingSystem();
        int transmuted = foraging.transmuteFullStackPickups(ground, 100, 100, 32, bag, registry);
        assertEquals(1, transmuted);
        assertEquals(WorldForagingSystem.MAX_GROUND_PICKUPS - 1, ground.size());
        assertNotNull(dupStaff);
    }

    @Test public void lastCoconutIsProtectedAsPermanentWeaponSeed() {
        ItemRegistry registry = ItemCatalog.create();
        Inventory bag = new Inventory(registry);
        bag.add("coco", 2);
        ConsumableService service = new ConsumableService();
        final double[] healed = {0};
        GameplayEffect.Target hurtTarget = new GameplayEffect.Target() {
            private final StatusEffects fx = new StatusEffects();
            @Override public boolean damage(com.example.amazonMages.combat.DamageEvent hit) { return false; }
            @Override public boolean heal(double amount) { healed[0] += amount; return true; }
            @Override public boolean mana(double amount) { return true; }
            @Override public StatusEffects statuses() { return fx; }
        };
        assertTrue(service.use(bag, registry.get("coco"), hurtTarget));
        assertEquals(1, bag.count("coco"));
        assertFalse(service.use(bag, registry.get("coco"), hurtTarget));
        assertEquals(1, bag.count("coco"));
        assertEquals(1.0, healed[0], 0.01);
    }
}

