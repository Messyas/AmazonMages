package com.example.amazonMages;

import android.graphics.Rect;

import com.example.amazonMages.gameobject.GameObject;

public class GameDisplay {
    public final Rect DISPLAY_RECT;
    private final Rect gameRect = new Rect();
    private int widthPixels;
    private int heightPixels;
    private final GameObject centerObject;
    private double displayCenterX;
    private double displayCenterY;
    private double gameToDisplayCoordinatesOffsetX;
    private double gameToDisplayCoordinatesOffsetY;
    private double gameCenterX;
    private double gameCenterY;

    public GameDisplay(int widthPixels, int heightPixels, GameObject centerObject) {
        this.widthPixels = widthPixels;
        this.heightPixels = heightPixels;
        DISPLAY_RECT = new Rect(0, 0, widthPixels, heightPixels);

        this.centerObject = centerObject;

        displayCenterX = widthPixels/2.0;
        displayCenterY = heightPixels/2.0;

        update();
    }

    public void resize(int width,int height) {
        widthPixels=Math.max(1,width); heightPixels=Math.max(1,height);
        displayCenterX=widthPixels/2.0; displayCenterY=heightPixels/2.0;
        DISPLAY_RECT.set(0,0,widthPixels,heightPixels); update();
    }

    public void update() {
        gameCenterX = centerObject.getPositionX();
        gameCenterY = centerObject.getPositionY();

        gameToDisplayCoordinatesOffsetX = displayCenterX - gameCenterX;
        gameToDisplayCoordinatesOffsetY = displayCenterY - gameCenterY;
    }

    public double gameToDisplayCoordinatesX(double x) {
        return x + gameToDisplayCoordinatesOffsetX;
    }

    public double gameToDisplayCoordinatesY(double y) {
        return y + gameToDisplayCoordinatesOffsetY;
    }

    public Rect getGameRect() {
        gameRect.set(
                (int) (gameCenterX - (double) widthPixels /2),
                (int) (gameCenterY - (double) heightPixels /2),
                (int) (gameCenterX + (double) widthPixels /2),
                (int) (gameCenterY + (double) heightPixels /2)
        );
        return gameRect;
    }
}
