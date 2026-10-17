package com.example.amazonMages.progression;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.gamepanel.ForestHud;

/** Canvas modal with seven tappable attributes and clear exit options. */
public final class AttributesPanel {
    private static final String[] KEYS = {"vigor", "mente", "resistencia", "inteligencia", "destreza", "arcano", "forca"};
    private static final String[] LABELS = {"Vigor", "Mente", "Resistencia", "Inteligencia", "Destreza", "Arcano", "Forca"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean open;
    public boolean isOpen() { return open; }
    public void toggle() { open = !open; }
    public void close() { open = false; }

    public boolean touch(float x, float y, int width, int height, PlayerProgression stats) {
        if (!open) return false;
        float left = width * .12f, top = height * .08f;
        float panelWidth = width * .76f, panelHeight = height * .84f;

        // 1. Toque fora do painel fecha a interface
        if (x < left || x > left + panelWidth || y < top || y > top + panelHeight) {
            close();
            return true;
        }

        // 2. Botão [X] no topo direito
        float closeBtnLeft = left + panelWidth - 60;
        float closeBtnBottom = top + 56;
        if (x >= closeBtnLeft && x <= left + panelWidth && y >= top && y <= closeBtnBottom) {
            close();
            return true;
        }

        // 3. Botão [FECHAR] na parte inferior
        float bottomBtnL = left + panelWidth * 0.22f;
        float bottomBtnT = top + panelHeight - 48;
        float bottomBtnR = left + panelWidth * 0.78f;
        float bottomBtnB = top + panelHeight - 10;
        if (x >= bottomBtnL && x <= bottomBtnR && y >= bottomBtnT && y <= bottomBtnB) {
            close();
            return true;
        }

        // 4. Compatibilidade com área de clique no cabeçalho
        if (x > left + panelWidth * .67f && y >= top && y < top + 58) {
            close();
            return true;
        }

        // 5. Atribuição de pontos (+)
        float rowTop = top + panelHeight * .28f;
        float rowHeight = panelHeight * .075f;
        int index = (int)((y - rowTop) / rowHeight);
        if (index >= 0 && index < KEYS.length && x > left + panelWidth * .60f && x < left + panelWidth && y >= rowTop) {
            return stats.allocate(KEYS[index]);
        }
        return false;
    }

    public void draw(Canvas canvas, int width, int height, PlayerProgression stats, ForestHud hud) {
        if (!open) return;
        float left = width * .12f, top = height * .08f;
        float panelWidth = width * .76f, panelHeight = height * .84f;

        // Fundo escurecido semi-transparente para modal
        canvas.drawColor(0x99000000);

        // Painel principal
        RectF panelRect = new RectF(left, top, left + panelWidth, top + panelHeight);
        if (hud != null) hud.drawPanel(canvas, panelRect.left, panelRect.top, panelRect.right, panelRect.bottom);
        else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xF013211E);
            canvas.drawRoundRect(panelRect, 18, 18, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0x88D3B977);
            paint.setStrokeWidth(3f);
            canvas.drawRoundRect(panelRect, 18, 18, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // Título "STATUS & ATRIBUTOS"
        paint.setColor(0xFFD3B977);
        float titleSize = Math.max(18, Math.min(width, height) * .042f);
        paint.setTextSize(titleSize);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(true);
        canvas.drawText("STATUS & ATRIBUTOS", left + 20, top + 36, paint);

        // Botão visual [X] no topo direito
        float closeBtnLeft = left + panelWidth - 48;
        float closeBtnTop = top + 10;
        float closeBtnRight = left + panelWidth - 12;
        float closeBtnBottom = top + 46;
        paint.setColor(0xFF8B2500);
        canvas.drawRoundRect(new RectF(closeBtnLeft, closeBtnTop, closeBtnRight, closeBtnBottom), 8, 8, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(22f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("✕", (closeBtnLeft + closeBtnRight) * 0.5f, closeBtnTop + 26, paint);

        // Subtítulo com nível, xp e pontos disponíveis
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
        paint.setColor(0xFFE2D6BE);
        paint.setTextSize(Math.max(13, Math.min(width, height) * .028f));
        canvas.drawText("Nivel " + stats.level + "  •  XP " + stats.experience + "  •  Pontos Disponiveis: " + stats.attributePoints,
            left + 20, top + panelHeight * .19f, paint);

        // Lista de atributos com botão [+]
        float rowTop = top + panelHeight * .28f;
        float rowHeight = panelHeight * .075f;
        int[] values = {stats.vigor, stats.mente, stats.resistencia, stats.inteligencia,
            stats.destreza, stats.arcano, stats.forca};
        for (int i = 0; i < KEYS.length; i++) {
            float y = rowTop + i * rowHeight + rowHeight * .62f;
            paint.setColor(0xFFF3EAD6);
            paint.setTextSize(Math.max(14, Math.min(width, height) * .030f));
            canvas.drawText(LABELS[i] + ": " + values[i], left + 20, y, paint);

            // Botão [+]
            float plusLeft = left + panelWidth * .75f;
            float plusTop = rowTop + i * rowHeight + 4;
            float plusRight = left + panelWidth * .88f;
            float plusBottom = plusTop + rowHeight - 6;
            paint.setColor(stats.attributePoints > 0 ? 0xFF355E4B : 0xFF2A3631);
            canvas.drawRoundRect(new RectF(plusLeft, plusTop, plusRight, plusBottom), 6, 6, paint);
            paint.setColor(stats.attributePoints > 0 ? 0xFFFFD17C : 0xFF777777);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            canvas.drawText("+", (plusLeft + plusRight) * 0.5f, plusTop + (plusBottom - plusTop) * 0.72f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
        }

        // Botão visual [FECHAR] na parte inferior
        float bottomBtnL = left + panelWidth * 0.22f;
        float bottomBtnT = top + panelHeight - 48;
        float bottomBtnR = left + panelWidth * 0.78f;
        float bottomBtnB = top + panelHeight - 12;
        paint.setColor(0xFF2A4237);
        canvas.drawRoundRect(new RectF(bottomBtnL, bottomBtnT, bottomBtnR, bottomBtnB), 10, 10, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFFFD17C);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(new RectF(bottomBtnL, bottomBtnT, bottomBtnR, bottomBtnB), 10, 10, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(0xFFFFE082);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(Math.max(14, Math.min(width, height) * .030f));
        canvas.drawText("FECHAR PAINEL (✕)", (bottomBtnL + bottomBtnR) * 0.5f, bottomBtnT + 24, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }
}

