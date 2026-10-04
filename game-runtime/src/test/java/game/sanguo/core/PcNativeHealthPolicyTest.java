package game.sanguo.core;
import java.nio.file.*;import java.util.*;
import game.sanguo.api.*;import game.sanguo.runtime.*;

/** Real source campaign turns, health ability refresh, saved strategies and cold continuation. */
public final class PcNativeHealthPolicyTest {
    private static int checks;
    static void require(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static int expected(int turn){return turn<3?3:turn<9?2:turn<15?1:0;}
    static void turns(GameSession game,World control,int end,int officer)throws Exception{
        while(control.turn<end){TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());require(control.nextTurn().ok&&computed.nextTurn().ok&&game.commitTurn(ticket,computed),"actual campaign turn commit");
            byte[] saved=game.captureSave();require(Arrays.equals(saved,SaveCodec.encode(control)),"complete authority/control World and both RNGs");
            require(control.contests.injury(officer)==expected(control.turn),"original even-ID/even-month recovery level");
            byte[] prior=SaveCodec.encode(control);PcNativeHealthPolicy.tick(control);require(Arrays.equals(prior,SaveCodec.encode(control)),"same turn cannot heal/draw twice");
            OfficerSnapshot dto=game.officers();require(dto.officer(officer).injury==expected(control.turn),"normal officer DTO reads actual saved health");require(Arrays.equals(saved,game.captureSave()),"normal query preserves full save/RNG");
        }
    }
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args.length>1?args[1]:"out/session1/contest23/cold");String mode=args.length>0?args[0]:"emit";int officer=10116;
        if(mode.equals("continue")){byte[] middle=Files.readAllBytes(folder.resolve("mid.sg11"));World w=SaveCodec.decode(middle),control=SaveCodec.decode(middle);try(GameSession game=new GameSession(w)){require(Arrays.equals(middle,game.captureSave()),"separate JVM exact saved bytes");turns(game,control,15,officer);require(Arrays.equals(Files.readAllBytes(folder.resolve("expected.sg11")),game.captureSave()),"independent JVM full cold continuation");}System.out.println("PASS PcNativeHealthCold "+checks);return;}
        Files.createDirectories(folder);String source=PcScenarioCatalog.all().get(0).identity.scenarioId;World w=PcScenarioCatalog.preview(source);require(!PcNativeHealthPolicy.enabled(w),"normal old source retains old strategy");
        PcNativeDebatePolicy.initializeOpening(w);require(PcNativeHealthPolicy.enabled(w),"explicit fresh native policy records recovery mode");long split=w.strategy.getRandomState();int nativeSeed=PcNativeDebatePolicy.seed(w);
        PcNativeHealthPolicy.setInjury(w,officer,3);require(w.contests.injury(officer)==3&&w.officerAbilities.afterExperience(officer,1,0)==w.officer(officer).war,"setter refreshes actual current values");
        byte[] initial=SaveCodec.encode(w);require(initial[7]==39&&Arrays.equals(initial,SaveCodec.encode(SaveCodec.decode(initial))),"actual saved strategy/full World round trip");require(w.strategy.getRandomState()==split&&PcNativeDebatePolicy.seed(w)==nativeSeed,"initial injury uses neither RNG");
        World control=SaveCodec.decode(initial);try(GameSession game=new GameSession(w)){
            World.City city=control.home();World.Officer actor=control.idle(city).get(0);require(control.patrol(city.id,actor.id).ok,"normal source patrol control");StateToken token=game.state();require(game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor.id)).ok(),"real typed source patrol");require(Arrays.equals(game.captureSave(),SaveCodec.encode(control)),"normal command full World/RNG");turns(game,control,5,officer);Files.write(folder.resolve("mid.sg11"),game.captureSave());turns(game,control,15,officer);Files.write(folder.resolve("expected.sg11"),game.captureSave());
        }
        for(boolean old39:new boolean[]{false,true}){World old=PcScenarioCatalog.preview(source);if(old39){PcNativeDebatePolicy.initializeOpening(old);old.extensions.put(PcNativeHealthPolicy.NAMESPACE,null);}old.contests.injuries.put(officer,new Contests.Injury(3,old.turn+3));old.officerAbilities.refresh();byte[] saved=SaveCodec.encode(old);World restored=SaveCodec.decode(saved);require(!PcNativeHealthPolicy.enabled(restored)&&Arrays.equals(saved,SaveCodec.encode(restored)),"old38/39 not backfilled or upgraded");for(int turn=0;turn<3;turn++)require(restored.nextTurn().ok,"old source full continuation");require(restored.contests.injury(officer)==0,"old three-turn expiry remains saved strategy");}
        World invalid=SaveCodec.decode(initial);byte[] raw=invalid.extensions.get(PcNativeHealthPolicy.NAMESPACE);raw[3]++;invalid.extensions.put(PcNativeHealthPolicy.NAMESPACE,raw);boolean rejected=false;try{SaveCodec.encode(invalid);}catch(java.io.IOException expected){rejected=true;}require(rejected,"invalid explicit recovery source/schema rejected");
        System.out.println("PASS PcNativeHealthPolicyTest "+checks+" normal commands/15 full turns/saved original calendar recovery; old38/39 strategy preserved");
    }
}
