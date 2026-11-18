package com.example.amazonMages.items;

import com.example.amazonMages.ecs.ColliderComponent;
import com.example.amazonMages.ecs.Entity;
import com.example.amazonMages.ecs.LifetimeComponent;
import com.example.amazonMages.ecs.PickupComponent;
import com.example.amazonMages.ecs.TransformComponent;
import com.example.amazonMages.ecs.World;
import com.example.amazonMages.map.WorldDefinition;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.progression.SkillTree;
import java.util.List;
import java.util.Random;

/**
 * Keeps Coconuts, Herbs, and Mana Essences continuously flowing via Castanheira regrowth,
 * tree-side foraging spawns, stale ground pickup recycling, and full-stack transmutation.
 */
public final class WorldForagingSystem {
    public static final int MAX_GROUND_PICKUPS = 44;
    private int regrowTicks;
    private int forageTicks;

    public interface SpawnSink {
        void spawn(String itemId, int quantity, double x, double y);
    }

    /** Advances passive coconut reserve regrowth and periodic foraging drops near the player. */
    public boolean update(double playerX, double playerY, int playerHealth, int maxHealth,
                          Inventory inventory, PlayerProgression progression, SkillTree skills,
                          World pickupWorld, WorldDefinition world, Random random, SpawnSink sink) {
        boolean inventoryChanged = false;
        int cap = skills == null ? 8 : skills.coconutReserveCap();
        int regrowPeriod = skills == null ? 300 : skills.coconutRegrowTicks();
        if (inventory != null && inventory.count("coco") < cap) {
            if (++regrowTicks >= regrowPeriod) {
                regrowTicks = 0;
                inventoryChanged = inventory.add("coco", 1) > 0;
            }
        } else {
            regrowTicks = 0;
        }

        int foragePeriod = skills == null ? 210 : skills.foragingIntervalTicks();
        if (++forageTicks >= foragePeriod && sink != null && random != null) {
            forageTicks = 0;
            String itemId = chooseNeededItem(playerHealth, maxHealth, inventory, progression, random);
            int qty = "coco".equals(itemId) ? 2 : 1;
            double[] spot = findForageSpot(playerX, playerY, world, random);
            recycleIfCrowded(pickupWorld, playerX, playerY);
            sink.spawn(itemId, qty, spot[0], spot[1]);
        }
        return inventoryChanged;
    }

    /**
     * Converts nearby ground pickups whose inventory stack is already full (e.g. duplicate staffs
     * or capped herbs/coconuts) into bonus Mana + XP instead of leaving clutter on the ground.
     */
    public int transmuteFullStackPickups(World world, double playerX, double playerY, double playerRadius,
                                         Inventory inventory, ItemRegistry registry) {
        if (world == null || inventory == null || registry == null) return 0;
        List<Entity> all = world.with(PickupComponent.class, TransformComponent.class, ColliderComponent.class);
        int transmuted = 0;
        for (Entity e : all) {
            PickupComponent pickup = e.get(PickupComponent.class);
            TransformComponent pos = e.get(TransformComponent.class);
            ColliderComponent col = e.get(ColliderComponent.class);
            ItemDefinition def = registry.get(pickup.itemId);
            if (def == null || inventory.count(pickup.itemId) < def.maxStack) continue;
            double reach = playerRadius + col.radius + 14.0;
            if (Math.hypot(pos.x - playerX, pos.y - playerY) <= reach) {
                if (world.destroy(e.id())) transmuted++;
            }
        }
        return transmuted;
    }

    /** Removes the stalest/farthest pickup when approaching cap so new drops never get blocked. */
    public static void recycleIfCrowded(World pickupWorld, double playerX, double playerY) {
        if (pickupWorld == null || pickupWorld.size() < MAX_GROUND_PICKUPS) return;
        List<Entity> all = pickupWorld.with(PickupComponent.class, TransformComponent.class);
        Entity worst = null;
        double worstScore = -1.0;
        for (Entity e : all) {
            TransformComponent pos = e.get(TransformComponent.class);
            double dist = Math.hypot(pos.x - playerX, pos.y - playerY);
            LifetimeComponent life = e.get(LifetimeComponent.class);
            double agePenalty = life == null ? 0.0 : (36000 - life.remainingTicks) * 0.05;
            double score = dist + agePenalty;
            if (score > worstScore) {
                worstScore = score;
                worst = e;
            }
        }
        if (worst != null) pickupWorld.destroy(worst.id());
    }

    private static String chooseNeededItem(int health, int maxHealth, Inventory inventory,
                                           PlayerProgression progression, Random random) {
        int cocos = inventory == null ? 0 : inventory.count("coco");
        int ervas = inventory == null ? 0 : inventory.count("erva");
        int manas = inventory == null ? 0 : inventory.count("mana");
        if (cocos < 14) return "coco";
        if ((health < maxHealth && ervas < 4) || ervas < 2) return "erva";
        if (progression != null && (progression.mana < progression.maxMana * 0.65 || manas < 2)) return "mana";
        return random.nextDouble() < 0.65 ? "coco" : "bencao";
    }

    private static double[] findForageSpot(double playerX, double playerY, WorldDefinition world, Random random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 180.0 + random.nextDouble() * 360.0;
            double sx = playerX + Math.cos(angle) * dist;
            double sy = playerY + Math.sin(angle) * dist;
            if (world == null) return new double[]{sx, sy};
            if (world.isWalkable(sx, sy, 16)) return new double[]{sx, sy};
        }
        if (world == null) return new double[]{playerX + 200, playerY};
        WorldDefinition.Position safe = world.safePosition(playerX + 180, playerY, 16);
        return new double[]{safe.x, safe.y};
    }
}

