package com.example.amazonMages.gamepanel;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.amazonMages.GameLoop;
import com.example.androidstudio2dgamedevelopment.R;

public class Performance {
    private final GameLoop gameLoop; // Instância do loop de jogo para acessar informações de desempenho
    private final Context context; // Contexto da aplicação

    // Construtor que inicializa o contexto e o GameLoop
    public Performance(Context context, GameLoop gameLoop) {
        this.context = context;
        this.gameLoop = gameLoop; // Associa o GameLoop à classe Performance
    }

    // Método para desenhar informações de desempenho no canvas
    public void draw(Canvas canvas) {
        drawUPS(canvas); // Desenha o UPS (Updates Per Second)
        drawFPS(canvas); // Desenha o FPS (Frames Per Second)
    }

    // Método para desenhar o UPS no canvas
    public void drawUPS(Canvas canvas) {
        String averageUPS = Double.toString(gameLoop.getAverageUPS()); // Obtém a média de UPS
        Paint paint = new Paint(); // Cria um objeto Paint para personalizar a aparência do texto
        int color = getColor(context, R.color.magenta); // Obtém a cor magenta do recursos
        paint.setColor(color); // Define a cor do texto
        paint.setTextSize(50); // Define o tamanho da fonte
        // Desenha o texto "UPS: " seguido do valor médio de UPS na posição (100, 100) do canvas
        canvas.drawText("UPS: " + averageUPS, 100, 100, paint);
    }

    // Método para desenhar o FPS no canvas
    public void drawFPS(Canvas canvas) {
        String averageFPS = Double.toString(gameLoop.getAverageFPS()); // Obtém a média de FPS
        Paint paint = new Paint(); // Cria um objeto Paint para personalizar a aparência do texto
        int color = getColor(context, R.color.magenta); // Obtém a cor magenta do recursos
        paint.setColor(color); // Define a cor do texto
        paint.setTextSize(50); // Define o tamanho da fonte
        // Desenha o texto "FPS: " seguido do valor médio de FPS na posição (100, 200) do canvas
        canvas.drawText("FPS: " + averageFPS, 100, 200, paint);
    }
}