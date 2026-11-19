package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.combat.WeaponDefinition;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.gamepanel.ForestHud;
import com.example.amazonMages.items.CraftingSystem;
import com.example.amazonMages.items.Inventory;
import com.example.amazonMages.items.ItemDefinition;
import com.example.amazonMages.items.ItemRegistry;
import com.example.amazonMages.progression.SkillTree;
import java.util.List;

/**
 * Tabbed GBA pixel-art modal hosting the Bag ("MOCHILA") and Alchemy Bench ("ALQUIMIA")
 * backed by PixelUiKit (ui_panel, ui_tab, ui_slot, ui_cursor_select, ui_craft_cauldron).
 */
public final class InventoryPanel {
    public static final int ACTION_CRAFT = -4;
    public static final int ACTION_NONE = -3;
    public static final int ACTION_CLOSE = -2;
    public static final int ACTION_USE = -1;

    private final ItemRegistry registry;
    private final ItemDefinition[] items;
    private final CraftingSystem crafting = new CraftingSystem();
    private final Paint shapePaint = new Paint();

    private final RectF panelRect = new RectF();
    private final RectF closeRect = new RectF();
    private final RectF tabBagRect = new RectF();
    private final RectF tabCraftRect = new RectF();
    private final RectF[] itemSlotRects;
    private final RectF[] weaponSlotRects = {new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF useRect = new RectF();

    private final RectF[] recipeRowRects;
    private final RectF craftBtnRect = new RectF();
    private final RectF showcaseRect = new RectF();
    private final RectF[] ingredientRects = {new RectF(), new RectF(), new RectF()};

    // Reusable scroll panes for inventory grid and craft recipes
    private final PixelScrollPane bagScrollPane = new PixelScrollPane();
    private final PixelScrollPane craftScrollPane = new PixelScrollPane();
    private final RectF bagViewport = new RectF();
    private final RectF craftViewport = new RectF();
    private float bagContentHeight;
    private float craftContentHeight;

    private PixelUiKit fallbackUiKit;
    private SkillTree skillTree;
    private int activeTab = 0; // 0 = Mochila, 1 = Alquimia (Craft)
    private int selected = 0;
    private int selectedRecipe = 0;
    private int cauldronBurstTicks = 0;
    private boolean open;

    public InventoryPanel(ItemRegistry registry) {
        this.registry = registry;
        this.items = registry.all().values().toArray(new ItemDefinition[0]);
        this.itemSlotRects = new RectF[items.length];
        for (int i = 0; i < items.length; i++) itemSlotRects[i] = new RectF();
        int rCount = crafting.recipes().size();
        this.recipeRowRects = new RectF[rCount];
        for (int i = 0; i < rCount; i++) recipeRowRects[i] = new RectF();
        shapePaint.setAntiAlias(false);
        shapePaint.setDither(false);
    }

    public void attachContext(Context context, SkillTree skillTree) {
        this.skillTree = skillTree;
        if (context != null && fallbackUiKit == null) {
            fallbackUiKit = new PixelUiKit(context);
        }
    }

    public boolean isOpen() { return open; }
    public void open() { open = true; activeTab = 0; }
    public void openCraft() { open = true; activeTab = 1; }
    public void close() {
        open = false;
        cauldronBurstTicks = 0;
        bagScrollPane.reset();
        craftScrollPane.reset();
    }
    public void selectTab(int tab) { if (tab == 0 || tab == 1) activeTab = tab; }
    public int activeTab() { return activeTab; }

    public ItemDefinition selected() { return items[Math.max(0, Math.min(items.length - 1, selected))]; }
    public CraftingSystem.Recipe selectedRecipe() {
        List<CraftingSystem.Recipe> list = crafting.recipes();
        return list.get(Math.max(0, Math.min(list.size() - 1, selectedRecipe)));
    }
    public CraftingSystem craftingSystem() { return crafting; }

    public int craftSelected(Inventory bag) {
        int made = crafting.craft(selectedRecipe(), bag, registry);
        if (made > 0) cauldronBurstTicks = 50;
        return made;
    }

    /** Full touch dispatcher supporting gesture scrolling, tabs, slots and buttons. */
    public int onTouch(android.view.MotionEvent event, int w, int h) {
        if (!open) return ACTION_NONE;
        layout(w, h);
        float x = event.getX();
        float y = event.getY();

        // Dispatch scroll gesture first
        if (activeTab == 0) {
            if (bagScrollPane.onTouchEvent(event, bagViewport, bagContentHeight)) {
                return ACTION_NONE;
            }
        } else {
            if (craftScrollPane.onTouchEvent(event, craftViewport, craftContentHeight)) {
                return ACTION_NONE;
            }
        }

        if (event.getActionMasked() != android.view.MotionEvent.ACTION_UP &&
            event.getActionMasked() != android.view.MotionEvent.ACTION_DOWN) {
            return ACTION_NONE;
        }

        // On tap (ACTION_DOWN or ACTION_UP without drag)
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
            if (closeRect.contains(x, y) || !panelRect.contains(x, y)) {
                close();
                return ACTION_CLOSE;
            }
            if (tabBagRect.contains(x, y)) {
                activeTab = 0;
                return ACTION_NONE;
            }
            if (tabCraftRect.contains(x, y)) {
                activeTab = 1;
                return ACTION_NONE;
            }
            if (activeTab == 0) {
                if (bagViewport.contains(x, y)) {
                    float sy = bagScrollPane.getScrollY();
                    for (int i = 0; i < itemSlotRects.length; i++) {
                        RectF r = itemSlotRects[i];
                        if (x >= r.left && x <= r.right && (y + sy) >= r.top && (y + sy) <= r.bottom) {
                            selected = i;
                            return ACTION_NONE;
                        }
                    }
                }
                if (useRect.contains(x, y) && selected().effect != null) return ACTION_USE;
                for (int s = 0; s < weaponSlotRects.length; s++) {
                    if (weaponSlotRects[s].contains(x, y)) return s;
                }
                return ACTION_NONE;
            }

            if (craftViewport.contains(x, y)) {
                float sy = craftScrollPane.getScrollY();
                for (int i = 0; i < recipeRowRects.length; i++) {
                    RectF row = recipeRowRects[i];
                    if (x >= row.left && x <= row.right && (y + sy) >= row.top && (y + sy) <= row.bottom) {
                        selectedRecipe = i;
                        return ACTION_NONE;
                    }
                }
            }
            if (craftBtnRect.contains(x, y)) return ACTION_CRAFT;
        }
        return ACTION_NONE;
    }

