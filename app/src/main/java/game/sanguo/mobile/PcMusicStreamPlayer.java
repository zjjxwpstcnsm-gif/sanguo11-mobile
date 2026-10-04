package game.sanguo.mobile;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original music stream. Owner supplies verified scene selection, shared focus and lifecycle; no rules/save/RNG. */
final class PcMusicStreamPlayer implements AutoCloseable {
    static final class Status {
        final String identity,error;
        final int resourceId;
        final long submittedFrames,playedFrames,loops,firstWriteMillis;
        final boolean active,released;
        Status(Job job){identity=job.identity;error=job.error;resourceId=job.resourceId;submittedFrames=job.submitted;playedFrames=job.played;loops=job.loops;firstWriteMillis=job.firstWriteMillis;active=job.audio!=null;released=job.released;}
    }
    private static final class Job {
        final String identity;
        final int musicId,resourceId,fadeMillis;
        final boolean repeat;
        final long began;
        final AtomicBoolean cancel=new AtomicBoolean();
        volatile AudioTrack audio;
        volatile String error="";
        volatile long submitted,played,loops,firstWriteMillis=-1;
        volatile boolean released;
        long previousHead,headBase;
        Job(String identity,int id,boolean repeat,int fade){this.identity=identity;musicId=id;resourceId=2237+id;this.repeat=repeat;fadeMillis=fade;began=SystemClock.elapsedRealtime();}
    }
    private final Context context;
    private final Object controls=new Object();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"PcMusicStream"));
    private volatile boolean foreground,focused,paused,muted,closed;
    private volatile float gain=.75f,focusDuck=1,voiceDuck=1;
    private volatile Job current,last;
    PcMusicStreamPlayer(Context context){this.context=context.getApplicationContext();}
    private static void ui(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Serial media owner thread required");}
    /** Identity includes the source scene/generation. Same established scene/track never restarts at refresh. */
    void play(String identity,int nativeMusicId,boolean repeat,int sourceFadeMillis){
        ui();if(closed)throw new IllegalStateException("Closed music player");
        if(identity==null||identity.isEmpty()||nativeMusicId<0||nativeMusicId>=30||sourceFadeMillis<0||sourceFadeMillis>10000)throw new IllegalArgumentException("Unverified/invalid music directive");
        Job previous=current;
        if(previous!=null&&!previous.cancel.get()&&previous.identity.equals(identity)&&previous.musicId==nativeMusicId&&previous.repeat==repeat)return;
        stop();Job job=new Job(identity,nativeMusicId,repeat,sourceFadeMillis);current=last=job;worker.execute(()->run(job));
    }
    void foreground(boolean value){ui();foreground=value;changed();}
    void focus(boolean granted,boolean ducked){ui();focused=granted;focusDuck=ducked?.2f:1;changed();}
    void paused(boolean value){ui();paused=value;changed();}
    void volume(float value){ui();if(!Float.isFinite(value))throw new IllegalArgumentException("gain");gain=Math.max(0,Math.min(1,value));changed();}
    void muted(boolean value){ui();muted=value;changed();}
    void voiceActive(boolean value){ui();voiceDuck=value?.35f:1;changed();}
    private void changed(){
        synchronized(controls){
            Job job=current;AudioTrack audio=job==null?null:job.audio;
            if(audio!=null){try{audio.setVolume(muted?0:gain*focusDuck*voiceDuck);if(!foreground||!focused||paused)audio.pause();}catch(IllegalStateException ignored){}}
            controls.notifyAll();
        }
    }
    void stop(){ui();Job job=current;current=null;
        if(job!=null){job.cancel.set(true);synchronized(controls){AudioTrack audio=job.audio;if(audio!=null)try{audio.pause();audio.flush();}catch(IllegalStateException ignored){}controls.notifyAll();}}
    }
    Status status(){ui();Job job=last;return job==null?null:new Status(job);}
    private static void cancel(Job job)throws IOException{if(job.cancel.get()||Thread.currentThread().isInterrupted())throw new IOException("Music stream cancelled");}
    private void head(Job job,AudioTrack audio){if(job.cancel.get())return;long head=Integer.toUnsignedLong(audio.getPlaybackHeadPosition());if(head<job.previousHead)job.headBase+=1L<<32;job.previousHead=head;job.played=job.headBase+head;}
    private void ready(Job job,AudioTrack audio)throws IOException {
        synchronized(controls){
            while(!foreground||!focused||paused){cancel(job);if(audio.getPlayState()==AudioTrack.PLAYSTATE_PLAYING)audio.pause();head(job,audio);try{controls.wait(30);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IOException("Music wait interrupted",error);}}
            cancel(job);audio.setVolume(muted?0:gain*focusDuck*voiceDuck);if(audio.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)audio.play();
        }
    }
    private void write(Job job,AudioTrack audio,byte[] buffer,int length)throws IOException {
        int offset=0;
        while(offset<length){ready(job,audio);
            if(job.fadeMillis>0){long start=job.firstWriteMillis<0?SystemClock.elapsedRealtime():job.began+job.firstWriteMillis;float ramp=Math.min(1,(SystemClock.elapsedRealtime()-start)/(float)job.fadeMillis);audio.setVolume(muted?0:gain*focusDuck*voiceDuck*ramp);}
            int count=audio.write(buffer,offset,length-offset,AudioTrack.WRITE_NON_BLOCKING);
            if(count<0)throw new IOException("Original music AudioTrack error "+count);
            if(count==0){SystemClock.sleep(5);cancel(job);continue;}
            if(count%4!=0)throw new IOException("AudioTrack accepted incomplete original stereo frame");
            if(job.firstWriteMillis<0)job.firstWriteMillis=SystemClock.elapsedRealtime()-job.began;
            offset+=count;job.submitted+=count/4;head(job,audio);
        }
    }
    private void run(Job job){File directory=null,pcm=null;AudioTrack audio=null;
        try{
            cancel(job);PcMusicCatalog.Track track=new PcMusicCatalog(context).track(job.musicId);if(track==null)throw new IOException("Unknown original music ID");
            directory=new File(context.getCacheDir(),"pc-music-stream-"+job.began+"-"+job.musicId);if(!directory.mkdir())throw new IOException("Fresh own stream cache required");pcm=new File(directory,"original.pcm");
            int minimum=AudioTrack.getMinBufferSize(track.sampleRate,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);if(minimum<=0)throw new IOException("Unsupported original audio format");
            audio=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(track.sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(Math.max(minimum,16384)).build();
            if(audio.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("AudioTrack not initialized");job.audio=audio;
            AudioTrack output=audio;PcVorbisDecoder.decode(context,track,pcm,job.cancel,(bytes,length)->write(job,output,bytes,length));
            if(job.repeat){job.loops=1;long loop=track.loopStart<0?0:track.loopStart;
                try(PcMusicFrameStream stream=new PcMusicFrameStream(pcm,track.frames,track.channels,track.loopStart,track.loopEnd,true,loop)){
                    byte[] buffer=new byte[16384];while(true){cancel(job);int size=stream.read(buffer);write(job,audio,buffer,size);job.loops=1+stream.loops();}
                }
            }else{
                while(job.played<job.submitted){ready(job,audio);head(job,audio);SystemClock.sleep(5);cancel(job);}
            }
        }catch(IOException|RuntimeException error){if(!job.cancel.get()){job.error=error.toString();android.util.Log.e("PcMusic","Original stream failed",error);}}
        finally{
            synchronized(controls){job.audio=null;if(audio!=null){try{audio.pause();audio.flush();}catch(IllegalStateException ignored){}audio.release();}}
            if(pcm!=null){File partial=new File(pcm.getPath()+".part");if(partial.exists()&&!partial.delete())android.util.Log.w("PcMusic","Own interrupted PCM retained");if(pcm.exists()&&!pcm.delete())android.util.Log.w("PcMusic","Own PCM retained");}
            if(directory!=null&&directory.exists()&&!directory.delete())android.util.Log.w("PcMusic","Own stream directory retained");job.released=true;
        }
    }
    @Override public void close(){ui();if(closed)return;closed=true;stop();worker.shutdown();}
}
