package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Independent boundary oracles exercise production commands, not a second gameplay implementation. */
public final class RulerTitleTest {
    private static int checks;
    private static final int[] CITIES={0,2,4,6,8,12,14,18,20,24};
    private static final int[] TROOPS={10000,11000,11000,12000,12000,13000,13000,14000,14000,15000};
    private static final int[] CIVIL={5000,6000,6000,7000,7000,8000,8000,9000,9000,15000};
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void ok(World.Result result){check(result.ok,result.message);}
    private static World fixture(int cities){
        World w=new World(80,40,"甲","乙");
        for(int i=0;i<25;i++){
            World.City c=new World.City(i,"城"+i,new Hex(4+(i%10)*7,4+(i/10)*8),i<cities?0:1);
            c.gold=50000;c.food=200000;c.troops=50000;Arrays.fill(c.equipment,0);
            for(int k=0;k<4;k++)c.equipment[k]=50000;w.cities.add(c);
        }
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,0,80,80,80,80,80));
        w.officers.add(new World.Officer(100,"乙君",1,24,80,80,80,80,80));
        w.officers.add(new World.Officer(101,"乙将",1,24,80,80,80,80,80));
        w.officer(0).role=Strategy.Role.RULER;w.officer(0).loyalty=100;
        w.officer(100).role=Strategy.Role.RULER;w.officer(100).loyalty=100;
        return w;
    }
    private static byte[] bytes(World w)throws IOException{return SaveCodec.encode(w);}
    private static World copy(World w)throws IOException{return SaveCodec.decode(bytes(w));}
    private static void fresh(World w){w.actionPoints[w.active]=60;for(World.Officer o:w.officers)if(o.owner==w.active)o.acted=false;for(World.Unit u:w.units)if(u.owner==w.active)u.acted=false;}
    private static void reject(World w,Supplier<World.Result> command)throws Exception{
        byte[] before=bytes(w);check(!command.get().ok,"command must reject");check(Arrays.equals(before,bytes(w)),"rejection preserves full save, reports and RNG");
    }
    private static void tech(World w,Campaign.Tech t){if(t.prerequisite!=null)tech(w,t.prerequisite);w.campaign.finishTech(0,t);}
    private static void appoint(World w,int target,String office)throws Exception{fresh(w);ok(w.government.appointRank(0,0,target,office));fresh(w);}
    private static World.Result deploy(World w,int leader,int troops,int... deputies){return w.army.deploy(0,leader,deputies,World.Weapon.SPEAR,Army.Ship.BOAT,troops,troops*2);}
    private static World.Unit unit(World w,int officer,Hex h,int troops){World.Unit u=new World.Unit(w.nextUnitId++,0,officer,World.Weapon.SPEAR,h,troops,20000);w.units.add(u);w.officer(officer).cityId=-1;w.officer(officer).unitId=u.id;return u;}

    public static void main(String[] args)throws Exception{
        tables();cityAccounting();appointments();commands();migration();legacySupply();ai();
        System.out.println("RULER TITLES PASS: "+checks+" assertions (10 titles / 80 offices, city boundaries, authority + merit + vacancy, commander-only caps, technology, legacy saves, supply and AI).");
    }
    private static void tables()throws Exception{
        check(RulerTitles.all().size()==10,"ten titles");
        for(int cities=0;cities<=42;cities++){
            int expected=0;while(expected<9&&cities>=CITIES[expected+1])expected++;
            check(RulerTitles.forCities(cities).grade()==expected,"city boundary "+cities);
            check(RulerTitles.forCities(cities).troops==TROOPS[expected],"nonlinear ruler cap "+cities);
        }
        check(RulerTitles.forCities(16)==RulerTitles.Title.MARSHAL,"16 cities do not grant duke");
        check(RulerTitles.forCities(18)==RulerTitles.Title.DUKE,"duke requires 18 cities");
        check(Government.ranks().size()==80,"all eighty offices");Set<String> unique=new HashSet<>();
        int[][] counts=new int[10][2];
        for(Government.Rank r:Government.ranks()){
            int g=r.requiredTitle.grade();check(unique.add(r.id),"unique office ID");counts[g][r.civilian?1:0]++;
            check(r.merit==g*4000,"merit tier");check(r.troops==(r.civilian?CIVIL[g]:6000+g*1000),"office command table");
            check(r.salary==(r.civilian&&g==9?60:10+5*g),"salary table");
        }
        for(int g=0;g<10;g++){
            check(counts[g][0]==4&&counts[g][1]==4,"four military + four civil per tier");
            World w=fixture(Math.max(1,CITIES[g]));check(w.government.unlockedRanks(0).size()==8*(g+1),"cumulative seats tier "+g);
            check(w.government.commandLimit(0)==TROOPS[g],"ruler command production lookup");
        }
        check(fixture(1).government.unlockedRanks(-1).isEmpty(),"invalid faction has no offices");
    }
    private static void cityAccounting()throws Exception{
        World w=fixture(17);w.city(20).kind=World.SiteKind.PORT;w.city(21).kind=World.SiteKind.GATE;w.city(20).owner=0;w.city(21).owner=0;w.invalidateSiteIndex();
        check(w.governance.cityCount(0)==17,"ports/gates excluded; seven-cell cities count once");
        check(w.governance.cityCount(-1)==0,"unaffiliated not counted");
        byte[] before=bytes(w);for(int i=0;i<10;i++){w.governance.title(0);w.governance.titleProgress(0);w.government.unlockedRanks(0);}
        check(Arrays.equals(before,bytes(w)),"title previews never mutate");
        w.city(17).owner=0;w.governance.reconcile(true);check(w.governance.currentTitle(0)==RulerTitles.Title.DUKE,"18th city promotes");
        w.city(17).owner=1;w.governance.reconcile(true);check(w.governance.currentTitle(0)==RulerTitles.Title.DUKE,"earned title retained after loss");
        check(copy(w).governance.currentTitle(0)==RulerTitles.Title.DUKE,"earned title persists through save");
        check(!fixture(12).governance.canNameNation(0),"new grade5 general is not old grade5 emperor");
        check(fixture(24).governance.canNameNation(0),"only emperor may name nation");
    }
    private static void appointments()throws Exception{
        World low=fixture(1);low.government.earn(1,36000);
        reject(low,()->low.government.appointRank(0,0,1,"大都督"));
        check(low.government.appointmentError(0,0,1,"大都督").contains("爵位不足"),"specific title rejection");
        World w=fixture(8);w.government.earn(1,15999);reject(w,()->w.government.appointRank(0,0,1,"军师将军"));
        check(w.government.appointmentError(0,0,1,"军师将军").contains("功绩不足"),"specific merit rejection");
        w.government.earn(1,1);int gold=w.city(0).gold,ap=w.actionPoints[0];ok(w.government.appointRank(0,0,1,"军师将军"));
        check(w.city(0).gold==gold-100&&w.actionPoints[0]==ap-10,"real appointment cost once");
        fresh(w);w.government.earn(2,16000);reject(w,()->w.government.appointRank(0,0,2,"军师将军"));
        reject(w,()->w.government.appointRank(0,0,1,"军师将军"));
        appoint(w,1,"安国将军");appoint(w,2,"军师将军");
        check(w.government.incumbent(0,"军师将军").id==2,"replacement frees previous seat");
        ok(w.government.removeRank(0,0,2));fresh(w);check(w.government.commandLimit(2)==5000,"removal restores unappointed ceiling");
        appoint(w,3,"左仆射");check(w.government.office(3).civilian&&w.government.commandLimit(3)==5000,"civil office formally appointed");
        w.governance.grades.put(1,4);w.government.earn(101,16000);w.active=1;fresh(w);ok(w.government.appointRank(24,100,101,"安国将军"));
        check(w.government.incumbent(1,"安国将军").id==101&&w.government.incumbent(0,"安国将军").id==1,"same office allowed in different factions");
        World restored=copy(w);check(restored.government.office(3).civilian&&Arrays.equals(bytes(w),bytes(restored)),"civil ranks round trip");
        World invalid=fixture(24);reject(invalid,()->invalid.government.appointRank(0,0,0,"议郎"));reject(invalid,()->invalid.government.appointRank(0,0,1,"missing"));
    }
    private static void commands()throws Exception{
        World w=fixture(1);check(w.government.commandLimit(1)==5000&&w.government.commandLimit(0)==10000,"ruler and unappointed limits differ");check(w.government.commandLimit(-1)==0,"missing officer");
        reject(w,()->deploy(w,1,5001));ok(deploy(w,1,5000));check(copy(w).unit(1).troops==5000,"exact base ceiling deploys");
        World military=fixture(8);military.government.earn(1,16000);appoint(military,1,"军师将军");reject(military,()->deploy(military,1,10001));ok(deploy(military,1,10000));
        World civil=fixture(20);civil.government.earn(1,32000);appoint(civil,1,"光禄勋");reject(civil,()->deploy(civil,1,9001));ok(deploy(civil,1,9000));
        World deputy=fixture(24);deputy.government.earn(2,36000);appoint(deputy,2,"大都督");reject(deputy,()->deploy(deputy,1,5001,2));ok(deploy(deputy,1,5000,2));
        check(deputy.unit(1).deputies[0]==2&&deputy.unit(1).troops==5000,"deputy does not raise/sum command capacity");
        World reformed=fixture(24);tech(reformed,Campaign.Tech.MILITARY_REFORM);check(reformed.government.commandLimit(1)==8000&&reformed.government.commandLimit(0)==18000,"technology exactly +3000");
        reformed.government.earn(1,36000);appoint(reformed,1,"丞相");check(reformed.government.commandLimit(1)==18000,"top civil + tech");reject(reformed,()->deploy(reformed,1,18001));ok(deploy(reformed,1,18000));
        check(copy(reformed).government.commandLimit(1)==18000,"technology and office persist without double addition");
    }
    private static byte[] segment(int magic,int grade,String name)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);d.writeInt(magic);d.writeInt(0);d.writeInt(2);d.writeInt(grade);d.writeUTF(name);d.writeInt(0);d.writeUTF("");return out.toByteArray();
    }
    private static void migration()throws Exception{
        int[] expected={0,1,2,5,8,9};
        for(int g=0;g<6;g++){
            World w=fixture(1);w.governance.read(new DataInputStream(new ByteArrayInputStream(segment(0x47563636,g,g==5?"汉":""))));
            check(w.governance.grade(0)==expected[g],"legacy grade maps by meaning "+g);
            World restored=copy(w);check(restored.governance.grade(0)==expected[g],"migrated title round trip "+g);
            if(g==5)check(restored.governance.nation(0).equals("汉"),"legacy imperial nation preserved");
        }
        for(int[] bad:new int[][]{{0,0},{0x47563636,6},{0x47563637,10}}){
            boolean rejected=false;try{fixture(1).governance.read(new DataInputStream(new ByteArrayInputStream(segment(bad[0],bad[1],""))));}catch(IOException ex){rejected=true;}check(rejected,"malformed marker/grade rejected");
        }
        World grandfather=fixture(1);grandfather.government.earn(1,36000);grandfather.government.ranks.put(1,"大都督");
        check(copy(grandfather).government.commandLimit(1)==15000,"existing valid office preserved despite newly added title gate");
        fresh(grandfather);grandfather.government.earn(2,36000);reject(grandfather,()->grandfather.government.appointRank(0,0,2,"卫将军"));
    }
    private static void legacySupply()throws Exception{
        World w=fixture(1);World.Unit old=unit(w,1,new Hex(5,4),10000);old.wounded=200;
        World.Unit source=unit(w,2,new Hex(6,4),3000);check(copy(w).unit(old.id).troops==10000,"legacy oversized unit not truncated");
        reject(w,()->w.supply.transfer(source.id,old.id,1,0));ok(w.supply.transfer(source.id,old.id,0,100));check(old.troops==10000&&old.food==20100,"food-only unit resupply preserved");
        fresh(w);reject(w,()->w.supply.replenish(0,0,old.id,1,0));ok(w.supply.replenish(0,0,old.id,0,100));check(old.wounded==200&&old.troops==10000,"city food supply preserves wounded and troops");
        check(Arrays.equals(bytes(w),bytes(copy(w))),"legacy oversized and wounded exact save round trip");
        World cargoWorld=fixture(2);World.Unit destination=unit(cargoWorld,1,new Hex(7,4),10000);
        ok(cargoWorld.domestic.transport(0,1,2,100,1000,1000,new int[4]));
        Domestic.Mission cargo=cargoWorld.domestic.missions.get(0);cargo.hex=new Hex(6,4);cargo.acted=false;
        reject(cargoWorld,()->cargoWorld.supply.convoyTransfer(cargo.id,destination.id,1,0,0));
        ok(cargoWorld.supply.convoyTransfer(cargo.id,destination.id,0,100,10));check(destination.troops==10000&&destination.gold==10,"convoy food and gold supply preserved");
    }
    private static void ai()throws Exception{
        World w=fixture(24);w.government.earn(101,36000);w.active=1;fresh(w);w.governance.reconcile(false);World twin=copy(w);
        int gold=w.city(24).gold,ap=w.actionPoints[1];w.government.appointAiRank();twin.government.appointAiRank();
        check(w.government.office(101)!=null&&w.government.office(101).requiredTitle==RulerTitles.Title.NONE,"AI obeys one-city authority despite high merit");
        check(w.city(24).gold==gold-100&&w.actionPoints[1]==ap-10,"AI uses real appointment costs");
        check(Arrays.equals(bytes(w),bytes(twin)),"AI deterministic across save replay");
        w.actionPoints[1]=20;byte[] before=bytes(w);w.government.appointAiRank();check(Arrays.equals(before,bytes(w)),"AI reserves minimum campaign AP");
        World player=fixture(8);player.government.earn(1,16000);before=bytes(player);player.government.appointAiRank();check(Arrays.equals(before,bytes(player)),"player not silently auto-appointed");
    }
}
