package com.example.amazonMages.ecs;

import java.util.ArrayList;
import java.util.List;

/** No threads, reflection or per-frame discovery; game thread owns the scheduler. */
public final class SystemScheduler {
    private final List<GameSystem> systems = new ArrayList<>();
    public void add(GameSystem system) {
        if (system == null || systems.contains(system)) throw new IllegalArgumentException("system");
        systems.add(system);
    }
    public void remove(GameSystem system) { systems.remove(system); }
    public void update(World world, int ticks) {
        if (ticks < 1) throw new IllegalArgumentException("ticks");
        for (GameSystem system : new ArrayList<>(systems)) system.update(world, ticks);
    }
}
