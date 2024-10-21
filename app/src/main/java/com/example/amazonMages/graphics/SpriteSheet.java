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
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.sprites, bitmapOptions); // Carrega a sprite sheet
        // Sprite do Personagem modificado
    }

    public Sprite[] getPlayerSpriteArray() {
        Sprite[] spriteArray = new Sprite[3]; // Cria um array de sprites para o jogador
        spriteArray[0] = new Sprite(this, new Rect(0, 0, 64, 64)); // Primeiro sprite
        spriteArray[1] = new Sprite(this, new Rect(64, 0, 2 * 64, 64)); // Segundo sprite
        spriteArray[2] = new Sprite(this, new Rect(2 * 64, 0, 3 * 64, 64)); // Terceiro sprite
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
        return new Sprite(this, new Rect(
                idxCol * SPRITE_WIDTH_PIXELS,
                SPRITE_HEIGHT_PIXELS,
                (idxCol + 1) * SPRITE_WIDTH_PIXELS,
                (1 + 1) * SPRITE_HEIGHT_PIXELS
        ));
    }
}
