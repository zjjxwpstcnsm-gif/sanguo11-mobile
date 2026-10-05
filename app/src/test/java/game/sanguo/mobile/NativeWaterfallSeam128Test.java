package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;

/** Expanded GLB corners must retain identical positions after terrain correction. */
public final class NativeWaterfallSeam128Test {
    record Point(int x,int y,int z) {}
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);
        var snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);
        Class<?> type=Class.forName("game.sanguo.mobile.Vegetation$Batch");
        var constructor=type.getDeclaredConstructor(MapSceneSnapshot.Ground.class);constructor.setAccessible(true);
        var cascade=type.getDeclaredMethod("cascade",Hex.class,Set.class,SceneMesh.class,float.class);cascade.setAccessible(true);
        var finish=type.getDeclaredMethod("mesh",float.class,float.class,float.class);finish.setAccessible(true);
        int comparisons=0;
        // v127 assets are intentionally retained byte-for-byte. They exercise the
        // precise duplicated-corner regression regardless of v128 art changes.
        for(int[] anchor:new int[][]{{147,59},{31,183},{119,146},{66,58}})for(int lod=0;lod<2;lod++){
            String id=anchor[0]==66?"fall-hukou":anchor[0]==31?"fall-wide":"fall-narrow";
            SceneMesh model;try(var input=Files.newInputStream(Path.of("app/src/main/assets/3d/field/v127/"+id+"-lod"+lod+".glb"))){model=SiteGlb.read(input);}
            Object batch=constructor.newInstance(snapshot.ground);Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(anchor[0],anchor[1]));
            cascade.invoke(batch,h,Vegetation.exclusions(snapshot),model,anchor[0]==66?1.15f:2.1f);
            SceneMesh baked=(SceneMesh)finish.invoke(batch,0f,0f,4f);
            if(baked.vertices.length!=model.vertices.length)throw new AssertionError("real waterfall emitted at "+Arrays.toString(anchor));
            Map<Point,Float> y=new HashMap<>();
            for(int k=0;k<model.vertices.length;k+=7){
                Point key=new Point(Float.floatToIntBits(model.vertices[k]),Float.floatToIntBits(model.vertices[k+1]),Float.floatToIntBits(model.vertices[k+2]));
                Float previous=y.putIfAbsent(key,baked.vertices[k+1]);
                if(previous!=null){comparisons++;if(Float.floatToIntBits(previous)!=Float.floatToIntBits(baked.vertices[k+1]))throw new AssertionError("expanded waterfall triangle tears at "+Arrays.toString(anchor)+" LOD"+lod+" delta="+Math.abs(previous-baked.vertices[k+1]));}
            }
        }
        if(comparisons<100||!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("missing seams or authority changed");
        System.out.println("PASS WATERFALL_SEAM128 duplicated-corner comparisons="+comparisons+" authority unchanged");
    }
}
