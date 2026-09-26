package game.sanguo.mobile;
import game.sanguo.core.map.SourceGridCoord;

import game.sanguo.core.*;
import java.util.*;

/** Immutable presentation values. Never publishes a World to mesh workers or Filament. */
final class MapSceneSnapshot {
    static final class Ground {
        final Set<Hex> bases; final TerrainSurface surface;
        private final BitSet baseCells;
        final int width,height,mapSeed,mapIdentity,sourceMapWidth,sourceOriginX,sourceOriginY; final float minX,minZ,maxX,maxZ; final byte[] terrain; final GridWorldTransform grid;
        Ground(World w) {
            width=w.width;height=w.height;sourceMapWidth=w.sourceMapWidth;sourceOriginX=w.sourceOriginX;sourceOriginY=w.sourceOriginY;mapIdentity=w.mapId.hashCode();mapSeed=31*w.mapId.hashCode()+w.mapRevision;grid=new GridWorldTransform(w.sourceMapWidth>0?(w.height-1)/2:0,w.columnStaggered);
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));bases=Collections.unmodifiableSet(flat);
            baseCells=new BitSet(width*height);for(Hex h:flat)if(h.q>=0&&h.r>=0&&h.q<width&&h.r<height)baseCells.set(h.r*width+h.q);
            terrain=new byte[width*height];
            float loX=Float.MAX_VALUE,loZ=loX,hiX=-loX,hiZ=-loX;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++){
                terrain[r*width+q]=(byte)(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal());
                if(terrain[r*width+q]!=World.Terrain.VOID.ordinal()){
                    float x=grid.x(q,r),z=grid.z(q,r);loX=Math.min(loX,x-.5f);loZ=Math.min(loZ,z-.5f);hiX=Math.max(hiX,x+.5f);hiZ=Math.max(hiZ,z+.5f);
                }
            }
            minX=loX==Float.MAX_VALUE?0:loX;minZ=loZ==Float.MAX_VALUE?0:loZ;
            maxX=hiX==-Float.MAX_VALUE?0:hiX;maxZ=hiZ==-Float.MAX_VALUE?0:hiZ;
            Map<Hex,Float> heights=new HashMap<>();
            if(w.visualMap!=null)for(var e:w.visualMap.heights.entrySet())heights.put(MapCoordinates.fromNationalSource(w,new SourceGridCoord(e.getKey()/200,e.getKey()%200)),e.getValue()/1000f);
            surface=new TerrainSurface(this,heights);
        }
        SourceGridCoord source(Hex h){
            SourceGridCoord local=grid.staggered?MapCoordinates.sourceColumn(h,sourceMapWidth):sourceMapWidth>0?MapCoordinates.sourceCoord(h,height):new SourceGridCoord(h.q,h.r);
            return new SourceGridCoord(local.x+sourceOriginX,local.y+sourceOriginY);
        }
        boolean matches(World w){
            if(sourceMapWidth!=w.sourceMapWidth||sourceOriginX!=w.sourceOriginX||sourceOriginY!=w.sourceOriginY)return false;
            Map<Hex,Float> expected=new HashMap<>();if(w.visualMap!=null)for(var e:w.visualMap.heights.entrySet())expected.put(MapCoordinates.fromNationalSource(w,new SourceGridCoord(e.getKey()/200,e.getKey()%200)),e.getValue()/1000f);if(!surface.overrides.equals(expected))return false;
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));if(!flat.equals(bases))return false;
            if(mapSeed!=31*w.mapId.hashCode()+w.mapRevision||width!=w.width||height!=w.height||grid.staggered!=w.columnStaggered||grid.offset!=(w.sourceMapWidth>0?(w.height-1)/2:0))return false;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)if(terrain[r*width+q]!=(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal()))return false;
            return true;
        }
        /** Exact immutable membership, without allocating a Hex in each material sample. */
        boolean isBase(int q,int r){return q>=0&&r>=0&&q<width&&r<height&&baseCells.get(r*width+q);}
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
    static final class FireState {
        final Hex hex;final int remaining;
        FireState(War.Fire f){hex=f.hex;remaining=f.remaining;}
    }
    final int month;
    final List<FireState> fires;
    final Ground ground; final List<Item> items; final Hex selected; final Set<Hex> reachable,siege,coverage,attackTargets;
    MapSceneSnapshot(Ground ground,World w,Hex selected,int moving) {
        month=(w.startMonth-1+w.turn/3)%12+1;
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
        List<FireState> fireList=new ArrayList<>();for(War.Fire f:w.war.fires())if(f.remaining>0)fireList.add(new FireState(f));fires=Collections.unmodifiableList(fireList);
        reachable=Collections.unmodifiableSet(new HashSet<>(w.orders.marchReachable(w.unit(moving)).keySet()));
        attackTargets=attackTargets(w,moving);
        coverage=Collections.unmodifiableSet(new HashSet<>(w.fieldworks.coverage(w.war.at(selected))));
        siege=Collections.unmodifiableSet(new HashSet<>(SiegeOverlay.selected(w,selected).cells));
    }
    /** Exact existing 2D authority queries, shared by both presentation paths. */
    static Set<Hex> attackTargets(World w,int moving){
        Set<Hex> targets=new HashSet<>();World.Unit actor=w.unit(moving);
        if(w.orders.error(actor)==null){
            for(World.Unit target:w.fieldUnits())if(target.id!=actor.id&&w.war.attackError(actor.id,target.id)==null)targets.add(target.hex);
            for(World.City city:w.cities)if(w.siegeError(actor.id,city.id)==null)
                for(Hex h:SiteFootprint.cells(city))if(h.equals(w.siegeHit(actor,city,h)))targets.add(h);
            for(Domestic.Facility f:w.domestic.facilities)if(w.war.facilityAttackError(actor.id,f.hex)==null)targets.add(f.hex);
            for(War.Structure structure:w.war.structures())if(w.war.structureAttackError(actor.id,structure.hex)==null)targets.add(structure.hex);
        }
        return Collections.unmodifiableSet(targets);
    }

}
