package game.sanguo.core;
import java.io.*;import java.util.*;
/** Explicit fresh native opening only. Original source Root18 is immutable
 * provenance; this closes the verified duel-life override, not all startup. */
public final class PcSourceOpeningOptions {
 static final String NAMESPACE="pc-source-opening-options-v1";
 private static final int MAGIC=0x504f5031;
 private static final String SHA="cc30cedaaf49a34f01aa62c10c14a64d3e8a6eb558cd1547da11c8ef92fd54b1";
 private static Map<String,String[]> cached;
 private static synchronized String[] source(PcScenarioIdentity.Source source)throws IOException {
  if(cached==null){byte[]raw;try(var in=PcSourceOpeningOptions.class.getResourceAsStream("/pc-duel/source-header-flags.tsv")){if(in==null)throw new IOException("原剧本标志资源缺失");raw=PcResourceBytes.readUpTo(in,16385);if(raw.length>16384)throw new IOException("原剧本标志资源过大");}
   try{if(!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals(SHA))throw new IOException("原剧本标志SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
   String[]lines=new String(raw,java.nio.charset.StandardCharsets.UTF_8).split("\n");if(lines.length!=18||!lines[0].equals("# original header flags 692391c85748e5da8d9ac1ae390ecf8e649ca2ba320ca4042048f929af57d3a0")||!lines[1].equals("# executable "+PcScenarioIdentity.EXE_SHA))throw new IOException("原剧本标志来源不同");Map<String,String[]> rows=new TreeMap<>();
   for(int k=2;k<lines.length;k++){String[]p=lines[k].split("\t",-1);if(p.length!=6||!p[4].matches("[0-9a-f]{64}")||!p[5].matches("[01]")||rows.put(p[0],p)!=null)throw new IOException("原剧本标志列或覆盖不同");}cached=rows;
  }
  String[]p=source==null?null:cached.get(source.scenarioId);if(p==null||!p[1].equals(source.path)||!p[2].equals(source.sha)||!p[3].equals(source.sharedSha))throw new IOException("原剧本标志当前身份不同");return p;
 }
 public static final class Facts {
  public final int flag18;public final PcDuelOptions requested,effective;
  private Facts(int flag,PcDuelOptions requested){flag18=flag;this.requested=requested;effective=new PcDuelOptions(flag!=0?3:requested.life,requested.death,requested.difficulty);}
 }
 /** Catalog identity and pinned header only; no source World is created. */
 public static int sourceFlag(String scenarioId)throws IOException {
  if(scenarioId==null)throw new IOException("请选择原来源剧本");
  for(var entry:PcScenarioCatalog.all())if(entry.identity.scenarioId.equals(scenarioId))return Integer.parseInt(source(entry.identity)[5]);
  throw new IOException("原来源剧本不存在");
 }
 static PcDuelOptions initialize(World w,PcDuelOptions requested)throws IOException {
  if(requested==null||!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null||PcDuelCampaignPolicy.enabled(w))throw new IOException("原来源寿命覆盖只能明确创建新局");var id=PcScenarioIdentity.saved(w);String[]src=source(id);Facts f=new Facts(Integer.parseInt(src[5]),requested);var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);for(String s:new String[]{id.scenarioId,id.sha,id.sourceVariant,id.sharedSha,PcScenarioIdentity.EXE_SHA,SHA,src[4]})d.writeUTF(s);d.writeInt(f.flag18);d.writeInt(requested.life);d.writeInt(requested.death);d.writeInt(requested.difficulty);w.extensions.put(NAMESPACE,b.toByteArray());return f.effective;
 }
 public static Facts saved(World w)throws IOException {
  byte[]raw=w.extensions.get(NAMESPACE);if(!w.pcSourceFrame||raw==null||raw.length<4||java.nio.ByteBuffer.wrap(raw).getInt()!=MAGIC)return null;if(raw.length>2048)throw new IOException("原来源寿命覆盖保存过大");var id=PcScenarioIdentity.saved(w);String[]src=source(id);var d=new DataInputStream(new ByteArrayInputStream(raw));if(d.readInt()!=MAGIC||d.readInt()!=1)throw new IOException("原来源寿命覆盖版本不同");for(String expected:new String[]{id.scenarioId,id.sha,id.sourceVariant,id.sharedSha,PcScenarioIdentity.EXE_SHA,SHA,src[4]})if(!d.readUTF().equals(expected))throw new IOException("原来源寿命覆盖保存来源不同");int flag=d.readInt();PcDuelOptions requested;try{requested=new PcDuelOptions(d.readInt(),d.readInt(),d.readInt());}catch(IllegalArgumentException e){throw new IOException(e);}if(flag!=Integer.parseInt(src[5])||d.available()!=0)throw new IOException("原来源寿命覆盖标志不同");var f=new Facts(flag,requested);var current=PcDuelOptions.current(w);if(current==null||PcDuelCampaignPolicy.read(w).version!=3||current.life!=f.effective.life||current.death!=requested.death||current.difficulty!=requested.difficulty)throw new IOException("原来源寿命覆盖与本局规则不同");return f;
 }
 static void validate(World w)throws IOException {saved(w);}
 private PcSourceOpeningOptions(){}
}
