package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Explicit opening-only source-text attachment; no numerical/rule import. */
public final class PcOfficerSources {
    private PcOfficerSources(){}
    public static final class Source {
        public final String id,name,path,sha;
        public final int year,month,day;
        private final Map<Integer,PcOfficerInfo.Person> people;
        private final Map<Integer,List<String>> acceptedNames;
        private Source(String id,String name,String path,String sha,int year,int month,int day,
                       Map<Integer,PcOfficerInfo.Person> people,Map<Integer,List<String>> names){
            this.id=id;this.name=name;this.path=path;this.sha=sha;this.year=year;this.month=month;this.day=day;
            this.people=Collections.unmodifiableMap(people);this.acceptedNames=Collections.unmodifiableMap(names);
        }
        public String label(){return name+" · "+year+"年"+month+"月 · "+path.substring(path.lastIndexOf('/')+1);}
        public int count(){return people.size();}
    }
    private static List<Source> cached;
    public static synchronized List<Source> all()throws IOException{
        if(cached!=null)return cached;
        String expected;
        try(InputStream in=PcOfficerSources.class.getResourceAsStream("/pc-officers/index.txt")){
            if(in==null)throw new IOException("PC人物资料索引缺失");
            ByteArrayOutputStream index=new ByteArrayOutputStream();int b;while((b=in.read())!=-1){if(index.size()>=128)throw new IOException("PC人物资料索引过大");index.write(b);}
            expected=new String(index.toByteArray(),StandardCharsets.US_ASCII).trim();
            if(!expected.matches("[0-9a-f]{64}"))throw new IOException("PC人物资料索引无效");
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream resource=PcOfficerSources.class.getResourceAsStream("/pc-officers/source-text.bin.gz")){
            if(resource==null)throw new IOException("PC人物资料缺失");
            try(InputStream in=new GZIPInputStream(resource)){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>PcOfficerInfo.MAX_BYTES)throw new IOException("PC人物资料过大");bytes.write(buffer,0,n);}
            }
        }
        byte[] raw=bytes.toByteArray();
        try{StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(raw))hash.append(String.format(Locale.ROOT,"%02x",b&255));if(!hash.toString().equals(expected))throw new IOException("PC人物资料指纹变化");}
        catch(NoSuchAlgorithmException e){throw new IOException(e);}
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=0x50435431)throw new IOException("PC人物资料版本未知");
        int count=PcOfficerInfo.bounded(in.readInt(),1,100);List<Source> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=0;i<count;i++){
            String id=PcOfficerInfo.text(in,80),name=PcOfficerInfo.text(in,200),path=PcOfficerInfo.text(in,1024),sha=PcOfficerInfo.text(in,64);
            if(!id.matches("[a-z0-9-]{1,80}")||!sha.matches("[0-9a-f]{64}")||!ids.add(id))throw new IOException("PC人物来源无效");
            int year=PcOfficerInfo.bounded(in.readInt(),1,9999),month=PcOfficerInfo.bounded(in.readInt(),1,12),day=PcOfficerInfo.bounded(in.readInt(),1,31);
            int n=PcOfficerInfo.bounded(in.readInt(),0,10000);Map<Integer,PcOfficerInfo.Person> people=new LinkedHashMap<>();Map<Integer,List<String>> names=new LinkedHashMap<>();
            for(int j=0;j<n;j++){
                PcOfficerInfo.Person p=PcOfficerInfo.read(in);
                if(!p.sourceSha.equals(sha)||!p.sourcePath.equals(path)||people.put(p.officerId,p)!=null)throw new IOException("PC人物来源连接无效");
                int aliases=PcOfficerInfo.bounded(in.readInt(),1,8);List<String> accepted=new ArrayList<>();
                for(int k=0;k<aliases;k++)accepted.add(PcOfficerInfo.text(in,200));names.put(p.officerId,List.copyOf(accepted));
            }
            result.add(new Source(id,name,path,sha,year,month,day,people,names));
        }
        if(in.available()!=0)throw new IOException("PC人物资料尾部未知");return cached=Collections.unmodifiableList(result);
    }
    /** Only called while authoring an explicitly selected new game, before installation. */
    public static int attachOpening(World world,String sourceId)throws IOException{
        if(world.turn!=0||world.commandRevision()!=0||world.extensions.get(PcOfficerInfo.NAMESPACE)!=null)throw new IOException("人物来源只能在新局建立时选择");
        Source selected=null;for(Source source:all())if(source.id.equals(sourceId))selected=source;
        if(selected==null)throw new IOException("人物来源不存在");
        List<PcOfficerInfo.Person> records=new ArrayList<>();
        for(World.Officer officer:world.officers){
            PcOfficerInfo.Person p=selected.people.get(officer.id);if(p==null)continue;
            if(!selected.acceptedNames.get(officer.id).contains(officer.name))continue;
            records.add(p.named(officer.name));
        }
        if(records.isEmpty())throw new IOException("本局没有经身份校验可连接的人物");
        world.extensions.put(PcOfficerInfo.NAMESPACE,PcOfficerInfo.encode(records));return records.size();
    }
}
