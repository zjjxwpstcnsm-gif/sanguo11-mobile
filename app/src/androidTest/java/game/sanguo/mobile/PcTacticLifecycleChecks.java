package game.sanguo.mobile;

import android.media.AudioTrack;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Explicit lifecycle fixture; normal committed trigger/PCM evidence is separate. */
final class PcTacticLifecycleChecks {
    interface Owner { void ui(Runnable work); void check(boolean ok,String message); }
    private static Object field(Object object,String name)throws Exception{
        Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    static List<AudioTrack> run(SoundEffects sounds,Owner owner)throws Exception{
        PcTacticPcmPlayer player=(PcTacticPcmPlayer)field(sounds,"pcTactics");
        owner.ui(()->owner.check(player.ready()&&player.error().isEmpty(),"original tactic worker prepared six source tracks without error"));
        ArrayList<AudioTrack> tracks=new ArrayList<>();
        for(Object slot:(Iterable<?>)field(player,"slots"))tracks.add((AudioTrack)field(slot,"track"));
        owner.check(tracks.size()==6,"original tactic PCM pool remains bounded to six tracks");
        long bytes=0;for(AudioTrack track:tracks)bytes+=track.getBufferSizeInFrames()*2L;
        owner.check(bytes>=545964&&bytes<600000,"original tactic pool measured PCM buffers below 600000 bytes: "+bytes);
        int before=sounds.playedCount();owner.ui(()->sounds.originalTacticEvent("explicit-lifecycle-49",49));
        owner.check(sounds.playedCount()==before+1,"explicit original49 lifecycle fixture starts one track");
        owner.ui(()->sounds.originalTacticEvent("explicit-lifecycle-49",49));owner.check(sounds.playedCount()==before+1,"duplicate original49 phase never restarts source");
        SystemClock.sleep(80);owner.ui(()->sounds.pauseEffects(true));SystemClock.sleep(100);
        long[] held=new long[tracks.size()];owner.ui(()->{for(int i=0;i<tracks.size();i++)held[i]=Integer.toUnsignedLong(tracks.get(i).getPlaybackHeadPosition());});
        SystemClock.sleep(180);owner.ui(()->{long progress=0;for(int i=0;i<tracks.size();i++){long head=Integer.toUnsignedLong(tracks.get(i).getPlaybackHeadPosition());owner.check(head==held[i],"paused original tactic playback head is fixed");progress+=head;}owner.check(progress>0,"source49 actually advanced before pause");});
        owner.ui(()->sounds.originalTacticEvent("explicit-paused-78",78));owner.check(sounds.playedCount()==before+1,"paused original78 phase consumes identity without playback");
        owner.ui(()->sounds.pauseEffects(false));SystemClock.sleep(100);owner.ui(()->{long advance=0;for(int i=0;i<tracks.size();i++)advance+=Integer.toUnsignedLong(tracks.get(i).getPlaybackHeadPosition())-held[i];owner.check(advance>0,"original tactic resumes existing PCM position");});
        owner.ui(()->sounds.originalTacticEvent("explicit-paused-78",78));owner.check(sounds.playedCount()==before+1,"resume never replays consumed original78 phase");
        owner.ui(()->{sounds.muted(true);sounds.originalTacticEvent("explicit-muted-78",78);sounds.muted(false);sounds.originalTacticEvent("explicit-muted-78",78);});owner.check(sounds.playedCount()==before+1,"mute and unmute never defer a consumed source phase");
        owner.ui(()->{sounds.volume(0);sounds.originalTacticEvent("explicit-zero-49",49);sounds.volume(75);sounds.originalTacticEvent("explicit-zero-49",49);});owner.check(sounds.playedCount()==before+1,"zero gain never defers original49 playback");
        owner.ui(player::stop);return tracks;
    }
    static void released(SoundEffects sounds,List<AudioTrack> tracks,Owner owner)throws Exception{
        owner.check(field(sounds,"pcTactics")==null,"normal destroy clears original tactic owner");
        for(AudioTrack track:tracks)owner.check(track.getState()==AudioTrack.STATE_UNINITIALIZED,"normal destroy releases each original tactic AudioTrack");
    }
}
