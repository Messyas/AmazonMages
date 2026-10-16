package com.example.amazonMages.combat;

public final class WeaponLoadout {
    public static final WeaponDefinition COCONUT = new WeaponDefinition("Domínio dos Cocos", WeaponType.INTRINSIC, new DamageEvent(1, 12, DamageType.PHYSICAL));
    private final WeaponDefinition[] slots = new WeaponDefinition[4];
    private int selected;
    private int basicCooldown;
    private int specialCooldown;
    public WeaponLoadout() { slots[0] = COCONUT; }
    public WeaponDefinition current() { return slots[selected]; }
    public int selectedSlot() { return selected; }
    public void equip(int index, WeaponDefinition item) { if (index < 0 || index >= 4) throw new IllegalArgumentException("slot"); slots[index] = item; }
    public void next() { for (int i = 1; i <= 4; i++) { int n = (selected + i) % 4; if (slots[n] != null) { selected = n; return; } } }
    public boolean basic() { if (current() == null || basicCooldown > 0) return false; basicCooldown = 15; return true; }
    public boolean special() { if (current() != COCONUT || specialCooldown > 0) return false; specialCooldown = 1080; return true; }
    public void update() { if (basicCooldown > 0) basicCooldown--; if (specialCooldown > 0) specialCooldown--; }
    public int specialCooldown() { return specialCooldown; }
}
