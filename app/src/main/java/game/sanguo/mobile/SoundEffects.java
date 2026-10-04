package game.sanguo.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
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
    private PcPcmEffectPlayer pcEffects;
    private AudioFocusRequest focusRequest;
    private boolean focused,ducked,focusLost,effectsPaused;
    private boolean voiceDucking,noisyRegistered;
    private int focusKind,focusEpoch;
    private final MediaCueLedger sourceLedger=new MediaCueLedger(512);
    private PcMediaPlayback sourceMedia;
    private game.sanguo.runtime.GameSession effectSession;
    private game.sanguo.api.GameApi.Subscription effectSubscription;
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(i.getAction())){focusLost=true;if(sourceMedia!=null)sourceMedia.interrupted(true);stop();}}};
    private long uiAt;
    private int played;
    SoundEffects(Context context){this.context=context.getApplicationContext();prefs=context.getSharedPreferences("sound-effects",0);manager=context.getSystemService(AudioManager.class);}
    void attach(Object owner){owners.add(owner);load();bindEffectSession();if(!noisyRegistered){context.registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));noisyRegistered=true;}}
    private void bindEffectSession(){
        if(!(context instanceof GameApplication))return;game.sanguo.runtime.GameSession next=((GameApplication)context).host().session();
        if(effectSession==next)return;if(effectSubscription!=null)effectSubscription.close();effectSubscription=null;effectSession=next;
        if(next!=null)effectSubscription=next.subscribe(event->{if(event.kind==game.sanguo.api.GameEvent.Kind.WORLD_REPLACED||event.kind==game.sanguo.api.GameEvent.Kind.CLOSED)stopStreams();});
    }
    void detach(Object owner){foreground.remove(owner);owners.remove(owner);if(foreground.isEmpty()){if(sourceMedia!=null)sourceMedia.foreground(false);stop();}if(owners.isEmpty())release();}
    void foreground(Object owner,boolean active){if(active){if(foreground.isEmpty()){focusLost=false;if(sourceMedia!=null)sourceMedia.interrupted(false);}foreground.add(owner);load();bindEffectSession();if(sourceMedia!=null)sourceMedia.foreground(true);}else{foreground.remove(owner);if(foreground.isEmpty()){if(sourceMedia!=null)sourceMedia.foreground(false);stop();}}}
    int volume(){return Math.max(0,Math.min(100,prefs.getInt("volume",75)));}
    boolean muted(){return prefs.getBoolean("muted",false);}
    void volume(int value){prefs.edit().putInt("volume",Math.max(0,Math.min(100,value))).apply();syncSourceGains();if(volume()==0){stopStreams();if(sourceMedia==null||!sourceMedia.needsFocus())stop();}else updateStreams();}
    void muted(boolean value){prefs.edit().putBoolean("muted",value).apply();syncSourceGains();if(value)stop();}
    int musicVolume(){return Math.max(0,Math.min(100,prefs.getInt("music-volume",volume())));}
    int voiceVolume(){return Math.max(0,Math.min(100,prefs.getInt("voice-volume",volume())));}
    void musicVolume(int value){prefs.edit().putInt("music-volume",Math.max(0,Math.min(100,value))).apply();syncSourceGains();}
    void voiceVolume(int value){prefs.edit().putInt("voice-volume",Math.max(0,Math.min(100,value))).apply();syncSourceGains();}
    boolean resumeEstablishedMusic(){if(sourceMedia==null||!sourceMedia.hasMusic()||foreground.isEmpty()||muted()||musicVolume()==0)return false;focusLost=false;sourceMedia.interrupted(false);return focus();}
    private void syncSourceGains(){if(sourceMedia!=null)sourceMedia.gains(musicVolume(),voiceVolume(),muted());}
    /** No ordinary BGM/voice trigger is inferred here. Producers must supply established original bindings. */
    PcMediaPlayback sourceMedia(){
        if(sourceMedia==null){sourceMedia=new PcMediaPlayback(context,new PcMediaPlayback.Owner(){
            public boolean requestFocus(){return focus();}
            public void voiceDucking(boolean value){voiceDucking=value;updateStreams();}
            public void idle(){if(streams.isEmpty()&&(pcEffects==null||!pcEffects.active())&&sourceMedia!=null&&!sourceMedia.needsFocus())stop();}
        },sourceLedger);sourceMedia.gains(musicVolume(),voiceVolume(),muted());sourceMedia.foreground(!foreground.isEmpty());sourceMedia.focus(focused,ducked);}
        if(context instanceof GameApplication)sourceMedia.bind(((GameApplication)context).host().session());return sourceMedia;
    }
    private void load(){
        if(pool!=null)return;
        SoundPool candidate=new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();pool=candidate;
        candidate.setOnLoadCompleteListener((source,id,status)->{if(pool==source&&status==0)ready.add(id);else if(status!=0)Log.e("GameAudio","load failed sample="+id+" status="+status);});
        for(Cue cue:Cue.values())if(cue!=Cue.TECHNIQUE_GAIN&&cue!=Cue.TECHNIQUE_LOSS)
            try(var file=context.getAssets().openFd(asset(cue))){samples.put(cue,candidate.load(file,1));}catch(java.io.IOException e){Log.e("GameAudio","Missing audio "+cue,e);}
        pcEffects=new PcPcmEffectPlayer(context);
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
        if(focusLost)return false;
        int wanted=sourceMedia!=null&&sourceMedia.hasMusic()&&musicVolume()>0&&!muted()?AudioManager.AUDIOFOCUS_GAIN:AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
        if(focused&&focusKind==wanted)return true;
        if(manager==null)return false;
        if(focusRequest==null||focusKind!=wanted){if(focusRequest!=null)manager.abandonAudioFocusRequest(focusRequest);focusKind=wanted;int epoch=++focusEpoch;
        focusRequest=new AudioFocusRequest.Builder(wanted)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setOnAudioFocusChangeListener(change->{
                if(epoch!=focusEpoch)return;
                if(change==AudioManager.AUDIOFOCUS_GAIN){focused=true;focusLost=false;ducked=false;if(sourceMedia!=null)sourceMedia.interrupted(false);updateStreams();}
                else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){ducked=true;updateStreams();}
                else {focused=false;focusLost=true;stopStreams();if(sourceMedia!=null)sourceMedia.interrupted(true);}
                if(sourceMedia!=null)sourceMedia.focus(focused,ducked);
            },handler).build();}
        focused=manager.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(sourceMedia!=null)sourceMedia.focus(focused,ducked);return focused;
    }
    void pauseEffects(boolean value){effectsPaused=value;if(sourceMedia!=null)sourceMedia.paused(value);if(pcEffects!=null)pcEffects.paused(value||foreground.isEmpty()||!focused||muted());if(pool!=null)for(int id:battleStreams){if(value)pool.pause(id);else if(!foreground.isEmpty()&&focused&&!muted())pool.resume(id);}}
    private static boolean battleCue(Cue cue){return cue==Cue.MARCH||cue==Cue.ATTACK||cue==Cue.TACTIC||cue==Cue.CRITICAL||cue==Cue.PLOT||cue==Cue.TECHNIQUE_GAIN||cue==Cue.TECHNIQUE_LOSS;}
    private void expireStream(int stream,SoundPool owner){if(pool!=owner)return;if(effectsPaused&&battleStreams.contains(stream)){handler.postDelayed(()->expireStream(stream,owner),500);return;}streams.remove(stream);battleStreams.remove(stream);}
    private void play(Cue cue){
        if(cue==Cue.TECHNIQUE_GAIN||cue==Cue.TECHNIQUE_LOSS){
            if(effectsPaused||foreground.isEmpty()||muted()||volume()==0||pcEffects==null||!pcEffects.ready()||!focus())return;
            int stream=pcEffects.play(effectGain());if(stream!=0){played++;Log.i("GameAudio","PLAY cue="+cue+" pcmStream="+stream+" count="+played+" gain="+effectGain());}return;
        }
        Integer sample=samples.get(cue);
        if((effectsPaused&&battleCue(cue))||pool==null||foreground.isEmpty()||muted()||volume()==0||sample==null||!ready.contains(sample)||!focus())return;
        float gain=volume()/100f*(ducked?.2f:1f);if(voiceDucking)gain*=.7f;int stream=pool.play(sample,gain,gain,cue==Cue.CRITICAL?2:1,0,1);
        if(stream!=0){played++;streams.add(stream);if(battleCue(cue))battleStreams.add(stream);SoundPool owner=pool;handler.postDelayed(()->expireStream(stream,owner),1200);Log.i("GameAudio","PLAY cue="+cue+" stream="+stream+" count="+played+" gain="+gain+" focused="+focused+" focusKind="+focusKind+" ducked="+ducked+" voiceDucking="+voiceDucking+" sourceMedia="+(sourceMedia!=null));}
        else Log.w("GameAudio","play rejected cue="+cue);
    }
    private float effectGain(){return volume()/100f*(ducked?.2f:1f)*(voiceDucking?.7f:1f);}
    private void updateStreams(){float gain=effectGain();if(pcEffects!=null)pcEffects.volume(gain);if(pool!=null)for(int id:streams)pool.setVolume(id,gain,gain);}
    private void stopStreams(){if(pcEffects!=null)pcEffects.stop();if(pool!=null)for(int id:streams)pool.stop(id);streams.clear();battleStreams.clear();}
    private void stop(){stopStreams();focusEpoch++;if(manager!=null&&focusRequest!=null)manager.abandonAudioFocusRequest(focusRequest);focusRequest=null;focused=false;ducked=false;if(sourceMedia!=null)sourceMedia.focus(false,false);}
    private void release(){stop();if(effectSubscription!=null){effectSubscription.close();effectSubscription=null;}effectSession=null;if(pcEffects!=null){pcEffects.close();pcEffects=null;}if(sourceMedia!=null){sourceMedia.close();sourceMedia=null;}if(noisyRegistered){context.unregisterReceiver(noisy);noisyRegistered=false;}handler.removeCallbacksAndMessages(null);if(pool!=null){pool.release();pool=null;}ready.clear();samples.clear();Log.i("GameAudio","RELEASE");}
    int playedCount(){return played;}
    boolean loaded(){return samples.size()==Cue.values().length-2&&ready.containsAll(samples.values())&&pcEffects!=null&&pcEffects.ready();}
    boolean active(){return !foreground.isEmpty()&&pool!=null;}
}
