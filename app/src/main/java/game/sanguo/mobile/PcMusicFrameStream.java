package game.sanguo.mobile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Bounded PCM frame reads; original intro once, exact source loop thereafter. No timing-based seeking. */
final class PcMusicFrameStream implements Closeable {
    private final RandomAccessFile input;
    private final int alignment;
    private final long end,start;
    private final boolean repeat;
    private long position,loopCount;
    PcMusicFrameStream(File pcm,long frames,int channels,long loopStart,long loopEnd,boolean repeat,long initialFrame)throws IOException {
        alignment=Math.multiplyExact(channels,2);this.repeat=repeat;
        if(channels<1||channels>2||frames<1||initialFrame<0||initialFrame>=frames
            ||!((loopStart==-1&&loopEnd==-1)||(loopStart>=0&&loopStart<loopEnd&&loopEnd==frames)))throw new IOException("Invalid source frame stream");
        end=frames;start=loopStart<0?0:loopStart;position=initialFrame;
        if(pcm.length()!=Math.multiplyExact(frames,alignment))throw new IOException("PCM/source frame extent differs");
        input=new RandomAccessFile(pcm,"r");input.seek(Math.multiplyExact(position,alignment));
    }
    int read(byte[] buffer)throws IOException {
        if(buffer.length<alignment||buffer.length%alignment!=0)throw new IOException("Whole-frame output buffer required");
        if(position==end){if(!repeat)return -1;position=start;loopCount++;input.seek(Math.multiplyExact(start,alignment));}
        int size=(int)Math.min(buffer.length,Math.multiplyExact(end-position,alignment));
        input.readFully(buffer,0,size);position+=size/alignment;return size;
    }
    long position(){return position;}
    long loops(){return loopCount;}
    @Override public void close()throws IOException{input.close();}
}
