package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;

/** Real national map: generated cascades stay on non-enterable corridors. */
public final class NativeFeedback125Test {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        try(java.io.DataInputStream in=new java.io.DataInputStream(Files.newInputStream(Path.of("app/src/main/assets/3d/field/v125/scenery-atlas.etc2")))){
            check(in.readInt()==0x45544332,"production ETC2 counterpart exists with proper header");int width=in.readInt(),height=in.readInt(),levels=in.readInt();
            check(width>0&&height>0&&levels==32-Integer.numberOfLeadingZeros(Math.max(width,height)),"scenery mip dimensions");
            for(int level=0;level<levels;level++){int bytes=in.readInt();check(bytes==((Math.max(1,width>>level)+3)/4)*((Math.max(1,height>>level)+3)/4)*8,"real compressed mip payload length");check(in.readNBytes(bytes).length==bytes,"all compressed levels present");}
            check(in.read()==-1,"compressed atlas has no trailing payload");
        }
        World w=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot s=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);
        FieldAssets a=new FieldAssets(n->Files.newInputStream(Path.of("app/src/main/assets/3d/field",n)));
        for(int[] source:new int[][]{{147,59},{31,183}}){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(source[0],source[1]));
            float x=s.ground.grid.x(h),z=s.ground.grid.z(h);
            List<SceneMesh> near=Vegetation.buildWindow(s.ground,Vegetation.exclusions(s),List.of(),a,new SceneMesh.TerrainWindow(x,z,5,5,8));
            int foam=0,far=0;
            for(SceneMesh m:near){
                for(int i=0;i<m.uv.length;i+=2)if(m.uv[i]>.625f&&m.uv[i]<.75f){
                    foam++;Hex at=s.ground.grid.cell(m.vertices[i/2*7],m.vertices[i/2*7+2]);
                    check(s.ground.valid(at),"cascade remains inside original map");
                    int t=s.ground.terrain[at.r*s.ground.width+at.q];
                    check(t==World.Terrain.MOUNTAIN.ordinal()||t==World.Terrain.NON_NAVIGABLE_WATER.ordinal(),"does not obscure traversable cell");
                    check(m.vertices[i/2*7+1]>=s.ground.surface.meshHeight(m.vertices[i/2*7],m.vertices[i/2*7+2]),"water does not disappear below canonical surface");
                }
                for(int i=0;i<m.distant.uv.length;i+=2)if(m.distant.uv[i]>.625f&&m.distant.uv[i]<.75f)far++;
                for(SceneMesh waterMesh:new SceneMesh[]{m,m.distant})for(int i=0;i<waterMesh.indices.length;i+=3){
                    int a0=waterMesh.indices[i],a1=waterMesh.indices[i+1],a2=waterMesh.indices[i+2];
                    if(waterMesh.uv[a0*2]<=.625f||waterMesh.uv[a0*2]>=.75f)continue;
                    for(int p=0;p<=6;p++)for(int q=0;q<=6-p;q++){
                        float u=p/6f,t=q/6f,v=1-u-t;
                        float px=waterMesh.vertices[a0*7]*u+waterMesh.vertices[a1*7]*t+waterMesh.vertices[a2*7]*v;
                        float pz=waterMesh.vertices[a0*7+2]*u+waterMesh.vertices[a1*7+2]*t+waterMesh.vertices[a2*7+2]*v;
                        float py=waterMesh.vertices[a0*7+1]*u+waterMesh.vertices[a1*7+1]*t+waterMesh.vertices[a2*7+1]*v;
                        check(py>=s.ground.surface.meshHeight(px,pz)+.029f,"ribbon face stays above terrain ridge between corners");
                    }
                }
            }
            check(foam>0&&far>0&&far<foam,"near/far cascade both present with bounded lower detail");
            check(Arrays.equals(before,SaveCodec.encode(w)),"landscape build preserves full authority/RNG");
            var reused=Vegetation.buildWindow(s.ground,Vegetation.exclusions(s),near,a,new SceneMesh.TerrainWindow(x,z,5,5,8));
            check(near.equals(reused),"streaming cache retains immutable landscape meshes");
            System.out.println("CASCADE source="+Arrays.toString(source)+" near="+foam+" far="+far+" chunks="+near.size());
        }
        World custom=new World(20,20,"one","two");custom.terrain[10][10]=World.Terrain.NON_NAVIGABLE_WATER;
        var g=new MapSceneSnapshot.Ground(custom);check(Vegetation.cascadeRegion(g,new Hex(10,10))==-1,"custom map never inherits regional waterfall coordinates");
        System.out.println("PASS LANDSCAPE125 checks="+checks+" national terrain, full save/RNG, exact cell safety and cache checks");
    }
}
