package com.example.amazonMages.gamepanel;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.ai.EnemyManager;
import com.example.amazonMages.gameobject.Enemy;

/**
 * Minecraft-style HUD:
 * - Top-left: Pixel hearts (Health) + Lapis/Mana bar with XP-style Level badge
 * - Top-right: Compact stone-beveled [STATUS] and [INV] buttons (leaving screen clean)
 * - Bottom-center: Minecraft Hotbar with 4 slots and active selection cursor
 * - Bottom-right: Cohesive stone action buttons (ATK, ESP, TROCA)
 * - Top-center: Minecraft-style Boss Bar (when a boss encounter is active)
 * - Center of the screen: Completely uncluttered!
 */
public final class MinecraftHud {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF statusBtnBounds = new RectF();
    private final RectF invBtnBounds = new RectF();
    private final RectF[] hotbarBounds = new RectF[4];

    public MinecraftHud() {
        for (int i = 0; i < 4; i++) {
            hotbarBounds[i] = new RectF();
        }
    }

    public void draw(Canvas canvas, Player player, PlayerProgression progression,
                     WeaponLoadout weapons, EnemyManager enemyManager, float density,
                     String notice, int stormTicks) {
        float w = canvas.getWidth();
        float h = canvas.getHeight();

        drawTopLeftStatus(canvas, player, progression, density);
        drawTopRightButtons(canvas, w, density);
        drawHotbar(canvas, w, h, weapons, density);
        drawActionButtons(canvas, w, h, weapons, density);

        if (enemyManager != null && enemyManager.activeBoss() != null) {
            drawMinecraftBossBar(canvas, w, enemyManager.activeBoss(), density);
        }

        if (notice != null) {
            drawActionbarNotice(canvas, w, h, notice, density);
        }
    }

    /** Top-left: Hearts row and Mana bar with Level badge. */
    private void drawTopLeftStatus(Canvas canvas, Player player, PlayerProgression progression, float density) {
        float startX = 16 * density;
        float startY = 14 * density;

        int maxHp = Math.max(1, player.getMaximumHealth());
        int curHp = Math.max(0, player.getHealthPoint());
        float heartSize = 18 * density;
        float heartSpacing = 3 * density;

        // 1. Desenha os corações no estilo clássico do Minecraft
        for (int i = 0; i < maxHp; i++) {
            float hx = startX + i * (heartSize + heartSpacing);
            drawMinecraftHeart(canvas, hx, startY, heartSize, i < curHp);
        }

        // 2. Nível de XP / Nível do Mago (Badge estilo Minecraft Verde com sombra preta)
        float levelX = startX + maxHp * (heartSize + heartSpacing) + 8 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(14 * density);
        paint.setFakeBoldText(true);
        // Sombra preta
        paint.setColor(0xFF000000);
        canvas.drawText("Nv " + progression.level, levelX + 1.5f, startY + 14 * density + 1.5f, paint);
        // Texto verde característico do XP do Minecraft
        paint.setColor(0xFF80FF20);
        canvas.drawText("Nv " + progression.level, levelX, startY + 14 * density, paint);

        // 3. Barra de Mana estilo barra de experiência (Lapis Lazuli / Azul)
        float barY = startY + heartSize + 4 * density;
        float barWidth = Math.max(130 * density, maxHp * (heartSize + heartSpacing));
        float barHeight = 8 * density;

        // Fundo da barra
        paint.setColor(0xFF141920);
        canvas.drawRect(startX, barY, startX + barWidth, barY + barHeight, paint);

        // Preenchimento de Mana
        float manaRatio = Math.max(0f, Math.min(1f, (float) progression.mana / Math.max(1, (float) progression.maxMana)));
        paint.setColor(0xFF3878E0);
        canvas.drawRect(startX + 1, barY + 1, startX + 1 + (barWidth - 2) * manaRatio, barY + barHeight - 1, paint);

        // Brilho no topo do preenchimento
        paint.setColor(0xFF62A5F8);
        canvas.drawRect(startX + 1, barY + 1, startX + 1 + (barWidth - 2) * manaRatio, barY + 3 * density, paint);

        // Borda cinza chanfrada estilo Minecraft
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF3F4852);
        paint.setStrokeWidth(1.5f);
        canvas.drawRect(startX, barY, startX + barWidth, barY + barHeight, paint);
        paint.setStyle(Paint.Style.FILL);

        // Segmentos da barra (tiques do XP)
        paint.setColor(0x88000000);
        for (int s = 1; s <= 4; s++) {
            float sx = startX + (barWidth / 5f) * s;
            canvas.drawLine(sx, barY, sx, barY + barHeight, paint);
        }

