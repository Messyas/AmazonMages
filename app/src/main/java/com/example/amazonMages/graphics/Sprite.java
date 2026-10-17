package com.example.amazonMages.graphics;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;

public class Sprite {

    private static final Paint PIXEL_PAINT = new Paint();
    static {
        PIXEL_PAINT.setAntiAlias(false);
        PIXEL_PAINT.setFilterBitmap(false);
        PIXEL_PAINT.setDither(false);
    }
    private final Bitmap bitmap;
    private final Rect rect;
    private final Rect destinationRect = new Rect(); // Retângulo que define a posição e o tamanho do sprite na sprite sheet

    public Sprite(SpriteSheet spriteSheet, Rect rect) {
        this(spriteSheet.getBitmap(), rect);
    }

    public Sprite(Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null || rect.isEmpty() || rect.left < 0 || rect.top < 0 ||
                rect.right > bitmap.getWidth() || rect.bottom > bitmap.getHeight())
            throw new IllegalArgumentException("Invalid sprite bitmap bounds");
        this.bitmap = bitmap;
        this.rect = rect; // Inicializa o retângulo que define a área do sprite
    }

    // Método que desenha o sprite no canvas em uma posição específica (x, y)
    public void draw(Canvas canvas, int x, int y) {
        destinationRect.set(x, y, x + getWidth(), y + getHeight());
        canvas.drawBitmap(
                bitmap,
                rect, // A área do sprite na sprite sheet
                destinationRect, // Retângulo de destino onde o sprite será desenhado
                PIXEL_PAINT
        );
    }

    // Retorna a largura do sprite
    public int getWidth() {
        return rect.width(); // Largura do retângulo
    }

    // Retorna a altura do sprite
    public int getHeight() {
        return rect.height(); // Altura do retângulo
    }
}
