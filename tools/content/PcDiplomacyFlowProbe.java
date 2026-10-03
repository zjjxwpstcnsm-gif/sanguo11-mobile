import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Production catalog and normal commands; host verification, not a PC-equivalence or UI claim. */
public final class PcDiplomacyFlowProbe {
    private static int checks;
    private static void check(boolean value,String detail){checks++;if(!value)throw new AssertionError(detail);}
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);
        for(var row:ScenarioCatalog.summaries()){
            World initial=ScenarioCatalog.load(row.id,0,20261003L);GameSession game=new GameSession(initial);
            byte[] before=game.captureSave();StateToken token=game.state();DiplomacyCommand selected=null;DiplomacyPreview best=null;
            for(World.City city:initial.cities)if(city.owner==initial.player){
                List<World.Officer> idle=initial.idle(city);if(idle.isEmpty())continue;
                for(int side=0;side<initial.factions.length;side++)if(side!=initial.player&&initial.alive(side)){
                    DiplomacyCommand command=new DiplomacyCommand(token,city.id,idle.get(0).id,side,"GOODWILL",0);
                    DiplomacyPreview preview=game.preview(command);
                    check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"query preserves full scenario, RNG and revision");
                    if(preview.allowed()&&(best==null||preview.forecast.roundTripTurns<best.forecast.roundTripTurns)){selected=command;best=preview;}
                }
            }
            check(selected!=null,"real campaign has available goodwill departure "+row.id);
            World direct=SaveCodec.decode(before);long rng=direct.strategy.getRandomState();
            check(direct.campaign.goodwill(selected.cityId,selected.officerId,selected.targetSide).ok,"ordinary departure");
            check(game.execute(selected).ok(),"typed actual campaign departure");
            byte[] departed=game.captureSave();check(Arrays.equals(departed,SaveCodec.encode(direct)),"typed ordinary save equality");
            check(direct.strategy.getRandomState()==rng&&direct.city(selected.cityId).gold==best.resources.goldRemaining,"real cost and no departure RNG");
            check(game.execute(selected).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(departed,game.captureSave()),"double confirmation has no effects");
            int turns=best.forecast.roundTripTurns;check(turns<=20,"bounded real round trip");
            for(int n=0;n<turns;n++){
                TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                check(computed.nextTurn().ok&&direct.nextTurn().ok,"ordinary full turn");check(game.commitTurn(ticket,computed),"one turn commit");
                byte[] after=game.captureSave();check(Arrays.equals(after,SaveCodec.encode(direct)),"multi-turn RNG and complete authority equality");
                game.close();game=new GameSession(SaveCodec.decode(after));check(Arrays.equals(after,game.captureSave()),"save load between every turn");
            }
            World finish=SaveCodec.decode(game.captureSave());int actor=selected.officerId;
            check(finish.envoys.missions().stream().noneMatch(m->m.actor==actor),"original envoy completed or was normally canceled");
            byte[] result=game.captureSave();Files.write(output.resolve(row.id+"-diplomacy.sg11"),result);
            String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result));
            System.out.println("PASS scenario="+row.id+" city="+selected.cityId+" target="+selected.targetSide+" roundTrip="+turns+" sha256="+sha);game.close();
        }
        System.out.println("PASS "+checks+" production-catalog diplomacy/session/full-turn/save checks; no UI or PC-parity claim");
    }
}
