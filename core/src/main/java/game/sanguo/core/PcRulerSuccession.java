package game.sanguo.core;
import java.io.*;import java.util.*;
/** Read-only current original candidate binding. Supported AI ruler-death
 * writes are owned by PcRulerCoronation/PcDuelExecution. */
final class PcRulerSuccession {
 static boolean ordinary(int n){return n>=0&&n<1100;}
 static boolean father(int n){return ordinary(n)||n>=2000&&n<=2060;}
 static final class Plan {
  final int ruler,owner,selected;final List<Integer> candidates;
  final Map<Integer,PcRulerSuccessionRules.Person> facts;
  final boolean fatherValid,rootValid,motherValid;
  Plan(int ruler,int owner,List<Integer>candidates,Map<Integer,PcRulerSuccessionRules.Person> facts,boolean f,boolean r,boolean m){
   this.ruler=ruler;this.owner=owner;this.candidates=List.copyOf(candidates);this.facts=Collections.unmodifiableMap(new LinkedHashMap<>(facts));fatherValid=f;rootValid=r;motherValid=m;
   int best=-1;for(int id:candidates)if(best<0||PcRulerSuccessionRules.better(facts.get(id),facts.get(best),f,r,m))best=id;selected=best;
  }
  private Plan(Plan current,int heir)throws IOException {
   if(!current.candidates.contains(heir))throw new IOException("继承人已不在当前原势力候选中");
   ruler=current.ruler;owner=current.owner;selected=heir;candidates=current.candidates;facts=current.facts;fatherValid=current.fatherValid;rootValid=current.rootValid;motherValid=current.motherValid;
  }
  Plan choose(int heir)throws IOException {return new Plan(this,heir);}
 }
 static Plan preview(World w,int ruler)throws IOException {
  if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||PcGovernorPolicy.data(w).format!=3)throw new IOException("旧保存没有明确原继承策略");
  var dead=w.officer(ruler);var sources=PcDuelSourceFacts.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);var ds=sources.get(ruler);var dp=ds==null||runtime==null?null:runtime.people.get(ds.nativeId);
  if(dead==null||!w.life.present(ruler)||dead.role!=Strategy.Role.RULER||dead.owner<0||dp==null)throw new IOException("原继承当前君主身份未核实");
  // Original4b7f7f has a native370/367 scenario-event exception governed by
  // root+40/+44 and living male children. Those current event flags are not
  // saved here; never substitute the generic comparator for that branch.
  if(ds.nativeId==370)throw new IOException("原特殊继承剧情开关尚未绑定");
  PcDuelKinship.unchangedParents(w);PcDuelRecruitmentAdmission.unchangedGroup(w,ruler,runtime,sources);
  boolean fv=father(dp.father),rv=ordinary(dp.root),mv=ordinary(dp.mother);var root=rv?runtime.people.get(dp.root):null;
  if(rv&&root==null)throw new IOException("原继承内部祖系引用的人物尚未覆盖");
  List<Integer>ids=new ArrayList<>();Map<Integer,PcRulerSuccessionRules.Person>facts=new LinkedHashMap<>();
  for(var o:w.officers){
   if(o.id==ruler||o.owner!=dead.owner||!w.life.present(o.id)||w.government.captive(o.id)||o.role==Strategy.Role.UNAFFILIATED)continue;
   var source=sources.get(o.id);var p=source==null?null:runtime.people.get(source.nativeId);if(p==null)throw new IOException("原继承当前候选的自定义/额外身份尚未覆盖");
   PcDuelRecruitmentAdmission.unchangedGroup(w,o.id,runtime,sources);int age=w.life.age(o.id);if(age<0)throw new IOException("原继承当前年龄未知");
   boolean[]flags={p.father==ds.nativeId||p.mother==ds.nativeId,p.father==dp.father,fv&&dp.father==source.nativeId,rv&&ordinary(p.root)&&source.nativeId!=dp.root&&root.root==p.root,mv&&dp.mother==source.nativeId,p.mother==dp.mother,ordinary(PcDuelSwornPolicy.current(w,dp.nativeId))&&PcDuelSwornPolicy.current(w,p.nativeId)==PcDuelSwornPolicy.current(w,dp.nativeId),w.relations.spouse(ruler)==o.id,o.sex==World.Sex.MALE};
   ids.add(o.id);facts.put(o.id,new PcRulerSuccessionRules.Person(source.nativeId,age,w.government.merit(o.id),PcRulerSuccessionRules.preference(source.nativeId),flags));
  }
  // Original roster insertion follows native registry order; project IDs are
  // independent identity joins, so ties may never use project ID arithmetic.
  ids.sort(Comparator.comparingInt(id->facts.get(id).nativeId));return new Plan(ruler,dead.owner,ids,facts,fv,rv,mv);
 }
 private PcRulerSuccession(){}
}
