package game.sanguo.mobile;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original compressed music/voice -> bounded-buffer PCM cache. No decoding/hashing on the UI thread. */
final class PcVorbisDecoder {
    interface PcmSink { void write(byte[] buffer,int length)throws IOException; }
    static final class Result {
        final String pcmSha256,codec;
        final long frames,bytes,elapsedMillis;
        final boolean referenceByteEqual,sourceFrameEqual;
        Result(String hash,String codec,long frames,long bytes,long millis,PcAudioSource track){pcmSha256=hash;this.codec=codec;this.frames=frames;this.bytes=bytes;elapsedMillis=millis;referenceByteEqual=hash.equals(track.referencePcmSha256);sourceFrameEqual=frames==track.frames;}
    }
    private static String hex(byte[] data){StringBuilder value=new StringBuilder();for(byte item:data)value.append(String.format(java.util.Locale.ROOT,"%02x",item&255));return value.toString();}
    private static void cancelled(AtomicBoolean cancel)throws IOException{if(cancel.get()||Thread.currentThread().isInterrupted())throw new IOException("Media decode cancelled");}
    static Result decode(Context context,PcAudioSource track,File output,AtomicBoolean cancel)throws IOException {
        return decode(context,track,output,cancel,null);
    }
    static Result decode(Context context,PcAudioSource track,File output,AtomicBoolean cancel,PcmSink sink)throws IOException {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Audio decode on UI thread");
        if(output.exists())throw new IOException("Fresh decoder output required");
        long started=SystemClock.elapsedRealtime();MediaExtractor extractor=new MediaExtractor();MediaCodec codec=null;
        File partial=new File(output.getPath()+".part");boolean published=false;
        if(partial.exists())throw new IOException("Existing interrupted media decode output");
        try{
            MessageDigest ogg=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];
            try(var input=context.getAssets().open(track.asset)){for(int n;(n=input.read(buffer))!=-1;){cancelled(cancel);ogg.update(buffer,0,n);}}
            if(!hex(ogg.digest()).equals(track.oggSha256))throw new IOException("Original Ogg source changed");
            try(var fd=context.getAssets().openFd(track.asset)){extractor.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
            if(extractor.getTrackCount()!=1)throw new IOException("Unexamined original audio tracks");
            MediaFormat input=extractor.getTrackFormat(0);
            if(!"audio/vorbis".equals(input.getString(MediaFormat.KEY_MIME))||input.getInteger(MediaFormat.KEY_SAMPLE_RATE)!=track.sampleRate||input.getInteger(MediaFormat.KEY_CHANNEL_COUNT)!=track.channels)throw new IOException("Original Vorbis format mismatch");
            extractor.selectTrack(0);codec=MediaCodec.createDecoderByType("audio/vorbis");String name=codec.getName();codec.configure(input,null,null,0);codec.start();
            boolean inputEnd=false,outputEnd=false,formatSeen=false;long bytes=0,lastProgress=SystemClock.elapsedRealtime();
            MessageDigest pcm=MessageDigest.getInstance("SHA-256");MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            try(FileOutputStream target=new FileOutputStream(partial)){
                while(!outputEnd){
                    cancelled(cancel);
                    if(!inputEnd){int index=codec.dequeueInputBuffer(10000);if(index>=0){var data=codec.getInputBuffer(index);if(data==null)throw new IOException("Missing codec input");data.clear();int size=extractor.readSampleData(data,0);
                        if(size<0){codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
                        else{codec.queueInputBuffer(index,0,size,extractor.getSampleTime(),0);extractor.advance();}lastProgress=SystemClock.elapsedRealtime();}}
                    int index=codec.dequeueOutputBuffer(info,10000);
                    if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat format=codec.getOutputFormat();int encoding=format.containsKey(MediaFormat.KEY_PCM_ENCODING)?format.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;
                        if(format.getInteger(MediaFormat.KEY_SAMPLE_RATE)!=track.sampleRate||format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)!=track.channels||encoding!=AudioFormat.ENCODING_PCM_16BIT)throw new IOException("Unexamined decoder PCM format");formatSeen=true;}
                    else if(index>=0){
                        try{if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){if(!formatSeen||info.size%(track.channels*2)!=0)throw new IOException("Incomplete PCM frame");var data=codec.getOutputBuffer(index);if(data==null)throw new IOException("Missing codec output");data.position(info.offset);data.limit(info.offset+info.size);
                            while(data.hasRemaining()){int n=Math.min(data.remaining(),buffer.length);data.get(buffer,0,n);target.write(buffer,0,n);pcm.update(buffer,0,n);bytes+=n;if(bytes>track.frames*track.channels*2+8192)throw new IOException("Decoder exceeded source timeline; no implicit trim");if(sink!=null){sink.write(buffer,n);lastProgress=SystemClock.elapsedRealtime();}}}
                            outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;lastProgress=SystemClock.elapsedRealtime();
                        }finally{codec.releaseOutputBuffer(index,false);}}
                    if(SystemClock.elapsedRealtime()-lastProgress>10000)throw new IOException("Original audio decoder stalled");
                }
                target.getFD().sync();
            }
            long frames=bytes/(track.channels*2);
            if(frames!=track.frames)throw new IOException("Source/Android frame count differs: "+track.frames+"/"+frames+"; no padding or trim guessed");
            cancelled(cancel);if(!partial.renameTo(output))throw new IOException("Cannot publish decoded cache");published=true;
            return new Result(hex(pcm.digest()),name,frames,bytes,SystemClock.elapsedRealtime()-started,track);
        }catch(IOException error){throw error;}catch(Exception error){throw new IOException("Original audio decoder",error);}
        finally{if(codec!=null){try{codec.stop();}catch(IllegalStateException ignored){}codec.release();}extractor.release();if(!published&&partial.exists()&&!partial.delete())android.util.Log.w("PcAudio","Interrupted own cache output retained: "+partial);}
    }
}
