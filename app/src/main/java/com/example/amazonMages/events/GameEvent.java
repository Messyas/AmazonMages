package com.example.amazonMages.events;

/** Typed event envelope for synchronous gameplay notifications. */
public final class GameEvent {
    public final String id;
    public final long entityId;
    public GameEvent(String id, long entityId) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("event id");
        this.id = id;
        this.entityId = entityId;
    }
}
