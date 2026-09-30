package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** Actual GLBs in the national stream, including the normal span15 far LOD. */
public final class NativeWaterfall127Test {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        World world=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(world);
        var snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(world),world,null,-1);
        var excluded=Vegetation.exclusions(snapshot);var assets=new FieldAssets(n->Files.newInputStream(Path.of("app/src/main/assets/3d/field",n)));
        check(LandscapeLandmarks.ALL.size()==5,"shared discovery targets include all national landmarks");
        for(var entry:LandscapeLandmarks.ALL){
            if(entry.cascade()<0)continue;Hex at=at(world,entry);var g=snapshot.ground;
            check(Vegetation.cascadeRegion(g,at)==entry.cascade(),"UI target is the actual production fall");
            for(int span:new int[]{6,15}){
                var window=new SceneMesh.TerrainWindow(g.grid.x(at),g.grid.z(at),5,5,span);
                var meshes=Vegetation.buildWindow(g,excluded,List.of(),assets,window);
                int water=0,foam=0;float lip=Float.POSITIVE_INFINITY,foot=-Float.POSITIVE_INFINITY;
                for(var mesh:meshes){
                    for(int i=0;i<mesh.uv.length;i+=2){
                        float u=mesh.uv[i],v=mesh.uv[i+1];int k=i/2*7;
                        boolean isWater=entry.cascade()==3?u>.375f&&u<.5f:u>.625f&&u<.75f;
                        boolean isFoam=u>.5f&&u<.625f&&v>.65f;
                        if(isWater){water++;if(Math.abs(v-(.06f+.88f*.34f))<.0001)lip=Math.min(lip,mesh.vertices[k+1]);if(Math.abs(v-(.06f+.88f*.50f))<.0001)foot=Math.max(foot,mesh.vertices[k+1]);}
                        if(isFoam)foam++;
                    }
                    for(int i=0;i<mesh.indices.length;i+=3){
                        int A=mesh.indices[i],B=mesh.indices[i+1],C=mesh.indices[i+2];float u=mesh.uv[A*2];
                        boolean landscape=(u>.375f&&u<.625f)||(u>.625f&&u<.75f);
                        if(!landscape)continue;
                        for(int a=0;a<=6;a++)for(int b=0;b<=6-a;b++){
                            float U=a/6f,V=b/6f,W=1-U-V;
                            float x=mesh.vertices[A*7]*U+mesh.vertices[B*7]*V+mesh.vertices[C*7]*W;
                            float z=mesh.vertices[A*7+2]*U+mesh.vertices[B*7+2]*V+mesh.vertices[C*7+2]*W;
                            float y=mesh.vertices[A*7+1]*U+mesh.vertices[B*7+1]*V+mesh.vertices[C*7+1]*W;
                            Hex h=g.grid.cell(x,z);check(g.valid(h),"whole fall/ledge/foam footprint inside map");
                            int terrain=g.terrain[h.r*g.width+h.q];check(terrain==World.Terrain.MOUNTAIN.ordinal()||terrain==World.Terrain.NON_NAVIGABLE_WATER.ordinal(),"no traversable cell obscured");
                            check(!excluded.contains(h)&&!g.bases.contains(h),"no site/facility obstruction");
                            check(y>=g.surface.meshHeight(x,z)+.029f,"whole face above canonical ground");
                        }
                    }
                }
                check(water>0&&foam>0,"fall and impact foam actually emitted at ordinary and near zoom");
                System.out.println("DROP127 "+entry.label()+" span="+span+" lip="+lip+" foot="+foot);
                check(Float.isFinite(lip)&&Float.isFinite(foot)&&lip-foot>.25f,"steep water drop survives terrain fitting");
                check(meshes.equals(Vegetation.buildWindow(g,excluded,meshes,assets,window)),"immutable cache reuse");
                System.out.println("WATERFALL127 "+entry.label()+" span="+span+" water="+water+" foam="+foam+" drop="+(lip-foot));
            }
        }
        check(Arrays.equals(before,SaveCodec.encode(world)),"full authority and rule RNG unchanged");
        World custom=SaveCodec.decode(before);custom.customMapId="edited-national";var altered=new MapSceneSnapshot.Ground(custom);
        check(!altered.originalNational,"custom edited national map is not populated with authored landmarks");
        check(!snapshot.ground.matches(custom),"map identity invalidates terrain reuse on custom switch");
        for(var entry:LandscapeLandmarks.ALL)check(Vegetation.cascadeRegion(altered,at(custom,entry))<0,"custom landmarks absent");
        System.out.println("PASS WATERFALL127 checks="+checks+" actual near/far drop, foam, faces, cache, full save and discovery target");
    }
    static Hex at(World world,LandscapeLandmarks.Entry entry){return MapCoordinates.fromNationalSource(world,new game.sanguo.core.map.SourceGridCoord(entry.x(),entry.y()));}
}
