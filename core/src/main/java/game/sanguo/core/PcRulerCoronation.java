package game.sanguo.core;
import java.io.*;import java.util.*;
/** Original4b78f0 weighted loyalty plan, including the temporarily
 * demoted departing ruler before4acae0 clears its terminal raw value.
 * Reads are pure; the validated AI execution callback performs
 * administrative writes without an engineering succession fallback. */
final class PcRulerCoronation {
 static final class Plan {
  final PcRulerSuccession.Plan succession;final int primaryArmy,heirArmy,homeCity,heirNative;
  final Map<Integer,Integer> loyalty;
  Plan(PcRulerSuccession.Plan succession,int primary,int army,int home,int nativeId,Map<Integer,Integer> loyalty){this.succession=succession;primaryArmy=primary;heirArmy=army;homeCity=home;heirNative=nativeId;this.loyalty=loyalty;}
 }
 static Plan preview(World w,int departed)throws IOException {
  return preview(w,PcRulerSuccession.preview(w,departed));
 }
 static Plan preview(World w,PcRulerSuccession.Plan succession)throws IOException {
  if(succession.selected<0)throw new IOException("原无人继承的势力解散尚未闭合");
  var data=PcGovernorPolicy.data(w);var heir=w.officer(succession.selected);var a=data.assignments.get(heir.id);int primary=PcArmyActionPolicy.primaryArmy(w,succession.owner);
  if(a==null||primary<0||a.army<0||data.mergedArmies.containsKey(a.army)||data.mergedArmies.containsKey(primary)||!Objects.equals(data.source.armyOwners.get(a.army),succession.owner))throw new IOException("原登位当前军团连接尚未覆盖");
  int city=PcPersonnelReturnRules.projectSite(w,a.home);if(w.city(city)==null||w.city(city).owner!=succession.owner||data.unknownSites.contains(city))throw new IOException("原登位当前行政驻点尚未覆盖");
  return new Plan(succession,primary,a.army,city,PcDuelSourceFacts.saved(w).get(heir.id).nativeId,loyalty(w,succession));
 }
 static Map<Integer,Integer> loyalty(World w,PcRulerSuccession.Plan succession)throws IOException {
  if(succession.selected<0)throw new IOException("原无人继承的势力解散尚未闭合");
  var ruler=w.officer(succession.selected);var facts=PcDuelSourceFacts.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);var rf=facts.get(ruler.id);var rp=runtime.people.get(rf.nativeId);
  PcDuelKinship.unchangedParents(w);PcDuelRecruitmentAdmission.unchangedGroup(w,ruler.id,runtime,facts);
  // Original rank80 has no ability bonus. The prospective getter retains
  // current age/base/growth/XP/injury/spouse without cloning a whole World
  // once per UI candidate, and never writes the current ability cache.
  int charm=w.officerAbilities.currentWithoutRank(ruler.id,4);
  Map<Integer,Integer> result=new LinkedHashMap<>();var rs=PcDebateCampaignRules.source(w,ruler.id);
  List<Integer> roster=new ArrayList<>(succession.candidates);roster.add(succession.ruler);roster.sort(Comparator.comparingInt(id->facts.get(id).nativeId));
  for(int id:roster){
   if(id==ruler.id)continue;var o=w.officer(id);var f=facts.get(id);var p=runtime.people.get(f.nativeId);var s=PcDebateCampaignRules.source(w,id);PcDuelRecruitmentAdmission.unchangedGroup(w,id,runtime,facts);
   int gap=Loyalty.distance(o,ruler);if(gap<0)throw new IOException("原登位忠诚当前相性未知");
   // Original48bb70 compares internal roots of target and referenced ruler,
   // requiring a valid ordinary root and excluding the reference itself.
   boolean root=p.root>=0&&p.root<1100&&p.root==rp.root;
   int raw=PcOfficerJoinRules.loyalty(PcDuelRawLoyalty.current(w,id),gap,o.honor-1,s.field(45),charm,true,false,w.relations.spouse(id)==ruler.id,w.relations.sworn(id,ruler.id),w.relations.likes(id,ruler.id),w.relations.dislikes(id,ruler.id),root,s.field(49)==rs.field(49),s.field(41)==rs.field(41));
   result.put(id,raw);
  }
  return Collections.unmodifiableMap(result);
 }
 static Plan validateExecution(World w,int departed)throws IOException {
  return validateExecution(w,departed,-1);
 }
 static Plan validateExecution(World w,int departed,int chosenHeir)throws IOException {
  var candidates=PcRulerSuccession.preview(w,departed);
  if(candidates.owner==w.player&&candidates.candidates.size()>1){if(chosenHeir<0)throw new IOException("请先选择本势力继承人");candidates=candidates.choose(chosenHeir);}
  else if(chosenHeir>=0)throw new IOException("当前继承无需玩家选择");
  Plan p=preview(w,candidates);
  validateMerge(w,p);
  var heir=w.officer(p.succession.selected);
  if(heir.otherTaskTurns!=0||w.domestic.busy(heir.id)||PcDuelRelease.busy(w,heir.id))throw new IOException("原登位继承者任务重建尚未闭合");
  if(heir.unitId>=0){
   if(!PcDuelEscortRelease.current(w))throw new IOException("旧策略的原登位继承者部队重建尚未闭合");
   var unit=w.unit(heir.unitId);
   if(unit==null||unit instanceof Domestic.Mission||unit.owner!=heir.owner||!w.army.contains(unit,heir.id))throw new IOException("原登位继承者当前编队连接无效");
   for(var member:w.army.crew(unit))PcDuelReplacement.candidate(w,member.id);
  }
  return p;
 }
 static void validateMerge(World w,Plan p)throws IOException {
  if(p.heirArmy==p.primaryArmy)return;var data=PcGovernorPolicy.data(w);
  PcDuelCapture.validateMerge(w,data,p.heirArmy,p.primaryArmy,p.succession.owner,true);
  for(var person:data.source.people.values())if(person.id<0&&person.allowed&&person.army==p.heirArmy)throw new IOException("原登位合并军团含未连接的有效人物");
 }
 static void apply(World w,Plan p)throws IOException {
  var departed=w.officer(p.succession.ruler);var heir=w.officer(p.succession.selected);var data=PcGovernorPolicy.data(w);
  var departedAssignment=data.assignments.get(departed.id);var oldHome=w.city(PcPersonnelReturnRules.projectSite(w,departedAssignment.home));
  departed.role=departedAssignment.home!=data.assignments.get(heir.id).home&&oldHome!=null&&w.governance.resident(departed,oldHome)?Strategy.Role.GOVERNOR:Strategy.Role.OFFICER;
  w.government.ranks.remove(heir.id);w.government.advisors.values().removeIf(id->id==heir.id);heir.role=Strategy.Role.RULER;w.officerAbilities.refresh(heir);
  var site=w.city(p.homeCity);var governor=w.officer(site.governorId);if(governor!=null&&governor.id!=heir.id&&governor.role==Strategy.Role.GOVERNOR)governor.role=Strategy.Role.OFFICER;
  site.governorId=heir.id;data.vacantSites.remove(site.id);
  if(p.heirArmy!=p.primaryArmy){
   PcArmyActionPolicy.clearArmy(w,p.heirArmy);
   for(var entry:data.assignments.entrySet())if(entry.getValue().army==p.heirArmy&&w.life.present(entry.getKey()))entry.getValue().army=p.primaryArmy;
   for(var entry:data.siteArmies.entrySet())if(entry.getValue()==p.heirArmy){entry.setValue(p.primaryArmy);var city=w.city(entry.getKey());if(city.governorId!=heir.id){var old=w.officer(city.governorId);if(old!=null&&old.role==Strategy.Role.GOVERNOR)old.role=Strategy.Role.OFFICER;city.governorId=-1;data.vacantSites.add(city.id);}}
   data.unitArmies.replaceAll((id,army)->army==p.heirArmy?p.primaryArmy:army);data.armyLeaders.put(p.heirArmy,-1);data.mergedArmies.put(p.heirArmy,p.primaryArmy);
  }
  data.armyLeaders.put(p.primaryArmy,p.heirNative);PcGovernorPolicy.write(w,data);
  for(var entry:p.loyalty.entrySet()){int id=entry.getKey(),raw=entry.getValue();PcDuelRawLoyalty.invalidate(w,id);w.officer(id).loyalty=Math.min(100,raw);PcDuelRawLoyalty.originalWrite(w,id,raw);}
  // Original4b78f0 calls4b21b0(unit,1) only when its new ruler is
  // not already the leader. A promoted deputy retains its unit/location;
  // existing leader joins the first free deputy slot after reselection.
  if(heir.unitId>=0){var unit=w.unit(heir.unitId);if(unit.officerId!=heir.id)PcDuelReplacement.reselect(w,unit);}
  // The native callback deliberately excludes the heir: raw99 stays99 here.
  w.note(heir.name+"继承"+w.faction(heir.owner)+"君主之位");
 }
 private PcRulerCoronation(){}
}
