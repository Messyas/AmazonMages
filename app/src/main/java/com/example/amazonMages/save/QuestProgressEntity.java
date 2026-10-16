package com.example.amazonMages.save;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "quest_progress", primaryKeys = {"slotId", "questId"})
public class QuestProgressEntity {
    public int slotId;
    @NonNull public String questId;
    @NonNull public String state;
    public int progress;

    public QuestProgressEntity(int slotId, @NonNull String questId, @NonNull String state, int progress) {
        this.slotId = slotId;
        this.questId = questId;
        this.state = state;
        this.progress = progress;
    }
}
