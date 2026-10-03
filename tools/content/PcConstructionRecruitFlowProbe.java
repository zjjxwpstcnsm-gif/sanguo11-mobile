import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.nio.file.*;
import java.util.*;
/** Real startup -> paid barracks construction -> full turns -> paid recruitment. */
public final class PcConstructionRecruitFlowProbe {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);int completed=0;
  for(var row:ScenarioCatalog.summaries()){
   GameSession game=new GameSession(ScenarioCatalog.load(row.id,0,20261003L));World direct=SaveCodec.decode(game.captureSave());ConstructionCommand build=null;
   find:for(var city:direct.cities)if(city.owner==direct.player)for(var actor:direct.idle(city))for(var hex:direct.domestic.buildSites(city.id)){
    var c=new ConstructionCommand(game.state(),city.id,actor.id,"BARRACKS",hex.q,hex.r);if(game.preview(c).allowed()){build=c;break find;}
   }
   check(build!=null,"production scenario has legal barracks site "+row.id);var plan=game.preview(build);
   check(plan.resources.goldCost==300&&plan.resources.actionPointsCost==20&&plan.completion.level==1,"native barracks price/AP/base level");
   check(direct.domestic.build(build.cityId,build.officerId,Domestic.Kind.BARRACKS,new Hex(build.q,build.r)).ok&&game.execute(build).ok(),"ordinary paid construction");
   check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"construction full-save/RNG parity");
   for(int n=0;n<plan.completion.turns;n++){
    var ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"ordinary full construction turn");byte[] saved=game.captureSave();check(Arrays.equals(saved,SaveCodec.encode(direct)),"all construction continuation state and RNG identical");game.close();game=new GameSession(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"reopen every construction turn");
   }
   check(direct.domestic.at(new Hex(build.q,build.r)).remaining==0,"barracks really completed");CityActionCommand recruit=null;
   for(var actor:direct.idle(direct.city(build.cityId))){var c=new CityActionCommand(game.state(),"RECRUIT",build.cityId,actor.id,new int[0]);if(game.preview(c).allowed()){recruit=c;break;}}
   check(recruit!=null,"normal recruitment unlocked after construction "+row.id);var preview=game.preview(recruit);check(preview.resources.actionPointsCost==20,"native recruit AP");
   check(direct.strategy.recruitSoldiers(recruit.cityId,recruit.officerId).ok&&game.execute(recruit).ok(),"ordinary paid recruitment");
   byte[] saved=game.captureSave();check(Arrays.equals(saved,SaveCodec.encode(direct)),"recruit full-save/RNG parity");var c=direct.city(recruit.cityId);
   check(c.troops==preview.effects.troopsAfter&&c.recruitReserve==preview.effects.reserveAfter&&c.morale==preview.effects.moraleAfter,"actual soldiers, reserve and morale");
   check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"final portable save reads byte exact");Files.write(out.resolve(row.id+"-built-recruited.sg11"),saved);System.out.println("PASS "+row.id+" city="+c.id+" constructionTurns="+plan.completion.turns+" recruited="+(preview.effects.troopsAfter-preview.effects.troopsBefore));game.close();completed++;
  }
  System.out.println("PASS checks="+checks+" scenarios="+completed+"; current engine workflow/save evidence, not complete PC formula or Android UI verification");
 }
}
