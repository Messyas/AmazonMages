package com.example.amazonMages.gamepanel;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.androidstudio2dgamedevelopment.R;

/**
 * GameOver is a panel which draws the Game Over screen and an interactive respawn button.
 */
public class GameOver {

    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF buttonBounds = new RectF();

    public GameOver(Context context) {
        titlePaint.setColor(getColor(context, R.color.gameOver));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);

        subtextPaint.setColor(0xFFF5EACD);
        subtextPaint.setTextAlign(Paint.Align.CENTER);

        buttonBgPaint.setColor(0xEE2A1810);
        buttonBgPaint.setStyle(Paint.Style.FILL);

        buttonBorderPaint.setColor(0xFFFFD17C);
        buttonBorderPaint.setStyle(Paint.Style.STROKE);
        buttonBorderPaint.setStrokeWidth(5f);

        buttonTextPaint.setColor(0xFFFFD17C);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setFakeBoldText(true);

        hintPaint.setColor(0xFFC0B89C);
        hintPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas canvas) {
        draw(canvas, canvas.getWidth(), canvas.getHeight());
    }

    /** Draws using the view's logical size (the canvas may be a smaller, scaled render buffer). */
    public void draw(Canvas canvas, float w, float h) {
        // Escurece o fundo suavemente
        canvas.drawColor(0x99000000);

        float cx = w * 0.5f;

        // Título "Game Over"
        String title = "Game Over";
        float titleSize = Math.min(w, h) * 0.12f;
        titlePaint.setTextSize(titleSize);
        float titleY = h * 0.38f;
        canvas.drawText(title, cx, titleY, titlePaint);

        // Subtítulo
        float subtextSize = Math.min(w, h) * 0.038f;
        subtextPaint.setTextSize(subtextSize);
        canvas.drawText("James esgotou suas forcas...", cx, titleY + titleSize * 0.45f, subtextPaint);

        // Botão "RENASCER AQUI"
        float btnWidth = Math.min(w * 0.52f, 360f);
        float btnHeight = Math.max(h * 0.11f, 54f);
        float btnLeft = cx - btnWidth * 0.5f;
        float btnTop = h * 0.52f;
        float btnRight = btnLeft + btnWidth;
        float btnBottom = btnTop + btnHeight;
        buttonBounds.set(btnLeft, btnTop, btnRight, btnBottom);

        float cornerRadius = btnHeight * 0.28f;
        canvas.drawRoundRect(buttonBounds, cornerRadius, cornerRadius, buttonBgPaint);
        canvas.drawRoundRect(buttonBounds, cornerRadius, cornerRadius, buttonBorderPaint);

        // Texto do botão centralizado verticalmente
        float btnTextSize = btnHeight * 0.38f;
        buttonTextPaint.setTextSize(btnTextSize);
        Paint.FontMetrics fm = buttonTextPaint.getFontMetrics();
        float textY = btnTop + (btnHeight * 0.5f) - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText("RENASCER AQUI", cx, textY, buttonTextPaint);

        // Dica logo abaixo do botão
        float hintSize = Math.min(w, h) * 0.026f;
        hintPaint.setTextSize(hintSize);
        canvas.drawText("Toque no botao para continuar de onde parou", cx, btnBottom + hintSize * 1.6f, hintPaint);
    }

    /**
     * Checks if a touch event coordinates intersect with the respawn button.
     */
    public boolean isRespawnPressed(float touchX, float touchY) {
        return buttonBounds.contains(touchX, touchY);
    }

    public RectF getButtonBounds() {
        return new RectF(buttonBounds);
    }
}
