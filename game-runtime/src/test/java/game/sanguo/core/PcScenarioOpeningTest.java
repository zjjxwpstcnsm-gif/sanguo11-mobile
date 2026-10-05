package game.sanguo.core;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.*;

/** Actual converted source worlds, separate from synthetic capacity/engineering fixtures. */
public final class PcScenarioOpeningTest {
    public static void main(String[] args)throws Exception{
        int errors=0,checks=0;
        for(PcScenarioCatalog.Source source:PcScenarioCatalog.all())try{
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);
            require(w.pcSourceFrame&&w.factions.length==47&&w.cities.size()==87&&w.officers.size()==670,"source roster/site/capacity");checks++;
            Map<Integer,PcScenarioPeople.Person> original=new TreeMap<>();for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))if(p.officerId>=0)original.put(p.officerId,p);
            for(World.Officer o:w.officers){PcScenarioPeople.Person p=original.get(o.id);require(p!=null,"identity record");for(int i=0;i<5;i++){require(w.officerAbilities.base(o.id,i)==p.field(25+i),"original base");require(w.officerAbilities.growthCode(o.id,i)==p.field(35+i),"original growth");require(w.officerAbilities.experience(o.id,i)==p.field(30+i),"original experience");checks+=3;}}
            byte[] saved=SaveCodec.encode(w);require(saved[7]==38,"explicit source header38");World reopen=SaveCodec.decode(saved);require(Arrays.equals(saved,SaveCodec.encode(reopen)),"complete reopening bytes/RNG");checks+=2;
            World control=SaveCodec.decode(saved);
            try(GameSession game=new GameSession(w)){
                byte[] before=game.captureSave();OfficerSnapshot dto=game.officers();require(Arrays.equals(before,game.captureSave()),"normal DTO leaves complete save/RNG");checks++;
                require(dto.officers.size()==670,"complete normal source officer DTO");checks++;
                for(World.Officer o:w.officers){OfficerSnapshot.Officer view=dto.officer(o.id);PcScenarioPeople.Person p=original.get(o.id);
                    require(view!=null&&view.source!=null&&view.source.nativeId==p.nativeId&&view.source.sourceVariant.equals(source.identity.sourceVariant),"normal DTO source connection");checks++;
                }
                World.City city=w.home();List<World.Officer> idle=w.idle(city);
                if(idle.isEmpty())throw new AssertionError("Source player has no idle resident");int actor=idle.get(0).id;
                require(control.patrol(city.id,actor).ok,"normal source patrol control");
                StateToken token=game.state();require(game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor)).ok(),"normal typed source patrol");
                require(!game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor)).ok(),"stale source command rejected");
                require(Arrays.equals(SaveCodec.encode(control),game.captureSave()),"source command complete save/RNG");checks+=4;
                for(int turn=0;turn<3;turn++){
                    TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                    require(control.nextTurn().ok&&computed.nextTurn().ok&&game.commitTurn(ticket,computed),"normal source full turn/commit");
                    byte[] full=game.captureSave();require(Arrays.equals(full,SaveCodec.encode(control)),"normal source turn complete save/RNG");
                    try(GameSession restored=new GameSession(SaveCodec.decode(full))){require(Arrays.equals(full,restored.captureSave()),"normal source cold session reopen");restored.officers();require(Arrays.equals(full,restored.captureSave()),"source DTO after reopening stays read-only");}
                    checks+=4;
                }
            }
            System.out.println("PASS source opening "+source.identity.path+" player="+w.player+" alive="+source.playerForces()+" source records="+original.size()+" strict="+source.strictPeople()+" save="+saved.length+"; full events and activation still unknown");
        }catch(Exception e){errors++;System.out.println("FAIL source opening "+source.identity.path+": "+e);e.printStackTrace(System.out);}
        if(errors>0)throw new AssertionError(errors+" original source openings rejected");System.out.println("PASS PcScenarioOpeningTest "+checks+" source record, formal patrol, three full turns and save/RNG/session reopening checks; actual APK flows remain separate");
    }
    private static void require(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
}
