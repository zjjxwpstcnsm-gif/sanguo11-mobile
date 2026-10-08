package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Initial native runtime facts, attached to explicit new source games only.
 * Initial health is provenance, never a substitute for mutable campaign health. */
public final class PcDuelSourceFacts {
    public static final String NAMESPACE="pc-duel-initial-person-facts-v1";
    private static final int MAGIC=0x50444931,MAX=128*1024;
    private static final String RESOURCE_SHA="258d1ce94630acc3e216fdeccd56a77158de17c6565baea2ce750fbab3432da6";
    public static final class Fact {
        public final int officerId,nativeId,initialPhysicalHealth,initialInjury,initialCurrentWar,initialNativeSkill,initialStatus;
        public final String recordSha;
        Fact(int id,int nativeId,String sha,int hp,int injury,int war,int skill,int status){officerId=id;this.nativeId=nativeId;recordSha=sha;initialPhysicalHealth=hp;initialInjury=injury;initialCurrentWar=war;initialNativeSkill=skill;initialStatus=status;}
    }
    private static final class Row {
        final String recordSha;final boolean validPerson;final int hp,injury,war,skill,status;
        Row(String[]v)throws IOException{recordSha=v[6];validPerson=number(v[7],0,1)!=0;number(v[8],0,1);status=number(v[9],-1,8);hp=number(v[10],0,100);injury=number(v[11],-1,3);war=number(v[12],0,255);skill=number(v[13],-1,99);}
    }
    private static final class Source {
        final String id,variant,path,sha;final Row[]rows=new Row[1100];
        Source(String[]v){id=v[1];variant=v[2];path=v[3];sha=v[4];}
    }
    private static List<Source> cached;
    private static int number(String s,int min,int max)throws IOException{try{return PcOfficerInfo.bounded(Integer.parseInt(s),min,max);}catch(NumberFormatException e){throw new IOException("原单挑人物资源字段无效",e);}}
    private static String hash(byte[]b)throws IOException{try{StringBuilder out=new StringBuilder();for(byte n:MessageDigest.getInstance("SHA-256").digest(b))out.append(String.format(Locale.ROOT,"%02x",n&255));return out.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    private static synchronized List<Source> catalog()throws IOException{
        if(cached!=null)return cached;byte[]raw;
        try(InputStream resource=PcDuelSourceFacts.class.getResourceAsStream("/pc-duel/person-state.tsv.gz")){
            if(resource==null)throw new IOException("原单挑人物资源缺失");try(InputStream in=new GZIPInputStream(resource);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[]buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>8*1024*1024)throw new IOException("原单挑人物资源过大");bytes.write(buffer,0,n);}raw=bytes.toByteArray();
            }
        }
        if(!hash(raw).equals(RESOURCE_SHA))throw new IOException("原单挑人物资源指纹变化");List<Source>out=new ArrayList<>();int count=0;
        for(String line:new String(raw,StandardCharsets.UTF_8).split("\n")){
            if(line.startsWith("#"))continue;String[]v=line.split("\t",-1);if(v.length!=14)throw new IOException("原单挑人物资源列数无效");int source=number(v[0],0,15),nativeId=number(v[5],0,1099);
            if(nativeId==0){if(source!=out.size())throw new IOException("原单挑来源顺序无效");out.add(new Source(v));}Source s=out.get(source);
            if(!s.id.equals(v[1])||!s.variant.equals(v[2])||!s.path.equals(v[3])||!s.sha.equals(v[4])||s.rows[nativeId]!=null)throw new IOException("原单挑来源身份重复或不一致");
            if(!v[6].equals("-")&&!v[6].matches("[0-9a-f]{64}"))throw new IOException("原单挑人物指纹无效");s.rows[nativeId]=new Row(v);count++;
        }
        if(count!=17600||out.size()!=16)throw new IOException("原单挑来源覆盖不足");for(Source s:out)for(Row v:s.rows)if(v==null)throw new IOException("原单挑人物槽缺失");return cached=List.copyOf(out);
    }
    static void initializeOpening(World w,PcScenarioCatalog.Source opening)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null)throw new IOException("原单挑初始事实仅能在明确新局建立");
        Source source=null;for(Source s:catalog())if(s.id.equals(opening.identity.scenarioId))source=s;
        if(source==null||!source.sha.equals(opening.identity.sha)||!source.variant.equals(opening.identity.sourceVariant)||!source.path.equals(opening.identity.path))throw new IOException("原单挑来源不一致");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);PcOfficerInfo.text(out,PcScenarioIdentity.EXE_SHA);PcOfficerInfo.text(out,source.id);PcOfficerInfo.text(out,source.sha);PcOfficerInfo.text(out,source.variant);
        List<Fact>facts=new ArrayList<>();for(PcScenarioPeople.Person p:opening.people)if(p.officerId>=0){Row v=source.rows[p.nativeId];
            if(p.nativeId>=670||!p.recordSha.equals(v.recordSha)||!v.validPerson||w.officer(p.officerId)==null)throw new IOException("原单挑人物稳定连接未核实："+p.nativeId);
            facts.add(new Fact(p.officerId,p.nativeId,p.recordSha,v.hp,PcOfficerInfo.bounded(v.injury,0,3),v.war,v.skill,PcOfficerInfo.bounded(v.status,0,8)));
        }
        out.writeInt(facts.size());for(Fact f:facts){out.writeInt(f.officerId);out.writeInt(f.nativeId);PcOfficerInfo.text(out,f.recordSha);for(int n:new int[]{f.initialPhysicalHealth,f.initialInjury,f.initialCurrentWar,f.initialNativeSkill,f.initialStatus})out.writeInt(n);}w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    /** Absence or an opaque future strategy remains inactive; no catalog read. */
    public static Map<Integer,Fact> saved(World w)throws IOException{
        byte[]raw=w.extensions.get(NAMESPACE);if(raw==null)return Collections.emptyMap();if(raw.length<4||new DataInputStream(new ByteArrayInputStream(raw)).readInt()!=MAGIC)return Collections.emptyMap();if(raw.length>MAX)throw new IOException("原单挑人物保存过大");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));in.readInt();PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(!w.pcSourceFrame||source==null||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA)||!PcOfficerInfo.text(in,80).equals(source.scenarioId)||!PcOfficerInfo.text(in,64).equals(source.sha)||!PcOfficerInfo.text(in,300).equals(source.sourceVariant))throw new IOException("已存原单挑人物来源不一致");
        Map<Integer,PcScenarioPeople.Person>people=new HashMap<>();for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w))if(p.officerId>=0)people.put(p.officerId,p);Map<Integer,Fact>out=new TreeMap<>();Set<Integer>nativeIds=new HashSet<>();int count=PcOfficerInfo.bounded(in.readInt(),670,670);
        for(int i=0;i<count;i++){int id=PcOfficerInfo.bounded(in.readInt(),0,999999),nativeId=PcOfficerInfo.bounded(in.readInt(),0,669);String sha=PcOfficerInfo.text(in,64);PcScenarioPeople.Person p=people.get(id);int hp=PcOfficerInfo.bounded(in.readInt(),0,100),injury=PcOfficerInfo.bounded(in.readInt(),0,3),war=PcOfficerInfo.bounded(in.readInt(),0,255),skill=PcOfficerInfo.bounded(in.readInt(),-1,99),status=PcOfficerInfo.bounded(in.readInt(),0,8);
            if(p==null||p.nativeId!=nativeId||!p.recordSha.equals(sha)||w.officer(id)==null||!nativeIds.add(nativeId)||out.put(id,new Fact(id,nativeId,sha,hp,injury,war,skill,status))!=null)throw new IOException("已存原单挑人物身份不一致");
        }
        if(in.available()!=0)throw new IOException("已存原单挑人物尾部未知");return Collections.unmodifiableMap(out);
    }
    static void validate(World w)throws IOException{saved(w);}
    private PcDuelSourceFacts(){}
}
