package game.sanguo.core;
import java.io.*;
/** Explicit new campaign settlement policy. Prototype39 never silently adopts it.
 * Current admission/start fee remains the declared engineering rule until original full caller closure. */
final class PcDebateCampaignPolicy {
 static final String NAMESPACE="pc-debate-campaign-settlement-v1";static final int MAGIC=0x50444331;
 static final class State {int version=1,lastFinished=-1,adoptedSession=-1;String source,sha,variant;}
 static boolean enabled(World w){byte[] b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
 static State read(World w)throws IOException {
  byte[] raw=w.extensions.get(NAMESPACE);if(raw==null||raw.length>2048)throw new IOException("Native debate campaign policy absent");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));var src=PcScenarioIdentity.saved(w);State s=new State();
  if(in.readInt()!=MAGIC||src==null)throw new IOException("Native debate campaign policy invalid");s.version=in.readInt();if(s.version<1||s.version>2)throw new IOException("Native debate campaign policy version unknown");s.source=in.readUTF();s.sha=in.readUTF();s.variant=in.readUTF();s.lastFinished=in.readInt();s.adoptedSession=in.readInt();
  if(!s.source.equals(src.scenarioId)||!s.sha.equals(src.sha)||!s.variant.equals(src.sourceVariant)||s.lastFinished< -1||s.lastFinished>=w.contests.nextId||s.adoptedSession< -1||s.adoptedSession>=w.contests.nextId||in.available()!=0||!PcNativeDebatePolicy.enabled(w)||!PcNativeHealthPolicy.enabled(w))throw new IOException("Native debate campaign source/receipt differs");return s;
 }
 static void write(World w,State s)throws IOException {if(s.version<1||s.version>2)throw new IOException("Native debate campaign write version unknown");ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(s.version);d.writeUTF(s.source);d.writeUTF(s.sha);d.writeUTF(s.variant);d.writeInt(s.lastFinished);d.writeInt(s.adoptedSession);w.extensions.put(NAMESPACE,b.toByteArray());}
 static boolean sameHan(World w,int first,int second)throws IOException {return enabled(w)&&read(w).version>=2?first==second:first==1&&second==1;}
 static void initializeOpening(World w)throws IOException {
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("Fresh explicit native debate campaign required");
  if(!PcNativeDebatePolicy.enabled(w))PcNativeDebatePolicy.initializeOpening(w);initialize(w);State s=read(w);s.version=2;write(w,s);
 }
 static void initialize(World w)throws IOException {
  var src=PcScenarioIdentity.saved(w);
  if(src==null||!w.pcSourceFrame||!PcNativeDebatePolicy.enabled(w)||w.extensions.get(NAMESPACE)!=null||PcContestProfiles.saved(w).isEmpty())throw new IOException("Saved native prototype adoption invalid");
  PcNativeDebatePolicy.seed(w);PcNativeHealthPolicy.validate(w);
  State s=new State();s.source=src.scenarioId;s.sha=src.sha;s.variant=src.sourceVariant;s.adoptedSession=w.contests.current()==null?-1:w.contests.current().id();
  PcNativeHealthPolicy.adoptNewInjuryRecovery(w);write(w,s);
 }
 static void recordFinished(World w,int id)throws IOException {State s=read(w);if(id<=s.lastFinished)throw new IOException("Native campaign result already applied");s.lastFinished=id;write(w,s);}
 static void validate(World w)throws IOException {if(enabled(w))read(w);}
 private PcDebateCampaignPolicy(){}
}
