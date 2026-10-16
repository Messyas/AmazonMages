package com.example.amazonMages.events;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class GameEventBusTest {
    @Test public void subscribersReceiveEventsInOrder() {
        GameEventBus bus = new GameEventBus();
        List<String> received = new ArrayList<>();
        GameEventBus.Listener first = e -> received.add("first:" + e.id);
        GameEventBus.Listener second = e -> received.add("second:" + e.id);
        bus.subscribe(first);
        bus.subscribe(second);
        bus.publish(new GameEvent("enemy.defeated", 1));
        assertEquals(2, received.size());
        assertEquals("first:enemy.defeated", received.get(0));
        bus.unsubscribe(first);
        bus.publish(new GameEvent("quest.completed", 2));
        assertEquals(3, received.size());
        assertEquals("second:quest.completed", received.get(2));
    }
}
