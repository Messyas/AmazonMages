package com.example.amazonMages.save;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface InventoryItemDao {
    @Query("SELECT * FROM inventory_items WHERE slotId = :slotId")
    List<InventoryItemEntity> loadForSlot(int slotId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<InventoryItemEntity> rows);

    @Query("DELETE FROM inventory_items WHERE slotId = :slotId")
    void deleteForSlot(int slotId);
}
