package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;

/** Actual catalog/commands/full-turn/save continuation; no arithmetic oracle substitute. */
public final class OfficerQueryTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static OfficerSnapshot verify(GameSession game)throws Exception{
        byte[] before=game.captureSave();StateToken token=game.state();
        OfficerSnapshot snapshot=game.officers();World saved=SaveCodec.decode(before);
        check(snapshot.state.equals(token)&&snapshot.officers.size()==saved.officers.size(),"complete roster/token");
        for(World.Officer o:saved.officers){
            OfficerSnapshot.Officer d=snapshot.officer(o.id);
            check(d!=null&&d.name.equals(o.name)&&d.owner==o.owner&&d.cityId==o.cityId&&d.unitId==o.unitId&&d.loyalty==o.loyalty,"saved identity/allegiance/location/loyalty");
            check(d.present==saved.life.present(o.id),"stored lifecycle presence");
            check(d.current.equals(Arrays.asList(o.leadership,o.war,o.intelligence,o.politics,o.charm)),"saved current values");
            check(d.merit==saved.government.merit(o.id)&&d.office.equals(saved.governance.office(o)),"saved merit/office");
            if(saved.officerAbilities.enabled())for(int i=0;i<5;i++)check(d.base.get(i)==saved.officerAbilities.base(o.id,i)&&d.growth.get(i)==saved.officerAbilities.growthCode(o.id,i)&&d.experience.get(i)==saved.officerAbilities.experience(o.id,i),"saved separated ability values");
            else check(d.base.isEmpty()&&d.growth.isEmpty()&&d.experience.isEmpty()&&d.unknown.contains("base"),"legacy base and XP not reverse-inferred");
            check(d.unknown.contains("biography")&&d.unknown.contains("nativeId"),"no catalog/source backfill");
        }
        check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"full save/RNG/token read-only");
        boolean immutable=false;try{snapshot.officers.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"immutable roster");
        immutable=false;try{snapshot.officers.get(0).current.set(0,0);}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"immutable current values");
        return snapshot;
    }
    public static void main(String[] args)throws Exception{
        if(args.length==1){
            Path fixtures=Path.of(args[0]);
            String[] files={"save-v32-central-native.sg11","pc-officer-legacy-am.sg11","pre-base-construction-v33.sg11",
                "pre-merchant-r25-v34.sg11","legacy-market-v35/host/coalition-190.sg11",
                "legacy-market-v35/art/coalition-190.sg11","legacy-production-v36/coalition-190-host.sg11",
                "legacy-production-v36/coalition-190-art.sg11"};
            for(String file:files){
                byte[] original=Files.readAllBytes(fixtures.resolve(file));World control=SaveCodec.decode(original);
                try(GameSession game=new GameSession(SaveCodec.decode(original))){
                    verify(game);byte[] saved=game.captureSave();World reopened=SaveCodec.decode(saved);
                    check(control.nextTurn().ok&&reopened.nextTurn().ok,"actual legacy full turn "+file);
                    check(Arrays.equals(SaveCodec.encode(control),SaveCodec.encode(reopened)),"legacy continuation/full-save/RNG "+file);
                }
                System.out.println("PASS officer legacy fixture="+file);
            }
            System.out.println("PASS OfficerQueryLegacy checks="+checks+" actual v32-v36 fixtures, unchanged query and full-turn continuation; v31 fixture unavailable");return;
        }
        for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries()){
            try(GameSession game=new GameSession(ScenarioCatalog.load(row.id,0,12345))){
                OfficerSnapshot retained=verify(game);String name=retained.officers.get(0).name;List<Integer> values=retained.officers.get(0).current;
                World w=game.legacyView().draft;
                World.City home=w.home();List<World.Officer> idle=w.idle(home);
                if(!idle.isEmpty()){
                    CommandResult command=game.execute(new GameCommand(GameCommand.Operation.PATROL,game.state(),home.id,idle.get(0).id));
                    check(command.ok(),"normal patrol command "+row.id);verify(game);
                }
                for(int turn=0;turn<3;turn++){
                    TurnTicket ticket=game.beginTurn();verify(game);World computed=SaveCodec.decode(ticket.initial());
                    check(computed.nextTurn().ok&&game.commitTurn(ticket,computed),"normal full turn "+row.id);verify(game);
                    byte[] saved=game.captureSave();try(GameSession restored=new GameSession(SaveCodec.decode(saved))){
                        verify(restored);check(Arrays.equals(saved,restored.captureSave()),"complete save roundtrip/RNG");
                        World control=SaveCodec.decode(saved),reopened=restored.legacyView().draft;
                        check(control.nextTurn().ok&&reopened.nextTurn().ok,"next turn after restore");
                        check(Arrays.equals(SaveCodec.encode(control),SaveCodec.encode(reopened)),"complete next-turn continuation/RNG");
                    }
                }
                check(retained.officers.get(0).name.equals(name)&&retained.officers.get(0).current.equals(values),"retained DTO unchanged");
                byte[] saved=game.captureSave();Throwable[] failure={null};Thread other=new Thread(()->{try{game.officers();}catch(Throwable e){failure[0]=e;}});other.start();other.join();
                check(failure[0] instanceof IllegalStateException&&Arrays.equals(saved,game.captureSave()),"wrong thread rejected");
                game.close();boolean closed=false;try{game.officers();}catch(IllegalStateException expected){closed=true;}check(closed,"closed session rejected");
            }
            System.out.println("PASS officer query scenario="+row.id);
        }
        System.out.println("PASS OfficerQueryTest checks="+checks+" nine actual project scenarios, normal patrol, three turns/save/reopen/continuation, full-save/RNG, immutable and thread guards; PC opening fidelity still pending");
    }
}
