package com.example.amazonMages.map;

import android.graphics.Canvas;
import android.graphics.Rect;
import com.example.amazonMages.graphics.Sprite;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.androidstudio2dgamedevelopment.R;

/** Animated terrain cell using the four-frame acid swamp atlas. */
final class AcidSwampTile extends Tile {
    private final Sprite sprite;

    AcidSwampTile(SpriteSheet sheet, Rect location, int variationSeed) {
        super(location, variationSeed);
        sprite = sheet.tileFrame(R.drawable.tile_acid_swamp, variant(4), 64, 64);
    }

    @Override public void draw(Canvas canvas) {
        sprite.draw(canvas, mapLocationRect.left, mapLocationRect.top);
    }
}
