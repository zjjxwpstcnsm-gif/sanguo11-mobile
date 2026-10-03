import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.nio.file.*;
import java.util.*;
import java.security.*;
/** Unmodified production scenarios, ordinary commands, full turns and reopen/save parity. */
public final class PcCityActionFlowProbe {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);int executed=0,unavailable=0;
  for(var row:ScenarioCatalog.summaries())for(var op:CityActionPlan.Operation.values()){
   GameSession game=new GameSession(ScenarioCatalog.load(row.id,0,20261003L));byte[] before=game.captureSave();StateToken token=game.state();World direct=SaveCodec.decode(before);CityActionCommand selected=null;
   find:for(var c:direct.cities)if(c.owner==direct.player)for(var actor:direct.idle(c)){
    int[] targets=new int[0];
    if(op==CityActionPlan.Operation.REWARD){var choices=new ArrayList<Integer>();for(var o:direct.officers)if(direct.strategy.rewardable(c,o)){choices.add(o.id);if(choices.size()==2)break;}targets=choices.stream().mapToInt(i->i).toArray();}
    if(op==CityActionPlan.Operation.APPOINT_GOVERNOR){var candidate=direct.idle(c).stream().filter(o->o.id!=c.governorId).findFirst();if(candidate.isEmpty())continue;targets=new int[]{candidate.get().id};}
    CityActionCommand command=new CityActionCommand(token,op.name(),c.id,actor.id,targets);CityActionPreview p=game.preview(command);
    check(p.error!=CommandResult.Error.HOST_ERROR,"production query has real rule reason");
    if(p.allowed()){selected=command;break find;}
   }
   check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"production queries pure");
   if(selected==null){System.out.println("UNAVAILABLE "+row.id+" "+op+" in unchanged startup state");unavailable++;game.close();continue;}
   var preview=game.preview(selected);check(direct.strategy.executeCityAction(op,selected.cityId,selected.officerId,selected.targets()).ok,"normal production core command");check(game.execute(selected).ok(),"normal production typed command");
   check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"production save/RNG exact after command");
   check(game.execute(selected).error==CommandResult.Error.STALE_REVISION,"no duplicate production debit");
   for(int n=0;n<3;n++){var ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"normal production full turn");byte[] saved=game.captureSave();check(Arrays.equals(saved,SaveCodec.encode(direct)),"all production turn state and RNG identical");game.close();game=new GameSession(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"fresh session/save reopen every turn");}
   byte[] saved=game.captureSave();Files.write(out.resolve(row.id+"-"+op+".sg11"),saved);System.out.println("PASS "+row.id+" "+op+" AP="+preview.resources.actionPointsCost+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(saved)));executed++;game.close();
  }
  check(executed>0,"real scenarios executed");System.out.println("PASS checks="+checks+" executed="+executed+" unavailable="+unavailable+"; engine/save proof, not complete PC or Android UI parity");
 }
}
