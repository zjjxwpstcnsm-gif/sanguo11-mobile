package game.sanguo.core;

import java.util.*;

/** Focused v63 regressions: fixtures only arrange inputs; every command is the shipped runtime. */
public final class CityMovement63Test {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World fixture(){return CityFootprint55Test.fixture();}
    private static World.Unit field(World w,Hex h){return CityFootprint55Test.unit(w,h,World.Weapon.SPEAR);}
    private static World.Unit probe(World.City c){return new World.Unit(-1,c.owner,1,World.Weapon.SPEAR,c.hex,5000,10000);}
    private static int cost(World w,World.Unit u,List<Hex> path){int total=0;for(int i=1;i<path.size();i++){
        check(path.get(i-1).distance(path.get(i))==1,"consecutive six-neighbor edge");
        int edge=w.army.moveCost(u,path.get(i-1),path.get(i));check(edge>0,"legal production edge");total+=edge;
    }return total;}
    private static void departures()throws Exception {
        for(int clicked=0;clicked<7;clicked++){
            World w=fixture();World.City c=w.cityAt(SiteFootprint.cells(w.city(10)).get(clicked));
            SiteFootprint.Deployment p=SiteFootprint.deployment(w,c,probe(c));
            check(c.id==10&&p.valid()&&p.origin().equals(c.hex),"all seven clicks resolve logical center");
            check(p.cost==cost(w,probe(c),p.path)&&p.cost==2,"both paid edges from center to outside");
            byte[] before=SaveCodec.encode(w);
            World.Unit preview=w.army.deploymentPreview(c,1,new int[]{2,3},World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000,123);
            check(Arrays.equals(before,SaveCodec.encode(w)),"deployment preview is read-only");
            ok(w.army.deploy(c.id,1,new int[]{2,3},World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000,123));
            World.Unit u=w.unit(w.officer(1).unitId);
            check(u.hex.equals(preview.hex)&&u.movementBudget==preview.movementBudget&&u.movementSpent==preview.movementSpent,"preview matches real deployment");
            check(u.movementSpent==2&&w.orders.remaining(u)==u.movementBudget-2,"exact remaining budget, no free exit");
            Hex end=new Hex(4,15);MarchOrders.Plan march=w.marches.previewMove(u.id,end);check(march.valid(),march.error);
            int expected=0;for(int j=1;j<=march.stepsNow;j++)expected+=w.army.moveCost(u,march.path.get(j-1),march.path.get(j));
            ok(w.marches.execute(march));check(u.movementSpent==2+expected&&u.movementSpent<=u.movementBudget,"automatic task does not reset/refund departure");
            World loaded=SaveCodec.decode(SaveCodec.encode(w));check(loaded.unit(u.id).movementSpent==u.movementSpent,"paid departure survives SaveCodec31");
            for(int turn=0;turn<2;turn++){ok(w.nextTurn());ok(loaded.nextTurn());check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(loaded)),"actual nextTurn save continuation deterministic");}
        }
        for(int direction=0;direction<6;direction++){
            World w=fixture();World.City c=w.city(10);Hex rim=c.hex.neighbors().get(direction);
            for(Hex h:c.hex.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
            w.terrain[rim.q][rim.r]=World.Terrain.FOREST;
            SiteFootprint.Deployment p=SiteFootprint.deployment(w,c,probe(c));
            check(p.valid()&&p.path.get(1).equals(rim),"forced departure direction "+direction);
            check(p.cost==3&&p.cost==cost(w,probe(c),p.path),"weighted forest plus plain rather than hex distance");
        }
        World w=fixture();World.City c=w.city(10);World.Unit blocker=field(w,c.hex);
        byte[] old=SaveCodec.encode(w);World.Result fail=w.army.deploy(10,2,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000,0);
        check(!fail.ok&&fail.message.contains("中心")&&Arrays.equals(old,SaveCodec.encode(w)),"blocked center fails atomically, preserves occupant");
        blocker.owner=1;check(!SiteFootprint.deployment(w,c,probe(c)).valid(),"hostile center not crossed");
        w=fixture();c=w.city(10);for(Hex h:c.hex.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        check(!SiteFootprint.deployment(w,c,probe(c)).valid(),"sealed ring cannot teleport");
        w=fixture();c=w.city(10);SiteFootprint.Deployment first=SiteFootprint.deployment(w,c,probe(c));field(w,first.exit());
        SiteFootprint.Deployment alternative=SiteFootprint.deployment(w,c,probe(c));check(alternative.valid()&&!alternative.exit().equals(first.exit()),"occupied initial spawn uses a different legal paid path");
    }
    private static void arrivals()throws Exception {
        for(Hex h:SiteFootprint.cells(fixture().city(10))){World w=fixture();World.Unit u=field(w,h);int troops=w.city(10).troops;
            MarchOrders.Plan p=w.marches.previewCity(u.id,10);check(p.valid()&&p.path.size()==1&&p.cost==0,"already on each legal city cell: no forced center step");
            ok(w.marches.execute(p));check(w.unit(u.id)==null&&w.city(10).troops==troops+6000,"one exact garrison deposit");}
        for(Hex h:SiteFootprint.edge(fixture().city(10))){World w=fixture();World.Unit u=field(w,h);byte[] old=SaveCodec.encode(w);
            check(!w.enter(u.id,10).ok&&Arrays.equals(old,SaveCodec.encode(w)),"outside neighbor is not already in city");
            MarchOrders.Plan p=w.marches.previewCity(u.id,10);check(p.valid()&&p.cost>0&&SiteFootprint.contains(w.city(10),p.path.get(p.path.size()-1)),"paid approach ends on actual footprint");
            ok(w.marches.execute(p));check(w.unit(u.id)==null,"paid city-rim arrival completes");}
        World w=fixture();World.Unit u=field(w,new Hex(7,10));World.City c=w.city(10);
        World.Unit block=new World.Unit(w.nextUnitId++,0,2,World.Weapon.SPEAR,new Hex(9,10),1000,1000);w.strategy.releaseGovernor(2);w.officer(2).cityId=-1;w.officer(2).unitId=block.id;w.units.add(block);
        for(Hex clicked:SiteFootprint.cells(c)){MarchOrders.Plan p=w.marches.preview(u.id,clicked);check(p.valid()&&p.order.targetId==10&&p.order.kind==MarchOrders.Kind.CITY&&!p.path.contains(block.hex),"clicked occupied entrance still means city, alternate entry: "+p.error);}
        for(Hex h:SiteFootprint.cells(c))w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        check(!w.marches.previewCity(u.id,10).valid(),"all city entrances blocked");
        w=fixture();u=field(w,new Hex(7,10));c=w.city(10);u.movementBudget=2;
        ok(w.marches.execute(w.marches.previewMove(u.id,new Hex(14,10))));
        check(SiteFootprint.contains(c,u.hex)&&w.unit(u.id)==u&&u.march.intent==MarchOrders.Intent.MOVE,"ordinary crossing stops inside but never garrisons");
        World loaded=SaveCodec.decode(SaveCodec.encode(w));check(loaded.unit(u.id).march.intent==MarchOrders.Intent.MOVE,"crossing semantics saved");
        w=fixture();u=field(w,new Hex(7,10));u.march=new MarchOrders.Order(MarchOrders.Kind.CITY,new Hex(2,2),10,0,MarchOrders.Intent.GARRISON);
        loaded=SaveCodec.decode(SaveCodec.encode(w));MarchOrders.Plan p=loaded.marches.current(loaded.unit(u.id));
        check(p.valid()&&p.path.get(p.path.size()-1).equals(new Hex(9,10)),"saved city ID wins over stale coordinate");
        w=fixture();u=field(w,new Hex(10,11));u.food=0;new CampaignAi(w).runUnit(u,false,x->false,x->x.id==10);
        check(w.unit(u.id)==null,"official AI retreat uses city rim");
    }
    private static void transports()throws Exception {
        World w=fixture();World.City c=new World.City(30,"收货城",new Hex(19,10),0);w.cities.add(c);
        ok(w.domestic.transport(10,30,1,new int[]{2},10,5000,1000,new int[4],false,false));
        Domestic.Mission m=w.domestic.missions.get(0);check(m.movementSpent==2&&m.movementTurn==w.turn,"transport pays same center path and records billing turn");
        int spent=m.movementSpent;MarchOrders.Plan p=w.marches.previewCity(m.id,30);int move=0;
        for(int i=1;i<=p.stepsNow;i++)move+=w.army.moveCost(m,p.path.get(i-1),p.path.get(i));
        ok(w.marches.execute(p));check(m.movementSpent==spent+move&&m.movementSpent<=m.movementBudget,"immediate transport auto task retains bill");
        World loaded=SaveCodec.decode(SaveCodec.encode(w));Domestic.Mission copy=loaded.domestic.mission(m.id);
        check(copy!=null&&copy.movementSpent==m.movementSpent&&copy.movementTurn==m.movementTurn,"transport budget and turn roundtrip");
        int before=m.movementSpent;w.domestic.tick();check(m.movementSpent>=before&&m.movementSpent<=m.movementBudget,"same-turn first settlement cannot reset movement");
    }
    private static void nationalAndCrops()throws Exception {
        for(String scenario:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
            World w=ScenarioCatalog.load(scenario,0,63L);
            for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY)for(Hex cell:SiteFootprint.cells(c)){
                SourceGridCoord source=MapCoordinates.nationalSource(w,cell);
                check(MapCoordinates.fromNationalSource(w,source).equals(cell)&&w.cityAt(cell)==c,"national/source/crop odd-q footprint "+scenario);
            }
        }
    }
    public static void main(String[] args)throws Exception{
        departures();arrivals();transports();nationalAndCrops();
        System.out.println("CITY63 PASS: "+checks+" actual-runtime assertions; departure, entry, AI retreat, transport, actual nextTurn, SaveCodec and four scenarios");
    }
}
