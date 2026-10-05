package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original personality/talk facts, attached only to explicit source new games. */
public final class PcContestProfiles {
    private static final int MAGIC=0x50435031,SAVED_MAGIC=0x50435032;
    public static final String NAMESPACE="pc-contest-profile-source-v1";
    private static final Debate.Temper[] TEMPER={Debate.Temper.TIMID,Debate.Temper.CALM,Debate.Temper.BOLD,Debate.Temper.RASH};
    private static final Debate.Talk[] TALK={Debate.Talk.SHOUT,Debate.Talk.GUILE,Debate.Talk.IGNORE,Debate.Talk.CALM,Debate.Talk.RAGE};
    private static final String[] TEMPER_LABELS={"膽小","冷靜","剛膽","莽撞"},TALK_LABELS={"大喝","詭辯","無視","鎮靜","憤怒"};
    private PcContestProfiles(){}
    public static final class Fact {
        public final int officerId,nativeId,nativePersonality,nativeTalkMask;
        public final String recordSha;
        Fact(int officer,int nativeId,int personality,int mask,String sha){officerId=officer;this.nativeId=nativeId;nativePersonality=personality;nativeTalkMask=mask;recordSha=sha;}
        public String personality(){return TEMPER_LABELS[nativePersonality];}
        public String talks(){List<String> labels=new ArrayList<>();for(int i=0;i<5;i++)if((nativeTalkMask&(1<<i))!=0)labels.add(TALK_LABELS[i]);return labels.isEmpty()?"原记录无话术标记":String.join("、",labels);}
        public Contests.Profile profile(){int mask=0;for(int i=0;i<5;i++)if((nativeTalkMask&(1<<i))!=0)mask|=1<<TALK[i].ordinal();return new Contests.Profile(TEMPER[nativePersonality],mask,0);}
    }
    private static final class Record {
        final int nativeId,birth,sex,temper,talk;
        final String sha,nameRaw;
        Record(DataInputStream in)throws IOException{nativeId=PcOfficerInfo.bounded(in.readInt(),0,849);sha=PcOfficerInfo.text(in,64);nameRaw=PcOfficerInfo.text(in,128);birth=in.readInt();sex=in.readInt();temper=PcOfficerInfo.bounded(in.readInt(),0,3);talk=PcOfficerInfo.bounded(in.readInt(),0,31);if(!sha.matches("[0-9a-f]{64}")||!nameRaw.matches("(?:[0-9a-f]{2})*"))throw new IOException("原对战记录指纹无效");}
    }
    private static final class Source {
        final String id,path,variant,sha;final List<Record> records;
        Source(DataInputStream in)throws IOException{id=PcOfficerInfo.text(in,80);path=PcOfficerInfo.text(in,1024);variant=PcOfficerInfo.text(in,300);sha=PcOfficerInfo.text(in,64);int n=PcOfficerInfo.bounded(in.readInt(),850,850);List<Record> rows=new ArrayList<>();for(int i=0;i<n;i++){Record r=new Record(in);if(r.nativeId!=i)throw new IOException("原对战记录顺序未知");rows.add(r);}records=List.copyOf(rows);}
    }
    private static List<Source> cached;
    private static synchronized List<Source> catalog()throws IOException{
        if(cached!=null)return cached;byte[] raw;
        try(InputStream resource=PcContestProfiles.class.getResourceAsStream("/pc-contest-profiles/profiles.bin.gz")){
            if(resource==null)throw new IOException("原对战属性资源缺失");try(InputStream in=new GZIPInputStream(resource);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>4*1024*1024)throw new IOException("原对战属性资源过大");bytes.write(buffer,0,n);}raw=bytes.toByteArray();}
        }
        try(InputStream index=PcContestProfiles.class.getResourceAsStream("/pc-contest-profiles/index.txt")){
            if(index==null)throw new IOException("原对战索引缺失");ByteArrayOutputStream expected=new ByteArrayOutputStream();int value;while((value=index.read())!=-1){if(expected.size()>=128)throw new IOException("原对战索引过大");expected.write(value);}String hex=new String(expected.toByteArray(),StandardCharsets.US_ASCII).trim();
            if(!hex.matches("[0-9a-f]{64}")||!hex.equals(hash(raw)))throw new IOException("原对战资源指纹变化");
        }
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=MAGIC||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA)||!PcOfficerInfo.text(in,64).equals("e4341546432e8d257851ed830bc3388d1f8ce8697db1547e108285d2b3b68d2c"))throw new IOException("原对战资源来源未知");
        int n=PcOfficerInfo.bounded(in.readInt(),16,16);List<Source> sources=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<n;i++){Source s=new Source(in);if(!ids.add(s.id))throw new IOException("原对战来源重复");sources.add(s);}if(in.available()!=0)throw new IOException("原对战资源尾部未知");return cached=List.copyOf(sources);
    }
    private static String hash(byte[] bytes)throws IOException{try{StringBuilder b=new StringBuilder();for(byte n:MessageDigest.getInstance("SHA-256").digest(bytes))b.append(String.format(Locale.ROOT,"%02x",n&255));return b.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    static void initializeOpening(World w,PcScenarioCatalog.Source opening)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原对战属性只能在明确新局初始化");
        Source source=null;for(Source s:catalog())if(s.id.equals(opening.identity.scenarioId))source=s;
        if(source==null||!source.sha.equals(opening.identity.sha)||!source.path.equals(opening.identity.path)||!source.variant.equals(opening.identity.sourceVariant))throw new IOException("原对战来源不匹配");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(SAVED_MAGIC);PcOfficerInfo.text(out,source.id);PcOfficerInfo.text(out,source.sha);out.writeInt(850);
        for(PcScenarioPeople.Person person:opening.people){Record r=source.records.get(person.nativeId);
            if(!r.sha.equals(person.recordSha)||!r.nameRaw.equals(person.nameRaw)||person.officerId>=0&&(r.birth!=person.field(8)||r.sex!=person.field(6)||r.temper!=person.field(47)))throw new IOException("原对战人物身份校验失败："+person.nativeId);
            Fact fact=new Fact(person.officerId,person.nativeId,r.temper,r.talk,r.sha);
            if(person.officerId>=0){if(w.contests.hasProfile(person.officerId)||w.officer(person.officerId)==null)throw new IOException("原对战人物配置冲突");w.contests.configure(person.officerId,fact.profile());}
            out.writeInt(fact.officerId);out.writeInt(fact.nativeId);out.writeInt(fact.nativePersonality);out.writeInt(fact.nativeTalkMask);PcOfficerInfo.text(out,fact.recordSha);
        }
        w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    /** Read only saved facts; old saves are never backfilled from catalog(). */
    public static Map<Integer,Fact> saved(World w)throws IOException{
        if(!w.pcSourceFrame)return Collections.emptyMap();byte[] raw=w.extensions.get(NAMESPACE);if(raw==null)return Collections.emptyMap();
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(in.readInt()!=SAVED_MAGIC||source==null||!PcOfficerInfo.text(in,80).equals(source.scenarioId)||!PcOfficerInfo.text(in,64).equals(source.sha))throw new IOException("已存原对战来源不匹配");
        int n=PcOfficerInfo.bounded(in.readInt(),850,850);Map<Integer,Fact> result=new TreeMap<>();Set<Integer> natives=new HashSet<>();
        for(int i=0;i<n;i++){int id=PcOfficerInfo.bounded(in.readInt(),-1,999999),nativeId=PcOfficerInfo.bounded(in.readInt(),0,849),temper=PcOfficerInfo.bounded(in.readInt(),0,3),mask=PcOfficerInfo.bounded(in.readInt(),0,31);String sha=PcOfficerInfo.text(in,64);if(!sha.matches("[0-9a-f]{64}")||!natives.add(nativeId))throw new IOException("已存原对战记录无效");if(id>=0&&(w.officer(id)==null||result.put(id,new Fact(id,nativeId,temper,mask,sha))!=null))throw new IOException("已存原对战人物无效");}
        if(in.available()!=0)throw new IOException("已存原对战尾部未知");return Collections.unmodifiableMap(result);
    }
}
