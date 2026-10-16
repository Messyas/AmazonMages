package com.example.amazonMages.graphics;

/** Immutable frame sequence; frames are advanced by game updates, not draw calls. */
public final class AnimationClip {
    private final Sprite[] frames;
    private final int ticksPerFrame;
    private final boolean looping;

    public AnimationClip(Sprite[] frames, int ticksPerFrame, boolean looping) {
        if (frames == null || frames.length == 0 || ticksPerFrame < 1)
            throw new IllegalArgumentException("Animation requires frames and a positive duration");
        this.frames = frames.clone();
        for (Sprite frame : frames) if (frame == null) throw new IllegalArgumentException("Null frame");
        this.ticksPerFrame = ticksPerFrame;
        this.looping = looping;
    }

    public Sprite frameAt(int ticks) {
        int index = Math.max(0, ticks) / ticksPerFrame;
        index = looping ? index % frames.length : Math.min(index, frames.length - 1);
        return frames[index];
    }
}
