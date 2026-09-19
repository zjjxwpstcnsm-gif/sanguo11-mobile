package game.sanguo.core;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Immutable, source-pinned definitions. Never read by SaveCodec and never a second game state. */
public final class ContentCatalog {
    public static final class Profile {
        public final int officer,bloodline,affinity,honor;
        public final boolean naturalDeath;
        Profile(String[] c)throws IOException {
            officer=integer(c[0],0,1000000);bloodline=integer(c[1],0,1000000);affinity=integer(c[2],0,149);
            require(c[3].equals("O")||c[3].equals("X"),"自然死标记错误");naturalDeath=c[3].equals("O");honor=integer(c[4],1,5);
        }
    }
    public static final class Relation {
        public final int officer,target;
        public final Relations.Kind kind;
        public final String field,raw;
        Relation(String[] c)throws IOException {
            officer=integer(c[0],0,1000000);target=integer(c[1],0,1000000);
            try{kind=Relations.Kind.valueOf(c[2]);}catch(IllegalArgumentException e){throw new IOException("未知资料关系",e);}
            field=c[3];raw=c[4];require(officer!=target,"资料关系不能自指");
        }
    }
    public static final class Entry {
        public final String id,name;
        public final List<String> fields;
        Entry(String[] cells){id=cells[0];name=cells[1];fields=Collections.unmodifiableList(Arrays.asList(cells.clone()));}
    }
    public static final class Officer {
        public final int id,sourceId,birth,death,appearance;
        public final String name,skillId,status,source,relationsRaw,personality,gender;
        private final int[] stats,aptitudes;
        Officer(String[] c)throws IOException {
            id=integer(c[0],0,1000000);sourceId=integer(c[1],0,669);name=c[2];
            String[] values=c[3].split(",",-1);require(values.length==5,"能力列数错误");stats=new int[5];
            for(int i=0;i<5;i++)stats[i]=integer(values[i],0,100);
            require(c[4].length()==6,"适性列数错误");aptitudes=new int[6];
            for(int i=0;i<6;i++){aptitudes[i]="CBAS".indexOf(c[4].charAt(i));require(aptitudes[i]>=0,"未知适性");}
            birth=integer(c[5],1,9999);death=integer(c[6],birth,9999);appearance=integer(c[7],birth,9999);
            skillId=c[8];status=c[9];source=c[10];relationsRaw=c[11];personality=c[12];gender=c[13];
            require(status.equals("collected")||status.equals("cross-checked"),"未知核验状态");
        }
        public int stat(int index){return stats[index];}
        public int aptitude(int index){return aptitudes[index];}
        public String aptitudeText(){StringBuilder s=new StringBuilder();for(int a:aptitudes)s.append("CBAS".charAt(a));return s.toString();}
    }
    private static ContentCatalog cached;
    private final List<Officer> officers;
    private final Map<Integer,Officer> officerById;
    private final Map<String,List<Entry>> tables;
    private final Map<Integer,String> aliases;
    private final Map<Integer,Profile> profiles;
    private final List<Relation> relations;
    public final String target;
    public final int revision;
    private ContentCatalog()throws IOException {
        Map<String,String> index=new LinkedHashMap<>();
        for(String l:new String(bytes("index.txt"),StandardCharsets.UTF_8).split("\n")){
            if(l.isEmpty()||l.startsWith("#"))continue;
            String[] p=l.split(" ",-1);require(p.length==2&&p[0].matches("[a-z]+\\.tsv")&&p[1].matches("[0-9a-f]{64}")&&index.put(p[0],p[1])==null,"内容索引无效");
        }
        require(index.keySet().equals(new HashSet<>(Arrays.asList("officers.tsv","sites.tsv","skills.tsv","items.tsv","scenarios.tsv","sources.tsv","manifest.tsv","aliases.tsv","profiles.tsv","relations.tsv"))),"未知/缺失内容资源");
        List<String[]> manifest=read(index,"manifest.tsv","format\trevision\ttarget\toriginalInstallationVerified");
        require(manifest.size()==1&&manifest.get(0)[0].equals("1")&&manifest.get(0)[3].equals("false"),"未知内容版本/核验声明");
        revision=integer(manifest.get(0)[1],1,1000000);target=manifest.get(0)[2];
        Map<String,List<Entry>> loaded=new LinkedHashMap<>();
        loaded.put("sources",entries(read(index,"sources.tsv","id\turl\tversion\tstatus")));
        loaded.put("skills",entries(read(index,"skills.tsv","id\tname\tsource")));
        loaded.put("sites",entries(read(index,"sites.tsv","id\tname\tkind\trawX\trawY\tdurability\tcoordinateStatus\tsource")));
        loaded.put("items",entries(read(index,"items.tsv","id\tname\tkind\tvalue\tholderRaw\tlocationRaw\tscenarioId\tsource")));
        loaded.put("scenarios",entries(read(index,"scenarios.tsv","id\tname\tyear\tmonth\tkind\tstatus\tsource")));
        tables=Collections.unmodifiableMap(loaded);
        Set<String> sources=keys(rows("sources")),skills=keys(rows("skills"));
        for(Entry s:rows("sources"))require(s.name.startsWith("https://")&&!s.fields.get(2).isEmpty(),"来源不完整");
        for(String k:Arrays.asList("skills","sites","items","scenarios"))for(Entry e:rows(k))require(sources.contains(e.fields.get(e.fields.size()-1)),"来源外键错误");
        Set<String> coordinates=new HashSet<>();int cities=0,gates=0,ports=0;
        for(Entry s:rows("sites")){
            List<String> f=s.fields;integer(s.id,0,1000000);integer(f.get(3),0,199);integer(f.get(4),0,199);integer(f.get(5),1,100000);
            require(Arrays.asList("collected","unknown","cross-checked").contains(f.get(6)),"坐标状态错误");
            require(coordinates.add(f.get(3)+","+f.get(4)),"原始据点重叠");
            switch(f.get(2)){case "city":cities++;break;case "gate":gates++;break;case "port":ports++;break;default:throw new IOException("未知据点类型");}
        }
        require(cities==42&&gates==10&&ports==35&&rows("skills").size()==100&&rows("items").size()==43,"内容数量差异");
        for(Entry s:rows("scenarios")){integer(s.fields.get(2),1,9999);integer(s.fields.get(3),1,12);require(Arrays.asList("historical","fictional").contains(s.fields.get(4))&&s.fields.get(5).equals("incomplete"),"未核验开局不可启用");}
        for(Entry s:rows("items")){integer(s.fields.get(3),0,100);require(s.fields.get(6).equals("?"),"宝物原表未指定剧本");}
        List<Officer> list=new ArrayList<>();Map<Integer,Officer> byId=new LinkedHashMap<>();Set<Integer> originalIds=new HashSet<>();
        for(String[] c:read(index,"officers.tsv","id\tsourceId\tname\tstats\taptitudes\tbirth\tdeath\tappearance\tskillId\tstatus\tsource\trelationsRaw\tpersonality\tgender")){
            Officer o=new Officer(c);require(byId.put(o.id,o)==null&&originalIds.add(o.sourceId),"重复武将ID");
            require(sources.contains(o.source)&&(o.skillId.equals("none")||skills.contains(o.skillId)),"武将来源/特技外键错误");list.add(o);
        }
        require(list.size()==670,"武将数量差异");officers=Collections.unmodifiableList(list);officerById=Collections.unmodifiableMap(byId);
        Map<Integer,String> names=new HashMap<>();for(String[] c:read(index,"aliases.tsv","id\talias")){int id=integer(c[0],0,1000000);require(byId.containsKey(id)&&names.put(id,c[1])==null,"别名ID错误");}aliases=Collections.unmodifiableMap(names);
        Map<Integer,Profile> details=new LinkedHashMap<>();
        for(String[] c:read(index,"profiles.tsv","officerId\tbloodline\taffinity\tnaturalDeath\thonor\tsource")){
            Profile p=new Profile(c);require(byId.containsKey(p.officer)&&details.put(p.officer,p)==null&&sources.contains(c[5]),"人物资料外键/重复错误");
        }
        require(details.size()==officers.size(),"人物资料不完整");profiles=Collections.unmodifiableMap(details);
        List<Relation> links=new ArrayList<>();Set<String> linkIds=new HashSet<>();
        for(String[] c:read(index,"relations.tsv","officerId\ttargetId\tkind\tfield\traw\tsource")){
            Relation r=new Relation(c);require(byId.containsKey(r.officer)&&byId.containsKey(r.target)&&sources.contains(c[5]),"关系外键缺失");
            require(byId.get(r.target).name.equals(r.raw)&&linkIds.add(r.officer+":"+r.target+":"+r.kind),"关系映射或重复错误");links.add(r);
        }
        relations=Collections.unmodifiableList(links);
    }
    public static synchronized ContentCatalog get()throws IOException {if(cached==null)cached=new ContentCatalog();return cached;}
    public List<Officer> officers(){return officers;}
    public Officer officer(int id){return officerById.get(id);}
    public Profile profile(int id){return profiles.get(id);}
    public List<Relation> relations(){return relations;}
    public List<Entry> rows(String table){List<Entry> r=tables.get(table);return r==null?Collections.emptyList():r;}
    public String alias(int id){return aliases.getOrDefault(id,"");}
    public String skillName(String id){if(id.equals("none"))return "原表无特技";for(Entry e:rows("skills"))if(e.id.equals(id))return e.name;return "未知";}
    public String sourceUrl(String id){for(Entry e:rows("sources"))if(e.id.equals(id))return e.name;return "未知来源";}
    /** Match only an explicit stable ID + approved alias bridge, never a same-name heuristic. */
    public void validateOpening(World w)throws IOException {
        for(World.Officer o:w.officers){Officer d=officer(o.id);require(d!=null&&(o.name.equals(d.name)||o.name.equals(alias(o.id))),"开局人物未在来源映射中");
            int[] stats={o.leadership,o.war,o.intelligence,o.politics,o.charm};for(int i=0;i<5;i++)require(stats[i]==d.stat(i),"开局能力与资料不同");for(int i=0;i<6;i++)require(o.aptitude[i]==d.aptitude(i),"开局适性与资料不同");}
    }
    private static Set<String> keys(List<Entry> rows){Set<String> ids=new HashSet<>();for(Entry e:rows)ids.add(e.id);return ids;}
    private static List<Entry> entries(List<String[]> rows)throws IOException {List<Entry> out=new ArrayList<>();Set<String> ids=new HashSet<>();for(String[] c:rows){require(ids.add(c[0]),"重复内容ID");out.add(new Entry(c));}return Collections.unmodifiableList(out);}
    private static List<String[]> read(Map<String,String> index,String name,String header)throws IOException {
        byte[] raw=bytes(name);require(digest(raw).equals(index.get(name)),"内容校验失败："+name);
        String text;try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(raw)).toString();}catch(CharacterCodingException e){throw new IOException("内容不是 UTF-8",e);}
        String[] lines=text.split("\n");require(lines.length>0&&lines[0].equals(header),"未知/重复列："+name);int count=header.split("\t").length;
        List<String[]> out=new ArrayList<>();for(int i=1;i<lines.length;i++){String[] c=lines[i].split("\t",-1);require(c.length==count,"内容列数错误");for(String s:c)require(!s.isEmpty(),"缺失内容字段");out.add(c);}return out;
    }
    private static byte[] bytes(String name)throws IOException {
        try(InputStream in=ContentCatalog.class.getResourceAsStream("/content/"+name)){
            require(in!=null,"内容资源缺失");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){require(out.size()+n<=2*1024*1024,"内容资源过大");out.write(buf,0,n);}return out.toByteArray();}
    }
    private static String digest(byte[] bytes)throws IOException {try{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    private static int integer(String s,int lo,int hi)throws IOException {try{int v=Integer.parseInt(s);require(v>=lo&&v<=hi,"内容数值越界");return v;}catch(NumberFormatException e){throw new IOException("内容数值无效",e);}}
    private static void require(boolean value,String message)throws IOException {if(!value)throw new IOException(message);}
}
