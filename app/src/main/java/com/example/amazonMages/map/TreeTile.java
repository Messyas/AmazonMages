package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;

class TreeTile extends Tile {
    private final Sprite grassSprite;

    public TreeTile(SpriteSheet spriteSheet, Rect mapLocationRect, int variationSeed) {
        super(mapLocationRect, variationSeed);
        grassSprite = spriteSheet.tileFrame(com.example.androidstudio2dgamedevelopment.R.drawable.tile_grass,
            variant(4), 64, 64);
    }

    @Override
    public void draw(Canvas canvas) {
        grassSprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
