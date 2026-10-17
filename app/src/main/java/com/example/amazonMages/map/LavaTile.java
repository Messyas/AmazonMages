package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

class LavaTile extends Tile {
    private final Sprite sprite;

    public LavaTile(SpriteSheet spriteSheet, Rect mapLocationRect, int variationSeed) {
        super(mapLocationRect, variationSeed);
        sprite = spriteSheet.tileFrame(R.drawable.tile_lava, variant(4), 64, 64);
    }

    @Override
    public void draw(Canvas canvas) {
        sprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
