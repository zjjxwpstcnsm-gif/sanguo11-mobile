package game.sanguo.core;

import java.util.*;
import java.io.*;

/** Prisoners have exactly one location, separate from a unit's fighting crew. */
public final class PrisonerEscortTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void ok(World.Result r){check(r.ok,r.message);}
    static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    static void rejected(World w,java.util.function.Supplier<World.Result> command)throws Exception{
        byte[] before=SaveCodec.encode(w);check(!command.get().ok,"command rejected");check(Arrays.equals(before,SaveCodec.encode(w)),"rejection preserves state including location and RNG");
    }
    static World battle(){
        World w=GovernmentTest.world();GovernmentTest.unit(w,1,0,new Hex(4,4),5000,2,3);GovernmentTest.unit(w,7,1,new Hex(5,4),1,8);
        w.officer(1).skillId=Skill.BOFU.id;ok(w.attack(1,2));return w;
    }
    public static void main(String[] args)throws Exception{
        marchAndEnter();escortDefeat();deathAndRansom();migrationAndValidation();noCity();
        System.out.println("PASS: "+checks+" prisoner escort assertions: movement, atomic entry, permanent city jail, rescue/transfer, carrier death/succession, ransom, v15 migration and invalid saves.");
    }
    static void marchAndEnter()throws Exception{
        World w=battle();Government g=w.government;World.Unit u=w.unit(1);Government.Prisoner p=g.prisoner(7);
        check(p.unitId==u.id&&p.cityId==-1&&g.escorted(u.id).size()==2,"capture joins actual victor, no city teleport");
        check(w.army.crew(u).size()==3&&w.officer(7).unitId==-1&&w.officer(7).owner==1,"escorts do not fill crew slots or switch allegiance");
        check(g.location(p).equals(u.hex)&&g.locationLabel(p).contains("将1部队"),"location describes escort");
        rejected(w,()->g.recruitPrisoner(0,0,7));rejected(w,()->g.release(0,0,7));rejected(w,()->w.life.executePrisoner(0,0,7));
        w.orders.reset(u);ok(w.marches.execute(w.marches.preview(u.id,new Hex(5,4))));
        check(g.location(p).equals(new Hex(5,4)),"prisoner follows actual move into defeated tile");
        World restored=copy(w);check(restored.government.prisoner(7).unitId==u.id&&restored.government.location(restored.government.prisoner(7)).equals(u.hex),"marching prisoner save roundtrip");
        u.hex=new Hex(2,1);w.orders.reset(u);w.city(0).troops=w.campaign.troopCap(w.city(0));
        rejected(w,()->w.enter(u.id,0));check(p.unitId==u.id&&p.cityId==-1,"failed entry does not jail prisoner early");
        w.city(0).troops=20000;World.Result entry=w.enter(u.id,0);ok(entry);
        check(entry.message.contains("俘虏2人")&&p.unitId==-1&&p.cityId==0&&w.unit(u.id)==null,"successful entry jails all prisoners exactly once");
        check(g.location(p).equals(w.city(0).hex)&&g.escorted(u.id).isEmpty(),"jailed location no longer references removed escort");
        GovernmentTest.refresh(w);ok(w.army.deploy(0,1,new int[]{2,3},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));
        check(p.cityId==0&&p.unitId==-1&&g.escorted(w.officer(1).unitId).isEmpty(),"commander redeployment leaves prisoners in entered city");copy(w);
    }
    static void escortDefeat()throws Exception{
        World w=battle();World.Unit carrier=w.unit(1),rescuer=GovernmentTest.unit(w,9,1,new Hex(6,4),5000);
        w.defeatUnit(carrier,rescuer);
        check(!w.government.captive(7)&&!w.government.captive(8)&&w.officer(7).cityId==2,"original faction defeats carrier and rescues prisoners");copy(w);
        w=battle();carrier=w.unit(1);World.Unit third=GovernmentTest.unit(w,11,2,new Hex(6,4),5000);
        w.defeatUnit(carrier,third);Government.Prisoner p=w.government.prisoner(7);
        check(p.captor==2&&p.unitId==third.id&&p.cityId==-1,"third faction takes over escort without teleport");
        byte[] before=SaveCodec.encode(w);w.defeatUnit(carrier,third);check(Arrays.equals(before,SaveCodec.encode(w)),"duplicate carrier defeat cannot duplicate transfer");copy(w);
        w.removeUnit(third);check(w.government.prisoners().isEmpty()&&w.officer(7).cityId==2,"unattributed carrier destruction releases all carried prisoners");copy(w);
    }
    static void deathAndRansom()throws Exception{
        World w=battle();w.life.die(1,"test");
        check(w.unit(1)!=null&&w.unit(1).officerId==2&&w.government.prisoner(7).unitId==1&&w.government.locationLabel(w.government.prisoner(7)).contains("将2部队"),"deputy succession preserves escort identity");copy(w);
        w.life.die(2,"test");w.life.die(3,"test");check(w.unit(1)==null&&!w.government.captive(7),"last crew death releases escorts without orphan reference");copy(w);
        w=battle();w.active=1;int total=w.city(2).gold+w.unit(1).gold;ok(w.government.ransom(2,6,7));
        check(!w.government.captive(7)&&w.officer(7).cityId==2&&w.city(2).gold+w.unit(1).gold==total,"ransom in field pays carrier and returns officer home");
        w.unit(1).gold=10000;w.officer(6).acted=false;World full=w;rejected(w,()->full.government.ransom(2,6,8));copy(w);
    }
    static void migrationAndValidation()throws Exception{
        byte[] raw;try(InputStream in=PrisonerEscortTest.class.getResourceAsStream("/legacy-v15.sg11.b64")){raw=Base64.getMimeDecoder().decode(in.readAllBytes());}
        check(raw[7]==15,"fixture produced by untouched v15 writer");World old=SaveCodec.decode(raw);Government.Prisoner p=old.government.prisoner(7);
        check(p.cityId==0&&p.unitId==-1,"old city prisoners stay at original jail");check(SaveCodec.encode(old)[7]==19,"v15 upgrades to v17");copy(old);
        World w=battle();p=w.government.prisoner(7);p.unitId=999;invalid(w);p.unitId=1;p.cityId=0;invalid(w);p.cityId=-1;p.captor=2;invalid(w);
    }
    static void invalid(World w)throws Exception{try{SaveCodec.encode(w);throw new AssertionError("invalid escort accepted");}catch(IOException expected){checks++;}}
    static void noCity()throws Exception{
        World w=battle();for(World.City c:w.cities)if(c.owner==0){c.owner=2;for(World.Officer o:w.officers)if(o.cityId==c.id&&o.owner==0)w.retreat(o,c.hex);}
        World.Unit enemy=GovernmentTest.unit(w,9,1,new Hex(6,4),1);check(w.government.captureChance(w.unit(1),enemy,w.officer(9))==100,"field army can capture without a home city");
        w.defeatUnit(enemy,w.unit(1));check(w.government.prisoner(9).unitId==1,"homeless army carries new prisoner");copy(w);
    }
}
