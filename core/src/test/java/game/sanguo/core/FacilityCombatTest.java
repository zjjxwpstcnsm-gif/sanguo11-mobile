package game.sanguo.core;

import java.util.*;
import java.nio.file.*;

/** Facility combat must change the real economy, occupancy and saved game, with atomic failures. */
public final class FacilityCombatTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void ok(World.Result r){check(r.ok,r.message);}
    static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    static void reject(World w,java.util.function.Supplier<World.Result> command)throws Exception{
        byte[] before=bytes(w);check(!command.get().ok,"invalid command fails");check(Arrays.equals(before,bytes(w)),"invalid command leaves state and RNG intact");
    }
    static Domestic.Facility add(World w,Domestic.Kind kind){
        Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,2,kind,new Hex(13,1),-1,0);w.domestic.facilities.add(f);return f;
    }
    static World.Unit unit(World w,World.Weapon weapon){World.Unit u=new World.Unit(w.nextUnitId++,0,1,weapon,new Hex(12,1),5000,20000);w.units.add(u);w.officer(1).unitId=u.id;w.officer(1).cityId=-1;return u;}
    public static void main(String[] args)throws Exception{
        normal();rejections();siege();construction();march();migration();
        System.out.println("PASS: "+checks+" facility combat assertions: normal/siege/fire, economy/occupancy/production, atomic rejection, route invalidation, v17 migration and v18 persistence.");
    }
    static void normal()throws Exception{
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY,World.Weapon.SWORD}){
            World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.MARKET);World.Unit u=unit(w,weapon);
            int income=w.domestic.monthlyGold(2);byte[] before=bytes(w);
            check(w.war.facilityAttackError(u.id,f.hex)==null&&w.war.facilityDamage(u.id)>0,"facility is legal normal target");
            check(Arrays.equals(before,bytes(w)),"damage preview is pure");
            World.Result hit=w.war.attackFacility(u.id,f.hex);ok(hit);check(f.hp<1000&&f.hp>0&&u.acted&&hit.feedback==World.Feedback.ATTACK,"normal attack pays action and damages durable target");
            reject(w,()->w.war.attackFacility(u.id,f.hex));
            World restored=SaveCodec.decode(bytes(w));check(restored.domestic.facility(f.id).hp==f.hp,"partial facility HP survives reload");
            check(Arrays.equals(bytes(w),bytes(restored)),"exact current roundtrip");
            f.hp=1;w.orders.reset(u);World.Result kill=w.war.attackFacility(u.id,f.hex);ok(kill);
            check(w.domestic.at(f.hex)==null&&kill.feedback==World.Feedback.DEFEAT&&kill.message.contains("地块已释放"),"destruction reports released tile");
            check(w.domestic.monthlyGold(2)<income,"destroyed market ceases contributing income");
            World.Unit follower=GovernmentTest.unit(w,2,0,new Hex(12,2),3000);
            check(w.orders.marchReachable(follower).containsKey(f.hex),"fresh unit can reach released facility cell");
            ok(w.marches.execute(w.marches.preview(follower.id,f.hex)));check(follower.hex.equals(f.hex),"real movement occupies destroyed facility cell");SaveCodec.validate(w);
        }
    }
    static void rejections()throws Exception{
        World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.FARM);World.Unit u=unit(w,World.Weapon.SPEAR);
        u.hex=new Hex(7,1);reject(w,()->w.war.attackFacility(u.id,f.hex));u.hex=new Hex(12,1);
        u.status=War.Status.CONFUSED;u.statusTurns=1;reject(w,()->w.war.attackFacility(u.id,f.hex));u.status=War.Status.NORMAL;u.statusTurns=0;
        reject(w,()->w.war.attackFacility(u.id,new Hex(12,2)));
        World.Unit friend=GovernmentTest.unit(w,7,1,new Hex(13,2),3000);w.active=1;reject(w,()->w.war.attackFacility(friend.id,f.hex));w.active=0;
        w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,6));reject(w,()->w.war.attackFacility(u.id,f.hex));
    }
    static void siege()throws Exception{
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT}){
            World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.FARM);World.Unit u=unit(w,weapon);Army.Tactic t=w.army.tactics(u).get(0);
            reject(w,()->w.war.attackFacility(u.id,f.hex));int energy=u.energy;
            ok(w.army.tactic(u.id,f.hex,t));check(u.energy==energy-t.energy&&u.acted&&f.hp<1000,"siege tactic targets facility and pays exactly once");SaveCodec.validate(w);
        }
        World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.FARM);World.Unit u=unit(w,World.Weapon.SPEAR);f.hp=150;
        w.war.ignite(f.hex,u);w.war.tick();check(w.domestic.at(f.hex)==null,"persistent fire destroys facility");SaveCodec.validate(w);
    }
    static void construction()throws Exception{
        World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.WORKSHOP);World.Unit u=unit(w,World.Weapon.SPEAR);
        w.active=1;ok(w.army.produce(2,7,World.Weapon.RAM,null));w.active=0;f.hp=1;ok(w.war.attackFacility(u.id,f.hex));
        check(w.army.productions.isEmpty()&&w.officer(7).otherTask.isEmpty(),"factory destruction terminates dependent production");
        w.orders.reset(u);f=add(w,Domestic.Kind.FARM);f.remaining=2;f.builderId=8;f.hp=1;
        ok(w.war.attackFacility(u.id,f.hex));check(!w.domestic.busy(8)&&w.officer(8).acted,"construction builder released without a free action");SaveCodec.validate(w);
    }
    static void march()throws Exception{
        World w=GovernmentTest.world();Domestic.Facility f=add(w,Domestic.Kind.MARKET);World.Unit u=unit(w,World.Weapon.SPEAR);u.hex=new Hex(4,1);
        MarchOrders.Plan plan=w.marches.preview(u.id,f.hex);check(plan.valid()&&plan.label.equals("市场")&&plan.path.get(plan.path.size()-1).distance(f.hex)==1,"facility route approaches without occupying or attacking it");
        ok(w.marches.execute(plan));check(u.march!=null&&u.march.kind==MarchOrders.Kind.FACILITY,"facility order retained across turns");
        World restored=SaveCodec.decode(bytes(w));check(restored.unit(u.id).march.kind==MarchOrders.Kind.FACILITY,"facility order roundtrips");
        w.domestic.damage(f,1000);w.orders.reset(u);Hex before=u.hex;w.marches.advanceAll();check(u.hex.equals(before)&&!u.march.paused.isEmpty(),"destroyed target pauses instead of retargeting stale object");SaveCodec.validate(w);
    }
    static void migration()throws Exception{
        try(java.io.InputStream in=FacilityCombatTest.class.getResourceAsStream("/save-v17-facility.sg11")){
            check(in!=null,"real pre-change v17 fixture exists");World w=SaveCodec.read(in);Domestic.Facility f=w.domestic.facilities.get(0);
            check(f.hp==1000&&f.level==3,"v17 existing facility keeps level and gains full durability");check(bytes(w)[7]==25,"writer uses v19");
            f.hp=234;check(SaveCodec.decode(bytes(w)).domestic.facility(f.id).hp==234,"migrated facility can be damaged and reloaded");
            f.hp=0;try{bytes(w);throw new AssertionError("zero hp should be removed");}catch(java.io.IOException expected){checks++;}
        }
    }
}