    /** -4: craft selected recipe, -2: close, -1: use, 0..3: equip weapon slot, -3: selection/consumed. */
    public int touch(float x, float y, int w, int h) {
        if (!open) return ACTION_NONE;
        layout(w, h);
        if (closeRect.contains(x, y) || !panelRect.contains(x, y)) {
            close();
            return ACTION_CLOSE;
        }
        if (tabBagRect.contains(x, y)) {
            activeTab = 0;
            return ACTION_NONE;
        }
        if (tabCraftRect.contains(x, y)) {
            activeTab = 1;
            return ACTION_NONE;
        }
        if (activeTab == 0) {
            float sy = bagScrollPane.getScrollY();
            for (int i = 0; i < itemSlotRects.length; i++) {
                RectF r = itemSlotRects[i];
                if (x >= r.left && x <= r.right && (y + sy) >= r.top && (y + sy) <= r.bottom) {
                    selected = i;
                    return ACTION_NONE;
                }
            }
            if (useRect.contains(x, y) && selected().effect != null) return ACTION_USE;
            for (int s = 0; s < weaponSlotRects.length; s++) {
                if (weaponSlotRects[s].contains(x, y)) return s;
            }
            return ACTION_NONE;
        }

        float sy = craftScrollPane.getScrollY();
        for (int i = 0; i < recipeRowRects.length; i++) {
            RectF row = recipeRowRects[i];
            if (x >= row.left && x <= row.right && (y + sy) >= row.top && (y + sy) <= row.bottom) {
                selectedRecipe = i;
                return ACTION_NONE;
            }
        }
        if (craftBtnRect.contains(x, y)) return ACTION_CRAFT;
        return ACTION_NONE;
    }

