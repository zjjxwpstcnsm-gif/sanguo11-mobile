package game.sanguo.mobile;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** One short original voice at a time. The owner bounds/prioritizes pending facts; this worker never reads a World. */
final class PcVoiceStreamPlayer implements AutoCloseable {
    interface Listener {void started(PcVoiceDirective directive);void finished(PcVoiceDirective directive);}
    static final class Status {
        final PcVoiceDirective directive;final long submittedFrames,playedFrames,firstWriteMillis,firstPlayMillis;final boolean released;final String error,decodedSha256,submittedSha256;final int underruns;
        Status(Job j){directive=j.directive;submittedFrames=j.submitted;playedFrames=j.played;firstWriteMillis=j.firstWriteMillis;firstPlayMillis=j.firstPlayMillis;released=j.released;error=j.error;decodedSha256=j.decodedSha256;submittedSha256=j.submittedSha256;underruns=j.underruns;}
    }
    private static final class Job {
        final PcVoiceDirective directive;final long began=SystemClock.elapsedRealtimeNanos();final AtomicBoolean cancel=new AtomicBoolean();
        volatile AudioTrack audio;volatile long submitted,played,firstWriteMillis=-1,firstPlayMillis=-1;volatile boolean released;volatile String error="",decodedSha256="",submittedSha256="";volatile int underruns;
        int prebufferFrames;boolean decodedComplete;
        Job(PcVoiceDirective directive){this.directive=directive;}
    }
    private final Context context;private final Listener listener;private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"PcVoiceStream"));
    private final Object controls=new Object();
    private volatile float gain=.75f,focusDuck=1;private volatile Job current,last;private volatile boolean closed,paused;
    private PcVoiceCatalog catalog;private volatile long catalogMillis=-1;private volatile String catalogError="";
    PcVoiceStreamPlayer(Context context,Listener listener){this.context=context.getApplicationContext();this.listener=listener;
        worker.execute(()->{long began=SystemClock.elapsedRealtime();try{PcVoiceCatalog loaded=new PcVoiceCatalog(this.context);if(!closed)catalog=loaded;}catch(IOException e){catalogError=e.toString();}finally{catalogMillis=SystemClock.elapsedRealtime()-began;}});
    }
    boolean prepared(){ui();return catalogMillis>=0&&catalogError.isEmpty();}
    long catalogMillis(){ui();return catalogMillis;}
    private static void ui(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Serial source media owner required");}
    void play(PcVoiceDirective directive){ui();if(closed||current!=null&&!current.released)throw new IllegalStateException("Source voice worker busy/closed");Job job=new Job(directive);current=last=job;worker.execute(()->run(job));}
    void volume(float value,boolean ducked){ui();if(!Float.isFinite(value))throw new IllegalArgumentException("voice gain");gain=Math.max(0,Math.min(1,value));focusDuck=ducked?.2f:1;synchronized(controls){if(current!=null&&current.audio!=null)current.audio.setVolume(gain*focusDuck);}}
    void paused(boolean value){ui();paused=value;synchronized(controls){if(value&&current!=null&&current.audio!=null)current.audio.pause();controls.notifyAll();}}
    void stop(){ui();Job j=current;if(j!=null){j.cancel.set(true);synchronized(controls){if(j.audio!=null)try{j.audio.pause();j.audio.flush();}catch(IllegalStateException ignored){}controls.notifyAll();}}}
    Status status(){ui();return last==null?null:new Status(last);}
    private static void check(Job job)throws IOException{if(job.cancel.get()||Thread.currentThread().isInterrupted())throw new IOException("Source voice cancelled");}
    private void ready(Job job,AudioTrack audio)throws IOException {
        synchronized(controls){while(paused){check(job);try{controls.wait(30);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Voice pause interrupted",e);}}
            check(job);if((job.submitted>=job.prebufferFrames||job.decodedComplete)&&audio.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING){audio.play();
                if(job.firstPlayMillis<0){job.firstPlayMillis=(SystemClock.elapsedRealtimeNanos()-job.began)/1000000;main.post(()->{if(!job.cancel.get()&&!closed)listener.started(job.directive);});}}}
    }
    private void run(Job job){File directory=null,pcm=null;AudioTrack audio=null;
        try {
            check(job);if(catalog==null)throw new IOException("Original voice catalog not prepared: "+catalogError);
            if(catalog.voiceType(job.directive.speaker)!=job.directive.voiceTypeRaw)throw new IOException("Actual saved speaker identity/type does not match original source");
            PcVoiceCatalog.Voice source=catalog.voice(job.directive.nativeVoiceId,job.directive.alternateRaw);if(source==null)throw new IOException("Unknown original voice resource");
            check(job);directory=new File(context.getCacheDir(),"pc-voice-stream-"+job.began);if(!directory.mkdir())throw new IOException("Fresh own voice cache required");pcm=new File(directory,"original.pcm");
            // Short original voices decode completely off UI before playback.
            // Feeding MediaCodec through a backpressured AudioTrack sink can
            // starve the track even when every submitted byte is correct.
            PcVorbisDecoder.Result decoded=PcVorbisDecoder.decode(context,source,pcm,job.cancel);job.decodedSha256=decoded.pcmSha256;check(job);
            int mask=source.channels==1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
            int minimum=AudioTrack.getMinBufferSize(source.sampleRate,mask,AudioFormat.ENCODING_PCM_16BIT);if(minimum<=0)throw new IOException("Unsupported original voice format");
            audio=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(source.sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(mask).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(minimum,8192)).build();
            if(audio.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Voice AudioTrack uninitialized");
            java.security.MessageDigest submittedDigest;
            try{submittedDigest=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IOException(impossible);}
            job.prebufferFrames=Math.min(2048,audio.getBufferCapacityInFrames());
            AudioTrack output=audio;synchronized(controls){check(job);job.audio=audio;audio.setVolume(gain*focusDuck);}
            try(var input=new java.io.FileInputStream(pcm)){byte[] buffer=new byte[8192];int length;
                while((length=input.read(buffer))!=-1)for(int offset=0;offset<length;){ready(job,output);int accepted=output.write(buffer,offset,length-offset,AudioTrack.WRITE_NON_BLOCKING);
                    if(accepted<0||accepted%(source.channels*2)!=0)throw new IOException("Voice AudioTrack output "+accepted);
                    if(accepted==0){SystemClock.sleep(5);continue;}submittedDigest.update(buffer,offset,accepted);offset+=accepted;job.submitted+=accepted/(source.channels*2);job.underruns=output.getUnderrunCount();
                    if(job.firstWriteMillis<0)job.firstWriteMillis=(SystemClock.elapsedRealtimeNanos()-job.began)/1000000;
                    job.played=Integer.toUnsignedLong(output.getPlaybackHeadPosition());}
            }
            job.decodedComplete=true;StringBuilder submittedHash=new StringBuilder(64);for(byte b:submittedDigest.digest())submittedHash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));job.submittedSha256=submittedHash.toString();
            if(!job.decodedSha256.equals(job.submittedSha256))throw new IOException("Original decoded/submitted voice PCM differs");
            while(job.played<decoded.frames){ready(job,audio);job.played=Integer.toUnsignedLong(audio.getPlaybackHeadPosition());job.underruns=audio.getUnderrunCount();SystemClock.sleep(5);}
        }catch(IOException|RuntimeException error){if(!job.cancel.get()){job.error=error.toString();android.util.Log.e("PcVoice","Original voice stream failed",error);}}
        finally {
            synchronized(controls){job.audio=null;if(audio!=null){job.underruns=audio.getUnderrunCount();try{audio.pause();audio.flush();}catch(IllegalStateException ignored){}audio.release();}}
            if(pcm!=null){File partial=new File(pcm.getPath()+".part");if(partial.exists()&&!partial.delete())android.util.Log.w("PcVoice","Own partial PCM retained");if(pcm.exists()&&!pcm.delete())android.util.Log.w("PcVoice","Own PCM retained");}
            if(directory!=null&&directory.exists()&&!directory.delete())android.util.Log.w("PcVoice","Own cache directory retained");job.released=true;
            main.post(()->{if(current==job)current=null;if(!closed)listener.finished(job.directive);});
        }
    }
    @Override public void close(){ui();if(closed)return;closed=true;stop();worker.shutdown();}
}
