package com.example.amazonMages.gamepanel;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.example.androidstudio2dgamedevelopment.R;


/**
 * GameOver is a panel which draws the text Game Over to the screen.
 */
public class GameOver {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameOver(Context context) {
        paint.setColor(getColor(context, R.color.gameOver));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas canvas) {
        String text = "Game Over";

        float x = canvas.getWidth() * .5f;
        float y = canvas.getHeight() * .45f;

        float textSize = Math.min(canvas.getWidth(), canvas.getHeight()) * .12f;
        paint.setTextSize(textSize);

        canvas.drawText(text, x, y, paint);
    }
}
