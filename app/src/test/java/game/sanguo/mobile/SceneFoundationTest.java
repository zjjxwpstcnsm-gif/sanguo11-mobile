package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

public final class SceneFoundationTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception {
        World w=ScenarioCatalog.all().get(0);
        byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(w);
        for(boolean staggered:new boolean[]{false,true}){
            GridWorldTransform g=new GridWorldTransform((w.height-1)/2,staggered);
            for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
                Hex h=new Hex(q,r);if(!w.inside(h))continue;
                check(g.cell(g.x(h),g.z(h)).equals(h),"valid cell center roundtrip "+h);
                for(float tilt:new float[]{35,55,70}){
                    SceneCamera c=new SceneCamera();c.width=1080;c.height=1920;c.x=g.x(h)-1;c.z=g.z(h)+2;c.tilt=tilt;c.span=15;
                    check(g.cell(c.worldX(c.screenX(g.x(h))),c.worldZ(c.screenY(g.z(h),0))).equals(h),"camera ground ray "+h);
                }
            }
        }
        GridWorldTransform g=new GridWorldTransform(0,false);
        check(g.cell(.5f,0).equals(new Hex(1,0)),"positive exact edge tie break");check(g.cell(-.5001f,0).equals(new Hex(-1,0)),"negative floor");
        MapSceneSnapshot snap=new MapSceneSnapshot(ground,w,w.cities.get(0).hex,-1);
        List<SceneMesh> meshes=SceneMesh.ground(ground);int cells=0;
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++)if(w.inside(new Hex(q,r)))cells++;
        int triangles=0;for(SceneMesh m:meshes){triangles+=m.indices.length/3;for(int i:m.indices)check(i>=0&&i<m.vertices.length/7,"valid mesh index");}
        check(triangles==cells*24,"exact twenty-four near triangles per actual cell, excluding VOID");
        Set<String> ids=new HashSet<>();for(MapSceneSnapshot.Item item:snap.items)check(ids.add(item.key),"stable disjoint ids");
        for(World.City site:w.cities)for(Hex h:SiteFootprint.cells(site))if(w.inside(h))check(w.cityAt(ground.grid.cell(ground.grid.x(h),ground.grid.z(h)))==site,"seven-cell same city");
        check(Arrays.equals(before,SaveCodec.encode(w)),"snapshot/mesh/camera do not mutate serialized authoritative state");
        Hex h=w.cities.get(0).hex;byte old=ground.terrain[h.r*w.width+h.q];w.terrain[h.q][h.r]=World.Terrain.SAND;check(ground.terrain[h.r*w.width+h.q]==old,"terrain detached from World");
        SceneCamera c=new SceneCamera();c.width=800;c.height=600;float x=c.worldX(123),z=c.worldZ(321);c.zoom(1.7f,123,321);check(Math.abs(x-c.worldX(123))<.001&&Math.abs(z-c.worldZ(321))<.001,"zoom preserves cursor ground point");
        World shown=SaveCodec.decode(before),headless=SaveCodec.decode(before);check(ground.matches(shown),"cloned turn world reuses static ground cache");boolean deployed=false;
        for(World.City site:shown.cities)if(site.owner==shown.player&&!shown.idle(site).isEmpty()){
            int officer=shown.idle(site).get(0).id;
            World probe=SaveCodec.decode(before);
            if(!probe.deploy(site.id,officer,World.Weapon.SWORD,1000).ok)continue;
            check(shown.deploy(site.id,officer,World.Weapon.SWORD,1000).ok,"real scenario deployment");
            check(headless.deploy(site.id,officer,World.Weapon.SWORD,1000).ok,"headless same deployment");
            World.Unit unit=shown.units.get(shown.units.size()-1);
            new MapSceneSnapshot(ground,shown,unit.hex,unit.id);
            for(Hex target:shown.orders.marchReachable(unit).keySet())if(!target.equals(unit.hex)&&shown.orders.previewMove(unit.id,target).valid()){
                check(shown.move(unit.id,target).ok,"real scenario movement");check(headless.move(unit.id,target).ok,"headless same movement");break;
            }
            deployed=true;break;
        }
        check(deployed,"a real scenario supports deployment");
        check(Arrays.equals(SaveCodec.encode(shown),SaveCodec.encode(headless)),"snapshot path and headless commands equivalent");
        check(shown.nextTurn().ok&&headless.nextTurn().ok,"both advance complete authoritative turn");
        new MapSceneSnapshot(ground,shown,null,-1);
        check(Arrays.equals(SaveCodec.encode(shown),SaveCodec.encode(headless)),"next-turn results equivalent with and without 3D snapshot");
        World loaded=SaveCodec.decode(SaveCodec.encode(shown));
        check(Arrays.equals(SaveCodec.encode(shown),SaveCodec.encode(loaded)),"save reload equivalent");
        System.out.println("PASS S01: "+checks+" assertions, "+cells+" valid cells, "+meshes.size()+" chunks, "+snap.items.size()+" objects");
    }
}
