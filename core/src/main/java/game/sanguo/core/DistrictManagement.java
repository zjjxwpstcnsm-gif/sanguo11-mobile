package game.sanguo.core;

import java.util.*;

/** Logistics consumes real stock, officers, AP and travel time. Thresholds are mobile AI policy. */
public final class DistrictManagement {
    private final World w;private final CampaignAi ai;
    public DistrictManagement(World w){this.w=w;ai=new CampaignAi(w);}
    public int residents(World.City c){int n=0;for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id&&!w.government.captive(o.id))n++;return n;}
    public int arriving(World.City c){int n=0;for(Domestic.Mission m:w.domestic.missions)if(m.owner==c.owner&&m.targetCity==c.id)n++;return n;}
    public int foodTurns(World.City c){return c.troops==0?999:Math.min(999,c.food/Math.max(1,(c.troops+49)/50));}
    public String reason(World.City c){List<String> reasons=new ArrayList<>();Districts.District d=w.districts.city(c.id);
        if(ai.incoming(c)>0)reasons.add("敌军逼近"+ai.incoming(c));
        if(foodTurns(c)<6)reasons.add("缺粮：约"+foodTurns(c)+"旬");
        if(residents(c)==0)reasons.add(arriving(c)>0?"缺将，人才在途":"缺将，等待调入");
        if(c.order<65)reasons.add("治安恶化"+c.order);
        if(d!=null){if(c.gold<=d.reserveGold)reasons.add("金保留量限制外运");if(d.points<10)reasons.add("军团预算不足");if(!d.transfer&&residents(c)==0)reasons.add("调将权限关闭");}
        return String.join("；",reasons);
    }
    void balance(Districts.District d){
        List<World.City> targets=new ArrayList<>();for(int id:d.cities)targets.add(w.city(id));
        targets.sort(Comparator.comparingInt(this::residents).thenComparingInt(c->c.id));
        for(World.City target:targets){if(residents(target)+arriving(target)>=3||w.actionPoints[d.owner]<10)continue;
            World.Officer chosen=null;World.City source=null;int best=Integer.MIN_VALUE;
            for(int id:d.cities){World.City c=w.city(id);if(id==target.id||residents(c)<=3||ai.incoming(c)>0)continue;
                for(World.Officer o:w.idle(c))if(o.role!=Strategy.Role.RULER&&o.role!=Strategy.Role.GOVERNOR){
                    int score=o.politics*2+o.charm-c.hex.distance(target.hex)*2;
                    if(score>best){List<Hex> path=w.domestic.route(c.hex,target.hex,c.owner);if(path!=null){best=score;chosen=o;source=c;}}
                }
            }
            if(chosen!=null&&w.domestic.transfer(source.id,target.id,chosen.id).ok)d.report+=source.name+"→"+target.name+"：调将"+chosen.name+"已出发\n";
        }
    }
    boolean supply(Districts.District d,World.City source,World.Officer officer){
        if(!d.supplyEnabled||ai.incoming(source)>0||residents(source)<=2)return false;
        List<World.City> targets=new ArrayList<>();for(World.City c:w.cities)if(c.owner==source.owner&&c.id!=source.id&&(d.supply>=0?c.id==d.supply:d.cities.contains(c.id)&&(ai.incoming(c)>0||foodTurns(c)<12)))targets.add(c);
        targets.sort(Comparator.comparingInt((World.City c)->-ai.incoming(c)).thenComparingInt(this::foodTurns).thenComparingInt(c->c.id));
        for(World.City target:targets){
            int gold=target.gold,food=target.food,troops=target.troops;int[] equipment=target.equipment.clone();
            for(Domestic.Mission m:w.domestic.missions)if(m.owner==source.owner&&m.targetCity==target.id&&m.transport){gold+=m.gold;food+=m.food;troops+=m.troops;for(int i=0;i<equipment.length;i++)equipment[i]+=m.equipment[i];}
            int keep=Math.max(d.reserveTroops,ai.reserve(source));
            int sendTroops=Math.min(8000,Math.min(Math.max(0,source.troops-keep),Math.max(0,Math.min(w.campaign.troopCap(target),d.supply>=0?w.campaign.troopCap(target):Math.max(16000,ai.incoming(target)))-troops)));
            int sendGold=Math.min(3000,Math.min(Math.max(0,source.gold-d.reserveGold-100),Math.max(0,Math.min(w.campaign.goldCap(target),d.supply>=0?w.campaign.goldCap(target):8000)-gold)));
            int sendFood=Math.min(20000,Math.min(Math.max(0,source.food-d.reserveFood),Math.max(0,Math.min(w.campaign.foodCap(target),d.supply>=0?w.campaign.foodCap(target):60000)-food)));
            int[] cargo=new int[equipment.length];for(World.Weapon weapon:World.Weapon.values())if(weapon!=World.Weapon.SWORD){int i=weapon.ordinal(),desired=Army.siegeWeapon(weapon)?1:8000;
                cargo[i]=Math.min(Army.siegeWeapon(weapon)?1:6000,Math.min(Math.max(0,source.equipment[i]-(Army.siegeWeapon(weapon)?1:6000)),Math.max(0,Math.min(w.campaign.equipmentCap(target,weapon),desired)-equipment[i])));}
            if(sendTroops+sendGold+sendFood+Arrays.stream(cargo).sum()==0)continue;
            if(sendTroops<1000){int escort=Math.min(1000,Math.min(source.troops-keep,w.campaign.troopCap(target)-troops));if(escort<1000)continue;sendTroops=escort;}
            World.Result result=w.domestic.transport(source.id,target.id,officer.id,sendGold,sendFood,sendTroops,cargo);if(result.ok)return true;
            if(d.report.length()<4500)d.report+=source.name+"→"+target.name+"："+result.message+"\n";
        }return false;
    }
    public String cargo(Districts.District d){StringBuilder b=new StringBuilder();for(Domestic.Mission m:w.domestic.missions)if(m.owner==d.owner&&(d.cities.contains(m.sourceCity)||d.cities.contains(m.targetCity))){
        b.append(w.officer(m.officerId).name).append(m.transport?"运输":"调将").append("→").append(w.city(m.targetCity).name);
        if(m.transport)b.append(" 金").append(m.gold).append(" 粮").append(m.food).append(" 兵").append(m.troops).append(" 兵装").append(Arrays.stream(m.equipment).sum());
        b.append(" · ").append(w.domestic.status(m)).append('\n');}return b.length()==0?"无在途任务":b.toString();}
}
