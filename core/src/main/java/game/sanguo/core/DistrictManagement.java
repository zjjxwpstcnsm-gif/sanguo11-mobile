package game.sanguo.core;

import java.util.*;

/** Real stock/AP/personnel only. Forecast horizons and buffers are engineering AI policy. */
public final class DistrictManagement {
    private final World w;private final CampaignAi ai;
    public DistrictManagement(World w){this.w=w;ai=new CampaignAi(w);}
    public int residents(World.City c){int n=0;for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id&&!w.government.captive(o.id))n++;return n;}
    public int arriving(World.City c){int n=0;for(Domestic.Mission m:w.domestic.missions)if(m.owner==c.owner&&m.targetCity==c.id&&(!m.returnOfficers||!m.transport))n+=m.crew().length;return n;}
    public int foodTurns(World.City c){int use=w.cityFoodUse(c);return use==0?999:Math.min(999,c.food/use);}
    public int income(World.City c,int turns,boolean food){int sum=0;for(int t=1;t<=turns;t++)sum+=food?foodIncome(c,t):w.domestic.goldIncome(c.id,w.turn+t);return sum;}
    private int foodIncome(World.City c,int offset){
        int turn=w.turn+offset,amount=w.domestic.foodIncome(c.id,turn);
        // Only count a farm after the turn on which its actual construction completes.
        boolean season=turn%9==0,tax=w.skills.city(c.id,Skill.ZHENGSHUI);
        if(season||tax&&turn%3==0)for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==c.id&&f.kind==Domestic.Kind.FARM&&f.remaining>0&&f.remaining<=offset){
            int extra=w.strategy.cityIncome(c.id,2500*(f.level==3?150:f.level==2?120:100)/100);
            if(!season)extra/=2;if(season&&w.skills.city(c.id,Skill.MIDAO))extra=extra*3/2;amount+=extra;
        }return amount;
    }
    /** Minimum reserve to bridge the scheduled harvest; user reserve is never overwritten. */
    public int keepFood(Districts.District d,World.City c){int draw=0,peak=0;for(int t=1;t<=9;t++){draw+=w.cityFoodUse(c);peak=Math.max(peak,draw);draw-=foodIncome(c,t);}return Math.max(d.reserveFood,peak+w.cityFoodUse(c)*3);}
    public int famineTurn(World.City c){
        int food=c.food,use=w.cityFoodUse(c);if(use==0)return 999;
        int[] inbound=new int[37];for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.owner==c.owner&&m.targetCity==c.id){int eta=w.domestic.deliverableEta(m);if(eta>=0&&eta<=36)inbound[Math.max(1,eta)]+=Math.max(0,m.food-eta*w.domestic.foodUse(m));}
        for(int t=1;t<=36;t++){food+=inbound[t];if(food<use)return t;food=Math.min(w.campaign.foodCap(c),food-use+foodIncome(c,t));}return 999;
    }
    public int incomingFood(World.City c){int n=0;for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.owner==c.owner&&m.targetCity==c.id)n+=m.food;return n;}
    public Set<Integer> logisticsBlockedCities(){Set<Integer> cities=new HashSet<>();for(Domestic.Mission m:w.domestic.missions){String s=w.domestic.status(m);if(s.contains("受阻")||s.contains("失守")||s.contains("满仓")||s.contains("断粮")||s.contains("截停")||s.contains("等待")||s.contains("已停止")){cities.add(m.sourceCity);cities.add(m.targetCity);}}return cities;}
    public String reason(World.City c){List<String> reasons=new ArrayList<>();Districts.District d=w.districts.city(c.id);int danger=ai.incoming(c);
        if(danger>0)reasons.add("敌军逼近"+danger);
        if(foodTurns(c)<6)reasons.add("缺粮：现粮约"+foodTurns(c)+"旬");
        if(residents(c)==0)reasons.add(arriving(c)>0?"缺将，人才在途":"缺将，等待调入");
        if(c.order<65)reasons.add("治安恶化"+c.order);
        if(d!=null){if(c.gold<=d.reserveGold)reasons.add("金保留量限制外运");if(d.points<10)reasons.add("军团预算不足");if(!d.transfer&&residents(c)==0)reasons.add("调将权限关闭");if(!d.supplyEnabled)reasons.add("补给运输权限关闭");if(residents(c)<=2)reasons.add("留守两将，等待返程或调入后再运输");}
        return String.join("；",reasons);
    }
    public String forecast(World.City c){int famine=famineTurn(c);return "未来9旬预计收入 金"+income(c,9,false)+" / 粮"+income(c,9,true)+"；驻军旬耗"+w.cityFoodUse(c)+"\n"+
        (famine==999?"预计36旬内无断粮（按现驻军/工期）":"预计第"+famine+"旬缺粮（含可达在途援助）")+"；在途携粮"+incomingFood(c);}
    void balance(Districts.District d){
        List<World.City> targets=new ArrayList<>();for(int id:d.cities)targets.add(w.city(id));
        targets.sort(Comparator.comparingInt(this::residents).thenComparingInt(c->c.id));
        for(World.City target:targets){if(residents(target)+arriving(target)>=3||w.actionPoints[d.owner]<10)continue;
            World.Officer chosen=null;World.City source=null;int best=Integer.MIN_VALUE;
            for(int id:d.cities){World.City c=w.city(id);if(id==target.id||residents(c)<=3||ai.incoming(c)>0)continue;
                World.Officer candidate=null;for(World.Officer o:w.idle(c))if(o.role!=Strategy.Role.RULER&&o.role!=Strategy.Role.GOVERNOR&&(candidate==null||o.politics*2+o.charm>candidate.politics*2+candidate.charm))candidate=o;
                if(candidate==null)continue;int score=candidate.politics*2+candidate.charm-c.hex.distance(target.hex)*2;
                if(score>best&&w.domestic.route(c.hex,target.hex,c.owner)!=null){best=score;chosen=candidate;source=c;}
            }
            if(chosen!=null&&w.domestic.transfer(source.id,target.id,chosen.id).ok)d.report+=source.name+"→"+target.name+"：调将"+chosen.name+"已出发\n";
        }
    }
    public static final class SupplyPlan {
        public final int source,target,officer,gold,food,troops;public final boolean returning;public boolean sea;public final String reason;private final int[] equipment;
        SupplyPlan(int source,int target,int officer,int gold,int food,int troops,int[] equipment,boolean returning,String reason){this.source=source;this.target=target;this.officer=officer;this.gold=gold;this.food=food;this.troops=troops;this.equipment=equipment.clone();this.returning=returning;this.reason=reason;}
        public boolean valid(){return reason==null;}
        public int[] equipment(){return equipment.clone();}
    }
    private SupplyPlan blocked(World.City source,World.City target,World.Officer o,String reason){return new SupplyPlan(source.id,target.id,o==null?-1:o.id,0,0,0,new int[World.Weapon.values().length],false,reason);}
    public SupplyPlan plan(Districts.District d,World.City source,World.City target,World.Officer officer){
        if(officer==null)return blocked(source,target,null,"无可行动的运输武将");
        if(!d.supplyEnabled)return blocked(source,target,officer,"军团禁止补给运输");
        if(d.supply>=0&&target.id!=d.supply)return blocked(source,target,officer,"限定运输目的地");
        if(d.supply<0&&!d.cities.contains(target.id))return blocked(source,target,officer,"没有跨军团支援授权");
        if(residents(source)<=2)return blocked(source,target,officer,"留守两将，等待返程/调将/登用");
        int danger=ai.incoming(source);if(danger>0&&d.supply<0)return blocked(source,target,officer,"出发城受威胁，未指定外运目的地");
        for(World.Unit enemy:w.units)if(w.campaign.hostile(source.owner,enemy.owner)&&enemy.hex.distance(source.hex)<=2)return blocked(source,target,officer,"敌军围城，暂停外运");
        int gold=target.gold,food=target.food,troops=target.troops;int[] equipment=target.equipment.clone();
        for(Domestic.Mission m:w.domestic.missions)if(m.owner==source.owner&&m.targetCity==target.id&&m.transport){gold+=m.gold;food+=m.food;troops+=m.troops;for(int i=0;i<equipment.length;i++)equipment[i]+=m.equipment[i];}
        int keep=Math.max(d.reserveTroops,Math.max(ai.reserve(source),danger+4000));
        int desiredTroops=d.supply>=0?w.campaign.troopCap(target):Math.max(16000,ai.incoming(target));
        int sendTroops=Math.min(8000,Math.min(Math.max(0,source.troops-keep),Math.max(0,Math.min(w.campaign.troopCap(target),desiredTroops)-troops)));
        int sendGold=Math.min(3000,Math.min(Math.max(0,source.gold-d.reserveGold),Math.max(0,Math.min(w.campaign.goldCap(target),d.supply>=0?w.campaign.goldCap(target):8000)-gold)));
        int desiredFood=Math.max(40000,w.cityFoodUse(target)*18+Math.max(0,ai.incoming(target))*3);
        int sendFood=Math.min(20000,Math.min(Math.max(0,source.food-keepFood(d,source)),Math.max(0,Math.min(w.campaign.foodCap(target),d.supply>=0?w.campaign.foodCap(target):desiredFood)-food)));
        int[] cargo=new int[equipment.length];for(World.Weapon weapon:World.Weapon.values())if(weapon!=World.Weapon.SWORD){int i=weapon.ordinal(),desired=Army.siegeWeapon(weapon)?1:8000;
            cargo[i]=Math.min(Army.siegeWeapon(weapon)?1:6000,Math.min(Math.max(0,source.equipment[i]-(Army.siegeWeapon(weapon)?1:6000)),Math.max(0,Math.min(w.campaign.equipmentCap(target,weapon),desired)-equipment[i])));}
        if(sendTroops+sendGold+sendFood+Arrays.stream(cargo).sum()==0)return blocked(source,target,officer,"已承诺和在途物资满足需求，或来源城达到保留线");
        if(sendTroops<1000){sendTroops=Math.min(1000,Math.min(source.troops-keep,w.campaign.troopCap(target)-troops));if(sendTroops<1000)return blocked(source,target,officer,"留守兵力或目的地护兵容量不足");}
        Domestic.Mission probe=new Domestic.Mission(0,source.owner,officer.id,source.id,target.id,source.hex,true,sendGold,sendFood,sendTroops,cargo);
        int eta=w.domestic.eta(probe);if(eta<0){probe.sea=true;eta=w.domestic.eta(probe);}if(eta<0)return blocked(source,target,officer,"道路受阻，暂无可达运输路线");
        String threat=w.domestic.threatReason(probe);if(threat!=null)return blocked(source,target,officer,threat);
        int ration=(eta+2)*w.domestic.foodUse(probe);sendFood=Math.max(sendFood,ration);
        if(sendFood>source.food-keepFood(d,source)||sendFood>w.campaign.foodCap(target)-food)return blocked(source,target,officer,"途中口粮或目的地粮仓空间不足");
        SupplyPlan result=new SupplyPlan(source.id,target.id,officer.id,sendGold,sendFood,sendTroops,cargo,!d.transfer||residents(target)+arriving(target)>=3,null);result.sea=probe.sea;return result;
    }
    World.Result send(SupplyPlan p){if(!p.valid())return w.fail(p.reason);return w.domestic.transport(p.source,p.target,p.officer,new int[0],p.gold,p.food,p.troops,p.equipment,p.sea,p.returning);}
    boolean supply(Districts.District d,World.City source,World.Officer officer){
        List<World.City> targets=new ArrayList<>();for(World.City c:w.cities)if(c.owner==source.owner&&c.id!=source.id&&(d.supply>=0?c.id==d.supply:d.cities.contains(c.id)&&(ai.incoming(c)>0||foodTurns(c)<12)))targets.add(c);
        targets.sort(Comparator.comparingInt((World.City c)->-ai.incoming(c)).thenComparingInt(this::foodTurns).thenComparingInt(c->c.id));
        for(World.City target:targets){SupplyPlan plan=plan(d,source,target,officer);if(plan.valid()){World.Result result=send(plan);if(result.ok)return true;if(d.report.length()<4000)d.report+=source.name+"→"+target.name+"："+result.message+"\n";}else if(d.report.length()<4000)d.report+=source.name+"→"+target.name+"："+plan.reason+"\n";}return false;
    }
    public String cargo(Districts.District d){StringBuilder b=new StringBuilder();for(Domestic.Mission m:w.domestic.missions)if(m.owner==d.owner&&(d.cities.contains(m.sourceCity)||d.cities.contains(m.targetCity))){
        b.append(w.officer(m.officerId).name).append(m.returning?"等将返程":m.transport?"运输":"调将").append("→").append(w.city(m.targetCity).name);
        if(m.transport)b.append(" 金").append(m.gold).append(" 粮").append(m.food).append(" 兵").append(m.troops).append(" 已耗粮").append(m.consumedFood);
        b.append(" · ").append(w.domestic.status(m)).append('\n');}return b.length()==0?"无在途任务":b.toString();}
}
