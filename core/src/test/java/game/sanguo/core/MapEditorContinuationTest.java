package game.sanguo.core;
import game.sanguo.core.map.SourceGridCoord;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Isolated deltas only: never modifies shipped map resources. */
public final class MapEditorContinuationTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface Action{void run()throws Exception;}
    static void reject(Action action,String message)throws Exception{try{action.run();throw new AssertionError(message);}catch(IOException|IllegalArgumentException expected){checks++;}}
    public static MapPatch.Site placement(World w,World.SiteKind kind,int id,int parent,Hex avoid)throws Exception{
        MapWater water=kind==World.SiteKind.PORT?new MapWater(w):null;
        for(int x=4;x<196;x++)for(int y=4;y<196;y++){
            MapPatch.Site site=new MapPatch.Site(id,"验收"+kind,x,y,kind,parent,WorldEvents.Tribe.BANDIT);Hex h=site.hex(w);
            if(avoid!=null&&avoid.distance(h)<8)continue;if(CustomMaps.placement(w,site,id)!=null)continue;
            if(kind==World.SiteKind.PORT){boolean main=false;for(Hex n:h.neighbors())main|=water.main(n);if(!main)continue;
                World.City ghost=new World.City(id,site.name(),h,0);ghost.kind=kind;w.cities.add(ghost);String error=water.portError(ghost);w.cities.remove(ghost);if(error!=null)continue;}
            if(kind==World.SiteKind.GATE){int exits=0;for(Hex n:h.neighbors())if(w.inside(n)&&w.army.entryCost(new World.Unit(-1,0,-1,World.Weapon.SPEAR,h,1,1),h,n)>0)exits++;if(exits<2)continue;}
            return site;
        }
        throw new AssertionError("No valid "+kind+" fixture location");
    }
    public static MapPatch fixture()throws Exception {
        MapPatch p=CustomMaps.base().fresh();p.name="独立地图验收";World w=CustomMaps.preview(p,p.preview);int parent=w.home().id;
        MapPatch.Site city=placement(w,World.SiteKind.CITY,100006701,-1,null);p.putSite(null,city);w=CustomMaps.preview(p,p.preview);
        MapPatch.Site port=placement(w,World.SiteKind.PORT,100006702,parent,city.hex(w));p.putSite(null,port);w=CustomMaps.preview(p,p.preview);
        MapPatch.Site gate=placement(w,World.SiteKind.GATE,100006703,parent,city.hex(w));p.putSite(null,gate);
        p.scenarios.computeIfAbsent(p.preview,k->new TreeMap<>()).put(port.id(),new MapPatch.Initial(true,0,1000,10000,0,90,70,2000));return p;
    }
    public static void main(String[] args)throws Exception {
        MapPatch original=CustomMaps.base().fresh();World base=ScenarioCatalog.load("heroes-250",0,123L);byte[] baseSave=SaveCodec.encode(base);
        check(CustomMaps.validateAll(original).stream().noneMatch(CustomMaps.Issue::blocking),"unchanged map passes production diagnostics");
        MapEditSession session=new MapEditSession(original);Hex first=null,last=null;
        for(int x=30;x<160&&first==null;x++)for(int y=30;y<160;y++){Hex h=MapCoordinates.fromNationalSource(session.world(),new SourceGridCoord(x,y));if(!session.protectedAt(h)&&session.world().terrain[h.q][h.r]==World.Terrain.PLAIN){first=h;break;}}
        check(first!=null,"editable legal cell");last=first.neighbors().stream().filter(h->!session.protectedAt(h)).findFirst().orElse(first);
        String oldHash=session.patch().fingerprint();Set<Hex> stroke=session.stroke(first,last,2);int changes=session.paint("一笔",stroke,World.Terrain.FOREST);
        check(changes>0,"real terrain mutation");check(session.undoLabel().equals("一笔"),"one stroke one transaction");session.undo();check(session.patch().fingerprint().equals(oldHash),"exact undo");session.redo();check(session.patch().terrain.size()==changes,"complete redo");
        Set<Hex> fill=session.fill(first);check(fill.contains(first),"six-neighbour fill");for(Hex h:fill)check(session.world().terrain[h.q][h.r]==World.Terrain.FOREST&&!session.protectedAt(h),"fill condition and protection");
        check(!session.rectangle(first,last).isEmpty(),"source-grid rectangle");World.City protectedCity=session.world().home();check(session.paint("protected",SiteFootprint.cells(protectedCity),World.Terrain.WATER)==0,"seven cells protected");
        reject(()->session.paint("VOID",Collections.singleton(protectedCity.hex),World.Terrain.VOID),"boundary modification rejected");
        check(Arrays.equals(MapPatch.decode(session.patch().encode()).encode(),session.patch().encode()),"phone JSON round trip");
        String beforeConflict=session.patch().fingerprint();MapPatch corrupt=session.patch();corrupt.base="0".repeat(64);reject(()->session.replace("conflict",corrupt),"base conflict rejected");check(beforeConflict.equals(session.patch().fingerprint()),"failed import atomic");
        MapPatch p=fixture();session.replace("实体事务",p);World w=session.world();World.City city=w.city(100006701),port=w.city(100006702),gate=w.city(100006703);
        check(w.cities.size()==base.cities.size()+3,"all three entity types in production roster");check(city.owner==-1&&city.food==10000,"neutral defaults");
        for(Hex h:SiteFootprint.cells(city))check(w.cityAt(h)==city,"seven-cell production hit index");check(SiteFootprint.cells(gate).size()==1&&SiteFootprint.cells(port).size()==1,"one-cell port and gate");check(w.development.capacity(city.id)>0,"real domestic capacity");
        MapPatch.Site moved=placement(w,World.SiteKind.CITY,city.id,-1,city.hex);MapPatch next=session.putSite(moved);Hex oldCenter=city.hex;session.replace("移动城池",next);World relocated=session.world();
        check(relocated.cityAt(oldCenter)==null,"no ghost old footprint");for(Hex h:SiteFootprint.cells(relocated.city(city.id)))check(relocated.cityAt(h).id==city.id,"new footprint indexed");session.undo();check(session.world().city(city.id).hex.equals(oldCenter),"move undo");session.redo();check(session.world().cityAt(oldCenter)==null,"move redo");
        p=session.patch();check(CustomMaps.validateAll(p).stream().noneMatch(CustomMaps.Issue::blocking),"all supported scenarios validate new sites");w=CustomMaps.load(p,p.preview,0,17L);check(w.city(city.id)!=null&&w.city(port.id)!=null&&w.city(gate.id)!=null,"formal new game uses patch");
        for(String scenario:CustomMaps.base().scenarioHashes.keySet()){World opening=CustomMaps.load(p,scenario,0,17L);check(opening.cities.size()==90,"new sites in every supported scenario");check(SaveCodec.decode(SaveCodec.encode(opening)).city(city.id)!=null,"save reads without external map");}
        MapWater water=new MapWater(w);port=w.city(port.id);check(water.portError(port)==null,"new port main-waterway and real dock");Hex sea=null,dock=null;for(Hex n:port.hex.neighbors())if(w.army.water(n)){Hex land=MapWater.landDock(w,port,n);if(land!=null){sea=n;dock=land;break;}}check(dock!=null&&sea!=null,"official embarkation dock");
        World.Officer leader=w.idle(w.home()).stream().filter(o->o.role!=Strategy.Role.RULER).findFirst().orElse(w.idle(w.home()).get(0));World.Unit unit=new World.Unit(w.nextUnitId++,0,leader.id,World.Weapon.SPEAR,dock,3000,9000);w.units.add(unit);leader.unitId=unit.id;leader.cityId=-1;
        World.Result embark=w.move(unit.id,sea);check(embark.ok,"official enter water: "+embark.message);unit.acted=false;unit.movementBudget=-1;unit.movementSpent=0;leader.acted=false;w.actionPoints[0]=60;
        World.Result disembark=w.move(unit.id,dock);check(disembark.ok,"official leave water: "+disembark.message);
        World.City parent=SiteAffiliation.parent(w,port);int owner=port.owner;parent.owner=1;check(port.owner==owner,"parent capture does not transfer port");check(w.districts.city(port.id)==null||w.districts.city(port.id).owner==port.owner,"no enemy-parent district");parent.owner=0;
        MapPatch enabled=p.copy();enabled.scenarios.computeIfAbsent(p.preview,k->new TreeMap<>()).put(city.id,MapPatch.Initial.neutral(World.SiteKind.CITY).enabled(false));World disabled=CustomMaps.load(enabled,p.preview,0,17L);check(disabled.city(city.id)==null&&disabled.cityAt(w.city(city.id).hex)==null,"disabled entity no ghost occupancy");
        List<String> refs=CustomMaps.references(p,w.home().id);check(refs.stream().anyMatch(s->s.contains("驻将"))&&refs.stream().anyMatch(s->s.contains("地理关联")),"actual officer and parent deletion references");
        MapPatch deleted=CustomMaps.deletionProposal(p,city.id,w.home().id);session.replace("删除新城",deleted);check(session.world().city(city.id)==null,"actual deletion");session.undo();check(session.world().city(city.id)!=null,"deletion undo");
        byte[] saved=SaveCodec.encode(CustomMaps.load(p,p.preview,0,17L));MapPatch newer=p.copy();newer.name="后来改名";newer.revision++;session.replace("新草稿",newer);World pinned=SaveCodec.decode(saved);check(pinned.customMapRevision==p.revision&&pinned.customMapName.equals(p.name),"save pins old identity");check(pinned.city(city.id)!=null,"save pins geography");
        MapPatch duplicate=MapPatch.decode(p.encode());check(CustomMaps.load(duplicate,duplicate.preview,0,17L).cities.size()==90,"repeat import idempotent");check(Arrays.equals(baseSave,SaveCodec.encode(base)),"live world isolated");check(ScenarioCatalog.load("heroes-250",0).cities.size()==87,"original map unchanged");
        if(args.length>0){Path file=Path.of(args[0]);Files.createDirectories(file.toAbsolutePath().getParent());Files.write(file,p.encode());}System.out.println("PASS "+checks+" custom-map production integration assertions");
    }
}
