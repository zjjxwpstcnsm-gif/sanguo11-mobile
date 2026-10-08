package game.sanguo.core;
import java.nio.file.*;import java.util.*;import java.util.regex.*;import game.sanguo.api.*;import game.sanguo.runtime.GameSession;
/** Replays only real recorded legal human inputs over actual immutable Android saves. */
public final class ActualSearchDebateReplay {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static byte[] canonical(byte[] b)throws Exception{return SaveCodec.encode(SaveCodec.decode(b));}
 static void turn(GameSession g)throws Exception{var t=g.beginTurn();World w=SaveCodec.decode(t.initial());check(w.nextTurn().ok&&g.commitTurn(t,w),"ordinary whole turn");}
 static final class Input{int revision,card;Input(int r,int c){revision=r;card=c;}}
 public static void main(String[]args)throws Exception{
  Path root=Path.of(args[0]);Path output=Path.of(args[1]);Files.createDirectory(output);String log=Files.readString(root.resolve("search/diagnosis.txt"));Pattern p=Pattern.compile("human contest=([0-9]+) revision=([0-9]+) phase=([0-9]+) round=([0-9]+) card=([0-9]+)");List<List<Input>> groups=new ArrayList<>();List<Input> group=null;
  for(String line:log.split("\\n")){Matcher m=p.matcher(line);if(!m.find())continue;int rev=Integer.parseInt(m.group(2));if(rev==0){group=new ArrayList<>();groups.add(group);}check(group!=null,"recorded input group starts at actual entry");group.add(new Input(rev,Integer.parseInt(m.group(5))));}
  byte[] discovered=Files.readAllBytes(root.resolve("search/discovered-choice.sg11")),declined=Files.readAllBytes(root.resolve("search/declined-search.sg11"));try(GameSession g=new GameSession(SaveCodec.decode(discovered))){var f=g.contest();check(f.kind==ContestSnapshot.Kind.SEARCH_CHOICE&&f.phase==1,"actual discovery saved stage");var cmd=ContestCommand.searchChoice(f.state,f.contestId,f.revision,false);check(g.execute(cmd).ok(),"ordinary actual decline search");byte[] done=g.captureSave();check(Arrays.equals(canonical(declined),canonical(done)),"actual decline fullWorld/allRNG exact replay");check(!g.execute(cmd).ok()&&Arrays.equals(done,g.captureSave()),"decline stale/double has no second callback");}System.out.println("PASS actual discovered-choice->decline once fullWorld/allRNG replay");int wins=0,losses=0,trials=0;
  for(int n=0;Files.exists(root.resolve("search/trial-"+n+"-finished.sg11"));n++){
   byte[] mid=Files.readAllBytes(root.resolve("search/trial-"+n+"-mid.sg11")),finished=Files.readAllBytes(root.resolve("search/trial-"+n+"-finished.sg11"));check(n<groups.size(),"actual trial inputs present");World initial=SaveCodec.decode(mid);int target=initial.contests.current().rightRef;int index=0,commands=0;List<Input> recorded=groups.get(n);
   try(GameSession g=new GameSession(initial)){
    while(g.contest().kind!=ContestSnapshot.Kind.NONE&&commands++<500){var f=g.contest();ContestCommand cmd;
     if(f.waitingMercy)cmd=ContestCommand.finishDebate(f.state,f.contestId,f.revision,true);
     else if(f.phase==9)cmd=ContestCommand.finishDebate(f.state,f.contestId,f.revision,false);
     else{while(index<recorded.size()&&recorded.get(index).revision<f.revision)index++;check(index<recorded.size(),"recorded actual card present trial="+n+" revision="+f.revision);Input input=recorded.get(index++);check(input.revision==f.revision,"actual recorded revision");var card=f.cards.stream().filter(c->c.enabled()&&c.nativeCard==input.card).findFirst().orElseThrow();cmd=ContestCommand.card(f.state,f.contestId,f.revision,card.slot);}
     check(g.execute(cmd).ok(),"ordinary committed recorded human/terminal input");byte[] committed=g.captureSave();check(Arrays.equals(committed,SaveCodec.encode(SaveCodec.decode(committed))),"every full native model/World/RNG roundtrip");check(!g.execute(cmd).ok()&&Arrays.equals(committed,g.captureSave()),"same actual command stale/double pure");
    }
    check(g.contest().kind==ContestSnapshot.Kind.NONE,"actual terminal released");World ended=SaveCodec.decode(g.captureSave());boolean won=ended.officer(target).owner==ended.player;if(won)wins++;else losses++;turn(g);Files.write(output.resolve("trial-"+n+"-replayed.sg11"),g.captureSave());check(Arrays.equals(canonical(finished),canonical(g.captureSave())),"actual Android finished save equals full replayed human/terminal/wholeturn World/allRNG trial="+n);System.out.println("PASS actual trial="+n+" source="+ended.scenarioId+" player="+ended.player+" target="+target+" won="+won+" commands="+commands+" fullWorldAllRngCanonical=true raw="+Arrays.equals(finished,g.captureSave()));
   }
   check(Arrays.equals(mid,Files.readAllBytes(root.resolve("search/trial-"+n+"-mid.sg11")))&&Arrays.equals(finished,Files.readAllBytes(root.resolve("search/trial-"+n+"-finished.sg11"))),"actual save artifacts immutable");trials++;
  }
  check(trials>0&&wins>0&&losses>0,"actual saved natural win/loss present");System.out.println("PASS actual normal SEARCH full-model legal human cards/once terminal/stale double/fullWorld/allRNG/wholeturn replay trials="+trials+" wins="+wins+" losses="+losses+"; full original gates/concession/diplomacy/ARM still unknown");
 }
}
