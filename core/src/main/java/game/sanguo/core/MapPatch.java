package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static game.sanguo.core.MapJson.*;

/** Editable geographic delta. No running campaign objects or current captures enter this file.
 * Coordinates are national SOURCE coordinates, not renderer pixels or axial array indexes. */
public final class MapPatch {
    public static final int FORMAT=1;
    public record Cell(int x,int y,World.Terrain before,World.Terrain after) {
        public int key(){return x*200+y;}
        Object json(){return obj("x",x,"y",y,"before",before.name(),"after",after.name());}
    }
    public record Site(int id,String name,int x,int y,World.SiteKind kind,int parent,WorldEvents.Tribe region) {
        public Hex hex(World w){return MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));}
        public Site at(int nx,int ny){return new Site(id,name,nx,ny,kind,parent,region);}
        public Site parent(int city){return new Site(id,name,x,y,kind,city,region);}
        public Site renamed(String value){return new Site(id,value,x,y,kind,parent,region);}
        public Site copy(int newId){return new Site(newId,name+"副本",x,y,kind,parent,region);}
        Object json(){return obj("id",id,"name",name,"x",x,"y",y,"kind",kind.name(),"parent",parent,"region",region.name());}
    }
    public record SiteChange(Site before,Site after) {public int id(){return before==null?after.id():before.id();}Object json(){return obj("before",before==null?null:before.json(),"after",after==null?null:after.json());}}
    public record Initial(boolean enabled,int owner,int gold,int food,int troops,int order,int morale,int defense) {
        public static Initial neutral(World.SiteKind kind){return new Initial(true,-1,1000,10000,0,90,70,kind==World.SiteKind.CITY?3000:kind==World.SiteKind.GATE?5000:2000);}
        public static Initial of(World.City c){return new Initial(true,c.owner,c.gold,c.food,c.troops,c.order,c.morale,c.defense);}
        public Initial enabled(boolean value){return new Initial(value,owner,gold,food,troops,order,morale,defense);}
        public void apply(World.City c){c.owner=owner;c.gold=gold;c.food=food;c.troops=troops;c.order=order;c.morale=morale;c.defense=defense;}
        Object json(){return obj("enabled",enabled,"owner",owner,"gold",gold,"food",food,"troops",troops,"order",order,"morale",morale,"defense",defense);}
    }
    public String id,name,base,preview="heroes-250";
    public int revision=1;
    public final SortedMap<Integer,Cell> terrain=new TreeMap<>();
    public final SortedMap<Integer,SiteChange> sites=new TreeMap<>();
    public final SortedMap<String,String> scenarioBases=new TreeMap<>();
    public final SortedMap<String,SortedMap<Integer,Initial>> scenarios=new TreeMap<>();
    /** Explicit per-scenario reference migration, never inferred at load time. */
    public final SortedMap<String,SortedMap<Integer,Integer>> redirects=new TreeMap<>();
    public MapPatch(String name,String base){this.id=UUID.randomUUID().toString();this.name=name;this.base=base;}
    public MapPatch copy(){MapPatch p=new MapPatch(name,base);p.id=id;p.revision=revision;p.preview=preview;p.terrain.putAll(terrain);p.sites.putAll(sites);p.scenarioBases.putAll(scenarioBases);for(var e:scenarios.entrySet())p.scenarios.put(e.getKey(),new TreeMap<>(e.getValue()));for(var e:redirects.entrySet())p.redirects.put(e.getKey(),new TreeMap<>(e.getValue()));return p;}
    public void putSite(Site original,Site value){int key=original==null?value.id():original.id();if(Objects.equals(original,value))sites.remove(key);else sites.put(key,new SiteChange(original,value));}
    public Initial initial(String scenario,int site){return scenarios.getOrDefault(scenario,Collections.emptySortedMap()).get(site);}
    public Integer redirect(String scenario,int site){return redirects.getOrDefault(scenario,Collections.emptySortedMap()).get(site);}
    public String summary(){int added=0,moved=0,removed=0,updated=0;for(SiteChange c:sites.values()){if(c.before==null)added++;else if(c.after==null)removed++;else if(c.before.x!=c.after.x||c.before.y!=c.after.y)moved++;else updated++;}return "地形 "+terrain.size()+" 格 · 新增 "+added+" · 移动 "+moved+" · 修改 "+updated+" · 删除 "+removed+" 据点\n剧本覆盖 "+scenarios.keySet()+" · 引用迁移 "+redirects.keySet();}
    public byte[] encode(){List<Object> cells=new ArrayList<>(),entities=new ArrayList<>(),states=new ArrayList<>(),refs=new ArrayList<>();for(Cell c:terrain.values())cells.add(c.json());for(SiteChange c:sites.values())entities.add(c.json());for(var e:scenarios.entrySet())for(var x:e.getValue().entrySet())states.add(obj("scenario",e.getKey(),"id",x.getKey(),"state",x.getValue().json()));for(var e:redirects.entrySet())for(var x:e.getValue().entrySet())refs.add(obj("scenario",e.getKey(),"from",x.getKey(),"to",x.getValue()));return bytes(obj("format",FORMAT,"baseFingerprint",base,"mapId",id,"name",name,"revision",revision,"previewScenario",preview,"scenarioFingerprints",new TreeMap<>(scenarioBases),"terrain",cells,"sites",entities,"initialStates",states,"redirects",refs));}
    public static MapPatch read(InputStream in)throws IOException {return decodeObject(MapJson.read(in));}
    public static MapPatch decode(byte[] bytes)throws IOException{return decodeObject(MapJson.parse(bytes));}
    private static MapPatch decodeObject(Object value)throws IOException {
        try{
            Map<String,Object> m=object(value);keys(m,"format","baseFingerprint","mapId","name","revision","previewScenario","scenarioFingerprints","terrain","sites","initialStates","redirects");integer(m.get("format"),FORMAT,FORMAT);
            MapPatch p=new MapPatch(string(m.get("name"),60),fingerprint(m.get("baseFingerprint")));p.id=string(m.get("mapId"),36);if(!p.id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IOException("地图标识必须是规范UUID");p.revision=integer(m.get("revision"),1,1000000);p.preview=scenario(m.get("previewScenario"));
            Map<String,Object> bases=object(m.get("scenarioFingerprints"));if(bases.size()>100)throw new IOException("剧本数量过多");for(var e:bases.entrySet())p.scenarioBases.put(scenario(e.getKey()),fingerprint(e.getValue()));
            for(Object item:array(m.get("terrain"))){Map<String,Object> c=object(item);keys(c,"x","y","before","after");Cell cell=new Cell(integer(c.get("x"),0,199),integer(c.get("y"),0,199),World.Terrain.valueOf(string(c.get("before"),30)),World.Terrain.valueOf(string(c.get("after"),30)));if(cell.before==cell.after||cell.before==World.Terrain.VOID||cell.after==World.Terrain.VOID||p.terrain.put(cell.key(),cell)!=null)throw new IOException("地形增量重复、无效或试图改变边界");}
            if(p.terrain.size()>40000)throw new IOException("地形增量过多");
            for(Object item:array(m.get("sites"))){Map<String,Object> c=object(item);keys(c,"before","after");Site a=site(c.get("before")),b=site(c.get("after"));if(a==null&&b==null||a!=null&&b!=null&&a.id!=b.id||Objects.equals(a,b))throw new IOException("据点增量ID不一致或为空");SiteChange change=new SiteChange(a,b);if(p.sites.put(change.id(),change)!=null)throw new IOException("据点ID重复："+change.id());}
            if(p.sites.size()>1000)throw new IOException("据点增量过多");
            for(Object item:array(m.get("initialStates"))){Map<String,Object> c=object(item);keys(c,"scenario","id","state");String s=scenario(c.get("scenario"));int id=integer(c.get("id"),0,Integer.MAX_VALUE);Initial state=state(c.get("state"));if(p.scenarios.computeIfAbsent(s,k->new TreeMap<>()).put(id,state)!=null)throw new IOException("剧本据点状态重复");}
            for(Object item:array(m.get("redirects"))){Map<String,Object> c=object(item);keys(c,"scenario","from","to");String s=scenario(c.get("scenario"));int from=integer(c.get("from"),0,Integer.MAX_VALUE),to=integer(c.get("to"),0,Integer.MAX_VALUE);if(from==to||p.redirects.computeIfAbsent(s,k->new TreeMap<>()).put(from,to)!=null)throw new IOException("引用迁移重复或循环");}
            for(String s:p.scenarios.keySet())if(!p.scenarioBases.containsKey(s))throw new IOException("剧本状态缺少基线指纹："+s);for(String s:p.redirects.keySet())if(!p.scenarioBases.containsKey(s))throw new IOException("引用迁移缺少剧本指纹："+s);return p;
        }catch(IllegalArgumentException e){throw new IOException("地图枚举或数值不合法："+e.getMessage(),e);}
    }
    private static Site site(Object value)throws IOException {if(value==null)return null;Map<String,Object> m=object(value);keys(m,"id","name","x","y","kind","parent","region");Site s=new Site(integer(m.get("id"),0,Integer.MAX_VALUE),string(m.get("name"),60),integer(m.get("x"),0,199),integer(m.get("y"),0,199),World.SiteKind.valueOf(string(m.get("kind"),12)),integer(m.get("parent"),-1,Integer.MAX_VALUE),WorldEvents.Tribe.valueOf(string(m.get("region"),12)));if(s.name.contains("|")||!s.name.equals(s.name.trim())||s.name.trim().isEmpty())throw new IOException("据点名称不能含分隔符或首尾空白");if(s.kind==World.SiteKind.CITY&&s.parent!=-1||s.kind!=World.SiteKind.CITY&&s.parent<0)throw new IOException("关联城市无效");return s;}
    private static Initial state(Object value)throws IOException {Map<String,Object> m=object(value);keys(m,"enabled","owner","gold","food","troops","order","morale","defense");return new Initial(bool(m.get("enabled")),integer(m.get("owner"),-1,31),integer(m.get("gold"),0,1000000),integer(m.get("food"),0,1000000),integer(m.get("troops"),0,100000),integer(m.get("order"),0,100),integer(m.get("morale"),0,120),integer(m.get("defense"),1,100000));}
    private static String scenario(Object v)throws IOException {String s=string(v,80);if(!s.matches("[a-z0-9-]{1,80}"))throw new IOException("剧本标识错误");return s;}
    private static String fingerprint(Object v)throws IOException {String s=string(v,64);if(!s.matches("[0-9a-f]{64}"))throw new IOException("内容指纹无效");return s;}
    public static String hash(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public String fingerprint(){return hash(encode());}
}
