package game.sanguo.core;
import java.io.*;import java.nio.ByteBuffer;import java.util.*;import java.security.*;import java.util.zip.GZIPInputStream;
/** Draft new-game-only policy. Original internal reference is NOT a biological parent. */
final class PcCommandCapacityPolicy {
 static final String NAMESPACE="pc-command-capacity-v1";static final int MAGIC=0x50434331;
 static final String RESOURCE_SHA="a3bf182f957f9b95200aed9e50d72355174a9c96bc3d6b14a24b7040ad50e8e8";
 static final String SHARED_SHA="dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f";
 static final class Ref{int nativeId,id,value;String sha;}
 static final class SourceRefs{String id,path,variant,sha;Map<Integer,Ref> rows=new TreeMap<>();}
 static Map<String,SourceRefs> references;
 static synchronized Map<String,SourceRefs> references()throws IOException{
  if(references!=null)return references;
  InputStream resource=PcCommandCapacityPolicy.class.getResourceAsStream("/pc-command-capacity/references.bin.gz");if(resource==null)throw new IOException("Original capacity references absent");byte[] raw;
  try(InputStream in=new GZIPInputStream(resource)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(b.size()+n>2*1024*1024)throw new IOException("Capacity evidence too large");b.write(block,0,n);}raw=b.toByteArray();}
  try{if(!hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(RESOURCE_SHA))throw new IOException("Capacity evidence SHA differs");}catch(NoSuchAlgorithmException e){throw new IOException(e);}
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=0x504e5031||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA))throw new IOException("Capacity evidence executable differs");PcOfficerInfo.text(in,64);if(in.readInt()!=16)throw new IOException("Capacity source coverage differs");Map<String,SourceRefs> all=new TreeMap<>();
  for(int s=0;s<16;s++){SourceRefs r=new SourceRefs();r.id=PcOfficerInfo.text(in,80);r.path=PcOfficerInfo.text(in,1024);r.variant=PcOfficerInfo.text(in,300);r.sha=PcOfficerInfo.text(in,64);if(in.readInt()!=1100)throw new IOException("Original constructed domain differs");
   for(int i=0;i<1100;i++){Ref f=new Ref();f.nativeId=in.readInt();f.id=in.readInt();f.sha=PcOfficerInfo.text(in,64);f.value=in.readInt();int valid=in.readInt();if(f.nativeId!=i||valid<0||valid>1||f.value< -1||f.value>=1100)throw new IOException("Original reference row differs");r.rows.put(i,f);}if(all.put(r.id,r)!=null)throw new IOException("Capacity source duplicate");
  }if(in.available()!=0)throw new IOException("Capacity evidence trailing bytes");return references=Collections.unmodifiableMap(all);
 }
 static final class Data{PcScenarioIdentity.Source source;Set<Integer> country5=new TreeSet<>(),same403=new TreeSet<>(),mapped=new TreeSet<>();long own,identity,people,forces;}
 static final Map<World,Data> caches=Collections.synchronizedMap(new WeakHashMap<>());
 static boolean recognized(World w){if(!w.pcSourceFrame)return false;byte[] raw=w.extensions.get(NAMESPACE);return raw!=null&&raw.length>=4&&ByteBuffer.wrap(raw).getInt()==MAGIC;}
 static SourceRefs sourceRefs(PcScenarioIdentity.Source s)throws IOException{if(s==null)throw new IOException("Capacity source absent");SourceRefs r=references().get(s.scenarioId);if(r==null||!r.path.equals(s.path)||!r.variant.equals(s.sourceVariant)||!r.sha.equals(s.sha)||!SHARED_SHA.equals(s.sharedSha))throw new IOException("Capacity source identity differs");return r;}
 static void initializeOpening(World w)throws IOException{
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("Capacity strategy requires explicit fresh PC-source game");PcScenarioIdentity.Source s=PcScenarioIdentity.saved(w);SourceRefs refs=sourceRefs(s);Data d=new Data();d.source=s;
  for(var e:PcScenarioPeople.savedForces(w).entrySet())if(Objects.equals(e.getValue().get(6),5))d.country5.add(e.getKey());
  Ref root=refs.rows.get(403);
  for(var p:PcScenarioPeople.saved(w))if(p.officerId>=0){Ref r=refs.rows.get(p.nativeId);if(r==null||r.id!=p.officerId||!r.sha.equals(p.recordSha)||w.officer(p.officerId)==null)throw new IOException("Capacity record/runtime join differs");d.mapped.add(p.officerId);if(r.value==root.value)d.same403.add(p.officerId);}
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(MAGIC);out.writeInt(1);for(String v:new String[]{s.scenarioId,s.sha,s.sourceVariant,s.sharedSha,PcScenarioIdentity.EXE_SHA,RESOURCE_SHA})PcOfficerInfo.text(out,v);
  writeSet(out,d.country5);writeSet(out,d.mapped);writeSet(out,d.same403);w.extensions.put(NAMESPACE,b.toByteArray());validate(w);
 }
 static void writeSet(DataOutputStream out,Set<Integer> values)throws IOException{out.writeInt(values.size());for(int n:values)out.writeInt(n);}
 static Set<Integer> readSet(DataInputStream in,int min,int max,int ceiling)throws IOException{int n=PcOfficerInfo.bounded(in.readInt(),min,max);Set<Integer> r=new TreeSet<>();int last=-1;for(int i=0;i<n;i++){int value=PcOfficerInfo.bounded(in.readInt(),0,ceiling);if(value<=last)throw new IOException("Capacity identity ordering differs");r.add(value);last=value;}return r;}
 static Data read(World w)throws IOException{
  byte[] raw=w.extensions.get(NAMESPACE);if(raw.length>8192)throw new IOException("Saved capacity policy too large");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source s=PcScenarioIdentity.saved(w);SourceRefs refs=sourceRefs(s);Data d=new Data();d.source=s;
  if(in.readInt()!=MAGIC||in.readInt()!=1)throw new IOException("Capacity policy version differs");for(String v:new String[]{s.scenarioId,s.sha,s.sourceVariant,s.sharedSha,PcScenarioIdentity.EXE_SHA,RESOURCE_SHA})if(!PcOfficerInfo.text(in,300).equals(v))throw new IOException("Saved capacity policy identity differs");
  d.country5=readSet(in,0,47,46);d.mapped=readSet(in,0,850,999999);d.same403=readSet(in,0,850,999999);if(in.available()!=0||!d.mapped.containsAll(d.same403))throw new IOException("Saved capacity policy tail/identity differs");
  Set<Integer> countries=new TreeSet<>(),mapped=new TreeSet<>(),same=new TreeSet<>();for(var e:PcScenarioPeople.savedForces(w).entrySet())if(Objects.equals(e.getValue().get(6),5))countries.add(e.getKey());Ref root=refs.rows.get(403);
  for(var p:PcScenarioPeople.saved(w))if(p.officerId>=0){Ref r=refs.rows.get(p.nativeId);if(r==null||r.id!=p.officerId||!r.sha.equals(p.recordSha)||w.officer(p.officerId)==null)throw new IOException("Saved capacity participant differs");mapped.add(p.officerId);if(r.value==root.value)same.add(p.officerId);}
  if(!countries.equals(d.country5)||!mapped.equals(d.mapped)||!same.equals(d.same403))throw new IOException("Saved capacity branch evidence differs");
  d.own=w.extensions.revision(NAMESPACE);d.identity=w.extensions.revision(PcScenarioIdentity.NAMESPACE);d.people=w.extensions.revision(PcScenarioPeople.NAMESPACE);d.forces=w.extensions.revision("pc-source-opening-record-v1");return d;
 }
 static Integer base(World w,World.Officer o){
  if(!w.pcSourceFrame||w.extensions.revision(NAMESPACE)==0)return null;
  Data d=caches.get(w);try{if(d==null||d.own!=w.extensions.revision(NAMESPACE)||d.identity!=w.extensions.revision(PcScenarioIdentity.NAMESPACE)||d.people!=w.extensions.revision(PcScenarioPeople.NAMESPACE)||d.forces!=w.extensions.revision("pc-source-opening-record-v1")){if(!recognized(w)){caches.remove(w);return null;}d=read(w);caches.put(w,d);}
   if(!PcScenarioIdentity.DATA_SOURCE.equals(w.dataSource)||!d.source.scenarioId.equals(w.scenarioId)||!d.source.sha.equals(w.dataHash)||d.source.year!=w.startYear||d.source.month!=w.startMonth)throw new IOException("Live capacity source differs");
   if(o.owner<0)return 0;if(!d.country5.contains(o.owner))return null;
   if(o.role==Strategy.Role.RULER)return RulerTitles.Title.EMPEROR.troops;
   if(!d.mapped.contains(o.id))return null;return d.same403.contains(o.id)?RulerTitles.Title.EMPEROR.troops:PcOfficerRanks.all().get(20).command;
  }catch(IOException e){throw new IllegalStateException(e);}
 }
 static void validate(World w)throws IOException{if(recognized(w))caches.put(w,read(w));}
 static String hex(byte[] bytes){char[] digits="0123456789abcdef".toCharArray(),out=new char[bytes.length*2];for(int i=0;i<bytes.length;i++){int value=bytes[i]&255;out[i*2]=digits[value>>>4];out[i*2+1]=digits[value&15];}return new String(out);}
 private PcCommandCapacityPolicy(){}
}
