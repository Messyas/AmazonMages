package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

class GrassTile extends Tile {
    private final Sprite sprite;
    private final Sprite detail;
    private final Sprite prop;

    public GrassTile(SpriteSheet spriteSheet, Rect mapLocationRect, int variationSeed) {
        super(mapLocationRect, variationSeed);
        sprite = spriteSheet.tileFrame(R.drawable.tile_grass, variant(4), 64, 64);
        int seed = variant(9);
        detail = seed == 0 ? spriteSheet.tileFrame(R.drawable.tile_grass, 4 + variant(4), 64, 64) : null;
        int objectRoll = variant(101);
        if (objectRoll == 0 || objectRoll == 1 || objectRoll == 2) {
            prop = spriteSheet.tileFrame(R.drawable.obj_bush, variant(4), 64, 64);
        } else if (objectRoll >= 5 && objectRoll <= 8) {
            prop = spriteSheet.tileFrame(R.drawable.obj_flowers, variant(6), 64, 64);
        } else if (objectRoll == 12 || objectRoll == 13) {
            prop = spriteSheet.tileFrame(R.drawable.obj_rock, variant(4), 64, 64);
        } else {
            prop = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        sprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
        if (detail != null) detail.draw(canvas, mapLocationRect.left, mapLocationRect.top);
        if (prop != null) prop.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
