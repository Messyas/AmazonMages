package com.example.amazonMages.save;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface QuestProgressDao {
    @Query("SELECT * FROM quest_progress WHERE slotId = :slotId")
    List<QuestProgressEntity> loadForSlot(int slotId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<QuestProgressEntity> progress);

    @Query("DELETE FROM quest_progress WHERE slotId = :slotId")
    void deleteForSlot(int slotId);
}
