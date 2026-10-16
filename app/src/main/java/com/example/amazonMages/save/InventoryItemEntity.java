package com.example.amazonMages.save;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** Inventory stack scoped to a save slot. */
@Entity(tableName = "inventory_items", primaryKeys = {"slotId", "itemId"})
public class InventoryItemEntity {
    public int slotId;
    @NonNull public String itemId;
    public int quantity;

    public InventoryItemEntity(int slotId, @NonNull String itemId, int quantity) {
        this.slotId = slotId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