    public void draw(Canvas c, int w, int h, Inventory bag, WeaponLoadout weapons, String notice, ForestHud hud) {
        if (!open || w <= 0 || h <= 0) return;
        PixelUiKit ui = hud != null ? hud.uiKit() : fallbackUiKit;
        if (ui == null) return;
        if (cauldronBurstTicks > 0) cauldronBurstTicks--;

        layout(w, h);
        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(0xA406100A);
        c.drawRect(0, 0, w, h, shapePaint);

        ui.drawPanel(c, panelRect);
        float ribbonW = Math.min(panelRect.width() * .34f, 260f * Math.max(2, ui.scale()));
        float ribbonH = Math.max(46f, Math.min(58f, panelRect.height() * .095f));
        ui.drawTitleRibbon(c, panelRect.centerX(), panelRect.top - ribbonH * 0.42f, ribbonW, ribbonH,
            activeTab == 0 ? "MOCHILA" : "ALQUIMIA");
        ui.drawCloseButton(c, closeRect, false);

        int craftReady = crafting.countCraftable(bag, registry);
        String craftTabLabel = craftReady > 0 ? "ALQUIMIA (" + craftReady + ")" : "ALQUIMIA";
        ui.drawTab(c, tabBagRect, PixelUiKit.ICON_BAG, -1, "MOCHILA", activeTab == 0);
        ui.drawTab(c, tabCraftRect, -1, PixelUiKit.CRAFT_ICON_CAULDRON, craftTabLabel, activeTab == 1);

        float splitX = panelRect.left + panelRect.width() * .485f;
        ui.drawDividerV(c, splitX, tabBagRect.bottom + 8f, panelRect.bottom - 12f);

        if (activeTab == 0) {
            drawBagTab(c, ui, bag, weapons, notice, splitX);
        } else {
            drawCraftTab(c, ui, bag, notice, splitX);
        }
    }

    private void drawBagTab(Canvas c, PixelUiKit ui, Inventory bag, WeaponLoadout weapons, String notice, float splitX) {
        int s = ui.scale();
        bagScrollPane.begin(c, bagViewport);
        for (int i = 0; i < items.length; i++) {
            ItemDefinition item = items[i];
            int qty = bag.count(item.id);
            RectF r = itemSlotRects[i];
            boolean isSel = i == selected;
            int frame = isSel ? 1 : 0;
            ui.drawSlot(c, r, frame, isSel);
            ui.drawItemIcon(c, item.id, r.centerX(), r.centerY(), r.width() * .56f, qty > 0 ? 255 : 105);
            String badge = "x" + qty;
            int badgeColor = qty > 0 ? 0xFFFFF1B0 : 0xFF7E927C;
            ui.drawTextShadow(c, badge, r.right - 3f * s, r.bottom - 3f * s,
                Math.max(10f, r.height() * .24f), badgeColor, true, Paint.Align.RIGHT);
        }
        bagScrollPane.end(c);
        bagScrollPane.drawScrollbar(c, ui, bagViewport, bagContentHeight);

        float hotbarTop = weaponSlotRects[0].top;
        ui.drawDividerH(c, panelRect.left + 16f, splitX - 10f, hotbarTop - 16f);
        ui.drawTextShadow(c, "EQUIPAR ARMA EM SLOT:", panelRect.left + 18f, hotbarTop - 4f,
            Math.max(10f, panelRect.height() * .028f), 0xFFB8C99A, true, Paint.Align.LEFT);

        for (int i = 0; i < 4; i++) {
            RectF wr = weaponSlotRects[i];
            WeaponDefinition equipped = weapons.at(i);
            boolean active = i == weapons.selectedSlot();
            ui.drawSlot(c, wr, active ? 1 : (equipped == null ? 3 : 0), false);
            if (equipped != null) {
                ui.drawWeaponIcon(c, equipped, wr.centerX(), wr.centerY() - 2f, wr.width() * .52f);
            }
            ui.drawTextShadow(c, "S" + (i + 1), wr.left + 3f * s, wr.top + Math.max(11f, wr.height() * .26f),
                Math.max(9.5f, wr.height() * .22f), active ? 0xFFF2D36B : 0xFFB8C99A, true, Paint.Align.LEFT);
        }

        // Right inspector card
        showcaseRect.set(splitX + 10f, tabBagRect.bottom + 10f, panelRect.right - 16f, panelRect.bottom - 14f);
        ui.drawSubPanel(c, showcaseRect);

        ItemDefinition sel = selected();
        int selQty = bag.count(sel.id);
        float cx = showcaseRect.centerX();
        float bigSlot = Math.min(showcaseRect.width() * .28f, showcaseRect.height() * .25f);
        RectF iconBox = new RectF(cx - bigSlot * .5f, showcaseRect.top + 14f, cx + bigSlot * .5f, showcaseRect.top + 14f + bigSlot);
        ui.drawSlot(c, iconBox, 1, false);
        ui.drawItemIcon(c, sel.id, iconBox.centerX(), iconBox.centerY(), bigSlot * .62f, selQty > 0 ? 255 : 140);

        float titleSize = Math.max(13f, showcaseRect.height() * .050f);
        float textY = iconBox.bottom + titleSize * 1.25f;
        ui.drawTextShadow(c, sel.name, cx, textY, titleSize, 0xFFFFE082, true, Paint.Align.CENTER);

        boolean material = sel.weapon == null && sel.effect == null;
        String cat = material ? "Material" : sel.weapon == null ? "Consumível"
            : sel.effect != null ? "Arma & Municao" : "Arma";
        float catSize = Math.max(10.5f, showcaseRect.height() * .036f);
        textY += catSize * 1.40f;
        ui.drawTextShadow(c, cat + "  ·  " + selQty + "/" + sel.maxStack, cx, textY,
            catSize, 0xFF9EE6A8, false, Paint.Align.CENTER);

        float descSize = Math.max(11f, showcaseRect.height() * .038f);
        textY += descSize * 1.50f;
        float afterDesc = ui.drawWrappedText(c, sel.description, cx, textY,
            showcaseRect.width() * .88f, descSize, 0xFFF4EBC8, false, Paint.Align.CENTER);

        if (notice != null) {
            ui.drawTextShadow(c, notice, cx, Math.min(useRect.top - 8f, afterDesc + 8f),
                Math.max(10.5f, showcaseRect.height() * .036f), 0xFFF2D36B, true, Paint.Align.CENTER);
        }

        boolean canUse = sel.effect != null && selQty > ("coco".equals(sel.id) ? 1 : 0);
        if (!material) {
            ui.drawButton(c, useRect, sel.effect == null ? "SO EQUIPAVEL" : "USAR ITEM", canUse ? 0 : 2);
        }
    }

