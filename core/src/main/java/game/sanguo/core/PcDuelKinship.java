package game.sanguo.core;
import java.io.*;import java.util.*;
/** Saved source48d3b0 classifier bytes. No restore-time catalog or ancestry
 * approximation. Complete normal campaign initializer remains separate. */
final class PcDuelKinship {
 static final String NAMESPACE="pc-duel-kinship-v1";static final int MAGIC=0x50444b31,SIZE=670*670;
 private static final String PACK_SHA="3aacd4a9c561a30fe7ecfe137ce141816827d02e399a340da744837806773295";
 private static final String DECODED_SHA="5d064bc867874b1b8a823dcfcdaa92c6c50f2158d9ede68ea97172376c3ef9bf";
 private static final class SourceMatrix {
  final String sha,variant,receipt;final byte[]matrix;
  SourceMatrix(String sha,String variant,String receipt,byte[]matrix){this.sha=sha;this.variant=variant;this.receipt=receipt;this.matrix=matrix;}
 }
 private static Map<String,SourceMatrix>catalog;
 private static String hash(byte[]bytes)throws IOException {
  try {byte[]digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format(Locale.ROOT,"%02x",b&255));return text.toString();}
  catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
 }
 private static byte[] bounded(InputStream in,int limit)throws IOException {
  if(in==null)throw new IOException("原亲族资源缺失");try(in;var out=new ByteArrayOutputStream()){byte[]buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("原亲族资源过大");out.write(buf,0,n);}return out.toByteArray();}
 }
 private static synchronized Map<String,SourceMatrix> catalog()throws IOException {
  if(catalog!=null)return catalog;
  byte[]packed=bounded(PcDuelKinship.class.getResourceAsStream("/pc-duel/kinship-16.bin.gz"),65536);
  if(!hash(packed).equals(PACK_SHA))throw new IOException("原亲族压缩资源SHA不同");
  byte[]raw=bounded(new java.util.zip.GZIPInputStream(new ByteArrayInputStream(packed)),8*1024*1024);
  if(!hash(raw).equals(DECODED_SHA))throw new IOException("原亲族解码资源SHA不同");
  var in=new DataInputStream(new ByteArrayInputStream(raw));byte[]header=new byte[11];in.readFully(header);
  if(!Arrays.equals(header,"PDK-PACK-1\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))throw new IOException("原亲族资源格式未知");
  Map<String,SourceMatrix>out=new LinkedHashMap<>();var sources=PcScenarioCatalog.all();if(sources.size()!=16)throw new IOException("原亲族来源数量不同");
  for(int index=0;index<16;index++){
   int metadataLength=in.readInt(),matrixLength=in.readInt();if(metadataLength<1||metadataLength>=32768||matrixLength!=SIZE)throw new IOException("原亲族条目长度无效");
   byte[]metadata=new byte[metadataLength];in.readFully(metadata);var m=MapJson.object(MapJson.parse(metadata));MapJson.keys(m,"sourceIndex","source","receiptSha","matrixSha");
   if(MapJson.integer(m.get("sourceIndex"),0,15)!=index)throw new IOException("原亲族来源顺序不同");
   var src=MapJson.object(m.get("source"));var expected=sources.get(index).identity;
   String id=MapJson.string(src.get("scenarioId"),80),sourceSha=MapJson.string(src.get("sourceSha256"),64),variant=MapJson.string(src.get("sourceVariant"),300);
   String receipt=MapJson.string(m.get("receiptSha"),64),matrixSha=MapJson.string(m.get("matrixSha"),64);
   if(!id.equals(expected.scenarioId)||!sourceSha.equals(expected.sha)||!variant.equals(expected.sourceVariant)||!receipt.matches("[0-9a-f]{64}")||!matrixSha.matches("[0-9a-f]{64}"))throw new IOException("原亲族条目来源未核实");
   byte[]matrix=new byte[SIZE];in.readFully(matrix);check(matrix);if(!hash(matrix).equals(matrixSha))throw new IOException("原亲族矩阵SHA不同");
   if(out.put(id,new SourceMatrix(sourceSha,variant,receipt,matrix))!=null)throw new IOException("原亲族来源重复");
  }
  if(in.available()!=0)throw new IOException("原亲族资源尾部未知");catalog=Collections.unmodifiableMap(out);return catalog;
 }
 static void initializeOpening(World w)throws IOException {
  var source=PcScenarioIdentity.saved(w);if(source==null)throw new IOException("原亲族新局来源缺失");var row=catalog().get(source.scenarioId);
  if(row==null||!row.sha.equals(source.sha)||!row.variant.equals(source.sourceVariant))throw new IOException("原亲族新局来源不同");initialize(w,row.matrix,row.receipt);
 }
 private static final class Entry {final byte[]matrix;final long revision,identityRevision;Entry(World w,byte[]matrix){this.matrix=matrix;revision=w.extensions.revision(NAMESPACE);identityRevision=w.extensions.revision(PcScenarioIdentity.NAMESPACE);}}
 private static final Map<World,Entry>cache=Collections.synchronizedMap(new WeakHashMap<>());
 static boolean enabled(World w){byte[]raw=w.extensions.get(NAMESPACE);return raw!=null&&raw.length>=4&&java.nio.ByteBuffer.wrap(raw).getInt()==MAGIC;}
 static void initialize(World w,byte[]matrix,String receiptSha)throws IOException {
  if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null||!receiptSha.matches("[0-9a-f]{64}"))throw new IOException("原亲族矩阵仅能在明确新局建立");check(matrix);var source=PcScenarioIdentity.saved(w);if(source==null||PcDuelRuntimeFacts.saved(w)==null)throw new IOException("原亲族来源缺失");var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeInt(1);out.writeUTF(PcScenarioIdentity.EXE_SHA);out.writeUTF(source.scenarioId);out.writeUTF(source.sha);out.writeUTF(source.sourceVariant);out.writeUTF(receiptSha);out.writeInt(SIZE);out.write(matrix);w.extensions.put(NAMESPACE,bytes.toByteArray());
 }
 private static void check(byte[]matrix)throws IOException {if(matrix==null||matrix.length!=SIZE)throw new IOException("原亲族矩阵长度不同");for(byte b:matrix)if(b!=0&&b!=1&&b!=2&&b!=3&&b!=-1)throw new IOException("原亲族分类未知");}
 private static byte[] read(World w)throws IOException {
  Entry old=cache.get(w);if(old!=null&&old.revision==w.extensions.revision(NAMESPACE)&&old.identityRevision==w.extensions.revision(PcScenarioIdentity.NAMESPACE)&&w.pcSourceFrame)return old.matrix;
  byte[]raw=w.extensions.get(NAMESPACE);if(!w.pcSourceFrame||!enabled(w)||raw.length>SIZE+2048)throw new IOException("当前存档没有原亲族策略");
  var source=PcScenarioIdentity.saved(w);var in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=MAGIC||in.readInt()!=1||!in.readUTF().equals(PcScenarioIdentity.EXE_SHA)||source==null||!in.readUTF().equals(source.scenarioId)||!in.readUTF().equals(source.sha)||!in.readUTF().equals(source.sourceVariant)||!in.readUTF().matches("[0-9a-f]{64}")||in.readInt()!=SIZE)throw new IOException("原亲族矩阵保存来源不同");byte[]matrix=new byte[SIZE];in.readFully(matrix);if(in.available()!=0)throw new IOException("原亲族保存尾部未知");check(matrix);cache.put(w,new Entry(w,matrix));return matrix;
 }
 static int classifier(World w,int ownNative,int targetNative)throws IOException {if(ownNative<0||ownNative>=670||targetNative<0||targetNative>=670)throw new IOException("原亲族当前人物未覆盖");return read(w)[ownNative*670+targetNative];}
 static void unchangedParents(World w)throws IOException {
  var runtime=PcDuelRuntimeFacts.saved(w);var facts=PcDuelSourceFacts.saved(w);if(runtime==null)throw new IOException("原亲族当前父母来源缺失");Map<Integer,Integer>stable=new HashMap<>();for(var f:facts.values())stable.put(f.nativeId,f.officerId);
  // The original classifier traverses the whole internal graph. Public edits
  // of any mapped parent require explicit original internal-graph mutation.
  // Unmapped external native parents stay -1 publicly, never guessed IDs.
  for(var f:facts.values()){var p=runtime.people.get(f.nativeId);if(p==null)throw new IOException("原亲族人物来源缺失");if(w.relations.parent(f.officerId,false)!=stable.getOrDefault(p.father,-1)||w.relations.parent(f.officerId,true)!=stable.getOrDefault(p.mother,-1))throw new IOException("当前父母关系变更的原亲族内部图尚未核实");}
 }
 static boolean nonHarm(World w,int own,int target)throws IOException {
  unchangedParents(w);return nonHarm(w,own,target,PcDuelSourceFacts.saved(w));
 }
 private static boolean nonHarm(World w,int own,int target,Map<Integer,PcDuelSourceFacts.Fact>facts)throws IOException {
  var a=facts.get(own);var b=facts.get(target);if(a==null||b==null)throw new IOException("原单挑人物稳定连接缺失");if(classifier(w,a.nativeId,b.nativeId)>=0)return true;int spouse=w.relations.spouse(own);if(spouse<0)return false;var s=facts.get(spouse);if(s==null)throw new IOException("原单挑当前配偶亲族来源未覆盖");return classifier(w,s.nativeId,b.nativeId)>=0;
 }
 static boolean[][] terminal(World w,int[]ids)throws IOException {if(ids.length!=6)throw new IOException("原单挑两侧编队缺失");boolean[][]out=new boolean[6][6];boolean present=false;for(int id:ids)if(id>=0)present=true;if(!present)return out;unchangedParents(w);var facts=PcDuelSourceFacts.saved(w);for(int i=0;i<6;i++)for(int j=0;j<6;j++)if(ids[i]>=0&&ids[j]>=0)out[i][j]=nonHarm(w,ids[i],ids[j],facts);return out;}
 static void validate(World w)throws IOException {if(enabled(w))read(w);}
 private PcDuelKinship(){}
}
