package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only national and custom-site mapping. Does not participate in movement or combat. */
final class SiteVisual {
    final String model; final float yaw,scale; final int damage; final List<Hex> cells;
    SiteVisual(World w,World.City c,GridWorldTransform g){
        int variant=Math.floorMod(c.id,3);int mountain=0,water=0;
        for(Hex h:c.hex.neighbors())if(w.inside(h)){if(w.terrain[h.q][h.r]==World.Terrain.MOUNTAIN)mountain++;if(w.army.water(h))water++;}
        if(mountain>=2)variant=2;else if(water>=2)variant=1;
        MapPatch.Appearance appearance=w.visualMap==null?null:w.visualMap.appearances.get(c.id);if(appearance!=null)variant=appearance.variant();
        model=c.kind==World.SiteKind.PORT?"port":c.kind==World.SiteKind.GATE?"gate":"city"+variant;
        scale=c.kind==World.SiteKind.CITY?(.94f+Math.floorMod(c.id,3)*.025f):1;
        damage=c.defense*3L<c.baseDefense?2:c.defense*3L<c.baseDefense*2L?1:0;
        cells=Collections.unmodifiableList(new ArrayList<>(SiteFootprint.cells(c)));
        float angle=0;
        if(c.kind==World.SiteKind.PORT){
            // Only navigation-authorized water categories; decorative non-navigable water is excluded.
            for(Hex h:c.hex.neighbors())if(w.army.water(h)){
                angle=(float)Math.atan2(g.x(h)-g.x(c.hex),g.z(h)-g.z(c.hex));break;
            }
        }else if(c.kind==World.SiteKind.GATE){
            float dx=0,dz=0;for(Hex h:c.hex.neighbors())if(w.inside(h)&&w.terrain[h.q][h.r]==World.Terrain.MOUNTAIN){dx+=Math.abs(g.x(h)-g.x(c.hex));dz+=Math.abs(g.z(h)-g.z(c.hex));}
            if(dz>dx)angle=(float)Math.PI/2;
        }
        yaw=appearance==null?angle:(float)Math.toRadians(appearance.degrees());
    }
    static boolean navigable(World.Terrain t){return t==World.Terrain.WATER||t==World.Terrain.SEA;}
    static int lod(float span,int previous){
        if(previous==0)return span>19?1:0;
        if(previous==2)return span<42?1:2;
        return span<15?0:span>48?2:1;
    }
    static SceneMesh fallback(int kind){return SceneMesh.proxy(kind,0xff918a75);}
}
