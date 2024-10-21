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

    private final Context context;

    public GameOver(Context context) {
        this.context = context;
    }

    public void draw(Canvas canvas) {
        String text = "Game Over";

        float x = 800;
        float y = 200;

        Paint paint = new Paint();
        int color = getColor(context, R.color.gameOver);
        paint.setColor(color);
        float textSize = 150;
        paint.setTextSize(textSize);

        canvas.drawText(text, x, y, paint);
    }
}
