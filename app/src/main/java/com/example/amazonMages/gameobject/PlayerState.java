package com.example.amazonMages.gameobject;

public class PlayerState {

    // Enumeration que define os possiveis estados de movimentos do personsagens, ou seja aqui voce modifica os sprites que vao ser usados.
    public enum State {
        NOT_MOVING,
        STARED_MOVING,
        IS_MOVING
    }

    private final Player player;
    private State state;

    public PlayerState(Player player) {
        this.player = player;
        this.state = State.NOT_MOVING;
    }

    public State getState() {
        return state;
    }

    // Pra a animacao ficar mais completa tem que mudar essa logica, a ideia é que seja correspondente aos estados das animacoes daquela sprite
    public void update() {
        switch (state) {
            case NOT_MOVING:
                if (player.velocityX != 0 || player.velocityY != 0)
                    state = State.STARED_MOVING;
                break;
            case STARED_MOVING:
                if (player.velocityX != 0 || player.velocityY != 0)
                    state = State.IS_MOVING;
                break;
            case IS_MOVING:
                if (player.velocityX == 0 && player.velocityY == 0)
                    state = State.NOT_MOVING;
                break;
            default:
                break;
        }
    }
}
