package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.*;

/** Actual source people, saved traits, formal contest commands and complete saves. */
public final class PcContestProfilesTest {
    private static int checks;
    private static void check(boolean ok,String detail){checks++;if(!ok)throw new AssertionError(detail);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static void main(String[] args)throws Exception{
        boolean debateExercised=false;
        for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);Map<Integer,PcContestProfiles.Fact> facts=PcContestProfiles.saved(w);
            check(facts.size()==670&&w.contests.profiles.size()==670,"all historical/source-only actors have saved source profiles");
            for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))if(p.officerId>=0){
                PcContestProfiles.Fact f=facts.get(p.officerId);check(f!=null&&f.nativeId==p.nativeId&&f.recordSha.equals(p.recordSha),"checked per-source identity");
                check(f.nativePersonality==p.field(47),"original personality");
                Contests.Profile current=w.contests.profile(p.officerId),expected=f.profile();check(current.temper==expected.temper&&current.talkMask==expected.talkMask,"runtime consumes original personality/talk traits");
            }
            // Original source0 record365: native2=bold;25=shout,calm,rage.
            if(source.identity.path.equals("Media/scenario/Scen000.s11")){
                PcContestProfiles.Fact sample=facts.values().stream().filter(f->f.nativeId==365).findFirst().orElseThrow();
                check(sample.nativePersonality==2&&sample.nativeTalkMask==25,"independent native source365 record");
                Contests.Profile p=w.contests.profile(sample.officerId);check(p.temper==Debate.Temper.BOLD&&p.talkMask==((1<<Debate.Talk.SHOUT.ordinal())|(1<<Debate.Talk.CALM.ordinal())|(1<<Debate.Talk.RAGE.ordinal())),"explicit native enum names, not ordinal copy");
            }
            byte[] saved=SaveCodec.encode(w);World restored=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(restored)),"entire source save/RNG roundtrip");
            try(GameSession session=new GameSession(restored)){
                byte[] before=session.captureSave();OfficerSnapshot snapshot=session.officers();check(Arrays.equals(before,session.captureSave()),"trait DTO pure full-save/RNG query");
                for(OfficerSnapshot.Officer o:snapshot.officers)check(o.source!=null&&o.source.originalInformation.contains("原性格：")&&o.source.originalInformation.contains("原话术标记："),"same normal officer DTO reports saved facts");
            }
            // Simulate the explicit absence in a pre-batch source38 save; no
            //decoder or query is permitted to consult current catalog to add it.
            World old=copy(w);old.contests.profiles.clear();old.extensions.put(PcContestProfiles.NAMESPACE,null);
            byte[] prior=SaveCodec.encode(old);World oldReload=SaveCodec.decode(prior);check(PcContestProfiles.saved(oldReload).isEmpty()&&oldReload.contests.profiles.isEmpty(),"existing source38 is not backfilled");check(Arrays.equals(prior,SaveCodec.encode(oldReload)),"old-source absence bytes unchanged");
            if(!debateExercised){
                outer:for(World.City city:w.cities)if(city.owner==w.player)for(World.Officer actor:w.idle(city))for(World.Officer target:w.officers){
                    if(w.contests.debateError(city.id,actor.id,target.id)!=null)continue;
                    try(GameSession game=new GameSession(copy(w))){
                        LegacyView view=game.legacyView();check(game.legacy(view.draft,()->view.draft.contests.persuade(city.id,actor.id,target.id)).ok,"normal source recruitment debate start");
                        World control=copy(view.draft);boolean special=false;
                        for(int turn=0;turn<3&&control.contests.current().debate().winner()==-2;turn++){
                            Contests.Session current=control.contests.current();Debate d=current.debate();int chosen=-1;
                            for(int i=0;i<d.speaker(0).hand().size();i++)if(d.cardError(i)==null){if(chosen<0)chosen=i;if(d.speaker(0).hand().get(i).talk!=null){chosen=i;special=true;break;}}
                            check(chosen>=0,"source profile produces legal cards");StateToken token=game.state();ContestCommand command=ContestCommand.card(token,current.id(),current.revision(),chosen);
                            check(control.contests.debateCard(current.id(),current.revision(),chosen).ok&&game.execute(command).ok(),"formal contest card on authoritative session");
                            byte[] full=game.captureSave();check(Arrays.equals(full,SaveCodec.encode(control)),"formal source contest complete save/RNG");
                            check(!game.execute(command).ok()&&Arrays.equals(full,game.captureSave()),"duplicate/stale contest does not alter full save/RNG");
                            try(GameSession cold=new GameSession(SaveCodec.decode(full))){check(Arrays.equals(full,cold.captureSave()),"mid-contest complete cold session reopen");cold.officers();check(Arrays.equals(full,cold.captureSave()),"mid-contest DTO remains pure");}
                        }
                        System.out.println("PASS real source traits and normal source debate "+source.identity.path+" actor="+actor.id+" target="+target.id+" special="+special+"; numeric engine still legacy until original model integration");
                        debateExercised=true;
                    }
                    break outer;
                }
            }
        }
        check(debateExercised,"at least one actual source opening has ordinary valid debate target");
        // Genuine captured Batch07 source files retain their original profiles
        //absence. Read only; preserve their exact saved policies and bytes.
        if(args.length>0)for(int i=0;i<16;i++){
            Path p=Path.of(args[0],String.format(Locale.ROOT,"source%03d-flow.sg11",i));byte[] raw=Files.readAllBytes(p);World old=SaveCodec.decode(raw);check(PcContestProfiles.saved(old).isEmpty()&&old.contests.profiles.isEmpty(),"actual pre-batch source38 no profile backfill");check(args.length>1,"exact parent host reencoding required for cross-runtime ART captures");byte[] parent=Files.readAllBytes(Path.of(args[1],p.getFileName().toString()));check(Arrays.equals(parent,SaveCodec.encode(old)),"actual installed source38 matches exact parent host reencoding, profiles remain absent");
        }
        System.out.println("PASS PcContestProfilesTest "+checks+" actual source facts, normal formal debate commands and complete source save/RNG checks; full original contest engine/APK remain separate");
    }
}
