package com.example.amazonMages.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;

public class Sprite {

    private final SpriteSheet spriteSheet; // Referência à sprite sheet que contém este sprite
    private final Rect rect; // Retângulo que define a posição e o tamanho do sprite na sprite sheet

    public Sprite(SpriteSheet spriteSheet, Rect rect) {
        this.spriteSheet = spriteSheet; // Inicializa a sprite sheet
        this.rect = rect; // Inicializa o retângulo que define a área do sprite
    }

    // Método que desenha o sprite no canvas em uma posição específica (x, y)
    public void draw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(
                spriteSheet.getBitmap(), // Bitmap da sprite sheet
                rect, // A área do sprite na sprite sheet
                new Rect(x, y, x + getWidth(), y + getHeight()), // Retângulo de destino onde o sprite será desenhado
                null // Opções de pintura (null indica que não há opções especiais)
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
