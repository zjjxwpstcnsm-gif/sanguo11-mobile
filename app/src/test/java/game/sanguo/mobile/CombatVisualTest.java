package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.security.MessageDigest;

public final class CombatVisualTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static World clone(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static void main(String[] args)throws Exception{
        for(String kind:new String[]{"melee","arrow","stone","charge","fire","critical-fire","trap-seed","trap-ball","trap-ship","lightning","critical","counter","defeat","enemy","facilities","site"})run(kind);
        for(int i=0;i<6;i++){SceneMesh m=CombatVisual.mesh(i);check(m.indices.length>0,"effect geometry");for(float v:m.vertices)check(Float.isFinite(v),"finite asset");}
        System.out.println("PASS S06 "+checks+" combat timeline, read-only deltas, pools, real commands and state-hash checks");
    }
    static void run(String kind)throws Exception{
        World initial=clone(CombatSceneFixture.world(kind));World reference=clone(initial);CombatSceneFixture.action(reference,kind);
        byte[] expected=SaveCodec.encode(reference);int events=-1;
        if(kind.equals("critical-fire")){
            check(reference.war.fires().stream().anyMatch(f->f.remaining==3),"critical fire lasts three turns");
            check(Arrays.equals(expected,SaveCodec.encode(clone(reference))),"critical fire full save roundtrip");
        }
        for(int speed:new int[]{1,2,4,0}){
            World w=clone(initial),visual=clone(initial);TurnJournal journal=new TurnJournal(w);
            CombatSceneFixture.action(w,kind);journal.close();
            if(kind.equals("fire")||kind.startsWith("trap")&&!kind.equals("trap-ship"))check(!w.war.fires().isEmpty(),"actual fire duration recorded");
            if(kind.equals("trap-ship"))check(w.war.fires().isEmpty(),"core ship explosion creates no persistent water fire");
            if(kind.startsWith("trap"))check(w.war.structures().isEmpty(),"actual trap detonation removed structure");
            if(events<0)events=journal.events().size();check(events==journal.events().size(),"business event count independent of playback");
            check(Arrays.equals(expected,SaveCodec.encode(w)),"recording equality "+kind);
            MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(visual);CombatVisual pool=new CombatVisual();
            boolean visible=false,removed=false;
            for(TurnJournal.Event e:journal.events()){
                if(kind.equals("counter"))check(e.strikes.size()>=2&&e.strikes.get(0).actorId==1&&e.strikes.get(1).actorId==2,"ordered real retaliation payload");
                check(e.sourceKey.isEmpty()||e.sourceKey.matches("[usc][0-9]+"),"stable source identity");
                for(TurnJournal.StateChange d:e.states){check(d.before!=null||d.after!=null,"nonempty delta");if(d.before!=null&&d.after==null)removed=true;}
                if(speed>0)for(int frame=0;frame<=100;frame+=speed){float f=frame/100f;pool.sample(e,f,ground);check(pool.count<=CombatVisual.CAPACITY,"bounded particle pool");visible|=pool.count>0;
                    for(int i=0;i<pool.count;i++){CombatVisual.Particle p=pool.particles[i];check(Float.isFinite(p.x+p.y+p.z+p.scale),"finite pose");}
                    // Repeat same fraction (pause/repaint) must not change any authoritative byte.
                    pool.sample(e,f,ground);
                }
                e.applyVisual(visual);pool.sample(null,0,ground);check(pool.count==0,"skip/release clears particles");
            }
            if(speed>0)check(visible,"visible native effect samples "+kind);
            if(kind.equals("defeat"))check(removed&&visual.unit(2)==null,"removed target historical snapshot survives until apply");
            for(World.Unit u:w.units){World.Unit shown=visual.unit(u.id);check(shown!=null&&shown.troops==u.troops&&shown.energy==u.energy&&shown.status==u.status,"historical display reaches final state");}
            check(Arrays.equals(expected,SaveCodec.encode(w)),"sampling cannot change RNG/resources/reports "+kind);
        }
        System.out.println("S06 case="+kind+" events="+events+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(expected)));
    }
}
