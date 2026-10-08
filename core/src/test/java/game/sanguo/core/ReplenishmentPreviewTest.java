package game.sanguo.core;
import java.util.*;
import static game.sanguo.core.GovernmentTest.*;
public final class ReplenishmentPreviewTest {
    static World fixture(){World w=world();unit(w,1,0,new Hex(2,1),3000);w.city(0).food=3000;return w;}
    static World weapon(World.Weapon weapon){World w=fixture();w.units.clear();w.units.add(new World.Unit(1,0,1,weapon,new Hex(2,1),3000,20000));w.city(0).equipment[weapon.ordinal()]=0;return w;}
    static ReplenishmentPlan quote(World w,int troops,int food)throws Exception{
        byte[] before=bytes(w);ReplenishmentPlan p=w.supply.replenishPlan(0,0,1,troops,food);
        check(Arrays.equals(before,bytes(w)),"quote preserves whole World and RNG");return p;
    }
    static void failure(World w,int troops,int food,String code)throws Exception{
        ReplenishmentPlan p=quote(w,troops,food);check(!p.allowed()&&p.failure.code.equals(code),"specific failure "+code);
        byte[] before=bytes(w);World.Result result=w.supply.replenish(0,0,1,troops,food);
        check(!result.ok&&result.message.equals(p.failure.detail),"preview/execution exact rejection");check(Arrays.equals(before,bytes(w)),"failure pure");
    }
    public static void main(String[] args)throws Exception{
        World w=fixture();failure(w,0,5000,"CITY_FOOD");failure(w,3000,5000,"CITY_FOOD");
        failure(w,-1,1000,"SUPPLY_AMOUNT");failure(w,0,0,"SUPPLY_AMOUNT");failure(w,0,1000001,"SUPPLY_AMOUNT");
        w.city(0).equipment[0]=0;failure(w,1000,1000,"CITY_EQUIPMENT");
        check(quote(weapon(World.Weapon.SWORD),1000,1000).allowed(),"sword costs no equipment");
        check(quote(weapon(World.Weapon.RAM),1000,1000).allowed(),"existing siege unit costs no extra engine");
        w.city(0).equipment[0]=50000;
        w.unit(1).troops=w.government.commandLimit(1)+1;check(quote(w,0,1000).allowed(),"old overcap formation can receive food only");failure(w,1,1000,"UNIT_TROOP_CAPACITY");
        w.unit(1).troops=3000;w.unit(1).food=999500;failure(w,0,1000,"UNIT_FOOD_CAPACITY");w.unit(1).food=20000;
        w.city(0).troops=500;failure(w,1000,1000,"CITY_TROOPS");w.city(0).troops=50000;
        w.actionPoints[0]=9;failure(w,0,1000,"ACTION_POINTS");w.actionPoints[0]=60;
        w.officer(0).acted=true;failure(w,0,1000,"LEADER_UNAVAILABLE");w.officer(0).acted=false;
        w.unit(1).hex=new Hex(4,4);failure(w,0,1000,"SUPPLY_POSITION");w.unit(1).hex=new Hex(2,1);
        ReplenishmentPlan p=quote(w,1000,1000);check(p.allowed()&&p.equipmentCost==1000&&p.actionPointsCost==10,"exact allowed costs");
        World hot=copy(w),cold=copy(hot);ok(hot.supply.replenish(0,0,1,1000,1000));ok(cold.supply.replenish(0,0,1,1000,1000));check(Arrays.equals(bytes(hot),bytes(cold)),"canonical hot/cold execution identical whole World");
        ok(w.supply.replenish(0,0,1,1000,1000));
        check(w.city(0).troops==49000&&w.city(0).food==2000&&w.city(0).equipment[0]==49000&&w.unit(1).troops==4000&&w.unit(1).food==21000&&w.actionPoints[0]==50&&w.officer(0).acted&&!w.unit(1).acted,"actual conservation and correct actor");
        failure(w,1000,1000,"LEADER_UNAVAILABLE");copy(w);
        System.out.println("PASS "+checks+" replenishment preview assertions; current numerical rules retained, PC calibration separate");
    }
}
