package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
/** Water appearance continuity is independent of executable navigation. */
public final class Reference60ProjectionTest {
    static int checks;
    static void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
    public static void main(String[] a)throws Exception {
        World.Terrain[] wet={World.Terrain.WATER,World.Terrain.NON_NAVIGABLE_WATER,World.Terrain.SEA,World.Terrain.SHALLOWS};
        for(World.Terrain from:wet)for(World.Terrain to:wet)for(int d=0;d<6;d++) {
            World w=new World(7,7);for(World.Terrain[] c:w.terrain)Arrays.fill(c,World.Terrain.MOUNTAIN);
            int q=3+TerrainConnections.DQ[d],r=3+TerrainConnections.DR[d];w.terrain[3][3]=from;w.terrain[q][r]=to;
            check((TerrainConnections.mask(w,3,3)&1<<d)!=0,"wet appearances join without fake shoreline");
            check((TerrainConnections.mask(w,q,r)&1<<((d+3)%6))!=0,"reciprocal wet appearance edge");
            w.terrain[q][r]=World.Terrain.VOID;check((TerrainConnections.mask(w,3,3)&1<<d)==0,"VOID is not painted as water");
        }
        for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}) {
            World w=ScenarioCatalog.load(id,0,590L);int joins=0;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)if(w.terrain[q][r]==World.Terrain.NON_NAVIGABLE_WATER){
                Hex h=new Hex(q,r);check(w.inside(h)&&TerrainArt.ground(w.terrain[q][r])==World.Terrain.WATER,"Q visible and selectable without new art ABI");
                for(int d=0;d<6;d++){int nq=q+TerrainConnections.DQ[d],nr=r+TerrainConnections.DR[d];if(!w.sourceInside(new Hex(nq,nr)))continue;
                    if(TerrainConnections.water(w.terrain[nq][nr])){check((TerrainConnections.mask(w,q,r)&1<<d)!=0,"actual Q/W coast seam joins");joins++;}}
            }
            System.out.println("REFERENCE60 WATER VISUAL JOINS "+id+" "+joins+"; no movement permission inferred");
        }
        System.out.println("REFERENCE60 PROJECTION PASS: "+checks);
    }
}
