package game.sanguo.mobile;

import java.util.*;
import java.util.concurrent.*;

/** Bounded CPU-only decode queue. Owner polls; no View, callback or GPU on worker.
 * Immutable bundled cohort; epoch invalidates world replacement and close results. */
final class SceneAssetQueue implements AutoCloseable {
    private final Thread owner=Thread.currentThread();
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"asset-decode");t.setDaemon(true);return t;});
    private final Map<String,Future<SceneMesh>> jobs=new LinkedHashMap<>();
    private final Map<String,SceneMesh> ready=new LinkedHashMap<>(64,.75f,true);
    private final Map<String,String> errors=new LinkedHashMap<>();
    private long bytes;private boolean closed;
    private void owner(){if(Thread.currentThread()!=owner)throw new IllegalStateException("asset owner thread");}
    SceneMesh request(String key,Callable<SceneMesh> decode){
        owner();if(closed)return null;SceneMesh m=ready.get(key);
        if(m!=null||errors.containsKey(key)||jobs.containsKey(key))return m;
        if(jobs.size()>=64)return null; // Caller retries on a later frame, never caller-runs decoding.
        jobs.put(key,executor.submit(decode));return null;
    }
    boolean drain(){
        owner();boolean changed=false;Iterator<Map.Entry<String,Future<SceneMesh>>> it=jobs.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Future<SceneMesh>> entry=it.next();if(!entry.getValue().isDone())continue;
            try{SceneMesh m=entry.getValue().get();ready.put(entry.getKey(),m);bytes+=size(m);}
            catch(Exception e){if(errors.size()>=128)errors.remove(errors.keySet().iterator().next());errors.put(entry.getKey(),String.valueOf(e.getCause()==null?e:e.getCause()));}
            it.remove();changed=true;
        }
        Iterator<SceneMesh> meshes=ready.values().iterator();while(bytes>24L*1024*1024&&ready.size()>1){bytes-=size(meshes.next());meshes.remove();}
        return changed;
    }
    private static long size(SceneMesh m){return 4L*(m.vertices.length+m.indices.length+(m.uv==null?0:m.uv.length)+(m.tangents==null?0:m.tangents.length));}
    String error(String key){owner();return errors.get(key);}
    int pending(){return jobs.size();}
    long bytes(){return bytes;}
    void invalidate(){owner();for(Future<?> f:jobs.values())f.cancel(true);jobs.clear();executor.getQueue().clear();ready.clear();errors.clear();bytes=0;}
    public void close(){owner();if(closed)return;invalidate();closed=true;executor.shutdownNow();}
}
