package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** CPU asset/animation/dirty contract; installed native tests report separately. */
public final class FieldAssetsTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void mesh(SceneMesh m){
        check(m.uv!=null&&m.uv.length==m.vertices.length/7*2,"complete UVs");
        check(m.indices.length>0&&m.indices.length%3==0,"actual triangles");
        for(float v:m.vertices)check(Float.isFinite(v),"finite geometry");
        for(float uv:m.uv)check(Float.isFinite(uv)&&uv>=0&&uv<=1,"atlas UV range");
        for(int i:m.indices)check(i>=0&&i<m.vertices.length/7,"index bounds");
    }
    public static void main(String[] args)throws Exception{
        FieldAssets assets=new FieldAssets(name->Files.newInputStream(Paths.get("app/src/main/assets/3d/field",name)));
        World all=SceneFacilityFixture.create();MapSceneSnapshot snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(all),all,null,-1);
        Set<String> variants=new HashSet<>();
        for(MapSceneSnapshot.Item item:snap.items)if(item.facility!=null)for(int lod=0;lod<3;lod++){
            String key=FieldAssets.facility(item.facility,lod);variants.add(key);mesh(assets.mesh(key));
        }
        check(variants.size()==120,"all 40 actual type/level variants times three LODs");
        int rigs=0;
        for(String kind:new String[]{"SPEAR","HALBERD","CROSSBOW","CAVALRY","SWORD","RAM","SIEGE_TOWER","WOODEN_BEAST","CATAPULT","transport","BOAT","TOWER_SHIP","WARSHIP"})for(int lod=0;lod<2;lod++){
            String model="unit-"+kind+"-lod"+lod;rigs++;SceneMesh rest=assets.pose(model,"walk",0,1),walk=assets.pose(model,"walk",3,1);mesh(rest);mesh(walk);
            check(!Arrays.equals(rest.vertices,walk.vertices),"articulated locomotion differs: "+model);
            // Articulated motion changes pairwise geometry, rather than translating the model.
            float first=walk.vertices[0]-rest.vertices[0];boolean nonRigid=false;
            for(int i=0;i<walk.vertices.length;i+=7)if(Math.abs((walk.vertices[i]-rest.vertices[i])-first)>.00001f||Math.abs(walk.vertices[i+1]-rest.vertices[i+1])>.00001f||Math.abs(walk.vertices[i+2]-rest.vertices[i+2])>.00001f){nonRigid=true;break;}
            check(nonRigid,"real joint/wheel/oar motion "+model);
            for(String clip:new String[]{"idle","turn","prepare","attack","hit","defeat","enter"})mesh(assets.pose(model,clip,7,1));
        }
        SceneMesh formation=assets.pose("unit-SPEAR-lod0","walk",3,8);mesh(formation);
        check(formation.vertices.length==assets.mesh("unit-SPEAR-lod0").vertices.length*8,"eight members merged into one mesh");
        World forest=new World(40,40,"甲","乙");for(World.Terrain[] row:forest.terrain)Arrays.fill(row,World.Terrain.FOREST);
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(forest);Set<Hex> excluded=new HashSet<>();
        List<SceneMesh> trees=Vegetation.build(g,excluded,Collections.emptyList(),assets.mesh("tree-lod0"),assets.mesh("tree-lod1"));
        for(SceneMesh tree:trees){mesh(tree);if(tree.distant.indices.length>0)mesh(tree.distant);}
        List<SceneMesh> same=Vegetation.build(g,excluded,trees,assets.mesh("tree-lod0"),assets.mesh("tree-lod1"));
        check(same.equals(trees),"unchanged forest chunks are reused");
        forest.mapRevision++;MapSceneSnapshot.Ground revised=new MapSceneSnapshot.Ground(forest);
        check(!g.matches(forest),"map revision invalidates visual seed");
        List<SceneMesh> revisionTrees=Vegetation.build(revised,excluded,trees,assets.mesh("tree-lod0"),assets.mesh("tree-lod1"));
        check(!Arrays.equals(revisionTrees.get(0).vertices,trees.get(0).vertices),"different map version produces deterministic new vegetation");
        forest.mapRevision--;
        excluded.add(new Hex(20,20));List<SceneMesh> patch=Vegetation.build(g,excluded,trees,assets.mesh("tree-lod0"),assets.mesh("tree-lod1"));
        int reused=0;for(SceneMesh tree:patch)if(trees.contains(tree))reused++;
        check(reused>=trees.size()-9,"facility patch rebuilds only bounded affected forest neighborhood");
        check(!Vegetation.eligible(g,excluded,new Hex(20,20)),"no tree on facility");
        forest.terrain[20][19]=World.Terrain.ROAD;MapSceneSnapshot.Ground road=new MapSceneSnapshot.Ground(forest);
        check(!Vegetation.eligible(road,Collections.emptySet(),new Hex(20,20)),"road corridor excluded");
        System.out.println("PASS S04/S05 assets: "+checks+" checks; "+variants.size()+" facility LOD assets; "+rigs+" rigs; forest local reuse "+reused+"/"+trees.size());
    }
}
