package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.resources.ResourceManager;

public class Joystick {

    private int outerCircleCenterPositionX;
    private int outerCircleCenterPositionY;
    private int innerCircleCenterPositionX;
    private int innerCircleCenterPositionY;

    private int outerCircleRadius;
    private int innerCircleRadius;

    private final Paint innerCirclePaint;
    private final Paint outerCirclePaint;
    private final Paint spritePaint = new Paint();
    private final Bitmap sprite;
    private final Rect spriteSource = new Rect();
    private final RectF spriteDestination = new RectF();
    private boolean isPressed = false;
    private double actuatorX;
    private double actuatorY;

    public Joystick(int centerPositionX, int centerPositionY, int outerCircleRadius, int innerCircleRadius) {
        this(centerPositionX, centerPositionY, outerCircleRadius, innerCircleRadius, null);
    }

    public Joystick(int centerPositionX, int centerPositionY, int outerCircleRadius, int innerCircleRadius,
                    Context context) {
        
        // Outer and inner circle make up the joystick
        outerCircleCenterPositionX = centerPositionX;
        outerCircleCenterPositionY = centerPositionY;
        innerCircleCenterPositionX = centerPositionX;
        innerCircleCenterPositionY = centerPositionY;
        
        // Radii of circles
        this.outerCircleRadius = outerCircleRadius;
        this.innerCircleRadius = innerCircleRadius;

        // paint of circles
        outerCirclePaint = new Paint();
        outerCirclePaint.setColor(Color.GRAY);
        outerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        innerCirclePaint = new Paint();
        innerCirclePaint.setColor(Color.BLACK);
        innerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        sprite = context == null ? null : ResourceManager.shared(context).bitmap(R.drawable.hud_joystick, false);
        spritePaint.setFilterBitmap(false);
    }

    public void layout(int width,int height) {
        outerCircleCenterPositionX=(int)(width*.18f);
        outerCircleCenterPositionY=(int)(height*.78f);
        outerCircleRadius=Math.max(1,(int)(Math.min(width,height)*.13f));
        innerCircleRadius=(int)(outerCircleRadius*.57f);
        resetActuator(); isPressed=false; updateInnerCirclePosition();
    }

    /**
     * Places this joystick as the aim stick, bottom-right. It is smaller than the movement stick
     * (10% vs 13% of the short screen side) since it only needs to select a direction.
     */
    public void layoutAim(int width,int height) {
        int shortSide = Math.min(width, height);
        outerCircleRadius = Math.max(1, (int)(shortSide * .10f));
        innerCircleRadius = (int)(outerCircleRadius * .55f);
        int margin = Math.round(shortSide * .02f);
        outerCircleCenterPositionX = Math.min((int)(width * .90f), width - outerCircleRadius - margin);
        outerCircleCenterPositionY = Math.min((int)(height * .80f), height - outerCircleRadius - margin);
        resetActuator(); isPressed = false; updateInnerCirclePosition();
    }

    /** Draws only the outer ring, for sticks whose knob is drawn by the HUD (the attack button). */
    public void drawBase(Canvas canvas) {
        if (sprite != null) {
            spriteSource.set(0, 0, 48, 48);
            spriteDestination.set(outerCircleCenterPositionX - outerCircleRadius,
                outerCircleCenterPositionY - outerCircleRadius,
                outerCircleCenterPositionX + outerCircleRadius,
                outerCircleCenterPositionY + outerCircleRadius);
            canvas.drawBitmap(sprite, spriteSource, spriteDestination, spritePaint);
            return;
        }
        canvas.drawCircle(outerCircleCenterPositionX, outerCircleCenterPositionY,
            outerCircleRadius, outerCirclePaint);
    }

    public int getKnobX() { return innerCircleCenterPositionX; }
    public int getKnobY() { return innerCircleCenterPositionY; }
    public int getKnobRadius() { return innerCircleRadius; }

    /** True when a touch is within the ring, widened by {@code slack} (1.3 = 30% larger). */
    public boolean contains(double touchPositionX, double touchPositionY, double slack) {
        return Math.hypot(touchPositionX - outerCircleCenterPositionX,
            touchPositionY - outerCircleCenterPositionY) < outerCircleRadius * slack;
    }

    /** How far the stick is pushed, 0 (centre) to 1 (rim). */
    public double magnitude() {
        return Math.hypot(actuatorX, actuatorY);
    }

    public void draw(Canvas canvas) {
        if (sprite != null) {
            float diameter = outerCircleRadius * 2f;
            float knob = innerCircleRadius * 2f;
            spriteSource.set(0, 0, 48, 48);
            spriteDestination.set(outerCircleCenterPositionX - outerCircleRadius,
                outerCircleCenterPositionY - outerCircleRadius,
                outerCircleCenterPositionX + outerCircleRadius,
                outerCircleCenterPositionY + outerCircleRadius);
            canvas.drawBitmap(sprite, spriteSource, spriteDestination, spritePaint);
            spriteSource.set(48, 0, 72, 24);
            spriteDestination.set(innerCircleCenterPositionX - innerCircleRadius,
                innerCircleCenterPositionY - innerCircleRadius,
                innerCircleCenterPositionX + innerCircleRadius,
                innerCircleCenterPositionY + innerCircleRadius);
            canvas.drawBitmap(sprite, spriteSource, spriteDestination, spritePaint);
            return;
        }
        // Draw outer circle
        canvas.drawCircle(
                outerCircleCenterPositionX,
                outerCircleCenterPositionY,
                outerCircleRadius,
                outerCirclePaint
        );

        // Draw inner circle
        canvas.drawCircle(
                innerCircleCenterPositionX,
                innerCircleCenterPositionY,
                innerCircleRadius,
                innerCirclePaint
        );
    }

    public void update() {
        updateInnerCirclePosition();
    }

    /**
     * Método que atualiza a posição do círculo interno com base nas
     * coordenadas do atuador (actuator).
     */
    private void updateInnerCirclePosition() {
        // Atualiza a posição do círculo interno com base nas coordenadas do atuador
        innerCircleCenterPositionX = (int) (outerCircleCenterPositionX + actuatorX * outerCircleRadius);
        innerCircleCenterPositionY = (int) (outerCircleCenterPositionY + actuatorY * outerCircleRadius);
    }

    public void setActuator(double touchPositionX, double touchPositionY) {
        double deltaX = touchPositionX - outerCircleCenterPositionX;
        double deltaY = touchPositionY - outerCircleCenterPositionY;
        double deltaDistance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        if(deltaDistance < outerCircleRadius) {
            actuatorX = deltaX/outerCircleRadius;
            actuatorY = deltaY/outerCircleRadius;
        } else {
            actuatorX = deltaX/deltaDistance;
            actuatorY = deltaY/deltaDistance;
        }
    }

    /**
     * Define se o joystick está pressionado.
     */
    public boolean isPressed(double touchPositionX, double touchPositionY) {
        //Distancia do joystick
        double joystickCenterToTouchDistance = Math.sqrt(
                Math.pow(outerCircleCenterPositionX - touchPositionX, 2) +
                        Math.pow(outerCircleCenterPositionY - touchPositionY, 2)
        );
        return joystickCenterToTouchDistance < outerCircleRadius;
    }

    /**
     * Verifica se o joystick está pressionado.
     */
    public boolean getIsPressed() {
        return isPressed;
    }

    public void setIsPressed(boolean isPressed) {
        this.isPressed = isPressed;
    }

    public double getActuatorX() {
        return actuatorX;
    }

    public double getActuatorY() {
        return actuatorY;
    }

    public void resetActuator() {
        actuatorX = 0;
        actuatorY = 0;
    }
}
