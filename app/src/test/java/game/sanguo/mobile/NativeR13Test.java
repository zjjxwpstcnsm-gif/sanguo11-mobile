package game.sanguo.mobile;

import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.util.*;

/** Persistence failure injection and incremental meshes, using the real editor/resolver. */
public final class NativeR13Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Hex editable(MapEditSession s){
        for(int x=40;x<150;x++)for(int y=40;y<150;y++){
            Hex h=MapCoordinates.fromNationalSource(s.world(),new SourceGridCoord(x,y));
            if(!s.protectedAt(h)&&s.world().terrain[h.q][h.r]==World.Terrain.PLAIN)return h;
        }throw new AssertionError("editable land");
    }
    public static void main(String[] args)throws Exception{
        MapEditSession s=new MapEditSession(CustomMaps.base().fresh());Hex h=editable(s);
        s.paint("first",Set.of(h),World.Terrain.FOREST);s.undo();
        byte[] original=s.patch().encode(),save=SaveCodec.encode(s.world());long bytes=s.historyBytes(),generation=s.generation();
        String undo=s.undoLabel(),redo=s.redoLabel();
        try{s.persist(()->s.paint("failed",Set.of(h),World.Terrain.SAND),p->{throw new IOException("injected disk full");});throw new AssertionError("write must fail");}catch(IOException expected){checks++;}
        check(Arrays.equals(original,s.patch().encode()),"failed write restores exact draft");
        check(Arrays.equals(save,SaveCodec.encode(s.world())),"failed write restores complete world/RNG");
        check(s.historyBytes()==bytes&&s.generation()==generation&&s.undoLabel().equals(undo)&&s.redoLabel().equals(redo),"failed write preserves both histories and epoch");
        s.persist(()->{s.redo();return null;},p->MapPatch.decode(p.encode()));check(!s.dirty(),"successful redo durably clean");
        byte[] painted=s.patch().encode();try{s.persist(()->{s.undo();return null;},p->{throw new IOException("interrupted undo");});throw new AssertionError();}catch(IOException expected){checks++;}
        check(Arrays.equals(painted,s.patch().encode())&&s.undoLabel().equals("first"),"failed undo stays undoable");
        s.persist(()->{s.undo();return null;},p->MapPatch.decode(p.encode()));check(Arrays.equals(original,s.patch().encode()),"retry undo exact");
        MapSceneSnapshot.Ground before=new MapSceneSnapshot.Ground(s.world());
        SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(before.grid.x(h),before.grid.z(h),30,30,30);
        List<SceneMesh> old=SceneMesh.ground(before,List.of(),window);
        s.persist(()->s.paint("water",Set.of(h),World.Terrain.WATER),p->MapPatch.decode(p.encode()));
        MapSceneSnapshot.Ground after=new MapSceneSnapshot.Ground(s.world());List<SceneMesh> cached=SceneMesh.ground(after,old,window),fresh=SceneMesh.ground(after,List.of(),window);
        long reused=cached.stream().filter(old::contains).count();check(reused>0&&reused<cached.size(),"one-cell edit retains unrelated chunks");
        check(cached.size()==fresh.size(),"same streaming window");
        for(int i=0;i<cached.size();i++){SceneMesh a=cached.get(i),b=fresh.get(i);check(Arrays.equals(a.vertices,b.vertices)&&Arrays.equals(a.indices,b.indices)&&Arrays.equals(a.surfaceData,b.surfaceData),"incremental heights/material/shore equal clean rebuild");}
        s.undo();MapSceneSnapshot.Ground restored=new MapSceneSnapshot.Ground(s.world());
        check(restored.source(h).equals(MapCoordinates.nationalSource(s.world(),h)),"overlay uses actual national source coordinate");
        SceneCamera camera=new SceneCamera();camera.width=1080;camera.height=1920;camera.span=5;camera.x=restored.grid.x(h);camera.z=restored.grid.z(h);
        for(float yaw:new float[]{0,47,135,270})for(float tilt:new float[]{40,55,70}){camera.yaw=yaw;camera.tilt=tilt;check(h.equals(restored.surface.pick(camera,camera.screenX(camera.x,camera.z),camera.screenY(camera.x,camera.z,restored.surface.at(h)))),"rotated brush uses same displayed surface");}
        MapPatch fixture=MapEditorContinuationTest.fixture();World custom=CustomMaps.load(fixture,fixture.preview,0,17L);MapSceneSnapshot shot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(custom),custom,null,-1);
        for(int id:new int[]{100006701,100006702,100006703})check(shot.items.stream().anyMatch(i->i.key.equals("site:"+id)&&i.site!=null&&!i.site.model.isEmpty()),"custom city/port/gate reaches normal resource mapping");
        System.out.println("PASS R13 "+checks+" checks; retained="+reused+"/"+cached.size());
    }
}
