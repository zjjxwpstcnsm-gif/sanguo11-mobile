package game.sanguo.core;

import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;

/** Actual typed producer and natural model terminal; declared adjacent source
 * units are not ordinary deployment, original GUI or APK acceptance. */
public final class PcDuelRulerAiSessionTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[]args)throws Exception {
        boolean completed=false;
        for(int seed=0;seed<64&&!completed;seed++){
            World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,3,seed,new PcDuelOptions(0,0,0));
            int[]natives={517,14,558,365},ids=new int[4];
            for(int i=0;i<4;i++){final int n=natives[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
            Hex first=null;
            outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                Hex h=new Hex(q,r),k=new Hex(q+1,r);
                if(w.inside(k)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(k,World.Weapon.SWORD)>0&&!w.army.water(h)&&!w.army.water(k)&&w.cityAt(h)==null&&w.cityAt(k)==null&&PcPersonnelReturnRules.cityAt(w,h)==15&&PcPersonnelReturnRules.cityAt(w,k)==15){first=h;break outer;}
            }
            check(first!=null,"source15 legal declared land");
            for(int side=0;side<2;side++){
                var leader=w.officer(ids[side==0?0:3]);var home=w.city(leader.cityId);
                var unit=new World.Unit(side+1,leader.owner,leader.id,World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);
                unit.deputies=new int[0];unit.gold=997;w.units.add(unit);
                for(var person:w.army.crew(unit)){w.strategy.releaseGovernor(person.id);person.unitId=unit.id;person.cityId=-1;}
                w.districts.deployed(home.id,unit);PcGovernorPolicy.deployed(w,unit,home);
            }
            w.nextUnitId=3;w.governance.reconcile(false);
            System.out.println("Declared human517/AI365 seed "+seed+" candidate chance "+w.contests.nativeDuelCandidates(1,2).get(0).chance);
            try(GameSession game=new GameSession(w)){
                var result=game.execute(ContestCommand.startNativeDuel(game.state(),1,2,ids[0]));check(result.ok(),"typed start "+result.detail);
                if(game.contest().nativeDuel==null){System.out.println("Original refusal seed "+seed);continue;}
                int inputs=0;
                while(!game.contest().nativeDuel.terminal&&inputs++<300){
                    var facts=game.contest();var choice=facts.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).findFirst().orElseThrow();
                    result=game.execute(ContestCommand.nativeDuelInput(facts.state,facts.contestId,facts.revision,choice.stance,choice.special,choice.replacement));check(result.ok(),"natural human/AI input "+result.detail);
                    byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"each input full cold");if(inputs%25==0)System.out.println("input "+inputs+" phase "+game.contest().phase+" sub "+game.contest().sub+" round "+game.contest().round+" frames "+game.contest().nativeDuel.frames);
                }
                if(!game.contest().nativeDuel.terminal){var path=java.nio.file.Path.of("out/session-b/duel-ruler-ai-seed1-nonterminal-v1.sg11");check(!java.nio.file.Files.exists(path),"preserve prior nonterminal checkpoint");java.nio.file.Files.write(path,game.captureSave());}
                check(game.contest().nativeDuel.terminal,"natural ruler battle terminal");
                var facts=game.contest();int outcome=facts.winner;
                if(outcome!=1){
                    result=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(result.ok(),"actual other natural outcome "+outcome+" "+result.detail);
                    if(game.contest().nativeDuel!=null){for(var row:game.contest().nativeDuel.disposition){var now=game.contest();result=game.execute(ContestCommand.nativeDuelDisposition(now.state,now.contestId,now.revision,row.officerId,2));check(result.ok(),"natural human victory RELEASE choice "+result.detail);}var now=game.contest();result=game.execute(ContestCommand.finishNativeDuel(now.state,now.contestId,now.revision));check(result.ok(),"natural human victory settlement "+result.detail);}
                    byte[]other=game.captureSave();check(Arrays.equals(other,SaveCodec.encode(SaveCodec.decode(other))),"other natural outcome fullWorld cold exact");System.out.println("Completed other natural outcome seed "+seed+" winner "+outcome+" inputs "+inputs);continue;
                }
                result=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(result.ok(),"typed terminal preparation "+result.detail);
                check(game.contest().nativeDuel==null,"natural player ruler loss fully executes original AI disposition without human choice");
                byte[]finished=game.captureSave();var current=SaveCodec.decode(finished);check(current.unit(1)==null&&current.officer(ids[0]).role==Strategy.Role.RULER&&PcDuelRelease.remaining(current,ids[0])==3,"original AI released single-ruler unit/task37");
                result=game.execute(ContestCommand.finishNativeDuel(game.state(),facts.contestId,facts.revision));check(!result.ok()&&Arrays.equals(finished,game.captureSave()),"terminal cannot write twice");
                for(int turn=0;turn<4;turn++){
                    var ticket=game.beginTurn();var computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok,"full campaign turn");check(game.commitTurn(ticket,computed),"turn once");byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"turn allWorld/RNG cold");
                }
                current=SaveCodec.decode(game.captureSave());check(!PcDuelRelease.busy(current,ids[0])&&current.officer(ids[0]).role==Strategy.Role.RULER,"returned ruler after whole turns");completed=true;
                System.out.println("PASS typed natural player-ruler loss/AI RELEASE seed "+seed+" inputs "+inputs);
            }
        }
        check(completed,"natural accepted ruler battle completed");
        System.out.println("PASS ruler typed terminal "+checks+" checks; declared units, ordinary deployment/menu/APK pending");
    }
}
