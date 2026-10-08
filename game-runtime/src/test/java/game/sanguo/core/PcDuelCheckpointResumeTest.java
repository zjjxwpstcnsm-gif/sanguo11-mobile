package game.sanguo.core;
import java.nio.file.*;import java.util.*;
import game.sanguo.api.*;import game.sanguo.runtime.GameSession;
/** Preserve and continue the actual failed seed1 save without phase/HP/RNG edits. */
public final class PcDuelCheckpointResumeTest {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of(args[0]));World initial=SaveCodec.decode(raw);check(Arrays.equals(raw,SaveCodec.encode(initial)),"load exact");
  try(var game=new GameSession(initial)){
   var start=game.contest();check(start.round==15&&!start.nativeDuel.terminal,"actual blocked round15");
   check(Arrays.equals(raw,game.captureSave()),"preview/read pure");
   var rejected=game.execute(ContestCommand.nativeDuelInput(start.state,start.contestId,start.revision,99,99,99));check(!rejected.ok()&&Arrays.equals(raw,game.captureSave()),"invalid input does not acknowledge or draw RNG");
   int inputs=0;
   while(!game.contest().nativeDuel.terminal&&inputs++<300){
    var f=game.contest();var choice=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).findFirst().orElseThrow();
    var result=game.execute(ContestCommand.nativeDuelInput(f.state,f.contestId,f.revision,choice.stance,choice.special,choice.replacement));check(result.ok(),"accepted input "+result.detail);
    byte[]accepted=game.captureSave();result=game.execute(ContestCommand.nativeDuelInput(f.state,f.contestId,f.revision,choice.stance,choice.special,choice.replacement));check(!result.ok()&&Arrays.equals(accepted,game.captureSave()),"old token/double input pure");
    byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"each fullWorld cold");
   }
   var terminal=game.contest();check(terminal.nativeDuel.terminal,"saved loop natural terminal");System.out.println("Natural saved seed1 terminal winner="+terminal.winner+" round="+terminal.round+" extraInputs="+inputs);
   var result=game.execute(ContestCommand.finishNativeDuel(terminal.state,terminal.contestId,terminal.revision));check(result.ok(),"terminal "+result.detail);
   if(game.contest().nativeDuel!=null){for(var row:game.contest().nativeDuel.disposition){var f=game.contest();result=game.execute(ContestCommand.nativeDuelDisposition(f.state,f.contestId,f.revision,row.officerId,2));check(result.ok(),"release "+result.detail);}var f=game.contest();result=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(result.ok(),"final "+result.detail);}
   check(game.contest().nativeDuel==null,"production settlement cleared once");
   for(int i=0;i<4;i++){var ticket=game.beginTurn();var next=SaveCodec.decode(ticket.initial());check(next.nextTurn().ok,"whole turn");check(game.commitTurn(ticket,next),"commit turn");byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"whole turn cold");}
   check(Arrays.equals(raw,Files.readAllBytes(Path.of(args[0]))),"original checkpoint preserved");System.out.println("PASS actual round15 saved cycle -> natural terminal -> once-only campaign ->4 whole turns/cold; APK pending");
  }
 }
}
