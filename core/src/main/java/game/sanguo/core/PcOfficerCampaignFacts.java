package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original hidden record snapshot, attached only at explicit source new-game
 * construction. Never substitutes for current campaign loyalty or backfills saves. */
public final class PcOfficerCampaignFacts {
    public static final String NAMESPACE="pc-officer-campaign-record-v1";
    private static final int CATALOG_MAGIC=0x50434631,SAVED_MAGIC=0x50434632;
    private static final String IDENTITY_SHA="e4341546432e8d257851ed830bc3388d1f8ce8697db1547e108285d2b3b68d2c";
    // 16x850 records share these formats. Compile once; retain full-match semantics.
    private static final java.util.regex.Pattern RECORD_SHA=java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final java.util.regex.Pattern RAW_NAME=java.util.regex.Pattern.compile("(?:[0-9a-f]{2})*");
    public static final class Fact {
        public final int officerId,nativeId,initialRawLoyalty;
        public final String recordSha;
        Fact(int officerId,int nativeId,int raw,String sha){this.officerId=officerId;this.nativeId=nativeId;initialRawLoyalty=raw;recordSha=sha;}
    }
    private static final class Record {
        final int nativeId,birth,sex,raw;final String sha,nameRaw;
        Record(DataInputStream in)throws IOException{nativeId=PcOfficerInfo.bounded(in.readInt(),0,849);sha=PcOfficerInfo.text(in,64);nameRaw=PcOfficerInfo.text(in,128);birth=in.readInt();sex=in.readInt();raw=in.readUnsignedByte();if(!RECORD_SHA.matcher(sha).matches()||!RAW_NAME.matcher(nameRaw).matches())throw new IOException("原内部人物记录指纹无效");}
    }
    private static final class Source {
        final String id,path,variant,sha;final List<Record> rows;
        Source(DataInputStream in)throws IOException{id=PcOfficerInfo.text(in,80);path=PcOfficerInfo.text(in,1024);variant=PcOfficerInfo.text(in,300);sha=PcOfficerInfo.text(in,64);int n=PcOfficerInfo.bounded(in.readInt(),850,850);List<Record> records=new ArrayList<>();for(int i=0;i<n;i++){Record r=new Record(in);if(r.nativeId!=i)throw new IOException("原内部人物记录顺序无效");records.add(r);}rows=List.copyOf(records);}
    }
    private static List<Source> cached;
    private static byte[] boundedBytes(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>max)throw new IOException("原内部人物资源过大");out.write(buffer,0,n);}return out.toByteArray();}
    private static String hash(byte[] data)throws IOException{try{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    private static synchronized List<Source> catalog()throws IOException{
        if(cached!=null)return cached;byte[] raw;
        try(InputStream resource=PcOfficerCampaignFacts.class.getResourceAsStream("/pc-officer-campaign/facts.bin.gz")){
            if(resource==null)throw new IOException("原内部人物资源缺失");try(InputStream in=new GZIPInputStream(resource)){raw=boundedBytes(in,4*1024*1024);}
        }
        try(InputStream index=PcOfficerCampaignFacts.class.getResourceAsStream("/pc-officer-campaign/index.txt")){
            if(index==null)throw new IOException("原内部人物索引缺失");String expected=new String(boundedBytes(index,128),StandardCharsets.US_ASCII).trim();if(!RECORD_SHA.matcher(expected).matches()||!expected.equals(hash(raw)))throw new IOException("原内部人物资源指纹改变");
        }
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=CATALOG_MAGIC||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA)||!PcOfficerInfo.text(in,64).equals(IDENTITY_SHA))throw new IOException("原内部人物资源来源不明");
        int n=PcOfficerInfo.bounded(in.readInt(),16,16);List<Source> sources=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<n;i++){Source s=new Source(in);if(!ids.add(s.id))throw new IOException("原内部人物来源重复");sources.add(s);}if(in.available()!=0)throw new IOException("原内部人物资源未知尾部");return cached=List.copyOf(sources);
    }
    static void initializeOpening(World w,PcScenarioCatalog.Source opening)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null||opening.people.size()!=850)throw new IOException("原内部人物快照仅能在明确新局建立");
        Source source=null;for(Source s:catalog())if(s.id.equals(opening.identity.scenarioId))source=s;
        if(source==null||!source.sha.equals(opening.identity.sha)||!source.path.equals(opening.identity.path)||!source.variant.equals(opening.identity.sourceVariant))throw new IOException("原内部人物来源不一致");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(SAVED_MAGIC);PcOfficerInfo.text(out,source.id);PcOfficerInfo.text(out,source.sha);PcOfficerInfo.text(out,source.variant);out.writeInt(850);
        for(PcScenarioPeople.Person p:opening.people){Record r=source.rows.get(p.nativeId);if(!r.sha.equals(p.recordSha)||!r.nameRaw.equals(p.nameRaw)||p.officerId>=0&&(r.birth!=p.field(8)||r.sex!=p.field(6)||w.officer(p.officerId)==null))throw new IOException("原内部人物身份校验失败："+p.nativeId);
            out.writeInt(p.officerId);out.writeInt(p.nativeId);out.writeByte(r.raw);PcOfficerInfo.text(out,r.sha);
        }
        w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    public static Map<Integer,Fact> saved(World w)throws IOException{
        if(!w.pcSourceFrame)return Collections.emptyMap();byte[] raw=w.extensions.get(NAMESPACE);if(raw==null)return Collections.emptyMap();
        if(raw.length>128*1024)throw new IOException("原内部人物保存过大");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(in.readInt()!=SAVED_MAGIC||source==null||!PcOfficerInfo.text(in,80).equals(source.scenarioId)||!PcOfficerInfo.text(in,64).equals(source.sha)||!PcOfficerInfo.text(in,300).equals(source.sourceVariant))throw new IOException("原内部人物保存来源不一致");
        Map<Integer,PcScenarioPeople.Person> people=new HashMap<>();for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))people.put(p.nativeId,p);
        int n=PcOfficerInfo.bounded(in.readInt(),850,850);Map<Integer,Fact> result=new TreeMap<>();
        for(int i=0;i<n;i++){int id=PcOfficerInfo.bounded(in.readInt(),-1,999999),nativeId=PcOfficerInfo.bounded(in.readInt(),0,849),value=in.readUnsignedByte();String sha=PcOfficerInfo.text(in,64);PcScenarioPeople.Person p=people.get(nativeId);
            if(nativeId!=i||p==null||p.officerId!=id||!p.recordSha.equals(sha))throw new IOException("原内部人物保存身份不一致");
            if(id>=0){if(w.officer(id)==null||result.put(id,new Fact(id,nativeId,value,sha))!=null)throw new IOException("原内部人物保存引用重复或失效");}
        }
        if(in.available()!=0)throw new IOException("原内部人物保存未知尾部");return Collections.unmodifiableMap(result);
    }
    private PcOfficerCampaignFacts(){}
}
