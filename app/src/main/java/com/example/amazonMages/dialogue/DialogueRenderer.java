package com.example.amazonMages.dialogue;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.gamepanel.ForestHud;

/** Canvas-only dialogue card with portrait placeholder and automatic word wrapping. */
public final class DialogueRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final ForestHud hud;

    public DialogueRenderer(ForestHud hud) { this.hud = hud; }

    public void draw(Canvas canvas, DialogueManager dialogue, int width, int height, float density) {
        if (!dialogue.isActive()) return;
        DialogueLine line = dialogue.currentLine();
        float scale = Math.max(0.8f, density);
        float margin = 14 * scale;
        float cardHeight = Math.min(height * 0.42f, 190 * scale);
        float top = height - cardHeight - margin + (1f - dialogue.openingProgress()) * 45 * scale;
        int alpha = Math.min(255, Math.round(255 * dialogue.openingProgress()));
        panel.set(margin, top, width - margin, height - margin);
        if (hud != null) hud.drawPanel(canvas, panel.left, panel.top, panel.right, panel.bottom, alpha);
        else {
            paint.setColor(Color.argb(alpha * 225 / 255, 19, 33, 30));
            canvas.drawRoundRect(panel, 15 * scale, 15 * scale, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * scale);
            paint.setColor(Color.argb(alpha, 174, 147, 91));
            canvas.drawRoundRect(panel, 15 * scale, 15 * scale, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        float portraitSize = Math.min(cardHeight * 0.58f, 84 * scale);
        float portraitX = margin + 12 * scale;
        float portraitY = top + 22 * scale;
        paint.setColor(Color.argb(alpha, 66, 89, 74));
        canvas.drawRoundRect(new RectF(portraitX, portraitY, portraitX + portraitSize,
            portraitY + portraitSize), 8 * scale, 8 * scale, paint);
        paint.setColor(Color.argb(alpha, 225, 213, 182));
        paint.setTextSize(12 * scale);
        String symbol = "player".equals(line.speakerId) ? "PLAYER" : "NPC";
        canvas.drawText(symbol, portraitX + 8 * scale, portraitY + portraitSize / 2, paint);

        float textX = portraitX + portraitSize + 14 * scale;
        float right = width - margin - 14 * scale;
        paint.setColor(Color.argb(alpha, 231, 193, 113));
        paint.setTextSize(16 * scale);
        paint.setFakeBoldText(true);
        canvas.drawText("player".equals(line.speakerId) ? "Mago" : "Anciao da Floresta",
            textX, top + 30 * scale, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(15 * scale);
        paint.setColor(Color.argb(alpha, 245, 242, 230));
        String text = line.text.substring(0, Math.min(dialogue.visibleCharacters(), line.text.length()));
        float y = top + 57 * scale;
        float lineHeight = 20 * scale;
        float maxY = height - margin - 27 * scale;
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            String candidate = row.length() == 0 ? word : row + " " + word;
            if (paint.measureText(candidate) > right - textX && row.length() > 0) {
                if (y > maxY) break;
                canvas.drawText(row.toString(), textX, y, paint);
                y += lineHeight;
                row.setLength(0);
                row.append(word);
            } else {
                row.setLength(0);
                row.append(candidate);
            }
        }
        if (row.length() > 0 && y <= maxY) canvas.drawText(row.toString(), textX, y, paint);
        paint.setTextSize(12 * scale);
        paint.setColor(Color.argb(alpha, 211, 202, 172));
        canvas.drawText(dialogue.visibleCharacters() < line.text.length() ? "Toque para revelar" : "Toque para continuar",
            textX, height - margin - 9 * scale, paint);
    }
}
