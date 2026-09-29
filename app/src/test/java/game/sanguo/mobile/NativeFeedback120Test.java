package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Same production forest before/after a legal movement, plus actual mesh projection. */
public final class NativeFeedback120Test {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        boolean baseline=args.length>0&&args[0].equals("baseline");
        World world=CombatSceneFixture.world("counter");
        for(World.Terrain[] row:world.terrain)Arrays.fill(row,World.Terrain.FOREST);
        for(World.City c:world.cities)for(Hex h:SiteFootprint.cells(c))world.terrain[h.q][h.r]=World.Terrain.PLAIN;
        world.terrain[13][6]=World.Terrain.ROAD;
        byte[] before=SaveCodec.encode(world);
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
        MapSceneSnapshot initial=new MapSceneSnapshot(ground,world,world.unit(1).hex,1);
        FieldAssets assets=new FieldAssets(n->new FileInputStream("app/src/main/assets/3d/field/"+n));
        SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(ground.grid.x(8,8),ground.grid.z(8,8),8,8,8);
        Set<Hex> excluded=Vegetation.exclusions(initial);
        List<SceneMesh> trees=Vegetation.buildWindow(ground,excluded,List.of(),assets,window);
        check(Arrays.equals(before,SaveCodec.encode(world)),"forest creation is read-only, including RNG");
        int atStart=Vegetation.placements(ground,excluded,new Hex(8,8)).size();
        MarchOrders.Plan plan=world.marches.previewMove(1,new Hex(7,8));
        check(plan.valid()&&world.marches.execute(plan).ok,"legal forest entry through original rule command");
        byte[] moved=SaveCodec.encode(world);
        MapSceneSnapshot after=new MapSceneSnapshot(ground,world,new Hex(7,8),1);
        Set<Hex> nextExcluded=Vegetation.exclusions(after);
        List<SceneMesh> next=Vegetation.buildWindow(ground,nextExcluded,trees,assets,window);
        int atEnd=Vegetation.placements(ground,nextExcluded,new Hex(7,8)).size();
        if(baseline){
            check(!excluded.equals(nextExcluded),"v119 reproduces movement invalidating forest mask");
            check(atStart==0&&atEnd==0,"v119 reproduces occupied forest cleared");
        }else {
            check(excluded.equals(nextExcluded),"moving unit cannot change permanent forest footprint");
            check(atStart>0&&atEnd>0,"both occupied cells retain actual trees");
            check(trees.size()==next.size(),"forest chunk count stable");
            for(int i=0;i<trees.size();i++)check(trees.get(i)==next.get(i),"movement reuses exact CPU forest mesh (no erase or re-upload)");
            check(excluded.containsAll(ground.bases),"fixed site footprints remain protected");
            check(Vegetation.placements(ground,nextExcluded,new Hex(13,6)).isEmpty(),"road stays clear");
        }
        check(Arrays.equals(moved,SaveCodec.encode(world)),"post-move render leaves full save/RNG untouched");
        // Projection uses the same individual model, formation offsets, contact shear,
        // yaw and scale as the production GPU shader and ScenePicking.
        SceneCamera camera=new SceneCamera();camera.width=1080;camera.height=1920;camera.span=4;
        UnitVisual unit=new UnitVisual(world,world.unit(1));
        float x=ground.grid.x(world.unit(1).hex),z=ground.grid.z(world.unit(1).hex);camera.x=x;camera.z=z;
        for(int lod:new int[]{0,1,2})for(float yaw:new float[]{0,.7f,2.1f})for(float azimuth:new float[]{0,85,190}){
            camera.yaw=azimuth;
            UnitFormation formation=new UnitFormation();formation.sample(unit,10000,false,lod,ground,x,z,yaw,1);
            SceneMesh model=assets.pose(FieldAssets.unit(unit,false,lod),"idle",0,1);
            float[] points=UnitSilhouette.project(model,camera,formation,x,z,yaw,1);
            check(points.length==model.vertices.length/7*formation.count*2,"all representative vertices, no invented shape");
            for(float f:points)check(Float.isFinite(f),"finite projected model");
            float[] same=UnitSilhouette.project(model,camera,formation,x,z,yaw,1);check(Arrays.equals(points,same),"deterministic stationary ghost");
            float shift=37;camera.x+=(float)camera.rightX()*shift/camera.pixels();camera.z-=(float)camera.backX()*shift/camera.pixels();
            float[] shifted=UnitSilhouette.project(model,camera,formation,x,z,yaw,1);
            for(int i=0;i<points.length;i+=2){check(Math.abs(shifted[i]-points[i]+shift)<.003f,"camera-aligned horizontal projection");check(Math.abs(shifted[i+1]-points[i+1])<.003f,"camera-aligned vertical projection");}
            camera.x=x;camera.z=z;
        }
        check(Arrays.equals(moved,SaveCodec.encode(world)),"silhouette projection is read-only");
        System.out.println("PASS FEEDBACK120 "+(baseline?"baseline bug reproduced":"candidate")+" checks="+checks+" occupiedPlacements="+atStart+"/"+atEnd+" chunks="+trees.size());
    }
}
