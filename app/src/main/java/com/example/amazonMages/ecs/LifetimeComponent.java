package com.example.amazonMages.ecs;

public final class LifetimeComponent implements Component {
    public int remainingTicks;
    public LifetimeComponent(int ticks) {
        if (ticks < 1) throw new IllegalArgumentException("ticks");
        remainingTicks = ticks;
    }
}
