package game.sanguo.mobile;

import java.util.concurrent.*;
import java.util.function.Consumer;

/** One running CPU job, one replaceable waiting job and one owner-thread result.
 * No Android/engine objects cross this boundary. A producer may publish immutable
 * phases, but never overruns the one-slot mailbox. Cancellation remains advisory;
 * epochs reject even a decoder which ignores interruption. */
final class SceneWorkQueue<T> implements AutoCloseable {
    @FunctionalInterface interface PhasedWork<T> { T build(Consumer<T> publish) throws Exception; }
    private final Thread owner=Thread.currentThread();
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"scene-cpu");t.setDaemon(true);return t;});
    private Future<?> task;
    private long epoch;
    private boolean closed;
    private T result;
    private Throwable error;
    private boolean ready,terminal;
    private long discarded,delivered,backpressureNanos;
    void owner(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Scene owner thread required");}
    synchronized void invalidate(){
        owner();epoch++;if(task!=null)task.cancel(true);task=null;executor.getQueue().clear();
        result=null;error=null;ready=terminal=false;notifyAll();
    }
    void submit(Callable<T> build){submitPhased(publish->build.call());}
    synchronized void submitPhased(PhasedWork<T> build){
        owner();if(closed)throw new IllegalStateException("Scene closed");invalidate();long ticket=epoch;
        task=executor.submit(()->{
            T value=null;Throwable failure=null;
            try{value=build.build(partial->publish(ticket,partial,null,false));}
            catch(Exception|LinkageError|OutOfMemoryError e){failure=e;}
            try{publish(ticket,value,failure,true);}catch(CancellationException ignored){/* superseded or closed */}
        });
    }
    /** Worker only. Waiting releases the monitor, so pause, resume and cancellation
     * cannot require an owner-thread blocking join or an unbounded result queue. */
    private synchronized void publish(long ticket,T value,Throwable failure,boolean last){
        long blocked=0;
        try{
            while(!closed&&ticket==epoch&&ready){
                if(blocked==0)blocked=System.nanoTime();
                try{wait();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException("Scene work interrupted");}
            }
        }finally{if(blocked!=0)backpressureNanos+=System.nanoTime()-blocked;}
        if(closed||ticket!=epoch){discarded++;throw new CancellationException("Stale scene epoch");}
        result=value;error=failure;terminal=last;ready=true;
    }
    void drain(Consumer<T> success,Consumer<Throwable> failure){
        owner();T value;Throwable problem;
        synchronized(this){
            if(closed||!ready)return;value=result;problem=error;
            // An intermediate upload does NOT finish the CPU task or make READY.
            if(terminal)task=null;
            result=null;error=null;ready=terminal=false;delivered++;notifyAll();
        }
        if(problem==null)success.accept(value);else failure.accept(problem);
    }
    synchronized int waiting(){return executor.getQueue().size();}
    synchronized int pending(){return task==null?0:1;}
    synchronized long discarded(){return discarded;}
    synchronized long delivered(){return delivered;}
    synchronized long backpressureNanos(){return backpressureNanos;}
    @Override public synchronized void close(){owner();if(closed)return;invalidate();closed=true;executor.shutdownNow();notifyAll();}
}
