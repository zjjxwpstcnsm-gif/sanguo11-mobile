package game.sanguo.mobile;

import java.util.concurrent.*;
import java.util.function.Consumer;

/** One running CPU job, one replaceable waiting job and one owner-thread result.
 * No Android/engine objects cross this boundary. Cancellation is advisory; epochs
 * reject even a decoder which ignores interruption. Results are polled by owner. */
final class SceneWorkQueue<T> implements AutoCloseable {
    private final Thread owner=Thread.currentThread();
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"scene-cpu");t.setDaemon(true);return t;});
    private Future<?> task;
    private long epoch;
    private boolean closed;
    private T result;
    private Throwable error;
    private boolean ready;
    private long discarded;
    void owner(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Scene owner thread required");}
    synchronized void invalidate(){
        owner();epoch++;if(task!=null)task.cancel(true);task=null;executor.getQueue().clear();
        result=null;error=null;ready=false;
    }
    synchronized void submit(Callable<T> build){
        owner();if(closed)throw new IllegalStateException("Scene closed");invalidate();long ticket=epoch;
        task=executor.submit(()->{
            T value=null;Throwable failure=null;
            try{value=build.call();}catch(Exception|LinkageError|OutOfMemoryError e){failure=e;}
            synchronized(this){if(closed||ticket!=epoch){discarded++;return;}
                result=value;error=failure;ready=true;}
        });
    }
    void drain(Consumer<T> success,Consumer<Throwable> failure){
        owner();T value;Throwable problem;
        synchronized(this){if(closed||!ready)return;value=result;problem=error;result=null;error=null;ready=false;task=null;}
        if(problem==null)success.accept(value);else failure.accept(problem);
    }
    synchronized int waiting(){return executor.getQueue().size();}
    synchronized int pending(){return task==null?0:1;}
    synchronized long discarded(){return discarded;}
    @Override public synchronized void close(){owner();if(closed)return;invalidate();closed=true;executor.shutdownNow();}
}
