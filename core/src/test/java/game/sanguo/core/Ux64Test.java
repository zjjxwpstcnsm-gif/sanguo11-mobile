package game.sanguo.core;

import java.util.*;

/** Real production commands, calendar settlement, save/reload and simulation/overlay parity. */
public final class Ux64Test {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void ok(World.Result result){check(result.ok,result.message);}
    private static World fixture(){return Ux64Fixture.create();}
    private static void districts()throws Exception{
        World w=fixture();
        byte[] before=SaveCodec.encode(w);
        check(!w.districts.configure(-1,"拒绝港",new int[]{13},Districts.Policy.ECONOMY,-1,-1,false,true).ok,"cannot select a port");
        check(!w.districts.configure(-1,"拒绝关",new int[]{14},Districts.Policy.ECONOMY,-1,-1,false,true).ok,"cannot select a gate");
        check(Arrays.equals(before,SaveCodec.encode(w)),"invalid delegation atomic");
        ok(w.districts.configure(-1,"后方军团",new int[]{11},Districts.Policy.ECONOMY,-1,-1,false,true));
        Districts.District d=w.districts.all().get(0);
        check(d.cities().equals(Set.of(11)),"only CITY anchors saved");
        check(w.districts.sites(d).equals(Set.of(11,13,14)),"port and gate inherited");
        check(!w.districts.directCity(13)&&!w.districts.directCity(14),"port and gate command guards inherited");
        check(w.districts.parentCity(13).id==11,"fixed parent");
        w.city(13).owner=1;check(w.districts.city(13)==null,"enemy port never controlled");
        check(w.districts.parentCity(13).id==11,"ownership does not remap parent");w.city(13).owner=0;
        byte[] saved=SaveCodec.encode(w);World copy=SaveCodec.decode(saved);
        check(Arrays.equals(saved,SaveCodec.encode(copy)),"new district save byte roundtrip");
        check(copy.districts.city(13)==copy.districts.city(11),"inheritance survives reload");
        for(World.Officer o:w.officers)o.acted=true;w.officer(2).acted=false;w.city(13).order=20;d.points=60;
        w.districts.run();check(w.city(13).order>20&&w.officer(2).acted,"delegation actually operates the port");
        check(d.points<60,"appendage spends district budget");
        w.actionPoints[0]=60;ok(w.districts.dissolve(d.id));
        check(w.districts.directCity(13)&&w.districts.directCity(14),"dissolve releases appendages");
        w.officer(0).cityId=13;
        check(w.districts.configureError(-1,"君主港",new int[]{11},Districts.Policy.ECONOMY,-1,-1)!=null,"ruler port protects its parent");
        World legacy=fixture();ok(legacy.districts.configure(-1,"旧军团",new int[]{11},Districts.Policy.ECONOMY,-1,-1,false,true));
        Districts.District old=legacy.districts.all().get(0);old.cities.add(13);old.cities.add(14);
        legacy.districts.migrateLegacySites();check(old.cities.equals(Set.of(11)),"legacy duplicated port/gate membership removed");
        check(legacy.districts.city(13)==old,"legacy port follows retained parent");
        SaveCodec.validate(legacy);
        World onlyPort=fixture();Districts.District legacyPort=new Districts.District(1,0,"旧港军团");legacyPort.policy=Districts.Policy.ECONOMY;legacyPort.cities.add(13);onlyPort.districts.groups.put(1,legacyPort);onlyPort.districts.nextId=2;
        onlyPort.districts.migrateLegacySites();check(onlyPort.districts.all().isEmpty()&&onlyPort.districts.directCity(11),"port-only legacy does not hijack direct parent");
        SaveCodec.validate(onlyPort);
    }
    private static void nationalParents()throws Exception{
        for(String id:new String[]{"huangjin-184","coalition-190","warlords-194","guandu-200","chibi-207","heroes-250"}){
            World w=TestScenarios.load(id,0);
            for(World.City site:w.cities)if(site.kind!=World.SiteKind.CITY){World.City parent=w.districts.parentCity(site.id);
                check(parent!=null&&parent.kind==World.SiteKind.CITY,"every national port/gate has a CITY parent "+site.name);
                check(parent.owner==site.owner,"fixed parent matches authored starting ownership "+id+" "+site.name);
            }
            check(w.districts.parentCity(20079).id==20029,"中庐 follows 襄阳");
            check(w.districts.parentCity(20080).id==20030,"乌林 follows 江陵");
            check(w.districts.parentCity(20074).id==20026,"陆口 follows 柴桑");
            check(w.districts.parentCity(20070).id==20023,"曲阿 follows 吴");
            check(w.districts.parentCity(20060).id==20010,"濡须 follows 寿春 in authored scenarios");
            check(w.districts.parentCity(20061).id==20006,"顿丘 follows 邺");
        }
    }
    private static void recruiting()throws Exception{
        World w=fixture();World.City c=w.city(10);FacilityProductionTest.facility(w,10,Domestic.Kind.BARRACKS);
        int amount=w.strategy.recruitAmount(10,5),morale=c.morale, troops=c.troops, pool=c.recruitReserve;
        int expected=(int)((long)morale*troops/(troops+amount));
        ok(w.recruit(10,5));check(c.morale==expected&&c.morale<morale,"new troops dilute morale by number");
        check(c.troops==troops+amount&&c.recruitReserve==pool-amount,"actual recruit stock accounting");
        byte[] before=SaveCodec.encode(w);check(!w.recruit(10,0).ok,"quota exhaustion rejected");
        check(Arrays.equals(before,SaveCodec.encode(w)),"failed recruit no morale/resources change");
        ok(w.strategy.trainArmy(10,0));check(c.morale>expected,"training restores garrison energy");
        byte[] saved=SaveCodec.encode(w);World restored=SaveCodec.decode(saved);
        check(restored.city(10).morale==c.morale&&restored.city(10).recruitReserve==c.recruitReserve,"recruiting state persisted");
        c.troops=0;c.morale=100;check(Conscription.moraleAfter(c,1000)==0,"empty garrison recruits have no training");
        c.troops=10000;c.morale=0;check(Conscription.moraleAfter(c,1000)==0,"zero morale never negative");
        c.troops=Integer.MAX_VALUE;c.morale=120;check(Conscription.moraleAfter(c,Integer.MAX_VALUE)==60,"weighted morale cannot overflow");
        c.recruitReserve=19750;check(Conscription.recovery(c)==250,"recovery capped to free reserve space");
        c.recruitReserve=20000;check(Conscription.recovery(c)==0,"full pool does not accumulate");
        c.recruitReserve=0;check(Conscription.recovery(c)==5000,"empty pool finite recovery");
        w.city(13).recruitReserve=0;check(Conscription.recovery(w.city(13))==0,"ports do not generate manpower");
    }
    private static void calendar()throws Exception{
        World dates=fixture();for(int month=1;month<=12;month++){dates.startMonth=month;
            for(int turn=0;turn<73;turn++){int absoluteMonth=month-1+turn/3;
                check(Conscription.quarterBegins(dates,turn)==(turn>0&&turn%3==0&&absoluteMonth%3==0),"quarter alignment");
                dates.turn=turn;int next=Conscription.nextRecoveryIn(dates);check(next>=1&&next<=9&&Conscription.quarterBegins(dates,turn+next),"next recovery forecast");
            }
        }
        for(int startingMonth:new int[]{3,6,9,12}){
            World w=fixture();w.startMonth=startingMonth;w.turn=2;
            for(World.City c:w.cities){c.gold=0;c.recruitReserve=0;}w.city(12).recruitReserve=19750;
            ok(w.nextTurn());check(w.city(10).recruitReserve==5000&&w.city(20).recruitReserve==5000,"one global quarterly grant for player and AI");
            check(w.city(12).recruitReserve==20000,"quarter respects cap");check(w.city(13).recruitReserve==0,"quarter ignores ports");
            byte[] saved=SaveCodec.encode(w);World copy=SaveCodec.decode(saved);
            check(Arrays.equals(saved,SaveCodec.encode(copy)),"loading on quarter boundary gives no extra grant");
            ok(copy.nextTurn());check(copy.city(10).recruitReserve==5000,"next mid-month turn no second grant");
            check(w.date().contains("上旬")&&w.date().contains((startingMonth==12?1:startingMonth+1)+"月"),"display calendar matches recovery date");
        }
    }
    private static War.Structure structure(World w,War.StructureKind kind){
        War.Structure s=new War.Structure(w.war.nextStructureId++,0,kind,new Hex(18,10),kind.hp);w.war.structures.add(s);return s;
    }
    private static World.Unit unit(World w,int officer,int q,int r){World.Officer o=w.officer(officer);w.strategy.releaseGovernor(o.id);o.cityId=-1;
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,o.id,World.Weapon.SPEAR,new Hex(q,r),6000,12000);o.unitId=u.id;w.units.add(u);return u;
    }
    private static void coverage()throws Exception{
        for(War.StructureKind kind:War.StructureKind.values()){
            World w=fixture();War.Structure s=structure(w,kind);int radius=Fieldworks.range(kind);
            Set<Hex> area=w.fieldworks.coverage(s);check(area.stream().allMatch(w::inside),"coverage inside playable map "+kind);
            if(radius>0)for(int q=12;q<=24;q++)for(int r=4;r<=16;r++){Hex h=new Hex(q,r);
                check(area.contains(h)==Fieldworks.inRange(s,h),"overlay matches radial effect "+kind+" "+h);
            }
            if(kind==War.StructureKind.CATAPULT_TOWER)check(!area.contains(new Hex(19,10))&&area.contains(new Hex(20,10)),"catapult adjacent blind spot");
            if(kind==War.StructureKind.EARTH_WALL||kind==War.StructureKind.STONE_WALL)check(area.isEmpty(),"wall has no fake coverage");
            s.complete=false;check(w.fieldworks.coverageDescription(s).contains("建成后")||area.isEmpty(),"unfinished coverage labelled planned");
            check(w.fieldworks.coverage(s).equals(area),"preview does not mutate pattern");
            w.war.structures.remove(s);check(w.fieldworks.coverage(s).isEmpty(),"destroyed selection clears coverage");
        }
        World w=fixture();War.Structure camp=structure(w,War.StructureKind.CAMP);World.Unit ally=unit(w,5,20,10);
        check(w.fieldworks.coverage(camp).contains(ally.hex)&&w.fieldworks.defensePercent(ally)==15,"camp overlay and actual defense agree");
        camp.complete=false;check(w.fieldworks.defensePercent(ally)==0,"planned range gives no actual aura");camp.complete=true;
        camp.kind=War.StructureKind.FORTRESS;ally.hex=new Hex(22,10);check(w.fieldworks.coverage(camp).contains(ally.hex)&&w.fieldworks.defensePercent(ally)==35,"upgraded fortress radius4");
        camp.kind=War.StructureKind.MUSIC;ally.hex=new Hex(20,10);check(w.energy.recovery(ally)==10,"music actual energy agrees");
        ally.hex=new Hex(21,10);check(w.energy.recovery(ally)==0&&!w.fieldworks.coverage(camp).contains(ally.hex),"music outside radius2");
        camp.kind=War.StructureKind.CATAPULT_TOWER;World.Unit enemy=unit(w,20,19,10);int before=enemy.troops;
        w.fieldworks.towers();check(enemy.troops==before,"catapult blind spot simulation");enemy.hex=new Hex(20,10);w.fieldworks.towers();check(enemy.troops<before,"catapult real hit inside range");
        World ball=fixture();War.Structure fire=structure(ball,War.StructureKind.FIRE_BALL);fire.direction=0;
        Hex delta=new Hex(0,0).neighbors().get(0);Hex wall=new Hex(fire.hex.q+delta.q*2,fire.hex.r+delta.r*2);ball.terrain[wall.q][wall.r]=World.Terrain.MOUNTAIN;
        check(wall.distance(fire.hex)==2&&!ball.fieldworks.coverage(fire).contains(wall)&&ball.fieldworks.coverage(fire).size()==2,"directional fireball stops before mountain");
        World dam=fixture();War.Structure d=structure(dam,War.StructureKind.DAM);World.Unit victim=unit(dam,20,19,10);
        check(dam.fieldworks.coverage(d).contains(victim.hex),"flood overlay follows connected lowlands");int troops=victim.troops;dam.fieldworks.destroy(d);check(victim.troops==troops-600,"same flood footprint used in damage");
    }
    public static void main(String[] args)throws Exception{
        districts();nationalParents();recruiting();calendar();coverage();
        System.out.println("PASS: "+checks+" v064 assertions: city-only delegation/inheritance/operations/saves, recruiting morale, bounded calendar recovery and real facility coverage parity");
    }
}
