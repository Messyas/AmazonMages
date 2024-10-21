package com.example.amazonMages;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * GameLoop é responsável por controlar o ciclo principal do jogo, garantindo que as atualizações
 * e renderizações ocorram em uma taxa consistente de atualizações por segundo (UPS) e quadros por segundo (FPS).
 */

public class GameLoop extends Thread{

    // Define o número máximo de atualizações por segundo (UPS)
    public static final double MAX_UPS = 60.0;
    // Define o período de atualização em milissegundos (UPS_PERIOD = 1000ms / MAX_UPS)
    private static final double UPS_PERIOD = 1E+3/MAX_UPS;

    // Referência ao jogo e ao SurfaceHolder, que controla o acesso à superfície de desenho
    private final Game game;
    private final SurfaceHolder surfaceHolder;

    // Variáveis de controle de loop
    private boolean isRunning = false;  // Indica se o jogo está rodando
    private double averageUPS;          // Média de atualizações por segundo
    private double averageFPS;          // Média de quadros por segundo

    /**
     * Construtor da classe GameLoop.
     *
     * @param game Referência ao objeto Game para atualizações e renderizações.
     * @param surfaceHolder O SurfaceHolder que controla a superfície de desenho do jogo.
     */
    public GameLoop(Game game, SurfaceHolder surfaceHolder) {
        this.game = game;
        this.surfaceHolder = surfaceHolder;
    }

    /**
     * Retorna a média de atualizações por segundo.
     *
     * @return averageUPS A média de atualizações por segundo.
     */
    public double getAverageUPS() {
        return averageUPS;
    }

    /**
     * Retorna a média de quadros por segundo.
     *
     * @return averageFPS A média de quadros por segundo.
     */
    public double getAverageFPS() {
        return averageFPS;
    }

    /**
     * Inicia o loop do jogo e registra no log.
     */
    public void startLoop() {
        Log.d("GameLoop.java", "startLoop()");
        isRunning = true;
        start();
    }

    /**
     * Método principal da thread, responsável pelo ciclo de jogo.
     */
    @Override
    public void run() {
        Log.d("GameLoop.java", "run()");
        super.run();

        // Variáveis de controle do tempo e contagem de ciclos
        int updateCount = 0;  // Contador de atualizações
        int frameCount = 0;   // Contador de quadros

        long startTime;   // Tempo de início do ciclo
        long elapsedTime; // Tempo decorrido desde o início do ciclo
        long sleepTime;   // Tempo de pausa para limitar a taxa de UPS

        // Loop do jogo
        Canvas canvas = null;   // Canvas para desenhar o jogo
        startTime = System.currentTimeMillis();  // Marca o início do loop
        while(isRunning) {

            // Tenta atualizar e renderizar o jogo
            try {
                canvas = surfaceHolder.lockCanvas(); // Bloqueia a superfície para desenho
                synchronized (surfaceHolder) {
                    game.update();   // Atualiza a lógica do jogo
                    updateCount++;   // Incrementa o contador de atualizações

                    game.draw(canvas);  // Renderiza o jogo na tela
                    frameCount++;       // Incrementa o contador de quadros
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();  // Trata exceções relacionadas a argumentos inválidos
            } finally {
                // Desbloqueia a superfície e envia o canvas para renderizar na tela
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();  // Trata exceções relacionadas ao canvas
                    }
                }
            }

            // Pausa o loop do jogo para não exceder a taxa de UPS (atualizações por segundo)
            elapsedTime = System.currentTimeMillis() - startTime;  // Calcula o tempo decorrido
            sleepTime = (long) (updateCount * UPS_PERIOD - elapsedTime);  // Calcula o tempo de pausa
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime);  // Faz a thread dormir pelo tempo necessário
                } catch (InterruptedException e) {
                    e.printStackTrace();  // Trata exceções de interrupção
                }
            }

            // Ignora frames para manter a taxa de atualizações por segundo (UPS) desejada
            while (sleepTime < 0 && updateCount < MAX_UPS - 1) {
                game.update();  // Atualiza o estado do jogo
                updateCount++;  // Incrementa o contador de atualizações
                elapsedTime = System.currentTimeMillis() - startTime;  // Calcula o tempo decorrido
                sleepTime = (long) (updateCount * UPS_PERIOD - elapsedTime);  // Recalcula o tempo de espera
            }

            // Calcula a média de UPS e FPS
            elapsedTime = System.currentTimeMillis() - startTime;  // Tempo total decorrido
            if (elapsedTime >= 1000) {  // Se já se passaram 1000 ms (1 segundo)
                averageUPS = updateCount / (1E-3 * elapsedTime);  // Calcula a média de UPS
                averageFPS = frameCount / (1E-3 * elapsedTime);  // Calcula a média de FPS
                updateCount = 0;  // Reinicia o contador de atualizações
                frameCount = 0;   // Reinicia o contador de quadros
                startTime = System.currentTimeMillis();  // Reinicia o tempo de início
            }
        }
    }

    /**
     * Para o loop do jogo e registra no log.
     */
    public void stopLoop() {
        Log.d("GameLoop.java", "stopLoop()");  // Registra no log que o loop foi parado
        isRunning = false;  // Atualiza a variável de controle para parar o loop
        try {
            join();  // Aguarda o término da thread do loop do jogo
        } catch (InterruptedException e) {
            e.printStackTrace();  // Trata a exceção de interrupção
        }
    }
}
