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
    private double zoom = 1.0;

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

    public double getZoom() { return zoom; }

    public void setZoom(double value) {
        zoom = Math.max(1.0, Math.min(3.0, value));
        update();
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
        // A couple of pixels of slack: after the zoom scale, rounding must never expose an empty edge.
        gameRect.set(
                (int) Math.floor(gameCenterX - (double) widthPixels /(2 * zoom)) - 2,
                (int) Math.floor(gameCenterY - (double) heightPixels /(2 * zoom)) - 2,
                (int) Math.ceil(gameCenterX + (double) widthPixels /(2 * zoom)) + 2,
                (int) Math.ceil(gameCenterY + (double) heightPixels /(2 * zoom)) + 2
        );
        return gameRect;
    }
}
