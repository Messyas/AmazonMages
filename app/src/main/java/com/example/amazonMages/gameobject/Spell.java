package com.example.amazonMages.gameobject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import androidx.core.content.ContextCompat;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.GameLoop;
import com.example.androidstudio2dgamedevelopment.R;

public class Spell extends Circle {
    public static final double SPEED_PIXELS_PER_SECOND = 800.0;
    private static final double MAX_SPEED = SPEED_PIXELS_PER_SECOND / GameLoop.MAX_UPS;
    private final Bitmap spellBitmap;

    public Spell(Context context, Player spellcaster) {
        super(
                ContextCompat.getColor(context, R.color.spell),
                spellcaster.getPositionX(),
                spellcaster.getPositionY(),
                25
        );

        // Carrega a imagem do feitiço
        spellBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.energyball); //mudar

        // Define a direção do feitiço com base na posição do jogador
        velocityX = spellcaster.getDirectionX() * MAX_SPEED;
        velocityY = spellcaster.getDirectionY() * MAX_SPEED;
    }

    @Override
    public void update() {
        positionX += velocityX;
        positionY += velocityY;
    }

    @Override
    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        // Converte as coordenadas para o display
        float displayX = (float) gameDisplay.gameToDisplayCoordinatesX(positionX);
        float displayY = (float) gameDisplay.gameToDisplayCoordinatesY(positionY);

        // Desenha o Bitmap do feitiço centralizado
        if (spellBitmap != null) {
            canvas.drawBitmap(
                    spellBitmap,
                    displayX - (float) spellBitmap.getWidth() / 2, // Centraliza o bitmap no eixo X
                    displayY - (float) spellBitmap.getHeight() / 2, // Centraliza o bitmap no eixo Y
                    null
            );
        }
    }
}
