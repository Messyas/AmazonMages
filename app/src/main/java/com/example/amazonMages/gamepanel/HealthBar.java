package com.example.amazonMages.gamepanel;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.amazonMages.GameDisplay;
import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.gameobject.Player;

/**
 * HealthBar display the players health to the screen
 */
public class HealthBar {
    private final Player player;
    private final Paint borderPaint;
    private final Paint healthPaint;
    private final int width;
    private final int height;
    private final int margin; // pixel value

    public HealthBar(Context context, Player player) {
        this.player = player;
        this.width = 80;
        this.height = 15;
        this.margin = 2;

        this.borderPaint = new Paint();
        int borderColor = getColor(context, R.color.healthBarBorder);
        borderPaint.setColor(borderColor);

        this.healthPaint = new Paint();
        int healthColor = getColor(context, R.color.healthBarHealth);
        healthPaint.setColor(healthColor);
    }

    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        float x = (float) player.getPositionX();
        float y = (float) player.getPositionY();
        float distanceToPlayer = 50;
        float healthPointPercentage = (float) player.getHealthPoint()/ Player.MAX_HEALTH_POINTS;

        // Draw border
        float borderLeft, borderTop, borderRight, borderBottom;
        borderLeft = x - (float) width /2;
        borderRight = x + (float) width /2;
        borderBottom = y - distanceToPlayer;
        borderTop = borderBottom - height;
        canvas.drawRect(
                (float) gameDisplay.gameToDisplayCoordinatesX(borderLeft),
                (float) gameDisplay.gameToDisplayCoordinatesY(borderTop),
                (float) gameDisplay.gameToDisplayCoordinatesX(borderRight),
                (float) gameDisplay.gameToDisplayCoordinatesY(borderBottom),
                borderPaint);

        // Draw health
        float healthLeft, healthTop, healthRight, healthBottom, healthWidth, healthHeight;
        healthWidth = width - 2*margin;
        healthHeight = height - 2*margin;
        healthLeft = borderLeft + margin;
        healthRight = healthLeft + healthWidth*healthPointPercentage;
        healthBottom = borderBottom - margin;
        healthTop = healthBottom - healthHeight;
        canvas.drawRect(
                (float) gameDisplay.gameToDisplayCoordinatesX(healthLeft),
                (float) gameDisplay.gameToDisplayCoordinatesY(healthTop),
                (float) gameDisplay.gameToDisplayCoordinatesX(healthRight),
                (float) gameDisplay.gameToDisplayCoordinatesY(healthBottom),
                healthPaint);
    }
}
