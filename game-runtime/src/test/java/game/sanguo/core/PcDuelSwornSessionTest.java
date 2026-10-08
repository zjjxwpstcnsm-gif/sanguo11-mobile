package game.sanguo.core;

import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;

/** Actual typed producer and natural model terminal; declared adjacent source
 * units are not ordinary deployment, original GUI or APK acceptance. */
public final class PcDuelSwornSessionTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[]args)throws Exception {
        boolean completed=false;
        for(int seed=28;seed<128&&!completed;seed++){
            World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,seed,new PcDuelOptions(0,0,0));
            int[]natives={365,116,466,635},ids=new int[4];
            for(int i=0;i<4;i++){final int n=natives[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
            Hex first=null;
            outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                Hex h=new Hex(q,r),k=new Hex(q+1,r);
                if(w.inside(k)&&w.cost(h,World.Weapon.SWORD)>0&&w.cost(k,World.Weapon.SWORD)>0&&!w.army.water(h)&&!w.army.water(k)&&w.cityAt(h)==null&&w.cityAt(k)==null&&PcPersonnelReturnRules.cityAt(w,k)==15){first=h;break outer;}
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
            System.out.println("Declared solo365/635 seed "+seed+" candidate chance "+w.contests.nativeDuelCandidates(1,2).get(0).chance);
            try(GameSession game=new GameSession(w)){
                var result=game.execute(ContestCommand.startNativeDuel(game.state(),1,2,ids[0]));check(result.ok(),"typed start "+result.detail);
                if(game.contest().nativeDuel==null){System.out.println("Original refusal seed "+seed);continue;}
                int inputs=0;
                while(!game.contest().nativeDuel.terminal&&inputs++<300){
                    var facts=game.contest();var choice=facts.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).findFirst().orElseThrow();
                    result=game.execute(ContestCommand.nativeDuelInput(facts.state,facts.contestId,facts.revision,choice.stance,choice.special,choice.replacement));check(result.ok(),"natural human/AI input "+result.detail);
                    byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"each input full cold");
                }
                check(game.contest().nativeDuel.terminal,"natural sworn battle terminal");
                var facts=game.contest();result=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(result.ok(),"typed terminal preparation "+result.detail);
                if(game.contest().nativeDuel==null){System.out.println("Natural no-disposition terminal seed "+seed+" inputs "+inputs);continue;}
                facts=game.contest();check(facts.nativeDuel.disposition.size()==1&&facts.nativeDuel.disposition.get(0).officerId==ids[3],"actual native635 fate");
                result=game.execute(ContestCommand.nativeDuelDisposition(facts.state,facts.contestId,facts.revision,ids[3],3));check(result.ok(),"original EXECUTE selection "+result.detail);
                byte[]pending=game.captureSave();game.replace(SaveCodec.decode(pending));check(Arrays.equals(pending,game.captureSave()),"selected fate cold exact");
                facts=game.contest();result=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(result.ok(),"complete typed sworn settlement "+result.detail);
                byte[]finished=game.captureSave();var current=SaveCodec.decode(finished);check(current.unit(2)==null&&current.life.state(ids[3])==Lifecycle.State.DEAD&&PcDuelSwornPolicy.current(current,98)==98&&PcDuelSwornPolicy.current(current,432)==98&&PcDuelSwornPolicy.current(current,635)==-1,"original unit deletion/death/current successor");
                result=game.execute(ContestCommand.finishNativeDuel(game.state(),facts.contestId,facts.revision));check(!result.ok()&&Arrays.equals(finished,game.captureSave()),"terminal cannot write twice");
                for(int turn=0;turn<4;turn++){
                    var ticket=game.beginTurn();var computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok,"full campaign turn");check(game.commitTurn(ticket,computed),"turn once");byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"turn allWorld/RNG cold");
                }
                current=SaveCodec.decode(game.captureSave());check(current.life.state(ids[3])==Lifecycle.State.DEAD&&PcDuelSwornPolicy.current(current,98)==98&&PcDuelSwornPolicy.current(current,432)==98,"original succession persists after whole turns");completed=true;
                System.out.println("PASS typed natural sworn EXECUTE terminal seed "+seed+" inputs "+inputs);
            }
        }
        check(completed,"natural accepted sworn battle completed");
        System.out.println("PASS sworn typed execution "+checks+" checks; declared units, ordinary deployment/menu/APK pending");
    }
}
