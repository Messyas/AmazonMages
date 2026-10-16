package com.example.amazonMages.items;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConsumableServiceTest {
    @Test public void healingConsumesOnlyWhenUseful() {
        Inventory bag = new Inventory(new ItemRegistry()
            .register(new ItemDefinition("erva", "Erva", 99)));
        bag.add("erva", 2);
        ConsumableService service = new ConsumableService();
        assertEquals(5, service.useHealingHerb(bag, 5, 5));
        assertEquals(2, bag.count("erva"));
        assertEquals(4, service.useHealingHerb(bag, 2, 5));
        assertEquals(1, bag.count("erva"));
        assertEquals(5, service.useHealingHerb(bag, 4, 5));
        assertEquals(0, bag.count("erva"));
        assertEquals(2, service.useHealingHerb(bag, 2, 5));
    }
}
