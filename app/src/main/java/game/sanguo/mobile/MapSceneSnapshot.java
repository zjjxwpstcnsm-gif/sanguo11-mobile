package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Immutable presentation values. Never publishes a World to mesh workers or Filament. */
final class MapSceneSnapshot {
    static final class Ground {
        final Set<Hex> bases; final TerrainSurface surface;
        final int width,height,mapSeed; final byte[] terrain; final GridWorldTransform grid;
        Ground(World w) {
            width=w.width;height=w.height;mapSeed=31*w.mapId.hashCode()+w.mapRevision;grid=new GridWorldTransform(w.sourceMapWidth>0?(w.height-1)/2:0,w.columnStaggered);
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));bases=Collections.unmodifiableSet(flat);
            terrain=new byte[width*height];
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)terrain[r*width+q]=(byte)(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal());
            surface=new TerrainSurface(this);
        }
        boolean matches(World w){
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));if(!flat.equals(bases))return false;
            if(mapSeed!=31*w.mapId.hashCode()+w.mapRevision||width!=w.width||height!=w.height||grid.staggered!=w.columnStaggered||grid.offset!=(w.sourceMapWidth>0?(w.height-1)/2:0))return false;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)if(terrain[r*width+q]!=(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal()))return false;
            return true;
        }
        boolean valid(Hex h){return h!=null&&h.q>=0&&h.r>=0&&h.q<width&&h.r<height&&terrain[h.r*width+h.q]!=World.Terrain.VOID.ordinal();}
    }
    static final class Item {
        final String key,label; final Hex hex; final int kind,color;
        final FacilityState facility; final SiteVisual site; final UnitVisual unit;
        Item(String key,String label,Hex hex,int kind,int color,FacilityState facility){
            this.key=key;this.label=label;this.hex=hex;this.kind=kind;this.color=color;this.facility=facility;this.site=null;this.unit=null;
        }
        Item(String key,String label,Hex hex,int kind,int color,SiteVisual site){
            this.key=key;this.label=label;this.hex=hex;this.kind=kind;this.color=color;this.site=site;this.facility=null;this.unit=null;
        }
        Item(World w,World.Unit u){
            key="unit:"+u.id;hex=u.hex;kind=3;color=FactionColors.color(w,u.owner);
            facility=null;site=null;unit=new UnitVisual(w,u);label=unit.label();
        }
        // Transition meshes are shared by silhouette/color only; status does not create GPU variants.
        String shapeKey(){return kind+":"+color;}
        String displayLabel(){return facility==null?label:label+facility.status();}
        Item(String key,String label,Hex hex,int kind,int color){this(key,label,hex,kind,color,(FacilityState)null);}
    }
    /** Exact detached state, ready for S03's future asset resolver; never retains a core entity. */
    static final class FacilityState {
        final String type; final int owner,level,upgradeTo,hp,maxHp,remaining,direction;
        final boolean complete,burning;
        FacilityState(String type,int owner,int level,int upgradeTo,int hp,int maxHp,
                      int remaining,int direction,boolean complete,boolean burning){
            this.type=type;this.owner=owner;this.level=level;this.upgradeTo=upgradeTo;
            this.hp=hp;this.maxHp=maxHp;this.remaining=remaining;this.direction=direction;
            this.complete=complete;this.burning=burning;
        }
        String status(){
            return (level>0?" Lv"+level:"")+(upgradeTo>0?" → Lv"+upgradeTo:"")
                +(!complete?(upgradeTo>0?" 升级中":" 建造中")+(remaining>0?"("+remaining+"旬)":""):"")
                +(hp<maxHp?" 耐久"+hp+"/"+maxHp:"")+(burning?" 起火":"");
        }
    }
    final Ground ground; final List<Item> items; final Hex selected; final Set<Hex> reachable,siege,coverage;
    MapSceneSnapshot(Ground ground,World w,Hex selected,int moving) {
        this.ground=ground;this.selected=selected;List<Item> list=new ArrayList<>();
        for(World.City c:w.cities){Item item=new Item("site:"+c.id,c.name,c.hex,c.kind==World.SiteKind.CITY?0:c.kind==World.SiteKind.PORT?1:2,FactionColors.color(w,c.owner),new SiteVisual(w,c,ground.grid));list.add(item);}
        for(World.Unit u:w.fieldUnits())list.add(new Item(w,u));
        for(Domestic.Facility f:w.domestic.facilities){
            World.City home=w.city(f.cityId);int owner=home==null?-1:home.owner;
            list.add(new Item("domestic:"+f.id,f.kind.label,f.hex,4,FactionColors.color(w,owner),
                new FacilityState("domestic/"+f.kind.name(),owner,f.level,f.upgradeTo,f.hp,f.maxHp(),
                    f.remaining,0,f.remaining==0,w.war.fireAt(f.hex)!=null)));
        }
        for(War.Structure s:w.war.structures())list.add(new Item("structure:"+s.id,s.kind.label,s.hex,5,FactionColors.color(w,s.owner),
            new FacilityState("military/"+s.kind.name(),s.owner,0,0,s.hp,s.kind.hp,0,s.direction,
                s.complete,w.war.fireAt(s.hex)!=null)));
        items=Collections.unmodifiableList(list);
        reachable=Collections.unmodifiableSet(new HashSet<>(w.orders.marchReachable(w.unit(moving)).keySet()));
        coverage=Collections.unmodifiableSet(new HashSet<>(w.fieldworks.coverage(w.war.at(selected))));
        siege=Collections.unmodifiableSet(new HashSet<>(SiegeOverlay.selected(w,selected).cells));
    }
}