        // Texto numérico discreto da Mana
        paint.setTextSize(10 * density);
        paint.setColor(0xFFB5D3FA);
        paint.setFakeBoldText(false);
        canvas.drawText((int) progression.mana + "/" + (int) progression.maxMana, startX + barWidth + 6 * density, barY + 7 * density, paint);
    }

    /** Desenha um coração clássico do Minecraft em vetor pixelizado. */
    private void drawMinecraftHeart(Canvas canvas, float x, float y, float size, boolean full) {
        float s = size / 9f;
        Path path = new Path();
        path.moveTo(x + 2 * s, y);
        path.lineTo(x + 4 * s, y);
        path.lineTo(x + 4 * s, y + s);
        path.lineTo(x + 5 * s, y + s);
        path.lineTo(x + 5 * s, y);
        path.lineTo(x + 7 * s, y);
        path.lineTo(x + 9 * s, y + 2 * s);
        path.lineTo(x + 9 * s, y + 5 * s);
        path.lineTo(x + 5 * s, y + 9 * s);
        path.lineTo(x + 4 * s, y + 9 * s);
        path.lineTo(x, y + 5 * s);
        path.lineTo(x, y + 2 * s);
        path.close();

        // Fundo (coração cheio ou vazio)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(full ? 0xFFDE2A2A : 0xFF2A1515);
        canvas.drawPath(path, paint);

        // Borda escura
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF140808);
        paint.setStrokeWidth(Math.max(1.5f, s * 0.9f));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        // Brilho specular branco no topo esquerdo quando cheio
        if (full) {
            paint.setColor(0xFFFFFFFF);
            canvas.drawRect(x + 2 * s, y + 2 * s, x + 3.4f * s, y + 3.4f * s, paint);
            paint.setColor(0xFF9E1010);
            canvas.drawRect(x + 5 * s, y + 5 * s, x + 7 * s, y + 7 * s, paint);
        }
    }

    /** Top-right: Botões elegantes [STATUS] e [INV] ao lado do botão de pause. */
    private void drawTopRightButtons(Canvas canvas, float w, float density) {
        float btnH = 34 * density;
        float btnW = 66 * density;
        float topY = 12 * density;

        // O botão de pause da MainActivity ocupa os últimos 68dp à direita.
        float invR = w - 74 * density;
        float invL = invR - btnW;
        invBtnBounds.set(invL, topY, invR, topY + btnH);
        drawMinecraftStoneButton(canvas, invBtnBounds, "🎒 INV", false, density);

        float statusR = invL - 8 * density;
        float statusL = statusR - (btnW + 12 * density);
        statusBtnBounds.set(statusL, topY, statusR, topY + btnH);
        drawMinecraftStoneButton(canvas, statusBtnBounds, "⭐ STATUS", false, density);
    }

    /** Botão chanfrado 3D clássico estilo Minecraft. */
    private void drawMinecraftStoneButton(Canvas canvas, RectF r, String label, boolean active, float density) {
        // Preenchimento
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? 0xFF4A4A4A : 0xFF2E3230);
        canvas.drawRect(r, paint);

        // Chanfro 3D (Luz superior e esquerda)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(active ? 0xFF888888 : 0xFF5A625E);
        canvas.drawLine(r.left, r.top, r.right, r.top, paint);
        canvas.drawLine(r.left, r.top, r.left, r.bottom, paint);

        // Chanfro 3D (Sombra inferior e direita)
        paint.setColor(0xFF141716);
        canvas.drawLine(r.left, r.bottom, r.right, r.bottom, paint);
        canvas.drawLine(r.right, r.top, r.right, r.bottom, paint);

        // Texto com sombra
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12 * density);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);

        float textX = r.centerX();
        float textY = r.centerY() + 4 * density;
        paint.setColor(0xFF000000);
        canvas.drawText(label, textX + 1.2f, textY + 1.2f, paint);
        paint.setColor(active ? 0xFFFFFFFF : 0xFFF5EACD);
        canvas.drawText(label, textX, textY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    /** Hotbar (Barra Rápida de Armas no Centro Inferior). */
    private void drawHotbar(Canvas canvas, float w, float h, WeaponLoadout weapons, float density) {
        float slotSize = 44 * density;
        float gap = 4 * density;
        float totalW = 4 * slotSize + 3 * gap;
        float startX = (w - totalW) * 0.5f;
        float startY = h - slotSize - 10 * density;

        int selectedSlot = weapons.selectedSlot();

        for (int i = 0; i < 4; i++) {
            float sl = startX + i * (slotSize + gap);
            float sr = sl + slotSize;
            float st = startY;
            float sb = st + slotSize;
            hotbarBounds[i].set(sl, st, sr, sb);

            boolean isSelected = (i == selectedSlot);

            // Fundo do slot estilo Minecraft
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xD01E2220);
            canvas.drawRect(hotbarBounds[i], paint);

            // Borda do slot chanfrada
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * density);
            paint.setColor(0xFF101211);
            canvas.drawLine(sl, st, sr, st, paint);
            canvas.drawLine(sl, st, sl, sb, paint);
            paint.setColor(0xFF4C5450);
            canvas.drawLine(sl, sb, sr, sb, paint);
            canvas.drawLine(sr, st, sr, sb, paint);

            // Cursor de seleção destacado do Minecraft (caixa branca com sombra)
            if (isSelected) {
                paint.setStrokeWidth(3 * density);
                paint.setColor(0xFF000000);
                canvas.drawRect(sl - 2 * density, st - 2 * density, sr + 2 * density, sb + 2 * density, paint);
                paint.setColor(0xFFFFFFFF);
                canvas.drawRect(sl - 1 * density, st - 1 * density, sr + 1 * density, sb + 1 * density, paint);
            }

            // Número do slot (1, 2, 3, 4)
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(9 * density);
            paint.setColor(0xFF9EA6A1);
            canvas.drawText(String.valueOf(i + 1), sl + 4 * density, st + 11 * density, paint);

            // Nome do item no slot
            String itemName = "—";
            if (weapons.at(i) != null) {
                itemName = weapons.at(i).name;
                if (itemName.contains("Cocos")) itemName = "COCO";
                else if (itemName.contains("Cajado")) itemName = "CAJADO";
                else if (itemName.length() > 6) itemName = itemName.substring(0, 6);
            }

            paint.setTextSize(10 * density);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(isSelected ? 0xFFFFD17C : 0xFFE0DDD5);
            canvas.drawText(itemName, hotbarBounds[i].centerX(), hotbarBounds[i].centerY() + 5 * density, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
        }
    }

    /** Botões de ação no canto inferior direito integrados com estilo de pedra tátil. */
    private void drawActionButtons(Canvas canvas, float w, float h, WeaponLoadout weapons, float density) {
        float rAtk = Math.min(w, h) * 0.082f;
        float cxAtk = w * 0.895f;
        float cyAtk = h * 0.845f;
        drawActionCircle(canvas, cxAtk, cyAtk, rAtk, "ATK", 0xFF8C3425, density);

        float rEsp = Math.min(w, h) * 0.068f;
        float cxEsp = w * 0.775f;
        float cyEsp = h * 0.855f;
        String espLabel = weapons.specialCooldown() == 0 ? "ESP" : "" + (weapons.specialCooldown() / 60 + 1);
        drawActionCircle(canvas, cxEsp, cyEsp, rEsp, espLabel, weapons.specialCooldown() == 0 ? 0xFF9E7B26 : 0xFF353C39, density);

        float rTroca = Math.min(w, h) * 0.058f;
        float cxTroca = w * 0.875f;
        float cyTroca = h * 0.655f;
        drawActionCircle(canvas, cxTroca, cyTroca, rTroca, "TROCA", 0xFF2F4C41, density);
    }

    private void drawActionCircle(Canvas canvas, float cx, float cy, float r, String label, int baseColor, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xCC000000 | (baseColor & 0x00FFFFFF));
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x88FFFFFF);
        paint.setStrokeWidth(2f * density);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(Math.max(12, r * 0.45f));
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFF000000);
        canvas.drawText(label, cx + 1.2f, cy + (r * 0.16f) + 1.2f, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText(label, cx, cy + (r * 0.16f), paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    /** Barra de chefe estilo Ender Dragon no topo da tela. */
    private void drawMinecraftBossBar(Canvas canvas, float w, Enemy boss, float density) {
        float barWidth = Math.min(w * 0.65f, 380 * density);
        float barLeft = (w - barWidth) * 0.5f;
        float barTop = 14 * density;
        float barHeight = 10 * density;

        // Fundo escuro
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF1B0B1E);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, paint);

        // Preenchimento roxo/magenta clássico do chefe no Minecraft
        float ratio = (float) boss.getHealthRatio();
        paint.setColor(0xFFB022D9);
        canvas.drawRect(barLeft + 1, barTop + 1, barLeft + 1 + (barWidth - 2) * ratio, barTop + barHeight - 1, paint);

        // Borda dourada
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(0xFFD4A46C);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, paint);
        paint.setStyle(Paint.Style.FILL);

        // Nome do chefe acima da barra
        paint.setTextSize(12 * density);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFF000000);
        canvas.drawText(boss.getTemplate().displayName + " - Fase " + (boss.getPhase() + 1),
            w * 0.5f + 1, barTop - 4 * density + 1, paint);
        paint.setColor(0xFFFFE082);
        canvas.drawText(boss.getTemplate().displayName + " - Fase " + (boss.getPhase() + 1),
            w * 0.5f, barTop - 4 * density, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    /** Notificação discreta estilo actionbar do Minecraft acima da hotbar. */
    private void drawActionbarNotice(Canvas canvas, float w, float h, String text, float density) {
        float y = h - 68 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12 * density);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFF000000);
        canvas.drawText(text, w * 0.5f + 1.2f, y + 1.2f, paint);
        paint.setColor(0xFFFFD17C);
        canvas.drawText(text, w * 0.5f, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    public boolean isInventoryPressed(float x, float y) {
        return invBtnBounds.contains(x, y);
    }

    public boolean isStatusPressed(float x, float y) {
        return statusBtnBounds.contains(x, y);
    }

    public int getHotbarSlotTouched(float x, float y) {
        for (int i = 0; i < 4; i++) {
            if (hotbarBounds[i].contains(x, y)) return i;
        }
        return -1;
    }
}
