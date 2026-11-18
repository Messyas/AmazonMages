package com.example.amazonMages.items;

import com.example.amazonMages.progression.PlayerProgression;
import org.junit.Test;
import static org.junit.Assert.*;

public class CraftingSystemTest {

    @Test public void allRecipesReferenceValidItemsAndCraftCleanly() {
        ItemRegistry registry = ItemCatalog.create();
        CraftingSystem crafting = new CraftingSystem();
        assertEquals(5, crafting.recipes().size());

        for (CraftingSystem.Recipe recipe : crafting.recipes()) {
            assertNotNull("Missing output item: " + recipe.outputItemId, registry.get(recipe.outputItemId));
            assertTrue(recipe.outputAmount >= 1);
            for (CraftingSystem.Ingredient ing : recipe.ingredients) {
                assertNotNull("Missing ingredient: " + ing.itemId, registry.get(ing.itemId));
                assertTrue(ing.amount >= 1);
            }
        }

        Inventory bag = new Inventory(registry);
        bag.add("coco", 15);
        bag.add("erva", 5);
        bag.add("mana", 4);
        bag.add("bencao", 2);
        assertEquals(5, crafting.countCraftable(bag, registry));

        CraftingSystem.Recipe elixir = crafting.byId("elixir_castanheira");
        assertEquals(1, crafting.craft(elixir, bag, registry));
        assertEquals(1, bag.count("elixir"));
        assertEquals(3, bag.count("erva"));
        assertEquals(13, bag.count("coco"));

        CraftingSystem.Recipe bomb = crafting.byId("bomba_pororoca");
        assertEquals(2, crafting.craft(bomb, bag, registry));
        assertEquals(2, bag.count("bomba_coco"));
        assertEquals(10, bag.count("coco"));
        assertEquals(3, bag.count("mana"));
    }

    @Test public void protectsLastPermanentCoconutSeed() {
        ItemRegistry registry = ItemCatalog.create();
        CraftingSystem crafting = new CraftingSystem();
        Inventory bag = new Inventory(registry);
        bag.add("coco", 2);
        bag.add("erva", 5);

        CraftingSystem.Recipe elixir = crafting.byId("elixir_castanheira");
        assertEquals(1, crafting.availableForCraft(bag, "coco"));
        assertFalse("Must preserve last coconut seed", crafting.canCraft(elixir, bag, registry));
        assertEquals(0, crafting.craft(elixir, bag, registry));
        assertEquals(2, bag.count("coco"));

        bag.add("coco", 1); // 3 total -> 2 spendable + 1 permanent seed
        assertTrue(crafting.canCraft(elixir, bag, registry));
        assertEquals(1, crafting.craft(elixir, bag, registry));
        assertEquals(1, bag.count("coco"));
        assertEquals(1, bag.count("elixir"));
    }

    @Test public void preventsCraftingWhenOutputStackIsFull() {
        ItemRegistry registry = ItemCatalog.create();
        CraftingSystem crafting = new CraftingSystem();
        Inventory bag = new Inventory(registry);
        bag.add("coco", 10);
        bag.add("erva", 10);
        bag.add("elixir", 10);

        CraftingSystem.Recipe elixir = crafting.byId("elixir_castanheira");
        assertFalse(crafting.canCraft(elixir, bag, registry));
        assertEquals(0, crafting.craft(elixir, bag, registry));
        assertEquals(10, bag.count("coco"));
    }

    @Test public void skillRuneCraftingAndUseGrantsSkillPoint() {
        ItemRegistry registry = ItemCatalog.create();
        CraftingSystem crafting = new CraftingSystem();
        Inventory bag = new Inventory(registry);
        bag.add("coco", 5); // 4 spendable + 1 seed
        bag.add("mana", 2);
        bag.add("bencao", 1);

        CraftingSystem.Recipe rune = crafting.byId("runa_floresta");
        assertTrue(crafting.canCraft(rune, bag, registry));
        assertEquals(1, crafting.craft(rune, bag, registry));
        assertEquals(1, bag.count("runa_ancestral"));
        assertEquals(1, bag.count("coco"));

        PlayerProgression progression = new PlayerProgression();
        int before = progression.skillPoints;
        ConsumableService consumables = new ConsumableService();
        assertTrue(consumables.useSkillRune(bag, progression));
        assertEquals(before + 1, progression.skillPoints);
        assertEquals(0, bag.count("runa_ancestral"));
    }
}

