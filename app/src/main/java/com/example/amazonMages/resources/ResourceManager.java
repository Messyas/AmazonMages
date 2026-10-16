package com.example.amazonMages.resources;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

/** Application-resource cache with a 16 MiB budget. Eviction never recycles a live renderer's bitmap. */
public final class ResourceManager {
    private static ResourceManager shared;
    private final Resources resources;
    private final LruCache<Long,Bitmap> bitmaps = new LruCache<Long,Bitmap>(16 * 1024 * 1024) {
        @Override protected int sizeOf(Long key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private ResourceManager(Resources resources) { this.resources=resources; }
    public static synchronized ResourceManager shared(Context context) {
        if (shared==null) shared=new ResourceManager(context.getApplicationContext().getResources());
        return shared;
    }
    public synchronized Bitmap bitmap(int id, boolean scaled) {
        long key=((long)id<<1)|(scaled?1:0);
        Bitmap bitmap=bitmaps.get(key);
        if (bitmap!=null) return bitmap;
        BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=scaled;
        bitmap=BitmapFactory.decodeResource(resources,id,options);
        if (bitmap!=null) bitmaps.put(key,bitmap);
        return bitmap;
    }
    public synchronized void trim() { bitmaps.evictAll(); }
    public synchronized int cachedBytes() { return bitmaps.size(); }
    public static synchronized void clearShared() { if (shared!=null) { shared.trim(); shared=null; } }
}
