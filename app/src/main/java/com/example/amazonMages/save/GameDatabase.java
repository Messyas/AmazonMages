package com.example.amazonMages.save;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PlayerPositionEntity.class, QuestProgressEntity.class}, version = 3, exportSchema = false)
public abstract class GameDatabase extends RoomDatabase {
    public abstract PlayerPositionDao playerPositionDao();
    public abstract QuestProgressDao questProgressDao();
}