    private void drawCraftTab(Canvas c, PixelUiKit ui, Inventory bag, String notice, float splitX) {
        List<CraftingSystem.Recipe> recipes = crafting.recipes();
        craftScrollPane.begin(c, craftViewport);
        for (int i = 0; i < recipes.size(); i++) {
            CraftingSystem.Recipe r = recipes.get(i);
            RectF row = recipeRowRects[i];
            boolean isSel = i == selectedRecipe;
            boolean ready = crafting.canCraft(r, bag, registry);

            shapePaint.setStyle(Paint.Style.FILL);
            shapePaint.setColor(0xFF0B1A12);
            c.drawRect(row, shapePaint);
            shapePaint.setColor(isSel ? 0xFFF2D36B : (ready ? 0xFF3DC45E : 0xFF3A523F));
            c.drawRect(row.left + 2f, row.top + 2f, row.right - 2f, row.bottom - 2f, shapePaint);
            shapePaint.setColor(isSel ? 0xFF214630 : 0xFF13291D);
            c.drawRect(row.left + 4f, row.top + 4f, row.right - 4f, row.bottom - 4f, shapePaint);

            float slotSize = Math.min(row.height() - 8f, row.width() * .20f);
            RectF mini = new RectF(row.left + 6f, row.centerY() - slotSize * .5f,
                row.left + 6f + slotSize, row.centerY() + slotSize * .5f);
            ui.drawSlot(c, mini, isSel ? 1 : (ready ? 2 : 0), isSel);
            ui.drawCraftIcon(c, r.craftIconIndex, mini.centerX(), mini.centerY(), slotSize * .64f, 255);

            float textLeft = mini.right + 8f;
            float titleSize = Math.max(10.5f, Math.min(row.height() * .32f, row.width() * .058f));
            ui.drawTextShadow(c, r.title + (r.outputAmount > 1 ? " x" + r.outputAmount : ""),
                textLeft, row.centerY() - 2f, titleSize,
                isSel ? 0xFFFFF1B0 : 0xFFF4EBC8, true, Paint.Align.LEFT);

            String tag = ready ? "PRONTO" : "FALTA";
            int tagColor = ready ? 0xFF9BF06A : 0xFF7E927C;
            ui.drawTextShadow(c, tag, textLeft, row.centerY() + titleSize * 1.05f,
                Math.max(9.5f, titleSize * .84f), tagColor, ready, Paint.Align.LEFT);
        }
        craftScrollPane.end(c);
        craftScrollPane.drawScrollbar(c, ui, craftViewport, craftContentHeight);

        // Right Altar of Alchemy
        showcaseRect.set(splitX + 10f, tabCraftRect.bottom + 10f, panelRect.right - 16f, panelRect.bottom - 14f);
        ui.drawSubPanel(c, showcaseRect);

        CraftingSystem.Recipe sel = selectedRecipe();
        ItemDefinition outDef = registry.get(sel.outputItemId);
        int outCount = bag.count(sel.outputItemId);
        int outMax = outDef != null ? outDef.maxStack : 99;
        boolean canCraft = crafting.canCraft(sel, bag, registry);

        float cauldronSize = Math.min(showcaseRect.width() * .28f, showcaseRect.height() * .32f);
        float cauldronCx = showcaseRect.left + 16f + cauldronSize * .5f;
        float cauldronCy = showcaseRect.top + 14f + cauldronSize * .5f;
        boolean burst = cauldronBurstTicks > 0;
        ui.drawCauldron(c, burst, cauldronCx, cauldronCy, cauldronSize);

        float infoLeft = cauldronCx + cauldronSize * .5f + 14f;
        float infoW = showcaseRect.right - 12f - infoLeft;
        float titleSize = Math.max(13f, Math.min(showcaseRect.height() * .044f, infoW * .078f));
        float infoY = showcaseRect.top + titleSize * 1.25f;
        ui.drawTextShadow(c, sel.title + (sel.outputAmount > 1 ? " (x" + sel.outputAmount + ")" : ""),
            infoLeft, infoY, titleSize, 0xFFFFE082, true, Paint.Align.LEFT);

        String bonusText = (skillTree != null && skillTree.alchemyMultiplier() > 1.0)
            ? "Mochila: " + outCount + "/" + outMax + "  ·  Alquimia " + Math.round(skillTree.alchemyMultiplier() * 100) + "%"
            : "Mochila: " + outCount + "/" + outMax;
        float subSize = Math.max(10.5f, showcaseRect.height() * .031f);
        infoY += subSize * 1.35f;
        ui.drawTextShadow(c, bonusText, infoLeft, infoY, subSize, 0xFF9EE6A8, false, Paint.Align.LEFT);

        float descSize = Math.max(10.5f, showcaseRect.height() * .032f);
        infoY += descSize * 1.45f;
        float afterDesc = ui.drawWrappedText(c, sel.summary, infoLeft, infoY, infoW,
            descSize, 0xFFE6EFDA, false, Paint.Align.LEFT);

        float divY = Math.max(cauldronCy + cauldronSize * .5f + 10f, afterDesc + 6f);
        ui.drawDividerH(c, showcaseRect.left + 12f, showcaseRect.right - 12f, divY);

        ui.drawTextShadow(c, "INGREDIENTES", showcaseRect.centerX(), divY + 22f,
            Math.max(10.5f, showcaseRect.height() * .035f), 0xFFB8C99A, true, Paint.Align.CENTER);

        int countIng = sel.ingredients.size();
        float ingBoxSize = Math.min((showcaseRect.width() - 48f) / Math.max(3, countIng), showcaseRect.height() * .22f);
        float ingGap = Math.max(8f, (showcaseRect.width() - countIng * ingBoxSize) / (countIng + 1));
        float ingTop = divY + 32f;
        for (int i = 0; i < countIng && i < ingredientRects.length; i++) {
            CraftingSystem.Ingredient ing = sel.ingredients.get(i);
            float ix = showcaseRect.left + ingGap + i * (ingBoxSize + ingGap);
            RectF ir = ingredientRects[i];
            ir.set(ix, ingTop, ix + ingBoxSize, ingTop + ingBoxSize);
            int avail = crafting.availableForCraft(bag, ing.itemId);
            boolean met = avail >= ing.amount;
            ui.drawSlot(c, ir, met ? 0 : 3, false);
            ui.drawItemIcon(c, ing.itemId, ir.centerX(), ir.centerY(), ir.width() * .56f, met ? 255 : 150);

            String stat = avail + "/" + ing.amount;
            ui.drawTextShadow(c, ing.label + " " + stat, ir.centerX(), ir.bottom + 13f,
                Math.max(9.5f, showcaseRect.height() * .033f), met ? 0xFF9BF06A : 0xFFF4AE8A, true, Paint.Align.CENTER);
        }

        if (notice != null) {
            ui.drawTextShadow(c, notice, showcaseRect.centerX(), craftBtnRect.top - 7f,
                Math.max(10f, showcaseRect.height() * .035f), 0xFFFFF1B0, true, Paint.Align.CENTER);
        }

        ui.drawButton(c, craftBtnRect, canCraft ? "CRIAR ITEM" : "FALTA RECURSO", canCraft ? 0 : 2);
    }

