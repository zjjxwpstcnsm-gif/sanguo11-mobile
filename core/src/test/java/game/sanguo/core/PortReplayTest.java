package game.sanguo.core;

import java.util.*;
import java.lang.reflect.*;

/** Reproductions for v47: actual core commands, not a second simulated pathfinder. */
public final class PortReplayTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World fixture(){
        World w=new World(24,14,"甲军","乙军");
        w.cities.add(new World.City(10,"甲城",new Hex(1,1),0));
        w.cities.add(new World.City(20,"乙城",new Hex(20,10),1));
        w.cities.add(new World.City(21,"乙后城",new Hex(22,1),1));
        World.Officer a=new World.Officer(0,"我将",0,-1,90,90,90,90,90);a.role=Strategy.Role.RULER;a.loyalty=100;a.unitId=1;Arrays.fill(a.aptitude,3);w.officers.add(a);
        World.Officer b=new World.Officer(1,"敌将",1,21,50,50,60,50,50);b.role=Strategy.Role.RULER;b.loyalty=100;Arrays.fill(b.aptitude,3);w.officers.add(b);
        w.units.add(new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,6),8000,30000));w.nextUnitId=2;return w;
    }
    private static World.City port(World w,int id,int q,int r,int owner){World.City c=new World.City(id,"港"+id,new Hex(q,r),owner);c.kind=World.SiteKind.PORT;c.baseDefense=2000;c.defense=1;c.troops=1;w.cities.add(c);return c;}
    private static World.Unit foe(World w,Hex h){World.Officer o=w.officer(1);o.cityId=-1;o.unitId=2;World.Unit u=new World.Unit(2,1,1,World.Weapon.SPEAR,h,6000,15000);w.units.add(u);w.nextUnitId=3;return u;}
    public static void main(String[] args)throws Exception {
        portEdges();aiDocks(false);aiDocks(true);friendlyDetour();capture();journalCommands();journalTurn();
        System.out.println("PASS v47: "+checks+" port, AI, capture and journal assertions");
    }
    private static void portEdges()throws Exception{
        World w=fixture();World.Unit u=w.unit(1);for(int r=0;r<w.height;r++)for(int q=9;q<=10;q++)w.terrain[q][r]=World.Terrain.WATER;
        check(w.army.moveCost(u,new Hex(8,6),new Hex(9,6))<0,"no wild embark");check(w.army.moveCost(u,new Hex(10,6),new Hex(11,6))<0,"no wild landing");
        World.City west=port(w,30,8,7,0);check(w.army.moveCost(u,new Hex(8,6),new Hex(9,6))>0,"owned dock embark");
        check(!w.marches.preview(1,new Hex(17,6)).valid(),"cannot route across without landing dock");
        World.City east=port(w,31,11,5,0);MarchOrders.Plan p=w.marches.preview(1,new Hex(17,6));check(p.valid(),p.error);
        for(int i=1;i<p.path.size();i++)check(w.army.moveCost(u,p.path.get(i-1),p.path.get(i))>0,"every planned edge executable");
        u.hex=new Hex(10,6);east.owner=1;check(!w.enter(u.id,east.id).ok,"hostile port is not a landing shortcut");east.owner=0;
        check(w.army.canEnterSite(u,u.hex,east),"owned shore port entry");
        // The v47 single-cell fixture overlapped the boat and the owned dock after
        // v55 introduced seven-cell cities. Use a legal seven-cell city away from docks.
        u.hex=new Hex(10,10);World.City fakeCity=new World.City(40,"隔水城",new Hex(12,10),0);
        for(Hex tile:SiteFootprint.cells(fakeCity))w.terrain[tile.q][tile.r]=World.Terrain.PLAIN;w.cities.add(fakeCity);
        check(!w.army.canEnterSite(u,u.hex,fakeCity),"non-port garrison cannot bypass landing rule");
        byte[] before=SaveCodec.encode(w);w.marches.preview(u.id,new Hex(17,6));check(Arrays.equals(before,SaveCodec.encode(w)),"preview preserves RNG and game state");
        west.owner=1;east.owner=1;check(!w.marches.preview(u.id,new Hex(17,6)).valid(),"port owner loss invalidates future path");
    }
    private static void aiDocks(boolean stranded)throws Exception{
        World w=fixture();for(int r=0;r<w.height;r++)for(int q=9;q<=10;q++)w.terrain[q][r]=World.Terrain.WATER;
        port(w,30,8,7,0);World.City east=port(w,31,11,5,1);World.Unit u=w.unit(1);if(stranded)u.hex=new Hex(9,6);
        int initial=w.city(20).defense;boolean sailed=stranded,landed=false;List<Hex> trace=new ArrayList<>();
        for(int turn=0;turn<22&&w.unit(1)!=null&&w.city(20).defense==initial;turn++){
            w.turn++;w.orders.reset(u);new CampaignAi(w).runUnit(u,true,c->c.id==20,c->c.owner==0);trace.add(u.hex);
            sailed|=w.army.water(u.hex);landed|=u.hex.q>10;
        }
        check(east.owner==0,"AI captures necessary intermediate hostile port "+stranded+" "+trace);
        check(sailed&&landed,"AI completes embark, sail and land "+stranded+" "+trace);
        check(w.city(20).defense<initial,"AI resumes original inland objective "+stranded+" "+trace);
    }
    private static void friendlyDetour()throws Exception{
        World w=fixture();w.cities.removeIf(c->c.id==20);w.cities.add(new World.City(20,"目标城",new Hex(20,6),1));
        for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.MOUNTAIN);
        for(int q=2;q<=21;q++)w.terrain[q][6]=World.Terrain.PLAIN;
        for(int r=2;r<=6;r++){w.terrain[3][r]=World.Terrain.PLAIN;w.terrain[9][r]=World.Terrain.PLAIN;}
        for(int q=3;q<=9;q++)w.terrain[q][2]=World.Terrain.PLAIN;
        for(World.City c:w.cities)w.terrain[c.hex.q][c.hex.r]=World.Terrain.PLAIN;
        World.Officer f=new World.Officer(2,"友将",0,-1,70,70,70,70,70);f.unitId=2;w.officers.add(f);World.Unit blocker=new World.Unit(2,0,2,World.Weapon.SPEAR,new Hex(5,6),5000,30000);w.units.add(blocker);w.nextUnitId=3;
        CampaignAi ai=new CampaignAi(w);Method route=CampaignAi.class.getDeclaredMethod("route",World.Unit.class,Hex.class,int.class);route.setAccessible(true);
        Object found=route.invoke(ai,w.unit(1),w.city(20).hex,1);check(found!=null,"alternate route found");Field path=found.getClass().getDeclaredField("path");path.setAccessible(true);
        @SuppressWarnings("unchecked") List<Hex> actual=(List<Hex>)path.get(found);
        check(!actual.contains(blocker.hex),"available long detour must beat fictitious passage through friend");
        check(actual.stream().anyMatch(h->h.r==2),"goes around the long corridor, not against the friend");
        ai.runUnit(w.unit(1),true,c->c.id==20,c->false);check(w.unit(1).hex.r<6,"real AI follows detour this turn");
    }
    private static void capture()throws Exception{
        int destroyed=0,total=0;
        for(int seed=0;seed<120;seed++){
            World w=fixture();World.City c=w.city(20);w.unit(1).hex=new Hex(18,10);c.defense=1;c.troops=100;
            int id=1;for(Hex h:SiteFootprint.edge(c))if(!h.equals(w.unit(1).hex)&&id<=5){w.domestic.facilities.add(new Domestic.Facility(id++,20,Domestic.Kind.FARM,h,-1,0));}
            w.domestic.nextFacilityId=id;w.strategy.setSeed(seed);World copy=SaveCodec.decode(SaveCodec.encode(w));int before=w.domestic.facilities.size();
            ok(w.siege(1,20));ok(copy.siege(1,20));check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"capture saved RNG replay deterministic");
            destroyed+=before-w.domestic.facilities.size();total+=before;
            for(Hex h:SiteFootprint.edge(c))if(w.domestic.at(h)==null&&!h.equals(w.unit(1).hex))check(w.unitAt(h)==null&&w.war.at(h)==null&&w.army.moveCost(w.unit(1),c.hex,h)>0,"destroyed facilities release occupation");
            byte[] after=SaveCodec.encode(w);check(!w.siege(1,20).ok&&Arrays.equals(after,SaveCodec.encode(w)),"repeated invalid attack does not roll another sack");
        }
        check(destroyed>total*.15&&destroyed<total*.35,"independent 25% loss distribution "+destroyed+"/"+total);
        World peace=fixture();port(peace,30,8,7,1);
        byte[] before=SaveCodec.encode(peace);check(peace.domestic.sack(30).isEmpty(),"ports have no domestic sacking");check(Arrays.equals(before,SaveCodec.encode(peace)),"non-city sack no RNG");
    }
    private static void journalCommands()throws Exception{
        World w=fixture();World.Unit target=foe(w,new Hex(6,6));World visual=SaveCodec.decode(SaveCodec.encode(w));TurnJournal journal=new TurnJournal(w);
        ok(w.move(1,new Hex(5,6)));ok(w.attack(1,target.id));w.orders.reset(w.unit(1));
        ok(w.war.plot(1,target.hex,War.Plot.CONFUSE));journal.close();
        check(journal.events().stream().anyMatch(e->e.kind==TurnJournal.Kind.MOVE&&e.path.size()>1),"movement retains exact visited route");
        check(journal.events().stream().anyMatch(e->e.kind==TurnJournal.Kind.ATTACK&&!e.impacts.isEmpty()),"attack and counter damage are visible deltas");
        check(journal.events().stream().anyMatch(e->e.kind==TurnJournal.Kind.PLOT),"stratagem event retained independent of success");
        byte[] after=SaveCodec.encode(w);for(TurnJournal.Event e:journal.events())e.applyVisual(visual);
        check(visual.unit(1).hex.equals(w.unit(1).hex)&&visual.unit(2).troops==w.unit(2).troops&&visual.unit(2).status==w.unit(2).status,"visual deltas reach actual unit result");
        for(TurnJournal.Event e:journal.events())e.applyVisual(visual);check(Arrays.equals(after,SaveCodec.encode(w)),"replay or repeated visuals cannot mutate authoritative outcome");
    }
    private static void journalTurn()throws Exception{
        World a=fixture();foe(a,new Hex(7,6));a.unit(1).march=new MarchOrders.Order(MarchOrders.Kind.TILE,new Hex(16,6),-1,-1,MarchOrders.Intent.MOVE);
        for(int n=0;n<4&&!a.gameOver();n++){
            World b=SaveCodec.decode(SaveCodec.encode(a));World visual=SaveCodec.decode(SaveCodec.encode(a));TurnJournal journal=new TurnJournal(b);
            ok(a.nextTurn());ok(b.nextTurn(p->journal.checkpoint(p.phase)));journal.close();
            check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"journal on/off whole-turn rules identical "+n);
            for(TurnJournal.Event e:journal.events())e.applyVisual(visual);
            check(visual.units.size()==b.units.size()&&visual.domestic.facilities.size()==b.domestic.facilities.size(),"render lists match final counts "+n);
            for(World.Unit u:b.units)check(visual.unit(u.id)!=null&&visual.unit(u.id).hex.equals(u.hex)&&visual.unit(u.id).troops==u.troops,"every final unit matches after playback");
        }
    }
}
