package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Normal original-terrain commands and the production source timeline reader. */
public final class PcCriticalsProbe {
    private static int checks;
    private static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        for(War.Plot plot:new War.Plot[]{null,War.Plot.SORCERY,War.Plot.LIGHTNING}){
            var fixture=PcCriticalsFixture.prepare(plot);boolean tactic=fixture.tactic;World w=fixture.world;
            World plain=SaveCodec.decode(SaveCodec.encode(w));TurnJournal journal=new TurnJournal(w);
            World.Result result=fixture.command(w),baseline=fixture.command(plain);journal.close();
            check(result.ok&&result.message.equals(baseline.message),"normal command result");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"normal national command exact save/RNG");
            List<PcPresentationPlan.Cue> cues=new ArrayList<>();for(var event:journal.events())cues.addAll(PcPresentationPlan.cues(event));
            check(cues.size()==1&&cues.get(0).selector==fixture.selector,"normal national critical original binding: "+fixture.label);
            PcPresentationTimeline timeline=PcPresentationTimeline.read(new FileInputStream(args[0]+"/template-"+cues.get(0).template+".pcps"),cues.get(0).template);
            check(timeline.maximum>1&&timeline.frames.length==125,"multilayer original source timeline");
            boolean over=false,add=false,dynamic=false;
            int sourceQuads=0,batches=0;
            for(ByteBuffer frame:timeline.frames)for(int at=0;at<frame.limit();at+=PcPresentationTimeline.RECORD_BYTES){
                over|=frame.getInt(at+4)==6;add|=frame.getInt(at+4)==2;dynamic|=frame.getInt(at)==32;
            }
            check(over&&add&&dynamic,"original alpha/additive/dynamic layers retained");
            double f=1/Math.tan(Math.PI/12),mapFar=1.7*2*f;
            double[] mapProjection={f/(4d/3),0,0,0,0,f,0,0,0,0,-(mapFar+.8)/(mapFar-.8),-1,0,0,-2*mapFar*.8/(mapFar-.8),0};
            double[] sourceProjection=mapProjection.clone();PcPresentationLens.depthRange(sourceProjection);
            for(int i=0;i<16;i++)if(i!=10&&i!=14)check(sourceProjection[i]==mapProjection[i],"source stage preserves projection size/aspect/center");
            int mapClipped=0,sourceClipped=0;
            for(ByteBuffer frame:timeline.frames)for(int at=0;at<frame.limit();at+=168){
                double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(int vertex=0;vertex<4;vertex++){
                    int v=at+8+vertex*24,m=at+104;float x=frame.getFloat(v),y=frame.getFloat(v+4),z=frame.getFloat(v+8);
                    double depth=-(x*frame.getFloat(m+8)+y*frame.getFloat(m+24)+z*frame.getFloat(m+40)+frame.getFloat(m+56))*PcEffectCoordinates.SCALE;
                    min=Math.min(min,depth);max=Math.max(max,depth);
                }
                if(min>mapFar||max<.8)mapClipped++;
                if(min>PcPresentationLens.FAR||max<PcPresentationLens.NEAR)sourceClipped++;
            }
            check(sourceClipped==0,"every original converted quad inside dedicated source depth range");
            check(tactic?mapClipped==600:mapClipped==0,"real source cohort reproduces map-lens600-quad clipping bug");
            for(ByteBuffer frame:timeline.frames){
                int covered=0;
                for(int first=0;first<frame.limit()/168;){int end=PcPresentationTimeline.batchEnd(frame,first);batches++;
                    for(int i=first;i<end;i++){
                        check(frame.getInt(i*168)==frame.getInt(first*168)&&frame.getInt(i*168+4)==frame.getInt(first*168+4),"batch preserves texture and original blend");
                        for(int index:new int[]{0,1,2,2,1,3})check((first*6+(i-first)*6)/6*4+index==i*4+index,"batched GPU range preserves every original triangle vertex/order");
                        covered++;sourceQuads++;
                    }first=end;
                }check(covered*168==frame.limit(),"batch covers every source record exactly once");
            }
            check(batches<sourceQuads&&timeline.maximumBatches<timeline.maximum,"actual source cohort reduces draws without removing quads");
            System.out.println("BATCH template="+timeline.template+" source_quads="+sourceQuads+" draws="+batches+" maximum_batches="+timeline.maximumBatches);
            for(var event:journal.events())if(!PcPresentationPlan.cues(event).isEmpty())check(PcPresentationPlan.duration(event)==PcPresentationPlan.CUE_MILLIS&&PcPresentationPlan.CUE_MILLIS>=500&&PcPresentationPlan.CUE_MILLIS<=1000,"one cue within user-approved duration range");
            TurnJournal.Event critical=journal.events().stream().filter(e->!PcPresentationPlan.cues(e).isEmpty()).findFirst().orElseThrow();
            int cueMillis=PcPresentationPlan.duration(critical),half=cueMillis/2;
            int total=critical.durationMillis()+cueMillis;
            CombatSequence source=new CombatSequence(List.of(critical),new CombatReplayLedger(),e->total,PcPresentationPlan::duration);
            source.advance(half,e->true);check(Math.abs(source.fraction()*total-half)<.01,"source cue follows actual elapsed despite sparse frames");
            source.pause(true);float frozen=source.fraction();source.advance(10000,e->true);check(source.fraction()==frozen,"paused source clock excludes actual elapsed");source.pause(false);
            source.advance(cueMillis-half,e->true);check(Math.abs(source.fraction()*total-cueMillis)<.01,"source cue finishes after configured visual duration");
            source.advance(250,e->true);check(Math.abs(source.fraction()*total-(cueMillis+50))<.01,"post-cue action preserves original50ms cap");
            CombatSequence fast=new CombatSequence(List.of(critical),new CombatReplayLedger(),e->total,PcPresentationPlan::duration);fast.speed(4);long fastMillis=(cueMillis+3)/4;fast.advance(fastMillis,e->true);check(Math.abs(fast.fraction()*total-fastMillis*4)<.01,"4x source clock advances configured cue without changing authority");fast.skip();check(fast.done(),"skip completes source sequence");
            CombatSequence legacy=new CombatSequence(List.of(critical),new CombatReplayLedger(),e->total);legacy.advance(250,e->true);check(Math.abs(legacy.fraction()*total-50)<.01,"legacy action cap unchanged");
            // Exercise the real source clock boundary under GPU rejection and
            // long owner stalls. Existing unbounded sequence/action semantics
            // above remain tested; only the source presentation host is gated.
            for(int speed:new int[]{1,2,4}){
                CombatSequence stalled=new CombatSequence(List.of(critical),new CombatReplayLedger(),e->total,PcPresentationPlan::duration);stalled.speed(speed);
                for(int wait=0;wait<8;wait++)stalled.advance(PcPresentationClock.elapsedMillis(10000,speed,false),e->true);
                check(stalled.fraction()==0,"rejected source stage cannot consume original entrance");
                float previous=0;int poses=0,peak=0;boolean entrance=false,exit=false;
                while(stalled.fraction()*total<cueMillis){
                    stalled.advance(PcPresentationClock.elapsedMillis(10000,speed,true),e->true);
                    float elapsed=stalled.fraction()*total;
                    check(elapsed-previous<=50.01,"source stall catch-up bounded in visual milliseconds atall speeds");previous=elapsed;
                    float phase=elapsed/cueMillis;
                    if(phase<1){poses++;peak=Math.max(peak,timeline.frame(phase).limit()/168);entrance|=phase<.3;exit|=phase>.65;}
                }
                check(poses>=14&&peak>10&&entrance&&exit,"stalled native submissions retain actual source entrance/main layers/exit");
                stalled.skip();check(stalled.done(),"stalled source still supports explicit skip");
            }
            byte[] before=SaveCodec.encode(w);for(int i=0;i<500;i++){for(var event:journal.events())PcPresentationPlan.cues(event);timeline.frame(i/499f);}
            check(Arrays.equals(before,SaveCodec.encode(w)),"repeat asset sampling cannot mutate authority/RNG");
            System.out.println(fixture.label+" actor="+fixture.actor+" target="+fixture.target+" selector="+cues.get(0).selector+" original_quads="+timeline.maximum+" result="+result.message);
        }
        var control=PcCriticalsFixture.prepare(War.Plot.CONFUSE);TurnJournal controlJournal=new TurnJournal(control.world);check(control.command(control.world).ok,"normal successful critical扰乱 control");controlJournal.close();
        check(controlJournal.events().stream().allMatch(e->PcPresentationPlan.cues(e).isEmpty()),"扰乱 cannot borrow original妖術 texture126");
        var custom=PcCriticalsFixture.prepare(true);World.Officer original=custom.world.officer(custom.world.unit(custom.actor).officerId);
        World.Officer replacement=new World.Officer(900001,original.name,original.owner,-1,original.leadership,original.war,original.intelligence,original.politics,original.charm);replacement.skillId=original.skillId;System.arraycopy(original.aptitude,0,replacement.aptitude,0,original.aptitude.length);replacement.unitId=custom.actor;
        custom.world.officers.add(replacement);original.unitId=-1;custom.world.unit(custom.actor).officerId=replacement.id;
        TurnJournal customJournal=new TurnJournal(custom.world);check(custom.command(custom.world).ok,"custom officer shares name but has separate identity");customJournal.close();
        check(customJournal.events().stream().anyMatch(e->e.critical!=null&&e.critical.officerId==900001),"custom control really emitted a critical with its own identity");
        check(customJournal.events().stream().allMatch(e->PcPresentationPlan.cues(e).isEmpty()),"same name never substitutes original canonical portrait");
        for(var fixture:PcCriticalsFixture.presentations())if(fixture.label.endsWith("-young")||fixture.label.endsWith("-old")){
            World w=fixture.world,baseline=SaveCodec.decode(SaveCodec.encode(w));TurnJournal journal=new TurnJournal(w);
            World.Result applied=fixture.command(w),controlResult=fixture.command(baseline);journal.close();
            check(applied.ok&&applied.message.equals(controlResult.message),"native portrait normal command "+fixture.label);
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(baseline)),"portrait selection exact applied save/RNG "+fixture.label);
            List<PcPresentationPlan.Cue> cues=new ArrayList<>();for(var event:journal.events())cues.addAll(PcPresentationPlan.cues(event));
            check(cues.size()==1&&cues.get(0).selector==fixture.selector,"native age boundary selects authentic portrait "+fixture.label);
            for(var event:journal.events())if(event.critical!=null){
                check(event.critical.year==w.startYear,"applied year captured immutably "+fixture.label);
                int year=w.startYear;w.startYear=180;
                check(PcPresentationPlan.cues(event).get(0)==cues.get(0),"later visual calendar cannot change recorded cue "+fixture.label);w.startYear=year;
            }
            check(new File(args[0]+"/selector-"+fixture.selector+".png").isFile(),"normal cue has converted source pixels "+fixture.label);
            System.out.println(fixture.label+" selector="+fixture.selector+" year="+w.startYear+" result="+applied.message);
        }
        System.out.println("PASS "+checks+" normal national critical commands/source templates/blends/duration/exact save/RNG");
    }
}
