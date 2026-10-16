package com.example.amazonMages.graphics;

import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;

/** Animation state machine shared by player and enemies. */
public final class SpriteAnimator {
    private final AnimationSet animations;
    private AnimationAction action = AnimationAction.IDLE;
    private Direction facing = Direction.SOUTH;
    private int ticks;

    public SpriteAnimator(AnimationSet animations) {
        this.animations = animations;
    }

    public void setState(AnimationAction action, Direction facing) {
        if (this.action != action || this.facing != facing) {
            this.action = action;
            this.facing = facing;
            ticks = 0;
        }
    }

    public void update() {
        if (ticks < Integer.MAX_VALUE) ticks++;
    }

    public void draw(Canvas canvas, GameDisplay display, double worldX, double worldY) {
        AnimationClip clip = animations.get(action, facing);
        if (clip == null) return;
        Sprite frame = clip.frameAt(ticks);
        frame.draw(canvas,
            (int) display.gameToDisplayCoordinatesX(worldX) - frame.getWidth() / 2,
            (int) display.gameToDisplayCoordinatesY(worldY) - frame.getHeight() / 2);
    }

    public Direction getFacing() { return facing; }
    public AnimationAction getAction() { return action; }
}
