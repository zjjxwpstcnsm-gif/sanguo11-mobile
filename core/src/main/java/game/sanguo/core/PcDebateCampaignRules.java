package game.sanguo.core;
import java.io.*;import java.util.*;
/** Complete original numeric settlement and ordinary recruitment-result callback.
 * Admission, native army action budgets and presentation are separate policies. */
final class PcDebateCampaignRules {
 static PcScenarioPeople.Person source(World w,int id)throws IOException {for(var p:PcScenarioPeople.saved(w))if(p.officerId==id)return p;throw new IOException("Original campaign identity absent");}
 static int joinLoyalty(World w,World.Officer target,int owner)throws IOException {
  World.Officer ruler=w.loyalty.ruler(owner);if(ruler==null)throw new IOException("Original current ruler absent");
  var t=source(w,target.id);var r=source(w,ruler.id);int gap=Loyalty.distance(target,ruler);if(gap<0)throw new IOException("Original affinity input absent");
  int father=w.relations.parent(target.id,false),rulerFather=w.relations.parent(ruler.id,false);
  // Display0..100 is exact even when an existing bonded raw value120..255 is unknown.
  // Never claim the capped result as a recovered current raw-loyalty byte.
  int raw=PcOfficerJoinRules.loyalty(target.loyalty,gap,target.honor-1,t.field(45),ruler.charm,false,false,w.relations.spouse(target.id)==ruler.id,w.relations.sworn(target.id,ruler.id),w.relations.likes(target.id,ruler.id),w.relations.dislikes(target.id,ruler.id),father>=0&&father==rulerFather,t.field(49)==1&&r.field(49)==1,t.field(41)==r.field(41));
  return Math.max(0,Math.min(100,raw));
 }
 static boolean guided(World w,World.Officer person){World.Unit u=w.unit(person.unitId);if(u==null)return false;for(var mate:w.army.crew(u))if(mate.id!=person.id&&w.life.present(mate.id)&&w.skills.has(mate,Skill.ZHIDAO))return true;return false;}
 static void terminalRewards(World w,World.Officer actor,World.Officer target,int winner,int outcome)throws IOException {
  source(w,actor.id);source(w,target.id);World.Officer[] people={actor,target};int[] owners={actor.owner,target.owner},points=new int[w.factions.length];for(int i=0;i<points.length;i++)points[i]=w.campaign.points(i);
  var rewards=PcDebateSettlement.calculate(winner,outcome,new int[]{w.government.merit(actor.id),w.government.merit(target.id)},new int[]{w.officerAbilities.experience(actor.id,2),w.officerAbilities.experience(target.id,2)},new int[]{PcNativeHealthPolicy.injury(w,actor.id),PcNativeHealthPolicy.injury(w,target.id)},owners,points,new boolean[]{guided(w,actor),guided(w,target)},new boolean[]{w.life.present(actor.id),w.life.present(target.id)});
  for(int i=0;i<2;i++){var p=people[i];w.government.earn(p.id,rewards.merit[i]-w.government.merit(p.id));w.officerAbilities.gainExperience(p.id,2,rewards.intelligenceExperience[i]-w.officerAbilities.experience(p.id,2));PcNativeHealthPolicy.setInjury(w,p.id,rewards.injury[i]);}
  for(int i=0;i<points.length;i++)if(rewards.techniquePoints[i]!=points[i])w.campaign.setPoints(i,rewards.techniquePoints[i],TechniquePointsJournal.Cause.DEBATE,-1,winner==0?actor.id:target.id);
 }
 /** Original5d3c90(target,actor,success), including5c4840/4a75a0 and rewards. */
 static void recruitmentResult(World w,World.Officer actor,World.Officer target,int city,boolean success)throws IOException {
  source(w,actor.id);source(w,target.id);
  if(success){int loyalty=joinLoyalty(w,target,actor.owner);w.strategy.releaseGovernor(target.id);w.government.allegianceChanged(target.id);target.owner=actor.owner;target.role=Strategy.Role.OFFICER;target.cityId=city;target.unitId=-1;target.loyalty=loyalty;target.lastRewardTurn=-1;target.acted=true;PcGovernorPolicy.joined(w,target,actor,city);
   w.campaign.setPoints(actor.owner,PcTechniquePoints.after(w.campaign.points(actor.owner),20+target.charm/3),TechniquePointsJournal.Cause.DEBATE,city,actor.id);
  }
  w.government.earn(actor.id,success?200:100);w.officerAbilities.gainExperience(actor.id,3,3*(guided(w,actor)?2:1));if(success)w.officerAbilities.gainExperience(actor.id,4,5*(guided(w,actor)?2:1));actor.acted=true;w.governance.reconcile(false);
 }
 private PcDebateCampaignRules(){}
}
