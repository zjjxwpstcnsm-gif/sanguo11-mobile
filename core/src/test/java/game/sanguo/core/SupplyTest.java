package game.sanguo.core;

import java.util.*;
import static game.sanguo.core.GovernmentTest.*;

public final class SupplyTest {
    public static void main(String[] args)throws Exception{
        transfers();raids();water();
        System.out.println("PASS: "+checks+" supply assertions: troop/food conservation, no duplicate delivery, raid/capture/treaty, amphibious routes and save restoration.");
    }
    static void transfers()throws Exception{
        World w=world();World.Unit a=unit(w,1,0,new Hex(4,4),5000),b=unit(w,2,0,new Hex(5,4),3000);
        int troops=a.troops+b.troops,food=a.food+b.food;ok(w.supply.transfer(a.id,b.id,1000,5000));
        check(a.troops+b.troops==troops&&a.food+b.food==food&&a.acted&&!b.acted,"supply conserves resources and only spends sender");
        rejected(w,()->w.supply.transfer(a.id,b.id,1000,1000));refresh(w);
        rejected(w,()->w.supply.transfer(a.id,b.id,5000,1000));
        rejected(w,()->w.supply.transfer(a.id,b.id,-1,1000));
        b.hex=new Hex(2,1);int cityTroops=w.city(0).troops,gear=w.city(0).equipment[0];food=w.city(0).food+b.food;troops=cityTroops+b.troops;
        ok(w.supply.replenish(0,0,b.id,2000,5000));
        check(w.city(0).troops+b.troops==troops&&w.city(0).food+b.food==food&&w.city(0).equipment[0]==gear-2000,"city supplies physical soldiers, food and matching equipment");
        copy(w);refresh(w);rejected(w,()->w.supply.replenish(0,0,b.id,15000,1000));
        b.ship=Army.Ship.TOWER_SHIP;a.hex=new Hex(3,1);rejected(w,()->w.supply.transfer(a.id,b.id,1000,1000));ok(w.supply.transfer(a.id,b.id,0,1000));
    }
    static void raids()throws Exception{
        World w=world();w.active=1;ok(w.domestic.transport(2,3,7,1000,5000,5000,new int[]{1000,0,0,0}));w.active=0;
        Domestic.Mission m=w.domestic.missions.get(0);m.hex=new Hex(5,4);World.Unit a=unit(w,1,0,new Hex(4,4),5000);
        byte[] before=bytes(w);int expected=w.supply.raidDamage(a.id,m.id);check(Arrays.equals(before,bytes(w)),"raid preview leaves RNG unchanged");
        World direct=copy(w);ok(direct.attack(a.id,m.id));int cargoTroops=m.troops;ok(w.supply.raid(a.id,m.id));check(Arrays.equals(bytes(w),bytes(direct))&&m.troops<cargoTroops&&w.domestic.mission(m.id)!=null,"raid alias exactly matches actual battle, surviving transport retains cargo");
        rejected(w,()->w.supply.raid(a.id,m.id));refresh(w);m.troops=1;w.officer(1).skillId=Skill.BOFU.id;
        int food=a.food,cityGold=w.city(0).gold;ok(w.supply.raid(a.id,m.id));
        check(w.domestic.mission(m.id)==null&&w.government.captive(7)&&a.food==food+5000,"defeat captures courier and loots available food");
        check(w.city(0).gold==cityGold,"captured cargo does not teleport into treasury");refresh(w);
        rejected(w,()->w.supply.raid(a.id,m.id));copy(w);
        World guarded=world();guarded.active=1;ok(guarded.domestic.transport(2,3,7,0,5000,1000,new int[4]));guarded.active=0;
        Domestic.Mission protectedMission=guarded.domestic.missions.get(0);World.Unit attacker=unit(guarded,1,0,new Hex(13,1),5000);
        rejected(guarded,()->guarded.supply.raid(attacker.id,protectedMission.id));protectedMission.hex=new Hex(12,1);
        guarded.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,6));
        rejected(guarded,()->guarded.supply.raid(attacker.id,protectedMission.id));
        guarded.campaign.treaties.clear();check(guarded.supply.aiRaid(attacker)&&attacker.acted,"AI uses same raid command");
    }
    static World river(){
        World w=world();for(int q=0;q<w.width;q++)for(int r=2;r<=8;r++)w.terrain[q][r]=World.Terrain.WATER;
        for(World.City c:w.cities)w.terrain[c.hex.q][c.hex.r]=World.Terrain.PLAIN;
        return w;
    }
    static void water()throws Exception{
        World w=river();rejected(w,()->w.domestic.transport(0,1,1,1000,5000,1000,new int[4]));
        int total=w.city(0).gold+w.city(1).gold;ok(w.domestic.transportSea(0,1,1,1000,5000,1000,new int[4]));
        Domestic.Mission m=w.domestic.missions.get(0);check(m.sea&&w.domestic.eta(m)>0,"water transport has real route and ETA");
        World clone=copy(w);w.domestic.tick();clone.domestic.tick();check(Arrays.equals(bytes(w),bytes(clone)),"water route advances identically after save");
        check(w.army.water(m.hex),"cargo occupies actual water tile");copy(w);
        for(int i=0;i<6&&!w.domestic.missions.isEmpty();i++){w.turn++;w.domestic.tick();}
        check(w.domestic.missions.isEmpty()&&w.officer(1).cityId==1,"water convoy arrives and unlocks courier");
        check(w.city(0).gold+w.city(1).gold==total,"water cargo gold conserved without dispatch fee (manual p38)");
        byte[] arrived=bytes(w);w.domestic.tick();check(Arrays.equals(arrived,bytes(w)),"no repeated delivery after arrival");
        World blocked=world();ok(blocked.domestic.transport(0,1,1,0,5000,1000,new int[4]));Domestic.Mission convoy=blocked.domestic.missions.get(0);
        Hex next=blocked.domestic.route(convoy.hex,blocked.city(1).hex,0).get(0);unit(blocked,7,1,next,1000);
        blocked.domestic.tick();check(!convoy.hex.equals(next)&&convoy.hex.distance(blocked.city(0).hex)<=4,"convoy obeys movement budget and cannot occupy enemy tile; legal detour allowed");
    }
}
