package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.nio.file.*;
import java.util.*;

/** Actual v128 models in the normal national stream, not a stand-alone gallery. */
public final class NativeLandmarks128Test {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Hex at(World w,int x,int y){return MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));}
    public static void main(String[] args)throws Exception{
        List<String> requested=new ArrayList<>();
        FieldAssets assets=new FieldAssets(name->{requested.add(name);return Files.newInputStream(Path.of("app/src/main/assets/3d/field",name));});
        for(String id:new String[]{"fall-narrow","fall-wide","fall-hukou","wall-earth","beacon-han","cliff-sandstone","cliff-granite","cliff-karst","shore-reeds","shore-rock"}){
            SceneMesh near=assets.mesh(id+"-lod0"),far=assets.mesh(id+"-lod1");
            check(near.indices.length>far.indices.length,"real reduced LOD "+id);
            check(near.indices.length<9000,"mobile triangle budget "+id);
            check(near.authoredTangentFrame&&far.authoredTangentFrame,"Blender normals imported "+id);
            check(requested.contains("v129/"+id+"-lod0.glb")&&requested.contains("v129/"+id+"-lod1.glb"),"runtime routes to new production asset "+id);
        }
        var atlas=javax.imageio.ImageIO.read(Path.of("app/src/main/assets/3d/field/v128/scenery-atlas.png").toFile());
        var old=javax.imageio.ImageIO.read(Path.of("app/src/main/assets/3d/field/v127/scenery-atlas.png").toFile());
        check(atlas.getWidth()==512&&atlas.getHeight()==64,"same opaque atlas budget");
        for(int panel:new int[]{1,2,6,7})for(int x=panel*64;x<(panel+1)*64;x++)for(int y=0;y<64;y++)check(atlas.getRGB(x,y)==old.getRGB(x,y),"unrelated retained atlas panel "+panel);
        check((atlas.getRGB(288,12)&255)<110&&(atlas.getRGB(288,52)&255)>120,"PNG rock/foam direction matches unchanged runtime upload");
        World world=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(world);
        var snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(world),world,null,-1);var g=snapshot.ground;
        var excluded=Vegetation.exclusions(snapshot);Hex lake=at(world,175,107);
        check(g.terrain[lake.r*g.width+lake.q]==World.Terrain.NON_NAVIGABLE_WATER.ordinal(),"Taihu anchor is existing blocked water");
        check(LandscapeLandmarks.ALL.stream().filter(e->e.cascade()==-2).count()==1,"normal menu has one lake entry");
        for(int span:new int[]{6,15}){
            requested.clear();var window=new SceneMesh.TerrainWindow(g.grid.x(lake),g.grid.z(lake),6,6,span);
            // A fresh loader records the actual shore asset requests for each LOD.
            FieldAssets fresh=new FieldAssets(name->{requested.add(name);return Files.newInputStream(Path.of("app/src/main/assets/3d/field",name));});
            var meshes=Vegetation.buildWindow(g,excluded,List.of(),fresh,window);
            check(requested.stream().anyMatch(n->n.startsWith("v129/shore-reeds")),"reeds requested by production window at span "+span);
            check(requested.stream().anyMatch(n->n.startsWith("v129/shore-rock")),"shore rocks requested by production window at span "+span);
            int reedVertices=0,stoneVertices=0;
            for(var m:meshes){
                for(int i=0;i<m.vertices.length;i+=7){
                    Hex h=g.grid.cell(m.vertices[i],m.vertices[i+2]);
                    if(!g.valid(h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.NON_NAVIGABLE_WATER.ordinal())continue;
                    int panel=(int)(m.uv[i/7*2]*8);
                    if(panel!=0&&panel!=6)continue;
                    check(Vegetation.taihuRegion(g,h)&&!excluded.contains(h)&&!g.bases.contains(h),"all wet-side shore dressing stays in original lake away from entities");
                    check(m.vertices[i+1]-g.surface.meshHeight(m.vertices[i],m.vertices[i+2])<.4f,"shore dressing remains low and readable");
                    if(panel==6)reedVertices++;else stoneVertices++;
                }
            }
            check(reedVertices>0&&stoneVertices>0,"both actual shore mesh families emitted, span "+span);
            check(meshes.equals(Vegetation.buildWindow(g,excluded,meshes,fresh,window)),"immutable lake window reused");
            System.out.println("LANDMARK128 lake span="+span+" reeds="+reedVertices+" stones="+stoneVertices);
        }
        World custom=SaveCodec.decode(before);custom.customMapId="custom-lake";var customGround=new MapSceneSnapshot.Ground(custom);
        for(int y=103;y<=111;y++)for(int x=171;x<=179;x++)check(!Vegetation.taihuRegion(customGround,at(custom,x,y)),"custom lake never inherits national dressing");
        check(Arrays.equals(before,SaveCodec.encode(world)),"entire save and rule RNG byte-identical");
        System.out.println("PASS LANDMARK128 checks="+checks+" v128 asset routing, lake shore bounds/LOD/cache, atlas retention and complete authority");
    }
}
