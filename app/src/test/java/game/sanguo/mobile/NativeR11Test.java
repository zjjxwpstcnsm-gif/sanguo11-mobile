package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;
import java.security.MessageDigest;

public final class NativeR11Test {
    private static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static void main(String[] args)throws Exception{
        Set<TurnJournal.Kind> kinds=EnumSet.noneOf(TurnJournal.Kind.class);
        Set<CombatVisual.Style> styles=EnumSet.noneOf(CombatVisual.Style.class);
        for(String name:new String[]{"melee","arrow","stone","charge","fire","critical-fire","trap-seed","trap-ball","trap-ship","lightning","critical","counter","defeat","enemy","facilities","site"}){
            World initial=copy(CombatSceneFixture.world(name)),actual=copy(initial),reference=copy(initial);
            CombatSceneFixture.action(reference,name);byte[] expected=SaveCodec.encode(reference);
            TurnJournal journal=new TurnJournal(actual);CombatSceneFixture.action(actual,name);journal.close();
            check(!journal.events().isEmpty(),"real journal "+name);
            check(Arrays.equals(expected,SaveCodec.encode(actual)),"recording changes no state/RNG "+name);
            var ground=new MapSceneSnapshot.Ground(initial);var pool=new CombatVisual();
            for(var e:journal.events()){
                kinds.add(e.kind);styles.add(CombatVisual.style(e));check(e.sequence>0&&e.id.equals(e.journalId+":"+e.sequence),"stable event identity");
                if(name.equals("lightning"))check(e.plot==War.Plot.LIGHTNING,"typed lightning");
                if(name.equals("stone"))check(e.equipmentTactic==Army.Tactic.STONE,"typed equipment tactic");
                if(name.equals("fire"))check(e.plot==War.Plot.FIRE,"typed fire plot");
                for(var impact:e.impacts){
                    check(impact.metric!=TurnJournal.Metric.TEXT,"all feedback typed "+name);
                    if(impact.metric==TurnJournal.Metric.TROOPS)check(impact.amount<0&&!impact.entityKey.isEmpty(),"actual casualty identity");
                    String text=CombatVisual.feedback(e,impact);check(text!=null,"feedback mapping");
                }
                for(int quality=0;quality<3;quality++){
                    pool.detail(quality);
                    for(int frame=0;frame<101;frame++){
                        float f=frame/100f;pool.sample(e,f,ground);int count=pool.count;
                        float[] sample=new float[count*7];
                        for(int i=0;i<count;i++){var p=pool.particles[i];
                            check(p.mesh>=0&&p.mesh<CombatVisual.MESH_COUNT,"shared mesh index");
                            check(Float.isFinite(p.x+p.y+p.z+p.scale+p.yaw+p.pitch),"finite effects");
                            if(p.mesh==0||p.mesh==1&&CombatVisual.phase(e,f)<CombatVisual.HIT)check(p.y>=ground.surface.sample(p.x,p.z)+.35f,"projectile clears sampled terrain");
                            sample[i*7]=p.x;sample[i*7+1]=p.y;sample[i*7+2]=p.z;sample[i*7+3]=p.scale;sample[i*7+4]=p.yaw;sample[i*7+5]=p.pitch;sample[i*7+6]=p.mesh;
                        }
                        pool.sample(e,f,ground);check(count==pool.count,"pause/repaint count deterministic");
                        for(int i=0;i<count;i++){var p=pool.particles[i];check(sample[i*7]==p.x&&sample[i*7+1]==p.y&&sample[i*7+2]==p.z&&sample[i*7+3]==p.scale&&sample[i*7+4]==p.yaw&&sample[i*7+5]==p.pitch&&sample[i*7+6]==p.mesh,"pause/repaint exact pose");}
                        var snapshot=new MapSceneSnapshot(ground,reference,null,-1);
                        for(int i=0;i<200;i++)for(var fire:snapshot.fires)pool.fire(fire,ground,1000,true);
                        check(pool.count<=CombatVisual.CAPACITY,"saturated fire/effect pool bounded");
                    }
                }
            }
            for(String mode:new String[]{"normal","pause","2x","4x","skip","offscreen","rebuild"}){
                var ledger=new CombatReplayLedger();var sequence=new CombatSequence(journal.events(),ledger);
                sequence.speed(mode.equals("2x")?2:mode.equals("4x")?4:1);
                if(mode.equals("pause")){sequence.advance(50,e->true);float f=sequence.fraction();var e=sequence.current();sequence.pause(true);for(int i=0;i<100;i++)sequence.advance(50,x->true);check(f==sequence.fraction()&&e==sequence.current(),"pause retains phase and identity");sequence.pause(false);}
                if(mode.equals("skip")||mode.equals("rebuild"))sequence.skip();
                for(int ticks=0;!sequence.done()&&ticks<2000;ticks++)sequence.advance(50,e->!mode.equals("offscreen"));
                check(sequence.done(),"bounded completion "+mode+" "+name);
                var recreated=new CombatSequence(journal.events(),ledger);recreated.advance(50,e->true);check(recreated.done(),"completed/offscreen event never replays after camera/rebuild");
                check(Arrays.equals(expected,SaveCodec.encode(actual)),"full authority/RNG/resources/save parity "+mode+" "+name);
            }
            System.out.println("R11 case="+name+" events="+journal.events().size()+" kinds="+journal.events().stream().map(e->e.kind.name()).toList()+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(expected)));
        }
        for(String name:new String[]{"status","fire-arrow","equipment-RAM","equipment-WOODEN_BEAST","equipment-TOWER_SHIP","facility-counter"}){
            World before=copy(NativeR11Fixture.world(name)),w=copy(before),reference=copy(before);
            check(NativeR11Fixture.command(reference,name).ok,"extended reference command");
            var journal=new TurnJournal(w);check(NativeR11Fixture.command(w,name).ok,"extended legal command");journal.close();
            check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(w)),"extended full-save parity "+name);
            var g=new MapSceneSnapshot.Ground(before);var pool=new CombatVisual();boolean visible=false;
            for(var e:journal.events()){
                kinds.add(e.kind);styles.add(CombatVisual.style(e));
                if(name.equals("fire-arrow")){check(e.infantryTactic==War.Tactic.FIRE_ARROW,"typed infantry fire arrow");check(CombatVisual.style(e)==CombatVisual.Style.FIRE,"fire arrow retains fire mapping");}
                if(name.equals("equipment-TOWER_SHIP"))check(e.sourceNaval&&e.strikes.get(0).naval,"ship source and actual hits naval");
                for(int quality=0;quality<3;quality++){pool.detail(quality);for(int frame=0;frame<=100;frame++){pool.sample(e,frame/100f,g);visible|=pool.count>0;check(pool.count<=CombatVisual.CAPACITY,"extended bounded native samples");}}
            }
            check(visible,"extended effect actually sampled "+name);
            System.out.println("R11 extended="+name+" events="+journal.events().size());
        }
        var ledger=new CombatReplayLedger();TurnJournal.Event oldest=null;
        for(int i=0;i<64;i++){World w=copy(CombatSceneFixture.world("melee"));var journal=new TurnJournal(w);CombatSceneFixture.action(w,"melee");journal.close();for(var e:journal.events()){if(oldest==null)oldest=e;ledger.finish(e);}check(ledger.size()<=CombatReplayLedger.CAPACITY,"retained ledger bound");}
        check(ledger.completed(oldest),"evicted old events remain expired");ledger.clear();check(!ledger.completed(oldest),"replacement resets per-session ledger");
        for(int kind=0;kind<CombatVisual.MESH_COUNT;kind++){var mesh=CombatVisual.mesh(kind);check(mesh.indices.length>0,"real effect geometry");for(float v:mesh.vertices)check(Float.isFinite(v),"valid geometry");}
        System.out.println("R11 observed kinds="+kinds+" styles="+styles);
        System.out.println("PASS R11 "+checks+" assertions; actual command journals, typed effects, terrain, bounded pools and seven presentation modes");
    }
}
