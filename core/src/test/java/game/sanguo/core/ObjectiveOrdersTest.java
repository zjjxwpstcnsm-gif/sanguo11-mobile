package game.sanguo.core;

import java.util.*;

/** Focused v45 objective/embark regression; no historical fixture rewrites. */
public final class ObjectiveOrdersTest {
    private static int checks;
    private static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World fixture(){
        World w=new World(24,14,"甲军","乙军");
        w.cities.add(new World.City(10,"甲城",new Hex(1,1),0));
        w.cities.add(new World.City(20,"乙城",new Hex(20,10),1));
        w.cities.add(new World.City(21,"乙后城",new Hex(22,1),1));
        World.Officer a=new World.Officer(0,"我将",0,-1,90,90,90,90,90);a.role=Strategy.Role.RULER;a.loyalty=100;a.unitId=1;Arrays.fill(a.aptitude,3);w.officers.add(a);
        World.Officer b=new World.Officer(1,"敌将",1,21,50,50,50,50,50);b.role=Strategy.Role.RULER;b.loyalty=100;w.officers.add(b);
        w.units.add(new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,6),8000,15000));w.nextUnitId=2;return w;
    }
    private static void reset(World w){w.turn++;for(World.Unit u:w.fieldUnits())w.orders.reset(u);w.fieldworks.continueOwner(w.active);w.marches.advanceAll();}
    private static World.Unit enemy(World w,Hex h,int troops){World.Officer b=w.officer(1);b.cityId=-1;b.unitId=2;World.Unit u=new World.Unit(2,1,1,World.Weapon.SPEAR,h,troops,20000);w.units.add(u);w.nextUnitId=3;return u;}
    public static void main(String[] args)throws Exception{
        reachability();attackAndSave();captureAndEnter();repairAndStop();dynamicTargets();manualOverride();convoyExactlyOnce();packagedMaps();
        System.out.println("PASS v45 objective/port/map checks: "+checks);
    }
    private static void reachability()throws Exception{
        World w=fixture();World.Unit a=w.unit(1);for(int r=0;r<w.height;r++)w.terrain[11][r]=World.Terrain.WATER;
        byte[] before=SaveCodec.encode(w);check(!w.marches.preview(1,new Hex(18,6)).valid(),"river unreachable without port");check(Arrays.equals(before,SaveCodec.encode(w)),"unreachable preview has no mutations");
        Hex land=new Hex(10,6),water=new Hex(11,6);check(w.army.moveCost(a,land,water)<0,"manual movement also rejects wild shore");
        World.City p=new World.City(30,"渡口",new Hex(10,7),0);p.kind=World.SiteKind.PORT;w.cities.add(p);
        check(w.army.moveCost(a,land,water)>0,"same owned port dock edge permits embarking");
        MarchOrders.Plan plan=w.marches.preview(1,new Hex(18,6));check(plan.valid(),"reachable via dock");
        boolean dock=false;for(int i=1;i<plan.path.size();i++)if(!w.army.water(plan.path.get(i-1))&&w.army.water(plan.path.get(i))){check(w.army.embarkPort(0,plan.path.get(i-1),plan.path.get(i))!=null,"route only uses legal port");dock=true;}check(dock,"route crosses river");
        p.owner=1;check(!w.marches.preview(1,new Hex(18,6)).valid(),"enemy port must be captured");p.owner=0;
        ok(w.marches.execute(w.marches.preview(1,new Hex(18,6))));for(int i=0;a.march!=null&&i<12;i++)reset(w);check(a.hex.equals(new Hex(18,6))&&a.march==null,"cross river and land, no teleport");
        World blocked=fixture();for(Hex h:blocked.unit(1).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;check(!blocked.marches.preview(1,new Hex(12,6)).valid(),"mountain isolation rejected");
    }
    private static void attackAndSave()throws Exception{
        World w=fixture();World.Unit a=w.unit(1),b=enemy(w,new Hex(13,6),5000);
        MarchOrders.Plan plan=w.marches.preview(1,b.hex);check(plan.valid()&&plan.actionLabel.equals("自动攻击"),"target intent is attack");byte[] before=SaveCodec.encode(w);w.marches.preview(1,b.hex);check(Arrays.equals(before,SaveCodec.encode(w)),"attack preview pure");ok(w.marches.execute(plan));
        check(a.march!=null,"order survives first move");World restored=SaveCodec.decode(SaveCodec.encode(w));check(restored.unit(1).march.intent==MarchOrders.Intent.ATTACK,"intent persists in save27");
        int n=0;while(w.unit(2)!=null&&n++<35){int hp=w.unit(2).troops;reset(w);reset(restored);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"deterministic save continuation");if(w.unit(2)!=null&&w.unit(2).troops<hp){int once=w.unit(2).troops;w.marches.advanceAll();restored.marches.advanceAll();check(w.unit(2).troops==once,"no second attack in same turn");w.marches.advanceAll();restored.marches.advanceAll(); /* idempotent pause */}}
        check(w.unit(2)==null&&a.march==null,"continues until target defeated");
    }
    private static void captureAndEnter()throws Exception{
        World w=fixture();World.Unit a=w.unit(1);World.City c=w.city(20);a.hex=new Hex(19,10);c.defense=1;c.troops=100;Arrays.fill(c.equipment,0);c.food=0;c.gold=0;
        ok(w.marches.execute(w.marches.preview(1,c.hex)));check(c.owner==0&&w.unit(1)==null,"capture and adjacent entry in same completed order");check(w.officer(0).cityId==c.id&&c.troops>0,"garrison transfers officer and troops");
        World f=fixture();World.City home=f.city(10);home.troops=0;home.food=0;Arrays.fill(home.equipment,0);World.Unit u=f.unit(1);
        ok(f.marches.execute(f.marches.preview(1,home.hex)));for(int i=0;f.unit(1)!=null&&i<8;i++)reset(f);check(f.unit(1)==null&&home.troops==8000,"friendly garrison final action");
        World full=fixture();World.City base=full.city(10);base.troops=full.campaign.troopCap(base);World.Unit q=full.unit(1);q.hex=new Hex(2,1);ok(full.marches.execute(full.marches.preview(1,base.hex)));check(full.unit(1)==q&&q.march!=null&&q.march.paused.contains("容量"),"capacity waits without losing supplies");base.troops=0;reset(full);check(full.unit(1)==null,"capacity cleared then enters");
    }
    private static void repairAndStop()throws Exception{
        World w=fixture();World.Unit a=w.unit(1);War.Structure s=new War.Structure(1,0,War.StructureKind.FORTRESS,new Hex(12,6),100);w.war.structures.add(s);w.war.nextStructureId=2;
        ok(w.marches.execute(w.marches.preview(1,s.hex)));for(int i=0;a.march!=null&&i<40;i++)reset(w);check(s.hp==s.kind.hp&&s.builder==-1&&a.march==null,"walk then repair to completion");
        s.hp=1;a.hex=new Hex(11,6);w.orders.reset(a);ok(w.marches.execute(w.marches.preview(1,s.hex)));int hp=s.hp;check(s.builder==a.id&&a.march!=null,"repair reserves builder");ok(w.marches.stop(a.id));reset(w);check(s.hp==hp&&s.builder==-1,"stopping task stops automatic repair");
        ok(w.marches.execute(w.marches.preview(1,s.hex)));ok(w.fieldworks.stop(a.id));check(a.march==null,"legacy stop-construction also stops task");
    }
    private static void dynamicTargets()throws Exception{
        World w=fixture();World.Unit a=w.unit(1),b=enemy(w,new Hex(18,6),5000);ok(w.marches.execute(w.marches.preview(1,b.hex)));b.hex=new Hex(20,5);check(w.marches.current(a).target.equals(b.hex),"tracks target identity after movement");
        w.units.remove(b);w.officer(1).unitId=-1;w.officer(1).cityId=21;reset(w);check(a.march==null,"destroyed target completes, no phantom tile attack");
        World f=fixture();World.Unit u=f.unit(1);ok(f.marches.execute(f.marches.preview(1,f.city(20).hex)));f.city(20).owner=0;reset(f);check(u.march!=null&&u.march.intent==MarchOrders.Intent.GARRISON,"friendly capture converts to enter intent");
        World lost=fixture();lost.cities.add(new World.City(31,"友后城",new Hex(1,12),0));World.Unit q=lost.unit(1);ok(lost.marches.execute(lost.marches.preview(1,lost.city(10).hex)));lost.city(10).owner=1;reset(lost);check(q.march==null,"friendly target lost stops instead of declaring attack");
        World treaty=fixture();World.Unit friend=treaty.unit(1);World.Unit foe=enemy(treaty,new Hex(15,6),5000);ok(treaty.marches.execute(treaty.marches.preview(1,foe.hex)));treaty.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);reset(treaty);check(friend.march==null&&foe.troops==5000,"treaty ends automatic attack without harming ally");
        World legacy=fixture();World.Unit old=legacy.unit(1);old.march=new MarchOrders.Order(MarchOrders.Kind.CITY,legacy.city(20).hex,20,1);for(int i=0;old.march!=null&&i<20;i++)reset(legacy);check(legacy.city(20).defense==3000&&legacy.city(20).owner==1,"legacy approach is not promoted to attack");
    }
    private static void manualOverride()throws Exception{
        World w=fixture();World.Unit a=w.unit(1);ok(w.marches.execute(w.marches.preview(1,new Hex(18,6))));w.orders.reset(a);ok(w.war.waitUnit(a.id));check(a.march==null,"manual wait replaces auto order");
    }
    private static void convoyExactlyOnce()throws Exception{
        World w=fixture();World.City to=new World.City(30,"接收城",new Hex(7,3),0);w.cities.add(to);
        World.Officer officer=new World.Officer(2,"运输将",0,10,60,60,60,60,60);w.officers.add(officer);
        ok(w.domestic.transport(10,30,2,new int[0],100,3000,1000,new int[4],false,true));
        Domestic.Mission m=w.domestic.missions.get(0);ok(w.marches.execute(w.marches.preview(m,to.hex)));
        World restored=SaveCodec.decode(SaveCodec.encode(w));check(restored.domestic.missions.get(0).march.intent==MarchOrders.Intent.GARRISON,"convoy intent saved");
        for(int i=0;i<8;i++){w.turn++;w.domestic.tick();}
        check(to.gold==5100&&to.troops==13000,"automatic convoy delivery exactly once");
        check(w.officer(2).cityId==10||w.domestic.busy(2),"unloaded crew returns without second cargo receipt");
    }
    private static void packagedMaps()throws Exception{
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(s.id,0);World restored=SaveCodec.decode(SaveCodec.encode(w));check(restored.cities.size()==w.cities.size(),"scenario save round trip "+s.id);
            World.City wu=w.city(20023),qu=w.city(20070);if(wu!=null&&qu!=null){check(wu.hex.distance(qu.hex)<=4,"QuA near Wu "+s.id);check(qu.hex.neighbors().stream().anyMatch(w.army::water),"QuA water frontage "+s.id);}
            World.City j=w.city(20005),h=w.city(20042),y=w.city(20006);if(j!=null&&h!=null&&y!=null)check(j.hex.r<h.hex.r&&h.hex.r<y.hex.r,"Huguan lies between Jinyang and Ye "+s.id);
        }
    }
}
