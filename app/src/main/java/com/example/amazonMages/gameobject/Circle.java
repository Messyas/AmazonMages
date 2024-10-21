package com.example.amazonMages.gameobject;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.amazonMages.GameDisplay;

/**
 * Circle is an abstract class which implements a draw method from GameObject for drawing the object
 * as a circle.
 */
public abstract class Circle extends GameObject {
    protected double radius;
    protected Paint paint;

    public Circle(int color, double positionX, double positionY, double radius) {
        super(positionX, positionY);
        this.radius = radius;

        // Set colors of circle
        paint = new Paint();
        paint.setColor(color);
    }

    /**
     * isColliding checks if two circle objects are colliding, based on their positions and radios
     * Verifica se a colisao entre dois circulos esta acontecendo baseada na posicao dos raios dos dois circulos
     */
    public static boolean isColliding(Circle obj1, Circle obj2) {
        double distance = getDistanceBetweenObjects(obj1, obj2);
        double distanceToCollision = obj1.getRadius() + obj2.getRadius();
        // se a distancia for menor que a distancia para colidir
        return distance < distanceToCollision;
    }

    public void draw(Canvas canvas, GameDisplay gameDisplay) {
        canvas.drawCircle(
                (float) gameDisplay.gameToDisplayCoordinatesX(positionX),
                (float) gameDisplay.gameToDisplayCoordinatesY(positionY),
                (float) radius,
                paint
        );
    }

    public double getRadius() {
        return radius;
    }
}