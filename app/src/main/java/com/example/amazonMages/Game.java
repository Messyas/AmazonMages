package com.example.amazonMages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.amazonMages.gameobject.Circle;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.Spell;
import com.example.amazonMages.gamepanel.GameOver;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.gamepanel.Performance;
import com.example.amazonMages.graphics.Animator;
import com.example.amazonMages.graphics.SpriteSheet;
import com.example.amazonMages.map.Tilemap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A classe Game gerencia todos os objetos do jogo e é responsável por atualizar todos os estados
 * e renderizar todos os objetos na tela.
 */
class Game extends SurfaceView implements SurfaceHolder.Callback {

    private final Tilemap tilemap;
    private int joystickPointerId = 0;
    private final Joystick joystick;
    private final Player player;
    private GameLoop gameLoop;
    private final List<Enemy> enemyList = new ArrayList<>();
    private final List<Spell> spellList = new ArrayList<>();
    private int numberOfSpellsToCast = 0;
    private final GameOver gameOver;
    private final Performance performance;
    private final GameDisplay gameDisplay;

    /**
     * Construtor da classe Game.
     *
     * @param context O contexto da aplicação, que é passado para a Superclasse SurfaceView.
     */
    public Game(Context context) {
        super(context);

        // Get surface holder and add callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        gameLoop = new GameLoop(this, surfaceHolder);

        // Initialize game panels
        performance = new Performance(context, gameLoop);
        gameOver = new GameOver(context);
        joystick = new Joystick(275, 700, 70, 40);

        // Initialize game objects
        SpriteSheet spriteSheet = new SpriteSheet(context);
        Animator animator = new Animator(spriteSheet.getPlayerSpriteArray());
        player = new Player(context, joystick, 2*500, 500, 32, animator);


        // Initialize display and center it around the player
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gameDisplay = new GameDisplay(displayMetrics.widthPixels, displayMetrics.heightPixels, player);

        // Initialize Tilemap
        tilemap = new Tilemap(spriteSheet);

        // Inicia o serviço de música
        Intent musicIntent = new Intent(context, MusicService.class);
        context.startService(musicIntent); // Inicia o serviço de música em segundo plano

        setFocusable(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Lida com ações de eventos de toque do usuário
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (joystick.getIsPressed()) {
                    // O joystick estava pressionado antes deste evento -> lançar feitiço
                    numberOfSpellsToCast++;
                } else if (joystick.isPressed(event.getX(), event.getY())) {
                    // O joystick está pressionado neste evento -> setIsPressed(true) e armazena o id do ponteiro
                    joystickPointerId = event.getPointerId(event.getActionIndex());
                    joystick.setIsPressed(true);
                } else {
                    // O joystick não estava pressionado anteriormente e não está pressionado neste evento -> lançar feitiço
                    numberOfSpellsToCast++;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (joystick.getIsPressed()) {
                    // O joystick foi pressionado anteriormente e agora está sendo movido
                    joystick.setActuator(event.getX(), event.getY());
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (joystickPointerId == event.getPointerId(event.getActionIndex())) {
                    // O ponteiro do joystick foi solto -> setIsPressed(false) e resetActuator()
                    joystick.setIsPressed(false);
                    joystick.resetActuator();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceCreated()");

        // Verifica se o loop do jogo está no estado TERMINATED
        if (gameLoop.getState().equals(Thread.State.TERMINATED)) {
            SurfaceHolder surfaceHolder = getHolder();
            surfaceHolder.addCallback(this); // Adiciona a classe atual como callback
            gameLoop = new GameLoop(this, surfaceHolder); // Cria uma nova instância de GameLoop
        }

        gameLoop.startLoop(); // Inicia o loop do jogo

    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d("Game.java", "surfaceChanged()");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d("Game.java", "surfaceDestroyed()");
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Desenha o Tilemap
        tilemap.draw(canvas, gameDisplay);

        // Desenha os objetos do jogo
        player.draw(canvas, gameDisplay);

        for (Enemy enemy : enemyList) {
            enemy.draw(canvas, gameDisplay);
        }

        for (Spell spell : spellList) {
            spell.draw(canvas, gameDisplay);
        }

        // Desenha os painéis do jogo
        joystick.draw(canvas);
        performance.draw(canvas);

        // Desenha "Game Over" se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            gameOver.draw(canvas);
        }
    }


    public void update() {
        // Para de atualizar o jogo se o jogador estiver morto
        if (player.getHealthPoint() <= 0) {
            return;
        }

        // Atualiza o estado do jogo
        joystick.update();
        player.update();

        // Gera inimigo
        if (Enemy.readyToSpawn()) {
            enemyList.add(new Enemy(getContext(), player));
        }

        // Atualiza o estado de todos os inimigos
        for (Enemy enemy : enemyList) {
            enemy.update();
        }

        // Atualiza o estado de todos os feitiços
        while (numberOfSpellsToCast > 0) {
            spellList.add(new Spell(getContext(), player));
            numberOfSpellsToCast--;
        }
        for (Spell spell : spellList) {
            spell.update();
        }

        // Itera pela lista de inimigos e verifica colisão entre cada inimigo e o jogador
        // e entre os feitiços na lista de feitiços.
        Iterator<Enemy> iteratorEnemy = enemyList.iterator();
        while (iteratorEnemy.hasNext()) {
            Circle enemy = iteratorEnemy.next();
            if (Circle.isColliding(enemy, player)) {
                // Remove o inimigo se colidir com o jogador
                iteratorEnemy.remove();
                player.setHealthPoint(player.getHealthPoint() - 1);
                continue;
            }

            Iterator<Spell> iteratorSpell = spellList.iterator();
            while (iteratorSpell.hasNext()) {
                Circle spell = iteratorSpell.next();
                // Remove o inimigo se colidir com um feitiço
                if (Circle.isColliding(spell, enemy)) {
                    iteratorSpell.remove();
                    iteratorEnemy.remove();
                    break;
                }
            }
        }

        // Atualiza o gameDisplay para que seu centro seja definido para o novo centro das
        // coordenadas do jogador no jogo
        gameDisplay.update();
    }


    public void pause() {
        gameLoop.stopLoop();
    }


}
