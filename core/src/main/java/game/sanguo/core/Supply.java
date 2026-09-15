package game.sanguo.core;

import java.util.*;

/** Tactical interactions with strategic cargo. Failed commands do not spend resources, actions or RNG. */
public final class Supply {
    private final World w;
    Supply(World w){this.w=w;}
    public String raidError(int actor,int mission){
        World.Unit u=w.unit(actor);Domestic.Mission m=w.domestic.mission(mission);String error=w.orders.error(u);if(error!=null)return error;
        if(m==null||!m.transport||!w.campaign.hostile(u.owner,m.owner))return "请选择交战势力的运输队";
        if(w.cityAt(m.hex)!=null)return "城内运输队受城防保护";
        int distance=u.hex.distance(m.hex);if(distance>w.war.range(u))return "运输队不在攻击范围内";
        if(!w.army.canAttackUnit(u))return "当前兵器不能截击运输队";
        return null;
    }
    public int raidDamage(int actor,int mission){
        World.Unit u=w.unit(actor);Domestic.Mission m=w.domestic.mission(mission);if(u==null||m==null)return 0;
        return Math.min(m.troops,Math.max(200,u.troops/5+w.army.war(u)*5-w.officer(m.officerId).leadership*2));
    }
    public World.Result raid(int actor,int mission){
        String error=raidError(actor,mission);if(error!=null)return w.fail(error);
        World.Unit u=w.unit(actor);Domestic.Mission m=w.domestic.mission(mission);int loss=raidDamage(actor,mission);
        u.acted=true;m.troops-=loss;w.battleImpact(m.hex,m.troops==0);w.government.earn(u.officerId,Math.max(20,loss/10));
        if(m.troops>0)return w.success("截击运输队，护送兵损失"+loss+"，货物仍在途");
        int food=Math.min(m.food,1000000-u.food),gold=Math.min(m.gold,Math.max(0,10000-u.gold));u.food+=food;u.gold+=gold;
        World.Officer courier=w.officer(m.officerId);World.City jail=w.government.refuge(u.owner,m.hex);
        w.domestic.missions.remove(m);
        boolean caught=jail!=null&&!w.skills.has(courier,Skill.QIANGYUN)&&!w.skills.has(courier,Skill.XUELU)&&!w.contests.profile(courier.id).has(Contests.Gear.HORSE)&&
            (w.skills.has(u,Skill.BOFU)||w.strategy.nextInt(100)<30);
        if(caught)w.government.capture(courier,jail);else w.retreat(courier,m.hex);
        w.campaign.earn(u.owner,40);w.checkVictory();
        w.battleOutcome("运输队被击破；缴获 金+"+gold+"、粮+"+food+"，加入攻击部队；俘虏："+(caught?courier.name+"（"+jail.name+"）":"无，"+courier.name+"逃脱")+"；其余货物损失");
        return w.success("运输队溃败，缴获"+food+"粮、"+gold+"金");
    }
    public World.Result transfer(int actor,int target,int troops,int food){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=w.orders.error(a);if(error!=null)return w.fail(error);
        if(b==null||b.id==a.id||b.owner!=a.owner||a.hex.distance(b.hex)!=1)return w.fail("请选择相邻己方部队");
        if(troops<0||food<0||troops>18000||food>1000000||troops+food==0)return w.fail("请指定有效兵粮数量");
        if(troops>0&&(a.weapon!=b.weapon||a.ship!=b.ship))return w.fail("移交士兵需要相同兵装和舰船");
        if(a.troops<=troops||a.food<food||b.troops>w.government.commandLimit(b.officerId)-troops||b.food>1000000-food)return w.fail("来源兵粮不足或接收部队超过统兵/携粮上限");
        a.troops-=troops;b.troops+=troops;a.food-=food;b.food+=food;a.acted=true;
        return w.success("向"+w.officer(b.officerId).name+"移交"+troops+"兵、"+food+"粮");
    }
    public World.Result replenish(int city,int officer,int target,int troops,int food){
        World.City c=w.city(city);World.Officer o=w.officer(officer);World.Unit u=w.unit(target);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        if(u==null||u.owner!=c.owner||c.hex.distance(u.hex)>1)return w.fail("请选择城池相邻的己方部队");
        if(troops<0||troops>18000||food<0||food>1000000||troops+food==0)return w.fail("请指定有效兵粮数量");
        int equipment=Army.siegeWeapon(u.weapon)?0:Army.equipmentNeeded(u.weapon,troops);
        if(c.troops<troops||c.food<food||c.equipment[u.weapon.ordinal()]<equipment||u.troops>w.government.commandLimit(u.officerId)-troops||u.food>1000000-food)return w.fail("城内兵粮/兵装不足或部队超过容量");
        w.spend(c,o,0);c.troops-=troops;c.food-=food;c.equipment[u.weapon.ordinal()]-=equipment;u.troops+=troops;u.food+=food;
        return w.success(c.name+"补给"+w.officer(u.officerId).name+"："+troops+"兵、"+food+"粮");
    }
    boolean aiRaid(World.Unit u){for(Domestic.Mission m:new ArrayList<>(w.domestic.missions))if(raidError(u.id,m.id)==null){raid(u.id,m.id);return true;}return false;}
}
