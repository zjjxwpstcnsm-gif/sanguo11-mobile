package game.sanguo.core;
import java.io.*;import java.nio.file.*;import java.util.*;
import game.sanguo.api.*;import game.sanguo.runtime.*;

/** Actual source new games, normal commands/turns/DTO and separate-JVM continuation. */
public final class PcOfficerCampaignFactsTest {
    private static int checks;
    private static void require(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static void verify(GameSession game,Map<Integer,PcOfficerCampaignFacts.Fact> expected)throws Exception{
        byte[] before=game.captureSave();OfficerSnapshot dto=game.officers();
        require(Arrays.equals(before,game.captureSave()),"normal query preserves full World/RNG");
        require(dto.officers.size()==670&&expected.size()==670,"all actual source officers, distinct from strict identity count");
        for(PcOfficerCampaignFacts.Fact fact:expected.values()){
            OfficerSnapshot.Officer view=dto.officer(fact.officerId);
            require(view!=null&&view.source!=null&&view.source.nativeId==fact.nativeId&&view.source.recordSha.equals(fact.recordSha),"identity/record SHA connection");
            require(view.source.initialRawLoyalty!=null&&view.source.initialRawLoyalty==fact.initialRawLoyalty,"saved original raw loyalty DTO");
            require(view.source.originalInformation.contains("原记录内部忠诚："+fact.initialRawLoyalty),"normal detail text uses same DTO");
        }
    }
    private static void turn(GameSession game,World control)throws Exception{
        TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
        require(control.nextTurn().ok&&computed.nextTurn().ok&&game.commitTurn(ticket,computed),"normal full turn transaction");
        require(Arrays.equals(game.captureSave(),SaveCodec.encode(control)),"complete World/RNG matches independent control");
    }
    public static void main(String[] args)throws Exception{
        String mode=args.length==0?"emit":args[0];Path folder=Path.of(args.length>1?args[1]:"out/session1/contest22/cold");
        if(mode.equals("continue")){
            for(int index=0;index<16;index++){
                byte[] middle=Files.readAllBytes(folder.resolve(index+"-mid.sg11"));World w=SaveCodec.decode(middle);World control=SaveCodec.decode(middle);
                Map<Integer,PcOfficerCampaignFacts.Fact> facts=PcOfficerCampaignFacts.saved(w);
                try(GameSession game=new GameSession(w)){require(Arrays.equals(middle,game.captureSave()),"new-JVM complete saved bytes");verify(game,facts);turn(game,control);
                    require(Arrays.equals(Files.readAllBytes(folder.resolve(index+"-expected.sg11")),game.captureSave()),"separate-JVM fourth-turn continuation vs live control");verify(game,facts);}
            }
            System.out.println("PASS PcOfficerCampaignFactsCold "+checks+" all16 actual saves and complete World/RNG continuation");return;
        }
        Files.createDirectories(folder);int index=0;
        for(PcScenarioCatalog.Source source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);Map<Integer,PcOfficerCampaignFacts.Fact> facts=PcOfficerCampaignFacts.saved(w);
            byte[] base=SaveCodec.encode(w);require(base[7]==38,"existing source version remains38");World control=SaveCodec.decode(base);
            try(GameSession game=new GameSession(w)){
                verify(game,facts);World.City city=control.home();World.Officer actor=control.idle(city).get(0);
                require(control.patrol(city.id,actor.id).ok,"actual source patrol control");StateToken token=game.state();
                require(game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor.id)).ok(),"normal typed patrol");
                require(!game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor.id)).ok(),"stale input rejected");
                require(Arrays.equals(SaveCodec.encode(control),game.captureSave()),"normal command complete parity");
                for(int turn=0;turn<3;turn++)turn(game,control);verify(game,facts);
                Files.write(folder.resolve(index+"-mid.sg11"),game.captureSave());turn(game,control);Files.write(folder.resolve(index+"-expected.sg11"),game.captureSave());
            }
            // Model a previously saved source world without this new snapshot.
            World old=SaveCodec.decode(base);old.extensions.put(PcOfficerCampaignFacts.NAMESPACE,null);byte[] legacy=SaveCodec.encode(old);
            try(GameSession game=new GameSession(SaveCodec.decode(legacy))){
                require(PcOfficerCampaignFacts.saved(SaveCodec.decode(game.captureSave())).isEmpty(),"old source does not acquire metadata");
                for(OfficerSnapshot.Officer o:game.officers().officers)require(o.source.initialRawLoyalty==null&&o.unknown.contains("originalHiddenLoyaltyMissingFromSave"),"old source displays unknown");
                require(Arrays.equals(legacy,game.captureSave()),"old source full save/RNG unchanged by query");
            }
            boolean duplicate=false;try{PcOfficerCampaignFacts.initializeOpening(SaveCodec.decode(base),source);}catch(IOException expected){duplicate=true;}require(duplicate,"no repeated initialization");
            System.out.println("PASS original campaign facts "+source.identity.path+" saved/normal DTO/patrol/four turns/legacy no backfill");index++;
        }
        require(index==16,"all16 independent sources");
        World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(0).identity.scenarioId);World.Officer target=null;
        for(PcOfficerCampaignFacts.Fact f:PcOfficerCampaignFacts.saved(w).values())if(f.nativeId==116)target=w.officer(f.officerId);
        require(target!=null&&target.loyalty==100&&PcOfficerCampaignFacts.saved(w).get(target.id).initialRawLoyalty==120,"source0 displayed100 versus actual saved raw120");
        byte[] valid=w.extensions.get(PcOfficerCampaignFacts.NAMESPACE),future=valid.clone();future[3]++;w.extensions.put(PcOfficerCampaignFacts.NAMESPACE,future);byte[] unknown=SaveCodec.encode(w);
        try(GameSession game=new GameSession(SaveCodec.decode(unknown))){OfficerSnapshot view=game.officers();require(view.officers.get(0).source.initialRawLoyalty==null,"unknown snapshot is not invented");require(Arrays.equals(unknown,game.captureSave()),"unknown extension and full authority preserved");}
        System.out.println("PASS PcOfficerCampaignFactsTest "+checks+" actual16 source worlds/current displayed loyalty preserved; strict identity coverage still666 per source");
    }
}
