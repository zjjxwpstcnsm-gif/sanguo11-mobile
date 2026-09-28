package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class ForestDensityTest {
    public static void main(String[] args)throws Exception {
        boolean baseline=args.length>0&&args[0].equals("baseline");
        World w=CombatSceneFixture.world("counter");
        for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.FOREST);
        for(World.City c:w.cities)for(Hex h:SiteFootprint.cells(c))w.terrain[h.q][h.r]=World.Terrain.PLAIN;
        w.terrain[11][8]=World.Terrain.ROAD;byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);
        Set<Hex> excluded=Vegetation.exclusions(snap);int trees=0,shrubs=0,eligible=0;
        for(int r=2;r<w.height-2;r++)for(int q=2;q<w.width-2;q++){
            Hex h=new Hex(q,r);List<Vegetation.Placement> list=Vegetation.placements(snap.ground,excluded,h);
            if(Vegetation.clear(snap.ground,excluded,h,snap.ground.grid.x(h),snap.ground.grid.z(h)))eligible++;
            List<Vegetation117.Placement> old=Vegetation117.placements(snap.ground,excluded,h);
            for(var a:old)if(list.stream().noneMatch(b->a.x==b.x&&a.z==b.z&&a.scale==b.scale&&a.angle==b.angle&&a.family==b.family))throw new AssertionError("old placement removed or changed");
            if(list.size()>(baseline?3:6))throw new AssertionError("candidate bound");
            for(var p:list){
                if(!h.equals(snap.ground.grid.cell(p.x,p.z))||!Vegetation.clear(snap.ground,excluded,h,p.x,p.z))throw new AssertionError("clearance / ownership");
                if(p.family<2)trees++;else if(p.family==2)shrubs++;
            }
        }
        if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("authority/RNG mutation");
        if(!baseline&&trees<eligible*3)throw new AssertionError("forest still sparse: "+trees+" / "+eligible);
        System.out.println("PASS FOREST phase="+(baseline?"baseline":"candidate")+" tree="+trees+" shrub="+shrubs+" clearCells="+eligible+" (same terrain/entities, no road/entry/unit apron relaxation)");
    }
}
