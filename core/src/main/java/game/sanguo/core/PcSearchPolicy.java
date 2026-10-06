package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;

/** Explicit fresh source search strategy. Original discovered-person branch;
 * treasure/no-discovery fallback remains the declared engineering branch.
 * Unknown relationship gates retain normal play via the declared legacy fallback.
 * Never adopts or looks up new content while decoding an existing save. */
final class PcSearchPolicy {
 static final String NAMESPACE="pc-search-discovery-choice-v1",REGION_SHA="7c99b9605ba37e29da637de1f099bf37175ee73ce3fe640c88f112b3ff98a52c";
 static final int MAGIC=0x50534332;
 static final class State {String source,sha,variant;final SortedMap<Integer,Integer> regions=new TreeMap<>();int session=-1,phase;}
 static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
 static boolean operative(World w){return enabled(w)&&PcArmyActionPolicy.enabled(w)&&PcDebateCampaignPolicy.enabled(w);}
 private static byte[] regionBytes(InputStream in)throws IOException {ByteArrayOutputStream b=new ByteArrayOutputStream();byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(b.size()+n>256*1024)throw new IOException("Search region input too large");b.write(buffer,0,n);}return b.toByteArray();}
 static State read(World w)throws IOException {
  byte[]b=w.extensions.get(NAMESPACE);if(b==null||b.length>8192)throw new IOException("Search policy absent");DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));State s=new State();var src=PcScenarioIdentity.saved(w);
  if(d.readInt()!=MAGIC||d.readInt()!=1||src==null)throw new IOException("Search policy schema invalid");s.source=d.readUTF();s.sha=d.readUTF();s.variant=d.readUTF();s.session=d.readInt();s.phase=d.readInt();int n=d.readInt();if(n!=42)throw new IOException("Search city coverage invalid");
  for(int i=0;i<n;i++){int id=d.readInt(),r=d.readInt();if(w.city(id)==null||w.city(id).kind!=World.SiteKind.CITY||r<0||r>11||s.regions.put(id,r)!=null)throw new IOException("Search city reference invalid");}
  if(!s.source.equals(src.scenarioId)||!s.sha.equals(src.sha)||!s.variant.equals(src.sourceVariant)||d.available()!=0||s.session< -1||s.phase<0||s.phase>2||(s.session==-1)!=(s.phase==0)||s.session>=w.contests.nextId||s.phase>0&&!operative(w))throw new IOException("Search policy identity/sequence invalid");return s;
 }
 static void write(World w,State s)throws IOException {var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(s.source);d.writeUTF(s.sha);d.writeUTF(s.variant);d.writeInt(s.session);d.writeInt(s.phase);d.writeInt(s.regions.size());for(var e:s.regions.entrySet()){d.writeInt(e.getKey());d.writeInt(e.getValue());}w.extensions.put(NAMESPACE,b.toByteArray());}
 static void initializeOpening(World w)throws IOException {
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("Fresh search source required");var src=PcScenarioIdentity.saved(w);State s=new State();s.source=src.scenarioId;s.sha=src.sha;s.variant=src.sourceVariant;
  try(var in=PcSearchPolicy.class.getResourceAsStream("/pc-scenarios/search-regions.tsv")){if(in==null)throw new IOException("Search regions absent");byte[]raw=regionBytes(in);try{byte[]digest=MessageDigest.getInstance("SHA-256").digest(raw);StringBuilder h=new StringBuilder();for(byte v:digest)h.append(String.format(Locale.ROOT,"%02x",v&255));if(!REGION_SHA.equals(h.toString()))throw new IOException("Search region input changed");}catch(NoSuchAlgorithmException e){throw new IOException(e);}for(String row:new String(raw,StandardCharsets.UTF_8).split("\n")){if(row.startsWith("#")||row.isBlank())continue;String[]p=row.split("\t");if(p.length!=5)throw new IOException("Search region row invalid");if(p[0].equals(s.source)){if(!p[1].equals(s.sha)||s.regions.put(Integer.parseInt(p[3]),Integer.parseInt(p[4]))!=null)throw new IOException("Search region source invalid");}}}
  write(w,s);read(w);
 }
 static int date(World w){return 3*w.life.year()+5*w.life.month()+7*(1+10*(w.turn%3));}
 static List<World.Officer> candidates(World w,int city)throws IOException {
  Map<Integer,PcScenarioPeople.Person> source=new HashMap<>();for(var p:PcScenarioPeople.saved(w))if(p.officerId>=0)source.put(p.officerId,p);
  List<World.Officer> list=new ArrayList<>();for(var o:w.life.undiscovered(city)){var p=source.get(o.id);if(p!=null&&p.nativeId<670&&!w.strategy.busy(o.id)&&!w.domestic.busy(o.id)&&!w.government.captive(o.id))list.add(o);}
  list.sort(Comparator.comparingInt(o->source.get(o.id).nativeId));return list;
 }
 static PcScenarioPeople.Person person(World w,int id)throws IOException {for(var p:PcScenarioPeople.saved(w))if(p.officerId==id)return p;return null;}
 static boolean handlesActor(World w,World.Officer actor){if(!operative(w)||actor==null||w.active!=w.player)return false;try{return person(w,actor.id)!=null;}catch(IOException e){throw new IllegalStateException(e);}}
 static int chance(World w,int city,int actor)throws IOException {var o=w.officer(actor);var p=person(w,actor);Integer r=read(w).regions.get(city);if(o==null||p==null||r==null)return o==null?0:w.strategy.searchChance(actor);return PcSearchRules.chance(candidates(w,city).size(),o.politics,w.skills.has(o,Skill.YANLI),p.field(41)==r);}
 static final class Selection {final World.Officer officer;final int seed;Selection(World.Officer o,int seed){officer=o;this.seed=seed;}}
 /** Pure lookup and local RNG clone; both preview and submission use this selection. */
 static Selection selection(World w,World.City city,World.Officer actor)throws IOException {
  State s=read(w);if(!handlesActor(w,actor)||s.session>=0||city.kind!=World.SiteKind.CITY||w.contests.nextId>=10000000)return null;
  List<World.Officer> list=candidates(w,city.id);if(list.isEmpty())return null;var a=PcDebateCampaignRules.source(w,actor.id);int firstGap=Loyalty.distance(actor,list.get(0));if(firstGap<0||w.life.age(actor.id)<0||!PcSearchRules.discovers(chance(w,city.id,actor.id),date(w),a.nativeId,firstGap))return null;
  var random=new PcMerchantRules.Random(PcNativeDebatePolicy.seed(w));World.Officer selected=list.get(0);int best=Integer.MIN_VALUE;boolean tie=false;
  for(var t:list){int gap=Loyalty.distance(actor,t);if(gap<0)return null;int score=list.size()==1?0:PcSearchRules.score(gap,w.life.age(actor.id),random);if(score>best){best=score;selected=t;tie=false;}else if(score==best)tie=true;}
  // Selected-target relationship special gates have not all been reconstructed.
  // Keep the established engineering result when the fallback is not proved.
  if(tie||!fallbackProven(w,actor,selected))return null;return new Selection(selected,random.state);
 }
 static boolean pendingPreview(World w,World.City c,World.Officer a){try{return enabled(w)&&selection(w,c,a)!=null;}catch(IOException e){throw new IllegalStateException(e);}}
 /** Returns null to retain explicit engineering fallback, without consuming RNG. */
 static Strategy.SearchResult found(World w,World.City city,World.Officer actor)throws IOException {
  Selection selection=selection(w,city,actor);if(selection==null)return null;State s=read(w);World.Officer selected=selection.officer;
  PcNativeDebatePolicy.setSeed(w,selection.seed);w.life.discover(selected.id);selected.acted=false;
  var c=w.contests;c.session=new Contests.Session(c.nextId++,w.active,w.turn,actor.id,selected.id,city.id);c.session.searchChoice=true;s.session=c.session.id;s.phase=1;write(w,s);
  return new Strategy.SearchResult(w.success(actor.name+"在"+city.name+"发现了"+selected.name+"，请选择是否招揽；选择和后续舌战均可保存续行"),Strategy.SearchOutcome.OFFICER,selected.id,0);
 }
 static boolean fallbackProven(World w,World.Officer actor,World.Officer target)throws IOException {
  if(target.owner>=0||target.role!=Strategy.Role.UNAFFILIATED||w.relations.spouse(target.id)>=0||!w.relations.links(target.id,Relations.Kind.SWORN).isEmpty()||w.relations.parent(target.id,false)>=0||w.relations.parent(target.id,true)>=0)return false;
  if(!w.relations.links(target.id,Relations.Kind.LIKE).isEmpty()||!w.relations.links(target.id,Relations.Kind.DISLIKE).isEmpty())return false;
  return PcDebateCampaignRules.source(w,target.id).field(20)==7;
 }
 static int probability(World w,World.Officer actor,World.Officer target)throws IOException {
  var a=PcDebateCampaignRules.source(w,actor.id);var t=PcDebateCampaignRules.source(w,target.id);World.Officer ruler=w.loyalty.ruler(actor.owner);if(ruler==null||!fallbackProven(w,actor,target))throw new IOException("Native free recruitment special gate not proved");var r=PcDebateCampaignRules.source(w,ruler.id);int gap=Loyalty.distance(target,ruler);if(gap<0)throw new IOException("Native affinity absent");
  // Native initialized7201978 nonzero context: free target uses70/honor2.
  // Full PC menu difficulty/settings remain unknown; this is saved strategy choice.
  int adjust=PcCommandRoll.calculate(3,a.nativeId,t.nativeId,actor.charm,0,r.nativeId,0,0);
  return PcRecruitmentFormula.calculate(70,2,25,gap,Math.max(30,actor.charm),0,0,0,0,adjust);
 }
 static void end(World w)throws IOException {State s=read(w);s.session=-1;s.phase=0;write(w,s);w.contests.session=null;}
 static boolean owns(World w,Contests.Session session){if(!enabled(w)||session==null)return false;try{return read(w).session==session.id;}catch(IOException e){throw new IllegalStateException(e);}}
 static World.Result choose(World w,boolean yes)throws IOException {
  State s=read(w);Contests.Session session=w.contests.session;if(session==null||!session.searchChoice||session.id!=s.session)throw new IOException("Search choice absent");World.Officer actor=w.officer(session.leftRef),target=w.officer(session.rightRef);World.City city=w.city(session.city);
  validate(w);int chance=0,roll=0,threshold=0;
  if(s.phase==1&&yes){chance=probability(w,actor,target);var a=PcDebateCampaignRules.source(w,actor.id);var t=PcDebateCampaignRules.source(w,target.id);roll=PcCommandRoll.calculate(100,date(w),a.nativeId,t.nativeId,Loyalty.distance(actor,target),0,0,0);threshold=chance+actor.charm-Loyalty.distance(actor,w.loyalty.ruler(actor.owner));if(roll<chance)PcDebateCampaignRules.joinLoyalty(w,target,actor.owner);}
  if(s.phase==1){w.campaign.setPoints(actor.owner,PcTechniquePoints.after(w.campaign.points(actor.owner),20),TechniquePointsJournal.Cause.DEBATE,city.id,actor.id);if(!yes){w.debitActionPoints(city,20);actor.acted=true;w.government.earn(actor.id,3);end(w);return w.success(target.name+"已成为在野武将；未招揽，行动力−20，功绩+3");}
   if(roll<chance||threshold<=80){boolean success=roll<chance;PcDebateCampaignRules.recruitmentResult(w,actor,target,city.id,success);w.debitActionPoints(city,20);end(w);return w.success(success?target.name+"接受招揽，加入"+w.faction(actor.owner):target.name+"拒绝招揽");}
   s.phase=2;session.revision++;write(w,s);return w.success(target.name+"仍不接受招揽，可以通过舌战继续说服，也可以放弃");
  }
  if(s.phase!=2)throw new IOException("Search choice phase invalid");
  if(!yes){PcDebateCampaignRules.recruitmentResult(w,actor,target,city.id,false);w.debitActionPoints(city,20);end(w);return w.success("放弃舌战，招揽未成功；原搜索回调已结算");}
  String error=PcDebateCampaign.inputError(w,actor.id,target.id);if(error!=null)throw new IOException(error);session.searchChoice=false;session.revision=0;actor.acted=true;target.acted=true;session.nativeDebate=new PcDebateCampaign(w,actor.id,target.id);write(w,s);return w.success("搜索招揽进入舌战；无额外金费，结束时结算搜索行动力20");
 }
 static void finish(World w,Contests.Session session)throws IOException {if(owns(w,session)){w.debitActionPoints(w.city(session.city),20);State s=read(w);s.session=-1;s.phase=0;write(w,s);}}
 static void validate(World w)throws IOException {if(!enabled(w))return;State s=read(w);if(s.session<0)return;var c=w.contests.session;if(c==null||c.id!=s.session||c.isDuel()||c.diplomatic()||c.searchChoice!=(c.nativeDebate==null))throw new IOException("Search continuation mixing invalid");World.Officer a=w.officer(c.leftRef),t=w.officer(c.rightRef);World.City city=w.city(c.city);if(a==null||t==null||city==null||a.owner!=c.owner||city.owner!=c.owner||a.cityId!=city.id||t.cityId!=city.id||t.owner>=0||a.unitId!=-1||t.unitId!=-1||!w.life.present(t.id)||w.cityActionPoints(city)<20||!fallbackProven(w,a,t))throw new IOException("Search continuation participants invalid");if(c.searchChoice&&(c.revision!=s.phase-1||a.acted||t.acted))throw new IOException("Search choice state invalid");}
 private PcSearchPolicy(){}
}
