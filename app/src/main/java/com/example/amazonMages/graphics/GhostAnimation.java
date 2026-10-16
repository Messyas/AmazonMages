package com.example.amazonMages.graphics;

/** Named animation clips. Each PNG atlas has eight rows (Direction order) and N frame columns. */
public enum GhostAnimation {
    IDLE("idle", 4, 12, true),
    MOVE("move", 6, 7, true),
    ALERT("alert", 4, 9, false),
    SPOTTED_PLAYER("spotted_player", 4, 9, false),
    STARTLED("startled", 4, 8, false),
    SPOTTED_ALLY("spotted_ally", 4, 10, false),
    SOCIAL("social", 4, 10, true),
    FLEE("flee", 6, 6, true),
    CHASE("chase", 6, 7, true),
    ATTACK("attack", 6, 7, false),
    HURT("hurt", 4, 7, false),
    ENRAGED("enraged", 4, 8, true),
    FUSION("fusion", 8, 8, false),
    DEATH("death", 8, 8, false);

    public final String fileSuffix;
    public final int frames;
    public final int ticksPerFrame;
    public final boolean loops;

    GhostAnimation(String suffix, int frames, int ticksPerFrame, boolean loops) {
        this.fileSuffix = suffix;
        this.frames = frames;
        this.ticksPerFrame = ticksPerFrame;
        this.loops = loops;
    }

    public int durationTicks() { return frames * ticksPerFrame; }
}
