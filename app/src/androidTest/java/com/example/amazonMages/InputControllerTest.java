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
        Joystick joystick=new Joystick(200,600,70,40);
        Player player=new Player(context,joystick,100,100,32,null);
        InputController input=new InputController(player,joystick,new WeaponLoadout(),()->{});
        MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,220,600,0);
        MotionEvent cancel=MotionEvent.obtain(0,1,MotionEvent.ACTION_CANCEL,220,600,0);
        try {
            input.touch(down,1200,800);assertTrue(joystick.getIsPressed());assertTrue(joystick.getActuatorX()>0);
            input.touch(cancel,1200,800);assertFalse(joystick.getIsPressed());assertEquals(0,joystick.getActuatorX(),0);
            assertFalse(input.consumeShot());
        } finally {down.recycle();cancel.recycle();}
    }
}
