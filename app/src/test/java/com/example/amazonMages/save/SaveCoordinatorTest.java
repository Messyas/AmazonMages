package com.example.amazonMages.save;
import com.example.amazonMages.ecs.*;
import com.example.amazonMages.items.*;
import com.example.amazonMages.combat.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SaveCoordinatorTest {
    @Test public void restorationReplacesGroundStateRejectsInvalidRowsAndRetainsEquipment() {
        ItemRegistry catalog=ItemCatalog.create(); Inventory inventory=new Inventory(catalog);
        inventory.add("coco",1);inventory.add("cajado",1);
        World ground=new World();WeaponLoadout weapons=new WeaponLoadout();
        SaveCoordinator saves=new SaveCoordinator(null,inventory,null,null,null,ground,weapons,catalog);
        List<WorldPickupEntity> rows=Arrays.asList(
            new WorldPickupEntity(1,1,"erva",999,100,100,99999),
            new WorldPickupEntity(1,2,"unknown",1,100,100,10),
            new WorldPickupEntity(1,3,"erva",1,Double.NaN,100,10),
            new WorldPickupEntity(1,4,"erva",1,100,100,0));
        List<EquipmentEntity> equipped=Arrays.asList(new EquipmentEntity(1,3,"cajado",true),new EquipmentEntity(1,9,"coco",false));
        saves.restoreWorld(equipped,rows);saves.restoreWorld(equipped,rows);
        assertEquals(1,ground.size()); assertEquals(3,weapons.selectedSlot());assertSame(ItemCatalog.STAFF,weapons.current());
        Entity pickup=ground.with(PickupComponent.class).get(0);
        assertEquals(99,pickup.get(PickupComponent.class).quantity);
        assertEquals(36000,pickup.get(LifetimeComponent.class).remainingTicks);
        List<WorldPickupEntity> many=new ArrayList<>();
        for(int i=0;i<100;i++)many.add(new WorldPickupEntity(1,i,"erva",1,100,100,10));
        saves.restoreWorld(Collections.emptyList(),many);assertEquals(64,ground.size());
    }
}
