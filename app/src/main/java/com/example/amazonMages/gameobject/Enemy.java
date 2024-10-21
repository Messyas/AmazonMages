package com.example.amazonMages.gameobject;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.GameLoop;
import com.example.androidstudio2dgamedevelopment.R;


/**
 * Enemy is a character which always moves in the direction of the player.
 * The Enemy class is an extension of a Circle, which is an extension of a GameObject
 */
public class Enemy extends Circle {

    private static final double SPEED_PIXELS_PER_SECOND = Player.SPEED_PIXELS_PER_SECOND * 0.6;
    private static final double MAX_SPEED = SPEED_PIXELS_PER_SECOND / GameLoop.MAX_UPS;
    private static final double SPAWNS_PER_MINUTE = 20;
    private static final double SPAWNS_PER_SECOND = SPAWNS_PER_MINUTE / 60.0;
    private static final double UPDATES_PER_SPAWN = GameLoop.MAX_UPS / SPAWNS_PER_SECOND;
    private static double updatesUntilNextSpawn = UPDATES_PER_SPAWN;
    private final Player player;
    private final Bitmap enemyBitmap;

    public Enemy(Context context, Player player) {
        super(getColor(context, R.color.enemy), Math.random() * 1000, Math.random() * 1000, 30);
        this.player = player;

        // Carrega a imagem do inimigo
        enemyBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ghost);
    }

    public static boolean readyToSpawn() {
        if (updatesUntilNextSpawn <= 0) {
            updatesUntilNextSpawn += UPDATES_PER_SPAWN;
            return true;
        } else {
            updatesUntilNextSpawn--;
            return false;
        }
    }

    @Override
    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        // Calcula as coordenadas ajustadas para o display
        float displayX = (float) gameDisplay.gameToDisplayCoordinatesX(positionX);
        float displayY = (float) gameDisplay.gameToDisplayCoordinatesY(positionY);

        // Desenha o Bitmap do inimigo centralizado
        if (enemyBitmap != null) {
            canvas.drawBitmap(
                    enemyBitmap,
                    displayX - (float) enemyBitmap.getWidth() / 2,
                    displayY - (float) enemyBitmap.getHeight() / 2,
                    null
            );
        }
    }


    public void update() {
        // =========================================================================================
        //   Update velocity of the enemy so that the velocity is in the direction of the player
        // =========================================================================================
        // Calculate vector from enemy to player (in x and y)
        double distanceToPlayerX = player.getPositionX() - positionX;
        double distanceToPlayerY = player.getPositionY() - positionY;

        // Calculate (absolute) distance between enemy (this) and player
        double distanceToPlayer = GameObject.getDistanceBetweenObjects(this, player);

        // Calculate direction from enemy to player
        double directionX = distanceToPlayerX/distanceToPlayer;
        double directionY = distanceToPlayerY/distanceToPlayer;

        // Set velocity in the direction to the player
        if(distanceToPlayer > 0) { // Avoid division by zero
            velocityX = directionX*MAX_SPEED;
            velocityY = directionY*MAX_SPEED;
        } else {
            velocityX = 0;
            velocityY = 0;
        }

        // =========================================================================================
        //   Update position of the enemy
        // =========================================================================================
        positionX += velocityX;
        positionY += velocityY;
    }
}