    private void layout(int w, int h) {
        float panelRight = Math.min(w * .905f, w - Math.max(150f, w * .10f));
        panelRect.set(w * .045f, h * .08f, panelRight, h * .95f);
        float closeSize = Math.max(38f, Math.min(52f, panelRect.height() * .09f));
        closeRect.set(panelRect.right - closeSize - 10f, panelRect.top + 8f,
            panelRect.right - 10f, panelRect.top + 8f + closeSize);

        float ribbonH = Math.max(46f, Math.min(58f, panelRect.height() * .095f));
        float tabTop = panelRect.top + ribbonH * .58f + 4f;
        float tabH = Math.max(40f, Math.min(48f, panelRect.height() * .095f));
        float tabW = Math.min(panelRect.width() * .24f, 220f);
        tabBagRect.set(panelRect.left + 16f, tabTop, panelRect.left + 16f + tabW, tabTop + tabH);
        tabCraftRect.set(tabBagRect.right + 10f, tabTop, tabBagRect.right + 10f + tabW, tabTop + tabH);

        float splitX = panelRect.left + panelRect.width() * .485f;
        float leftMargin = panelRect.left + 16f;
        float leftRight = splitX - 12f;
        float bodyTop = tabBagRect.bottom + 10f;
        float bodyBottom = panelRect.bottom - 14f;

        // Bag grid (3 cols) + bottom weapon hotbar slots
        float hotbarH = Math.min(68f, (bodyBottom - bodyTop) * .24f);
        float gridBottom = bodyBottom - hotbarH - 22f;
        bagViewport.set(leftMargin, bodyTop, leftRight, gridBottom);

        int cols = 3;
        int rows = (items.length + cols - 1) / cols;
        float gap = 6f;
        float cell = (leftRight - leftMargin - gap * (cols - 1)) / cols;
        float gridW = cols * cell + (cols - 1) * gap;
        float gridLeft = leftMargin + (leftRight - leftMargin - gridW) * .5f;
        for (int i = 0; i < items.length; i++) {
            int r = i / cols, col = i % cols;
            float sl = gridLeft + col * (cell + gap);
            float st = bodyTop + r * (cell + gap);
            itemSlotRects[i].set(sl, st, sl + cell, st + cell);
        }
        bagContentHeight = rows * (cell + gap);

        float slotGap = 6f;
        float wSize = Math.min(hotbarH, (leftRight - leftMargin - slotGap * 3f) / 4f);
        float wRowLeft = leftMargin + (leftRight - leftMargin - (wSize * 4f + slotGap * 3f)) * .5f;
        float wTop = bodyBottom - wSize;
        for (int i = 0; i < 4; i++) {
            float wl = wRowLeft + i * (wSize + slotGap);
            weaponSlotRects[i].set(wl, wTop, wl + wSize, wTop + wSize);
        }

        // Right inspector buttons
        float insLeft = splitX + 12f;
        float insRight = panelRect.right - 18f;
        float insCx = (insLeft + insRight) * .5f;
        float btnW = Math.min(320f, (insRight - insLeft) * .72f);
        float btnH = Math.max(46f, Math.min(58f, panelRect.height() * .11f));
        useRect.set(insCx - btnW * .5f, panelRect.bottom - btnH - 22f, insCx + btnW * .5f, panelRect.bottom - 22f);
        craftBtnRect.set(insCx - btnW * .5f, panelRect.bottom - btnH - 22f, insCx + btnW * .5f, panelRect.bottom - 22f);

        // Craft recipe list on left with dedicated scroll viewport
        craftViewport.set(leftMargin, bodyTop, leftRight, bodyBottom);
        int rCount = recipeRowRects.length;
        float rowGap = 6f;
        float rowH = Math.max(42f, (bodyBottom - bodyTop - rowGap * 3f) / 4f);
        for (int i = 0; i < rCount; i++) {
            float rt = bodyTop + i * (rowH + rowGap);
            recipeRowRects[i].set(leftMargin, rt, leftRight - 10f, rt + rowH);
        }
        craftContentHeight = rCount * (rowH + rowGap);
    }
}
