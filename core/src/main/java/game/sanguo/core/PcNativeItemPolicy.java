package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original item identity bridge, explicitly initialized for fresh sources.
 * Current ownership lives in existing saved Treasures; initial holders never
 * refill after reward, confiscation, capture or save loading. */
final class PcNativeItemPolicy {
    static final String NAMESPACE="pc-native-item-identities-v1";
    private static final int MAGIC=0x504e4931;
    private static final String RESOURCE_SHA="bd4cfadb7f1024a9403bfb4b5cbee13fb152f941040ffb9948d67c43a712e74b";
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null||!w.treasures.items.isEmpty())throw new IOException("原宝物连接仅能在明确空宝物新局建立");
        var source=PcScenarioIdentity.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);if(source==null||runtime==null)throw new IOException("原宝物来源缺失");byte[]raw;
        try(var in=PcNativeItemPolicy.class.getResourceAsStream("/pc-duel/native-items.tsv");var b=new ByteArrayOutputStream()){if(in==null)throw new IOException("原宝物连接资源缺失");byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(b.size()+n>512*1024)throw new IOException("原宝物连接资源过大");b.write(buffer,0,n);}raw=b.toByteArray();}
        try{if(!RESOURCE_SHA.equals(PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw))))throw new IOException("原宝物连接指纹不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        Map<Integer,Treasures.Definition> definitions=new TreeMap<>();for(String line:new String(raw,StandardCharsets.UTF_8).split("\n")){if(line.startsWith("#"))continue;String[]p=line.split("\t",-1);if(p.length!=11)throw new IOException("原宝物连接列数不同");if(!p[1].equals(source.scenarioId))continue;if(!p[2].equals(source.sha))throw new IOException("原宝物连接来源不同");int nativeId=Integer.parseInt(p[3]);Treasures.Definition d=Treasures.definition(p[4]);var original=runtime.items.get(nativeId);if(original==null||!original.valid||!d.name.equals(p[6])||original.kind!=Integer.parseInt(p[7])||d.kind.ordinal()!=original.kind||d.value!=Integer.parseInt(p[8])||definitions.put(nativeId,d)!=null)throw new IOException("原宝物定义身份不同");}
        if(definitions.size()!=43)throw new IOException("原宝物定义连接不完整");Map<Integer,Integer>people=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())people.put(f.nativeId,f.officerId);
        var b=new ByteArrayOutputStream();var out=new DataOutputStream(b);out.writeInt(MAGIC);out.writeInt(1);out.writeUTF(source.scenarioId);out.writeUTF(source.sha);out.writeUTF(source.sourceVariant);out.writeInt(definitions.size());
        for(var e:definitions.entrySet()){out.writeInt(e.getKey());out.writeUTF(e.getValue().id);out.writeUTF(e.getValue().name);out.writeInt(e.getValue().kind.ordinal());out.writeInt(e.getValue().value);int owner=runtime.items.get(e.getKey()).initialOwner;if(owner>=0){Integer id=people.get(owner);if(id==null||w.officer(id)==null)throw new IOException("原持宝人物未连接");if(w.life.present(id))w.treasures.place(e.getValue(),Treasures.Place.OFFICER,id);}}
        w.extensions.put(NAMESPACE,b.toByteArray());validate(w);
    }
    static Map<String,Integer> identities(World w)throws IOException {
        if(!enabled(w))return Collections.emptyMap();byte[]raw=w.extensions.get(NAMESPACE);if(raw.length>8192)throw new IOException("原宝物连接保存过大");var d=new DataInputStream(new ByteArrayInputStream(raw));var source=PcScenarioIdentity.saved(w);if(d.readInt()!=MAGIC||d.readInt()!=1||source==null||!d.readUTF().equals(source.scenarioId)||!d.readUTF().equals(source.sha)||!d.readUTF().equals(source.sourceVariant)||d.readInt()!=43)throw new IOException("原宝物连接保存来源不同");Map<String,Integer>out=new TreeMap<>();for(int i=0;i<43;i++){int nativeId=d.readInt();String id=d.readUTF(),name=d.readUTF();int kind=d.readInt(),value=d.readInt();if(nativeId!=i||!id.equals(String.format(Locale.ROOT,"item-%03d",i))||name.isEmpty()||name.length()>80||kind<0||kind>7||value<0||value>100||out.put(id,nativeId)!=null)throw new IOException("原宝物保存身份无效");var current=w.treasures.item(id);if(current!=null&&(!current.definition.name.equals(name)||current.definition.kind.ordinal()!=kind||current.definition.value!=value))throw new IOException("当前宝物定义与已存原定义不同");}if(d.available()!=0)throw new IOException("原宝物保存尾部未知");return out;
    }
    static int[][] held(World w,int officer)throws IOException {var ids=identities(w);List<int[]>out=new ArrayList<>();for(var item:w.treasures.held(officer)){Integer id=ids.get(item.definition.id);if(id==null)throw new IOException("当前携物原身份未知");out.add(new int[]{id,item.definition.kind.ordinal()});}return out.toArray(new int[0][]);}
    static void validate(World w)throws IOException {if(enabled(w)){var ids=identities(w);var facts=PcDuelRuntimeFacts.saved(w);if(facts==null)throw new IOException("原宝物保存缺失绑定来源");for(var item:w.treasures.items()){Integer id=ids.get(item.definition.id);var source=id==null?null:facts.items.get(id);if(source==null||!source.valid||source.kind!=item.definition.kind.ordinal())throw new IOException("当前宝物原类型不同");}}}
    private PcNativeItemPolicy(){}
}
