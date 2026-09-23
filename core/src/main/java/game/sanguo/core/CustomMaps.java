package game.sanguo.core;
import game.sanguo.core.map.SourceGridCoord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One production resolver for editor preview, new campaigns, validation and repository tools.
 * The resolver always starts with an independently loaded opening, never the current campaign. */
public final class CustomMaps {
    private CustomMaps(){}
    public record Issue(boolean blocking,String scenario,String message,Hex hex,Integer site) {}
    public static final class Base {
        public final String fingerprint;
        public final SortedMap<Integer,MapPatch.Site> sites;
        public final SortedMap<String,String> scenarioHashes;
        private final World world;
        private Base(World world,SortedMap<Integer,MapPatch.Site> sites,SortedMap<String,String> hashes){this.world=world;this.sites=Collections.unmodifiableSortedMap(sites);scenarioHashes=Collections.unmodifiableSortedMap(hashes);
            StringBuilder b=new StringBuilder(NationalMap.SHA256).append('\n');for(MapPatch.Site s:sites.values())b.append(new String(MapJson.bytes(s.json()),StandardCharsets.UTF_8));b.append(Arrays.deepToString(MapWater.ANCHORS));fingerprint=MapPatch.hash(b.toString().getBytes(StandardCharsets.UTF_8));}
        public boolean restricted(int x,int y){return NationalMap.restricted(world,MapCoordinates.fromNationalSource(world,new SourceGridCoord(x,y)));}
        public World.Terrain terrain(int x,int y){Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(x,y));return world.sourceInside(h)?world.terrain[h.q][h.r]:World.Terrain.VOID;}
        public MapPatch fresh(){MapPatch p=new MapPatch("自定义全国地图",fingerprint);p.scenarioBases.putAll(scenarioHashes);return p;}
    }
    private static Base cached;
    public static synchronized Base base()throws IOException {
        if(cached!=null)return cached;World canonical=ScenarioCatalog.load("heroes-250",0);SortedMap<Integer,MapPatch.Site> sites=new TreeMap<>();for(World.City c:canonical.cities)sites.put(c.id,definition(canonical,c));SortedMap<String,String> hashes=new TreeMap<>();
        for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries()){World w=row.id.equals("heroes-250")?canonical:ScenarioCatalog.load(row.id,0);if(compatible(w))hashes.put(row.id,w.dataHash);}return cached=new Base(canonical,sites,hashes);
    }
    public static boolean compatible(World w){return NationalMap.ID.equals(w.mapId)&&w.columnStaggered&&w.sourceColumns()==200&&w.sourceRows()==200&&w.sourceOriginX==0&&w.sourceOriginY==0;}
    public static MapPatch.Site definition(World w,World.City c){SourceGridCoord s=MapCoordinates.nationalSource(w,c.hex);World.City parent=SiteAffiliation.parent(w,c);return new MapPatch.Site(c.id,c.name,s.x,s.y,c.kind,c.kind==World.SiteKind.CITY?-1:parent==null?-1:parent.id,w.events.region(c.id));}
    public static SortedMap<Integer,MapPatch.Site> sites(MapPatch patch)throws IOException {SortedMap<Integer,MapPatch.Site> out=new TreeMap<>(base().sites);for(MapPatch.SiteChange c:patch.sites.values())if(c.after()==null)out.remove(c.id());else out.put(c.id(),c.after());return out;}
    public static void verifyBase(MapPatch p)throws IOException {
        Base b=base();if(!b.fingerprint.equals(p.base))throw new IOException("基础地图内容指纹不匹配；没有覆盖或部分应用任何数据");
        if(!b.scenarioHashes.containsKey(p.preview))throw new IOException("预览剧本不支持200×200地图");
        for(var e:p.scenarioBases.entrySet())if(!Objects.equals(b.scenarioHashes.get(e.getKey()),e.getValue()))throw new IOException("剧本内容基线冲突："+e.getKey());
        for(MapPatch.Cell c:p.terrain.values())if(b.restricted(c.x(),c.y())||b.terrain(c.x(),c.y())!=c.before())throw new IOException("地形前置值冲突："+c.x()+","+c.y());
        for(MapPatch.SiteChange c:p.sites.values())if(!Objects.equals(b.sites.get(c.id()),c.before()))throw new IOException("据点前置值或ID冲突："+c.id());
        SortedMap<Integer,MapPatch.Site> geo=sites(p);for(int id:p.appearances.keySet())if(!geo.containsKey(id))throw new IOException("视觉元数据引用不存在的据点 #"+id);for(int cell:p.heights.keySet())if(b.restricted(cell/200,cell%200)||b.terrain(cell/200,cell%200)==World.Terrain.VOID)throw new IOException("不能给地图边界设置高度");if(geo.size()>1000||geo.isEmpty())throw new IOException("有效据点数量必须在1—1000之间");
        for(MapPatch.Site s:geo.values())if(s.kind()!=World.SiteKind.CITY){MapPatch.Site parent=geo.get(s.parent());if(parent==null||parent.kind()!=World.SiteKind.CITY)throw new IOException(s.name()+"的关联城市无效");}
        for(var e:p.scenarios.entrySet()){if(!b.scenarioHashes.containsKey(e.getKey()))throw new IOException("未知剧本覆盖");for(int id:e.getValue().keySet())if(!geo.containsKey(id))throw new IOException("剧本覆盖引用已删除据点："+id);}
        for(var e:p.redirects.entrySet())for(var r:e.getValue().entrySet())if(!geo.containsKey(r.getValue())||r.getKey().equals(r.getValue()))throw new IOException("引用迁移目的地无效");
    }
    public static World load(MapPatch p,String scenario,int player,long seed)throws IOException {return resolve(p,scenario,player,seed,true);}
    public static World preview(MapPatch p,String scenario)throws IOException {return resolve(p,scenario,0,17L,false);}
    private static World resolve(MapPatch p,String scenario,int player,long seed,boolean strict)throws IOException {
        MapPatch.decode(p.encode());verifyBase(p);World w=ScenarioCatalog.load(scenario,player,seed);if(!compatible(w))throw new IOException("此自定义地图仅适用于200×200全国剧本；裁区剧本请选原版地图");
        applyGeography(p,w);
        if(strict){List<Issue> issues=diagnose(w,p,scenario);for(Issue i:issues)if(i.blocking)throw new IOException(i.message);SaveCodec.validate(w);if(!w.alive(player))throw new IOException("所选势力没有启用的城池，不能开局");}
        return w;
    }
    private static void applyGeography(MapPatch p,World w)throws IOException {
        SortedMap<Integer,MapPatch.Site> geo=sites(p);Map<Integer,World.City> original=new TreeMap<>();for(World.City c:w.cities)original.put(c.id,c);
        for(MapPatch.Cell c:p.terrain.values()){Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(c.x(),c.y()));if(!w.inside(h)||c.after()==World.Terrain.VOID)throw new IOException("地图边界受保护："+c.x()+","+c.y());w.terrain[h.q][h.r]=c.after();}
        List<World.City> resolved=new ArrayList<>();Set<Integer> disabled=new TreeSet<>();Map<Integer,Hex> shifts=new TreeMap<>();
        for(MapPatch.Site s:geo.values()){
            MapPatch.Initial init=p.initial(w.scenarioId,s.id());if(init!=null&&!init.enabled()){disabled.add(s.id());continue;}
            World.City old=original.get(s.id());World.City c=new World.City(s.id(),s.name(),s.hex(w),old==null?-1:old.owner);c.kind=s.kind();
            if(old!=null){copyState(old,c);if(old.kind!=s.kind())throw new IOException("已有据点类型不能直接转换；请新建目标类型并显式处理删除引用");}
            else{MapPatch.Initial.neutral(s.kind()).apply(c);Arrays.fill(c.equipment,0);c.recruitReserve=s.kind()==World.SiteKind.CITY?Conscription.RESERVE_CAP:0;c.baseDefense=c.defense;}
            if(init!=null){if(init.owner()>=w.factions.length)throw new IOException("初始势力在此剧本中不存在："+s.name());init.apply(c);c.baseDefense=Math.max(c.baseDefense,c.defense);}
            if(old!=null&&!old.hex.equals(c.hex))shifts.put(c.id,new Hex(c.hex.q-old.hex.q,c.hex.r-old.hex.r));resolved.add(c);
        }
        Set<Integer> removed=new TreeSet<>(original.keySet());for(World.City c:resolved)removed.remove(c.id);
        w.cities.clear();w.cities.addAll(resolved);w.invalidateSiteIndex();
        for(MapPatch.Site s:geo.values())if(w.city(s.id())!=null){w.events.configureRegion(s.id(),s.region());if(s.kind()!=World.SiteKind.CITY){World.City parent=w.city(s.parent());if(parent==null||parent.kind!=World.SiteKind.CITY)throw new IOException(s.name()+"已启用，但关联城市已禁用");w.siteParents.put(s.id(),s.parent());}}
        for(var e:shifts.entrySet()){
            int id=e.getKey();Hex delta=e.getValue();if(w.development.configured(id)){List<Hex> moved=new ArrayList<>();for(Hex h:w.development.parcels(id))moved.add(shift(h,delta));w.development.replace(id,moved);}
            for(int i=0;i<w.domestic.facilities.size();i++){Domestic.Facility f=w.domestic.facilities.get(i);if(f.cityId==id)w.domestic.facilities.set(i,facility(f,id,shift(f.hex,delta)));}
            for(int i=0;i<w.events.camps.size();i++){WorldEvents.Camp c=w.events.camps.get(i);if(c.city==id)w.events.camps.set(i,new WorldEvents.Camp(c.id,id,c.tribe,shift(c.hex,delta),c.troops));}
        }
        // Only absent or ownership-incompatible opening references are rewritten, and only by
        // explicit per-scenario redirect entries saved in the patch's confirmation transaction.
        for(World.Officer o:w.officers)if(o.cityId>=0&&(w.city(o.cityId)==null||o.owner>=0&&w.city(o.cityId).owner!=o.owner)){
            int target=redirect(p,w,o.cityId,"武将 "+o.name);World.City c=w.city(target);if(o.owner>=0&&c.owner!=o.owner)throw new IOException(o.name+"的迁移城市不属于其初始势力");o.cityId=target;
        }
        for(var e:new ArrayList<>(w.life.people.entrySet())){Lifecycle.Life l=e.getValue();if(w.city(l.home)==null){int target=redirect(p,w,l.home,"登场地 "+w.officer(l.officer).name);Lifecycle.Life n=new Lifecycle.Life(l.officer,l.birth,l.appearance,l.expectedDeath,target,l.state);n.diedTurn=l.diedTurn;w.life.people.put(e.getKey(),n);}}
        for(int i=0;i<w.strategy.talents.size();i++){Strategy.Talent t=w.strategy.talents.get(i);if(w.city(t.cityId)==null)w.strategy.talents.set(i,new Strategy.Talent(t.id,t.name,redirect(p,w,t.cityId,"未发现武将 "+t.name),t.leadership,t.war,t.intelligence,t.politics,t.charm,t.availableTurn));}
        for(Treasures.Item item:new ArrayList<>(w.treasures.items.values()))if(item.place==Treasures.Place.HIDDEN&&w.city(item.holder)==null)w.treasures.place(item.definition,item.place,redirect(p,w,item.holder,"未发现宝物 "+item.definition.name));
        for(int id:removed){
            // These objects carry their own coordinates/capacities: never silently delete them.
            for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==id)throw new IOException("删除/禁用受阻："+original.get(id).name+"有初始设施 "+f.kind.label+" #"+f.id+"；须先调整剧本设施数据");
            for(WorldEvents.Camp c:w.events.camps)if(c.city==id)throw new IOException("删除/禁用受阻：初始营寨 #"+c.id+"引用该据点");
            if(w.events.hazards.containsKey(id))throw new IOException("删除/禁用受阻：初始灾害引用该据点");
            w.development.remove(id);w.events.regions.remove(id);
        }
        w.customMapId=p.id;w.customMapName=p.name;w.customMapRevision=p.revision;w.customMapBase=p.base;w.customMapFingerprint=p.logicalFingerprint();w.visualMap=p.copy();w.terrainRevision++;
        w.governance.reconcile(false);w.invalidateSiteIndex();
        // This is a new opening, not an in-progress geographic/capture event.
        // Reset the report comparison baseline after all geographic/state redirects.
        w.reports.rebase();
    }
    private static int redirect(MapPatch p,World w,int from,String label)throws IOException {Integer to=p.redirect(w.scenarioId,from);if(to==null||w.city(to)==null)throw new IOException(label+"仍引用删除/禁用或变更归属的据点 #"+from+"；必须确认迁移");return to;}
    private static Hex shift(Hex h,Hex d){return new Hex(h.q+d.q,h.r+d.r);}
    private static Domestic.Facility facility(Domestic.Facility f,int id,Hex h){Domestic.Facility n=new Domestic.Facility(f.id,id,f.kind,h,f.builderId,f.remaining);n.level=f.level;n.upgradeTo=f.upgradeTo;n.hp=f.hp;n.lastUseTurn=f.lastUseTurn;return n;}
    public static void copyState(World.City a,World.City b){b.owner=a.owner;b.gold=a.gold;b.food=a.food;b.troops=a.troops;b.order=a.order;b.morale=a.morale;b.defense=a.defense;b.baseDefense=a.baseDefense;b.recruitReserve=a.recruitReserve;b.governorId=a.governorId;System.arraycopy(a.equipment,0,b.equipment,0,a.equipment.length);System.arraycopy(a.ships,0,b.ships,0,a.ships.length);}
    public static List<Issue> diagnose(World w,MapPatch p,String scenario){
        List<Issue> out=new ArrayList<>();Set<Integer> ids=new HashSet<>();Map<Hex,Integer> occupied=new HashMap<>();
        for(World.City c:w.cities){if(!ids.add(c.id))out.add(new Issue(true,scenario,"据点ID重复："+c.id,c.hex,c.id));for(Hex h:SiteFootprint.cells(c)){
            Integer previous=occupied.put(h,c.id);if(!w.inside(h)||previous!=null&&previous!=c.id)out.add(new Issue(true,scenario,c.name+"占地越界或与 #"+previous+"重叠",h,c.id));
            else if(c.kind==World.SiteKind.CITY&&w.terrain[h.q][h.r]!=World.Terrain.PLAIN||c.kind!=World.SiteKind.CITY&&(w.army.water(h)||w.cost(h,World.Weapon.SPEAR)<1))out.add(new Issue(true,scenario,c.name+"占地地形不合法",h,c.id));}
            if(c.kind!=World.SiteKind.CITY&&SiteAffiliation.parent(w,c)==null)out.add(new Issue(true,scenario,c.name+"关联城市不存在",c.hex,c.id));
        }
        MapWater water=new MapWater(w);for(World.City c:w.cities){if(c.kind==World.SiteKind.PORT){String error=water.portError(c);if(error!=null)out.add(new Issue(true,scenario,c.name+"："+error,c.hex,c.id));}
            if(c.kind==World.SiteKind.GATE){int exits=0;World.Unit probe=new World.Unit(-1,c.owner,-1,World.Weapon.SWORD,c.hex,1,1);for(Hex h:c.hex.neighbors())if(!w.army.water(h)&&w.army.entryCost(probe,c.hex,h)>0)exits++;if(exits<2)out.add(new Issue(true,scenario,c.name+"没有两个可用的陆地通行方向",c.hex,c.id));else out.add(new Issue(false,scenario,c.name+"为一格关卡；是否存在绕行路线需结合实际部队和势力判断",c.hex,c.id));}}
        for(var e:water.isolated().entrySet())out.add(new Issue(false,scenario,"独立水域 "+e.getValue()+"格：没有主水系锚点，不自动判错或填平",water.representative(e.getKey()),null));
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);if(w.sourceInside(h)&&w.terrain[q][r]==World.Terrain.VOID&&NationalExterior.surface(w,h)==null)out.add(new Issue(false,scenario,"有效源范围内的VOID，需要人工核对",h,null));}
        try{SaveCodec.validate(w);}catch(IOException|RuntimeException e){out.add(new Issue(true,scenario,"正式存档/运行时校验："+e.getMessage(),null,null));}return out;
    }
    public static List<Issue> validateAll(MapPatch p){List<Issue> issues=new ArrayList<>();try{MapPatch.decode(p.encode());verifyBase(p);for(String id:base().scenarioHashes.keySet()){try{World w=preview(p,id);List<Issue> all=diagnose(w,p,id);for(Issue i:all)if(i.blocking||id.equals(p.preview))issues.add(i);}catch(IOException|RuntimeException e){issues.add(new Issue(true,id,e.getMessage(),null,null));}}}catch(IOException|RuntimeException e){issues.add(new Issue(true,p.preview,e.getMessage(),null,null));}return issues;}
    public static String placement(World w,MapPatch.Site s,int ignore){
        Hex center=s.hex(w);World.City candidate=new World.City(s.id(),s.name(),center,-1);candidate.kind=s.kind();for(Hex h:SiteFootprint.cells(candidate)){
            if(!w.inside(h))return "占地越界或进入VOID/外景";World.City at=w.cityAt(h);if(at!=null&&at.id!=ignore)return "与据点 "+at.name+" 重叠";
            if(w.domestic.at(h)!=null||w.war.at(h)!=null||w.unitAt(h)!=null)return "与初始设施或部队冲突";
            World.City parcel=w.development.cityAt(h);if(parcel!=null&&parcel.id!=ignore)return "与 "+parcel.name+" 的内政开发地冲突";
            if(s.kind()==World.SiteKind.CITY?w.terrain[h.q][h.r]!=World.Terrain.PLAIN:w.army.water(h)||w.cost(h,World.Weapon.SPEAR)<1)return "城市七格须为平原；港关须位于可通行陆地";
        }
        if(s.kind()!=World.SiteKind.CITY){World.City parent=w.city(s.parent());if(parent==null||parent.kind!=World.SiteKind.CITY)return "请选择有效关联城市";}
        if(s.kind()==World.SiteKind.PORT){boolean water=false,land=false;for(Hex h:center.neighbors())if(w.army.water(h))water=true;else if(w.inside(h)&&w.cost(h,World.Weapon.SPEAR)>0&&w.cityAt(h)==null)land=true;if(!water||!land)return "港口需要真实陆地出口与邻接可通航水格";}
        return null;
    }
    /** Read-only, concrete dependencies from every compatible opening, including hidden people.
     * Returned entries are shown before deletion; unhandled positioned objects block deletion. */
    public static List<String> references(MapPatch p,int id)throws IOException {
        List<String> result=new ArrayList<>();for(MapPatch.Site s:sites(p).values())if(s.parent()==id)result.add("地理关联："+s.name()+" #"+s.id());
        for(String scenario:base().scenarioHashes.keySet()){World w=preview(p,scenario);World.City c=w.city(id);if(c==null)continue;String prefix=w.scenarioName+"：";result.add(prefix+"据点初始配置（"+w.faction(c.owner)+"，兵"+c.troops+"，金"+c.gold+"，粮"+c.food+"）");for(World.Officer o:w.officers)if(o.cityId==id)result.add(prefix+"驻将 "+o.name+" #"+o.id);
            for(Lifecycle.Life l:w.life.people())if(l.home==id)result.add(prefix+"登场地 "+w.officer(l.officer).name);for(Strategy.Talent t:w.strategy.hiddenTalents())if(t.cityId==id)result.add(prefix+"未发现武将 "+t.name);
            for(Treasures.Item item:w.treasures.items())if(item.place==Treasures.Place.HIDDEN&&item.holder==id)result.add(prefix+"未发现宝物 "+item.definition.name);
            if(w.development.configured(id))result.add(prefix+"开发地 "+w.development.parcels(id).size()+"格（删除城市时随实体移除）");for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==id)result.add(prefix+"阻止删除：初始设施 "+f.kind.label+" #"+f.id);for(WorldEvents.Camp camp:w.events.camps())if(camp.city==id)result.add(prefix+"阻止删除：初始营寨 #"+camp.id);for(WorldEvents.Hazard h:w.events.hazards())if(h.city==id)result.add(prefix+"阻止删除：初始灾害 "+h.kind.label);
        }return result;
    }
    /** Builds an explicit proposal; caller must display and confirm this plan before commit. */
    public static MapPatch deletionProposal(MapPatch current,int id,int parentReplacement)throws IOException {
        MapPatch next=current.copy();SortedMap<Integer,MapPatch.Site> geo=sites(current);MapPatch.Site old=geo.get(id);if(old==null)throw new IOException("据点不存在");if(parentReplacement==id)throw new IOException("不能迁移到自身");
        for(MapPatch.Site child:geo.values())if(child.parent()==id){MapPatch.Site parent=geo.get(parentReplacement);if(parent==null||parent.kind()!=World.SiteKind.CITY)throw new IOException("须为关联港关选择保留的城市");next.putSite(base().sites.get(child.id()),child.parent(parentReplacement));}
        for(String scenario:base().scenarioHashes.keySet()){
            World w=preview(current,scenario);World.City c=w.city(id);if(c!=null){World.City best=relocation(w,id,parentReplacement);if(best!=null)next.redirects.computeIfAbsent(scenario,k->new TreeMap<>()).put(id,best.id);}
            SortedMap<Integer,MapPatch.Initial> states=next.scenarios.get(scenario);if(states!=null){states.remove(id);if(states.isEmpty())next.scenarios.remove(scenario);}
        }
        if(base().sites.containsKey(id))next.putSite(base().sites.get(id),null);else next.sites.remove(id);next.appearances.remove(id);
        // Redirects to an entity being deleted are redirected too, atomically with this deletion.
        for(var e:next.redirects.entrySet())for(var r:new ArrayList<>(e.getValue().entrySet()))if(r.getValue()==id){Integer destination=e.getValue().get(id);if(destination==null)throw new IOException("其他已删除据点仍迁移到此处，须先改引用");e.getValue().put(r.getKey(),destination);}
        for(String scenario:base().scenarioHashes.keySet())preview(next,scenario);return next;
    }
    public static World.City relocation(World w,int id,int preferred){World.City old=w.city(id);if(old==null)return null;World.City chosen=w.city(preferred);if(chosen!=null&&chosen.id!=id&&chosen.kind==World.SiteKind.CITY&&chosen.owner==old.owner)return chosen;World.City best=null;for(World.City c:w.cities)if(c.id!=id&&c.kind==World.SiteKind.CITY&&c.owner==old.owner&&(best==null||c.hex.distance(old.hex)<best.hex.distance(old.hex)||c.hex.distance(old.hex)==best.hex.distance(old.hex)&&c.id<best.id))best=c;return best;}
}
