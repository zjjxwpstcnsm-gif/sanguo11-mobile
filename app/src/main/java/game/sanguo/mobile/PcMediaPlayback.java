package game.sanguo.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import game.sanguo.api.GameApi;
import game.sanguo.api.GameEvent;
import game.sanguo.api.StateToken;
import game.sanguo.runtime.GameSession;
import java.util.ArrayDeque;

/** App-owned source playback, actual subscribed receipt membership, shared focus and bounded transient voice queue. */
final class PcMediaPlayback implements AutoCloseable {
    interface Owner {boolean requestFocus();void voiceDucking(boolean active);void idle();}
    private final Context context;private final Owner owner;private final Handler main=new Handler(Looper.getMainLooper());
    private final MediaCueLedger ledger;private final ArrayDeque<PcVoiceDirective> pending=new ArrayDeque<>();
    private final PcMusicStreamPlayer music;private final PcVoiceStreamPlayer voice;
    private GameSession session;private GameApi.Subscription subscription;
    private PcVoiceDirective current;
    private boolean foreground,focused,ducked,paused,muted,closed,musicWanted,interrupted;
    private final Runnable musicEnd=this::checkMusicEnd;
    private int musicGain=75,voiceGain=75,started,dropped;
    PcMediaPlayback(Context context,Owner owner,MediaCueLedger ledger){
        this.context=context.getApplicationContext();this.owner=owner;this.ledger=ledger;music=new PcMusicStreamPlayer(context);
        voice=new PcVoiceStreamPlayer(context,new PcVoiceStreamPlayer.Listener(){
            public void started(PcVoiceDirective directive){if(closed||current!=directive)return;started++;music.voiceActive(true);owner.voiceDucking(true);android.util.Log.i("PcVoice","START id="+directive.id+" parent="+directive.parentId+" phase="+directive.presentationParentId+" officerId="+directive.speaker.officerId+" nativeId="+directive.speaker.nativeId+" variant="+directive.speaker.sourceVariant+" resource="+(2287+directive.nativeVoiceId+(directive.alternateRaw&&directive.nativeVoiceId>=5?996:0)));}
            public void finished(PcVoiceDirective directive){if(closed||current!=directive)return;current=null;music.voiceActive(false);owner.voiceDucking(false);pump();if(current==null&&!musicWanted)owner.idle();}
        });
    }
    private static void ui(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Serial source media owner required");}
    /** Called before arming source producers; snapshots establish a barrier and never manufacture playback. */
    void bind(GameSession next){
        ui();if(session==next)return;if(subscription!=null)subscription.close();subscription=null;session=next;stopTransient();stopMusic();
        if(next!=null){ledger.resynchronize(next.state());subscription=next.subscribe(this::observed);}
        else ledger.retire();
    }
    private void observed(GameEvent event){ui();if(ledger.observe(event)){stopTransient();stopMusic();}
        if(event.kind!=GameEvent.Kind.CLOSED&&(!foreground||muted||voiceGain==0||interrupted))ledger.resynchronize(event.state);
        if(ledger.needsResync())stopTransient();}
    /** Source facts must arrive through the bound session. Unknown facts are consumed without guessed playback. */
    boolean sourceMapMusic(PcMapMusicDirective directive){
        ui();if(closed||session==null||!directive.state.equals(session.state()))return false;
        if(!ledger.claim(directive.state,directive.id,directive.parentId)){if(ledger.needsResync()){stopTransient();stopMusic();}return false;}
        int selected=directive.selectedMusicId();
        if(selected==PcMusicPolicy.UNBOUND){stopMusic();android.util.Log.i("PcMusic","UNBOUND id="+directive.id+" parent="+directive.parentId);return false;}
        sourceMusic(directive.establishedScene,selected,true,500);
        android.util.Log.i("PcMusic","SOURCE5880e0 id="+directive.id+" parent="+directive.parentId+" phase="+directive.presentationParentId+" musicId="+selected);
        return true;
    }
    /** Native track selection and scene role must be proven by the producer. Current ordinary scene bindings remain pending. */
    void sourceMusic(String establishedScene,int nativeMusicId,boolean repeat,int fadeMillis){
        ui();if(closed||session==null||ledger.closed())throw new IllegalStateException("No established source media session");
        if(establishedScene==null||establishedScene.isEmpty()||nativeMusicId<0||nativeMusicId>=30||fadeMillis<0||fadeMillis>10000)throw new IllegalArgumentException("Unverified source music scene/directive");
        StateToken state=session.state();musicWanted=true;
        music.foreground(foreground);music.volume(musicGain/100f);music.muted(muted);
        if(foreground&&!muted&&musicGain>0)owner.requestFocus();
        music.play(state.sessionId+":"+state.generation+":"+establishedScene,nativeMusicId,repeat,fadeMillis);
        main.removeCallbacks(musicEnd);if(!repeat)main.postDelayed(musicEnd,100);
    }
    private void checkMusicEnd(){if(closed||!musicWanted)return;var status=music.status();if(status!=null&&status.released){musicWanted=false;owner.idle();}else main.postDelayed(musicEnd,100);}
    /** Only IDs whose parent arrived through this exact session subscription can schedule original voice PCM. */
    boolean sourceVoice(PcVoiceDirective directive){
        ui();if(closed)return false;if(!ledger.claim(directive.state,directive.id,directive.parentId)){if(ledger.needsResync())stopTransient();return false;}
        if(!foreground||muted||interrupted||voiceGain==0||!owner.requestFocus()){dropped++;return false;}
        if(pending.size()>=4){dropped++;ledger.overflow();stopTransient();return false;}
        pending.addLast(directive);
        if(current!=null&&directive.priority>current.priority){voice.stop();dropped++;}
        pump();return true;
    }
    private void pump(){
        if(closed||current!=null||pending.isEmpty()||paused||!foreground||!focused||muted||voiceGain==0)return;
        PcVoiceDirective best=null;for(var candidate:pending)if(best==null||candidate.priority>best.priority)best=candidate;
        pending.remove(best);current=best;voice.volume(voiceGain/100f,ducked);voice.play(best);
    }
    void foreground(boolean value){ui();foreground=value;music.foreground(value);if(!value)stopTransient();else if(musicWanted&&!muted&&musicGain>0)owner.requestFocus();}
    void focus(boolean value,boolean duck){ui();focused=value;ducked=duck;music.focus(value,duck);voice.volume(muted?0:voiceGain/100f,duck);if(!value)stopTransient();else pump();}
    void interrupted(boolean value){ui();interrupted=value;if(value){stopTransient();if(session!=null)ledger.resynchronize(session.state());}}
    void paused(boolean value){ui();paused=value;voice.paused(value);if(!value)pump();}
    void gains(int musicPercent,int voicePercent,boolean masterMuted){
        ui();musicGain=Math.max(0,Math.min(100,musicPercent));voiceGain=Math.max(0,Math.min(100,voicePercent));muted=masterMuted;
        music.volume(musicGain/100f);music.muted(masterMuted);voice.volume(masterMuted?0:voiceGain/100f,ducked);
        if(masterMuted||voiceGain==0){stopTransient();if(session!=null)ledger.resynchronize(session.state());}if(foreground&&musicWanted&&!masterMuted&&musicGain>0)owner.requestFocus();else if(!needsFocus())owner.idle();
    }
    void stopTransient(){ui();pending.clear();voice.stop();music.voiceActive(false);owner.voiceDucking(false);}
    void discardPresentation(String parent){ui();pending.removeIf(d->d.presentationParentId.equals(parent));if(current!=null&&current.presentationParentId.equals(parent))voice.stop();}
    void stopMusic(){ui();musicWanted=false;music.stop();main.removeCallbacksAndMessages(null);owner.idle();}
    boolean hasMusic(){return musicWanted;}
    boolean needsFocus(){return !muted&&(musicWanted&&musicGain>0||hasTransient()&&voiceGain>0);}
    boolean hasTransient(){return current!=null||!pending.isEmpty();}
    boolean needsResync(){return ledger.needsResync();}
    void resynchronize(){ui();stopTransient();if(session!=null)ledger.resynchronize(session.state());}
    int voiceStarts(){return started;}
    int drops(){return dropped;}
    int pendingVoices(){return pending.size();}
    boolean voicePrepared(){return voice.prepared();}
    long voiceCatalogMillis(){return voice.catalogMillis();}
    PcMusicStreamPlayer.Status musicStatus(){return music.status();}
    PcVoiceStreamPlayer.Status voiceStatus(){return voice.status();}
    @Override public void close(){ui();if(closed)return;closed=true;if(subscription!=null)subscription.close();subscription=null;session=null;stopTransient();stopMusic();music.close();voice.close();}
}
