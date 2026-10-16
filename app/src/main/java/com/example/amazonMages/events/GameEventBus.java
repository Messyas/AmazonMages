package com.example.amazonMages.events;

import java.util.ArrayList;
import java.util.List;

/** Lightweight synchronous bus. Subscribe/unsubscribe on game thread. */
public final class GameEventBus {
    public interface Listener { void onEvent(GameEvent event); }
    private Listener[] subscribed = new Listener[0];
    private final List<Listener> listeners = new ArrayList<>();
    public void subscribe(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener");
        if (!listeners.contains(listener)) listeners.add(listener);
        subscribed=listeners.toArray(new Listener[0]);
    }
    public void unsubscribe(Listener listener) { listeners.remove(listener); subscribed=listeners.toArray(new Listener[0]); }
    public void publish(GameEvent event) {
        for (Listener listener : subscribed) listener.onEvent(event);
    }
}
