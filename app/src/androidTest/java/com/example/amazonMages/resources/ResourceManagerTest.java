package com.example.amazonMages.resources;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.androidstudio2dgamedevelopment.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ResourceManagerTest {
    @Test public void sharesDecodedBitmapsAndTrimDoesNotRecycleLiveViews() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        ResourceManager resources=ResourceManager.shared(context);
        Bitmap first=resources.bitmap(R.drawable.energyball,false);
        assertSame(first,resources.bitmap(R.drawable.energyball,false));
        assertTrue(resources.cachedBytes()<=resources.budgetBytes());
        resources.trim();assertEquals(0,resources.cachedBytes());assertFalse(first.isRecycled());
        assertNotSame(first,resources.bitmap(R.drawable.energyball,false));
        ResourceManager.clearShared();
    }
}
