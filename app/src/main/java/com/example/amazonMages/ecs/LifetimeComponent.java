package com.example.amazonMages.ecs;

public final class LifetimeComponent implements Component {
    public static final int PERMANENT = Integer.MAX_VALUE;
    public int remainingTicks;
    int revealTicks;

    public LifetimeComponent(int ticks) {
        this(ticks, 0);
    }

    public LifetimeComponent(int ticks, int revealTicks) {
        if (ticks < 1 || revealTicks < 0) throw new IllegalArgumentException("ticks");
        remainingTicks = ticks;
        this.revealTicks = revealTicks;
    }

    public boolean isPermanent() { return remainingTicks == PERMANENT; }
    public boolean isVisible() { return revealTicks == 0; }
}
