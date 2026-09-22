package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** v66 integration checks against the production commands, not independent formula replicas. */
public final class Governance66Test {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World fixture(){World w=Ux64Fixture.create();
        for(World.City c:w.cities){c.gold=5000;c.food=30000;Arrays.fill(c.equipment,0);}
        for(World.Officer o:w.officers)if(o.role!=Strategy.Role.RULER)o.loyalty=80;
        return w;
    }
    private static World.Officer officer(World w,int id,int city,int politics){World.Officer o=new World.Officer(id,"新将"+id,0,city,80,80,80,politics,80);w.officers.add(o);return o;}
    private static World.Unit unit(World w,int commander,Hex at,int troops){World.Officer o=w.officer(commander);w.strategy.releaseGovernor(o.id);
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,commander,World.Weapon.SPEAR,at,troops,10000);o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    private static void roundtrip(World w)throws Exception{byte[] b=SaveCodec.encode(w);check(Arrays.equals(b,SaveCodec.encode(SaveCodec.decode(b))),"canonical v32 roundtrip");}
    private static void wounds()throws Exception{
        World w=fixture();World.Unit u=unit(w,5,new Hex(12,12),5000);int initial=u.troops;
        for(int i=0;i<10;i++)w.combatEffects.hit(null,u,1,false,false);
        check(u.wounded==3&&u.troops==initial-10&&u.woundRemainder==0,"fractional hits aggregate exactly 30 percent");
        w.combatEffects.hit(null,u,123,false,false);check(u.wounded==39&&u.woundRemainder==90,"actual damage not requested overkill is counted");
        World copy=SaveCodec.decode(SaveCodec.encode(w));World.Unit resumed=copy.unit(u.id);w.combatEffects.hit(null,u,7,false,false);copy.combatEffects.hit(null,resumed,7,false,false);
        check(u.wounded==42&&resumed.wounded==42&&u.woundRemainder==0,"fraction persists through save");
        check(Logistics.foodUse(w,u)==Logistics.baseUse(u.troops+u.wounded,false),"wounded consume rations but do not add combat troops");
        int wounds=u.wounded;u.food=0;for(World.City c:w.cities)if(c.owner==1){c.gold=0;c.troops=0;c.food=0;}
        ok(w.nextTurn());check(w.unit(u.id)!=null&&u.wounded==wounds&&u.troops<4860,"starvation desertion creates no wounded");
        World.Officer dead=w.officer(5);World.Officer deputy=officer(w,31,-1,90);deputy.unitId=u.id;u.deputies=new int[]{31};
        int troops=u.troops,remaining=u.wounded;w.life.die(dead.id,"战死");World.Unit next=w.unit(u.id);
        check(next.officerId==31&&next.troops==troops&&next.wounded==remaining,"commander replacement preserves wounded");roundtrip(w);
        w.combatEffects.hit(null,next,Integer.MAX_VALUE,false,false);check(w.unit(next.id)==null,"wiped unit scatters instead of keeping zero-combat zombie army");
        // A real enemy attack and counterattack use the same casualty path.
        World fight=fixture();World.Unit a=unit(fight,5,new Hex(12,12),5000),b=unit(fight,20,new Hex(13,12),5000);
        int before=b.troops;ok(fight.attack(a.id,b.id));check(b.wounded==(before-b.troops)*30/100,"actual attack creates wounded");roundtrip(fight);
    }
    private static void arrivals()throws Exception{
        for(World.SiteKind kind:World.SiteKind.values()){
            World shape=fixture();World.City reference=shape.city(kind==World.SiteKind.CITY?10:kind==World.SiteKind.PORT?13:14);
            for(Hex target:SiteFootprint.cells(reference)){
                World w=fixture();World.City c=w.city(reference.id);Hex from=null;
                for(Hex h:target.neighbors())if(w.inside(h)&&w.cityAt(h)==null){from=h;break;}
                if(from==null)from=target.neighbors().get(0);World.Unit u=unit(w,5,from,3000);u.wounded=123;u.gold=77;
                int troops=c.troops,gold=c.gold,food=c.food;World.Result moved=w.move(u.id,target);check(moved.ok,kind+" "+from+" -> "+target+" "+moved.message);
                check(w.unit(u.id)==null&&w.officer(5).cityId==c.id,"move endpoint auto-enters "+kind+" "+target);
                check(c.troops==troops+3123&&c.gold==gold+77&&c.food==food+10000&&c.equipment[0]==3000,"soldiers heal once; wounded never create weapons");
                check(!w.enter(u.id,c.id).ok&&c.troops==troops+3123,"cannot double-enter/recover");roundtrip(w);
            }
        }
        World w=fixture();World.City c=w.city(10);c.troops=w.campaign.troopCap(c)-1000;World.Unit u=unit(w,5,new Hex(5,3),1500);u.wounded=500;
        ok(w.move(u.id,new Hex(4,3)));check(w.unit(u.id)==u&&u.wounded==500&&c.troops==99000,"full city never deletes soldiers or wounded");
        c.troops=10000;ok(w.marches.execute(w.marches.previewCity(u.id,c.id)));check(w.unit(u.id)==null&&c.troops==12000,"retry after capacity freed docks exactly once");roundtrip(w);
        // Preserve through-routes including an overnight stop on a city rim.
        World transit=CityFootprint55Test.fixture();for(int q=0;q<transit.width;q++)for(int r=0;r<transit.height;r++)if(r!=10)transit.terrain[q][r]=World.Terrain.MOUNTAIN;
        // Restore complete seven-cell footprints for save validation.
        for(World.City city:transit.cities)for(Hex h:SiteFootprint.cells(city))transit.terrain[h.q][h.r]=World.Terrain.PLAIN;
        World.Unit t=CityFootprint55Test.unit(transit,new Hex(7,10),World.Weapon.SPEAR);t.movementBudget=2;
        ok(transit.marches.execute(transit.marches.previewMove(t.id,new Hex(14,10))));check(transit.unit(t.id)==t&&t.march!=null,"intermediate city rest doesn't eat a route beyond it");
    }
    private static void capture()throws Exception{
        for(World.SiteKind kind:World.SiteKind.values()){
            World w=fixture();World.City c=w.city(20);c.kind=kind;w.invalidateSiteIndex();c.defense=1;c.troops=100;c.gold=0;c.food=0;Arrays.fill(c.equipment,0);
            // Keep a second enemy city so victory cleanup doesn't obscure admission.
            World.City reserve=new World.City(21,"敌后方",new Hex(30,22),1);w.cities.add(reserve);w.officer(20).cityId=21;
            Hex from=kind==World.SiteKind.CITY?new Hex(30,3):new Hex(31,3);World.Unit u=unit(w,5,from,4000);u.wounded=100;
            ok(w.siege(u.id,c.id));check(c.owner==0&&w.unit(u.id)==null&&c.troops==4100&&w.officer(5).cityId==c.id,"adjacent manual capture enters "+kind);roundtrip(w);
        }
        World w=CityFootprint55Test.fixture();World.City c=w.city(20);World.Unit u=CityFootprint55Test.unit(w,new Hex(23,10),World.Weapon.CATAPULT);c.defense=1;c.troops=100;c.gold=0;c.food=0;
        // The direct paid siege resolver is shared by engine tactics; distance prevents teleporting artillery.
        Hex hit=SiteFootprint.hit(c,u.hex,1,5,null,h->true);ok(w.resolveSiege(u,c,true,false,hit));check(c.owner==0&&w.unit(u.id)==u,"non-adjacent artillery does not teleport into captured city");
    }
    private static void governors()throws Exception{
        World w=fixture();World.Officer best=officer(w,30,10,99),backup=w.officer(5);backup.politics=90;
        w.city(10).governorId=-1;w.governance.reconcile(false);check(w.city(10).governorId==30,"empty governorship chooses highest politics");
        best.otherTask="外交出使";best.otherTaskTurns=2;w.governance.reconcile(true);check(w.city(10).governorId==5&&best.role==Strategy.Role.OFFICER,"envoy replaced even with retained home city id");
        best.otherTask="";best.otherTaskTurns=0;w.governance.reconcile(true);check(w.city(10).governorId==5,"returning incumbent doesn't displace valid replacement");
        backup.otherTask="出使登用：贤士";backup.otherTaskTurns=2;w.governance.reconcile(true);check(w.city(10).governorId==30,"recruiting trip vacates office");
        best.otherTask="技巧研究";best.otherTaskTurns=3;w.governance.reconcile(true);check(w.city(10).governorId==30,"local research remains physically resident");
        check(w.city(13).governorId==2&&w.city(14).governorId==3,"port and gate auto appoint residents");
        best.otherTask="";best.otherTaskTurns=0;unit(w,30,new Hex(12,12),3000);w.governance.reconcile(true);check(w.city(10).governorId==0,"field commander is absent; busy external backup skipped");
        roundtrip(w);
        World transfer=fixture();World.Officer higher=officer(transfer,30,11,99);transfer.city(11).governorId=-1;transfer.governance.reconcile(false);ok(transfer.domestic.transfer(11,12,30));
        check(transfer.city(11).governorId==1,"real transfer command reassigns governor before next turn");
    }
    private static void rewards()throws Exception{
        World w=fixture();World.Officer one=w.officer(5),two=officer(w,30,10,85);one.loyalty=50;two.loyalty=60;int gold=w.city(10).gold,ap=w.actionPoints[0];
        byte[] b=SaveCodec.encode(w);check(!w.strategy.rewardOfficers(10,0,new int[]{5,5}).ok,"duplicates rejected");check(Arrays.equals(b,SaveCodec.encode(w)),"duplicate selection atomic");
        check(!w.strategy.rewardOfficers(10,0,new int[]{5,20}).ok,"cross-faction batch rejected");check(Arrays.equals(b,SaveCodec.encode(w)),"invalid mixed batch atomic");
        ok(w.strategy.rewardOfficers(10,0,new int[]{5,30}));check(w.city(10).gold==gold-400&&w.actionPoints[0]==ap-10&&one.loyalty>50&&two.loyalty>60,"two rewards one action and per-recipient gold");
        check(!w.strategy.rewardOfficers(10,5,new int[]{30}).ok,"same turn duplicate reward forbidden");roundtrip(w);
    }
    private static void succession()throws Exception{
        World w=fixture();World.Officer close=officer(w,30,10,90),far=officer(w,31,10,89),spouse=officer(w,32,10,88);
        World.Officer heir=w.officer(5);heir.affinity=0;close.affinity=0;far.affinity=75;spouse.affinity=75;close.loyalty=far.loyalty=spouse.loyalty=100;w.relations.link(spouse.id,heir.id,Relations.Kind.SPOUSE);
        w.life.die(0,"战死");check(w.life.pending(),"ruler battle death uses persisted succession");World copy=SaveCodec.decode(SaveCodec.encode(w));
        ok(w.life.inherit(0,5));ok(copy.life.inherit(0,5));check(close.loyalty==95&&far.loyalty==80&&spouse.loyalty==100,"monotonic affinity loss with spouse protection");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"succession replay and loyalty loss exactly once");roundtrip(w);
        World exile=fixture();ok(exile.campaign.dismiss(10,5,0));check(exile.life.pending()&&exile.life.state(0)==Lifecycle.State.ACTIVE,"living ruler exile triggers succession without killing");roundtrip(exile);ok(exile.life.inherit(0,5));roundtrip(exile);
        World ai=fixture();World.Officer successor=new World.Officer(21,"敌继承",1,20,80,80,80,80,80);ai.officers.add(successor);ai.life.die(20,"寿终");check(successor.role==Strategy.Role.RULER&&!ai.life.pending(),"AI succession completes immediately");
    }
    private static void manpowerAndTitles()throws Exception{
        World w=fixture();World.City c=w.city(10);c.recruitReserve=10000;
        FacilityProductionTest.facility(w,10,Domestic.Kind.FARM);FacilityProductionTest.facility(w,10,Domestic.Kind.MARKET);
        check(Conscription.quarterlyRecovery(w,c)==5500&&Conscription.reserveCap(w,c)==22000,"completed farms plus500; markets plus2000");
        Conscription.Summary before=Conscription.summary(w,0);w.city(13).recruitReserve=999999;w.city(14).recruitReserve=999999;Conscription.Summary after=Conscription.summary(w,0);
        check(before.reserve==after.reserve&&before.cap==after.cap,"ports and gates excluded from manpower aggregate");w.city(13).recruitReserve=20000;w.city(14).recruitReserve=20000;
        RealmOverview.Faction f=new RealmOverview(w).factions.get(0);check(f.manpower==after.reserve&&f.manpowerCap==after.cap&&f.advisor.equals("未任命"),"real overview exposes advisor and manpower");
        int[] thresholds={1,2,4,6,8,12,14,18,20,24};String[] titles={"无爵位","州刺史","州牧","羽林中郎将","五官中郎将","大将军","大司马","公","王","皇帝"};int[] caps={10000,11000,11000,12000,12000,13000,13000,14000,14000,15000};
        World empire=new World(70,35,"甲","乙");for(int i=0;i<25;i++)empire.cities.add(new World.City(i,"城"+i,new Hex(4+(i%10)*6,4+(i/10)*8),i==0?0:1));
        empire.officers.add(new World.Officer(0,"甲君",0,0,90,90,90,90,90));empire.officers.add(new World.Officer(1,"乙君",1,24,90,90,90,90,90));empire.strategy.initializeOffices();
        for(int g=0;g<thresholds.length;g++){
            for(World.City site:empire.cities)site.owner=site.id<thresholds[g]?0:1;
            empire.governance.reconcile(false);check(empire.governance.title(0).equals(titles[g]),"city threshold "+thresholds[g]);check(empire.government.commandLimit(0)==caps[g],"ruler command follows researched non-linear title table");
            if(g<9)check(!empire.governance.nameNation(0,"大汉").ok,"non-emperor cannot name nation");
        }
        ok(empire.governance.nameNation(0,"大汉"));check(empire.governance.label(0).equals("大汉")&&empire.faction(0).equals("大汉")&&empire.factions[0].equals("甲"),"national title preserves underlying identity");
        empire.city(23).owner=1;empire.governance.reconcile(false);check(empire.governance.title(0).equals("皇帝"),"earned titles do not oscillate on city loss");roundtrip(empire);
        byte[] invalidBefore=SaveCodec.encode(empire);check(!empire.governance.nameNation(0,"坏\\n名").ok,"control character rejected");check(Arrays.equals(invalidBefore,SaveCodec.encode(empire)),"invalid title atomic");
        byte[] encoded=SaveCodec.encode(w);ByteArrayOutputStream mapExtension=new ByteArrayOutputStream();CustomMapSave.write(w,new DataOutputStream(mapExtension));
        int extensionSize=8+w.fieldUnits().size()*12+4+w.factions.length*6+mapExtension.size();
        byte[] legacyPayload=Arrays.copyOfRange(encoded,20,encoded.length-extensionSize);CRC32 crc=new CRC32();crc.update(legacyPayload);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x53473131);out.writeInt(31);out.writeInt(legacyPayload.length);out.writeLong(crc.getValue());out.write(legacyPayload);
        World old=SaveCodec.decode(bytes.toByteArray());check(old.governance.title(0).equals("州刺史")&&old.city(10).governorId>=0,"v31 migration defaults zero wounded and initializes governance");roundtrip(old);
    }
    private static void national()throws Exception{
        World w=ScenarioCatalog.load("heroes-250",0,12345L);check(w.factions.length==28&&w.sourceColumns()==200&&w.sourceRows()==200,"national 200x200 / 28-faction scenario retained");
        World twin=SaveCodec.decode(SaveCodec.encode(w));
        for(int i=0;i<3;i++){long begin=System.nanoTime();ok(w.nextTurn());long ms=(System.nanoTime()-begin)/1000000;ok(twin.nextTurn());
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(twin)),"saved national turn retains deterministic outcomes "+i);roundtrip(w);
            System.out.println("GOVERNANCE66 NATIONAL turn="+w.turn+" computeMs="+ms+" units="+w.units.size());}
    }
    public static void main(String[] args)throws Exception{
        wounds();arrivals();capture();governors();rewards();succession();manpowerAndTitles();national();
        System.out.println("GOVERNANCE66 PASS: "+checks+" production-command assertions; wounded/starvation, every site cell, capture/transit/capacity, governors, batch rewards, succession, titles, faction accounts and v31/v32 saves.");
    }
}
