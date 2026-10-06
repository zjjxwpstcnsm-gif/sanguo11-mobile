package game.sanguo.mobile;
import game.sanguo.core.map.SourceGridCoord;

import game.sanguo.core.*;
import game.sanguo.api.SceneFactsSnapshot;
import game.sanguo.api.StateToken;
import java.util.*;

/** Immutable presentation values. Never publishes a World to mesh workers or Filament. */
final class MapSceneSnapshot {
    private static final World.Terrain[] TERRAIN_TYPES=World.Terrain.values();
    /** Shared 2D/3D presentation filter, using the authoritative technology family. */
    static boolean gridTerrain(World.Terrain type,boolean difficultMarch){
        return type!=World.Terrain.VOID&&type!=World.Terrain.MOUNTAIN&&type!=World.Terrain.NON_NAVIGABLE_WATER
            &&(difficultMarch||!Fieldworks.requiresDifficultMarch(type));
    }
    static final class Ground {
        final Set<Hex> bases; final TerrainSurface surface;
        private final BitSet baseCells, gridCells;
        final WaterVisualField.Shoreline shoreline;
        private volatile WaterVisualField waterSamples;
        final boolean originalNational; final PcMap pcMap;
        final int gridForce; final boolean gridDifficultMarch;
        final int width,height,mapSeed,mapIdentity,sourceMapWidth,sourceOriginX,sourceOriginY; final float minX,minZ,maxX,maxZ; final byte[] terrain; final GridWorldTransform grid;
        Ground(World w) {this(w,w.player);}
        Ground(World w,int force) {
            gridForce=force;gridDifficultMarch=w.campaign.has(force,Campaign.Tech.DIFFICULT_MARCH);
            originalNational=NationalMap.ID.equals(w.mapId)&&w.sourceMapWidth==200&&w.customMapId.isEmpty();
            pcMap=NationalMap.ID.equals(w.mapId)&&NationalMap.pcRevision(w.mapRevision)&&w.customMapId.isEmpty()&&w.columnStaggered?PcMap.get():null;
            width=w.width;height=w.height;sourceMapWidth=w.sourceMapWidth;sourceOriginX=w.sourceOriginX;sourceOriginY=w.sourceOriginY;mapIdentity=w.mapId.hashCode();mapSeed=31*w.mapId.hashCode()+w.mapRevision;grid=new GridWorldTransform(w.sourceMapWidth>0?(w.height-1)/2:0,w.columnStaggered);
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));bases=Collections.unmodifiableSet(flat);
            baseCells=new BitSet(width*height);for(Hex h:flat)if(h.q>=0&&h.r>=0&&h.q<width&&h.r<height)baseCells.set(h.r*width+h.q);
            terrain=new byte[width*height];gridCells=new BitSet(terrain.length);
            float loX=Float.MAX_VALUE,loZ=loX,hiX=-loX,hiZ=-loX;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++){
                terrain[r*width+q]=(byte)(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal());
                World.Terrain type=w.terrain[q][r];
                if(terrain[r*width+q]!=World.Terrain.VOID.ordinal()&&gridTerrain(type,true)&&!NationalMap.restricted(w,new Hex(q,r)))gridCells.set(r*width+q);
                if(terrain[r*width+q]!=World.Terrain.VOID.ordinal()){
                    float x=grid.x(q,r),z=grid.z(q,r);loX=Math.min(loX,x-.5f);loZ=Math.min(loZ,z-.5f);hiX=Math.max(hiX,x+.5f);hiZ=Math.max(hiZ,z+.5f);
                }
            }
            minX=loX==Float.MAX_VALUE?0:loX;minZ=loZ==Float.MAX_VALUE?0:loZ;
            maxX=hiX==-Float.MAX_VALUE?0:hiX;maxZ=hiZ==-Float.MAX_VALUE?0:hiZ;
            Map<Hex,Float> heights=new HashMap<>();
            if(w.visualMap!=null)for(var e:w.visualMap.heights.entrySet())heights.put(MapCoordinates.fromNationalSource(w,new SourceGridCoord(e.getKey()/200,e.getKey()%200)),e.getValue()/1000f);
            shoreline=new WaterVisualField.Shoreline(this);
            surface=new TerrainSurface(this,heights);
        }
        SourceGridCoord source(Hex h){
            SourceGridCoord local=grid.staggered?MapCoordinates.sourceColumn(h,sourceMapWidth):sourceMapWidth>0?MapCoordinates.sourceCoord(h,height):new SourceGridCoord(h.q,h.r);
            return new SourceGridCoord(local.x+sourceOriginX,local.y+sourceOriginY);
        }
        /** A research/force change reuses immutable geometry, never mutates an older snapshot. */
        private Ground(Ground old,int force,boolean difficultMarch){
            gridForce=force;gridDifficultMarch=difficultMarch;
            originalNational=old.originalNational;pcMap=old.pcMap;width=old.width;height=old.height;
            sourceMapWidth=old.sourceMapWidth;sourceOriginX=old.sourceOriginX;sourceOriginY=old.sourceOriginY;
            mapIdentity=old.mapIdentity;mapSeed=old.mapSeed;grid=old.grid;
            bases=old.bases;baseCells=old.baseCells;terrain=old.terrain;gridCells=old.gridCells;
            minX=old.minX;minZ=old.minZ;maxX=old.maxX;maxZ=old.maxZ;
            shoreline=old.shoreline;surface=old.surface;waterSamples=old.waterField();
        }
        /** One bounded exact sampling cache per immutable terrain, shared by all
         * chunk workers. Publish after surface construction; scene disposal drops
         * the owner. Grid-force copies have identical sampling inputs. */
        WaterVisualField waterField(){
            WaterVisualField result=waterSamples;
            if(result==null)synchronized(this){result=waterSamples;if(result==null)waterSamples=result=new WaterVisualField(this);}
            return result;
        }
        boolean matchesGridContext(World w,int force){return gridForce==force&&gridDifficultMarch==w.campaign.has(force,Campaign.Tech.DIFFICULT_MARCH);}
        Ground withGridContext(World w,int force){return matchesGridContext(w,force)?this:new Ground(this,force,w.campaign.has(force,Campaign.Tech.DIFFICULT_MARCH));}
        boolean matches(World w){return matchesGridContext(w,w.player)&&matchesTerrain(w);}
        boolean matchesTerrain(World w){
            if(originalNational!=(NationalMap.ID.equals(w.mapId)&&w.sourceMapWidth==200&&w.customMapId.isEmpty()))return false;
            if(sourceMapWidth!=w.sourceMapWidth||sourceOriginX!=w.sourceOriginX||sourceOriginY!=w.sourceOriginY)return false;
            Map<Hex,Float> expected=new HashMap<>();if(w.visualMap!=null)for(var e:w.visualMap.heights.entrySet())expected.put(MapCoordinates.fromNationalSource(w,new SourceGridCoord(e.getKey()/200,e.getKey()%200)),e.getValue()/1000f);if(!surface.overrides.equals(expected))return false;
            Set<Hex> flat=new HashSet<>();for(World.City c:w.cities)flat.addAll(SiteFootprint.cells(c));if(!flat.equals(bases))return false;
            if(mapSeed!=31*w.mapId.hashCode()+w.mapRevision||width!=w.width||height!=w.height||grid.staggered!=w.columnStaggered||grid.offset!=(w.sourceMapWidth>0?(w.height-1)/2:0))return false;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)if(terrain[r*width+q]!=(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal()))return false;
            return true;
        }
        /** Exact immutable membership, without allocating a Hex in each material sample. */
        boolean isBase(int q,int r){return q>=0&&r>=0&&q<width&&r<height&&baseCells.get(r*width+q);}
        /** Ordinary grid follows the viewing force's authoritative difficult-march
         * terrain family, not the active AI side, selected unit, occupancy or weapon.
         * Editing still exposes blocked terrain so it can be inspected and painted. */
        boolean gridCell(Hex h,boolean editing){
            if(!valid(h))return false;
            int cell=h.r*width+h.q;
            return editing||gridCells.get(cell)&&gridTerrain(TERRAIN_TYPES[terrain[cell]],gridDifficultMarch);
        }
        boolean valid(int q,int r){return q>=0&&r>=0&&q<width&&r<height&&terrain[r*width+q]!=World.Terrain.VOID.ordinal();}
        boolean valid(Hex h){return h!=null&&valid(h.q,h.r);}
    }
    static final class Item {
        final String key,label; final Hex hex; final int kind,color,textColor;
        final FacilityState facility; final SiteVisual site; final UnitVisual unit;
        Item(String key,String label,Hex hex,int kind,int color,FacilityState facility){
            this.key=key;this.label=label;this.hex=hex;this.kind=kind;this.color=color;this.textColor=FactionColors.textColor(color);this.facility=facility;this.site=null;this.unit=null;
        }
        Item(String key,String label,Hex hex,int kind,int color,SiteVisual site){
            this.key=key;this.label=label;this.hex=hex;this.kind=kind;this.color=color;this.textColor=FactionColors.textColor(color);this.site=site;this.facility=null;this.unit=null;
        }
        Item(World w,World.Unit u){
            key="unit:"+u.id;hex=u.hex;kind=3;color=FactionColors.color(w,u.owner);textColor=FactionColors.textColor(color);
            facility=null;site=null;unit=new UnitVisual(w,u);label=unit.label();
        }
        // Transition meshes are shared by silhouette/color only; status does not create GPU variants.
        String shapeKey(){return kind+":"+color;}
        String displayLabel(){return facility==null?label:label+facility.status();}
        Item(String key,String label,Hex hex,int kind,int color){this(key,label,hex,kind,color,(FacilityState)null);}
    }
    /** Exact detached state, ready for S03's future asset resolver; never retains a core entity. */
    static final class FacilityState {
        final String type; final int owner,level,upgradeTo,hp,maxHp,remaining,direction,builderUnitId,builderOfficerId;
        final boolean complete,burning;
        FacilityState(String type,int owner,int level,int upgradeTo,int hp,int maxHp,
                      int remaining,int direction,boolean complete,boolean burning){
            this(type,owner,level,upgradeTo,hp,maxHp,remaining,direction,complete,burning,-1,-1);
        }
        FacilityState(String type,int owner,int level,int upgradeTo,int hp,int maxHp,int remaining,int direction,boolean complete,boolean burning,int builderUnitId,int builderOfficerId){
            this.builderUnitId=builderUnitId;this.builderOfficerId=builderOfficerId;
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
        final Hex hex;final int remaining,owner,power;final boolean trap;final Integer sourceX,sourceY;final String label;
        FireState(War.Fire f){hex=f.hex;remaining=f.remaining;owner=f.owner;power=f.power;trap=f.trap;sourceX=null;sourceY=null;label="火 · "+remaining+"旬";}
        FireState(SceneFactsSnapshot.Fire f){hex=new Hex(f.cell.q,f.cell.r);remaining=f.remaining;owner=f.owner;power=f.power;trap=f.trap;sourceX=f.cell.sourceX;sourceY=f.cell.sourceY;label="火 · "+remaining+"旬";}
    }
    final int month;
    final StateToken state;
    final boolean authoritativeSceneFacts;
    /** Source odd-q connection bits, derived only from detached live wall values. */
    final Map<Hex,Integer> wallConnections;
    final List<FireState> fires;
    final Ground ground; final List<Item> items; final Hex selected; final Set<Hex> reachable,siege,coverage,attackTargets;
    MapSceneSnapshot(Ground ground,World w,Hex selected,int moving){this(ground,w,selected,moving,null,null);}
    MapSceneSnapshot(Ground ground,World w,Hex selected,int moving,StateToken expected,SceneFactsSnapshot input) {
        SceneFactsSnapshot facts=SceneFactsPresentation.accept(w,expected,input);state=expected;authoritativeSceneFacts=facts!=null;
        month=facts==null?(w.startMonth-1+w.turn/3)%12+1:facts.month;
        this.ground=ground;this.selected=selected;List<Item> list=new ArrayList<>();
        for(World.City c:w.cities){Item item=new Item("site:"+c.id,c.name,c.hex,c.kind==World.SiteKind.CITY?0:c.kind==World.SiteKind.PORT?1:2,FactionColors.color(w,c.owner),new SiteVisual(w,c,ground.grid));list.add(item);}
        for(World.Unit u:w.fieldUnits())list.add(new Item(w,u));
        List<FireState> fireList=new ArrayList<>();
        if(facts==null){for(War.Fire f:w.war.fires())if(f.remaining>0)fireList.add(new FireState(f));}
        else for(SceneFactsSnapshot.Fire f:facts.fires)if(f.remaining>0)fireList.add(new FireState(f));
        Set<Hex> burning=new HashSet<>();for(FireState fire:fireList)burning.add(fire.hex);
        if(facts==null){
        for(Domestic.Facility f:w.domestic.facilities){
            World.City home=w.city(f.cityId);int owner=home==null?-1:home.owner;
            list.add(new Item("domestic:"+f.id,f.kind.label,f.hex,4,FactionColors.color(w,owner),
                new FacilityState("domestic/"+f.kind.name(),owner,f.level,f.upgradeTo,f.hp,f.maxHp(),
                    f.remaining,0,f.remaining==0,w.war.fireAt(f.hex)!=null)));
        }
        for(War.Structure s:w.war.structures())list.add(new Item("structure:"+s.id,s.kind.label,s.hex,5,FactionColors.color(w,s.owner),
            new FacilityState("military/"+s.kind.name(),s.owner,0,0,s.hp,s.kind.hp,0,s.direction,
                s.complete,w.war.fireAt(s.hex)!=null)));
        }else{
            for(SceneFactsSnapshot.Domestic f:facts.domestic){
                Hex hex=new Hex(f.cell.q,f.cell.r);Domestic.Kind kind=Domestic.Kind.valueOf(f.kind);
                list.add(new Item("domestic:"+f.id,kind.label,hex,4,FactionColors.color(w,f.owner),new FacilityState("domestic/"+f.kind,f.owner,f.level,f.upgradeTo,f.hp,f.maxHp,f.remaining,0,f.remaining==0,burning.contains(hex),-1,f.builderOfficerId)));
            }
            for(SceneFactsSnapshot.Military f:facts.military){
                Hex hex=new Hex(f.cell.q,f.cell.r);War.StructureKind kind=War.StructureKind.valueOf(f.kind);
                list.add(new Item("structure:"+f.id,kind.label,hex,5,FactionColors.color(w,f.owner),new FacilityState("military/"+f.kind,f.owner,0,0,f.hp,f.maxHp,0,f.direction,f.complete,burning.contains(hex),f.builderUnitId,-1)));
            }
        }
        items=Collections.unmodifiableList(list);
        wallConnections=wallConnections(ground,list);
        fires=Collections.unmodifiableList(fireList);
        reachable=Collections.unmodifiableSet(new HashSet<>(w.orders.marchReachable(w.unit(moving)).keySet()));
        attackTargets=attackTargets(w,moving);
        coverage=Collections.unmodifiableSet(new HashSet<>(w.fieldworks.coverage(w.war.at(selected))));
        siege=Collections.unmodifiableSet(new HashSet<>(SiegeOverlay.selected(w,selected).cells));
    }
    private static Map<Hex,Integer> wallConnections(Ground g,List<Item> items){
        if(g.pcMap==null)return Collections.emptyMap();
        Map<SourceGridCoord,Item> healthy=new HashMap<>();
        for(Item i:items)if(wallType(i)&&i.facility.complete&&i.facility.hp>=Math.min(i.facility.maxHp/2,500))healthy.put(g.source(i.hex),i);
        int[][][] steps={{{-1,-1},{0,-1},{1,-1},{-1,0},{0,1},{1,0}},{{-1,0},{0,-1},{1,0},{-1,1},{0,1},{1,1}}};
        Map<Hex,Integer> masks=new HashMap<>();
        for(var e:healthy.entrySet()){
            SourceGridCoord s=e.getKey();Item i=e.getValue();int mask=0;
            for(int k=0;k<6;k++){int[] delta=steps[s.x&1][k];Item n=healthy.get(new SourceGridCoord(s.x+delta[0],s.y+delta[1]));if(n!=null&&n.facility.type.equals(i.facility.type))mask|=1<<k;}
            masks.put(i.hex,mask);
        }
        return Collections.unmodifiableMap(masks);
    }
    private static boolean wallType(Item i){return i.facility!=null&&(i.facility.type.equals("military/EARTH_WALL")||i.facility.type.equals("military/STONE_WALL"));}
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
