package com.example.amazonMages.graphics;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Player;

public class Animator {
    private final Sprite[] playerSpriteArray; // Array de sprites do jogador para animações
    private int idxMovingFrame = 1; // Índice do quadro atual da animação de movimento
    private int updatesBeforeNextMoveFrame; // Contador para determinar quando mudar o quadro
    private static final int MAX_UPDATES_BEFORE_NEXT_MOVE_FRAME = 5; // Número máximo de atualizações antes de mudar o quadro

    public Animator(Sprite[] playerSpriteArray) {
        this.playerSpriteArray = playerSpriteArray; // Inicializa o array de sprites
    }

    // Método responsável por desenhar o jogador no canvas, considerando o estado atual
    public void draw(Canvas canvas, GameDisplay gameDisplay, @NonNull Player player) {
        int idxNotMovingFrame = 0; // Índice do quadro quando o jogador não está se movendo
        switch (player.getPlayerState().getState()) {
            case NOT_MOVING: // Caso o jogador não esteja se movendo
                drawFrame(canvas, gameDisplay, player, playerSpriteArray[idxNotMovingFrame]);
                break;
            case STARED_MOVING: // Caso o jogador tenha começado a se mover
                updatesBeforeNextMoveFrame = MAX_UPDATES_BEFORE_NEXT_MOVE_FRAME; // Reinicia o contador
                drawFrame(canvas, gameDisplay, player, playerSpriteArray[idxMovingFrame]);
                break;
            case IS_MOVING: // Caso o jogador esteja em movimento
                updatesBeforeNextMoveFrame--; // Decrementa o contador
                if(updatesBeforeNextMoveFrame == 0) { // Se o contador atingir 0, muda o quadro
                    updatesBeforeNextMoveFrame = MAX_UPDATES_BEFORE_NEXT_MOVE_FRAME; // Reinicia o contador
                    toggleIdxMovingFrame(); // Alterna entre os quadros de movimento
                }
                drawFrame(canvas, gameDisplay, player, playerSpriteArray[idxMovingFrame]);
                break;
            default:
                break; // Caso o estado não se encaixe nas opções anteriores
        }
    }

    // Alterna entre os quadros de animação de movimento
    private void toggleIdxMovingFrame() {
        if(idxMovingFrame == 1)
            idxMovingFrame = 2; // Se está no quadro 1, vai para o quadro 2
        else
            idxMovingFrame = 1; // Se está no quadro 2, volta para o quadro 1
    }

    // Método para desenhar um quadro específico no canvas
    public void drawFrame(Canvas canvas, GameDisplay gameDisplay, Player player, Sprite sprite) {
        sprite.draw(
                canvas,
                (int) gameDisplay.gameToDisplayCoordinatesX(player.getPositionX()) - sprite.getWidth()/2, // Centraliza o sprite no eixo X
                (int) gameDisplay.gameToDisplayCoordinatesY(player.getPositionY()) - sprite.getHeight()/2 // Centraliza o sprite no eixo Y
        );
    }
}
