package com.example.amazonMages.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic insertion-order registry; snapshot queries avoid concurrent modification. */
public final class World {
    private final Map<Long, Entity> entities = new LinkedHashMap<>();
    private long nextId = 1;
    public Entity create() {
        Entity entity = new Entity(nextId++);
        entities.put(entity.id(), entity);
        return entity;
    }
    public boolean destroy(long id) { return entities.remove(id) != null; }
    public Entity find(long id) { return entities.get(id); }
    public int size() { return entities.size(); }
    @SafeVarargs
    public final List<Entity> with(Class<? extends Component>... types) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            boolean matches = true;
            for (Class<? extends Component> type : types)
                if (!entity.has(type)) { matches = false; break; }
            if (matches) result.add(entity);
        }
        return Collections.unmodifiableList(result);
    }
}
