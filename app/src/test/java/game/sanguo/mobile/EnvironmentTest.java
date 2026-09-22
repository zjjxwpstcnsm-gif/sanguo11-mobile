package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

public final class EnvironmentTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void frames(SceneMesh m){
        check(m.tangents!=null&&m.tangents.length==m.vertices.length/7*4,"complete tangent frames");
        for(int i=0;i<m.tangents.length;i+=4){
            float len=0;for(int k=0;k<4;k++){float v=m.tangents[i+k];check(Float.isFinite(v),"finite frame");len+=v*v;}
            check(Math.abs(len-1)<.001,"unit quaternion");
        }
        // Compare decoded normal with area-weighted geometry; this also checks posed joint frames.
        float[] saved=m.tangents.clone();m.generateTangents();
        for(int i=0;i<saved.length;i++)check(Math.abs(saved[i]-m.tangents[i])<.001,"frame follows geometry");
    }
    public static void main(String[] args)throws Exception{
        int meshes=0;
        for(String folder:new String[]{"sites","field"})try(var paths=Files.list(Path.of("app/src/main/assets/3d",folder))){
            for(Path path:paths.filter(p->p.toString().endsWith(".glb")).toList())try(var in=Files.newInputStream(path)){frames(SiteGlb.read(in));meshes++;}
        }
        FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
        for(String model:new String[]{"unit-SPEAR-lod0","unit-CAVALRY-lod0","unit-WARSHIP-lod0","unit-CATAPULT-lod0"}){
            SceneMesh rest=assets.pose(model,"walk",0,1),pose=assets.pose(model,"walk",3,1);frames(pose);
            check(!Arrays.equals(rest.tangents,pose.tangents),"animated normals differ "+model);
        }
        // Known outward-facing horizontal triangle: +Y normal encoded by quaternion.
        SceneMesh up=new SceneMesh(new float[]{0,0,0,1,1,1,1, 0,0,1,1,1,1,1, 1,0,0,1,1,1,1},new int[]{0,1,2},0,0,1);
        up.generateTangents();check(Math.abs(up.tangents[0]+.7071068f)<.0001,"up facing frame");
        World w=new World(24,24,"甲","乙");for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.FOREST);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);byte[] before=g.terrain.clone();
        List<SceneMesh> forest=Vegetation.build(g,Collections.emptySet(),Collections.emptyList(),assets.mesh("tree-lod0"),assets.mesh("tree-lod1"),assets.mesh("tree-upland-lod0"),assets.mesh("tree-upland-lod1"));
        for(SceneMesh m:forest){frames(m);frames(m.distant);check(m.vegetation&&m.distant.vegetation,"both forest LODs use forest shadow budget");check(m.radius==m.distant.radius&&m.x==m.distant.x&&m.z==m.distant.z,"same tree centers at LOD");}
        Set<Hex> removed=new HashSet<>();removed.add(new Hex(12,12));
        List<SceneMesh> excluded=Vegetation.build(g,removed,forest,assets.mesh("tree-lod0"),assets.mesh("tree-lod1"),assets.mesh("tree-upland-lod0"),assets.mesh("tree-upland-lod1"));
        check(!Vegetation.eligible(g,removed,new Hex(12,12)),"site footprint excluded");
        check(!Vegetation.eligible(g,removed,new Hex(13,12)),"site margin excluded");
        List<SceneMesh> restored=Vegetation.build(g,Collections.emptySet(),excluded,assets.mesh("tree-lod0"),assets.mesh("tree-lod1"),assets.mesh("tree-upland-lod0"),assets.mesh("tree-upland-lod1"));
        for(int i=0;i<forest.size();i++)check(Arrays.equals(forest.get(i).vertices,restored.get(i).vertices),"delete restores deterministic tree distribution");
        check(Arrays.equals(before,new MapSceneSnapshot.Ground(w).terrain),"terrain neutral");
        System.out.println("PASS S12 environment: "+checks+" assertions; "+meshes+" authored meshes, posed frames, LOD centers, site edit restore, terrain neutrality");
    }
}
