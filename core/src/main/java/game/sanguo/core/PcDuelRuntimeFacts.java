package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Saved native reference/item snapshot. Native owners and references remain
 * native IDs, including external parent IDs; no project item/name arithmetic. */
final class PcDuelRuntimeFacts {
    static final String NAMESPACE="pc-duel-runtime-bindings-v1";
    private static final int MAGIC=0x50444231;
    static final class Person {
        final int nativeId,root,father,mother,spouse,sworn,birthplace,rawLoyalty,treasureBonus;
        Person(int[]v){nativeId=v[0];root=v[1];father=v[2];mother=v[3];spouse=v[4];sworn=v[5];birthplace=v[6];rawLoyalty=v[7];treasureBonus=v[8];}
        int[]values(){return new int[]{nativeId,root,father,mother,spouse,sworn,birthplace,rawLoyalty,treasureBonus};}
    }
    static final class Item {
        final int nativeId,kind,initialOwner;final boolean valid;
        Item(int id,boolean valid,int kind,int owner){nativeId=id;this.valid=valid;this.kind=kind;initialOwner=owner;}
    }
    static final class State {
        String id,sha,variant;final SortedMap<Integer,Person>people=new TreeMap<>();final SortedMap<Integer,Item>items=new TreeMap<>();
        int[][]held(int nativeId){return items.values().stream().filter(i->i.valid&&i.initialOwner==nativeId).map(i->new int[]{i.nativeId,i.kind}).toArray(int[][]::new);}
    }
    private static Map<String,State>catalog;
    private static String hash(byte[]raw)throws IOException {try{return PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw));}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}}
    private static synchronized Map<String,State>catalog()throws IOException {
        if(catalog!=null)return catalog;byte[]raw;try(var resource=PcDuelRuntimeFacts.class.getResourceAsStream("/pc-duel/runtime-bindings.tsv.gz")){if(resource==null)throw new IOException("原单挑绑定资源缺失");try(var in=new GZIPInputStream(resource);var b=new ByteArrayOutputStream()){byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(b.size()+n>4*1024*1024)throw new IOException("原单挑绑定资源过大");b.write(buffer,0,n);}raw=b.toByteArray();}}
        String expected;try(var in=PcDuelRuntimeFacts.class.getResourceAsStream("/pc-duel/runtime-bindings-sha.txt")){if(in==null)throw new IOException("原单挑绑定指纹缺失");var b=new ByteArrayOutputStream();int v;while((v=in.read())!=-1){if(b.size()>=128)throw new IOException("原绑定索引过大");b.write(v);}expected=new String(b.toByteArray(),StandardCharsets.US_ASCII).trim();}if(!expected.matches("[0-9a-f]{64}")||!hash(raw).equals(expected))throw new IOException("原单挑绑定资源指纹不同");
        Map<String,State>out=new TreeMap<>();int rows=0;for(String line:new String(raw,StandardCharsets.UTF_8).split("\n")){if(line.startsWith("#"))continue;String[]p=line.split("\t",-1);if(p.length!=14&&p.length!=11)throw new IOException("原单挑绑定资源列数不同");State s=out.get(p[1]);if(s==null){s=new State();s.id=p[1];s.sha=p[2];s.variant=p[3];out.put(s.id,s);}if(!s.sha.equals(p[2])||!s.variant.equals(p[3]))throw new IOException("原单挑绑定来源不同");try{if(p[4].equals("P")){if(p.length!=14)throw new IOException("原人物绑定列数错误");int[]v=new int[9];for(int i=0;i<9;i++)v[i]=Integer.parseInt(p[i+5]);if(s.people.put(v[0],new Person(v))!=null)throw new IOException("原单挑人物重复");}else if(p[4].equals("I")){if(p.length!=11)throw new IOException("原物品绑定列数错误");int id=Integer.parseInt(p[5]),valid=Integer.parseInt(p[6]),kind=Integer.parseInt(p[7]),owner=Integer.parseInt(p[8]);if(valid<0||valid>1||!p[9].matches("[0-9a-f]{64}")||!p[10].matches("[0-9a-f]{168}")||s.items.put(id,new Item(id,valid==1,kind,owner))!=null)throw new IOException("原单挑物品记录无效");}else throw new IOException("原单挑绑定类型未知");}catch(NumberFormatException e){throw new IOException(e);}rows++;}
        if(out.size()!=16||rows!=12320)throw new IOException("原单挑绑定覆盖不足");for(var s:out.values())validate(s);return catalog=Collections.unmodifiableMap(out);
    }
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原单挑绑定只能在明确新局建立");var source=PcScenarioIdentity.saved(w);var s=catalog().get(source.scenarioId);if(s==null||!s.sha.equals(source.sha)||!s.variant.equals(source.sourceVariant))throw new IOException("原单挑绑定来源未核实");write(w,s);
    }
    static State saved(World w)throws IOException {
        byte[]raw=w.extensions.get(NAMESPACE);if(raw==null||raw.length<4||java.nio.ByteBuffer.wrap(raw).getInt()!=MAGIC)return null;if(raw.length>65536)throw new IOException("原单挑绑定保存过大");var d=new DataInputStream(new ByteArrayInputStream(raw));d.readInt();if(d.readInt()!=1)throw new IOException("原单挑绑定保存策略未知");State s=new State();s.id=d.readUTF();s.sha=d.readUTF();s.variant=d.readUTF();var source=PcScenarioIdentity.saved(w);if(source==null||!s.id.equals(source.scenarioId)||!s.sha.equals(source.sha)||!s.variant.equals(source.sourceVariant))throw new IOException("原单挑绑定保存来源不同");if(d.readInt()!=670)throw new IOException("原单挑绑定人数不同");for(int i=0;i<670;i++){int[]v=new int[9];for(int j=0;j<9;j++)v[j]=d.readInt();if(s.people.put(v[0],new Person(v))!=null)throw new IOException("原单挑绑定人物重复");}if(d.readInt()!=100)throw new IOException("原单挑绑定物品数不同");for(int i=0;i<100;i++){int id=d.readInt();boolean valid=d.readBoolean();if(s.items.put(id,new Item(id,valid,d.readInt(),d.readInt()))!=null)throw new IOException("原单挑绑定物品重复");}if(d.available()!=0)throw new IOException("原单挑绑定保存尾部未知");validate(s);return s;
    }
    private static void validate(State s)throws IOException {
        if(s.people.size()!=670||s.items.size()!=100)throw new IOException("原单挑绑定覆盖不同");for(int n=0;n<670;n++){Person p=s.people.get(n);if(p==null||p.root< -1||p.root>=1100||p.father< -1||p.father>2060||p.mother< -1||p.mother>=1100||p.spouse< -1||p.spouse>=1100||p.sworn< -1||p.sworn>=1100||p.birthplace< -1||p.birthplace>11||p.rawLoyalty<0||p.rawLoyalty>255||p.treasureBonus<0||p.treasureBonus>30)throw new IOException("原单挑绑定人物范围未知");}for(int n=0;n<100;n++){Item i=s.items.get(n);if(i==null||i.kind< -1||i.kind>7||i.initialOwner< -1||i.initialOwner>999999)throw new IOException("原单挑绑定物品范围未知");}}
    private static void write(World w,State s)throws IOException {validate(s);var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(s.id);d.writeUTF(s.sha);d.writeUTF(s.variant);d.writeInt(s.people.size());for(var p:s.people.values())for(int v:p.values())d.writeInt(v);d.writeInt(s.items.size());for(var i:s.items.values()){d.writeInt(i.nativeId);d.writeBoolean(i.valid);d.writeInt(i.kind);d.writeInt(i.initialOwner);}w.extensions.put(NAMESPACE,b.toByteArray());}
    static void validate(World w)throws IOException {saved(w);}
    private PcDuelRuntimeFacts(){}
}
