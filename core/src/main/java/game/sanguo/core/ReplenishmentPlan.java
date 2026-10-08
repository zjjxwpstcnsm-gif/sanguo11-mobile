package game.sanguo.core;

/** Immutable current-rule supply quote. Numerical PC supply parity is separate. */
public final class ReplenishmentPlan {
    public final RuleFailure failure;
    public final int cityId,officerId,unitId,troopsCost,foodCost,equipmentCost;
    public final int stockTroops,stockFood,stockEquipment,unitTroops,unitFood,commandLimit;
    public final int actionPointsAvailable,actionPointsCost;
    public final boolean nativeArmyBudget,reserveEnforced;
    public final String equipmentLabel;
    ReplenishmentPlan(World w,int city,int officer,int target,int troops,int food){
        cityId=city;officerId=officer;unitId=target;troopsCost=troops;foodCost=food;
        World.City c=w.city(city);World.Officer o=w.officer(officer);World.Unit u=w.unit(target);
        stockTroops=c==null?0:c.troops;stockFood=c==null?0:c.food;
        stockEquipment=c==null||u==null?0:c.equipment[u.weapon.ordinal()];
        equipmentCost=u==null?0:Army.siegeWeapon(u.weapon)?0:Army.equipmentNeeded(u.weapon,troops);
        equipmentLabel=u==null?"":u.weapon.label;
        unitTroops=u==null?0:u.troops;unitFood=u==null?0:u.food;
        commandLimit=u==null?0:w.government.commandLimit(u.officerId);
        actionPointsAvailable=w.cityActionPoints(c);actionPointsCost=w.cityActionCost(o);
        nativeArmyBudget=PcArmyActionPolicy.enabled(w);reserveEnforced=c!=null&&w.districts.executing(city);
        RuleFailure error=w.cityFailure(c,o,0);
        if(error==null&&reserveEnforced&&!w.districts.city(city).supplyEnabled)error=new RuleFailure("SUPPLY_DISABLED","city","军团禁止补给运输");
        if(error==null){String reserve=w.districts.reserveError(c,0,food,troops);if(reserve!=null)error=new RuleFailure("DISTRICT_RESERVE","resources",reserve);}
        if(error==null&&(u==null||u instanceof Domestic.Mission||u.owner!=c.owner||!w.army.canEnterSite(u,u.hex,c)))error=new RuleFailure("SUPPLY_POSITION","target","请选择可从当前位置进驻该据点的己方战斗部队");
        if(error==null&&(troops<0||troops>18000||food<0||food>1000000||troops+food==0))error=new RuleFailure("SUPPLY_AMOUNT","amount","请指定有效兵粮数量");
        if(error==null&&stockTroops<troops)error=new RuleFailure("CITY_TROOPS","troops","据点兵力不足：现有"+stockTroops+"，需要"+troops);
        if(error==null&&stockFood<food)error=new RuleFailure("CITY_FOOD","food","据点兵粮不足：现有"+stockFood+"，需要"+food);
        if(error==null&&stockEquipment<equipmentCost)error=new RuleFailure("CITY_EQUIPMENT","equipment",equipmentLabel+"兵装不足：现有"+stockEquipment+"，需要"+equipmentCost);
        if(error==null&&troops>0&&unitTroops>commandLimit-troops)error=new RuleFailure("UNIT_TROOP_CAPACITY","troops","超过部队统兵上限：现有"+unitTroops+"，上限"+commandLimit);
        if(error==null&&unitFood>1000000-food)error=new RuleFailure("UNIT_FOOD_CAPACITY","food","超过部队携粮上限1000000：现有"+unitFood);
        failure=error;
    }
    public boolean allowed(){return failure==null;}
}
