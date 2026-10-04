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
import game.sanguo.api.*;
import game.sanguo.core.*;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Normal3D host/actual receipt + explicitly chosen original test profile/track. No original PATROL/BGM binding claim. */
public final class SharedMediaInstrumentation extends SceneInstrumentation {
    private SoundEffects sounds;private PcMediaPlayback media;private final JSONArray observations=new JSONArray();
    private PcVoiceDirective directive;private GameEvent parent;private PortraitMediaIdentity speaker;private int voiceType,profile;
    private byte[] capture()throws Exception{byte[][] value={null};runOnMainSync(()->{try{value[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new IllegalStateException(e);}});return value[0];}
    private PcVoiceStreamPlayer.Status voiceStatus(){PcVoiceStreamPlayer.Status[] s={null};runOnMainSync(()->s[0]=media.voiceStatus());return s[0];}
    private PcMusicStreamPlayer.Status musicStatus(){PcMusicStreamPlayer.Status[] s={null};runOnMainSync(()->s[0]=media.musicStatus());return s[0];}
    private void await(java.util.function.BooleanSupplier condition,long timeout,String label){long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(condition.getAsBoolean()){check(true,label);return;}SystemClock.sleep(25);}throw new AssertionError(label+" timed out");}
    private PcVoiceDirective plan(String id,int priority){return new PcVoiceDirective(parent.state,parent.id+":source-probe:"+id,parent.id,parent.id+":declared-probe-phase",speaker,voiceType,profile,0,false,priority);}
    private boolean admit(PcVoiceDirective d){boolean[] result={false};runOnMainSync(()->result[0]=media.sourceVoice(d));return result[0];}
    private void observe(String label)throws Exception {
        var v=voiceStatus();var m=musicStatus();int[] starts={0};runOnMainSync(()->starts[0]=media.voiceStarts());
        observations.put(new JSONObject().put("label",label).put("wallMillis",SystemClock.elapsedRealtime()).put("voiceStarts",starts[0])
            .put("voiceId",v==null?-1:v.directive.nativeVoiceId).put("voiceFactId",v==null?"":v.directive.id).put("parentId",v==null?"":v.directive.parentId)
            .put("presentationParentId",v==null?"":v.directive.presentationParentId).put("voiceSubmittedFrames",v==null?0:v.submittedFrames).put("voicePlayedFrames",v==null?0:v.playedFrames)
            .put("voiceFirstWriteMillis",v==null?-1:v.firstWriteMillis).put("voiceReleased",v==null||v.released).put("voiceError",v==null?"":v.error)
            .put("voiceUnderruns",v==null?0:v.underruns).put("voiceDecodedSha256",v==null?"":v.decodedSha256).put("voiceSubmittedSha256",v==null?"":v.submittedSha256)
            .put("musicPlayedFrames",m==null?0:m.playedFrames).put("musicFirstWriteMillis",m==null?-1:m.firstWriteMillis).put("musicReleased",m==null||m.released));
    }
    @Override public void onStart(){Bundle result=new Bundle();try {
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");ready();
        byte[] initial=capture();World reference=SaveCodec.decode(initial);Map<Integer,PortraitMediaIdentity> identities=PortraitSavedSources.read(reference.extensions.get(PcOfficerInfo.NAMESPACE),names(reference));
        World.City city=null;World.Officer officer=null;
        outer:for(var c:reference.cities)if(c.owner==reference.player)for(var o:reference.idle(c))if(identities.containsKey(o.id)){World copy=SaveCodec.decode(initial);if(copy.patrol(c.id,o.id).ok){city=c;officer=o;break outer;}}
        check(city!=null&&officer!=null,"real saved source campaign has an available approved patrol actor");speaker=identities.get(officer.id);
        PcVoiceCatalog catalog=new PcVoiceCatalog(getTargetContext());voiceType=catalog.voiceType(speaker);check(voiceType>=0,"actual approved source identity provides original actor type");
        long longest=-1;for(int p=0;p<71;p++){int id=PcVoicePolicy.feedbackAVoice(p,voiceType,true,0);var source=catalog.voice(id,false);if(source.frames>longest){longest=source.frames;profile=p;}}
        check(longest>44100,"explicit original test profile has enough source PCM for lifecycle observation");
        final int c=city.id,o=officer.id;CommandResult[] command={null};
        runOnMainSync(()->{sounds=((GameApplication)activity.getApplication()).sounds();sounds.muted(false);sounds.volume(75);sounds.musicVolume(75);sounds.voiceVolume(75);media=sounds.sourceMedia();
            var session=((GameApplication)activity.getApplication()).host().session();command[0]=session.execute(new GameCommand(GameCommand.Operation.PATROL,session.state(),c,o));});
        check(command[0].ok()&&command[0].event!=null,"actual session emitted committed parent before source test adapter");parent=command[0].event;
        check(reference.patrol(c,o).ok&&Arrays.equals(SaveCodec.encode(reference),capture()),"only actual patrol changes rules; complete reference Save/RNG matches");byte[] committed=capture();
        await(()->{boolean[] prepared={false};runOnMainSync(()->prepared[0]=media.voicePrepared());return prepared[0];},10000,"original voice catalog prewarmed off UI thread");
        runOnMainSync(()->media.sourceMusic("probe-established3d:source2261",24,true,0));await(()->musicStatus()!=null&&musicStatus().playedFrames>44100,10000,"shared owner original music advances");observe("music-alone");SystemClock.sleep(1200);
        check((Integer)field(sounds,"focusKind")==AudioManager.AUDIOFOCUS_GAIN,"one persistent shared music focus request");
        directive=plan("first",1);check(admit(directive),"actual receipt membership admits explicit original profile test adapter");await(()->voiceStatus()!=null&&voiceStatus().firstWriteMillis>=0,10000,"original voice PCM submitted on background worker");observe("voice-duck");
        check(voiceStatus().firstWriteMillis<1000,"prepared original voice first PCM latency below1s");
        check(!admit(directive),"duplicate fact never replays");SystemClock.sleep(700);runOnMainSync(()->sounds.pauseEffects(true));SystemClock.sleep(250);long head=voiceStatus().playedFrames;SystemClock.sleep(350);check(voiceStatus().playedFrames==head,"animation pause holds current voice position");observe("voice-paused");
        runOnMainSync(()->sounds.pauseEffects(false));await(()->voiceStatus().playedFrames>head,3000,"voice resumes existing source position once");
        await(()->voiceStatus().released,10000,"original voice drains exact source and releases");check(voiceStatus().error.isEmpty(),"original voice has no decoder/AudioTrack error");observe("voice-ended");SystemClock.sleep(900);
        PcVoiceDirective lower=plan("lower",0),higher=plan("higher",3);check(admit(lower),"low-priority original source accepted");await(()->voiceStatus()!=null&&voiceStatus().directive==lower&&voiceStatus().firstWriteMillis>=0,5000,"low-priority PCM begins");check(admit(higher),"higher-priority source preempts");await(()->voiceStatus().directive==higher&&voiceStatus().firstWriteMillis>=0,5000,"only higher source becomes current after old release");observe("priority-preempt");
        runOnMainSync(()->media.discardPresentation(higher.presentationParentId));await(()->voiceStatus().released,3000,"skip discards current and queued transient sources");check(!admit(higher),"skipped fact ID stays consumed");
        runOnMainSync(()->sounds.muted(true));PcVoiceDirective silent=plan("muted",1);check(!admit(silent),"master mute suppresses source voice");SystemClock.sleep(500);observe("master-muted");runOnMainSync(()->sounds.muted(false));check(!admit(silent),"unmute cannot replay old muted receipt");
        AudioManager manager=activity.getSystemService(AudioManager.class);AudioFocusRequest competing=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(x->{},new Handler(Looper.getMainLooper())).build();
        int[] granted={0};runOnMainSync(()->granted[0]=manager.requestAudioFocus(competing));check(granted[0]==AudioManager.AUDIOFOCUS_REQUEST_GRANTED,"competing real focus granted");SystemClock.sleep(450);long musicHead=musicStatus().playedFrames;SystemClock.sleep(350);check(musicStatus().playedFrames==musicHead,"real focus loss pauses long stream");observe("focus-lost");runOnMainSync(()->manager.abandonAudioFocusRequest(competing));await(()->musicStatus().playedFrames>musicHead+3000,5000,"focus gain resumes established music");
        check(Arrays.equals(committed,capture()),"all source playback/queue/lifecycle checks leave complete Save/RNG unchanged");
        try(var descriptor=getUiAutomation().executeShellCommand("su 1000 am broadcast -a android.media.AUDIO_BECOMING_NOISY");var input=new java.io.FileInputStream(descriptor.getFileDescriptor())){byte[] data=new byte[2048];while(input.read(data)!=-1){}}
        await(()->{try{return (Boolean)field(sounds,"focusLost");}catch(Exception e){throw new IllegalStateException(e);}},3000,"actual system-UID noisy broadcast reaches registered receiver");
        SystemClock.sleep(300);long noisyHead=musicStatus().playedFrames;SystemClock.sleep(300);check(musicStatus().playedFrames==noisyHead,"noisy event leaves established music paused");observe("system-noisy");
        boolean[] resumed={false};runOnMainSync(()->resumed[0]=sounds.resumeEstablishedMusic());check(resumed[0],"explicit established music resume reacquires one shared focus");await(()->musicStatus().playedFrames>noisyHead+3000,5000,"noisy resume continues original position");
        runOnMainSync(()->sounds.foreground(activity,false));SystemClock.sleep(300);long background=musicStatus().playedFrames;SystemClock.sleep(300);check(musicStatus().playedFrames==background,"background pauses same music track");observe("background");runOnMainSync(()->sounds.foreground(activity,true));await(()->musicStatus().playedFrames>background+3000,5000,"foreground resumes same track without voice replay");
        runOnMainSync(()->media.stopMusic());await(()->musicStatus().released,3000,"long source stopped/released");observe("stopped");
        check(Arrays.equals(committed,capture()),"full authority unchanged after all media lifecycle work");result.putString("sharedMedia","SHARED_MEDIA PASS checks="+checks+"; actual receipt/shared focus/music+voice PCM; explicit test profile/track, original normal bindings pending");
    }catch(Throwable e){result.putString("sharedMedia","FAIL "+android.util.Log.getStackTraceString(e));}
    finally {
        try{finishActivityForRestore();Files.write(getTargetContext().getFilesDir().toPath().resolve("shared-media.json"),new JSONObject().put("scope","Normal3D host + actual PATROL receipt + explicit original profile/track test adapter; not original PATROL or normal BGM binding")
            .put("speakerOfficerId",speaker==null?-1:speaker.officerId).put("nativeProfile",profile).put("voiceTypeRaw",voiceType).put("observations",observations).put("result",result.getString("sharedMedia")).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        catch(Exception e){result.putString("sharedMedia","FAIL teardown "+e);}
    }
    finish(result.getString("sharedMedia").startsWith("SHARED_MEDIA PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    private static Map<Integer,String> names(World world){Map<Integer,String> result=new HashMap<>();for(var o:world.officers)result.put(o.id,o.name);return result;}
}
