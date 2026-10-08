package game.sanguo.core;
import java.io.*;import java.util.*;
/** Explicit fresh-source saved original-army budget. Old saves never acquire it on load.
 * Native opening-event generation and missing facility40/42 construction remain separate. */
public final class PcArmyActionPolicy {
 static final String NAMESPACE="pc-army-action-budget-v1";static final int MAGIC=0x50415031;
 static final class State {String source,sha,variant;int lastTurn;final int[] points=new int[47];}
 static final class Cached {long revision,identityRevision,governorRevision;int year,month;State state;Cached(World w,State s){revision=w.extensions.revision(NAMESPACE);identityRevision=w.extensions.revision(PcScenarioIdentity.NAMESPACE);governorRevision=w.extensions.revision(PcGovernorPolicy.NAMESPACE);year=w.startYear;month=w.startMonth;state=s;}}
 static final Map<World,Cached> cache=Collections.synchronizedMap(new WeakHashMap<>());
 public static boolean enabled(World w){byte[] raw=w.extensions.get(NAMESPACE);return raw!=null&&raw.length>=4&&java.nio.ByteBuffer.wrap(raw).getInt()==MAGIC;}
 static State read(World w)throws IOException {
  // Source validation also checks live identity fields, which can change
  // without an extension revision. Never accept a stale cached identity.
  Cached c=cache.get(w);if(c!=null&&c.revision==w.extensions.revision(NAMESPACE)&&c.identityRevision==w.extensions.revision(PcScenarioIdentity.NAMESPACE)&&c.governorRevision==w.extensions.revision(PcGovernorPolicy.NAMESPACE)&&c.state.lastTurn<=w.turn){
   if(!w.pcSourceFrame||!PcScenarioIdentity.DATA_SOURCE.equals(w.dataSource)||!c.state.source.equals(w.scenarioId)||!c.state.sha.equals(w.dataHash)||w.startYear!=c.year||w.startMonth!=c.month)throw new IOException("Original army budget live identity differs");return c.state;
  }
  byte[] raw=w.extensions.get(NAMESPACE);if(raw==null||raw.length>1024)throw new IOException("Original army budget absent/large");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));var source=PcScenarioIdentity.saved(w);
  if(in.readInt()!=MAGIC||in.readInt()!=1||source==null||!PcGovernorPolicy.recognized(w))throw new IOException("Original army budget context invalid");State s=new State();s.source=in.readUTF();s.sha=in.readUTF();s.variant=in.readUTF();s.lastTurn=in.readInt();
  if(!s.source.equals(source.scenarioId)||!s.sha.equals(source.sha)||!s.variant.equals(source.sourceVariant)||s.lastTurn<0||s.lastTurn>w.turn)throw new IOException("Original army budget source/turn differs");
  var src=PcGovernorPolicy.source(w);
  for(int i=0;i<47;i++){s.points[i]=in.readInt();if(s.points[i]< -1||s.points[i]>255||(!src.armyOriginalValid.get(i)||PcGovernorPolicy.data(w).mergedArmies.containsKey(i))&&s.points[i]!=0)throw new IOException("Original army budget range/validity differs");}if(in.available()!=0)throw new IOException("Original army budget unknown tail");cache.put(w,new Cached(w,s));return s;
 }
 static void write(World w,State s)throws IOException {ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(s.source);d.writeUTF(s.sha);d.writeUTF(s.variant);d.writeInt(s.lastTurn);for(int v:s.points)d.writeInt(v);w.extensions.put(NAMESPACE,bytes.toByteArray());cache.put(w,new Cached(w,s));}
 static State copy(State prior){State s=new State();s.source=prior.source;s.sha=prior.sha;s.variant=prior.variant;s.lastTurn=prior.lastTurn;System.arraycopy(prior.points,0,s.points,0,47);return s;}
 static int calculate(World w,int army,int current)throws IOException {
  var data=PcGovernorPolicy.data(w);var src=data.source;
  if(!src.armyOriginalValid.get(army)||data.mergedArmies.containsKey(army))return 0;int owner=src.armyOwners.get(army);if(owner<0||owner>=42)return -1;
  var person=src.people.get(data.armyLeaders.get(army));World.Officer leader=person==null?null:w.officer(person.id);
  if(leader==null||leader.owner!=owner||!w.life.present(leader.id)||w.government.captive(leader.id))return 0;
  int cities=0,officers=0;
  for(var site:w.cities)if(site.owner==owner&&Objects.equals(data.siteArmies.get(site.id),army)){
   if(site.kind==World.SiteKind.CITY)cities++;int nativeSite=src.sites.get(site.id).nativeId;
   for(var o:w.officers){var a=data.assignments.get(o.id);if(a!=null&&a.home==nativeSite&&w.life.present(o.id)&&o.owner==owner&&o.role!=Strategy.Role.UNAFFILIATED&&!w.government.captive(o.id))officers++;}
   for(var p:src.people.values())if(p.id<0&&p.home==nativeSite&&p.allowed&&p.mask)return -1;
  }
  World.Officer adviser=w.officer(w.government.advisors.getOrDefault(owner,-1));if(adviser!=null&&(adviser.owner!=owner||!w.life.present(adviser.id)||w.government.captive(adviser.id)))adviser=null;
  // Original tally-platform40 is not a supported current facility kind; no
  // source or saved tally platform is fabricated to award the modifier.
  return PcArmyActionRules.after(Math.max(0,current),true,leader.leadership,leader.charm,cities,officers,adviser!=null,adviser==null?0:adviser.intelligence,0);
 }
 static void initializeOpening(World w)throws IOException {
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.extensions.get(NAMESPACE)!=null||!PcGovernorPolicy.recognized(w))throw new IOException("Fresh explicit original army budget required");
  var source=PcScenarioIdentity.saved(w);State s=new State();s.source=source.scenarioId;s.sha=source.sha;s.variant=source.sourceVariant;s.lastTurn=0;for(int i=0;i<47;i++)s.points[i]=calculate(w,i,0);write(w,s);mirror(w);
 }
 public static int points(World w,int army){if(!enabled(w)||army<0||army>=47)return -1;try{return read(w).points[army];}catch(IOException e){throw new IllegalStateException(e);}}
 public static int cityArmy(World w,int city){if(!enabled(w))return -1;try{return PcGovernorPolicy.data(w).siteArmies.getOrDefault(city,-1);}catch(IOException e){throw new IllegalStateException(e);}}
 public static int cityPoints(World w,int city){return points(w,cityArmy(w,city));}
 static int primaryArmy(World w,int owner)throws IOException {
  var source=PcGovernorPolicy.source(w);int found=-1;
  for(int i=0;i<47;i++)if(source.armyOriginalValid.get(i)&&source.armyOwners.get(i)==owner&&source.armyDisplays.get(i)==1){if(found>=0)throw new IOException("Original first army ambiguous");found=i;}
  return found;
 }
 static RuleFailure primaryFailure(World w,int owner,int amount){
  if(!enabled(w))return null;try{int army=primaryArmy(w,owner);if(army<0)return new RuleFailure("ORIGINAL_ARMY_UNKNOWN","army","第一军团分配未知");int budget=points(w,army);return budget<amount?new RuleFailure("ACTION_POINTS","army","第一军团行动力"+budget+"，需要"+amount):null;}catch(IOException e){throw new IllegalStateException(e);}
 }
 static void debitPrimary(World w,int owner,int amount){
  RuleFailure failure=primaryFailure(w,owner,amount);if(failure!=null)throw new IllegalStateException(failure.detail);try{State s=copy(read(w));s.points[primaryArmy(w,owner)]-=amount;write(w,s);mirror(w);}catch(IOException e){throw new IllegalStateException(e);}
 }
 static void editPrimary(World w,int owner,int value){
  if(value<0||value>255)throw new IllegalArgumentException("原第一军团行动力范围0至255");try{int army=primaryArmy(w,owner);if(army<0)throw new IllegalArgumentException("第一军团未知，不能编辑其他军团代替");State s=copy(read(w));s.points[army]=value;write(w,s);mirror(w);}catch(IOException e){throw new IllegalArgumentException(e);}
 }
 static void clearArmy(World w,int army){if(!enabled(w))return;try{State s=copy(read(w));s.points[army]=0;write(w,s);mirror(w);}catch(IOException e){throw new IllegalStateException(e);}}
 static void clearOwner(World w,int owner){
  if(!enabled(w))return;try{State s=copy(read(w));var source=PcGovernorPolicy.source(w);for(int i=0;i<47;i++)if(source.armyOwners.get(i)==owner)s.points[i]=0;write(w,s);mirror(w);}catch(IOException e){throw new IllegalStateException(e);}
 }
 static RuleFailure failure(World w,World.City city,int cost){return failure(w,city,cost,false);}
 static RuleFailure failure(World w,World.City city,int cost,boolean directRequired){
  if(!enabled(w))return null;if(city==null)return new RuleFailure("CITY_UNAVAILABLE","city","请选择己方据点");int army=cityArmy(w,city.id);try{var src=PcGovernorPolicy.source(w);
   if(army<0||!src.armyOriginalValid.get(army)||PcGovernorPolicy.data(w).mergedArmies.containsKey(army)||src.armyOwners.get(army)!=city.owner)return new RuleFailure("ORIGINAL_ARMY_UNKNOWN","city","原军团分配未知，不能借用其他军团行动力");
   if(directRequired&&w.active==w.player&&src.armyDisplays.get(army)!=1)return new RuleFailure("ORIGINAL_ARMY_DELEGATED","city","该据点属于原委任军团，直属命令须由第一军团执行");
   int available=points(w,army);if(available<0)return new RuleFailure("ORIGINAL_ARMY_INPUT_UNKNOWN","city","原军团有效人物或设施输入未闭合");
   return available<cost?new RuleFailure("ACTION_POINTS","army","原军团行动力"+available+"，需要"+cost):null;
  }catch(IOException e){throw new IllegalStateException(e);}
 }
 static void debit(World w,World.City city,int amount){if(!enabled(w))throw new IllegalStateException("Original budget inactive");RuleFailure f=failure(w,city,amount);if(f!=null)throw new IllegalStateException(f.detail);try{State s=copy(read(w));s.points[cityArmy(w,city.id)]-=amount;write(w,s);mirror(w);}catch(IOException e){throw new IllegalStateException(e);}}
 static void replenish(World w){if(!enabled(w))return;try{State old=read(w);if(old.lastTurn==w.turn)return;State s=copy(old);for(int i=0;i<47;i++)s.points[i]=calculate(w,i,s.points[i]);s.lastTurn=w.turn;write(w,s);mirror(w);}catch(IOException e){throw new IllegalStateException(e);}}
 static int[] projection(World w,State s)throws IOException {var src=PcGovernorPolicy.source(w);int[] result=new int[w.actionPoints.length];for(int i=0;i<47;i++)if(src.armyOriginalValid.get(i)&&src.armyDisplays.get(i)==1){int owner=src.armyOwners.get(i),points=s.points[i];if(owner>=0&&owner<result.length&&points>=0)result[owner]+=points;}return result;}
 static void mirror(World w){if(!enabled(w))return;try{int[] projected=projection(w,read(w));System.arraycopy(projected,0,w.actionPoints,0,projected.length);}catch(IOException e){throw new IllegalStateException(e);}}
 static void validate(World w)throws IOException{if(enabled(w)){cache.remove(w);State s=read(w);if(!Arrays.equals(w.actionPoints,projection(w,s)))throw new IOException("Original army budget projection differs");}}
 private PcArmyActionPolicy(){}
}
