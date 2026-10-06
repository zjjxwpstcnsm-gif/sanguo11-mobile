package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Immutable per-source original records. Live abilities/assignments remain in World.
 * Extra/NPC/unmapped identities are explicit; no slot arithmetic becomes an officer ID. */
public final class PcScenarioPeople {
    private static final java.util.regex.Pattern FORMAT_0=java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final java.util.regex.Pattern FORMAT_1=java.util.regex.Pattern.compile("(?:[0-9a-f]{2})*");
    public static final String NAMESPACE="pc-scenario-people-v1";
    private static final int MAGIC=0x50535031;
    private PcScenarioPeople(){}
    public static final class Person {
        public final int nativeId,officerId;
        public final boolean strictIdentity;
        public final String originalName,nameRaw,recordSha,courtesy,courtesyRaw;
        public final Map<Integer,Integer> fields;
        public final List<String> unknown;
        Person(int nativeId,int id,boolean strict,String name,String raw,String sha,String courtesy,String courtesyRaw,Map<Integer,Integer> fields,List<String> unknown){
            this.nativeId=nativeId;officerId=id;strictIdentity=strict;originalName=name;nameRaw=raw;recordSha=sha;
            this.courtesy=courtesy;this.courtesyRaw=courtesyRaw;
            this.fields=Collections.unmodifiableMap(new TreeMap<>(fields));this.unknown=List.copyOf(unknown);
        }
        public int field(int key)throws IOException{Integer n=fields.get(key);if(n==null)throw new IOException("原人物字段缺失："+nativeId+"/"+key);return n;}
    }
    private static final class ParsedPeople {
        final byte[] raw;final List<Person> people;
        ParsedPeople(byte[] raw,List<Person> people){this.raw=raw;this.people=people;}
    }
    private static final class PeopleKey {
        final byte[] digest;
        PeopleKey(byte[] raw)throws IOException{
            try{digest=java.security.MessageDigest.getInstance("SHA-256").digest(raw);}
            catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        }
        @Override public int hashCode(){return Arrays.hashCode(digest);}
        @Override public boolean equals(Object other){return other instanceof PeopleKey&&Arrays.equals(digest,((PeopleKey)other).digest);}
    }
    private static final class PeopleCache {
        final long revision;final ParsedPeople parsed;
        PeopleCache(long revision,ParsedPeople parsed){this.revision=revision;this.parsed=parsed;}
    }
    // Neither cache value references its weak World key. Identical immutable source
    // blobs may be reused across committed/save-decoded copies; current officer facts
    // remain in each World. Exact raw equality protects the content-addressed lookup.
    private static final Map<World,PeopleCache> PEOPLE_CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<PeopleKey,java.lang.ref.WeakReference<ParsedPeople>> PARSED_PEOPLE=
        new LinkedHashMap<PeopleKey,java.lang.ref.WeakReference<ParsedPeople>>(16,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<PeopleKey,java.lang.ref.WeakReference<ParsedPeople>> entry){return size()>16;}
        };
    public static List<Person> saved(World w)throws IOException{
        if(!w.pcSourceFrame)return Collections.emptyList();
        long revision=w.extensions.revision(NAMESPACE);PeopleCache cached=PEOPLE_CACHE.get(w);
        if(cached!=null&&cached.revision==revision)return cached.parsed.people;
        byte[] raw=w.extensions.get(NAMESPACE);
        if(raw==null){PEOPLE_CACHE.remove(w);return Collections.emptyList();}
        PeopleKey key=new PeopleKey(raw);ParsedPeople parsed;
        synchronized(PARSED_PEOPLE){
            java.lang.ref.WeakReference<ParsedPeople> reference=PARSED_PEOPLE.get(key);parsed=reference==null?null:reference.get();
            if(parsed==null||!Arrays.equals(parsed.raw,raw)){
                parsed=new ParsedPeople(raw,readSaved(raw));PARSED_PEOPLE.put(key,new java.lang.ref.WeakReference<>(parsed));
            }
        }
        PEOPLE_CACHE.put(w,new PeopleCache(revision,parsed));return parsed.people;
    }
    private static List<Person> readSaved(byte[] raw)throws IOException{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=MAGIC)throw new IOException("\u539f\u4eba\u7269\u72b6\u6001\u5feb\u7167\u7248\u672c\u672a\u77e5");
        int n=PcOfficerInfo.bounded(in.readInt(),0,1100);List<Person> result=new ArrayList<>();Set<Integer> nativeIds=new HashSet<>(),ids=new HashSet<>();
        for(int i=0;i<n;i++){Person p=read(in);if(!nativeIds.add(p.nativeId)||p.officerId>=0&&!ids.add(p.officerId))throw new IOException("\u539f\u4eba\u7269\u8eab\u4efd\u91cd\u590d");result.add(p);}
        if(in.available()!=0)throw new IOException("\u539f\u4eba\u7269\u72b6\u6001\u5c3e\u90e8\u672a\u77e5");return List.copyOf(result);
    }
    private static final String OPENING_NAMESPACE="pc-source-opening-record-v1";
    static void attachOpening(World w,PcScenarioCatalog.Source source)throws IOException{
        if(!w.pcSourceFrame||w.extensions.get(OPENING_NAMESPACE)!=null)throw new IOException("原开局快照只能在来源新局建立");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x50534f31);out.writeUTF(source.identity.scenarioId);out.writeUTF(source.identity.sha);
        out.writeInt(source.sites.size());for(PcScenarioCatalog.Site site:source.sites){out.writeInt(site.nativeId);out.writeInt(site.id);out.writeUTF(site.name);writeFields(out,site.fields);out.writeInt(site.rate);out.writeInt(site.parent);}
        out.writeInt(source.forces.size());for(PcScenarioCatalog.Force force:source.forces){out.writeInt(force.nativeId);out.writeBoolean(force.valid);writeFields(out,force.fields);}
        w.extensions.put(OPENING_NAMESPACE,bytes.toByteArray());
    }
    private static void writeFields(DataOutputStream out,Map<Integer,Integer> fields)throws IOException{
        out.writeInt(fields.size());for(Map.Entry<Integer,Integer> e:fields.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue());}
    }
    /** Original immutable force records, from the save rather than the current catalog. */
    public static Map<Integer,Map<Integer,Integer>> savedForces(World w)throws IOException{
        if(!w.pcSourceFrame)return Collections.emptyMap();byte[] raw=w.extensions.get(OPENING_NAMESPACE);if(raw==null)return Collections.emptyMap();
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(in.readInt()!=0x50534f31||source==null||!in.readUTF().equals(source.scenarioId)||!in.readUTF().equals(source.sha))throw new IOException("原开局快照来源不一致");
        int n=PcOfficerInfo.bounded(in.readInt(),87,87);for(int i=0;i<n;i++){if(in.readInt()!=i||in.readInt()<0)throw new IOException("原据点快照引用错误");in.readUTF();readFields(in);PcOfficerInfo.bounded(in.readInt(),0,255);PcOfficerInfo.bounded(in.readInt(),0,999999);}
        n=PcOfficerInfo.bounded(in.readInt(),47,47);Map<Integer,Map<Integer,Integer>> result=new TreeMap<>();for(int i=0;i<n;i++){if(in.readInt()!=i)throw new IOException("原势力快照引用错误");boolean valid=in.readBoolean();Map<Integer,Integer> fields=readFields(in);if(valid)result.put(i,fields);}
        if(in.available()!=0)throw new IOException("原开局快照尾部未知");return Collections.unmodifiableMap(result);
    }
    private static Map<Integer,Integer> readFields(DataInputStream in)throws IOException{
        int n=PcOfficerInfo.bounded(in.readInt(),0,400);Map<Integer,Integer> result=new TreeMap<>();for(int i=0;i<n;i++){int key=PcOfficerInfo.bounded(in.readInt(),0,400);if(result.put(key,in.readInt())!=null)throw new IOException("原快照字段重复");}return Collections.unmodifiableMap(result);
    }
    static Person read(DataInputStream in)throws IOException{
        int nativeId=PcOfficerInfo.bounded(in.readInt(),0,1099),id=PcOfficerInfo.bounded(in.readInt(),-1,999999);boolean strict=in.readBoolean();
        String name=PcOfficerInfo.text(in,1024),raw=PcOfficerInfo.text(in,128),sha=PcOfficerInfo.text(in,64),courtesy=PcOfficerInfo.text(in,1024),courtesyRaw=PcOfficerInfo.text(in,128);
        if(!FORMAT_0.matcher(sha).matches()||!FORMAT_1.matcher(raw).matches()||id>=0&&raw.isEmpty()||strict&&id<0)throw new IOException("原人物字节身份无效");
        int n=PcOfficerInfo.bounded(in.readInt(),0,300);Map<Integer,Integer> fields=new TreeMap<>();
        for(int i=0;i<n;i++){int key=PcOfficerInfo.bounded(in.readInt(),0,300);if(fields.put(key,in.readInt())!=null)throw new IOException("原人物字段重复");}
        n=PcOfficerInfo.bounded(in.readInt(),0,128);List<String> unknown=new ArrayList<>();for(int i=0;i<n;i++)unknown.add(PcOfficerInfo.text(in,256));
        return new Person(nativeId,id,strict,name,raw,sha,courtesy,courtesyRaw,fields,unknown);
    }
    static void write(DataOutputStream out,Person p)throws IOException{
        out.writeInt(p.nativeId);out.writeInt(p.officerId);out.writeBoolean(p.strictIdentity);PcOfficerInfo.text(out,p.originalName);PcOfficerInfo.text(out,p.nameRaw);PcOfficerInfo.text(out,p.recordSha);PcOfficerInfo.text(out,p.courtesy);PcOfficerInfo.text(out,p.courtesyRaw);
        out.writeInt(p.fields.size());for(Map.Entry<Integer,Integer> e:p.fields.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue());}
        out.writeInt(p.unknown.size());for(String gap:p.unknown)PcOfficerInfo.text(out,gap);
    }
    static void attach(World w,List<Person> people)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.extensions.get(NAMESPACE)!=null)throw new IOException("原人物状态只能明确新局附加");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeInt(people.size());for(Person p:people)write(out,p);w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
}
