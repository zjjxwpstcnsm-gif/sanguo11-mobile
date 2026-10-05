package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import java.util.zip.GZIPInputStream;

/** Font/name/birth/sex identity links. Runtime IDs stay stable; old saves are never backfilled. */
public final class PcOfficerIdentities {
    public static final String NAMESPACE="pc-officer-font-identity-v1";
    private static final int CATALOG=0x50474931,SAVED=0x50474932;
    public static final class Fact {
        public final int officerId,nativeId,canonicalOfficerId,birth,sex;
        public final String name,nameRaw,recordSha,courtesy,courtesyRaw,biography,biographyResourceSha,biographyRenderedSha;
        public final List<String> unknown;
        Fact(DataInputStream in)throws IOException{officerId=PcOfficerInfo.bounded(in.readInt(),100000,999999);nativeId=PcOfficerInfo.bounded(in.readInt(),0,669);canonicalOfficerId=PcOfficerInfo.bounded(in.readInt(),0,999999);birth=in.readInt();sex=PcOfficerInfo.bounded(in.readInt(),0,1);name=PcOfficerInfo.text(in,1024);nameRaw=PcOfficerInfo.text(in,128);recordSha=PcOfficerInfo.text(in,64);courtesy=PcOfficerInfo.text(in,1024);courtesyRaw=PcOfficerInfo.text(in,128);biography=PcOfficerInfo.text(in,32768);biographyResourceSha=PcOfficerInfo.text(in,64);biographyRenderedSha=PcOfficerInfo.text(in,64);int n=PcOfficerInfo.bounded(in.readInt(),0,128);List<String> gaps=new ArrayList<>();for(int i=0;i<n;i++)gaps.add(PcOfficerInfo.text(in,1024));unknown=List.copyOf(gaps);if(name.isEmpty()||!nameRaw.matches("(?:[0-9a-f]{2})+")||!recordSha.matches("[0-9a-f]{64}")||!biographyResourceSha.matches("[0-9a-f]{64}")||!biographyRenderedSha.matches("[0-9a-f]{64}"))throw new IOException("Original font identity fingerprint invalid");}
        void write(DataOutputStream out)throws IOException{for(int n:new int[]{officerId,nativeId,canonicalOfficerId,birth,sex})out.writeInt(n);for(String s:List.of(name,nameRaw,recordSha,courtesy,courtesyRaw,biography,biographyResourceSha,biographyRenderedSha))PcOfficerInfo.text(out,s);out.writeInt(unknown.size());for(String s:unknown)PcOfficerInfo.text(out,s);}
        PcOfficerInfo.Person person(PcScenarioIdentity.Source s){return new PcOfficerInfo.Person(officerId,nativeId,name,s.sourceVariant,s.path,s.sha,recordSha,courtesy,courtesyRaw,biography,biographyResourceSha,biographyRenderedSha,unknown);}
    }
    private static final class Source {final String id,path,variant,sha;final List<Fact> rows;Source(DataInputStream in)throws IOException{id=PcOfficerInfo.text(in,80);path=PcOfficerInfo.text(in,1024);variant=PcOfficerInfo.text(in,300);sha=PcOfficerInfo.text(in,64);int n=PcOfficerInfo.bounded(in.readInt(),4,4);List<Fact> facts=new ArrayList<>();for(int i=0;i<n;i++)facts.add(new Fact(in));rows=List.copyOf(facts);}}
    private static List<Source> catalog;private static String proofSha;
    private static byte[] bytes(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>max)throw new IOException("Original font identity resource too large");out.write(b,0,n);}return out.toByteArray();}
    private static String hash(byte[] data)throws IOException{try{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    private static synchronized List<Source> catalog()throws IOException{
        if(catalog!=null)return catalog;byte[] raw;
        try(InputStream input=PcOfficerIdentities.class.getResourceAsStream("/pc-officer-identities/identities.bin.gz")){if(input==null)throw new IOException("Original font identity resource missing");try(InputStream unzip=new GZIPInputStream(input)){raw=bytes(unzip,1024*1024);}}
        try(InputStream index=PcOfficerIdentities.class.getResourceAsStream("/pc-officer-identities/index.txt")){if(index==null||!new String(bytes(index,128),StandardCharsets.US_ASCII).trim().equals(hash(raw)))throw new IOException("Original font identity resource SHA differs");}
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=CATALOG||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA))throw new IOException("Original font identity provenance differs");proofSha=PcOfficerInfo.text(in,64);if(!proofSha.matches("[0-9a-f]{64}"))throw new IOException("Original glyph evidence SHA missing");int n=PcOfficerInfo.bounded(in.readInt(),16,16);List<Source> result=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<n;i++){Source s=new Source(in);if(!ids.add(s.id))throw new IOException("Original font identity source duplicated");result.add(s);}if(in.available()!=0)throw new IOException("Original font identity tail unknown");return catalog=List.copyOf(result);
    }
    static int newSourceCount(String id)throws IOException{for(Source s:catalog())if(s.id.equals(id))return s.rows.size();throw new IOException("Original font identity source missing");}
    static String openingName(PcScenarioCatalog.Source opening,PcScenarioPeople.Person person)throws IOException{
        if(person.officerId<100000)return person.originalName;
        for(Source s:catalog())if(s.id.equals(opening.identity.scenarioId)&&s.sha.equals(opening.identity.sha)&&s.variant.equals(opening.identity.sourceVariant))for(Fact f:s.rows)if(f.officerId==person.officerId){if(f.nativeId!=person.nativeId||!f.recordSha.equals(person.recordSha)||!f.nameRaw.equals(person.nameRaw)||f.birth!=person.field(8)||f.sex!=person.field(6))throw new IOException("Original font name identity differs");return f.name;}
        throw new IOException("Original font name link missing");
    }
    static void initializeOpening(World w,PcScenarioCatalog.Source opening)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.extensions.get(NAMESPACE)!=null)throw new IOException("Original font identity needs explicit fresh source game");
        Source source=null;for(Source s:catalog())if(s.id.equals(opening.identity.scenarioId))source=s;
        if(source==null||!source.path.equals(opening.identity.path)||!source.sha.equals(opening.identity.sha)||!source.variant.equals(opening.identity.sourceVariant))throw new IOException("Original font identity source differs");
        Map<Integer,PcOfficerInfo.Person> text=new TreeMap<>(PcOfficerInfo.saved(w));ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);out.writeInt(SAVED);PcOfficerInfo.text(out,source.id);PcOfficerInfo.text(out,source.sha);PcOfficerInfo.text(out,source.variant);PcOfficerInfo.text(out,proofSha);out.writeInt(source.rows.size());
        for(Fact f:source.rows){PcScenarioPeople.Person p=opening.people.get(f.nativeId);if(p.officerId!=f.officerId||p.strictIdentity||!p.recordSha.equals(f.recordSha)||!p.nameRaw.equals(f.nameRaw)||p.field(8)!=f.birth||p.field(6)!=f.sex||w.officer(f.officerId)==null)throw new IOException("Original font identity participant differs");f.write(out);if(!w.officer(f.officerId).name.equals(f.name))throw new IOException("Original fresh source name differs");if(text.put(f.officerId,f.person(opening.identity))!=null)throw new IOException("Original font text participant already attached");}
        w.extensions.put(NAMESPACE,buffer.toByteArray());w.extensions.put(PcOfficerInfo.NAMESPACE,PcOfficerInfo.encode(text.values()));
    }
    public static Map<Integer,Fact> saved(World w)throws IOException{
        byte[] raw=w.extensions.get(NAMESPACE);if(!w.pcSourceFrame||raw==null)return Collections.emptyMap();if(raw.length>128*1024)throw new IOException("Original font identity save too large");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(in.readInt()!=SAVED||source==null||!PcOfficerInfo.text(in,80).equals(source.scenarioId)||!PcOfficerInfo.text(in,64).equals(source.sha)||!PcOfficerInfo.text(in,300).equals(source.sourceVariant)||!PcOfficerInfo.text(in,64).matches("[0-9a-f]{64}"))throw new IOException("Original font identity saved source differs");
        Map<Integer,PcScenarioPeople.Person> people=new HashMap<>();for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))people.put(p.nativeId,p);int n=PcOfficerInfo.bounded(in.readInt(),4,4);Map<Integer,Fact> result=new TreeMap<>();Set<Integer> canonical=new HashSet<>();for(int i=0;i<n;i++){Fact f=new Fact(in);PcScenarioPeople.Person p=people.get(f.nativeId);if(p==null||p.officerId!=f.officerId||!p.recordSha.equals(f.recordSha)||!p.nameRaw.equals(f.nameRaw)||p.field(8)!=f.birth||p.field(6)!=f.sex||w.officer(f.officerId)==null||result.put(f.officerId,f)!=null||!canonical.add(f.canonicalOfficerId))throw new IOException("Original font identity saved participant differs");}if(in.available()!=0)throw new IOException("Original font identity saved tail unknown");return Collections.unmodifiableMap(result);
    }
    static void validate(World w)throws IOException{if(w.extensions.get(NAMESPACE)!=null&&w.pcSourceFrame)saved(w);}
    private PcOfficerIdentities(){}
}
