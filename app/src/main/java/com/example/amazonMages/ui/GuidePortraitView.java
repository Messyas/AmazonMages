package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;

/** Keeps the stage fixed while switching between the closed and open speaking frames. */
final class GuidePortraitView extends View {
    private static final float WAIST_CROP = .45f;
    private static final float FRAME_SIDE_MARGIN = .08f;
    private final ResourceManager resources;
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private Bitmap portrait;
    private int currentResource, frameWidth, frame;

    GuidePortraitView(Context context) {
        super(context);
        resources = ResourceManager.shared(context);
        setContentDescription("Iara, guia do santuário");
    }

    void pose(String expression, boolean blue, boolean mouthOpen) {
        int resource, frames = 2;
        switch (expression) {
            case "greet": resource = blue ? R.drawable.guide_greet_blue : R.drawable.guide_greet; break;
            case "explain": resource = blue ? R.drawable.guide_explain_blue : R.drawable.guide_explain; break;
            case "warn": resource = blue ? R.drawable.guide_warn_blue : R.drawable.guide_warn; break;
            case "think": resource = blue ? R.drawable.guide_think_blue : R.drawable.guide_think; break;
            default:
                resource = blue ? R.drawable.guide_portrait_blue : R.drawable.guide_portrait;
                frames = 1;
                break;
        }
        int nextFrame = frames == 2 && mouthOpen ? 1 : 0;
        if (currentResource == resource && frame == nextFrame) return;
        if (currentResource != resource) {
            portrait = resources.bitmap(resource, false);
            frameWidth = portrait == null ? 0 : portrait.getWidth() / frames;
            currentResource = resource;
        }
        frame = nextFrame;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (portrait == null || frameWidth == 0) return;
        int margin = Math.round(frameWidth * FRAME_SIDE_MARGIN);
        source.set(frame * frameWidth + margin, 0, (frame + 1) * frameWidth - margin,
            Math.round(portrait.getHeight() * WAIST_CROP));
        float scale = Math.min(getWidth() / (float) source.width(), getHeight() / (float) source.height());
        float w = source.width() * scale, height = source.height() * scale;
        float left = (getWidth() - w) * .25f, top = getHeight() - height;
        target.set(left, top, left + w, top + height);
        canvas.drawBitmap(portrait, source, target, paint);
    }
}
