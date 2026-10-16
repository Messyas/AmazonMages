package com.example.amazonMages.combat;
import com.example.amazonMages.items.ItemCatalog;
import org.junit.Test;
import static org.junit.Assert.*;
public class WeaponLoadoutTest {
    @Test public void movingAWeaponDoesNotDuplicateItAcrossSlots() {
        WeaponLoadout weapons=new WeaponLoadout();weapons.equip(3,WeaponLoadout.COCONUT);
        assertNull(weapons.at(0));assertSame(WeaponLoadout.COCONUT,weapons.at(3));assertNotNull(weapons.current());
    }
    @Test public void initialCoconutAndSelectionSkipEmptySlots() {
        WeaponLoadout weapons=new WeaponLoadout();
        assertSame(WeaponLoadout.COCONUT,weapons.current());
        weapons.equip(3,ItemCatalog.STAFF); weapons.next();
        assertEquals(3,weapons.selectedSlot());
        assertFalse(weapons.special()); weapons.next();
        assertEquals(0,weapons.selectedSlot()); assertTrue(weapons.special()); assertFalse(weapons.special());
        weapons.equip(0,null);
        assertNotNull(weapons.current());
    }
}
