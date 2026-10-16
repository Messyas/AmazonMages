package com.example.amazonMages.save;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositorySessionTest {
    @Test public void reopenObservesQueuedSaveAndOldCallbacksAreDiscarded() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="session-order-test.db";context.deleteDatabase(name);
        CountDownLatch loaded=new CountDownLatch(1),closed=new CountDownLatch(1);
        AtomicBoolean staleCallback=new AtomicBoolean();
        AtomicReference<PlayerPositionEntity> position=new AtomicReference<>();
        AtomicReference<GameSaveRepository> second=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            GameSaveRepository first=new GameSaveRepository(context,1,name);
            first.savePosition(123,456,3);
            first.loadPosition(row->staleCallback.set(true));
            first.close();first.close();
            first.savePosition(999,999,1); // A disposed session cannot overwrite its replacement.
            second.set(new GameSaveRepository(context,1,name));
            second.get().loadPosition(row->{position.set(row);loaded.countDown();});
        });
        try {
            assertTrue(loaded.await(10,TimeUnit.SECONDS));
            assertNotNull(position.get());assertEquals(123,position.get().x,0);
            assertEquals(3,position.get().health);assertFalse(staleCallback.get());
        } finally {
            second.get().close(closed::countDown);
            assertTrue(closed.await(10,TimeUnit.SECONDS));
            context.deleteDatabase(name);
        }
    }
}
