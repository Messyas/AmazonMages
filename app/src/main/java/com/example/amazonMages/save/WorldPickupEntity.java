package com.example.amazonMages.save;
import androidx.room.Entity;
import androidx.annotation.NonNull;
@Entity(tableName="world_pickups", primaryKeys={"slotId","pickupId"})
public class WorldPickupEntity {
    public int slotId, quantity, remainingTicks;
    public long pickupId;
    @NonNull public String itemId;
    public double x,y;
    public WorldPickupEntity(int slotId,long pickupId,@NonNull String itemId,int quantity,double x,double y,int remainingTicks) {
        this.slotId=slotId;this.pickupId=pickupId;this.itemId=itemId;this.quantity=quantity;
        this.x=x;this.y=y;this.remainingTicks=remainingTicks;
    }
}
