package com.example.amazonMages.ui;

import android.graphics.Canvas;
import com.example.amazonMages.items.*;
import com.example.amazonMages.combat.WeaponLoadout;

/** Fixed catalog selection; quantities are read live after collection, use and restoration. */
public final class InventoryPanel {
    private final CanvasUi ui = new CanvasUi();
    private final ItemDefinition[] items;
    private int selected;
    private boolean open;
    public InventoryPanel(ItemRegistry registry) { items = registry.all().values().toArray(new ItemDefinition[0]); }
    public boolean isOpen() { return open; }
    public void open() { open = true; }
    public void close() { open = false; }
    public ItemDefinition selected() { return items[selected]; }
    /** -2: close, -1: use, 0..3: equip, -3: selection/no action. */
    public int touch(float x, float y, int w, int h) {
        float nx=x/w, ny=y/h;
        if (CanvasUi.hit(nx,ny,.78f,.08f,.94f,.18f)) { close(); return -2; }
        if (CanvasUi.hit(nx,ny,.10f,.22f,.58f,.66f)) {
            int row=(int)((ny-.22f)/.11f);
            if (row >= 0 && row < items.length) selected=row;
        }
        if (CanvasUi.hit(nx,ny,.62f,.51f,.90f,.63f)) return -1;
        if (ny >= .73f && ny < .85f && nx >= .10f && nx < .90f) return Math.min(3,(int)((nx-.10f)/.20f));
        return -3;
    }
    public void draw(Canvas c, int w, int h, Inventory bag, WeaponLoadout weapons, String notice) {
        if (!open) return;
        ui.panel(c,w*.06f,h*.06f,w*.94f,h*.94f);
        ui.text(c,"INVENTARIO",w*.10f,h*.17f,h*.055f,0xFFF5EACD);
        ui.button(c,"X",w*.78f,h*.08f,w*.94f,h*.18f,false);
        for (int i=0;i<items.length;i++) {
            float t=h*(.22f+i*.11f);
            ui.button(c,items[i].name+" x"+bag.count(items[i].id),w*.10f,t,w*.58f,t+h*.09f,i==selected);
        }
        ItemDefinition item=selected();
        ui.text(c,item.description,w*.61f,h*.29f,h*.025f,0xFFF5EACD);
        ui.text(c,item.weapon==null ? "Consumivel" : "Escolha um slot abaixo",w*.61f,h*.40f,h*.025f,0xFFB9D8BE);
        ui.button(c,"USAR",w*.62f,h*.51f,w*.90f,h*.63f,false);
        for (int i=0;i<4;i++) ui.button(c,"Slot "+(i+1)+(weapons.at(i)==null ? " -" : " +"),
            w*(.10f+i*.20f),h*.73f,w*(.28f+i*.20f),h*.85f,i==weapons.selectedSlot());
        if (notice!=null) ui.text(c,notice,w*.10f,h*.91f,h*.027f,0xFFFFD17C);
    }
}
