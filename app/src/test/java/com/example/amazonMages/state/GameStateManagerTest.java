package com.example.amazonMages.state;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.amazonMages.state.GameStateManager.State.*;
public class GameStateManagerTest {
    @Test public void lifecyclePreservesModalWithoutAllowingGameplay() {
        GameStateManager states=new GameStateManager(); assertEquals(MENU,states.state());
        states.loading(); states.resume(); assertFalse(states.acceptsInput());
        states.loaded(); assertTrue(states.updatesWorld());
        states.modal(INVENTORY); assertFalse(states.updatesWorld()); assertTrue(states.acceptsInput());
        states.pause(); assertEquals(PAUSED,states.state()); assertFalse(states.acceptsInput());
        states.resume(); assertEquals(INVENTORY,states.state());
        states.modal(PLAYING); states.gameOver(); assertFalse(states.acceptsInput());
        states.pause(); states.resume(); assertEquals(GAME_OVER,states.state());
        states.revive();assertTrue(states.updatesWorld());
        states.loading(); states.loaded(); states.resume(); assertTrue(states.updatesWorld());
    }
}
