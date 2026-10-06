package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.zip.GZIPInputStream;

/** Fresh original same-city free recruitment, distinct from search persuasion.
 * Trust tracks original free raw loyalty, invalidated on any allegiance change.
 * Old absent/opaque policy is never adopted. Other branches retain engineering rules. */
final class PcDirectRecruitmentPolicy {
 static final String NAMESPACE="pc-direct-recruitment-v1",RAW_INPUT_SHA="5b67c64a732b0cb326e176c161aa3517a06442989f3701765b1d7b6864386d8a";static final int MAGIC=0x50445231;
 static final class Raw {final int id,nativeId,loyalty;final String record;Raw(int id,int n,int l,String r){this.id=id;nativeId=n;loyalty=l;record=r;}}
 static final class State {String source,sha,variant;final SortedMap<Integer,Raw> trusted=new TreeMap<>();}
 static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
 static boolean operative(World w){return enabled(w)&&PcArmyActionPolicy.enabled(w);}
 static State read(World w)throws IOException {
  byte[]b=w.extensions.get(NAMESPACE);if(b==null||b.length>128*1024)throw new IOException("Direct recruitment policy absent");DataInputStream d=new DataInputStream(new ByteArrayInputStream(b));var src=PcScenarioIdentity.saved(w);State s=new State();if(d.readInt()!=MAGIC||d.readInt()!=1||src==null)throw new IOException("Direct recruitment schema invalid");s.source=d.readUTF();s.sha=d.readUTF();s.variant=d.readUTF();int n=d.readInt();if(n<0||n>670)throw new IOException("Direct raw trust count invalid");Map<Integer,PcScenarioPeople.Person> people=new HashMap<>();for(var p:PcScenarioPeople.saved(w))if(p.officerId>=0)people.put(p.officerId,p);
  for(int i=0;i<n;i++){int id=d.readInt(),nativeId=d.readInt(),raw=d.readInt();String record=d.readUTF();var p=people.get(id);if(p==null||nativeId!=p.nativeId||nativeId<0||nativeId>=670||raw<0||raw>255||!record.equals(p.recordSha)||!(p.field(20)==4||p.field(20)==7)||s.trusted.put(id,new Raw(id,nativeId,raw,record))!=null)throw new IOException("Direct raw source trust reference invalid");}
  if(!s.source.equals(src.scenarioId)||!s.sha.equals(src.sha)||!s.variant.equals(src.sourceVariant)||d.available()!=0)throw new IOException("Direct raw trust source differs");return s;
 }
 static void write(World w,State s)throws IOException {var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(s.source);d.writeUTF(s.sha);d.writeUTF(s.variant);d.writeInt(s.trusted.size());for(var r:s.trusted.values()){d.writeInt(r.id);d.writeInt(r.nativeId);d.writeInt(r.loyalty);d.writeUTF(r.record);}w.extensions.put(NAMESPACE,b.toByteArray());}
 static void initializeOpening(World w)throws IOException {
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("Fresh direct source required");var src=PcScenarioIdentity.saved(w);State s=new State();s.source=src.scenarioId;s.sha=src.sha;s.variant=src.sourceVariant;Map<Integer,PcScenarioPeople.Person> people=new HashMap<>();for(var p:PcScenarioPeople.saved(w))if(p.officerId>=0)people.put(p.nativeId,p);
  InputStream resource=PcDirectRecruitmentPolicy.class.getResourceAsStream("/pc-scenarios/raw-loyalty.tsv.gz");if(resource==null)throw new IOException("Original raw source input absent");byte[] packed;try(InputStream in=resource){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Original raw source compressed input too large");out.write(buffer,0,n);}packed=out.toByteArray();}try{if(!RAW_INPUT_SHA.equals(PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(packed))))throw new IOException("Original raw source input changed");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}resource=new ByteArrayInputStream(packed);int rows=0;
  try(var zip=new GZIPInputStream(resource);var reader=new BufferedReader(new InputStreamReader(zip,StandardCharsets.UTF_8))){String row;int total=0;while((row=reader.readLine())!=null){total+=row.length();if(total>8*1024*1024)throw new IOException("Original raw source too large");if(row.startsWith("#")||row.isBlank())continue;String[]p=row.split("\t");if(p.length!=10)throw new IOException("Original raw source shape invalid");if(!p[0].equals(s.source))continue;if(!p[1].equals(s.sha))throw new IOException("Original raw source identity invalid");rows++;int nativeId=Integer.parseInt(p[2]),loyalty=Integer.parseInt(p[4]),status=Integer.parseInt(p[6]);var person=people.get(nativeId);if(person==null||nativeId>=670||!(status==4||status==7)||!p[8].equals("1"))continue;if(!person.recordSha.equals(p[3])||person.field(20)!=status||loyalty<0||loyalty>255)throw new IOException("Original raw/person join invalid");if(s.trusted.put(person.officerId,new Raw(person.officerId,nativeId,loyalty,p[3]))!=null)throw new IOException("Original raw duplicate");}}
  if(rows!=1100)throw new IOException("Original raw source domain incomplete");write(w,s);read(w);
 }
 static void allegianceChanged(World w,int officer){if(!enabled(w))return;try{State s=read(w);if(s.trusted.remove(officer)!=null)write(w,s);}catch(IOException e){throw new IllegalStateException(e);}}
 static Raw eligible(World w,World.City c,World.Officer actor,World.Officer target)throws IOException {
  if(!operative(w)||c==null||actor==null||target==null||target.owner>=0||target.role!=Strategy.Role.UNAFFILIATED||target.cityId!=c.id||target.unitId!=-1||!w.life.present(target.id)||w.government.merit(actor.id)>60000||w.government.captive(target.id)||w.domestic.busy(target.id)||w.strategy.busy(target.id))return null;
  var a=PcSearchPolicy.person(w,actor.id);Raw t=read(w).trusted.get(target.id);if(a==null||t==null||w.loyalty.ruler(actor.owner)==null||actor.charm<0||actor.charm>100||Loyalty.distance(actor,target)<0)return null;
  for(var kind:Relations.Kind.values())if(!w.relations.links(target.id,kind).isEmpty())return null;
  // Until raw/display mutation handling is proved, an edited nonzero original
  // free raw value is not allowed to masquerade as unchanged source raw data.
  if(t.loyalty!=0&&w.editor.edited())return null;return t;
 }
 static int probability(World w,World.Officer actor,World.Officer target)throws IOException {
  var a=PcSearchPolicy.person(w,actor.id);var t=PcSearchPolicy.person(w,target.id);World.Officer ruler=w.loyalty.ruler(actor.owner);var r=PcSearchPolicy.person(w,ruler.id);int gap=Loyalty.distance(target,ruler);if(a==null||t==null||r==null||gap<0)throw new IOException("Original direct affinity/identity absent");int adjust=PcCommandRoll.calculate(3,a.nativeId,t.nativeId,actor.charm,0,r.nativeId,0,0);
  return PcRecruitmentFormula.calculate(70,2,25,gap,Math.max(30,actor.charm),0,0,0,0,adjust);
 }
 static boolean decision(World w,World.Officer actor,World.Officer target,Raw raw,int probability)throws IOException {var a=PcSearchPolicy.person(w,actor.id);return PcDirectRecruitmentRules.succeeds(probability,PcSearchPolicy.date(w),raw.nativeId,a.nativeId,raw.loyalty,actor.charm,Loyalty.distance(actor,target));}
 static World.Result execute(World w,RecruitmentPlan plan)throws IOException {
  World.City c=w.city(plan.cityId);World.Officer a=w.officer(plan.officerId),t=w.officer(plan.targetId);if(!plan.nativeRules||!plan.allowed())throw new IOException("Native direct plan unavailable");int loyalty=plan.nativeSuccess?PcDebateCampaignRules.joinLoyalty(w,t,a.owner):0;
  w.debitActionPoints(c,20);a.acted=true;
  if(plan.nativeSuccess){w.strategy.releaseGovernor(t.id);w.government.allegianceChanged(t.id);t.owner=a.owner;t.role=Strategy.Role.OFFICER;t.cityId=c.id;t.unitId=-1;t.loyalty=loyalty;t.lastRewardTurn=-1;t.acted=true;PcGovernorPolicy.joined(w,t,a,c.id);w.campaign.setPoints(a.owner,PcTechniquePoints.after(w.campaign.points(a.owner),20+t.charm/3),TechniquePointsJournal.Cause.DIRECT_RECRUITMENT,c.id,a.id);}
  int merit=PcDirectRecruitmentRules.meritAward(plan.nativeSuccess);w.government.earn(a.id,Math.max(0,Math.min(merit,60000-w.government.merit(a.id))));w.officerAbilities.gainExperience(a.id,4,PcDirectRecruitmentRules.charmExperience(plan.nativeSuccess)*(PcDebateCampaignRules.guided(w,a)?2:1));return w.success(plan.nativeSuccess?t.name+"接受登用，加入"+w.faction(a.owner)+"；原当城登用金0、行动力20":a.name+"登用"+t.name+"未成功；原当城登用金0、行动力20、功绩10、魅力经验1");
 }
 static void validate(World w)throws IOException {if(enabled(w)){State s=read(w);for(var r:s.trusted.values()){World.Officer o=w.officer(r.id);if(o==null||o.owner>=0||o.unitId>=0)throw new IOException("Direct raw trust survived allegiance mutation");}}}
 private PcDirectRecruitmentPolicy(){}
}
