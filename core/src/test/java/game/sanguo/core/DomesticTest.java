package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Behavioral and conservation regression tests. No Android or external test dependency. */
public final class DomesticTest {
    private static int checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static void reject(World w,Supplier<World.Result> call)throws Exception{
        byte[] before=SaveCodec.encode(w);check(!call.get().ok,"invalid command accepted");check(Arrays.equals(before,SaveCodec.encode(w)),"failed command mutated state");
    }
    private static void invalid(World w)throws Exception{try{SaveCodec.encode(w);throw new AssertionError("invalid save accepted");}catch(IOException expected){checks++;}}
    private static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    private static World fixture(){
        World w=new World(15,12);w.cities.add(new World.City(10,"甲",new Hex(2,2),0));w.cities.add(new World.City(20,"乙",new Hex(9,2),0));w.cities.add(new World.City(30,"敌",new Hex(13,10),1));
        for(int i=0;i<12;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,i==1?60:90,80));
        w.city(10).gold=100000;w.city(30).gold=0;w.city(30).troops=0;w.city(30).food=0;return w;
    }
    private static Domestic.Facility build(World w,Domestic.Kind kind,int officer){ok(w.domestic.build(10,officer,kind,w.domestic.buildSites(10).get(0)));return w.domestic.facilities.get(w.domestic.facilities.size()-1);}
    private static void next(World w)throws Exception{ok(w.nextTurn());SaveCodec.validate(w);}
    public static void main(String[] args)throws Exception{
        building();economy();movement();capacity();capture();validation();legacy();campaigns();
        System.out.println("PASS: "+checks+" domestic assertions covering facilities, travel, cargo conservation, rerouting, capacity and save v1/v2/v3/v4 continuity.");
    }
    private static void building()throws Exception{
        World w=fixture();Hex site=w.domestic.buildSites(10).get(0);byte[] original=SaveCodec.encode(w);
        reject(w,()->w.domestic.build(999,0,Domestic.Kind.MARKET,site));reject(w,()->w.domestic.build(30,0,Domestic.Kind.MARKET,site));
        reject(w,()->w.domestic.build(10,999,Domestic.Kind.MARKET,site));reject(w,()->w.domestic.build(10,0,null,site));
        reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,null));reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,w.city(10).hex));
        reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,new Hex(-1,2)));reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,new Hex(7,7)));
        for(World.Terrain terrain:new World.Terrain[]{World.Terrain.WATER,World.Terrain.MOUNTAIN,World.Terrain.FOREST}){
            w.terrain[site.q][site.r]=terrain;reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,site));
        }
        w.terrain[site.q][site.r]=World.Terrain.PLAIN;w.city(10).gold=999;reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,site));w.city(10).gold=100000;
        w.actionPoints[0]=9;reject(w,()->w.domestic.build(10,0,Domestic.Kind.MARKET,site));w.actionPoints[0]=60;
        check(Arrays.equals(original,SaveCodec.encode(w)),"invalid build probes preserve original");
        Domestic.Facility f=build(w,Domestic.Kind.MARKET,0);check(f.remaining==2&&w.domestic.busy(0),"high politics occupies builder for two turns");
        reject(w,()->w.deploy(10,0,World.Weapon.SPEAR,3000));reject(w,()->w.domestic.build(10,1,Domestic.Kind.FARM,site));
        check(w.domestic.monthlyGold(10)==800,"unfinished market gives no yield");next(w);check(f.remaining==1,"first tick construction");
        check(!w.officer(0).acted&&w.domestic.busy(0)&&!w.idle(w.city(10)).contains(w.officer(0)),"reset does not free builder");
        World restored=copy(w);check(restored.domestic.busy(0)&&restored.domestic.facility(f.id).remaining==1,"construction restored");next(restored);
        check(restored.domestic.monthlyGold(10)==1400&&!restored.domestic.busy(0),"completion frees officer and activates market");
        ok(restored.domestic.demolish(f.id,0));check(restored.domestic.count(10)==0&&restored.domestic.monthlyGold(10)==800,"demolition removes effect");
        next(w);Domestic.Facility slow=build(w,Domestic.Kind.FARM,1);check(slow.remaining==3,"low politics requires three turns");next(w);check(slow.remaining==2,"slow construction advances");
        int gold=w.city(10).gold;ok(w.domestic.cancelBuild(slow.id));check(w.city(10).gold==gold&&w.officer(1).acted&&!w.domestic.busy(1),"cancel no refund or same-turn reuse");
        reject(w,()->w.domestic.cancelBuild(slow.id));reject(w,()->w.domestic.demolish(f.id,1));
        World full=fixture();for(int i=0;i<6;i++)build(full,Domestic.Kind.FARM,i);next(full);
        reject(full,()->full.domestic.build(10,6,Domestic.Kind.MARKET,new Hex(3,2)));check(full.domestic.count(10)==6,"six slots include construction");
        World blocked=fixture();for(Hex h:blocked.city(10).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.WATER;
        Hex exit=new Hex(3,2);blocked.terrain[exit.q][exit.r]=World.Terrain.PLAIN;
        reject(blocked,()->blocked.domestic.build(10,0,Domestic.Kind.MARKET,exit));
        check(!blocked.domestic.buildSites(10).contains(exit),"last city exit protected");
        World deployed=fixture();ok(deployed.deploy(10,0,World.Weapon.SPEAR,3000));Hex occupied=deployed.unit(1).hex;
        reject(deployed,()->deployed.domestic.build(10,1,Domestic.Kind.FARM,occupied));
        Domestic.Facility obstacle=build(deployed,Domestic.Kind.MARKET,2);check(!deployed.reachable(deployed.unit(1)).containsKey(obstacle.hex),"facilities block tactical movement");
    }
    private static void economy()throws Exception{
        World w=fixture();for(int i=0;i<4;i++)build(w,Domestic.Kind.values()[i],i==1?4:i);next(w);next(w);
        check(w.domestic.recruitAmount(10)==2750&&w.domestic.produceAmount(10)==2750,"completed barracks and smith improve orders");
        int troops=w.city(10).troops,equipment=w.city(10).equipment[0];ok(w.recruit(10,0));ok(w.produce(10,2,World.Weapon.SPEAR));
        check(w.city(10).troops==troops+2750&&w.city(10).equipment[0]==equipment+2750,"actual command yields use facilities");
        w.city(10).troops=99000;reject(w,()->w.recruit(10,3));w.city(10).equipment[0]=99000;reject(w,()->w.produce(10,3,World.Weapon.SPEAR));
        w.city(10).troops=0;int gold=w.city(10).gold,food=w.city(10).food;next(w);
        check(w.city(10).gold==gold+1330&&w.city(10).food==food,"month income uses completed facilities; food waits for the season");
        w.city(10).gold=999999;w.city(10).food=999999;for(int i=0;i<6;i++)next(w);
        check(w.city(10).gold==1000000&&w.city(10).food==1000000,"month and season caps prevent overflow");
    }
    private static void movement()throws Exception{
        World w=fixture();reject(w,()->w.domestic.transfer(10,10,0));reject(w,()->w.domestic.transfer(10,30,0));reject(w,()->w.domestic.transfer(10,999,0));
        reject(w,()->w.domestic.transport(10,20,0,0,0,0,new int[4]));reject(w,()->w.domestic.transport(10,20,0,0,0,0,null));
        reject(w,()->w.domestic.transport(10,20,0,0,0,0,new int[3]));
        for(int bad:new int[]{-1,Integer.MAX_VALUE}){
            reject(w,()->w.domestic.transport(10,20,0,bad,1,0,new int[4]));reject(w,()->w.domestic.transport(10,20,0,0,bad,0,new int[4]));
            reject(w,()->w.domestic.transport(10,20,0,0,1,bad,new int[4]));reject(w,()->w.domestic.transport(10,20,0,0,1,0,new int[]{bad,0,0,0}));
        }
        reject(w,()->w.domestic.transport(10,20,0,100001,1,0,new int[4]));reject(w,()->w.domestic.transport(10,20,0,0,50000,0,new int[4]));
        reject(w,()->w.domestic.transport(10,20,0,0,1,15000,new int[4]));reject(w,()->w.domestic.transport(10,20,0,0,1,0,new int[]{15000,0,0,0}));
        int[] cargo={500,600,700,800};ok(w.domestic.transport(10,20,0,1000,5000,1000,cargo));cargo[0]=999;
        Domestic.Mission m=w.domestic.missions.get(0);check(m.equipment[0]==500,"cargo defensively copied");
        check(w.city(10).gold==99000&&w.city(10).food==35000&&w.city(10).troops==11000,"dispatch deducts only cargo; manual p38 no gold fee");
        check(w.officer(0).cityId==-1&&w.domestic.busy(0),"officer in transit not available");reject(w,()->w.domestic.transfer(10,20,0));
        check(w.domestic.eta(m)==2,"seven plain steps need two ticks");next(w);check(m.hex.distance(w.city(10).hex)==4&&w.city(20).troops==12000,"no teleport/early delivery");
        World clone=copy(w);next(w);next(clone);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(clone)),"transit save continuation deterministic");
        check(w.domestic.missions.isEmpty()&&w.officer(0).cityId==20&&w.city(20).troops==13000,"cargo delivered and officer placed");
        check(w.city(20).gold==6000&&w.city(20).equipment[0]==12500,"gold and equipment delivered");next(w);check(w.city(20).troops==13000&&w.city(20).equipment[0]==12500,"no duplicate cargo delivery");
        World personnel=fixture();ok(personnel.domestic.transfer(10,20,0));next(personnel);next(personnel);check(personnel.officer(0).cityId==20&&personnel.city(10).gold==100000,"personnel travel has no gold fee");
        World forest=fixture();for(int q=0;q<forest.width;q++)for(int r=0;r<forest.height;r++)if(forest.cityAt(new Hex(q,r))==null)forest.terrain[q][r]=World.Terrain.FOREST;
        ok(forest.domestic.transfer(10,20,0));int estimate=forest.domestic.eta(forest.domestic.missions.get(0)),elapsed=0;
        while(!forest.domestic.missions.isEmpty()&&elapsed<20){next(forest);elapsed++;}check(elapsed==estimate&&elapsed>2,"weighted route ETA matches actual travel");
        World island=fixture();for(Hex h:island.city(20).hex.neighbors())island.terrain[h.q][h.r]=World.Terrain.WATER;
        reject(island,()->island.domestic.transfer(10,20,0));reject(island,()->island.domestic.transport(10,20,0,0,1,0,new int[4]));
    }
    private static void capacity()throws Exception{
        for(int resource=0;resource<7;resource++){
            World w=fixture();World.City d=w.city(20);int gold=resource==0?1:0,food=resource==1?1:resource==2?20:0,troops=resource==2?1:0;int[] eq=new int[4];if(resource>=3)eq[resource-3]=1;
            if(resource==0)d.gold=1000000;if(resource==1){d.food=1000000;d.troops=0;}if(resource==2)d.troops=100000;if(resource>=3)d.equipment[resource-3]=100000;
            ok(w.domestic.transport(10,20,0,gold,food,troops,eq));next(w);next(w);
            check(w.domestic.missions.size()==1&&w.domestic.missions.get(0).hex.distance(d.hex)==1,"capacity wait retains cargo "+resource);
            World clone=copy(w);check(clone.domestic.missions.size()==1&&clone.officer(0).cityId==-1,"capacity wait survives save "+resource);
            d.gold=Math.min(d.gold,999999);d.food=Math.min(d.food,999999);d.troops=Math.min(d.troops,99999);for(int i=0;i<4;i++)d.equipment[i]=Math.min(d.equipment[i],99999);
            next(w);check(w.domestic.missions.isEmpty()&&w.officer(0).cityId==20,"capacity release delivers once "+resource);
        }
        World back=fixture();ok(back.domestic.transport(10,20,0,0,1000,0,new int[4]));next(back);int id=back.domestic.missions.get(0).id;
        reject(back,()->back.domestic.redirect(id,30));reject(back,()->back.domestic.redirect(id,20));
        ok(back.domestic.redirect(id,10));check(back.actionPoints[0]==50,"redirect AP cost");next(back);check(back.domestic.missions.isEmpty()&&back.officer(0).cityId==10,"return to source supported");
        World blocked=fixture();ok(blocked.domestic.transfer(10,20,0));next(blocked);Hex before=blocked.domestic.missions.get(0).hex;
        for(Hex h:blocked.city(20).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.WATER;
        next(blocked);check(blocked.domestic.missions.get(0).hex.equals(before),"new obstruction waits without teleport");
        for(Hex h:blocked.city(20).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.PLAIN;next(blocked);check(blocked.officer(0).cityId==20,"reopened road resumes");
    }
    private static void capture()throws Exception{
        World w=fixture();ok(w.domestic.transport(10,20,0,1000,1000,0,new int[4]));next(w);w.city(20).owner=1;next(w);
        check(w.domestic.missions.isEmpty()&&w.officer(0).cityId==10,"lost destination reroutes to reachable own city");check(w.city(20).gold==5000,"no cargo delivered to enemy");
        World lost=fixture();ok(lost.domestic.transfer(10,20,0));lost.city(10).owner=1;lost.city(20).owner=1;
        for(World.Officer o:lost.officers)o.cityId=-1;lost.checkVictory();check(lost.domestic.missions.isEmpty()&&lost.gameOver(),"missions do not keep defeated faction alive");SaveCodec.validate(lost);
        World siege=fixture();Domestic.Facility built=build(siege,Domestic.Kind.FARM,0);next(siege);next(siege);Domestic.Facility progress=build(siege,Domestic.Kind.MARKET,1);
        siege.officers.add(new World.Officer(99,"敌将",1,30,99,99,99,99,99));siege.city(30).troops=6000;siege.city(30).food=12000;siege.active=1;
        ok(siege.deploy(30,99,World.Weapon.SPEAR,3000));World.Unit unit=siege.unit(siege.officer(99).unitId);
        for(Hex h:siege.city(10).hex.neighbors())if(siege.domestic.at(h)==null){unit.hex=h;break;}
        siege.city(10).defense=1;ok(siege.siege(unit.id,10));check(siege.domestic.facility(progress.id)==null&&!siege.domestic.busy(1),"capture cancels construction");
        check(siege.domestic.facility(built.id)!=null&&siege.city(10).owner==1,"completed facilities retained by victor");check(siege.officer(1).cityId==20,"builder retreats to surviving own city");SaveCodec.validate(siege);
    }
    private static void validation()throws Exception{
        World w=fixture();Domestic.Facility f=build(w,Domestic.Kind.MARKET,0);f.remaining=4;invalid(w);f.remaining=2;f.builderId=999;invalid(w);f.builderId=0;
        w.domestic.facilities.add(f);invalid(w);w.domestic.facilities.remove(w.domestic.facilities.size()-1);f.remaining=0;invalid(w);f.builderId=-1;SaveCodec.validate(w);
        World m=fixture();ok(m.domestic.transport(10,20,0,0,1000,0,new int[4]));Domestic.Mission task=m.domestic.missions.get(0);
        task.targetCity=999;invalid(m);task.targetCity=20;task.equipment[0]=-1;invalid(m);task.equipment[0]=0;m.officer(0).cityId=10;invalid(m);m.officer(0).cityId=-1;
        m.domestic.missions.add(task);invalid(m);m.domestic.missions.remove(1);SaveCodec.validate(m);
        byte[] bytes=SaveCodec.encode(m);bytes[bytes.length-1]^=1;try{SaveCodec.decode(bytes);throw new AssertionError("CRC ignored");}catch(IOException expected){checks++;}
    }
    private static void legacy()throws Exception{
        for(String file:new String[]{"/m0-v1.sg11.b64","/m1-v2.sg11.b64"}){
            byte[] original;try(InputStream in=DomesticTest.class.getResourceAsStream(file)){if(in==null)throw new IOException(file);original=Base64.getMimeDecoder().decode(in.readAllBytes());}
            check(original[7]==(file.contains("v1")?1:2),"fixture genuinely old version");World w=SaveCodec.decode(original);check(w.domestic.facilities.isEmpty()&&w.domestic.missions.isEmpty(),"legacy initializes empty strategic layer");
            World clone=copy(w);check(SaveCodec.encode(w)[7]==22,"new writes use save v19");for(int i=0;i<5&&!w.gameOver();i++){next(w);next(clone);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(clone)),"legacy deterministic continuation");}
        }
    }
    private static void campaigns()throws Exception{
        for(int player=0;player<3;player++){
            World w=TestScenarios.load("regional-sandbox",player);
            for(int t=0;t<45&&!w.gameOver();t++){
                for(World.City c:new ArrayList<>(w.cities))if(c.owner==player){
                    List<World.Officer> idle=w.idle(c);List<Hex> sites=w.domestic.buildSites(c.id);
                    if(!idle.isEmpty()&&!sites.isEmpty()&&w.domestic.count(c.id)<2)w.domestic.build(c.id,idle.get(0).id,Domestic.Kind.values()[t%4],sites.get(0));
                    idle=w.idle(c);if(!idle.isEmpty()&&t%8==0)for(World.City d:w.cities)if(d.owner==player&&d.id!=c.id){w.domestic.transport(c.id,d.id,idle.get(0).id,0,1000,100,new int[4]);break;}
                }
                World clone=copy(w);next(w);next(clone);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(clone)),"campaign deterministic "+player+"/"+t);
                check(w.cities.stream().allMatch(c->c.gold>=0&&c.food>=0&&c.troops>=0),"campaign resources nonnegative");
            }
        }
    }
}
