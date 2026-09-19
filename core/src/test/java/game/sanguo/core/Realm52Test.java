package game.sanguo.core;

import java.util.*;
import java.security.MessageDigest;

public final class Realm52Test {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        accounts();relations();recruitmentQueries();immediateMoves();geography();critical();national();
        System.out.println("REALM52 PASS "+checks+" account, cache invalidation, critical provenance and exact national-turn checks");
    }
    private static void accounts()throws Exception{
        World w=Turn48Fixture.world();byte[] before=SaveCodec.encode(w);RealmOverview all=new RealmOverview(w);
        check(all.factions.size()==3&&all.units.size()==3,"all factions and actual units");
        RealmOverview.Faction f=all.factions.get(0);check(f.cities==1&&f.units==1&&f.officers==3,"counts by real owner");
        check(f.gold==35000&&f.food==130000&&f.troops==32000,"stocks plus field cargo counted once");
        check(f.goldIncome==0&&f.foodIncome==0,"no fictional every-turn income on a non-payment date");
        check(f.foodUse==w.cityFoodUse(w.city(0))+Logistics.foodUse(w,w.unit(1)),"shared ration formula including aura");
        check(f.goldUse==0&&f.netFood()==-f.foodUse,"explicit upkeep and projected net");
        RealmOverview.researchSummary(w,0);check(Arrays.equals(before,SaveCodec.encode(w)),"overview does not mutate AP, rules or RNG");
        w.turn=2;w.city(0).gold=w.campaign.goldCap(w.city(0))-5;
        check(new RealmOverview(w).factions.get(0).goldIncome==5,"forecast respects capacity at scheduled collection");
        w.campaign.finishTech(0,Campaign.Tech.SPEAR_DRILL);
        check(new RealmOverview(w).factions.get(0).techs>0,"learned technique count uses actual state");
        // A pending convoy at a city still owns cargo until unloading. Not a battlefield duplicate.
        Domestic.Mission m=new Domestic.Mission(77,0,0,0,1,w.city(0).hex,true,120,900,200,new int[9]);
        long gold=new RealmOverview(w).factions.get(0).gold;w.domestic.missions.add(m);
        RealmOverview added=new RealmOverview(w);check(added.factions.get(0).gold==gold+120&&added.factions.get(0).convoys==1,"waiting undelivered convoy is included once");
        m.legacyOverlap=true;check(new RealmOverview(w).factions.get(0).gold==gold,"legacy duplicate convoy excluded");
    }
    private static void relations()throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0,12345L);
        for(int n=0;n<w.officers.size();n+=11){World.Officer t=w.officers.get(n);
            boolean bond=false;if(t.owner>=0&&w.alive(t.owner))for(World.Officer o:w.officers)if(o.owner==t.owner&&w.life.present(o.id)&&w.relations.bonded(t.id,o.id)){bond=true;break;}
            check(w.relations.loyalBond(t.id)==bond,"adjacency loyal bond matches roster scan");
            for(int side=0;side<w.factions.length;side++){
                int recruiter=w.officers.get(side%w.officers.size()).id;boolean refuses=bond||w.relations.dislikes(t.id,recruiter);
                int bonus=w.relations.bonded(t.id,recruiter)||w.relations.likes(t.id,recruiter)?20:0;
                for(World.Officer o:w.officers)if(o.owner==side){if(o.role==Strategy.Role.RULER&&w.relations.dislikes(t.id,o.id))refuses=true;if(bonus==0&&(w.relations.bonded(t.id,o.id)||w.relations.likes(t.id,o.id)))bonus=10;}
                check(w.relations.refuses(t.id,recruiter,side)==refuses,"adjacency refusal matches roster scan");check(w.relations.recruitmentBonus(t.id,recruiter,side)==bonus,"adjacency recruitment bonus matches scan");
            }
        }
    }
    private static void recruitmentQueries()throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0,12345L);byte[] initial=SaveCodec.encode(w);
        for(World.Weapon weapon:World.Weapon.values()){
            World.Unit probe=new World.Unit(900001,0,w.officers.get(0).id,weapon,w.home().hex,1000,2000);Army.MovementCosts costs=w.army.movementCosts(probe);
            for(int q=0;q<w.width;q+=13)for(int r=0;r<w.height;r+=11){Hex from=new Hex(q,r);for(Hex to:from.neighbors()){
                int expected=w.army.moveCost(probe,from,to);if(costs.cost(from,to)!=expected||costs.cost(from,to)!=expected)throw new AssertionError("query cost cache differs from authoritative movement");}}
            check(true,"query-local movement cost matches authoritative edges for "+weapon);
        }

        World.City c=w.home();List<World.Officer> idle=w.idle(c);int n=0;
        for(World.Officer target:w.strategy.recruitmentTargets(c.id)){
            List<World.Officer> expected=new ArrayList<>(idle);
            expected.sort(Comparator.comparingInt((World.Officer o)->w.strategy.recruitmentChance(c.id,o.id,target.id)).reversed()
                .thenComparingInt(o->{int d=Loyalty.distance(o,target);return d<0?76:d;}).thenComparingInt(o->o.id));
            check(expected.equals(w.loyalty.recruitmentActors(c.id,target.id)),"scored recruitment list preserves exact probability/affinity/id order");
            check(expected.get(0)==w.loyalty.bestRecruiter(c.id,target.id,idle),"AI best recruiter equals the complete original sorted list");
            if(++n>=40)break;
        }
        check(Arrays.equals(initial,SaveCodec.encode(w)),"ranked read scopes change no world field or RNG");
        World.Officer[] expected=new World.Officer[w.factions.length];for(int side=0;side<expected.length;side++)expected[side]=w.loyalty.ruler(side);
        w.loyalty.readRulers(()->{for(int side=0;side<expected.length;side++)check(expected[side]==w.loyalty.ruler(side),"scoped ruler equals fresh ordered lookup");return null;});
        World.Officer old=expected[0];Strategy.Role role=old.role;old.role=Strategy.Role.OFFICER;
        check(w.loyalty.ruler(0)!=old,"scope cannot retain a stale ruler after direct role mutation");
        try{w.loyalty.readRulers(()->{throw new IllegalStateException("expected");});}catch(IllegalStateException expectedError){}
        old.role=role;check(w.loyalty.ruler(0)==old,"failed read scope is discarded before next lookup");
    }
    private static void immediateMoves()throws Exception{
        World w=Realm52Fixture.criticalWorld(true);UnitOrders.MovePlan plan=null;
        for(Hex h:w.unit(1).hex.neighbors()){UnitOrders.MovePlan test=w.orders.previewMove(1,h);if(test.valid()){plan=test;break;}}
        check(plan!=null,"valid route fixture");World copy=SaveCodec.decode(SaveCodec.encode(w));
        check(w.orders.execute(plan).ok&&copy.orders.executeImmediateRoute(1,plan.path).ok,"public validated and worker-immediate routes execute");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"immediate route retains movement costs, entry modes and all authoritative fields");
        World immediate=Realm52Fixture.criticalWorld(true);
        check(immediate.orders.executeImmediateMove(1,plan.path.get(plan.path.size()-1)).ok,"synchronous destination planner executes");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(immediate)),"immediate destination preserves public planner path, cost, journal and state");
        byte[] unchanged=SaveCodec.encode(copy);List<Hex> bad=Arrays.asList(copy.unit(1).hex,new Hex(21,15));
        check(!copy.orders.executeImmediateRoute(1,bad).ok&&Arrays.equals(unchanged,SaveCodec.encode(copy)),"worker routes still reject invalid nonadjacent edges atomically");
        World stale=Realm52Fixture.criticalWorld(true);UnitOrders.MovePlan stalePlan=stale.orders.previewMove(1,plan.path.get(plan.path.size()-1));
        check(stalePlan.valid(),"stale route starts valid");stale.unit(1).food++;byte[] altered=SaveCodec.encode(stale);
        check(!stale.orders.execute(stalePlan).ok&&Arrays.equals(altered,SaveCodec.encode(stale)),"player-facing previews retain full stale-world rejection");
    }
    private static void geography(){
        World w=new World(30,20,"甲","乙");w.cities.add(new World.City(9,"东",new Hex(24,9),1));w.cities.add(new World.City(1,"西",new Hex(2,3),0));w.cities.add(new World.City(4,"中",new Hex(13,8),0));
        Hex near=new Hex(13,9);check(w.personnel.region(near).id==4,"nearest-city region");
        List<Integer> route=w.personnel.route(w.city(1).hex,9);check(route.equals(w.personnel.route(w.city(1).hex,9)),"cached route keeps BFS order");
        w.city(4).kind=World.SiteKind.PORT;check(w.personnel.region(near).id!=4,"same-size mutable kind invalidates regions");
        w.city(4).kind=World.SiteKind.CITY;check(w.personnel.region(near).id==4,"restoring city kind invalidates regions");
        w.cities.set(2,new World.City(4,"改址",new Hex(3,4),0));check(w.personnel.region(near).id!=4,"same-id replacement invalidates geometry");
        w.terrainRevision++;check(!w.personnel.route(w.city(1).hex,9).isEmpty(),"terrain revision rebuild preserves reachability");
        w.cities.remove(2);check(w.personnel.region(near)!=null,"site deletion invalidates safely");
    }
    private static void critical()throws Exception{
        World w=Realm52Fixture.criticalWorld(true),plain=SaveCodec.decode(SaveCodec.encode(w));byte[] initial=SaveCodec.encode(w);
        w.war.tacticPreview(1,2,War.Tactic.SPIRAL);check(Arrays.equals(initial,SaveCodec.encode(w)),"critical preview is pure");
        TurnJournal journal=new TurnJournal(w);World.Result r=Realm52Fixture.criticalCommand(w);journal.close();
        check(r.ok&&r.critical!=null&&r.critical.name.equals("赵云")&&r.critical.tactic.equals(War.Tactic.SPIRAL.label),"paid damaging critical carries actual commander and tactic");
        check(journal.events().stream().filter(e->e.critical!=null).count()==1,"one journal highlight per actual command");
        CriticalHit hit=r.critical;w.officer(1).war=20;check(hit.officerCopy().war==90,"portrait uses an immutable officer snapshot");w.officer(1).war=90;
        Realm52Fixture.criticalCommand(plain);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"highlight emission cannot change damage or RNG");
        check(w.fail("failed").critical==null&&w.success("next command").critical==null,"critical does not leak into later or failed commands");
        World normal=Realm52Fixture.criticalWorld(false);check(Realm52Fixture.criticalCommand(normal).critical==null,"noncritical tactic has no highlight");
        boolean missed=false;for(int seed=0;seed<300&&!missed;seed++){
            World m=Realm52Fixture.criticalWorld(true);m.unit(2).status=War.Status.NORMAL;m.unit(2).statusTurns=0;m.strategy.setSeed(seed);
            int troops=m.unit(2).troops;World.Result miss=Realm52Fixture.criticalCommand(m);
            if(m.unit(2)!=null&&m.unit(2).troops==troops){check(miss.ok&&miss.critical==null,"missed critical-capable tactic never emits cut-in");missed=true;}
        }
        check(missed,"explicit missed-tactic fixture exercised");
    }
    private static void national()throws Exception{
        String[] goldens={"1ae4521c16defb8c43250e35da3cc81019112816e6271e4f84b66c38b8e0826a","2374c4549c7ab176cfda91566486fad5e69da955c153b2cfda13d8b656e7438b","da27cfd72068a454ee5b43b196c68387b5451a3a0db83e4eafa4f92e2c5dce68","032a45c1054516902ea6fea62fdfd03e686bd3981f1b0881560e3d9cd5fa0851","d3cf0274fa0ed2b00d2a1fca56d43a02979783755cbb6700f0f56051b7c32d2a","ee828a3292c29b67ef3c78f5d5b0ffb3673ef55667a8e94c8e284502d78bd079"};
        World w=ScenarioCatalog.load("heroes-250",0,12345L);check(w.factions.length==28,"real 250 scenario has 28 factions");
        for(int i=0;i<goldens.length;i++){
            // Real app decodes a fresh authoritative copy every turn; include that cold-cache path.
            w=SaveCodec.decode(SaveCodec.encode(w));long start=System.nanoTime();TurnJournal journal=new TurnJournal(w);
            check(w.nextTurn(p->{journal.checkpoint(p.phase);if(p.boundary)journal.drainEvents();}).ok,"national next turn succeeds");journal.close();
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));
            check(hash.equals(goldens[i]),"v51 exact save and RNG identity at national turn "+(i+1));
            System.out.printf(Locale.ROOT,"NATIONAL52 turn=%d computeMs=%.1f units=%d sha256=%s%n",i+1,(System.nanoTime()-start)/1e6,w.units.size(),hash);
        }
    }
}
