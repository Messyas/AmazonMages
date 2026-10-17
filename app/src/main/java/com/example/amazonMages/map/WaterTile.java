package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;

class WaterTile extends Tile {
    private final Sprite sprite;

    public WaterTile(SpriteSheet spriteSheet, Rect mapLocationRect) {
        this(spriteSheet, mapLocationRect, 0, 0);
    }

    public WaterTile(SpriteSheet spriteSheet, Rect mapLocationRect, int atlasColumn, int variationSeed) {
        super(mapLocationRect, variationSeed);
        sprite = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.tile_water,
            atlasColumn, 64, 64);
    }

    @Override
    public void draw(Canvas canvas) {
        sprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
