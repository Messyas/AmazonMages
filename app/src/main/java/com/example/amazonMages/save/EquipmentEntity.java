package com.example.amazonMages.save;
import androidx.room.Entity;
import androidx.annotation.NonNull;
@Entity(tableName="equipment", primaryKeys={"slotId","weaponSlot"})
public class EquipmentEntity {
    public int slotId, weaponSlot;
    @NonNull public String itemId;
    public boolean selected;
    public EquipmentEntity(int slotId,int weaponSlot,@NonNull String itemId,boolean selected) {
        this.slotId=slotId; this.weaponSlot=weaponSlot; this.itemId=itemId; this.selected=selected;
    }
}
