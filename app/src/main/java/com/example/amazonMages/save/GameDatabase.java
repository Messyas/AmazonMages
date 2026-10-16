package com.example.amazonMages.save;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PlayerPositionEntity.class, QuestProgressEntity.class, PlayerStatsEntity.class, InventoryItemEntity.class}, version = 5, exportSchema = false)
public abstract class GameDatabase extends RoomDatabase {
    public abstract PlayerPositionDao playerPositionDao();
    public abstract QuestProgressDao questProgressDao();
    public abstract PlayerStatsDao playerStatsDao();
    public abstract InventoryItemDao inventoryItemDao();
}
