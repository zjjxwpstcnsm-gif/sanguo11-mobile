package game.sanguo.core;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/** Saved source text facts. Loading/querying never consults the current catalog. */
public final class PcOfficerInfo {
    public static final String NAMESPACE="pc-officer-source-v1";
    static final int MAGIC=0x50434f31,MAX_BYTES=16*1024*1024;
    private PcOfficerInfo(){}
    public static final class Person {
        public final int officerId,nativeId;
        public final String worldName,sourceVariant,sourcePath,sourceSha,recordSha,courtesy,courtesyRaw;
        public final String biography,biographyResourceSha,biographyRenderedSha;
        public final List<String> unknown;
        public Person(int officerId,int nativeId,String worldName,String sourceVariant,String sourcePath,String sourceSha,
                      String recordSha,String courtesy,String courtesyRaw,String biography,String biographyResourceSha,
                      String biographyRenderedSha,List<String> unknown){
            this.officerId=officerId;this.nativeId=nativeId;this.worldName=worldName;this.sourceVariant=sourceVariant;
            this.sourcePath=sourcePath;this.sourceSha=sourceSha;this.recordSha=recordSha;this.courtesy=courtesy;
            this.courtesyRaw=courtesyRaw;this.biography=biography;this.biographyResourceSha=biographyResourceSha;
            this.biographyRenderedSha=biographyRenderedSha;this.unknown=List.copyOf(unknown);
        }
        Person named(String name){return new Person(officerId,nativeId,name,sourceVariant,sourcePath,sourceSha,recordSha,
            courtesy,courtesyRaw,biography,biographyResourceSha,biographyRenderedSha,unknown);}
    }
    public static Map<Integer,Person> saved(World world)throws IOException{
        byte[] raw=world.extensions.get(NAMESPACE);if(raw==null)return Collections.emptyMap();
        DataInputStream input=new DataInputStream(new ByteArrayInputStream(raw));
        if(input.readInt()!=MAGIC)throw new IOException("人物来源资料格式未知");
        int count=bounded(input.readInt(),0,10000);Map<Integer,Person> result=new LinkedHashMap<>();
        for(int i=0;i<count;i++){
            Person p=read(input);if(result.put(p.officerId,p)!=null)throw new IOException("人物来源编号重复");
        }
        if(input.available()!=0)throw new IOException("人物来源资料尾部未知");
        return Collections.unmodifiableMap(result);
    }
    static byte[] encode(Collection<Person> people)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(people.size());for(Person p:people)write(out,p);out.flush();
        if(bytes.size()>MAX_BYTES)throw new IOException("人物来源资料过大");return bytes.toByteArray();
    }
    static void write(DataOutputStream out,Person p)throws IOException{
        out.writeInt(p.officerId);out.writeInt(p.nativeId);
        for(String s:Arrays.asList(p.worldName,p.sourceVariant,p.sourcePath,p.sourceSha,p.recordSha,p.courtesy,p.courtesyRaw,
                p.biography,p.biographyResourceSha,p.biographyRenderedSha))text(out,s);
        out.writeInt(p.unknown.size());for(String s:p.unknown)text(out,s);
    }
    static Person read(DataInputStream in)throws IOException{
        int id=bounded(in.readInt(),0,1000000),nativeId=bounded(in.readInt(),0,1099);String[] s=new String[10];
        for(int i=0;i<s.length;i++)s[i]=text(in,i==7?32768:1024);
        if(s[0].isEmpty()||s[1].isEmpty()||s[2].isEmpty()||!s[3].matches("[0-9a-f]{64}")||!s[4].matches("[0-9a-f]{64}")||
                !s[6].matches("(?:[0-9a-f]{2})*")||!s[8].matches("[0-9a-f]{64}")||!s[9].matches("(?:[0-9a-f]{64})?"))
            throw new IOException("人物来源资料身份或指纹无效");
        int n=bounded(in.readInt(),0,100);List<String> unknown=new ArrayList<>();for(int i=0;i<n;i++)unknown.add(text(in,256));
        return new Person(id,nativeId,s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9],unknown);
    }
    static int bounded(int value,int min,int max)throws IOException{
        if(value<min||value>max)throw new IOException("人物来源字段越界");return value;
    }
    static String text(DataInputStream in,int max)throws IOException{
        int n=bounded(in.readInt(),0,max);if(n>in.available())throw new IOException("人物来源资料截断");
        byte[] raw=new byte[n];in.readFully(raw);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(raw)).toString();
    }
    static void text(DataOutputStream out,String text)throws IOException{
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }
}
