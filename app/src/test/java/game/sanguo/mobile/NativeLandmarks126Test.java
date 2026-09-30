package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;

/** Real national projection, both LODs, full save/RNG and footprint rejection. */
public final class NativeLandmarks126Test {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        List<String> requested=new ArrayList<>();
        FieldAssets assets=new FieldAssets(n->{requested.add(n);return Files.newInputStream(Path.of("app/src/main/assets/3d/field",n));});
        for(String id:new String[]{"wall-earth","beacon-han","cliff-sandstone","cliff-granite","cliff-karst","cascade-hukou"}){
            var near=assets.mesh(id+"-lod0");var far=assets.mesh(id+"-lod1");
            check(near.indices.length>far.indices.length&&near.indices.length<(id.equals("cascade-hukou")?2250:9000),"bounded actual near/far GLB "+id);
            check(near.uv!=null&&near.tangents!=null,"texture and authored normals "+id);
        }
        World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot s=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);
        for(int[] source:new int[][]{{66,58},{92,15},{147,59},{31,183}}){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(source[0],source[1]));
            var window=new SceneMesh.TerrainWindow(s.ground.grid.x(h),s.ground.grid.z(h),5,5,8);
            var meshes=Vegetation.buildWindow(s.ground,Vegetation.exclusions(s),List.of(),assets,window);
            int water=0,wall=0;long bytes=0;
            for(var m:meshes){
                bytes+=4L*(m.vertices.length+m.indices.length+m.uv.length+m.tangents.length);
                for(SceneMesh lod:new SceneMesh[]{m,m.distant}){
                    for(int i=0;i<lod.vertices.length;i+=7){
                        check(Float.isFinite(lod.vertices[i+1]),"finite height");
                        if(lod.vertices[i+3]==.98f&&lod.vertices[i+4]==.87f&&lod.vertices[i+5]==.69f){
                            Hex at=s.ground.grid.cell(lod.vertices[i],lod.vertices[i+2]);
                            check(s.ground.valid(at)&&s.ground.terrain[at.r*s.ground.width+at.q]==World.Terrain.MOUNTAIN.ordinal(),"wall only on blocked mountain");wall++;
                        }
                    }
                    for(int i=0;i<lod.indices.length;i+=3){
                        int A=lod.indices[i],B=lod.indices[i+1],C=lod.indices[i+2];
                        if(lod.uv[A*2]<=.375f||lod.uv[A*2]>=.5f)continue;
                        water++;
                        for(int p=0;p<=6;p++)for(int q=0;q<=6-p;q++){
                            float u=p/6f,t=q/6f,v=1-u-t;
                            float x=lod.vertices[A*7]*u+lod.vertices[B*7]*t+lod.vertices[C*7]*v;
                            float z=lod.vertices[A*7+2]*u+lod.vertices[B*7+2]*t+lod.vertices[C*7+2]*v;
                            float y=lod.vertices[A*7+1]*u+lod.vertices[B*7+1]*t+lod.vertices[C*7+1]*v;
                            Hex at=s.ground.grid.cell(x,z);check(s.ground.valid(at),"Hukou stays inside original map");
                            int terrain=s.ground.terrain[at.r*s.ground.width+at.q];
                            check(terrain==World.Terrain.MOUNTAIN.ordinal()||terrain==World.Terrain.NON_NAVIGABLE_WATER.ordinal(),"Hukou does not cover traversable cells");
                            check(y>=s.ground.surface.meshHeight(x,z)+.029f,"water face above canonical terrain");
                        }
                    }
                }
            }
            if(source[0]==66)check(water>0,"Hukou really emitted in production landscape batch");
            if(source[0]==92)check(wall>0,"northern wall really emitted in production landscape batch");
            check(Arrays.equals(before,SaveCodec.encode(w)),"all landscape work preserves authority and RNG");
            check(meshes.equals(Vegetation.buildWindow(s.ground,Vegetation.exclusions(s),meshes,assets,window)),"cache identity retained");
            System.out.println("LANDMARK source="+Arrays.toString(source)+" waterTriangles="+water+" wallVertices="+wall+" nearCPUBytes="+bytes);
        }
        World custom=SaveCodec.decode(before);custom.mapId="custom-national-lookalike";var g=new MapSceneSnapshot.Ground(custom);
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);check(!Vegetation.wallRegion(g,h)&&Vegetation.cascadeRegion(g,h)<0&&Vegetation.cliffRegion(g,h)==null,"no regional landmarks on custom map");
        }
        System.out.println("PASS LANDMARK126 checks="+checks+" actual models, non-enterable footprints, LOD cache, full SaveCodec/RNG");
    }
}
