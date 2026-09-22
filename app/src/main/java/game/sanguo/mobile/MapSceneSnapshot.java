package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Immutable presentation values. Never publishes a World to mesh workers or Filament. */
final class MapSceneSnapshot {
    static final class Ground {
        final int width,height; final byte[] terrain; final GridWorldTransform grid;
        Ground(World w) {
            width=w.width;height=w.height;grid=new GridWorldTransform(w.sourceMapWidth>0?(w.height-1)/2:0,w.columnStaggered);
            terrain=new byte[width*height];
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)terrain[r*width+q]=(byte)(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal());
        }
        boolean matches(World w){
            if(width!=w.width||height!=w.height||grid.staggered!=w.columnStaggered||grid.offset!=(w.sourceMapWidth>0?(w.height-1)/2:0))return false;
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)if(terrain[r*width+q]!=(w.inside(new Hex(q,r))?w.terrain[q][r].ordinal():World.Terrain.VOID.ordinal()))return false;
            return true;
        }
        boolean valid(Hex h){return h!=null&&h.q>=0&&h.r>=0&&h.q<width&&h.r<height&&terrain[h.r*width+h.q]!=World.Terrain.VOID.ordinal();}
    }
    static final class Item {
        final String key,label; final Hex hex; final int kind,color;
        Item(String key,String label,Hex hex,int kind,int color){this.key=key;this.label=label;this.hex=hex;this.kind=kind;this.color=color;}
    }
    final Ground ground; final List<Item> items; final Hex selected; final Set<Hex> reachable,siege,coverage;
    MapSceneSnapshot(Ground ground,World w,Hex selected,int moving) {
        this.ground=ground;this.selected=selected;List<Item> list=new ArrayList<>();
        for(World.City c:w.cities)list.add(new Item("site:"+c.id,c.name,c.hex,c.kind==World.SiteKind.CITY?0:c.kind==World.SiteKind.PORT?1:2,FactionColors.color(w,c.owner)));
        for(World.Unit u:w.fieldUnits())list.add(new Item("unit:"+u.id,u.weapon.label,u.hex,3,FactionColors.color(w,u.owner)));
        for(Domestic.Facility f:w.domestic.facilities)list.add(new Item("domestic:"+f.id,f.kind.label,f.hex,4,0xffad9b69));
        for(War.Structure s:w.war.structures())list.add(new Item("structure:"+s.id,s.kind.label,s.hex,5,FactionColors.color(w,s.owner)));
        items=Collections.unmodifiableList(list);
        reachable=Collections.unmodifiableSet(new HashSet<>(w.orders.marchReachable(w.unit(moving)).keySet()));
        coverage=Collections.unmodifiableSet(new HashSet<>(w.fieldworks.coverage(w.war.at(selected))));
        siege=Collections.unmodifiableSet(new HashSet<>(SiegeOverlay.selected(w,selected).cells));
    }
}
