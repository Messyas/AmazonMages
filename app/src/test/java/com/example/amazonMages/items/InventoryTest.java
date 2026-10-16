package com.example.amazonMages.items;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class InventoryTest {
    @Test public void restoreClampsCountsAndSkipsUnknownItems() {
        ItemRegistry registry = new ItemRegistry()
            .register(new ItemDefinition("herb", "Herb", 5));
        Inventory inventory = new Inventory(registry);
        Map<String, Integer> saved = new LinkedHashMap<>();
        saved.put("herb", 100);
        saved.put("missing", 3);
        inventory.restore(saved);
        assertEquals(5, inventory.count("herb"));
        assertEquals(0, inventory.count("missing"));
        assertEquals(1, inventory.snapshot().size());
    }

    @Test public void removingLastItemClearsStack() {
        Inventory inventory = new Inventory(new ItemRegistry()
            .register(new ItemDefinition("herb", "Herb", 5)));
        assertEquals(3, inventory.add("herb", 3));
        assertTrue(inventory.remove("herb", 3));
        assertEquals(0, inventory.count("herb"));
        assertTrue(inventory.snapshot().isEmpty());
    }
}
