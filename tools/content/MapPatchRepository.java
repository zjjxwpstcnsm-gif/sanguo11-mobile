package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static game.sanguo.core.MapJson.*;

/** Repository adapter; validation and expected worlds use the actual Android resolver. */
public final class MapPatchRepository {
    static final String MAP="core/src/main/resources/maps/national-map-v056.properties";
    static Path root;
    static Properties read(String path)throws IOException {Properties p=new Properties();try(Reader in=Files.newBufferedReader(root.resolve(path),StandardCharsets.UTF_8)){p.load(in);}return p;}
    static String[] fields(Properties p,String key){return p.getProperty(key).split("\\|",-1);}
    static int count(Properties p,String key){return Integer.parseInt(p.getProperty(key,"0"));}
    static List<String> rows(Properties p,String count,String prefix){List<String> out=new ArrayList<>();for(int i=0;i<count(p,count);i++)out.add(p.getProperty(prefix+i));return out;}
    static void rows(Properties p,String count,String prefix,List<String> values){int old=count(p,count);for(int i=0;i<Math.max(old,values.size());i++)if(i<values.size())p.setProperty(prefix+i,values.get(i));else p.remove(prefix+i);if(old>0||!values.isEmpty()||p.containsKey(count))p.setProperty(count,""+values.size());}
    static List<Integer> ints(int[] a){List<Integer> r=new ArrayList<>();for(int n:a)r.add(n);return r;}
    static String xy(World w,Hex h){SourceGridCoord s=MapCoordinates.nationalSource(w,h);return s.x+","+s.y;}
    static String terrain(World w){StringBuilder s=new StringBuilder();for(int y=0;y<200;y++)for(int x=0;x<200;x++){Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));s.append(TerrainCode.encode(w.terrain[h.q][h.r]));}return MapPatch.hash(s.toString().getBytes(StandardCharsets.UTF_8));}
    /** Semantic opening fingerprint excludes only source identity and editor UI metadata. */
    static String fingerprint(World w){
        Map<String,Object> sites=new TreeMap<>(),officers=new TreeMap<>(),life=new TreeMap<>(),talents=new TreeMap<>(),treasures=new TreeMap<>();
        for(World.City c:w.cities){List<String> plots=new ArrayList<>();for(Hex h:w.development.parcels(c.id))plots.add(xy(w,h));Collections.sort(plots);World.City parent=SiteAffiliation.parent(w,c);
            sites.put(""+c.id,obj("name",c.name,"position",xy(w,c.hex),"kind",c.kind.name(),"region",w.events.region(c.id).name(),"parent",parent==null?-1:parent.id,"initial",MapPatch.Initial.of(c).json(),"baseDefense",c.baseDefense,"equipment",ints(c.equipment),"ships",ints(c.ships),"plots",plots));}
        for(World.Officer o:w.officers)officers.put(""+o.id,Arrays.asList(o.owner,o.cityId,o.unitId));
        for(Lifecycle.Life l:w.life.people())life.put(""+l.officer,l.home);
        for(Strategy.Talent t:w.strategy.hiddenTalents())talents.put(""+t.id,t.cityId);
        for(Treasures.Item t:w.treasures.items.values())treasures.put(t.definition.id,Arrays.asList(t.place.name(),""+t.holder));
        return MapPatch.hash(bytes(obj("terrain",terrain(w),"sites",sites,"officers",officers,"life",life,"talents",talents,"treasures",treasures)));
    }
    static void scenario(Properties p,World before,World after,MapPatch patch)throws IOException {
        Map<Integer,String[]> original=new LinkedHashMap<>();for(String row:rows(p,"cities","city.")){String[] f=row.split("\\|",-1);original.put(Integer.parseInt(f[0]),f);}
        List<Integer> order=new ArrayList<>();for(int id:original.keySet())if(after.city(id)!=null)order.add(id);for(World.City c:after.cities)if(!original.containsKey(c.id))order.add(c.id);
        List<String> cities=new ArrayList<>();for(int id:order){World.City c=after.city(id);String[] f=original.containsKey(id)?original.get(id).clone():new String[15];SourceGridCoord position=MapCoordinates.nationalSource(after,c.hex);
            f[0]=""+id;f[1]=c.name;if(!original.containsKey(id)){f[2]=""+position.x;f[3]=""+position.y;}
            int[] state={c.owner,c.gold,c.food,c.troops,c.order,c.morale,c.defense,c.equipment[0],c.equipment[1],c.equipment[2],c.equipment[3]};for(int i=0;i<state.length;i++)f[i+4]=""+state[i];cities.add(String.join("|",f));}
        rows(p,"cities","city.",cities);
        List<String> kinds=new ArrayList<>();Set<Integer> kindsSeen=new HashSet<>();for(String row:rows(p,"site-kinds","site-kind.")){int id=Integer.parseInt(row.split("\\|",-1)[0]);World.City c=after.city(id);if(c!=null){kinds.add(id+"|"+c.kind.name()+"|"+c.baseDefense);kindsSeen.add(id);}}
        for(int id:order){World.City c=after.city(id);if(!kindsSeen.contains(id)&&(c.kind!=World.SiteKind.CITY||c.baseDefense!=Math.max(3000,c.defense)))kinds.add(id+"|"+c.kind.name()+"|"+c.baseDefense);}rows(p,"site-kinds","site-kind.",kinds);
        List<String> arsenal=new ArrayList<>();Set<Integer> arsenalSeen=new HashSet<>();for(String row:rows(p,"arsenals","arsenal.")){int id=Integer.parseInt(row.split("\\|",-1)[0]);if(after.city(id)!=null){arsenal.add(row);arsenalSeen.add(id);}}
        for(int id:order)if(!original.containsKey(id)&&!arsenalSeen.contains(id)){World.City c=after.city(id);arsenal.add(id+"|"+c.equipment[5]+"|"+c.equipment[6]+"|"+c.equipment[7]+"|"+c.equipment[8]+"|"+c.ships[0]+"|"+c.ships[1]);}rows(p,"arsenals","arsenal.",arsenal);
        List<String> regions=new ArrayList<>();Set<Integer> regionSeen=new HashSet<>();for(String row:rows(p,"regions","region.")){int id=Integer.parseInt(row.split("\\|",-1)[0]);if(after.city(id)!=null){regions.add(id+"|"+after.events.region(id).name());regionSeen.add(id);}}
        for(int id:order)if(!regionSeen.contains(id)&&after.events.region(id)!=WorldEvents.Tribe.BANDIT)regions.add(id+"|"+after.events.region(id).name());rows(p,"regions","region.",regions);
        for(int i=0;i<count(p,"officers");i++){String key="officer."+i;String[] f=fields(p,key);World.Officer o=after.officer(Integer.parseInt(f[0]));World.Officer old=before.officer(o.id);if(o.cityId!=old.cityId){f[3]=""+o.cityId;p.setProperty(key,String.join("|",f));}}
        for(int i=0;i<count(p,"lifetimes");i++){String key="lifetime."+i;String[] f=fields(p,key);Lifecycle.Life l=after.life.people.get(Integer.parseInt(f[0]));f[4]=""+l.home;p.setProperty(key,String.join("|",f));}
        for(int i=0;i<count(p,"talents");i++){String key="talent."+i;String[] f=fields(p,key);int id=Integer.parseInt(f[0]);for(Strategy.Talent t:after.strategy.hiddenTalents())if(t.id==id){f[2]=""+t.cityId;p.setProperty(key,String.join("|",f));}}
        for(int i=0;i<count(p,"treasures");i++){String key="treasure."+i;String[] f=fields(p,key);Treasures.Item t=after.treasures.item(f[0]);if(t.place==Treasures.Place.HIDDEN){f[2]=""+t.holder;p.setProperty(key,String.join("|",f));}}
        // Coordinate-dependent initial objects need explicit specialized migration, never guesses.
        for(var change:patch.sites.values())if(change.before()!=null&&(change.after()==null||change.before().x()!=change.after().x()||change.before().y()!=change.after().y())){
            for(WorldEvents.Camp c:before.events.camps())if(c.city==change.id())throw new IOException("仓库合并需人工迁移初始营寨："+before.scenarioId+" #"+c.id);
            for(Domestic.Facility f:before.domestic.facilities)if(f.cityId==change.id())throw new IOException("仓库合并需人工迁移初始设施："+before.scenarioId+" #"+f.id);
        }
    }
    static Object delta(String path,Properties old,Properties next)throws IOException {Map<String,Object> changes=new TreeMap<>();Set<String> keys=new TreeSet<>(old.stringPropertyNames());keys.addAll(next.stringPropertyNames());for(String key:keys)if(!Objects.equals(old.getProperty(key),next.getProperty(key)))changes.put(key,next.getProperty(key));return obj("beforeSha256",MapPatch.hash(Files.readAllBytes(root.resolve(path))),"properties",changes);}
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IOException("Usage: MapPatchRepository plan|verify PATCH_OR_PLAN REPOSITORY OUTPUT_JSON");root=Path.of(args[2]);Map<String,Object> result=new LinkedHashMap<>();
        if(args[0].equals("verify")){
            Map<String,Object> plan=object(MapJson.parse(Files.readAllBytes(Path.of(args[1]))));Map<String,Object> expected=object(plan.get("expected"));
            for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries()){World w=ScenarioCatalog.load(row.id,0);if(expected.containsKey(row.id)&&!fingerprint(w).equals(expected.get(row.id)))throw new IOException("正式资源与手机Patch解析结果不一致："+row.id);}
            result.put("verified",true);result.put("scenarios",new ArrayList<>(expected.keySet()));Files.write(Path.of(args[3]),bytes(result));return;
        }
        if(!args[0].equals("plan"))throw new IOException("Unknown command");MapPatch patch=MapPatch.decode(Files.readAllBytes(Path.of(args[1])));CustomMaps.verifyBase(patch);
        List<Object> issues=new ArrayList<>();for(CustomMaps.Issue i:CustomMaps.validateAll(patch)){issues.add(obj("blocking",i.blocking(),"scenario",i.scenario(),"message",i.message(),"site",i.site()));if(i.blocking())throw new IOException(i.scenario()+"："+i.message());}
        Properties geo=read(MAP),oldGeo=(Properties)geo.clone();for(MapPatch.Cell c:patch.terrain.values()){String key="terrain."+c.y();char[] row=geo.getProperty(key).toCharArray();row[c.x()]=TerrainCode.encode(c.after());geo.setProperty(key,new String(row));}
        World canonical=CustomMaps.preview(patch,"heroes-250");for(MapPatch.SiteChange c:patch.sites.values())if(c.after()==null){geo.remove("site."+c.id());geo.remove("plots."+c.id());geo.remove("parent."+c.id());}else{MapPatch.Site s=c.after();geo.setProperty("site."+s.id(),s.x()+","+s.y());if(s.kind()!=World.SiteKind.CITY)geo.setProperty("parent."+s.id(),""+s.parent());
            if(c.before()!=null&&(c.before().x()!=s.x()||c.before().y()!=s.y())&&canonical.development.configured(s.id())){List<String> plots=new ArrayList<>();for(Hex h:canonical.development.parcels(s.id()))plots.add(xy(canonical,h));geo.setProperty("plots."+s.id(),String.join(";",plots));}}
        Map<String,Object> files=new TreeMap<>(),expected=new TreeMap<>();files.put(MAP,delta(MAP,oldGeo,geo));
        for(String id:CustomMaps.base().scenarioHashes.keySet()){String path="core/src/main/resources/scenarios/"+id+".properties";Properties p=read(path),old=(Properties)p.clone();World original=ScenarioCatalog.load(id,0),resolved=CustomMaps.preview(patch,id);scenario(p,original,resolved,patch);files.put(path,delta(path,old,p));expected.put(id,fingerprint(resolved));}
        // Cropped scenarios are not auto-expanded; post-merge loading checks original membership.
        result.put("format",1);result.put("mapId",patch.id);result.put("revision",patch.revision);result.put("patchFingerprint",patch.fingerprint());result.put("baseFingerprint",patch.base);result.put("summary",patch.summary());result.put("issues",issues);result.put("files",files);result.put("expected",expected);Files.write(Path.of(args[3]),bytes(result));
    }
}
