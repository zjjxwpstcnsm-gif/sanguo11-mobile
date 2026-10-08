package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Read-only actual ordinary battle endpoints. No model or outcome edits. */
public final class PcDuelNormalTerminalResultTest {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  byte[]terminalBytes=Files.readAllBytes(Path.of("out/session-b/normal-deployed-duel-contest-v5.sg11"));
  byte[]finishedBytes=Files.readAllBytes(Path.of("out/session-b/normal-deployed-duel-finished-v5.sg11"));
  byte[]futureBytes=Files.readAllBytes(Path.of("out/session-b/normal-deployed-duel-current-v5.sg11"));
  World before=SaveCodec.decode(terminalBytes),after=SaveCodec.decode(finishedBytes),future=SaveCodec.decode(futureBytes);
  var duel=before.contests.session.nativeDuel;int dead=PcDuelNaturalDeathSessionTest.stable(before,503),winner=PcDuelNaturalDeathSessionTest.stable(before,411),helper=PcDuelNaturalDeathSessionTest.stable(before,669);
  check(duel.frames==177&&duel.inputs==33&&duel.terminal()&&duel.state.officers[0]==dead&&duel.state.officers[3]==winner&&duel.state.officers[4]==helper,"actual ordinary177frames/33inputs, no declaration endpoint");
  check(PcDuelKernel.readManager(duel.state.manager,0x54)==1&&PcDuelKernel.readManager(duel.state.manager,0x64)==2,"actual generated AI victory/player native503 death");
  check(after.life.state(dead)==Lifecycle.State.DEAD&&after.officer(dead).owner==-1&&after.officer(dead).cityId==-1&&after.officer(dead).unitId==-1&&!after.government.captive(dead),"normal death full identity/lifecycle; no captured menu");
  var a=PcGovernorPolicy.data(after).assignments.get(dead);check(a.army==-1&&a.home==-1&&after.government.merit(dead)==0&&after.officerAbilities.experience(dead,1)==0&&PcDuelHealthPolicy.health(after,dead)==1,"normal death cleanup/physicalHP1/no dead rewards");
  check(after.unit(41)==null&&after.unit(6)!=null&&after.unit(6).troops==before.unit(6).troops&&after.unit(6).gold==before.unit(6).gold&&after.unit(6).food==before.unit(6).food,"losing sole unit removed, no cargo/troops confiscation");
  check(after.government.merit(winner)==before.government.merit(winner)+100,"actual active winner natural100 merit, no capture200");
  check(before.skills.has(before.officer(helper),Skill.ZHIDAO)&&after.officerAbilities.experience(winner,1)==before.officerAbilities.experience(winner,1)+20,"actual original LuZhi guidance crew doubles winner WARXP10 to20");
  check(after.government.merit(helper)==before.government.merit(helper)&&after.officerAbilities.experience(helper,1)==before.officerAbilities.experience(helper,1),"waiting support no invented reward");
  check(PcNativeDebatePolicy.seed(after)==duel.state.random.state&&Integer.toUnsignedLong(PcNativeDebatePolicy.seed(after))==321129023L&&after.strategy.getRandomState()==before.strategy.getRandomState(),"actual ordinary terminal exact both RNG/no removed-unit troop draw");
  for(int id:new int[]{winner,helper}){boolean unknown=false;try{PcDuelRawLoyalty.current(after,id);}catch(java.io.IOException e){unknown=e.getMessage().equals("当前原始忠诚变更尚未核实");}check(unknown,"live unconsumed raw remains unknown, no byte/display backfill "+id);}
  check(after.turn==25&&future.turn==28&&future.life.state(dead)==Lifecycle.State.DEAD&&!after.contests.busy()&&!future.contests.busy()&&PcDuelCampaignPolicy.lastFinished(after)==before.contests.session.id,"one normal terminal receipt and3actual future turns");
  check(Arrays.equals(terminalBytes,SaveCodec.encode(before))&&Arrays.equals(finishedBytes,SaveCodec.encode(after))&&Arrays.equals(futureBytes,SaveCodec.encode(future)),"all three complete actual World/RNG checkpoints byte exact");
  System.out.println("PASS actual normal native503 natural death/rewards/cargo/unknown raw/fullWorld/bothRNG/28turn continuation "+checks+" checks; Android menu/APK separate");
 }
}
