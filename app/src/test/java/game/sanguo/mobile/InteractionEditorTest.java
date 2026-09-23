package game.sanguo.mobile;
import game.sanguo.core.map.SourceGridCoord;
import game.sanguo.core.*;
import java.util.*;
import java.io.*;
public final class InteractionEditorTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    interface Action {void run()throws Exception;}
    static void reject(Action action)throws Exception{try{action.run();throw new AssertionError("invalid metadata accepted");}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        SceneCamera camera=new SceneCamera();camera.width=1080;camera.height=1920;
        for(int facing:new int[]{1,-1})for(float span:new float[]{3,15,80,160}){
            camera.facing=facing;camera.span=span;float x=camera.worldX(237),z=camera.worldZ(711);
            camera.zoom(1.2f,237,711);check(Math.abs(camera.worldX(237)-x)<.001&&Math.abs(camera.worldZ(711)-z)<.001,"zoom anchor");
        }
        camera.x=Float.NaN;camera.z=Float.POSITIVE_INFINITY;camera.span=-1;camera.tilt=Float.NaN;camera.sanitize();
        check(Float.isFinite(camera.x)&&Float.isFinite(camera.z)&&camera.span==3&&camera.tilt==55,"corrupt camera fallback");
        camera.zoom(Float.NaN,0,0);check(Float.isFinite(camera.span),"invalid scale ignored");
        SceneMesh mesh=SceneMesh.proxy(0,0xffffffff);camera.x=0;camera.z=0;camera.span=5;
        check(Float.isFinite(ScenePicking.hit(camera,mesh,0,0,0,0,1,camera.screenX(0),camera.screenY(0,.2f))),"real triangle hit");
        check(!Float.isFinite(ScenePicking.hit(camera,mesh,0,0,0,0,1,-1000,-1000)),"outside mesh miss");
        MapPatch p=CustomMaps.base().fresh();MapEditSession session=new MapEditSession(p);Hex chosen=null;
        for(int x=30;x<160&&chosen==null;x++)for(int y=30;y<160;y++){Hex h=MapCoordinates.fromNationalSource(session.world(),new SourceGridCoord(x,y));if(!session.protectedAt(h)&&session.world().terrain[h.q][h.r]==World.Terrain.PLAIN){chosen=h;break;}}
        check(chosen!=null,"editable tile");SourceGridCoord at=MapCoordinates.nationalSource(session.world(),chosen);int cell=at.x*200+at.y;
        MapSceneSnapshot.Ground originalGround=new MapSceneSnapshot.Ground(session.world());List<SceneMesh> originalChunks=SceneMesh.ground(originalGround);
        byte[] before=SaveCodec.encode(session.world());String logic=p.logicalFingerprint();p.heights.put(cell,2300);
        int city=session.world().home().id;p.appearances.put(city,new MapPatch.Appearance(2,90));
        MapPatch imported=MapPatch.decode(p.encode());check(imported.heights.equals(p.heights)&&imported.appearances.equals(p.appearances),"format2 roundtrip");
        check(imported.logicalFingerprint().equals(logic),"visuals excluded from logical fingerprint");
        session.replace("visual",imported);check(Arrays.equals(before,SaveCodec.encode(session.world())),"full gameplay save bytes unchanged by visuals");
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(session.world());check(ground.surface.overrides.get(chosen)==2.3f,"height reaches renderer");
        List<SceneMesh> editedChunks=SceneMesh.ground(ground,originalChunks);long retained=editedChunks.stream().filter(originalChunks::contains).count();check(retained>originalChunks.size()/2&&retained<originalChunks.size(),"height-only edit rebuilds affected chunks and retains distant chunks");
        SiteVisual site=new SiteVisual(session.world(),session.world().home(),ground.grid);check(site.model.equals("city2")&&Math.abs(site.yaw-Math.PI/2)<.001,"whitelisted model and orientation applied");
        session.undo();check(session.patch().heights.isEmpty(),"visual undo");session.redo();check(session.patch().heights.containsKey(cell),"visual redo");
        session.paint("paint",Collections.singleton(chosen),World.Terrain.FOREST);check(!session.patch().heights.containsKey(cell),"terrain edit invalidates affected visual height");
        session.undo();check(session.patch().heights.get(cell)==2300,"paint undo restores metadata");session.redo();check(!session.patch().heights.containsKey(cell),"paint redo invalidates again");
        MapPatch old=CustomMaps.base().fresh();check(MapPatch.decode(old.encode()).heights.isEmpty(),"old format1 defaults");
        MapPatch fork=imported.copy();fork.id=UUID.randomUUID().toString();check(MapPatch.decode(fork.encode()).id.equals(fork.id),"copy rebinds metadata identity");
        MapPatch bad=imported.copy();bad.heights.put(cell,2601);reject(()->MapPatch.decode(bad.encode()));
        bad.heights.put(cell,100);bad.appearances.put(city,new MapPatch.Appearance(3,0));reject(()->MapPatch.decode(bad.encode()));
        bad.appearances.clear();bad.appearances.put(Integer.MAX_VALUE,new MapPatch.Appearance(0,0));reject(()->CustomMaps.verifyBase(bad));
        String json=new String(imported.encode(),java.nio.charset.StandardCharsets.UTF_8);
        reject(()->MapPatch.decode(json.replace("\"height\":2300","\"height\":1.5").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        reject(()->MapPatch.decode(json.replace(imported.logicalFingerprint(),"0".repeat(64)).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        reject(()->MapPatch.decode(new byte[MapJson.LIMIT+1]));
        System.out.println("PASS S07 interaction/editor: "+checks+" checks");
    }
}
