package com.example.amazonMages;
import android.content.Context;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.combat.WeaponLoadout;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class InputControllerTest {
    @Test public void cancelResetsMovementAndQueuedAttacks() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Joystick joystick=new Joystick(200,600,70,40,context);
        Joystick aim=new Joystick(0,0,55,30,context);aim.layoutAim(1200,800);
        Player player=new Player(context,joystick,100,100,32,null);
        InputController input=new InputController(player,joystick,aim,new WeaponLoadout(),()->{});
        MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,220,600,0);
        MotionEvent cancel=MotionEvent.obtain(0,1,MotionEvent.ACTION_CANCEL,220,600,0);
        try {
            input.touch(down,1200,800);assertTrue(joystick.getIsPressed());assertTrue(joystick.getActuatorX()>0);
            input.touch(cancel,1200,800);assertFalse(joystick.getIsPressed());assertEquals(0,joystick.getActuatorX(),0);
            assertFalse(input.consumeShot());
        } finally {down.recycle();cancel.recycle();}
    }
    @Test public void dragAimStickAndReleaseFiresOnce() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Joystick joystick=new Joystick(200,600,70,40,context);
        Joystick aim=new Joystick(0,0,55,30,context);aim.layoutAim(1200,800);
        Player player=new Player(context,joystick,100,100,32,null);
        InputController input=new InputController(player,joystick,aim,new WeaponLoadout(),()->{});
        float cx=aim.getKnobX(),cy=aim.getKnobY(),far=cx+aim.getKnobRadius()*3;
        MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,cx,cy,0);
        MotionEvent move=MotionEvent.obtain(0,1,MotionEvent.ACTION_MOVE,far,cy,0);
        MotionEvent up=MotionEvent.obtain(0,2,MotionEvent.ACTION_UP,far,cy,0);
        try {
            input.touch(down,1200,800);assertFalse(input.consumeShot());
            input.touch(move,1200,800);assertTrue(aim.getIsPressed());assertTrue(player.getAttackDirectionX()>0);
            assertFalse(input.consumeShot());
            input.touch(up,1200,800);assertTrue(input.consumeShot());assertFalse(input.consumeShot());
            assertFalse(aim.getIsPressed());
        } finally {down.recycle();move.recycle();up.recycle();}
    }
}
