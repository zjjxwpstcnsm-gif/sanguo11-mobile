package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Explicit source choice through actual openings, commands, full turns and saves. */
public final class PcOfficerInfoTest {
    private static int checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    private static byte[] withoutSource(World w)throws Exception{
        byte[] source=w.extensions.get(PcOfficerInfo.NAMESPACE);w.extensions.put(PcOfficerInfo.NAMESPACE,null);
        byte[] raw=SaveCodec.encode(w);w.extensions.put(PcOfficerInfo.NAMESPACE,source);return raw;
    }
    private static void run(String scenario,PcOfficerSources.Source source)throws Exception{
        World w=ScenarioCatalog.load(scenario,0,23);byte[] original=SaveCodec.encode(w);
        int count=PcOfficerSources.attachOpening(w,source.id);
        check(count>0&&Arrays.equals(original,withoutSource(w)),"source text does not change any rule/base/XP/RNG/save policy");
        boolean duplicate=false;try{PcOfficerSources.attachOpening(w,source.id);}catch(java.io.IOException expected){duplicate=true;}
        check(duplicate,"no second source silently replaces fixed opening provenance");
        World control=SaveCodec.decode(original);
        try(GameSession game=new GameSession(w)){
            byte[] saved=game.captureSave();OfficerSnapshot retained=game.officers();int connected=0;
            Map<Integer,PcOfficerInfo.Person> sourceRecords=PcOfficerInfo.saved(SaveCodec.decode(saved));
            for(OfficerSnapshot.Officer row:retained.officers){
                PcOfficerInfo.Person p=sourceRecords.get(row.id);
                check((p==null)==(row.source==null),"DTO joins only saved identity");
                if(p==null)continue;connected++;
                check(row.source.nativeId==p.nativeId&&row.source.sourceVariant.equals(p.sourceVariant)&&row.source.recordSha.equals(p.recordSha),"exact source identity and record SHA");
                check(row.source.biography.equals(p.biography)&&row.source.courtesy.equals(p.courtesy),"same saved text authority");
                check(row.source.unknown.contains("activeResourcePriority"),"resource activation is not inferred from explicit selection");
                if(!p.courtesy.isEmpty())check(row.searchText().contains(p.courtesy),"search contains saved courtesy name");
            }
            check(connected==count&&Arrays.equals(saved,game.captureSave()),"complete selected roster and read-only full save/RNG");
            World.City home=control.home();List<World.Officer> idle=control.idle(home);
            if(!idle.isEmpty()){
                int officer=idle.get(0).id;check(control.patrol(home.id,officer).ok,"control normal patrol");
                check(game.execute(new GameCommand(GameCommand.Operation.PATROL,game.state(),home.id,officer)).ok(),"normal session patrol");
            }
            for(int i=0;i<3;i++){
                TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                check(computed.nextTurn().ok&&game.commitTurn(ticket,computed)&&control.nextTurn().ok,"real full turn");
                saved=game.captureSave();World reopened=SaveCodec.decode(saved);
                check(Arrays.equals(SaveCodec.encode(control),withoutSource(reopened)),"source attachment preserves complete actual rules and RNG continuation");
                try(GameSession reload=new GameSession(reopened)){
                    check(Arrays.equals(saved,reload.captureSave()),"source text survives complete save/reopen");
                    for(OfficerSnapshot.Officer row:reload.officers().officers){PcOfficerInfo.Person p=sourceRecords.get(row.id);
                        if(p!=null)check(row.source!=null&&row.source.biography.equals(p.biography)&&row.source.courtesy.equals(p.courtesy),"immutable text remains fixed after turns/reopen");}
                }
            }
            check(retained.officers.get(0).name.equals(w.officers.get(0).name),"retained DTO unchanged");
            if(source.path.endsWith("Scen014.S11")){
                OfficerSnapshot.Officer swapped=retained.officer(10333);
                if(swapped!=null)check(swapped.source.biography.isEmpty()&&swapped.source.unknown.contains("biographyIdentityDisagreement"),"swapped native identity cannot receive another person's biography");
            }
        }
        System.out.println("PASS PC text opening="+scenario+" source="+source.path+" attached="+count);
    }
    private static void unknownSavedMetadata()throws Exception{
        World w=ScenarioCatalog.load("coalition-190",0,23);PcOfficerSources.attachOpening(w,PcOfficerSources.all().get(0).id);
        byte[] original=w.extensions.get(PcOfficerInfo.NAMESPACE),future=original.clone();future[3]=2;
        w.extensions.put(PcOfficerInfo.NAMESPACE,future);
        try(GameSession game=new GameSession(w)){
            byte[] before=game.captureSave();
            for(OfficerSnapshot.Officer row:game.officers().officers)check(row.source==null&&row.unknown.contains("sourceMetadataUnreadable"),"future source namespace stays opaque; no live-catalog backfill");
            check(Arrays.equals(before,game.captureSave())&&Arrays.equals(future,SaveCodec.decode(before).extensions.get(PcOfficerInfo.NAMESPACE)),"unknown source bytes and every gameplay/RNG byte preserved");
        }
        byte[] mismatch=original.clone(),name="刘备".getBytes(java.nio.charset.StandardCharsets.UTF_8),different="曹操".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int found=-1;outer:for(int i=0;i<=mismatch.length-name.length;i++){for(int j=0;j<name.length;j++)if(mismatch[i+j]!=name[j])continue outer;found=i;break;}
        check(found>=0,"stored identity guard name exists");System.arraycopy(different,0,mismatch,found,different.length);w.extensions.put(PcOfficerInfo.NAMESPACE,mismatch);
        try(GameSession game=new GameSession(w)){
            byte[] before=game.captureSave();OfficerSnapshot.Officer row=game.officers().officer(1000);
            check(row.name.equals("刘备")&&row.source==null&&row.unknown.contains("sourceIdentityChanged"),"retargeted source identity cannot replace actual officer facts or biography");
            check(Arrays.equals(before,game.captureSave()),"rejected source identity read preserves complete save/RNG");
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length==1&&args[0].equals("--edges")){unknownSavedMetadata();System.out.println("PASS PcOfficerInfoEdges checks="+checks);return;}
        List<PcOfficerSources.Source> sources=PcOfficerSources.all();check(sources.size()==16,"all 16 independent local source files");
        check(sources.stream().mapToInt(PcOfficerSources.Source::count).sum()==10656,"strict verified identity rows, no gaiji/extra-slot inflation");
        String coalition="coalition-190";
        for(PcOfficerSources.Source source:sources)run(coalition,source);
        for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries())if(!row.id.equals(coalition))run(row.id,sources.get(0));
        unknownSavedMetadata();
        System.out.println("PASS PcOfficerInfoTest checks="+checks+" explicit sources, unchanged numerical state, actual patrol/three turns/save/reopen/full RNG, unknowns; complete PC scenario restoration remains pending");
    }
}
