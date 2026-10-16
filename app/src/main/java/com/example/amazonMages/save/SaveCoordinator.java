package com.example.amazonMages.save;

import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.items.*;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.ecs.*;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.quest.QuestJournal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Captures one coherent simulation snapshot on its owner's lock, before asynchronous I/O. */
public final class SaveCoordinator {
    private final GameSaveRepository repository;
    private final Inventory inventory;
    private final PlayerProgression progression;
    private final QuestJournal quests;
    private final Player player;
    private boolean ready;
    private final World pickups;
    private final WeaponLoadout weapons;
    private final ItemRegistry registry;

    public SaveCoordinator(GameSaveRepository repository, Inventory inventory,
            PlayerProgression progression, QuestJournal quests, Player player, World pickups, WeaponLoadout weapons, ItemRegistry registry) {
        this.repository = repository;
        this.inventory = inventory;
        this.progression = progression;
        this.quests = quests;
        this.player = player;
        this.pickups = pickups; this.weapons = weapons; this.registry = registry;
    }

    public void restoreWorld(List<EquipmentEntity> equipment, List<WorldPickupEntity> ground) {
        weapons.reset();
        for (EquipmentEntity row : equipment) {
            ItemDefinition item = registry.get(row.itemId);
            if (row.weaponSlot < 0 || row.weaponSlot >= 4 || item == null || item.weapon == null || inventory.count(item.id) == 0) continue;
            weapons.equip(row.weaponSlot, item.weapon);
            if (row.selected) weapons.select(row.weaponSlot);
        }
        pickups.clear();
        for (WorldPickupEntity row : ground) {
            ItemDefinition item = registry.get(row.itemId);
            if (pickups.size() >= 64) break;
            if (item == null || row.quantity < 1 || row.remainingTicks < 1 || !Double.isFinite(row.x) || !Double.isFinite(row.y)
                || row.x < 0 || row.y < 0 || row.x >= 6400 || row.y >= 4480) continue;
            pickups.create().add(TransformComponent.class, new TransformComponent(row.x, row.y))
                .add(ColliderComponent.class, new ColliderComponent(13))
                .add(PickupComponent.class, new PickupComponent(item.id, Math.min(row.quantity, item.maxStack)))
                .add(LifetimeComponent.class, new LifetimeComponent(Math.min(36000, row.remainingTicks)));
        }
    }

    public void ready() { ready = true; }

    public void save() {
        if (!ready) return;
        int slot = repository.getSlotId();
        List<InventoryItemEntity> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> item : inventory.snapshot().entrySet())
            rows.add(new InventoryItemEntity(slot, item.getKey(), item.getValue()));
        List<EquipmentEntity> equipment = new ArrayList<>();
        for (int i=0;i<4;i++) {
            String id=ItemCatalog.weaponId(weapons.at(i));
            if (id!=null) equipment.add(new EquipmentEntity(slot,i,id,i==weapons.selectedSlot()));
        }
        List<WorldPickupEntity> ground = new ArrayList<>();
        for (Entity entity : pickups.with(PickupComponent.class, TransformComponent.class, LifetimeComponent.class)) {
            PickupComponent item=entity.get(PickupComponent.class);
            TransformComponent position=entity.get(TransformComponent.class);
            ground.add(new WorldPickupEntity(slot,entity.id(),item.itemId,item.quantity,position.x,position.y,
                entity.get(LifetimeComponent.class).remainingTicks));
        }
        repository.saveSnapshot(new PlayerPositionEntity(slot, player.getPositionX(),
            player.getPositionY(), player.getHealthPoint(), System.currentTimeMillis()),
            progression.snapshot(slot), quests.snapshot(slot), rows, equipment, ground);
    }
}
