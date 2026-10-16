package com.example.amazonMages.ecs;

import java.util.HashMap;
import java.util.Map;

/** Small composition-first entity; IDs are local to a World. */
public final class Entity {
    private final long id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    Entity(long id) { this.id = id; }
    public long id() { return id; }
    public <T extends Component> Entity add(Class<T> type, T component) {
        if (type == null || component == null || !type.isInstance(component))
            throw new IllegalArgumentException("Invalid component");
        components.put(type, component);
        return this;
    }
    public <T extends Component> T get(Class<T> type) {
        return type.cast(components.get(type));
    }
    public boolean has(Class<? extends Component> type) { return components.containsKey(type); }
    public <T extends Component> void remove(Class<T> type) { components.remove(type); }
}
