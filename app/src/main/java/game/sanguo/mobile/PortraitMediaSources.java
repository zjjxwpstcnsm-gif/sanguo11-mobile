package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import game.sanguo.api.StateToken;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

/** Readonly DTO/immutable saved-byte seams. Workers never retain or inspect a mutable World. */
final class PortraitMediaSources {
    private static final class Entry {
        Map<Integer,PortraitMediaIdentity> identities=Collections.emptyMap();
        final WeakHashMap<PcPortraitLoader.Target,Boolean> listeners=new WeakHashMap<>();
        boolean pending;String error="";
    }
    private static final Map<Object,Entry> sources=new WeakHashMap<>();
    private static final Map<Object,Boolean> retired=new WeakHashMap<>();
    private static final Handler ui=new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),r->new Thread(r,"SavedPortraitSources"),new ThreadPoolExecutor.AbortPolicy());
    static{worker.allowCoreThreadTimeOut(true);}
    private PortraitMediaSources(){}
    private static Object field(Object value,String name)throws ReflectiveOperationException{return value.getClass().getField(name).get(value);}
    static synchronized void bind(Object view,StateToken expected,Object snapshot){
        if(view==null||snapshot==null)throw new IllegalArgumentException("Readonly portrait snapshot required");
        try{
            if(!expected.equals(field(snapshot,"state")))throw new IllegalArgumentException("Portrait metadata token differs");
            Map<Integer,PortraitMediaIdentity> next=new HashMap<>();
            for(Object officer:(Iterable<?>)field(snapshot,"officers")){
                Object source=field(officer,"source");if(source==null)continue;int id=(Integer)field(officer,"id");
                PortraitMediaIdentity identity=new PortraitMediaIdentity(id,(Integer)field(source,"nativeId"),(String)field(source,"sourceVariant"),(String)field(source,"sourcePath"),(String)field(source,"sourceSha"),(String)field(source,"recordSha"));
                if(next.put(id,identity)!=null)throw new IllegalArgumentException("Duplicate portrait source identity");
            }
            retired.remove(view);Entry entry=new Entry();entry.identities=Collections.unmodifiableMap(next);Entry previous=sources.put(view,entry);if(previous!=null)notifyReady(previous);
        }catch(ReflectiveOperationException error){throw new IllegalArgumentException("Completed source DTO contract missing",error);}
    }
    static synchronized boolean bound(Object view){return sources.containsKey(view);}
    /** raw is the detached copy supplied by SaveExtensions.get; names are already copied on the UI boundary. */
    static synchronized void saved(Object view,byte[] raw,Map<Integer,String> names){
        if(sources.containsKey(view)||retired.containsKey(view))return;Entry entry=new Entry();sources.put(view,entry);if(raw==null)return;
        entry.pending=true;WeakReference<Object> key=new WeakReference<>(view);
        try{worker.execute(()->{
            Map<Integer,PortraitMediaIdentity> parsed=Collections.emptyMap();String failure="";
            try{parsed=PortraitSavedSources.read(raw,names);}catch(Exception error){failure=error.toString();android.util.Log.e("PcPortrait","Saved source rejected",error);}
            synchronized(PortraitMediaSources.class){Object current=key.get();if(current==null||sources.get(current)!=entry)return;entry.identities=parsed;entry.error=failure;entry.pending=false;notifyReady(entry);}
        });}catch(RejectedExecutionException busy){entry.pending=false;entry.error="Saved portrait source queue overflow; new detached view required";android.util.Log.w("PcPortrait",entry.error);}
    }
    private static void notifyReady(Entry entry){ArrayList<PcPortraitLoader.Target> listeners=new ArrayList<>(entry.listeners.keySet());entry.listeners.clear();ui.post(()->{for(var target:listeners)target.ready();});}
    static synchronized PortraitMediaIdentity source(Object view,int id){Entry e=sources.get(view);return e==null?null:e.identities.get(id);}
    static synchronized boolean pending(Object view,PcPortraitLoader.Target target){Entry e=sources.get(view);if(e!=null&&e.pending){e.listeners.put(target,true);return true;}return false;}
    static synchronized String error(Object view){if(retired.containsKey(view))return "Retired presentation source";Entry e=sources.get(view);return e==null?"":e.error;}
    static synchronized void discard(Object view){retired.put(view,true);Entry e=sources.remove(view);if(e!=null)notifyReady(e);}
}
