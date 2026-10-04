package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.runtime.GameSession;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual packaged Android codec, frame-loop bytes and Save/RNG guards; not normal scene playback acceptance. */
public final class MusicSourceInstrumentation extends Instrumentation {
    private int checks;
    private Bundle arguments;
    private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args==null?new Bundle():args;start();}
    @Override public void onStart(){Bundle result=new Bundle();JSONArray tracks=new JSONArray();AtomicReference<GameSession> game=new AtomicReference<>();byte[][] saved={null};File directory=new File(getTargetContext().getCacheDir(),"music-source-probe-"+android.os.SystemClock.elapsedRealtimeNanos());
        try{
            check(directory.mkdir(),"fresh own media probe cache");
            runOnMainSync(()->{try{game.set(new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L)));saved[0]=game.get().captureSave();}catch(Exception error){throw new IllegalStateException(error);}});
            PcMusicCatalog catalog=new PcMusicCatalog(getTargetContext());check(catalog.tracks().size()==30,"all30 original packaged sources");
            for(var track:catalog.tracks().values()){
                if(arguments.containsKey("resource")&&Integer.parseInt(arguments.getString("resource"))!=track.resourceId)continue;
                File pcm=new File(directory,track.resourceId+".pcm");PcVorbisDecoder.Result decoded=PcVorbisDecoder.decode(getTargetContext(),track,pcm,new AtomicBoolean());
                check(decoded.frames==track.frames&&decoded.bytes==track.frames*track.channels*2,"actual Android original timeline "+track.resourceId);
                long start=Math.max(0,track.frames-256);byte[] ending=new byte[1024],looping=new byte[1024];
                try(PcMusicFrameStream stream=new PcMusicFrameStream(pcm,track.frames,track.channels,track.loopStart,track.loopEnd,true,start)){
                    check(stream.read(ending)==1024&&stream.position()==track.frames,"last original256 frames "+track.resourceId);
                    check(stream.read(looping)==1024&&stream.loops()==1&&stream.position()==(track.loopStart<0?0:track.loopStart)+256,"exact original loop start "+track.resourceId);
                }
                try(java.io.RandomAccessFile original=new java.io.RandomAccessFile(pcm,"r")){
                    byte[] expected=new byte[1024];original.seek(start*4);original.readFully(expected);check(Arrays.equals(expected,ending),"unmodified original ending "+track.resourceId);
                    original.seek((track.loopStart<0?0:track.loopStart)*4);original.readFully(expected);check(Arrays.equals(expected,looping),"unmodified original repeat bytes "+track.resourceId);
                }
                tracks.put(new JSONObject().put("resourceId",track.resourceId).put("musicId",track.musicId).put("oggSha256",track.oggSha256).put("referencePcmSha256",track.referencePcmSha256)
                    .put("actualPcmSha256",decoded.pcmSha256).put("referenceByteEqual",decoded.referenceByteEqual).put("frames",decoded.frames).put("bytes",decoded.bytes).put("codec",decoded.codec).put("decodeMillis",decoded.elapsedMillis).put("loopStart",track.loopStart).put("loopEnd",track.loopEnd));
                if(Boolean.parseBoolean(arguments.getString("retainPcm","false")))Files.copy(pcm.toPath(),getTargetContext().getFilesDir().toPath().resolve("music-source-"+track.resourceId+".pcm"));
                check(pcm.delete(),"release own temporary PCM cache "+track.resourceId);
            }
            runOnMainSync(()->{try{check(Arrays.equals(saved[0],game.get().captureSave()),"all decoder/cache/loop work preserves full authority Save/RNG");}catch(Exception error){throw new IllegalStateException(error);}});
            check(tracks.length()==(arguments.containsKey("resource")?1:30),"requested original sources all processed");
            check(directory.delete(),"no own decode cache leak");
            result.putString("musicSource","MUSIC_SOURCE PASS checks="+checks+"; actual Android decoder and sample-loop reads; no scene/audio playback claim");
        }catch(Throwable error){result.putString("musicSource","FAIL "+android.util.Log.getStackTraceString(error));}
        finally{
            try{Files.write(getTargetContext().getFilesDir().toPath().resolve("music-source.json"),new JSONObject().put("scope","Packaged codec/frame-loop proof, not scene playback").put("checks",checks).put("tracks",tracks).put("result",result.getString("musicSource")).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception error){result.putString("musicSource","FAIL evidence "+error);}
            runOnMainSync(()->{if(game.get()!=null)game.get().close();});
            File[] own=directory.listFiles();if(own!=null)for(File file:own)if(!file.delete())android.util.Log.w("PcAudio","Probe cache retained "+file);if(directory.exists()&&!directory.delete())android.util.Log.w("PcAudio","Probe directory retained "+directory);
        }
        finish(result.getString("musicSource").startsWith("MUSIC_SOURCE PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
