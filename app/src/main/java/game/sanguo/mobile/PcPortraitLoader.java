package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded original bitmap cache. Drawable keeps no bitmap reference; evictions never recycle live display lists. */
final class PcPortraitLoader {
    interface Target {void ready();}
    private static PcPortraitLoader shared;
    static synchronized PcPortraitLoader shared(Context context){if(shared==null)shared=new PcPortraitLoader(context);return shared;}
    private final Context context;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->new Thread(r,"OriginalPortraitPixels"),new ThreadPoolExecutor.AbortPolicy());
    private final LruCache<String,Bitmap> pixels=new LruCache<String,Bitmap>(16*1024*1024){@Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}};
    private final Set<String> pending=new HashSet<>(),failed=new HashSet<>();
    private final WeakHashMap<Target,Boolean> listeners=new WeakHashMap<>();
    private volatile PcPortraitCatalog catalog;
    private volatile String error="";
    private boolean loading;
    private int decoded;
    private PcPortraitLoader(Context context){this.context=context.getApplicationContext();worker.allowCoreThreadTimeOut(true);}
    private void changed(){ui.post(()->{ArrayList<Target> live;synchronized(this){live=new ArrayList<>(listeners.keySet());listeners.clear();}for(Target target:live)target.ready();});}
    synchronized Bitmap get(PortraitMediaIdentity identity,int year,int form,Target target){
        if(identity==null)return null;
        PcPortraitCatalog known=catalog;
        if(known==null){
            if(error.isEmpty()){listeners.put(target,true);if(!loading){loading=true;worker.execute(()->{try{catalog=new PcPortraitCatalog(context);}catch(IOException failure){error=failure.toString();android.util.Log.e("PcPortrait","Catalog source rejected",failure);}finally{changed();}});}}return null;
        }
        PcPortraitCatalog.Image image=known.resolve(identity,year,form);if(image==null)return null;
        Bitmap found=pixels.get(image.asset);if(found!=null)return found;if(failed.contains(image.asset))return null;
        listeners.put(target,true);if(!pending.add(image.asset))return null;
        try{worker.execute(()->{
            try{
                byte[] raw;try(var input=context.getAssets().open(image.asset)){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=input.read(buffer))!=-1;){if(bytes.size()+n>256*1024)throw new IOException("Original portrait PNG exceeded source extent");bytes.write(buffer,0,n);}raw=bytes.toByteArray();}
                StringBuilder hash=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(raw))hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));if(!hash.toString().equals(image.pngSha))throw new IOException("Original PNG changed");
                BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
                Bitmap bitmap=BitmapFactory.decodeByteArray(raw,0,raw.length,options);
                if(bitmap==null||bitmap.getWidth()!=image.width||bitmap.getHeight()!=image.height)throw new IOException("Original bitmap dimensions differ");
                synchronized(this){pixels.put(image.asset,bitmap);decoded++;}
            }catch(Exception failure){synchronized(this){failed.add(image.asset);error=failure.toString();}android.util.Log.e("PcPortrait","Original pixels rejected",failure);}
            finally{synchronized(this){pending.remove(image.asset);}changed();}
        });}catch(java.util.concurrent.RejectedExecutionException busy){pending.remove(image.asset);ui.postDelayed(target::ready,40);}
        return null;
    }
    synchronized int bytes(){return pixels.size();}
    synchronized int decoded(){return decoded;}
    int images(){PcPortraitCatalog value=catalog;return value==null?0:value.imageCount();}
    int identities(){PcPortraitCatalog value=catalog;return value==null?0:value.identityCount();}
    String error(){return error;}
    synchronized void trim(){pixels.evictAll();failed.clear();}
}
