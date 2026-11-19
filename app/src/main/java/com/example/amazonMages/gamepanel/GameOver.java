package com.example.amazonMages.gamepanel;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.ui.PixelUiKit;
import com.example.androidstudio2dgamedevelopment.R;

/** Pixel-art Game Over screen with interactive respawn button. */
public class GameOver {
    private final PixelUiKit ui;
    private final int titleColor;
    private final RectF panelBounds = new RectF();
    private final RectF buttonBounds = new RectF();

    public GameOver(Context context) {
        ui = new PixelUiKit(context);
        titleColor = getColor(context, R.color.gameOver);
    }

    public void draw(Canvas canvas) {
        draw(canvas, canvas.getWidth(), canvas.getHeight());
    }

    public void draw(Canvas canvas, float w, float h) {
        canvas.drawColor(0xA406100A);

        float cx = w * 0.5f;
        float pw = Math.min(w * .70f, 480f);
        float ph = Math.min(h * .58f, 300f);
        panelBounds.set(cx - pw * .5f, h * .5f - ph * .5f, cx + pw * .5f, h * .5f + ph * .5f);
        ui.drawPanel(canvas, panelBounds);

        float titleSize = Math.min(w, h) * 0.095f;
        float titleY = panelBounds.top + ph * 0.34f;
        ui.drawTextShadow(canvas, "GAME OVER", cx, titleY, titleSize, titleColor, true, Paint.Align.CENTER);

        float subSize = Math.min(w, h) * 0.036f;
        ui.drawTextShadow(canvas, "James esgotou suas forcas...", cx, titleY + titleSize * 0.45f,
            subSize, 0xFFF4EBC8, false, Paint.Align.CENTER);

        float btnWidth = Math.min(pw * 0.88f, 380f);
        float btnHeight = Math.max(ph * 0.22f, 48f);
        buttonBounds.set(cx - btnWidth * 0.5f, panelBounds.top + ph * 0.54f,
            cx + btnWidth * 0.5f, panelBounds.top + ph * 0.54f + btnHeight);
        ui.drawButton(canvas, buttonBounds, "RENASCER NO SANTUARIO", 0, 0xFFFFF1B0);

        float hintSize = Math.min(w, h) * 0.025f;
        ui.drawTextShadow(canvas, "A aura de folha ancestral reconstrói suas forças no centro",
            cx, buttonBounds.bottom + hintSize * 1.45f, hintSize, 0xFFB8C99A, false, Paint.Align.CENTER);
    }

    public boolean isRespawnPressed(float touchX, float touchY) {
        return buttonBounds.contains(touchX, touchY);
    }
}
