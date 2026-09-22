package game.sanguo.core;

import java.util.*;

/** Tactical interactions with strategic cargo. Failed commands do not spend resources, actions or RNG.
 * Legacy over-command formations may receive food/gold, but never additional soldiers. */
public final class Supply {
    private final World w;
    Supply(World w){this.w=w;}
    public String raidError(int actor,int mission){Domestic.Mission m=w.domestic.mission(mission);return m==null||!m.transport?"请选择运输队":w.war.attackError(actor,m.id);}
    public int raidDamage(int actor,int mission){Domestic.Mission m=w.domestic.mission(mission);return m==null?0:w.war.previewDamage(actor,m.id);}
    public World.Result raid(int actor,int mission){w.reports.prepare();String error=raidError(actor,mission);return error==null?w.attack(actor,w.domestic.mission(mission).id):w.fail(error);}
    /** Official manual permits convoy-to-unit soldiers, gold and food. Matching weapon cargo equips supplied soldiers; this conversion is provisional. */
    public World.Result convoyTransfer(int actor,int target,int troops,int food,int gold){w.reports.prepare();
        World.Unit a=w.unit(actor),b=w.unit(target);String error=w.orders.error(a);if(error!=null)return w.fail(error);
        if(!(a instanceof Domestic.Mission)||b==null||b instanceof Domestic.Mission||b.owner!=a.owner||a.hex.distance(b.hex)!=1)return w.fail("请选择相邻己方野战部队");
        if(troops<0||food<0||gold<0||troops+food+gold==0||troops>=a.troops||food>a.food||gold>a.gold||(troops>0&&troops>w.government.commandLimit(b.officerId)-b.troops)||food>1000000-b.food||gold>10000-b.gold)return w.fail("补给数量不足或超过接收容量；运输至少保留1兵");
        int gear=Army.siegeWeapon(b.weapon)?0:Army.equipmentNeeded(b.weapon,troops);Domestic.Mission cargo=(Domestic.Mission)a;if(cargo.equipment[b.weapon.ordinal()]<gear)return w.fail("运输队缺少接收兵种的兵装");cargo.equipment[b.weapon.ordinal()]-=gear;
        a.troops-=troops;a.food-=food;a.gold-=gold;b.troops+=troops;b.food+=food;b.gold+=gold;a.acted=true;
        return w.success("运输队补给"+w.officer(b.officerId).name+"：兵"+troops+" / 粮"+food+" / 金"+gold);
    }
    public World.Result transfer(int actor,int target,int troops,int food){w.reports.prepare();
        World.Unit a=w.unit(actor),b=w.unit(target);if(a instanceof Domestic.Mission)return convoyTransfer(actor,target,troops,food,0);String error=w.orders.error(a);if(error!=null)return w.fail(error);
        if(b==null||b instanceof Domestic.Mission||b.id==a.id||b.owner!=a.owner||a.hex.distance(b.hex)!=1)return w.fail("请选择相邻己方部队");
        if(troops<0||food<0||troops>18000||food>1000000||troops+food==0)return w.fail("请指定有效兵粮数量");
        if(troops>0&&(a.weapon!=b.weapon||a.ship!=b.ship))return w.fail("移交士兵需要相同兵装和舰船");
        if(a.troops<=troops||a.food<food||(troops>0&&b.troops>w.government.commandLimit(b.officerId)-troops)||b.food>1000000-food)return w.fail("来源兵粮不足或接收部队超过统兵/携粮上限");
        a.troops-=troops;b.troops+=troops;a.food-=food;b.food+=food;a.acted=true;
        return w.success("向"+w.officer(b.officerId).name+"移交"+troops+"兵、"+food+"粮");
    }
    public World.Result replenish(int city,int officer,int target,int troops,int food){w.reports.prepare();
        World.City c=w.city(city);World.Officer o=w.officer(officer);World.Unit u=w.unit(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(w.districts.executing(city)&&!w.districts.city(city).supplyEnabled)return w.fail("军团禁止补给运输");
        if(w.districts.reserveError(c,0,food,troops)!=null)return w.fail(w.districts.reserveError(c,0,food,troops));
        if(u==null||u instanceof Domestic.Mission||u.owner!=c.owner||!w.army.canEnterSite(u,u.hex,c))return w.fail("请选择城池相邻的己方部队");
        if(troops<0||troops>18000||food<0||food>1000000||troops+food==0)return w.fail("请指定有效兵粮数量");
        int equipment=Army.siegeWeapon(u.weapon)?0:Army.equipmentNeeded(u.weapon,troops);
        if(c.troops<troops||c.food<food||c.equipment[u.weapon.ordinal()]<equipment||(troops>0&&u.troops>w.government.commandLimit(u.officerId)-troops)||u.food>1000000-food)return w.fail("城内兵粮/兵装不足或部队超过容量");
        w.spend(c,o,0);c.troops-=troops;c.food-=food;c.equipment[u.weapon.ordinal()]-=equipment;u.troops+=troops;u.food+=food;
        return w.success(c.name+"补给"+w.officer(u.officerId).name+"："+troops+"兵、"+food+"粮");
    }
    boolean aiRaid(World.Unit u){for(Domestic.Mission m:new ArrayList<>(w.domestic.missions))if(raidError(u.id,m.id)==null){raid(u.id,m.id);return true;}return false;}
}
