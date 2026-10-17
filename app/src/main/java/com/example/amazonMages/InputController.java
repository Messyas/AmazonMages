package com.example.amazonMages;
import android.view.MotionEvent;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.combat.WeaponLoadout;

/** Multi-pointer gameplay input; Game routes modal input before reaching this controller. */
final class InputController {
    private final Player player;
    private final Joystick joystick;
    private final Joystick aimStick;
    private final WeaponLoadout weapons;
    private final Runnable special;
    private final Runnable parry;
    /** Stick pushes below this are "not aiming": a tap fires at the last aim, a drag back here cancels. */
    static final double AIM_DEADZONE=.28;
    private static final double AIM_TOUCH_SLACK=1.3;
    private int movementPointer=-1,aimPointer=-1,pendingShots;
    private boolean aimDragged;
    InputController(Player player,Joystick joystick,Joystick aimStick,WeaponLoadout weapons,Runnable special,Runnable parry) {
        this.player=player;this.joystick=joystick;this.aimStick=aimStick;this.weapons=weapons;this.special=special;this.parry=parry;
    }
    void reset() {
        joystick.setIsPressed(false);joystick.resetActuator();
        releaseAimStick();
        movementPointer=-1;pendingShots=0;
    }
    private void releaseAimStick() {
        aimStick.setIsPressed(false);aimStick.resetActuator();
        aimPointer=-1;aimDragged=false;
    }
    /** Follows the finger; the player's aim only changes once the stick leaves the dead zone. */
    private void dragAimStick(float x,float y) {
        aimStick.setActuator(x,y);
        if(aimStick.magnitude()>=AIM_DEADZONE) {
            aimDragged=true;
            player.aim(aimStick.getActuatorX(),aimStick.getActuatorY());
        }
    }
    /** Lifting the finger fires along the aim; dragging back to the centre first cancels the shot. */
    private void releaseAim() {
        boolean cancelled=aimDragged && aimStick.magnitude()<AIM_DEADZONE;
        if(!cancelled) {
            // Never aimed yet: shoot the way the player last moved instead of a fixed direction.
            if(!aimDragged && player.getAttackDirectionX()==0 && player.getAttackDirectionY()==0)
                player.aim(player.getDirectionX(),player.getDirectionY());
            attack();
        }
        releaseAimStick();
    }
    boolean consumeShot() { if(pendingShots==0)return false;pendingShots--;return true; }
    private void attack() { pendingShots=Math.min(8,pendingShots+1); }
    boolean touch(MotionEvent event,int width,int height) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL: reset();return true;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int index=event.getActionIndex();float x=event.getX(index),y=event.getY(index);
                if(aimPointer<0 && aimStick.contains(x,y,AIM_TOUCH_SLACK)) {
                    aimPointer=event.getPointerId(index);aimDragged=false;
                    aimStick.setIsPressed(true);dragAimStick(x,y);
                } else if(x>width*.55f && x<=width*.73f && y>height*.68f) parry.run();
                else if(x>width*.72f && y>height*.55f) {
                    if(y<height*.75f)weapons.next();
                    else if(x<width*.85f)special.run();
                    // Bottom-right corner belongs to the aim stick; taps just outside it do nothing.
                } else if(joystick.getIsPressed())attack();
                else if(joystick.isPressed(x,y)) {
                    movementPointer=event.getPointerId(index);joystick.setIsPressed(true);joystick.setActuator(x,y);
                } else attack();
                return true;
            case MotionEvent.ACTION_MOVE:
                int aim=event.findPointerIndex(aimPointer);
                if(aim>=0 && aimStick.getIsPressed())dragAimStick(event.getX(aim),event.getY(aim));
                int movement=event.findPointerIndex(movementPointer);
                if(joystick.getIsPressed() && movement>=0)joystick.setActuator(event.getX(movement),event.getY(movement));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int pointer=event.getPointerId(event.getActionIndex());
                if(aimPointer==pointer)releaseAim();
                if(movementPointer==pointer) {movementPointer=-1;joystick.setIsPressed(false);joystick.resetActuator();}
                return true;
            default:return false;
        }
    }
}
