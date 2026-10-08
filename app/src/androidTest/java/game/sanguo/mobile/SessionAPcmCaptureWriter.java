package game.sanguo.mobile;

import java.io.IOException;
import java.io.OutputStream;

/** Test measurement only. Fixed storage, exact FIFO bytes; overflow invalidates capture. */
final class SessionAPcmCaptureWriter {
    private final byte[][] blocks;
    private final int[] lengths;
    private final OutputStream output;
    private final Thread worker;
    private int read,write,count,highWater;
    private boolean closing;
    private IOException failure;
    private long writtenBytes,maxWriteNanos;
    SessionAPcmCaptureWriter(OutputStream output,int slots,int blockBytes) {
        if(output==null||slots<2||blockBytes<4)throw new IllegalArgumentException("Invalid capture queue");
        this.output=output;blocks=new byte[slots][blockBytes];lengths=new int[slots];
        worker=new Thread(this::drain,"session-a-pcm-disk");worker.start();
    }
    synchronized void append(byte[] data,int bytes)throws IOException {
        if(failure!=null)throw failure;
        if(closing)throw new IOException("Capture queue closed");
        if(bytes<0||bytes>data.length||bytes>blocks[0].length||bytes%4!=0)throw new IOException("Invalid capture frame bytes");
        if(count==blocks.length){failure=new IOException("Capture disk queue overflow; no samples silently dropped");notifyAll();throw failure;}
        System.arraycopy(data,0,blocks[write],0,bytes);lengths[write]=bytes;
        write=(write+1)%blocks.length;count++;highWater=Math.max(highWater,count);notifyAll();
    }
    private void drain() {
        try {
            for(;;) {
                int slot,n;
                synchronized(this){while(count==0&&!closing&&failure==null)wait();if(failure!=null)return;if(count==0&&closing)return;slot=read;n=lengths[slot];}
                long start=System.nanoTime();output.write(blocks[slot],0,n);long elapsed=System.nanoTime()-start;
                synchronized(this){writtenBytes+=n;maxWriteNanos=Math.max(maxWriteNanos,elapsed);read=(read+1)%blocks.length;count--;notifyAll();}
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();synchronized(this){failure=new IOException("Capture writer interrupted",e);notifyAll();}}
        catch(IOException e){synchronized(this){failure=e;notifyAll();}}
    }
    void finish()throws IOException {
        synchronized(this){closing=true;notifyAll();}
        try{worker.join(10000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Capture writer join interrupted",e);}
        if(worker.isAlive())throw new IOException("Capture disk writer still alive; file not complete");
        synchronized(this){if(failure!=null)throw failure;}
    }
    synchronized long writtenBytes(){return writtenBytes;}
    synchronized long maxWriteNanos(){return maxWriteNanos;}
    synchronized int highWater(){return highWater;}
}
