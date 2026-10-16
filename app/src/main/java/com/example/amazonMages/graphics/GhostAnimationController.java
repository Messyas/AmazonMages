package com.example.amazonMages.graphics;

/** Separates short reactions from locomotion. All durations use game-loop ticks. */
public final class GhostAnimationController {
    private GhostAnimation current = GhostAnimation.IDLE;
    private int ticks;
    private int remaining;

    public void update(int elapsedTicks, GhostAnimation ambient) {
        int elapsed = Math.max(1, elapsedTicks);
        if (remaining > 0) {
            remaining = Math.max(0, remaining - elapsed);
            ticks += elapsed;
            if (remaining > 0) return;
        }
        if (current != ambient) {
            current = ambient;
            ticks = 0;
        } else ticks += elapsed;
    }

    public void react(GhostAnimation reaction) {
        if (reaction.loops) throw new IllegalArgumentException("Reaction must be finite");
        if (current == GhostAnimation.DEATH && remaining > 0) return;
        current = reaction;
        ticks = 0;
        remaining = reaction.durationTicks();
    }

    public boolean isReacting() { return remaining > 0; }
    public GhostAnimation getCurrent() { return current; }
    public int getTicks() { return ticks; }
}
