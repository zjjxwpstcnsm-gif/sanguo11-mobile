package game.sanguo.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.*;

/** Process presentation audio. Call only on the UI thread; no gameplay or rule RNG. */
final class SoundEffects {
    enum Cue { UI, MARCH, ATTACK, TACTIC, CRITICAL, PLOT, CONSTRUCTION, COMPLETE, TURN, TECHNIQUE_GAIN, TECHNIQUE_LOSS }
    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager manager;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<Object> owners=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Object> foreground=Collections.newSetFromMap(new IdentityHashMap<>());
    private final EnumMap<Cue,Integer> samples=new EnumMap<>(Cue.class);
    private final Set<Integer> ready=new HashSet<>(),streams=new HashSet<>(),battleStreams=new HashSet<>();
    private final LinkedHashSet<String> heard=new LinkedHashSet<>();
    private SoundPool pool;
    private AudioFocusRequest focusRequest;
    private boolean focused,ducked,focusLost,effectsPaused;
    private long uiAt;
    private int played;
    SoundEffects(Context context){this.context=context.getApplicationContext();prefs=context.getSharedPreferences("sound-effects",0);manager=context.getSystemService(AudioManager.class);}
    void attach(Object owner){owners.add(owner);load();}
    void detach(Object owner){foreground.remove(owner);owners.remove(owner);if(foreground.isEmpty())stop();if(owners.isEmpty())release();}
    void foreground(Object owner,boolean active){if(active){if(foreground.isEmpty())focusLost=false;foreground.add(owner);load();}else{foreground.remove(owner);if(foreground.isEmpty())stop();}}
    int volume(){return Math.max(0,Math.min(100,prefs.getInt("volume",75)));}
    boolean muted(){return prefs.getBoolean("muted",false);}
    void volume(int value){prefs.edit().putInt("volume",Math.max(0,Math.min(100,value))).apply();if(volume()==0)stop();else updateStreams();}
    void muted(boolean value){prefs.edit().putBoolean("muted",value).apply();if(value)stop();}
    private void load(){
        if(pool!=null)return;
        SoundPool candidate=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();pool=candidate;
        candidate.setOnLoadCompleteListener((source,id,status)->{if(pool==source&&status==0)ready.add(id);else if(status!=0)Log.e("GameAudio","load failed sample="+id+" status="+status);});
        for(Cue cue:Cue.values())try(var file=context.getAssets().openFd(asset(cue))){samples.put(cue,candidate.load(file,1));}catch(java.io.IOException e){Log.e("GameAudio","Missing audio "+cue,e);}
    }
    // Native HUD33 has one proved sample for changed values in either direction.
    // Other cues retain their explicitly labelled mobile compositions until source bindings close.
    private static String asset(Cue cue){return cue==Cue.TECHNIQUE_GAIN||cue==Cue.TECHNIQUE_LOSS
        ?"audio/pc/technique-33.wav":"audio/"+cue.name().toLowerCase(Locale.ROOT)+".wav";}
    void ui(){long now=android.os.SystemClock.uptimeMillis();if(now-uiAt<60)return;uiAt=now;play(Cue.UI);}
    /** A committed event phase is claimed once even when muted/backgrounded.
     * Loading/recreation never queues stale battle sounds for later replay. */
    void committed(game.sanguo.api.StateToken state,String phase,Cue cue){event("receipt:"+state.sessionId+":"+state.generation+":"+state.revision+":"+phase,cue);}
    void event(String identity,Cue cue){
        if(identity==null||!heard.add(identity))return;
        while(heard.size()>4096)heard.remove(heard.iterator().next());
        play(cue);
    }
    private boolean focus(){
        if(focusLost)return false;if(focused)return true;
        if(manager==null)return false;
        if(focusRequest==null)focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setOnAudioFocusChangeListener(change->{
                if(change==AudioManager.AUDIOFOCUS_GAIN){focused=true;focusLost=false;ducked=false;updateStreams();}
                else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){ducked=true;updateStreams();}
                else {focused=false;focusLost=true;stopStreams();}
            },handler).build();
        focused=manager.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;return focused;
    }
    void pauseEffects(boolean value){effectsPaused=value;if(pool!=null)for(int id:battleStreams){if(value)pool.pause(id);else if(!foreground.isEmpty()&&focused&&!muted())pool.resume(id);}}
    private static boolean battleCue(Cue cue){return cue==Cue.MARCH||cue==Cue.ATTACK||cue==Cue.TACTIC||cue==Cue.CRITICAL||cue==Cue.PLOT;}
    private void expireStream(int stream,SoundPool owner){if(pool!=owner)return;if(effectsPaused&&battleStreams.contains(stream)){handler.postDelayed(()->expireStream(stream,owner),500);return;}streams.remove(stream);battleStreams.remove(stream);}
    private void play(Cue cue){
        Integer sample=samples.get(cue);
        if((effectsPaused&&battleCue(cue))||pool==null||foreground.isEmpty()||muted()||volume()==0||sample==null||!ready.contains(sample)||!focus())return;
        float gain=volume()/100f*(ducked?.2f:1f);int stream=pool.play(sample,gain,gain,cue==Cue.CRITICAL?2:1,0,1);
        if(stream!=0){played++;streams.add(stream);if(battleCue(cue))battleStreams.add(stream);SoundPool owner=pool;handler.postDelayed(()->expireStream(stream,owner),1200);Log.i("GameAudio","PLAY cue="+cue+" stream="+stream+" count="+played);}
        else Log.w("GameAudio","play rejected cue="+cue);
    }
    private void updateStreams(){if(pool!=null){float gain=volume()/100f*(ducked?.2f:1f);for(int id:streams)pool.setVolume(id,gain,gain);}}
    private void stopStreams(){if(pool!=null)for(int id:streams)pool.stop(id);streams.clear();battleStreams.clear();}
    private void stop(){stopStreams();if(manager!=null&&focusRequest!=null)manager.abandonAudioFocusRequest(focusRequest);focused=false;ducked=false;}
    private void release(){stop();handler.removeCallbacksAndMessages(null);if(pool!=null){pool.release();pool=null;}ready.clear();samples.clear();Log.i("GameAudio","RELEASE");}
    int playedCount(){return played;}
    boolean loaded(){return ready.size()==Cue.values().length;}
    boolean active(){return !foreground.isEmpty()&&pool!=null;}
}
