package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Actual original comparator and declared unit preservation; ordinary trigger separate. */
public final class PcDuelReplacementTest {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  Map<Integer,PcDuelReplacement.Candidate>facts=new HashMap<>();int rows=0;
  for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");if(p[0].equals("P")){int n=Integer.parseInt(p[1]);facts.put(n,new PcDuelReplacement.Candidate(n,n,Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]),Integer.parseInt(p[6])));}else if(p[0].equals("R")){rows++;check((PcDuelReplacement.compare(facts.get(Integer.parseInt(p[1])),facts.get(Integer.parseInt(p[2])))<0)==p[3].equals("1"),"exact original4cf160 comparator");}}
  check(rows==36,"all6x6 actual source pairs");
  World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var source=PcDuelSourceFacts.saved(w);int[]natives={558,14,517},ids=new int[3];for(int i=0;i<3;i++){final int n=natives[i];ids[i]=source.values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}
  World.Unit u=new World.Unit(1,w.officer(ids[0]).owner,ids[0],World.Weapon.SWORD,new Hex(1,1),5000,9999);u.deputies=new int[]{ids[1],ids[2]};u.gold=777;u.energy=95;u.wounded=73;u.woundRemainder=9;u.movementBudget=22;u.movementSpent=3;u.acted=true;w.units.add(u);w.nextUnitId=2;for(int id:ids){var o=w.officer(id);o.unitId=1;o.cityId=-1;}
  check(PcDuelReplacement.select(w,u,ids[0])==ids[2],"original linked case picks native517 ruler over other deputy");
  int next=PcDuelReplacement.remove(w,u,ids[0]);check(next==ids[2]&&w.unit(1)==u&&u.officerId==ids[2]&&Arrays.equals(u.deputies,new int[]{ids[1]}),"same stable unit, exact remaining crew order");
  check(u.troops==5000&&u.food==9999&&u.gold==777&&u.wounded==73&&u.woundRemainder==9&&u.movementBudget==22&&u.movementSpent==3&&u.acted,"cargo wounds orders and action preserved");
  check(u.energy==Math.min(95,w.campaign.energyCap(u.owner)),"new leader original energy clamp");
  System.out.println("PASS original replacement "+checks+" checks; declared unit, ordinary admission/complete casualty still pending");
 }
}
