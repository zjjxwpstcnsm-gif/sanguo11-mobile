package game.sanguo.core;
import java.io.*;
/** Original human4b2380 uses collector+4 force ruler for4afd60, distinct from active fighter
 * and deployed unit head. Old absent/opaque namespaces retain their strategy. */
final class PcDuelHumanActorPolicy {
 static final String NAMESPACE="pc-duel-human-force-ruler-v1";
 static final String RESOLVER_SHA="46c5346993898415a7f7d667e792292ae7aa457774f4630c872a22ad1a66475c";
 static final String CALLER_RECEIPT_SHA="9c2db04c806c15b672785ed015b54b4207c7522890b27f2ba6275fd2fd8d3385";
 static final String SAME_CITY_RECRUIT_RECEIPT_SHA="3411fb5aea75ceea7bf5a5cea820e15008e00db60e224bd161c6f5d6077fc8bb";
 private static final int MAGIC=0x50444831;
 static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
 static void validate(World w)throws IOException{
  if(!enabled(w))return;byte[]b=w.extensions.get(NAMESPACE);var src=PcScenarioIdentity.saved(w);
  if(b.length>2048||src==null||!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3)throw new IOException("原人工处分君主绑定来源或旧策略不同");
  try(var d=new DataInputStream(new ByteArrayInputStream(b))){if(d.readInt()!=MAGIC||d.readInt()!=1||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!d.readUTF().equals(src.scenarioId)||!d.readUTF().equals(src.sha)||!d.readUTF().equals(src.sourceVariant)||d.readInt()!=0x4ad960||!d.readUTF().equals(RESOLVER_SHA)||!d.readUTF().equals(CALLER_RECEIPT_SHA)||!d.readUTF().equals(SAME_CITY_RECRUIT_RECEIPT_SHA)||d.available()!=0)throw new IOException("原人工处分君主绑定保存与原调用证据不同");}
 }
 static String adoptionError(World w){
  if(w.extensions.get(NAMESPACE)!=null)return enabled(w)?"当前保存已采用原人工处分势力君主绑定":"保存已有未知人工处分人物绑定，请保留原档";
  try{if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3)return "旧保存没有明确原单挑战役策略，不自动补填";}
  catch(IOException e){return e.getMessage();}return null;
 }
 static void adopt(World w)throws IOException{
  String e=adoptionError(w);if(e!=null)throw new IOException(e);var src=PcScenarioIdentity.saved(w);var b=new ByteArrayOutputStream();
  try(var d=new DataOutputStream(b)){d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(src.scenarioId);d.writeUTF(src.sha);d.writeUTF(src.sourceVariant);d.writeInt(0x4ad960);d.writeUTF(RESOLVER_SHA);d.writeUTF(CALLER_RECEIPT_SHA);d.writeUTF(SAME_CITY_RECRUIT_RECEIPT_SHA);}
  w.extensions.put(NAMESPACE,b.toByteArray());validate(w);
 }
 static void initializeOpening(World w)throws IOException{if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy())throw new IOException("原人工处分君主绑定仅在明确新局启用");adopt(w);}
 static World.Officer actor(World w,World.Unit winner)throws IOException{
  if(winner==null)throw new IOException("原人工处分胜方部队不存在");
  if(!enabled(w))return w.officer(winner.officerId);
  validate(w);return ruler(w,winner);
 }
 static World.Officer ruler(World w,World.Unit winner)throws IOException{
  if(winner==null)throw new IOException("原人工处分胜方部队不存在");var ruler=w.loyalty.ruler(winner.owner);var facts=PcDuelSourceFacts.saved(w);
  if(ruler==null||ruler.owner!=winner.owner||!w.life.present(ruler.id)||!facts.containsKey(ruler.id))throw new IOException("原人工处分当前势力君主身份未绑定");return ruler;
 }
 private PcDuelHumanActorPolicy(){}
}
