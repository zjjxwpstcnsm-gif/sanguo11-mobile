package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Pinned installation candidates. Distinct files remain distinct selectable sources. */
public final class PcScenarioCatalog {
    private PcScenarioCatalog(){}
    static final class Site {
        final int nativeId,id,rate,parent;final String name;final Map<Integer,Integer> fields;
        Site(int nativeId,int id,String name,Map<Integer,Integer> fields,int rate,int parent){this.nativeId=nativeId;this.id=id;this.name=name;this.fields=fields;this.rate=rate;this.parent=parent;}
        int value(int key)throws IOException{Integer v=fields.get(key);if(v==null)throw new IOException("原据点字段缺失");return v;}
    }
    static final class Force {
        final int nativeId;final boolean valid;final Map<Integer,Integer> fields;
        Force(int id,boolean valid,Map<Integer,Integer> fields){nativeId=id;this.valid=valid;this.fields=fields;}
        int value(int key)throws IOException{Integer v=fields.get(key);if(v==null)throw new IOException("原势力字段缺失");return v;}
    }
    public static final class Source {
        public final PcScenarioIdentity.Source identity;
        final List<Site> sites;final List<Force> forces;final List<PcScenarioPeople.Person> people;final String inputSha;final int strictCount,forceCount;final Map<Integer,List<int[]>> plots;
        Source(PcScenarioIdentity.Source identity,List<Site> sites,List<Force> forces,List<PcScenarioPeople.Person> people,String inputSha,int strictCount,int forceCount,Map<Integer,List<int[]>> plots){this.identity=identity;this.sites=List.copyOf(sites);this.forces=List.copyOf(forces);this.people=List.copyOf(people);this.inputSha=inputSha;this.strictCount=strictCount;this.forceCount=forceCount;this.plots=plots;}
        public String label(){return identity.name+" · "+identity.year+"年"+identity.month+"月 · "+identity.path.substring(identity.path.lastIndexOf('/')+1);}
        public int strictPeople(){return strictCount;}
        public int playerForces(){return forceCount;}
    }
    private static List<Source> cached;
    public static synchronized List<Source> all()throws IOException{
        if(cached!=null)return cached;String expected;
        try(InputStream in=PcScenarioCatalog.class.getResourceAsStream("/pc-scenarios/index.txt")){if(in==null)throw new IOException("PC剧本来源目录缺失");expected=new String(readBytes(in,128),StandardCharsets.US_ASCII).trim();}
        byte[] raw=resource("catalog.bin.gz",expected,1024*1024);
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=0x50434931||!PcScenarioIdentity.EXE_SHA.equals(text(in)))throw new IOException("PC剧本原EXE或版本不匹配");
        int n=bound(in,1,100);List<Source> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=0;i<n;i++){
            PcScenarioIdentity.Source identity=identity(in);int strict=bound(in,0,1100),forces=bound(in,0,42);String sha=text(in);
            if(!sha.matches("[0-9a-f]{64}")||!ids.add(identity.scenarioId))throw new IOException("PC剧本来源重复或指纹错误");
            result.add(new Source(identity,List.of(),List.of(),List.of(),sha,strict+PcOfficerIdentities.newSourceCount(identity.scenarioId),forces,Collections.emptyMap()));
        }
        if(in.available()!=0)throw new IOException("PC剧本目录未知尾部");return cached=List.copyOf(result);
    }
    private static PcScenarioIdentity.Source identity(DataInputStream in)throws IOException{
        String id=text(in),name=text(in),variant=text(in),path=text(in),sha=text(in),shared=text(in);int year=in.readInt(),month=in.readInt(),day=in.readInt();
        int gaps=bound(in,0,128);List<String> unknown=new ArrayList<>();for(int i=0;i<gaps;i++)unknown.add(text(in));
        return new PcScenarioIdentity.Source(id,name,variant,path,sha,shared,year,month,day,unknown);
    }
    private static byte[] resource(String name,String expected,int limit)throws IOException{
        byte[] raw;try(InputStream in=PcScenarioCatalog.class.getResourceAsStream("/pc-scenarios/"+name)){if(in==null)throw new IOException("PC剧本来源缺失");try(InputStream unzip=new GZIPInputStream(in)){raw=readBytes(unzip,limit);}}
        try{StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))h.append(String.format(Locale.ROOT,"%02x",b&255));if(!h.toString().equals(expected))throw new IOException("PC剧本来源指纹不匹配");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}return raw;
    }
    private static Source read(Source summary)throws IOException{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(resource(summary.identity.scenarioId+".bin.gz",summary.inputSha,2*1024*1024)));PcScenarioIdentity.Source identity=identity(in);
        if(!summary.identity.scenarioId.equals(identity.scenarioId))throw new IOException("PC剧本来源ID不一致");
        int count=bound(in,87,87);List<Site> sites=new ArrayList<>();Set<Integer> siteIds=new HashSet<>();
        for(int j=0;j<count;j++){int nativeId=bound(in,0,86),siteId=bound(in,0,999999);String siteName=text(in);Map<Integer,Integer> fields=fields(in);if(nativeId!=j||!siteIds.add(siteId))throw new IOException("PC据点连接重复");sites.add(new Site(nativeId,siteId,siteName,fields,bound(in,0,255),bound(in,0,999999)));}
        count=bound(in,47,47);List<Force> forces=new ArrayList<>();for(int j=0;j<count;j++){int nativeId=bound(in,0,46);boolean valid=in.readBoolean();Map<Integer,Integer> fields=fields(in);if(nativeId!=j)throw new IOException("PC势力连接错位");forces.add(new Force(nativeId,valid,fields));}
        count=bound(in,591,591);Map<Integer,List<int[]>> plots=new TreeMap<>();for(int i=0;i<count;i++){int city=bound(in,0,999999),x=bound(in,0,199),y=bound(in,0,199);plots.computeIfAbsent(city,k->new ArrayList<>()).add(new int[]{x,y});}
        count=bound(in,850,850);List<PcScenarioPeople.Person> people=new ArrayList<>();Set<Integer> officerIds=new HashSet<>();for(int j=0;j<count;j++){PcScenarioPeople.Person p=PcScenarioPeople.read(in);if(p.nativeId!=j||p.officerId>=0&&!officerIds.add(p.officerId))throw new IOException("PC人物连接错位");people.add(p);}
        if(in.available()!=0)throw new IOException("PC剧本开局未知尾部");return new Source(identity,sites,forces,people,summary.inputSha,summary.strictCount,summary.forceCount,plots);
    }
    private static Map<Integer,Integer> fields(DataInputStream in)throws IOException{int n=bound(in,0,400);Map<Integer,Integer> result=new TreeMap<>();for(int i=0;i<n;i++){int key=bound(in,0,400),value=in.readInt();if(result.put(key,value)!=null)throw new IOException("PC字段重复");}return Collections.unmodifiableMap(result);}
    private static int bound(DataInputStream in,int min,int max)throws IOException{return PcOfficerInfo.bounded(in.readInt(),min,max);}
    private static String text(DataInputStream in)throws IOException{return PcOfficerInfo.text(in,2048);}
    private static byte[] readBytes(InputStream in,int max)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(bytes.size()+n>max)throw new IOException("PC剧本目录过大");bytes.write(buf,0,n);}return bytes.toByteArray();}
    public static boolean contains(String id)throws IOException{for(Source s:all())if(s.identity.scenarioId.equals(id))return true;return false;}
    public static World load(String id,int player,long seed)throws IOException{for(Source s:all())if(s.identity.scenarioId.equals(id))return PcScenarioOpening.create(read(s),player,seed);throw new IOException("PC剧本来源不存在");}
    public static World preview(String id)throws IOException{for(Source s:all())if(s.identity.scenarioId.equals(id))return PcScenarioOpening.create(read(s),-1,0);throw new IOException("PC剧本来源不存在");}
}
