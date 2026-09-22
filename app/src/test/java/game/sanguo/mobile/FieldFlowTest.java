package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
public final class FieldFlowTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        World w=FieldSceneFixture.port(),reference=SaveCodec.decode(SaveCodec.encode(w)),visual=SaveCodec.decode(SaveCodec.encode(w));
        World.Unit u=w.units.get(0);TurnJournal journal=new TurnJournal(w);Hex target=new Hex(21,19);
        World.Result result=w.move(u.id,target);check(result.ok,result.message);journal.close();check(reference.move(u.id,target).ok,"reference move");
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(visual);boolean sea=false,land=false;
        for(TurnJournal.Event event:journal.events()){
            MapSceneSnapshot snap=new MapSceneSnapshot(g,visual,null,-1);MapSceneSnapshot.Item item=snap.items.stream().filter(i->i.unit!=null).findFirst().orElseThrow();UnitAnimation animation=new UnitAnimation();
            for(int i=0;i<=100;i++){animation.sample(item,g,event,i/100f,i*16,0);sea|=animation.naval;land|=sea&&!animation.naval;}
            event.applyVisual(visual);
        }
        check(sea&&land,"actual legal route switches model sea and back to land");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reference)),"journal/animation preserves exact rule result");
        for(int n:new int[]{50,100}){World fixture=FieldSceneFixture.create(n,true,true);check(fixture.units.size()==n,"stress count exact");Set<Hex> occupied=new HashSet<>();for(World.Unit unit:fixture.units)check(occupied.add(unit.hex),"unique unit cells");for(War.Structure s:fixture.war.structures())check(!occupied.contains(s.hex),"stress fixtures do not overlap units/facilities");}
        World a=FieldLifecycleFixture.start(),b=SaveCodec.decode(SaveCodec.encode(a));
        @SuppressWarnings("unchecked") java.util.function.Consumer<World>[] actions=new java.util.function.Consumer[]{
            (java.util.function.Consumer<World>)FieldLifecycleFixture::build,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::finish,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::upgrade,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::damage,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::repair,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::destroy,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::domesticBuild,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::domesticFinish,
            (java.util.function.Consumer<World>)FieldLifecycleFixture::domesticDemolish};
        for(java.util.function.Consumer<World> action:actions){
            action.accept(a);action.accept(b);new MapSceneSnapshot(new MapSceneSnapshot.Ground(a),a,null,-1);
            check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"construction/upgrade/attack/repair/destruction/demolition exact state equality");
        }
        System.out.println("PASS S04/S05 flow: real legal port move, water/land asset transitions, 50/100 stress identities, real facility lifecycle and exact core equality");
    }
}
