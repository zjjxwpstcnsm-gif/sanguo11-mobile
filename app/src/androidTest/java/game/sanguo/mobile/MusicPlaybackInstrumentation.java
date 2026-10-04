package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.nio.file.Files;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;

/** Normal3D host + explicit original resource directive; not original normal-scene binding acceptance. */
public final class MusicPlaybackInstrumentation extends SceneInstrumentation {
    private PcMusicStreamPlayer player;
    private AudioManager manager;
    private AudioFocusRequest request;
    private final JSONArray observations=new JSONArray();
    private byte[] capture()throws Exception{byte[][] value={null};runOnMainSync(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception error){throw new IllegalStateException(error);}});return value[0];}
    private PcMusicStreamPlayer.Status status(){PcMusicStreamPlayer.Status[] value={null};runOnMainSync(()->value[0]=player.status());return value[0];}
    private void observe(String label)throws Exception{var s=status();observations.put(new JSONObject().put("label",label).put("wallMillis",SystemClock.elapsedRealtime()).put("resourceId",s.resourceId)
        .put("submittedFrames",s.submittedFrames).put("playedFrames",s.playedFrames).put("loops",s.loops).put("firstWriteMillis",s.firstWriteMillis).put("active",s.active).put("released",s.released).put("error",s.error));}
    private void await(java.util.function.Predicate<PcMusicStreamPlayer.Status> condition,long timeout,String label)throws Exception{
        long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){var s=status();if(!s.error.isEmpty())throw new AssertionError(s.error);if(condition.test(s)){check(true,label);return;}SystemClock.sleep(25);}throw new AssertionError(label+" timed out");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");ready();byte[] before=capture();
        runOnMainSync(()->{
            player=new PcMusicStreamPlayer(getTargetContext());manager=getTargetContext().getSystemService(AudioManager.class);
            request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener(change->player.focus(change==AudioManager.AUDIOFOCUS_GAIN||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK),new Handler(Looper.getMainLooper())).build();
            player.foreground(true);player.volume(.75f);player.focus(manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED,false);
            player.play("probe-established3d:original-resource2261",24,true,500);
        });
        await(s->s.playedFrames>0,10000,"actual AudioTrack playback head advances");
        check(status().firstWriteMillis<2000,"first original PCM submitted before whole song decode");observe("first-play");
        long first=status().firstWriteMillis;
        runOnMainSync(()->player.play("probe-established3d:original-resource2261",24,true,500));SystemClock.sleep(350);
        check(status().firstWriteMillis==first,"same established source directive never restarts");
        await(s->s.loops>=2,18000,"two actual original full-track repeat boundaries");observe("two-loops");
        runOnMainSync(()->player.paused(true));SystemClock.sleep(250);long paused=status().playedFrames;SystemClock.sleep(350);
        check(status().playedFrames==paused,"paused music head stays fixed");observe("paused");
        runOnMainSync(()->player.paused(false));await(s->s.playedFrames>paused+4000,3000,"resume advances existing stream once");
        runOnMainSync(()->player.voiceActive(true));SystemClock.sleep(600);observe("voice-duck");runOnMainSync(()->player.voiceActive(false));
        runOnMainSync(()->player.muted(true));SystemClock.sleep(650);observe("muted");
        long muted=status().playedFrames;runOnMainSync(()->player.muted(false));await(s->s.playedFrames>muted+4000,3000,"unmute continues same position");
        runOnMainSync(()->{player.foreground(false);player.focus(false,false);manager.abandonAudioFocusRequest(request);});SystemClock.sleep(250);
        long background=status().playedFrames;SystemClock.sleep(350);check(status().playedFrames==background,"background relinquishes stream playback");observe("background");
        runOnMainSync(()->{player.foreground(true);player.focus(manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED,false);});await(s->s.playedFrames>background+4000,3000,"foreground resumes same stream position");
        check(status().firstWriteMillis==first&&Arrays.equals(before,capture()),"media lifecycle does not restart or change full Save/RNG");
        runOnMainSync(()->player.stop());await(s->s.released,3000,"explicit music stop releases track/codec/cache");observe("stopped");
        runOnMainSync(()->player.play("probe-new-established3d:original-resource2261",24,false,500));await(s->s.playedFrames>0,5000,"new established scene starts fresh source stream");
        await(s->s.released,12000,"source one-shot drains original final PCM and releases");observe("one-shot-ended");
        check(status().loops==0&&status().playedFrames>=283136&&Arrays.equals(before,capture()),"one-shot exact ending and complete Save/RNG retained");
        runOnMainSync(()->{player.close();manager.abandonAudioFocusRequest(request);});
        FileCheck.noStreamCache(this);result.putString("stream","MUSIC_PLAYBACK PASS "+checks+" checks; actual AudioTrack in normal3D with explicit original resource test adapter; normal-scene binding/shared focus pending\n");
    }catch(Throwable error){result.putString("stream","FAIL MUSIC_PLAYBACK "+android.util.Log.getStackTraceString(error));}
    finally{
        try{if(player!=null)runOnMainSync(()->{player.close();if(manager!=null&&request!=null)manager.abandonAudioFocusRequest(request);});finishActivityForRestore();
            Files.write(getTargetContext().getFilesDir().toPath().resolve("music-playback.json"),new JSONObject().put("scope","Actual source stream test adapter, not original scene binding").put("observations",observations).put("result",result.getString("stream")).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(Exception error){result.putString("stream","FAIL teardown/evidence "+error);}
    }
    finish(Activity.RESULT_OK,result);}
    private static final class FileCheck {
        static void noStreamCache(MusicPlaybackInstrumentation probe){java.io.File[] files=probe.getTargetContext().getCacheDir().listFiles(file->file.getName().startsWith("pc-music-stream-"));probe.check(files!=null&&files.length==0,"all own source stream caches released");}
    }
}
