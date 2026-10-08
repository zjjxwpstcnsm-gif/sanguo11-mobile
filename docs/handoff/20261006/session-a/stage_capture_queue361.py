#!/usr/bin/env python3
"""Separate actual capture read from bounded sequential disk writer; retain every byte."""
from pathlib import Path
import json
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/capture-queue361';BASE=ROOT/'out/session-a/capture-test-build356/source'
WRITER='app/src/androidTest/java/game/sanguo/mobile/SessionAPcmCaptureWriter.java';SERVICE='app/src/androidTest/java/game/sanguo/mobile/SessionAAudioCaptureService.java'
CODE='''package game.sanguo.mobile;

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
'''
def main():
 assert not OUT.exists();(OUT/WRITER).parent.mkdir(parents=True);(OUT/WRITER).write_text(CODE)
 s=(BASE/SERVICE).read_text();s=s.replace('int actualBufferFrames=0;String failure="";','int actualBufferFrames=0,queueHighWater=0;long writerBytes=0;String failure="";')
 old='out.write(header(0,sampleRate));\n                while';new='out.write(header(0,sampleRate));\n                SessionAPcmCaptureWriter disk=new SessionAPcmCaptureWriter(out,64,8192);\n                try {\n                while';assert s.count(old)==1;s=s.replace(old,new)
 old='out.write(buffer,0,n);';assert s.count(old)==1;s=s.replace(old,'disk.append(buffer,n);')
 old='out.flush();file.getFD().sync();';new='} finally {disk.finish();queueHighWater=disk.highWater();writerBytes=disk.writtenBytes();maxWriteNanos=disk.maxWriteNanos();}\n                if(writerBytes!=bytes)throw new IllegalStateException("Capture writer byte count mismatch");\n                out.flush();file.getFD().sync();';assert s.count(old)==1;s=s.replace(old,new)
 old='.put("samplesDroppedOrSynthesized",false)';s=s.replace(old,old+'.put("diskQueueSlots",64).put("diskQueueBytes",524288).put("diskQueueHighWater",queueHighWater).put("writerBytes",writerBytes)');(OUT/SERVICE).write_text(s)
 paths=[{'path':p,'beforeSha256':sha(BASE/p) if (BASE/p).exists() else None,'afterSha256':sha(OUT/p),'stagedPath':str(OUT/p)} for p in [SERVICE,WRITER]]
 report={'paths':paths,'testOnly':True,'game326Unchanged':True,'queueBytes':524288,'queueOverflowIsFailure':True,'noBlockingDiskWriteOnCaptureReadThread':True,'scope':'Actual358 max diskwrite57ms versus actual nativebuffer64ms motivates isolate disk from read. Bounded64x8192 preallocated FIFO, no PCM splice/resample/drop, original0.995 still required. Not unique capture/player root proof or Android acceptance.','wholeGoalComplete':False};(D/'CAPTURE_QUEUE361.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
