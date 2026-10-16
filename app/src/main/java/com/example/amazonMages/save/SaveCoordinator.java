package com.example.amazonMages.save;

import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.items.Inventory;
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

    public SaveCoordinator(GameSaveRepository repository, Inventory inventory,
            PlayerProgression progression, QuestJournal quests, Player player) {
        this.repository = repository;
        this.inventory = inventory;
        this.progression = progression;
        this.quests = quests;
        this.player = player;
    }

    public void ready() { ready = true; }

    public void save() {
        if (!ready) return;
        int slot = repository.getSlotId();
        List<InventoryItemEntity> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> item : inventory.snapshot().entrySet())
            rows.add(new InventoryItemEntity(slot, item.getKey(), item.getValue()));
        repository.saveSnapshot(new PlayerPositionEntity(slot, player.getPositionX(),
            player.getPositionY(), player.getHealthPoint(), System.currentTimeMillis()),
            progression.snapshot(slot), quests.snapshot(slot), rows);
    }
}
