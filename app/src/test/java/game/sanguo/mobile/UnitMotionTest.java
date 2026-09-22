package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;

public final class UnitMotionTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void near(float a,float b,String message){check(Math.abs(a-b)<.00001f,message);}
    public static void main(String[] args)throws Exception{
        Hex a=new Hex(2,2),b=new Hex(3,2),c=new Hex(3,3);
        TurnJournal.Event bent=UnitMotionFixture.move(7,a,b,c);
        for(boolean staggered:new boolean[]{false,true}){
            GridWorldTransform grid=new GridWorldTransform(5,staggered);
            UnitMotion first=new UnitMotion(),second=new UnitMotion();first.settle(a,grid);second.settle(c,grid);
            first.sample(bent,.25f,grid);near(first.x,(grid.x(a)+grid.x(b))/2,"first legal segment x");near(first.z,(grid.z(a)+grid.z(b))/2,"first segment z");
            first.sample(bent,.75f,grid);near(first.x,(grid.x(b)+grid.x(c))/2,"bend x");near(first.z,(grid.z(b)+grid.z(c))/2,"bend z");
            near(second.x,grid.x(c),"independent instance x");near(second.z,grid.z(c),"independent instance z");
            first.sample(bent,9,grid);near(first.x,grid.x(c),"clamped endpoint");
            first.sample(null,0,grid);near(first.x,grid.x(a),"clear clip restores snapshot");
            first.settle(c,grid);first.sample(null,0,grid);near(first.z,grid.z(c),"completed snapshot wins");
            first.sample(bent,Float.NaN,grid);near(first.x,grid.x(a),"nonfinite fraction safe");
            first.sample(UnitMotionFixture.move(7,a,new Hex(12,12)),.5f,grid);near(first.x,grid.x(a),"no shortcut without legal intermediate cells");
            for(int count:new int[]{50,100}){
                List<UnitMotion> poses=new ArrayList<>();
                for(int i=0;i<count;i++){UnitMotion p=new UnitMotion();p.settle(a,grid);poses.add(p);}
                for(int frame=0;frame<=60;frame++)for(int i=0;i<count;i++){
                    float f=((frame+i)%61)/60f;poses.get(i).sample(bent,f,grid);
                    check(Float.isFinite(poses.get(i).x)&&Float.isFinite(poses.get(i).z),"synthetic CPU pose stress only");
                }
            }
        }
        World w=ScenarioCatalog.all().get(0);MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(w);
        World.Officer officer=w.officers.get(0);Hex h=w.cities.get(0).hex;
        for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
            World.Unit unit=new World.Unit(99999,officer.owner,officer.id,weapon,h,4321,9876);unit.ship=ship;unit.wounded=123;
            UnitVisual visual=new UnitVisual(w,unit);check(visual.weapon==weapon&&visual.ship==ship,"exact equipment enum");
            unit.troops=1;unit.wounded=0;check(visual.troops==4321&&visual.wounded==123,"detached strength/wounded");
            check(visual.commander.equals(officer.name)&&visual.label().contains("4321"),"commander and strength HUD");
        }
        World waterWorld=new World(8,8,"甲","乙");Hex cell=new Hex(3,3);
        World.Unit probe=new World.Unit(11,0,-1,World.Weapon.CAVALRY,cell,3000,9000);
        for(World.Terrain terrain:World.Terrain.values()){
            waterWorld.terrain[3][3]=terrain;
            UnitVisual value=new UnitVisual(waterWorld,probe);
            check(value.naval==(terrain==World.Terrain.WATER||terrain==World.Terrain.SEA),"naval equipment follows core, not visual water mask: "+terrain);
        }
        waterWorld.terrain[3][3]=World.Terrain.WATER;
        for(Army.Ship ship:Army.Ship.values()){
            probe.ship=ship;check(new UnitVisual(waterWorld,probe).modelKey().equals("ship/"+ship.name()),"all actual ship model keys");
        }
        // Real deployment and movement, replay fractions/speed changes cannot change authority.
        byte[] before=SaveCodec.encode(w);boolean moved=false;
        for(World.City city:w.cities){
            if(city.owner!=w.player||w.idle(city).isEmpty())continue;
            int id=w.idle(city).get(0).id;
            if(!w.deploy(city.id,id,World.Weapon.SWORD,1000).ok)continue;
            World.Unit unit=w.units.get(w.units.size()-1);World plain=SaveCodec.decode(SaveCodec.encode(w));
            TurnJournal journal=new TurnJournal(w);
            for(Hex target:w.orders.marchReachable(unit).keySet())if(!target.equals(unit.hex)&&w.orders.previewMove(unit.id,target).valid()){
                check(w.move(unit.id,target).ok&&plain.move(unit.id,target).ok,"same real commands");moved=true;break;
            }
            journal.close();check(moved,"real route exists");byte[] after=SaveCodec.encode(w);
            for(TurnJournal.Event event:journal.events()){
                UnitMotion pose=new UnitMotion();pose.settle(event.start==null?unit.hex:event.start,ground.grid);
                for(float f:new float[]{0,.1f,.4f,.9f,1,1,0,1})pose.sample(event,f,ground.grid);
            }
            check(Arrays.equals(after,SaveCodec.encode(w)),"replay/skip does not mutate authority");
            check(Arrays.equals(after,SaveCodec.encode(plain)),"journal on/off resources and state identical");break;
        }
        check(moved&&!Arrays.equals(before,SaveCodec.encode(w)),"actual command changed state");
        System.out.println("PASS S05 unit presentation: "+checks+" assertions; 50/100 CPU pose stress (not GPU performance)");
    }
}
