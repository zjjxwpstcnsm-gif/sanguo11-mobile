package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public final class LifecycleTest {
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private static void reject(World w,Supplier<World.Result> command)throws Exception{byte[] before=bytes(w);check(!command.get().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"rejected command is atomic");}
    private static World fixture(){
        World w=new World(40,24,"甲军","乙军");w.startYear=200;
        w.cities.add(new World.City(10,"甲城",new Hex(2,2),0));w.cities.add(new World.City(11,"甲关",new Hex(2,18),0));w.cities.add(new World.City(20,"乙城",new Hex(36,20),1));
        for(int i=0;i<7;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,i==6?11:10,70+i,70,70,70,70));
        for(int i=10;i<13;i++)w.officers.add(new World.Officer(i,"乙将"+i,1,20,70,70,70,70,70));
        w.strategy.initializeOffices();return w;
    }
    private static void ageToDeath(World w,int id){World.Officer o=w.officer(id);w.life.configure(id,140,160,191,o.cityId>=0?o.cityId:10,Lifecycle.State.ACTIVE);w.life.naturalDeaths=true;}
    private static void nextMonth(World w){w.turn+=3;w.life.tick();w.active=w.player;w.checkVictory();}
    public static void main(String[] args)throws Exception{
        migration();appearance();deathAndInheritance();tasksAndCrew();execution();aiSuccession();coordinates();replay();
        System.out.println("PASS: "+checks+" lifecycle/national-grid assertions: actual v14 migration, annual arrival, mortality, task cleanup, commander replacement, pending succession guards/replay, execution, AI heirs, 40000 offset cells and import/save/path.");
    }
    private static void migration()throws Exception{
        String text=new String(LifecycleTest.class.getResourceAsStream("/legacy-v14.b64").readAllBytes(),StandardCharsets.UTF_8).trim();byte[] old=Base64.getDecoder().decode(text);
        check(new DataInputStream(new ByteArrayInputStream(old)).readInt()==0x53473131,"real previous writer header");World w=SaveCodec.decode(old);
        check(w.scenarioId.equals("world-drill")&&!w.life.enabled()&&w.life.people().isEmpty()&&!w.life.pending(),"old saves do not invent mortality or dates");
        check(Arrays.equals(bytes(w),bytes(copy(w))),"v14 to v17 canonical roundtrip");
    }
    private static void appearance()throws Exception{
        World w=fixture();w.startMonth=12;World.Officer future=new World.Officer(30,"后辈",-1,10,80,70,60,50,40);w.officers.add(future);
        w.life.configure(30,185,201,250,10,Lifecycle.State.UNAPPEARED);byte[] initial=bytes(w);
        check(!w.life.present(30)&&w.strategy.officerState(30).activity==Strategy.Activity.UNAPPEARED,"future officer cannot act or recruit");
        w.life.tick();check(Arrays.equals(initial,bytes(w)),"not January has no arrival");
        w.turn=3;w.life.tick();check(w.life.present(30)&&future.owner==-1&&future.cityId==10&&w.life.age(30)==17,"January arrival in configured location");
        int events=w.life.history().size();w.life.tick();check(w.life.history().size()==events,"arrival recorded exactly once");check(Arrays.equals(bytes(w),bytes(copy(w))),"arrival survives save");
        w.life.naturalDeaths=true;w.turn=300;check(w.life.deathChance(1)==0,"unconfigured age never guessed");
        try{w.life.configure(1,200,190,210,10,Lifecycle.State.ACTIVE);throw new AssertionError("invalid chronology accepted");}catch(IllegalArgumentException expected){checks++;}
    }
    private static void deathAndInheritance()throws Exception{
        World w=fixture();ageToDeath(w,0);w.relations.link(2,0,Relations.Kind.FATHER);
        w.treasures.place(Treasures.definition("item-000"),Treasures.Place.OFFICER,0);int gold=w.city(10).gold;
        nextMonth(w);check(w.life.state(0)==Lifecycle.State.DEAD&&w.life.pending(),"ruler death requires choice");
        check(w.life.successors().get(0).id==2,"known child sorts first");check(w.treasures.item("item-000").place==Treasures.Place.TREASURY,"dead ruler treasure in treasury");
        check(w.officer(0).cityId==-1&&w.officer(0).owner==-1&&!w.strategy.officerState(0).canAct,"dead officer detached");
        reject(w,()->w.nextTurn());reject(w,()->w.recruit(10,1));reject(w,()->w.life.inherit(99,2));reject(w,()->w.life.inherit(0,10));reject(w,()->w.life.toggle());
        check(!w.editor.faction(0,60,1000).valid(),"editor blocked until succession resolved");
        World restored=copy(w);check(restored.life.pending()&&restored.life.successors().size()==6,"pending choice restored");
        check(w.life.inherit(0,2).ok&&restored.life.inherit(0,2).ok,"select heir on both branches");
        check(Arrays.equals(bytes(w),bytes(restored)),"same resumed inheritance result");check(w.officer(2).role==Strategy.Role.RULER&&w.officer(2).loyalty==100&&w.city(10).gold==gold,"crown changes role without resource grant");
        reject(w,()->w.life.inherit(0,2));int events=w.life.history().size();nextMonth(w);check(events==w.life.history().size(),"dead ruler never dies twice");
        check(w.life.toggle().ok&&!w.life.enabled()&&w.life.state(0)==Lifecycle.State.DEAD,"disable mortality does not resurrect");
    }
    private static void tasksAndCrew()throws Exception{
        World w=fixture();w.city(10).gold=50000;w.campaign.points.put(0,10000);
        check(w.domestic.build(10,1,Domestic.Kind.MARKET,new Hex(3,2)).ok,"builder task exists");
        check(w.campaign.research(10,2,Campaign.Tech.SPEAR_DRILL).ok,"research exists");
        check(w.domestic.transfer(10,11,3).ok,"transfer exists");
        ageToDeath(w,1);ageToDeath(w,2);ageToDeath(w,3);nextMonth(w);
        check(w.domestic.facilities.isEmpty()&&w.domestic.missions.isEmpty()&&w.campaign.projects.isEmpty(),"death removes unfinished tasks");check(Arrays.equals(bytes(w),bytes(copy(w))),"task cleanup saveable");
        World c=fixture();World.Unit unit=new World.Unit(1,0,1,World.Weapon.SPEAR,new Hex(8,8),5000,12345);unit.deputies=new int[]{2,3};unit.gold=678;unit.energy=71;unit.movementBudget=4;unit.movementSpent=2;unit.march=new MarchOrders.Order(MarchOrders.Kind.TILE,new Hex(20,8),-1,-1);c.units.add(unit);c.nextUnitId=2;
        for(int id:new int[]{1,2,3}){c.officer(id).cityId=-1;c.officer(id).unitId=1;}ageToDeath(c,1);nextMonth(c);
        World.Unit survivor=c.unit(1);check(survivor.officerId==3&&Arrays.equals(survivor.deputies,new int[]{2}),"best deputy replaces leader");
        check(survivor.troops==5000&&survivor.food==12345&&survivor.gold==678&&survivor.energy==71&&survivor.movementSpent==2&&survivor.march!=null,"all army resources and orders preserved");check(Arrays.equals(bytes(c),bytes(copy(c))),"replaced commander references save correctly");
        ageToDeath(c,2);nextMonth(c);check(c.unit(1).deputies.length==0&&c.unit(1).officerId==3,"deputy death removes only deputy");
        ageToDeath(c,3);nextMonth(c);check(c.unit(1)==null,"no leader left dissolves army");bytes(c);
    }
    private static void execution()throws Exception{
        World w=fixture();w.government.capture(w.officer(10),w.city(10));int ap=w.actionPoints[0];
        check(w.life.executePrisoner(10,1,10).ok,"captured ruler execution");check(w.officer(11).role==Strategy.Role.RULER&&w.life.state(10)==Lifecycle.State.DEAD,"AI successor installed");check(w.actionPoints[0]==ap-10&&w.strategy.factionRelation(0,1)==-100,"execution cost and hostility");
        reject(w,()->w.life.executePrisoner(10,2,10));bytes(w);
        World empty=fixture();empty.officers.removeIf(o->o.owner==1&&o.id!=10);ageToDeath(empty,10);nextMonth(empty);check(empty.city(20).owner==-1&&empty.winner==0,"no successor dissolves faction and checks victory");bytes(empty);
    }
    private static void aiSuccession()throws Exception{
        World w=fixture();w.relations.link(12,10,Relations.Kind.FATHER);ageToDeath(w,10);ageToDeath(w,12);nextMonth(w);
        check(w.officer(11).role==Strategy.Role.RULER&&w.life.state(12)==Lifecycle.State.DEAD,"same-month dying child cannot become heir");bytes(w);
    }
    private static void coordinates()throws Exception{
        for(int y=0;y<200;y++)for(int x=0;x<200;x++){
            Hex h=MapCoordinates.axial(x,y,200);check(MapCoordinates.source(h,200).equals(new Hex(x,y)),"every cell inverse coordinate");
            if(x<199)check(h.distance(MapCoordinates.axial(x+1,y,200))==1,"horizontal neighbor");
            if(y<199){int left=x-((y&1)==0?1:0);if(left>=0)check(h.distance(MapCoordinates.axial(left,y+1,200))==1,"lower left neighbor");int right=left+1;if(right<200)check(h.distance(MapCoordinates.axial(right,y+1,200))==1,"lower right neighbor");}
        }
        StringBuilder p=new StringBuilder("format=1\nid=grid-test\nname=格点测试\nsource=user-supplied\nrevision=1\nyear=200\nmonth=1\nwidth=200\nheight=200\ncoordinates=odd-r\nfactions=2\nfaction.0=甲\nfaction.1=乙\n");
        for(int y=0;y<200;y++)p.append("terrain.").append(y).append('=').append("P".repeat(200)).append('\n');
        p.append("cities=2\ncity.0=10|甲城|1|1|0|10000|100000|3000|90|80|3000|10000|10000|10000|10000\ncity.1=20|乙城|198|198|1|10000|100000|3000|90|80|3000|10000|10000|10000|10000\nofficers=2\nofficer.0=0|甲将|0|10|80|80|80|80|80\nofficer.1=1|乙将|1|20|80|80|80|80|80\n");
        World w=ScenarioData.read(new ByteArrayInputStream(p.toString().getBytes(StandardCharsets.UTF_8)),0);check(w.width==299&&w.height==200&&w.sourceMapWidth==200,"200x200 offset map preserved in axial rectangle");check(MapCoordinates.source(w.city(20).hex,200).equals(new Hex(198,198)),"city source coordinates survive");
        World restored=copy(w);check(restored.sourceMapWidth==200&&Arrays.equals(bytes(w),bytes(restored)),"coordinate metadata saved");
        w.officers.add(new World.Officer(2,"行军将",0,-1,80,80,80,80,80));w.officer(2).unitId=1;w.units.add(new World.Unit(1,0,2,World.Weapon.SPEAR,MapCoordinates.axial(2,2,200),3000,50000));w.nextUnitId=2;
        long start=System.nanoTime();MarchOrders.Plan route=w.marches.preview(1,MapCoordinates.axial(197,197,200));check(route.valid()&&route.path.size()>190,"national-size path spans map");System.out.println("200x200 offset map cold route: "+(System.nanoTime()-start)/1000000+" ms");
        check(w.marches.execute(route).ok,"national path executes");bytes(w);
        w.terrain[0][0]=World.Terrain.PLAIN;try{bytes(w);throw new AssertionError("padding accepted");}catch(IOException expected){checks++;}
    }
    private static void replay()throws Exception{
        World a=fixture();for(World.City c:a.cities){c.troops=3000;c.food=300000;}
        a.life.configure(0,145,170,200,10,Lifecycle.State.ACTIVE);a.life.configure(10,145,170,200,20,Lifecycle.State.ACTIVE);a.life.naturalDeaths=true;World b=copy(a);
        for(int i=0;i<48&&!a.gameOver();i++){
            check(a.nextTurn().ok&&b.nextTurn().ok,"long campaign turn");check(Arrays.equals(bytes(a),bytes(b)),"mortality and AI replay with saved RNG");
            if(a.life.pending()){int id=a.life.successors().get(0).id;check(a.life.inherit(a.life.departedRuler(),id).ok&&b.life.inherit(b.life.departedRuler(),id).ok,"replayed player choice");}
            b=copy(b);
        }
    }
}
