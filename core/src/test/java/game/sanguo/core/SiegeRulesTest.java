package game.sanguo.core;

import java.lang.reflect.Method;
import java.util.*;

/** Exercises actual production economy, recruiting, combat, reports and global settlement. */
public final class SiegeRulesTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static World fixture(){return Ux64Fixture.create();}
    private static World.Unit enemy(World w,World.City c,int distance,int troops){
        int id=100+w.units.size();World.Officer o=new World.Officer(id,"围城将"+id,1,20,80,80,80,80,80);
        Arrays.fill(o.aptitude,2);w.officers.add(o);
        Hex h=new Hex(c.hex.q+distance+(c.kind==World.SiteKind.CITY?1:0),c.hex.r);
        World.Unit u=new World.Unit(w.nextUnitId++,1,o.id,World.Weapon.SPEAR,h,troops,30000);
        o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    private static void settle(World w)throws Exception{
        Method m=World.class.getDeclaredMethod("settleGlobalTurn",java.util.function.Consumer.class);m.setAccessible(true);
        m.invoke(w,(java.util.function.Consumer<String>)s->{});w.reports.endTurn();
    }
    private static void geometry()throws Exception{
        World w=fixture();byte[] saved=SaveCodec.encode(w);
        for(World.City c:w.cities){
            List<Hex> cells=SiegeRules.cells(w,c);check(cells.size()==(c.kind==World.SiteKind.CITY?30:18),"two complete outer rings "+c.name);
            check(new HashSet<>(cells).size()==cells.size(),"no duplicate tiles");
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                Hex h=new Hex(q,r);int d=SiteFootprint.distance(c,h);
                check(cells.contains(h)==(d>=1&&d<=2),"overlay exact exterior geometry");
                check(SiegeRules.inArea(w,c,h)==(d<=2),"pressure uses footprint distance");
            }
        }
        check(Arrays.equals(saved,SaveCodec.encode(w)),"range preview pure incl RNG/save");
        World small=new World(8,8);World.City edge=new World.City(1,"边关",new Hex(0,0),0);edge.kind=World.SiteKind.GATE;small.cities.add(edge);
        check(SiegeRules.cells(small,edge).size()==5,"corner clipping");small.terrain[1][0]=World.Terrain.VOID;
        check(SiegeRules.cells(small,edge).size()==4&&!SiegeRules.inArea(small,edge,new Hex(1,0)),"VOID excluded");
        check(SiegeRules.cells(w,null).isEmpty()&&!SiegeRules.blockaded(w,null),"empty selection safe");
    }
    private static void diplomacy()throws Exception{
        for(World.SiteKind kind:World.SiteKind.values()){
            World w=fixture();World.City c=w.city(10);c.kind=kind;w.invalidateSiteIndex();
            World.Unit u=enemy(w,c,2,6000);
            check(w.cityDefense.besieged(c)&&w.cityDefense.inRange(c,u)&&w.cityDefense.range(c)==2,"all site kinds share second ring "+kind);
            u.hex=new Hex(u.hex.q+1,u.hex.r);check(!SiegeRules.blockaded(w,c)&&!w.cityDefense.inRange(c,u),"no phantom third ring");
            u.hex=new Hex(u.hex.q-1,u.hex.r);u.owner=0;check(!SiegeRules.blockaded(w,c),"friendly no siege");u.owner=1;
            for(Campaign.TreatyKind t:Campaign.TreatyKind.values()){
                w.campaign.concludeTreaty(0,1,t,12);check(!SiegeRules.blockaded(w,c)&&!w.cityDefense.inRange(c,u),"treaty respected "+t);w.campaign.treaties.clear();
            }
            u.troops=0;check(!SiegeRules.blockaded(w,c),"dead unit ignored");u.troops=6000;
            c.owner=-1;check(!SiegeRules.blockaded(w,c)&&!w.cityDefense.inRange(c,u),"neutral has no fake defense");c.owner=0;
            u.hex=c.hex;check(SiegeRules.blockaded(w,c)&&w.cityDefense.inRange(c,u),"ownership-change footprint is not sanctuary");
        }
        World w=fixture();World.City c=w.city(10);Domestic.Mission convoy=new Domestic.Mission(1,1,20,20,10,new Hex(6,3),true,100,10000,6000,new int[4]);
        w.domestic.missions.add(convoy);check(!SiegeRules.blockaded(w,c)&&w.cityDefense.inRange(c,convoy),"transport targetable but cannot blockade");
        int before=convoy.troops;w.cityDefense.tick();check(convoy.troops<before,"actual convoy object receives damage");
    }
    private static void economy()throws Exception{
        World w=fixture();World.City c=w.city(10);w.startMonth=1;World.Unit u=enemy(w,c,2,6000);
        byte[] saved=SaveCodec.encode(w);int gold=c.gold,food=c.food;
        for(int turn=1;turn<=18;turn++){
            int normalGold=w.domestic.goldIncome(c.id,turn,false),normalFood=w.domestic.foodIncome(c.id,turn,false);
            check(w.domestic.goldIncome(c.id,turn)==normalGold*75/100,"gold schedule unchanged; discount once");
            check(w.domestic.foodIncome(c.id,turn)==normalFood*75/100,"food season unchanged; discount once");
        }
        for(Skill skill:new Skill[]{Skill.FUHAO,Skill.ZHENGSHUI,Skill.MIDAO,Skill.ZHENGSHOU}){
            w.officer(5).skillId=skill.id;
            check(w.domestic.goldIncome(c.id,9)==w.domestic.goldIncome(c.id,9,false)*75/100,"gold skill then siege "+skill);
            check(w.domestic.foodIncome(c.id,9)==w.domestic.foodIncome(c.id,9,false)*75/100,"food skill then siege "+skill);
        }
        w.officer(5).skillId="none";
        check(gold==c.gold&&food==c.food,"preview never drains stored money or food");
        int base=w.domestic.goldIncome(c.id,3);enemy(w,c,1,6000);check(w.domestic.goldIncome(c.id,3)==base,"economic penalty does not stack per unit");
        World.City other=w.city(11);check(w.domestic.goldIncome(other.id,3)==w.domestic.goldIncome(other.id,3,false),"unrelated same-owner city unaffected");
        World decoded=SaveCodec.decode(saved);check(SiegeRules.blockaded(decoded,decoded.city(10)),"save derives siege immediately");
        check(Arrays.equals(saved,SaveCodec.encode(decoded)),"loading no damage, income or extra turn");
        u.hex=new Hex(15,10);w.units.remove(1);check(!SiegeRules.blockaded(w,c),"withdrawal immediate");
    }
    private static void recruitment()throws Exception{
        World w=fixture();World.City c=w.city(10);FacilityProductionTest.facility(w,10,Domestic.Kind.BARRACKS);
        int full=w.strategy.recruitAmount(10,5);World.Unit u=enemy(w,c,2,6000);
        int reduced=w.strategy.recruitAmount(10,5);check(reduced==full*75/100,"actual recruit preview reduced");
        c.recruitReserve=1;check(w.strategy.recruitAmount(10,5)==1,"last recruit not lost by rounding reserve");c.recruitReserve=20000;
        int troops=c.troops,pool=c.recruitReserve;World.Result result=w.recruit(10,5);check(result.ok,result.message);
        check(c.troops==troops+reduced&&c.recruitReserve==pool-reduced,"paid recruit matches preview");
        check(c.morale<90,"existing new-recruit training preserved");
        c.recruitReserve=0;check(Conscription.quarterlyRecovery(w,c)==3750,"quarter growth reduced");
        FacilityProductionTest.facility(w,10,Domestic.Kind.FARM);
        check(Conscription.quarterlyRecovery(w,c)==4125,"farm bonus included before siege");
        c.recruitReserve=19900;check(Conscription.recovery(w,c)==100,"apply penalty before free-space clamp");
        check(Conscription.recovery(w,w.city(13))==0,"ports retain no independent recruitment");
        w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.CEASEFIRE,12);
        check(Conscription.quarterlyRecovery(w,c)==5500,"ceasefire resumes quarter forecast");
    }
    private static void attritionAndRepair()throws Exception{
        World w=fixture();World.City c=w.city(10);c.defense=800;int repair=w.cityDefense.repairAmount(c,w.officer(5));
        World.Unit u=enemy(w,c,2,6000);check(w.cityDefense.recovery(c)==0&&w.cityDefense.repairAmount(c,w.officer(5))==repair/4,"repair blockade unified");
        check(SiegeRules.attrition(c,SiegeRules.state(w,c))==60,"attrition bounded by besieger strength");
        u.troops=1;check(SiegeRules.attrition(c,SiegeRules.state(w,c))==0,"one-man siege cannot bleed defenders");
        u.troops=50000;c.troops=50000;check(SiegeRules.attrition(c,SiegeRules.state(w,c))==200,"attrition absolute ceiling");
        c.troops=1;check(SiegeRules.attrition(c,SiegeRules.state(w,c))==0,"last defender never auto-captured");
        c.troops=10000;Map<Integer,SiegeRules.State> snapshot=SiegeRules.snapshot(w);int morale=c.morale;
        SiegeRules.settleAttrition(w,snapshot);check(c.troops==9900&&c.defense==800&&c.morale==morale,"only bounded siege deserters, no free wall damage or morale drain");
        c.owner=1;check(!SiegeRules.blocked(snapshot,c),"snapshot invalid for changed owner");
    }
    private static void volley()throws Exception{
        for(World.SiteKind kind:World.SiteKind.values()){
            World w=fixture();World.City c=w.city(10);c.kind=kind;w.invalidateSiteIndex();World.Unit u=enemy(w,c,2,6000);
            int expected=w.cityDefense.counterDamage(c,u),before=u.troops;TurnJournal journal=new TurnJournal(w);
            w.cityDefense.tick();journal.close();
            check(before-u.troops==expected,"auto and passive use exact same damage "+kind);
            check(u.wounded==expected*30/100,"actual wound pipeline retained");
            check(journal.events().stream().anyMatch(e->e.kind==TurnJournal.Kind.FACILITY_ATTACK&&e.owner==c.owner&&c.hex.equals(e.start)),"defense animation with real city source");
            check(w.reports.query(-1,1,BattleReports.Scope.RECEIVED,BattleReports.Kind.COMBAT,"自动射击").stream().anyMatch(e->e.actor==0),"enemy-related reports attribute city owner");
            c.food=0;before=u.troops;w.cityDefense.tick();check(before==u.troops,"no food means no volley");c.food=10000;c.troops=0;
            w.cityDefense.tick();check(before==u.troops,"no garrison means no volley");c.troops=10000;c.defense=0;
            w.cityDefense.tick();check(before==u.troops,"destroyed walls no volley");
        }
        World w=fixture();World.City c=w.city(10);List<Hex> cells=SiegeRules.cells(w,c);
        int total=0,peak=0;for(int i=0;i<12;i++){World.Unit u=enemy(w,c,1,6000);u.hex=cells.get(i);total+=u.troops;peak=Math.max(peak,w.cityDefense.counterDamage(c,u));}
        w.cityDefense.tick();int remaining=0;for(World.Unit u:w.units)remaining+=u.troops;
        check(total-remaining<=peak*2&&total>remaining,"multi-target volley has bounded total damage");
        World tiny=fixture();World.City site=tiny.city(10);World.Unit last=enemy(tiny,site,2,1);tiny.cityDefense.tick();check(tiny.unit(last.id)==null,"last enemy defeated by shared casualty path");
    }
    private static void globalTurn()throws Exception{
        World w=fixture();w.startMonth=3;w.turn=2;World.City c=w.city(10);c.recruitReserve=0;c.gold=0;c.defense=800;
        enemy(w,c,2,1);int expectedGold=w.domestic.goldIncome(c.id,3),expectedFood=w.domestic.foodIncome(c.id,3),beforeFood=c.food;
        settle(w);check(w.turn==3&&w.units.isEmpty(),"complete global volley destroys last besieger");
        check(c.gold==expectedGold,"settlement keeps start-of-volley money penalty");
        check(c.food==beforeFood-w.cityFoodUse(c)+expectedFood,"same snapshot for seasonal food");
        check(c.recruitReserve==3750&&c.defense==800,"same snapshot for quarterly growth and repair");
        byte[] saved=SaveCodec.encode(w);World copy=SaveCodec.decode(saved);check(Arrays.equals(saved,SaveCodec.encode(copy)),"reload after quarter/kill no duplicated settlement");
        settle(copy);check(copy.city(10).recruitReserve==3750&&copy.city(10).defense==820,"next turn peaceful, no repeated quarter recovery");
        check(w.reports.query(-1,0,BattleReports.Scope.RELATED,null,"已减25%").size()>0,"settlement explains actual discounted income");
    }
    public static void main(String[] args)throws Exception{
        geometry();diplomacy();economy();recruitment();attritionAndRepair();volley();globalTurn();
        System.out.println("PASS: "+checks+" siege geometry/economy/recruitment/defense/settlement contracts");
    }
}
