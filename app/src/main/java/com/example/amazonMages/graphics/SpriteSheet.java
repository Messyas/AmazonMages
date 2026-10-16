package com.example.amazonMages.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.example.androidstudio2dgamedevelopment.R;

public class SpriteSheet {
    private static final int SPRITE_WIDTH_PIXELS = 64; // Largura de cada sprite em pixels
    private static final int SPRITE_HEIGHT_PIXELS = 64; // Altura de cada sprite em pixels
    private final Bitmap bitmap; // Bitmap que contém a sprite sheet

    public SpriteSheet(Context context) {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inScaled = false; // Desativa a escala automática da imagem
        bitmap = com.example.amazonMages.resources.ResourceManager.shared(context).bitmap(R.drawable.sprites, false); // Carrega a sprite sheet
        // Sprite do Personagem modificado
    }

    /** Extract any grid-aligned frame without hardcoded pixel rectangles. */
    public Sprite frame(int column, int row) {
        return frame(column, row, SPRITE_WIDTH_PIXELS, SPRITE_HEIGHT_PIXELS);
    }

    public Sprite frame(int column, int row, int width, int height) {
        if (column < 0 || row < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid sprite grid coordinates");
        int left = column * width;
        int top = row * height;
        if (left + width > bitmap.getWidth() || top + height > bitmap.getHeight())
            throw new IllegalArgumentException("Sprite frame outside sheet");
        return new Sprite(this, new Rect(left, top, left + width, top + height));
    }

    public Sprite[] frames(int startColumn, int row, int count) {
        if (count <= 0) throw new IllegalArgumentException("Empty animation");
        Sprite[] result = new Sprite[count];
        for (int i = 0; i < count; i++) result[i] = frame(startColumn + i, row);
        return result;
    }

    /** Legacy three-frame player, reused for all facings until directional art is added. */
    public AnimationSet getPlayerAnimations() {
        Sprite[] frames = frames(0, 0, 3);
        return new AnimationSet()
            .putFallback(AnimationAction.IDLE, new AnimationClip(new Sprite[]{frames[0]}, 1, true))
            .putFallback(AnimationAction.MOVE, new AnimationClip(new Sprite[]{frames[1], frames[2]}, 5, true));
    }

    public Sprite[] getPlayerSpriteArray() {
        Sprite[] spriteArray = new Sprite[3]; // Cria um array de sprites para o jogador
        spriteArray[0] = frame(0, 0); // Primeiro sprite
        spriteArray[1] = frame(1, 0); // Segundo sprite
        spriteArray[2] = frame(2, 0); // Terceiro sprite
        return spriteArray; // Retorna o array de sprites do jogador
    }

    public Bitmap getBitmap() {
        return bitmap; // Retorna o bitmap da sprite sheet
    }

    public Sprite getWaterSprite() {
        return getSpriteByIndex(0); // Retorna o sprite da água
    }

    public Sprite getLavaSprite() {
        return getSpriteByIndex(1); // Retorna o sprite da lava
    }

    public Sprite getGroundSprite() {
        return getSpriteByIndex(2); // Retorna o sprite do chão
    }

    public Sprite getGrassSprite() {
        return getSpriteByIndex(3); // Retorna o sprite da grama
    }

    public Sprite getTreeSprite() {
        return getSpriteByIndex(4); // Retorna o sprite da árvore
    }

     // isso aqui é interessante, tenta entender depois
    private Sprite getSpriteByIndex(int idxCol) {
        // Retorna um sprite baseado no índice da coluna fornecido
        return frame(idxCol, 1);
    }
}
