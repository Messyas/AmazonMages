package com.example.amazonMages.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java Forest Alchemy / Crafting catalog and recipe evaluator.
 * Protects the player's last "coco" permanent weapon seed when evaluating or crafting.
 */
public final class CraftingSystem {

    public static final class Ingredient {
        public final String itemId;
        public final String label;
        public final int amount;

        public Ingredient(String itemId, String label, int amount) {
            this.itemId = itemId;
            this.label = label;
            this.amount = amount;
        }
    }

    public static final class Recipe {
        public final String id;
        public final String outputItemId;
        public final int outputAmount;
        public final String title;
        public final String summary;
        public final int craftIconIndex;
        public final List<Ingredient> ingredients;

        public Recipe(String id, String outputItemId, int outputAmount, String title,
                      String summary, int craftIconIndex, Ingredient... ingredients) {
            this.id = id;
            this.outputItemId = outputItemId;
            this.outputAmount = outputAmount;
            this.title = title;
            this.summary = summary;
            this.craftIconIndex = craftIconIndex;
            this.ingredients = Collections.unmodifiableList(Arrays.asList(ingredients));
        }
    }

    private final List<Recipe> recipes = Collections.unmodifiableList(Arrays.asList(
        new Recipe(
            "elixir_castanheira", "elixir", 1,
            "Elixir da Castanheira",
            "Combina ervas e coco para curar +4 de Vida (escala com Alquimia).",
            0,
            new Ingredient("erva", "Erva", 2),
            new Ingredient("coco", "Coco", 2)
        ),
        new Recipe(
            "bomba_pororoca", "bomba_coco", 2,
            "Bomba de Pororoca",
            "Forja 2 bombas explosivas (240px) que zera a recarga do Especial.",
            1,
            new Ingredient("coco", "Coco", 3),
            new Ingredient("mana", "Mana", 1)
        ),
        new Recipe(
            "tonico_tupa", "tonico", 1,
            "Tonico de Tupa",
            "Concede +30% velocidade/defesa por 10s e restaura +30 Mana.",
            2,
            new Ingredient("mana", "Mana", 2),
            new Ingredient("erva", "Erva", 1),
            new Ingredient("coco", "Coco", 1)
        ),
        new Recipe(
            "essencia_pura", "mana", 2,
            "Destilar Essencia",
            "Transmuta cocos excedentes e erva em 2 Essencias de Mana pura.",
            5,
            new Ingredient("coco", "Coco", 3),
            new Ingredient("erva", "Erva", 1)
        ),
        new Recipe(
            "runa_floresta", "runa_ancestral", 1,
            "Runa da Floresta",
            "Reliquia rara: ao usar, concede +1 Ponto de Habilidade permanente!",
            3,
            new Ingredient("coco", "Coco", 4),
            new Ingredient("mana", "Mana", 2),
            new Ingredient("bencao", "Bencao", 1)
        )
    ));

    public List<Recipe> recipes() {
        return recipes;
    }

    public Recipe byId(String id) {
        if (id == null) return null;
        for (Recipe r : recipes) {
            if (id.equals(r.id)) return r;
        }
        return null;
    }

    /** Coconuts keep 1 permanent weapon seed in the bag, so spendable coco count is count - 1. */
    public int availableForCraft(Inventory bag, String itemId) {
        if (bag == null || itemId == null) return 0;
        int raw = bag.count(itemId);
        return "coco".equals(itemId) ? Math.max(0, raw - 1) : raw;
    }

    public boolean canCraft(Recipe recipe, Inventory bag, ItemRegistry registry) {
        if (recipe == null || bag == null || registry == null) return false;
        ItemDefinition outDef = registry.get(recipe.outputItemId);
        if (outDef == null || bag.count(recipe.outputItemId) >= outDef.maxStack) return false;
        for (Ingredient ing : recipe.ingredients) {
            if (availableForCraft(bag, ing.itemId) < ing.amount) return false;
        }
        return true;
    }

    public int countCraftable(Inventory bag, ItemRegistry registry) {
        int ready = 0;
        for (Recipe r : recipes) {
            if (canCraft(r, bag, registry)) ready++;
        }
        return ready;
    }

    /** Consumes required ingredients and adds output up to stack limit. Returns units added (0 on failure). */
    public int craft(Recipe recipe, Inventory bag, ItemRegistry registry) {
        if (!canCraft(recipe, bag, registry)) return 0;
        for (Ingredient ing : recipe.ingredients) {
            if (!bag.remove(ing.itemId, ing.amount)) return 0;
        }
        int added = bag.add(recipe.outputItemId, recipe.outputAmount);
        return Math.max(1, added);
    }
}

