package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Saved, explicit installation-source identity. Never looked up during decoding.
 * This identifies a candidate source, not completed opening/event restoration. */
public final class PcScenarioIdentity {
    private static final java.util.regex.Pattern FORMAT_0=java.util.regex.Pattern.compile("[0-9a-f]{64}");
    private static final java.util.regex.Pattern FORMAT_1=java.util.regex.Pattern.compile("pc-scen[0-9]{3}-[0-9a-f]{64}");
    private static final java.util.regex.Pattern FORMAT_2=java.util.regex.Pattern.compile("[a-z0-9-]+");
    private static final java.util.regex.Pattern FORMAT_3=java.util.regex.Pattern.compile("Media/scenario/(?i:Scen)[0-9]{3}\\.(?i:S11)");
    public static final String NAMESPACE="pc-scenario-source-v1";
    public static final String DATA_SOURCE="pc-installed-candidate";
    public static final String EXE_SHA="30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb";
    public static final int SAVE_VERSION=38, MAX_FACTIONS=47;
    private static final int MAGIC=0x50534331;
    private PcScenarioIdentity(){}
    public static final class Source {
        public final String scenarioId,name,sourceVariant,path,sha,sharedSha;
        public final int year,month,day;
        public final List<String> unknown;
        public Source(String scenarioId,String name,String sourceVariant,String path,String sha,String sharedSha,
                      int year,int month,int day,List<String> unknown)throws IOException{
            if(scenarioId==null||sha==null||!FORMAT_0.matcher(sha).matches()||!FORMAT_1.matcher(scenarioId).matches()||!scenarioId.endsWith(sha))throw new IOException("PC剧本身份/SHA无效");
            if(name==null||name.isEmpty()||name.length()>100||sourceVariant==null||sourceVariant.length()>200||!FORMAT_2.matcher(sourceVariant).matches()||!sourceVariant.endsWith(sha))throw new IOException("PC剧本名称/变体无效");
            if(path==null||!FORMAT_3.matcher(path).matches()||!path.substring(path.lastIndexOf('/')+1,path.lastIndexOf('.')).substring(4).equals(scenarioId.substring(7,10)))throw new IOException("PC剧本路径/槽位无效");
            if(!sourceVariant.equals(variant(path,sha)))throw new IOException("PC剧本变体与路径/SHA不一致");
            if(sharedSha==null||!FORMAT_0.matcher(sharedSha).matches()||year<1||year>9999||month<1||month>12||day!=1)throw new IOException("PC剧本日期/Shared来源无效；当前日期结构仅支持原1日开局");
            if(unknown==null||unknown.size()>128)throw new IOException("PC剧本缺口列表无效");
            Set<String> seen=new HashSet<>();for(String gap:unknown)if(gap==null||gap.isEmpty()||gap.length()>200||!seen.add(gap))throw new IOException("PC剧本缺口重复或无效");
            this.scenarioId=scenarioId;this.name=name;this.sourceVariant=sourceVariant;this.path=path;this.sha=sha;this.sharedSha=sharedSha;
            this.year=year;this.month=month;this.day=day;this.unknown=List.copyOf(unknown);
        }
    }
    public static String variant(String path,String sha)throws IOException{
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(path.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash=new StringBuilder();for(byte b:digest)hash.append(String.format(Locale.ROOT,"%02x",b&255));
            return path.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]","-").replaceAll("^-+|-+$","")+"-"+hash.substring(0,12)+"-"+sha;
        }catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
    /** The only creation entry; existing worlds cannot be relabeled as PC sources. */
    public static World create(int width,int height,String[] factions,Source source)throws IOException{
        if(source==null)throw new IOException("PC来源缺失");
        World w=World.pcOpening(width,height,factions);
        w.scenarioId=source.scenarioId;w.scenarioName=source.name;w.dataSource=DATA_SOURCE;w.dataHash=source.sha;
        w.startYear=source.year;w.startMonth=source.month;w.dataRevision=1;
        w.extensions.put(NAMESPACE,encode(source));return w;
    }
    private static byte[] encode(Source s)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeUTF(EXE_SHA);out.writeUTF(s.scenarioId);out.writeUTF(s.name);out.writeUTF(s.sourceVariant);
        out.writeUTF(s.path);out.writeUTF(s.sha);out.writeUTF(s.sharedSha);out.writeInt(s.year);out.writeInt(s.month);out.writeInt(s.day);
        out.writeInt(s.unknown.size());for(String gap:s.unknown)out.writeUTF(gap);return bytes.toByteArray();
    }
    public static Source saved(World w)throws IOException{
        // Preserve unknown legacy extension bytes, even a colliding name.
        // Only an explicit factory or header38 opts into this format.
        if(!w.pcSourceFrame)return null;
        byte[] bytes=w.extensions.get(NAMESPACE);if(bytes==null)return null;
        if(bytes.length>32768)throw new IOException("PC来源段过大");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=MAGIC||!EXE_SHA.equals(in.readUTF()))throw new IOException("PC来源段标记/原EXE无效");
        String id=in.readUTF(),name=in.readUTF(),variant=in.readUTF(),path=in.readUTF(),sha=in.readUTF(),shared=in.readUTF();
        int year=in.readInt(),month=in.readInt(),day=in.readInt(),n=in.readInt();if(n<0||n>128)throw new IOException("PC来源缺口条数无效");
        List<String> unknown=new ArrayList<>();for(int i=0;i<n;i++)unknown.add(in.readUTF());
        if(in.available()!=0)throw new IOException("PC来源段未知尾部");
        Source s=new Source(id,name,variant,path,sha,shared,year,month,day,unknown);
        if(!DATA_SOURCE.equals(w.dataSource)||!s.scenarioId.equals(w.scenarioId)||!s.sha.equals(w.dataHash)||s.year!=w.startYear||s.month!=w.startMonth)throw new IOException("PC来源段与保存世界不一致");
        return s;
    }
    static void validate(World w)throws IOException{
        Source source=saved(w);
        if(source==null){if(w.pcSourceFrame||w.factions.length>32)throw new IOException("PC扩展势力须有明确保存的来源身份");}
        else {
            if(w.factions.length>MAX_FACTIONS)throw new IOException("PC势力数超出原47槽位");
            if(!w.officerAbilities.enabled()||!w.merchantMarket.enabled()||!w.pcProduction.enabled()||!w.pcTechniquePoints.enabled())throw new IOException("PC来源新局须完整初始化策略；不提升或追填旧存档策略");
        }
    }
}
