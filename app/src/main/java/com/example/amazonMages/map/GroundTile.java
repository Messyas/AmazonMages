package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;

class GroundTile extends Tile {
    private final Sprite sprite;

    public GroundTile(SpriteSheet spriteSheet, Rect mapLocationRect) {
        super(mapLocationRect);
        sprite = spriteSheet.getGroundSprite();
    }

    @Override
    public void draw(Canvas canvas) {
        sprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
