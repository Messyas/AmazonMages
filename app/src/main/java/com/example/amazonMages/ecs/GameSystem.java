package com.example.amazonMages.ecs;

/** Fixed-tick system contract. Systems execute in explicit registration order. */
public interface GameSystem {
    void update(World world, int ticks);
}
