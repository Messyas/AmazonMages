package com.example.amazonMages.state;

/** Activity lifecycle overlays a preserved modal scene; loading and death always block gameplay. */
public final class GameStateManager {
    public enum State { MENU, LOADING, PLAYING, PAUSED, DIALOGUE, INVENTORY, ATTRIBUTES, GAME_OVER }
    private boolean menu=true, loaded, resumed, dead;
    private State modal=State.PLAYING;
    public synchronized void menu() { menu=true; resumed=false; }
    public synchronized void loading() { menu=false; loaded=false; resumed=false; dead=false; modal=State.PLAYING; }
    public synchronized void loaded() { loaded=true; }
    public synchronized void resume() { resumed=true; }
    public synchronized void pause() { resumed=false; }
    public synchronized void gameOver() { dead=true; }
    public synchronized void modal(State state) {
        if (state!=State.PLAYING && state!=State.DIALOGUE && state!=State.INVENTORY && state!=State.ATTRIBUTES)
            throw new IllegalArgumentException("modal");
        modal=state;
    }
    public synchronized State state() {
        if (menu) return State.MENU;
        if (!loaded) return State.LOADING;
        if (dead) return State.GAME_OVER;
        if (!resumed) return State.PAUSED;
        return modal;
    }
    public synchronized boolean updatesWorld() { return state()==State.PLAYING; }
    public synchronized boolean acceptsInput() { State s=state(); return s==State.PLAYING || s==State.DIALOGUE || s==State.INVENTORY || s==State.ATTRIBUTES; }
}
