package game.sanguo.core;

import java.util.*;
import java.io.*;

/** v55 production-command regression. Fixtures arrange situations; assertions call the shipped engine. */
public final class CityFootprint55Test {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    static World fixture(){
        World w=new World(36,26,"甲军","乙军");
        w.cities.add(new World.City(10,"甲城",new Hex(10,10),0));
        w.cities.add(new World.City(20,"乙城",new Hex(27,10),1));
        w.cities.add(new World.City(21,"乙后城",new Hex(29,21),1));
        for(int i=0;i<6;i++){World.Officer o=new World.Officer(i,"甲将"+i,0,10,90,90,90,90,90);Arrays.fill(o.aptitude,3);o.loyalty=100;if(i==0)o.role=Strategy.Role.RULER;w.officers.add(o);}
        World.Officer enemy=new World.Officer(10,"乙君",1,21,70,70,70,70,70);enemy.role=Strategy.Role.RULER;enemy.loyalty=100;w.officers.add(enemy);
        for(World.City c:w.cities){c.troops=10000;c.gold=10000;c.food=100000;for(int e=0;e<c.equipment.length;e++)c.equipment[e]=e<4?20000:e==4?0:20;}
        w.strategy.initializeOffices();return w;
    }
    static World.Unit unit(World w,Hex h,World.Weapon weapon){
        World.Officer o=w.officer(1);w.strategy.releaseGovernor(o.id);o.cityId=-1;o.unitId=w.nextUnitId;
        World.Unit u=new World.Unit(w.nextUnitId++,0,o.id,weapon,h,6000,16000);u.gold=123;w.units.add(u);return u;
    }
    private static void reset(World w){w.turn++;for(World.Unit u:w.fieldUnits())w.orders.reset(u);w.marches.advanceAll();}
    private static void corridor(World w){for(int q=4;q<=17;q++)for(int r=3;r<=17;r++)w.terrain[q][r]=World.Terrain.MOUNTAIN;for(int q=4;q<=17;q++)w.terrain[q][10]=World.Terrain.PLAIN;for(Hex h:SiteFootprint.cells(w.city(10)))w.terrain[h.q][h.r]=World.Terrain.PLAIN;}
    public static void main(String[] args)throws Exception{
        geometry();entry();transit();permissions();attacks();engines();lightning();deployment();ports();convoys();ai();geographicRoutes();maps();
        System.out.println("CITY55 PASS: "+checks+" checks (formal movement, garrison, combat, AI, maps, turns and saves)");
    }
    private static void geometry(){
        for(int x=4;x<11;x++)for(int y=4;y<11;y++){
            World.City c=new World.City(1,"城",MapCoordinates.axial(x,y,26),0);List<Hex> cells=SiteFootprint.cells(c);
            check(cells.size()==7&&new HashSet<>(cells).size()==7,"seven unique cells at "+x+","+y);
            Map<Integer,Integer> rows=new TreeMap<>();for(Hex h:cells){Hex source=MapCoordinates.source(h,26);rows.merge(source.r,1,Integer::sum);}
            check(new ArrayList<>(rows.values()).equals(Arrays.asList(2,3,2)),"2/3/2 at both parities");
            check(SiteFootprint.edge(c).size()==12,"twelve unique exterior cells");
            c.kind=World.SiteKind.GATE;check(SiteFootprint.cells(c).size()==1,"gate cache refresh is one cell");c.kind=World.SiteKind.PORT;check(SiteFootprint.cells(c).size()==1,"port one");
        }
    }
    private static void entry()throws Exception{
        for(Hex h:SiteFootprint.cells(fixture().city(10))){
            World w=fixture();World.City c=w.city(10);World.Unit u=unit(w,h,World.Weapon.SPEAR);u.deputies=new int[]{2,3};for(int id:u.deputies){w.strategy.releaseGovernor(id);w.officer(id).cityId=-1;w.officer(id).unitId=u.id;}
            check(w.cityAt(h)==c&&w.unitAt(h)==u,"field unit and city independently indexed");
            int soldiers=c.troops,food=c.food,gold=c.gold;ok(w.enter(u.id,c.id));
            check(w.unit(u.id)==null&&c.troops==soldiers+6000&&c.food==food+16000&&c.gold==gold+123,"exact once stock transfer from "+h);
            check(w.officer(1).cityId==c.id&&w.officer(2).cityId==c.id&&w.officer(3).cityId==c.id,"all three crew in same entity");
            check(!w.enter(u.id,c.id).ok&&c.troops==soldiers+6000,"repeat command cannot duplicate resources");
            SaveCodec.decode(SaveCodec.encode(w));
        }
        for(Hex h:SiteFootprint.edge(fixture().city(10))){World w=fixture();World.Unit u=unit(w,h,World.Weapon.SPEAR);MarchOrders.Plan p=w.marches.previewCity(u.id,10);check(p.valid(),"all twelve approaches reachable: "+p.error);ok(w.marches.execute(p));check(w.unit(u.id)==null,"edge entry needs no center step");}
        World w=fixture();World.Unit u=unit(w,new Hex(4,10),World.Weapon.SPEAR);w.terrain[8][10]=World.Terrain.MOUNTAIN;
        MarchOrders.Plan p=w.marches.previewCity(u.id,10);check(p.valid(),"find alternative actually reachable entrance");for(Hex h:p.path)check(w.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN,"never choose blocked straight line");
        Hex last=p.path.get(p.path.size()-1);check(SiteFootprint.contains(w.city(10),last)&&!last.equals(w.city(10).hex),"route targets first legal edge, not center");
    }
    private static void transit()throws Exception{
        World w=fixture();corridor(w);World.Unit u=unit(w,new Hex(7,10),World.Weapon.SPEAR);u.movementBudget=2;
        MarchOrders.Plan p=w.marches.previewMove(u.id,new Hex(14,10));check(p.valid()&&p.cost==7,"seven normal flat costs, not free transit");byte[] before=SaveCodec.encode(w);w.marches.previewMove(u.id,new Hex(14,10));check(Arrays.equals(before,SaveCodec.encode(w)),"preview pure");
        ok(w.marches.execute(p));check(u.hex.equals(new Hex(9,10))&&u.movementSpent==2&&u.march.intent==MarchOrders.Intent.MOVE,"stops on rim, consumes exactly 2, preserves march");check(w.city(10).troops==10000,"passing never transfers stock");
        World restored=SaveCodec.decode(SaveCodec.encode(w));check(restored.unit(u.id)!=null&&restored.unit(u.id).march.intent==MarchOrders.Intent.MOVE,"save on city footprint retains field unit and intent");
        for(int i=0;i<4&&u.march!=null;i++){reset(w);reset(restored);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"cross-turn replay deterministic");}
        check(u.hex.equals(new Hex(14,10))&&w.unit(u.id)==u&&u.march==null,"crosses city and finishes remote tile without garrison");
        World formal=fixture();corridor(formal);World.Unit v=unit(formal,new Hex(7,10),World.Weapon.SPEAR);v.movementBudget=2;ok(formal.marches.execute(formal.marches.previewMove(v.id,new Hex(14,10))));
        for(World.City c:formal.cities)if(c.owner==1){c.gold=0;c.food=0;c.troops=0;}ok(formal.nextTurn());check(formal.unit(v.id)!=null&&v.hex.q>9,"actual nextTurn resumes city-crossing command");
        World occupied=fixture();World.Unit a=unit(occupied,new Hex(7,10),World.Weapon.SPEAR);World.Officer o=occupied.officer(2);o.cityId=-1;o.unitId=occupied.nextUnitId;World.Unit block=new World.Unit(occupied.nextUnitId++,0,2,World.Weapon.SPEAR,new Hex(10,10),1000,2000);occupied.units.add(block);
        MarchOrders.Plan alternate=occupied.marches.previewMove(a.id,new Hex(14,10));check(alternate.valid()&&!alternate.path.contains(block.hex),"ordinary field-unit occupancy retained inside city");
    }
    private static void permissions(){
        World w=fixture();corridor(w);World.Unit u=unit(w,new Hex(7,10),World.Weapon.SPEAR);MarchOrders.Plan stale=w.marches.previewMove(u.id,new Hex(14,10));w.city(10).owner=1;check(!w.marches.execute(stale).ok,"owner change invalidates preview");
        check(w.army.moveCost(u,new Hex(8,10),new Hex(9,10))<0,"enemy body cannot be entered");
        w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,10);check(w.army.moveCost(u,new Hex(8,10),new Hex(9,10))<0,"alliance not ownership");
        u.hex=new Hex(10,10);u.march=new MarchOrders.Order(MarchOrders.Kind.TILE,new Hex(14,10),-1,-1,MarchOrders.Intent.MOVE);SiteFootprint.ownershipChanged(w,w.city(10));
        check(w.unit(u.id)==u&&u.troops==6000&&u.march==null,"capture under field unit preserves army, cancels stale task");
        check(w.army.moveCost(u,u.hex,new Hex(11,10))==1&&w.army.moveCost(u,new Hex(11,10),new Hex(12,10))==1,"enclosed unit has paid outward escape");
        check(w.army.moveCost(u,new Hex(11,10),new Hex(10,10))<0,"cannot move deeper into enemy body");
    }
    private static void attacks()throws Exception{
        for(Hex position:SiteFootprint.edge(fixture().city(20))){World w=fixture();World.Unit u=unit(w,position,World.Weapon.SPEAR);World.City c=w.city(20);Hex hit=w.siegeHit(u,c);check(hit!=null&&u.hex.distance(hit)==1,"melee finds all exterior edges");
            byte[] before=SaveCodec.encode(w);Displacement.Preview p=w.siegePreview(u.id,c.id,hit);check(p.valid()&&p.riskHexes.contains(hit),"preview names actual rim hit");check(Arrays.equals(before,SaveCodec.encode(w)),"siege preview pure");int hp=c.defense;World.Result result=w.siege(u.id,c.id,hit);ok(result);check(hit.equals(result.impact),"preview and result impact identical");check(c.defense<hp&&u.acted,"one exterior attack applies once");int once=c.defense;check(!w.siege(u.id,c.id,hit).ok&&c.defense==once,"no repeated same-turn strike");}
        World w=fixture();World.Unit u=unit(w,new Hex(25,10),World.Weapon.CROSSBOW);World.City c=w.city(20);Hex far=SiteFootprint.hit(c,u.hex,3,3,null,h->true);check(SiteFootprint.distance(c,u.hex)==1&&far!=null&&u.hex.distance(far)==3,"minimum range existential check, not nearest-distance shortcut");
        check(SiteFootprint.hit(c,u.hex,3,3,null,h->false)==null,"per-cell constraints applied");ok(w.siege(u.id,c.id));
        World capture=fixture();World.Unit a=unit(capture,new Hex(25,10),World.Weapon.SPEAR);World.City target=capture.city(20);target.defense=1;target.troops=100;target.food=target.gold=0;Arrays.fill(target.equipment,0);
        ok(capture.marches.execute(capture.marches.previewCity(a.id,target.id)));check(target.owner==0&&capture.unit(a.id)==null&&capture.officer(1).cityId==20,"edge capture followed by exactly-once garrison");
        check(capture.cities.size()==3,"city still one entity after capture");
    }
    private static void engines(){
        World.Weapon[] weapons={World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT};
        for(World.Weapon weapon:weapons){World w=fixture();World.Unit u=unit(w,new Hex(25,10),weapon);World.City c=w.city(20);Army.Tactic tactic=w.army.tactics(u).get(0);Displacement.Preview p=w.army.tacticCityPreview(u.id,c.id,tactic);check(p.valid(),weapon+" edge preview: "+p.error);int hp=c.defense,stock=c.troops;ok(w.army.tacticCity(u.id,c.id,tactic));check(c.defense<hp||c.troops<stock,"engine applies city edge damage "+weapon);}
        World w=fixture();World.Unit u=unit(w,new Hex(25,10),World.Weapon.CATAPULT);World.Officer o=new World.Officer(11,"守将",1,-1,60,60,60,60,60);o.unitId=w.nextUnitId;w.officers.add(o);World.Unit b=new World.Unit(w.nextUnitId++,1,11,World.Weapon.SPEAR,new Hex(26,10),3000,5000);w.units.add(b);
        int defense=w.city(20).defense;ok(w.army.tacticCity(u.id,20,Army.Tactic.STONE));check(w.city(20).defense<defense,"explicit city target not replaced by field unit on same tile");
    }
    private static void lightning(){
        boolean success=false;for(int seed=0;seed<30&&!success;seed++){World w=fixture();World.Unit u=unit(w,new Hex(25,10),World.Weapon.SPEAR);w.officer(1).skillId=Skill.GUIMEN.id;w.strategy.setSeed(seed);World.City c=w.city(20);int hp=c.defense,stock=c.troops;ok(w.war.plot(u.id,c.hex,War.Plot.LIGHTNING));if(c.defense<hp){success=true;check(hp-c.defense==500&&stock-c.troops==800,"seven-cell lightning overlap damages city once");}}
        check(success,"deterministic sample includes a successful real lightning cast");World w=fixture();check(SiteFootprint.covered(w,SiteFootprint.cells(w.city(20))).size()==1,"AoE entity dedup query");
    }
    private static void deployment(){
        World w=fixture();World.City c=w.city(10);Hex out=w.army.deploymentExit(c,World.Weapon.SPEAR);check(out!=null&&SiteFootprint.distance(c,out)==1&&!SiteFootprint.contains(c,out),"sortie on connected exterior, not occupied city rim");
        World.Unit preview=w.army.deploymentPreview(c,1,new int[]{2,3},World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000,123);check(preview!=null&&preview.hex.equals(out),"sortie preview uses exact production exit");ok(w.army.deploy(10,1,new int[]{2,3},World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000,123));check(w.unit(w.officer(1).unitId).hex.equals(out),"real deployment matches preview");
        World sealed=fixture();for(Hex h:sealed.city(10).hex.neighbors())sealed.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;check(sealed.army.deploymentExit(sealed.city(10),World.Weapon.SPEAR)==null,"cannot jump across impassable city ring");
    }
    private static void ports(){
        World w=fixture();World.Unit u=unit(w,new Hex(7,10),World.Weapon.SPEAR);Hex land=new Hex(11,10),water=new Hex(12,10);w.terrain[water.q][water.r]=World.Terrain.WATER;check(w.army.moveCost(u,land,water)<0,"city edge is not a port");
        World.City p=new World.City(30,"渡口",new Hex(11,11),0);p.kind=World.SiteKind.PORT;w.cities.add(p);check(w.army.moveCost(u,land,water)>0,"owned adjacent dock permits edge transition");p.owner=1;check(w.army.moveCost(u,land,water)<0,"lost port instantly removes embark permission");
        World gate=fixture();World.Unit a=unit(gate,new Hex(15,10),World.Weapon.SPEAR);World.City g=new World.City(30,"关",new Hex(17,10),1);g.kind=World.SiteKind.GATE;gate.cities.add(g);
        check(gate.army.moveCost(a,new Hex(16,9),new Hex(17,9))==1,"gate does not invent adjacent collision");check(gate.army.moveCost(a,new Hex(16,10),g.hex)<0,"gate center genuinely blocked");
    }
    private static void convoys()throws Exception{
        World w=fixture();World.City to=new World.City(30,"友方收货城",new Hex(19,10),0);w.cities.add(to);
        ok(w.domestic.transport(10,30,1,new int[]{2},123,12000,1000,new int[4],false,false));
        Domestic.Mission m=w.domestic.missions.get(0);m.hex=new Hex(10,10);m.stopped=true;
        check(w.unitAt(m.hex)==m,"convoy on city remains selectable field unit");
        World copy=SaveCodec.decode(SaveCodec.encode(w));check(copy.unit(m.id)!=null,"convoy on city saves without disappearing");
        int troops=to.troops,gold=to.gold;for(int turn=0;turn<8&&w.unit(m.id)!=null;turn++){w.orders.reset(m);ok(w.marches.execute(w.marches.previewCity(m.id,to.id)));if(w.unit(m.id)!=null)reset(w);}
        check(w.unit(m.id)==null&&to.troops==troops+1000&&to.gold==gold+123,"convoy receipt exactly once after leaving source footprint");
        check(!w.domestic.unload(m,to.id).ok&&to.gold==gold+123,"cannot submit duplicate convoy receipt");
    }
    private static boolean landPath(World w,World.Unit probe,Hex start,Hex goal,int minX,int maxX,int minY,int maxY){
        Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> queue=new ArrayDeque<>();seen.add(start);queue.add(start);
        while(!queue.isEmpty()){Hex h=queue.remove();if(h.equals(goal))return true;for(Hex n:h.neighbors()){
            if(!w.inside(n))continue;SourceGridCoord source=MapCoordinates.source(w,n);
            if(source.x<minX||source.x>maxX||source.y<minY||source.y>maxY||w.army.water(n)||w.army.moveCost(probe,h,n)<1||w.domestic.at(n)!=null||w.war.at(n)!=null)continue;
            if(seen.add(n))queue.add(n);
        }}return false;
    }
    private static void geographicRoutes()throws Exception{
        World w=ScenarioCatalog.load("coalition-190",5,55L);World.Unit probe=new World.Unit(-1,5,-1,World.Weapon.SPEAR,w.city(20017).hex,1,1);
        check(landPath(w,probe,w.city(20017).hex,w.city(20015).hex,45,96,60,90),"real ChangAn-Tongguan-Hangu-Luoyang road connected through owned gates");
        for(int id:new int[]{20044,20045,20043}){World.City gate=w.city(id);SourceGridCoord source=MapCoordinates.source(w,gate.hex);Hex left=MapCoordinates.axial(w,new SourceGridCoord(source.x-2,source.y)),right=MapCoordinates.axial(w,new SourceGridCoord(source.x+2,source.y));int owner=gate.owner;
            gate.owner=probe.owner;check(landPath(w,probe,left,right,source.x-4,source.x+4,source.y-5,source.y+5),"owned pass opens a physical road "+gate.name);
            gate.owner=0;check(!landPath(w,probe,left,right,source.x-4,source.x+4,source.y-5,source.y+5),"no local around-gate land bypass "+gate.name);gate.owner=owner;
        }
        World.City meng=w.city(20063);check(MapCoordinates.source(w,meng.hex).equals(new SourceGridCoord(79,69)),"Mengjin corrected to observed bank");check(meng.hex.neighbors().stream().anyMatch(w.army::water),"Mengjin has actual water frontage");
        int id=1;for(World.City c:w.cities)for(Hex h:w.development.parcels(c.id))w.domestic.facilities.add(new Domestic.Facility(id++,c.id,Domestic.Kind.FARM,h,-1,0));
        for(World.City c:w.cities)check(w.army.deploymentExit(c,World.Weapon.SPEAR)!=null,"all development built leaves a connected sortie "+c.name);
        check(landPath(w,probe,w.city(20017).hex,w.city(20015).hex,45,96,60,90),"fully built economic plots preserve GuanLuo connection");
        System.out.println("GUANLUO55 PASS: connected regional roads, single-tile gate seals, Mengjin bank and all 591 plots built");
    }
    private static void ai(){
        World w=fixture();World.Unit u=unit(w,new Hex(25,10),World.Weapon.SPEAR);int hp=w.city(20).defense;CampaignAi.Action action=new CampaignAi(w).bestAction(u.id,true);check(action!=null,"AI sees edge attack despite center out of melee range");ok(new CampaignAi(w).execute(action));check(w.city(20).defense<hp,"AI executes same formal siege");
        World retreat=fixture();World.Unit a=unit(retreat,new Hex(11,10),World.Weapon.SPEAR);a.food=0;new CampaignAi(retreat).runUnit(a,false,c->false,c->c.id==10);check(retreat.unit(a.id)==null,"AI can retreat from city rim instead of circling center");
    }
    private static void maps()throws Exception{
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries()){
            long time=System.nanoTime();World w=ScenarioCatalog.load(s.id,0,55L);Set<Hex> sites=new HashSet<>();int cities=0;
            for(World.City c:w.cities){if(c.kind==World.SiteKind.CITY)cities++;for(Hex h:SiteFootprint.cells(c)){check(w.inside(h)&&sites.add(h),"inside and disjoint "+s.id+"/"+c.name);check(w.cityAt(h)==c,"indexed city identity");}check(w.army.deploymentExit(c,World.Weapon.SPEAR)!=null,"connected sortie from "+s.id+"/"+c.name);}
            for(World.City c:w.cities)for(Hex h:w.development.parcels(c.id))check(!sites.contains(h),"development distinct from body");
            World reload=SaveCodec.decode(SaveCodec.encode(w));check(reload.cities.size()==w.cities.size(),"all production scenarios roundtrip");
            System.out.println("MAP55 "+s.id+" sites="+w.cities.size()+" cities="+cities+" load+save_ms="+(System.nanoTime()-time)/1000000);
        }
        World national=ScenarioCatalog.load("heroes-250",0,55L);for(int t=0;t<3;t++){long time=System.nanoTime();ok(national.nextTurn());System.out.println("TURN55 heroes-250 turn="+national.turn+" ms="+(System.nanoTime()-time)/1000000);national=SaveCodec.decode(SaveCodec.encode(national));}
    }
}
