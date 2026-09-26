package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only national and custom-site mapping. Does not participate in movement or combat. */
final class SiteVisual {
    final String model; final float yaw,scale; final int damage; final List<Hex> cells;
    SiteVisual(World w,World.City c,GridWorldTransform g){
        // Region is derived from actual terrain, never from arbitrary stable entity IDs.
        int mountain=0,water=0;
        Set<Hex> region=new HashSet<>(SiteFootprint.cells(c));
        for(int ring=0;ring<3;ring++)for(Hex h:new ArrayList<>(region))region.addAll(h.neighbors());
        for(Hex h:region)if(w.inside(h)){
            if(w.terrain[h.q][h.r]==World.Terrain.MOUNTAIN)mountain++;
            if(w.army.water(h))water++;
        }
        int variant=mountain>=2?2:water>=2?1:0;
        MapPatch.Appearance appearance=w.visualMap==null?null:w.visualMap.appearances.get(c.id);if(appearance!=null)variant=appearance.variant();
        model=c.kind==World.SiteKind.PORT?"port":c.kind==World.SiteKind.GATE?"gate":"city"+variant;
        scale=c.kind==World.SiteKind.CITY?.96f:1;
        damage=c.defense*3L<c.baseDefense?2:c.defense*3L<c.baseDefense*2L?1:0;
        cells=Collections.unmodifiableList(new ArrayList<>(SiteFootprint.cells(c)));
        float angle=0;
        if(c.kind==World.SiteKind.PORT){
            // Only navigation-authorized water categories; decorative non-navigable water is excluded.
            for(Hex h:c.hex.neighbors())if(w.army.water(h)){
                angle=(float)Math.atan2(g.x(h)-g.x(c.hex),g.z(h)-g.z(c.hex));break;
            }
        }else if(c.kind==World.SiteKind.GATE){
            // Gateway local +Z follows a real open approach, with an opposite exit
            // preferred. Scoring mountain sums lost diagonal mountain-pass axes.
            float best=-Float.MAX_VALUE;
            for(Hex h:c.hex.neighbors())if(openApproach(w,h)){
                float dx=g.x(h)-g.x(c.hex),dz=g.z(h)-g.z(c.hex),length=(float)Math.hypot(dx,dz);
                float score=0;
                for(Hex other:c.hex.neighbors())if(openApproach(w,other)){
                    float ox=g.x(other)-g.x(c.hex),oz=g.z(other)-g.z(c.hex);
                    float dot=(dx*ox+dz*oz)/(length*(float)Math.hypot(ox,oz));
                    score=Math.max(score,-dot);
                }
                if(score>best){best=score;angle=(float)Math.atan2(dx,dz);}
            }
        }
        yaw=appearance==null?angle:(float)Math.toRadians(appearance.degrees());
    }
    private static boolean openApproach(World w,Hex h){
        if(!w.inside(h))return false;
        World.Terrain t=w.terrain[h.q][h.r];
        return t!=World.Terrain.MOUNTAIN&&t!=World.Terrain.VOID&&!w.army.water(h)
            &&t!=World.Terrain.NON_NAVIGABLE_WATER&&t!=World.Terrain.SHALLOWS;
    }
    static boolean navigable(World.Terrain t){return t==World.Terrain.WATER||t==World.Terrain.SEA;}
    static int lod(float span,int previous){
        if(previous==0)return span>19?1:0;
        if(previous==2)return span<42?1:2;
        return span<15?0:span>48?2:1;
    }
    static SceneMesh fallback(int kind){return SceneMesh.proxy(kind,0xff918a75);}
}
