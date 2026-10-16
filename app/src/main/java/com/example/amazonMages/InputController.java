package com.example.amazonMages;
import android.view.MotionEvent;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.combat.WeaponLoadout;

/** Multi-pointer gameplay input; Game routes modal input before reaching this controller. */
final class InputController {
    private final Player player;
    private final Joystick joystick;
    private final WeaponLoadout weapons;
    private final Runnable special;
    private int movementPointer=-1,attackPointer=-1,pendingShots;
    InputController(Player player,Joystick joystick,WeaponLoadout weapons,Runnable special) {
        this.player=player;this.joystick=joystick;this.weapons=weapons;this.special=special;
    }
    void reset() {
        joystick.setIsPressed(false);joystick.resetActuator();
        movementPointer=-1;attackPointer=-1;pendingShots=0;
    }
    boolean consumeShot() { if(pendingShots==0)return false;pendingShots--;return true; }
    private void attack() { pendingShots=Math.min(8,pendingShots+1); }
    boolean touch(MotionEvent event,int width,int height) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL: reset();return true;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int index=event.getActionIndex();float x=event.getX(index),y=event.getY(index);
                if(x>width*.72f && y>height*.55f) {
                    if(y<height*.75f)weapons.next();
                    else if(x<width*.85f)special.run();
                    else {attackPointer=event.getPointerId(index);attack();}
                } else if(joystick.getIsPressed())attack();
                else if(joystick.isPressed(x,y)) {
                    movementPointer=event.getPointerId(index);joystick.setIsPressed(true);joystick.setActuator(x,y);
                } else attack();
                return true;
            case MotionEvent.ACTION_MOVE:
                int aim=event.findPointerIndex(attackPointer);
                if(aim>=0)player.aim(event.getX(aim)-width*.90,event.getY(aim)-height*.86);
                int movement=event.findPointerIndex(movementPointer);
                if(joystick.getIsPressed() && movement>=0)joystick.setActuator(event.getX(movement),event.getY(movement));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int pointer=event.getPointerId(event.getActionIndex());
                if(attackPointer==pointer)attackPointer=-1;
                if(movementPointer==pointer) {movementPointer=-1;joystick.setIsPressed(false);joystick.resetActuator();}
                return true;
            default:return false;
        }
    }
}
