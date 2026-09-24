package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** Pure deterministic production presentation tests; GPU / device art is a separate gate. */
public final class NativeR10Test {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void near(float a,float b,String why){check(Float.isFinite(a)&&Math.abs(a-b)<.00003f,why+" "+a+" / "+b);}
    static World normalized(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    static MapSceneSnapshot.Item item(MapSceneSnapshot s,int id){return s.items.stream().filter(i->i.unit!=null&&i.unit.id==id).findFirst().orElseThrow();}
    static void parity(World initial,String kind,boolean attack)throws Exception {
        World original=normalized(initial),authority=normalized(original),control=normalized(original);
        TurnJournal journal=new TurnJournal(authority);
        World.Result a=attack?UnitR10Fixture.fight(authority,kind):UnitR10Fixture.move(authority);check(a.ok,kind+" actual action "+a.message);journal.close();
        World.Result b=attack?UnitR10Fixture.fight(control,kind):UnitR10Fixture.move(control);check(b.ok,kind+" no journal control "+b.message);
        byte[] expected=SaveCodec.encode(control);check(Arrays.equals(expected,SaveCodec.encode(authority)),kind+" journal/no-journal exact authority");
        check(!journal.events().isEmpty(),kind+" recorded real action");
        for(int frames:new int[]{60,15,1}){
            World display=normalized(original);
            for(TurnJournal.Event event:journal.events()){
                MapSceneSnapshot snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(display),display,null,-1);
                for(int frame=0;frame<=frames;frame++)for(MapSceneSnapshot.Item item:snap.items)if(item.unit!=null){
                    float f=frame/(float)frames;UnitMotion pose=new UnitMotion();pose.settle(item.hex,snap.ground.grid);
                    if(event.actorId==item.unit.id)pose.sample(event,f,snap.ground.grid);
                    UnitAnimation anim=new UnitAnimation();anim.sample(item,snap.ground,event,f,12345,frame%3);
                    UnitFormation group=new UnitFormation();group.sample(item.unit,UnitAnimation.shownTroops(item.unit,event,f),anim.naval,frame%3,snap.ground,pose.x,pose.z,pose.yaw,anim.scale);
                    check(group.count>=1&&group.count<=8,"bounded count at all replay speeds");
                    check(item.unit.identity().contains("#"+item.unit.id),"non-color identity");
                    if(event.actorId==item.unit.id&&event.kind==TurnJournal.Kind.MOVE){
                        UnitMotion independentlySampled=new UnitMotion();independentlySampled.settle(item.hex,snap.ground.grid);independentlySampled.sample(event,f,snap.ground.grid);
                        near(pose.yaw,independentlySampled.yaw,"phase-derived heading");
                    }
                }
                event.applyVisual(display);
            }
            check(Arrays.equals(expected,SaveCodec.encode(authority)),kind+" normal / fast / skip leaves authority bytes and RNG unchanged");
        }
    }
    public static void main(String[] args)throws Exception {
        FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
        for(String kind:UnitR10Fixture.KINDS){
            World w=normalized(UnitR10Fixture.moving(kind,false));World.Unit u=UnitR10Fixture.actor(w);UnitVisual v=new UnitVisual(w,u);
            for(int lod=0;lod<3;lod++){
                String model=FieldAssets.unit(v,v.naval,lod);check(model.equals("unit-"+kind+"-lod"+Math.min(1,lod)),"all 13 actual mappings "+model);
                for(String clip:new String[]{"idle","walk","turn","prepare","attack","hit","defeat","enter"}){
                    SceneMesh mesh=assets.pose(model,clip,5,1),repeat=assets.pose(model,clip,5,1);
                    check(Arrays.equals(mesh.vertices,repeat.vertices),"deterministic rigid pose "+model+"/"+clip);
                    check(mesh.vertices.length==assets.mesh(model).vertices.length,"runtime single-member shared geometry");
                    for(float value:mesh.vertices)check(Float.isFinite(value),"finite skinned-free geometry");
                }
            }
            parity(w,kind,false);
            if(!kind.equals("transport"))parity(UnitR10Fixture.fighting(kind),kind,true);
        }
        World w=normalized(UnitR10Fixture.moving("SPEAR",true));parity(w,"SPEAR-ridge",false);
        UnitVisual u=new UnitVisual(w,UnitR10Fixture.actor(w));MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(w);
        UnitFormation f=new UnitFormation();float x=ground.grid.x(UnitR10Fixture.actor(w).hex),z=ground.grid.z(UnitR10Fixture.actor(w).hex);
        for(float yaw:new float[]{0,.4f,1.7f,3.14f}){
            f.sample(u,10000,false,0,ground,x,z,yaw,1);check(f.count==8,"large foot group");
            float[][] full=new float[8][];for(int i=0;i<8;i++)full[i]=f.placement[i].clone();
            for(int strength:new int[]{7500,5000,1250,0,Integer.MAX_VALUE}){
                f.sample(u,strength,false,0,ground,x,z,yaw,1);
                for(int i=0;i<f.count;i++){
                    near(f.placement[i][0],full[i][0],"prefix slot x survives losses");near(f.placement[i][2],full[i][2],"prefix slot z survives losses");
                    float wx=x+(float)Math.cos(yaw)*f.placement[i][0]+(float)Math.sin(yaw)*f.placement[i][2],wz=z-(float)Math.sin(yaw)*f.placement[i][0]+(float)Math.cos(yaw)*f.placement[i][2];
                    near(f.rootY+f.placement[i][1],ground.surface.meshHeight(wx,wz)+.02f,"independent exact member anchor");
                }
            }
            long n=f.updates;check(!f.sample(u,Integer.MAX_VALUE,false,0,ground,x,z,yaw,1)&&f.updates==n,"stationary contact cache skips repeated terrain/uniform work");
        }
        check(UnitLod.select(0,8.7f,0)==0&&UnitLod.select(0,8.9f,0)==1,"near exit hysteresis");
        check(UnitLod.select(1,7.3f,0)==1&&UnitLod.select(1,7.1f,0)==0,"near enter hysteresis");
        check(UnitLod.select(1,23.9f,0)==1&&UnitLod.select(1,24.1f,0)==2,"far exit hysteresis");
        check(UnitLod.select(2,20.1f,0)==2&&UnitLod.select(2,19.9f,0)==1,"far enter hysteresis");
        for(long t=0;t<10000;t+=17)for(int id=0;id<30;id++){
            int fine=UnitLod.idleFrame(t,id,0);check(UnitLod.idleFrame(t,id,1)==fine/2*2&&UnitLod.idleFrame(t,id,2)==fine/4*4,"LOD quantizes same clock");
        }
        // Continuous turn through a legal bend; positions stay on the original polyline.
        Hex a=new Hex(2,2),b=new Hex(3,2),c=new Hex(3,3);TurnJournal.Event bent=UnitMotionFixture.move(7,a,b,c);
        UnitMotion m=new UnitMotion();m.settle(a,ground.grid);m.sample(bent,.49999f,ground.grid);float yaw=m.yaw;m.sample(bent,.50001f,ground.grid);near(yaw,m.yaw,"heading continuous at bend");
        m.sample(bent,1,ground.grid);float end=m.yaw;UnitMotion fresh=new UnitMotion();fresh.settle(a,ground.grid);fresh.sample(bent,1,ground.grid);near(end,fresh.yaw,"skip endpoint heading deterministic");
        // Render picking and shader use identical member contact shears.
        SceneCamera camera=new SceneCamera();camera.width=720;camera.height=1280;camera.x=x;camera.z=z;camera.span=3;
        f.sample(u,10000,false,0,ground,x,z,0,1);SceneMesh mesh=assets.pose("unit-SPEAR-lod0","idle",0,1);
        float sx=camera.screenX(x,z),sy=camera.screenY(x,z,f.rootY+.20f);
        check(Float.isFinite(f.hit(camera,mesh,x,z,0,1,sx,sy)),"actual representative triangles pickable");
        System.out.println("PASS R10 "+checks+" assertions; 13 categories / 8 clips / 2 meshes; contact, hysteresis, exact command parity. CPU checks, not physical-device art/FPS.");
    }
}
