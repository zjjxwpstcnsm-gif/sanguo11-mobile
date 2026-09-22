package game.sanguo.core;

import java.util.*;

/** Mobile balance rules, not an assertion of the original game's damage formula. */
public final class CityDefense {
    private final World w;
    CityDefense(World w){this.w=w;}
    /** Same two exterior rings used by income, recruitment and selection overlays. */
    public boolean besieged(World.City c){return SiegeRules.blockaded(w,c);}
    public int recovery(World.City c){return recovery(c,besieged(c));}
    int recovery(World.City c,boolean blockaded){
        if(c==null||c.owner<0||c.food<=0||blockaded||c.defense>=w.campaign.defenseCap(c))return 0;
        return Math.min(w.campaign.defenseCap(c)-c.defense,w.campaign.has(c.owner,Campaign.Tech.ENGINEERING)?40:20);
    }
    public int repairAmount(World.City c,World.Officer o){
        int amount=400+o.politics*4;
        if(w.campaign.has(c.owner,Campaign.Tech.ENGINEERING))amount=amount*3/2;
        if(besieged(c))amount/=4;
        return Math.max(0,Math.min(w.campaign.defenseCap(c)-c.defense,amount));
    }
    public int range(World.City city){return SiegeRules.RANGE;}
    private boolean ready(World.City c){return c!=null&&c.owner>=0&&c.troops>0&&c.food>0&&c.defense>0;}
    public boolean inRange(World.City c,World.Unit u){return ready(c)&&SiegeRules.hostile(w,c,u);}
    /** Bounded output scales with remaining garrison, morale and wall condition. */
    public int strength(World.City c){
        if(!ready(c))return 0;
        int wall=Math.min(100,c.defense*100/Math.max(1,w.campaign.defenseCap(c)));
        int amount=(40+Math.min(40000,c.troops)/100)*(40+c.morale)/140*(50+wall)/150;
        if(c.kind!=World.SiteKind.CITY)amount=amount*3/4;
        if(w.campaign.has(c.owner,Campaign.Tech.DEFENSE_REINFORCEMENT))amount=amount*5/4;
        return Math.max(1,Math.min(c.kind==World.SiteKind.CITY?360:240,amount));
    }
    private int against(World.Unit u,int amount){
        World.Officer o=w.officer(u.officerId);
        return Math.max(1,amount*150/(100+(o==null?50:o.leadership)));
    }
    public int counterDamage(World.City c,World.Unit attacker){return inRange(c,attacker)?Math.min(attacker.troops,against(attacker,strength(c)*2/3)):0;}
    public String preview(World.City c,World.Unit u){int n=counterDamage(c,u);return n>0?"据点存续时反击：至多 "+n+" 兵（随剩余守军降低）；防御射程 "+range(c)+" 格。":"当前距离或守备状态下无据点反击。";}
    public String describe(World.City c){return SiegeRules.description(w,c)+"\n守备射程：占地外"+range(c)+"圈。每旬自动射击与被动反击共用伤害公式；多目标均摊、总伤害不超过两次最高单体反击。守军、气力、耐久、守备科技与目标统率影响伤害；缺粮、无守军或城防归零时停射。";}
    private int hurt(World.City c,World.Unit u,int damage,boolean counter){
        if(w.unit(u.id)!=u||damage<=0)return 0;
        String reason=counter?"反击":"自动射击",label=c.name+reason;
        TurnJournal.Kind kind=counter?TurnJournal.Kind.FACILITY_COUNTER:TurnJournal.Kind.FACILITY_ATTACK;
        BattleReports.ActionContext context=w.reports.beginSite(c,u.hex,kind,label);
        if(w.turnJournal!=null)w.turnJournal.site(c,u.hex,kind,label);
        World.Officer officer=w.officer(u.officerId);
        int actual=w.combatEffects.hit(null,u,damage,false,false);
        w.battleImpact(u.hex,u.troops==0);
        String message=label+"，"+(officer==null?"敌军":officer.name)+"部队损失"+actual;
        // Preserve the parent attack's report context during a nested city counterattack.
        w.reports.finishCounter(context,message);
        w.log.add(message);while(w.log.size()>40)w.log.remove(0);
        if(w.turnJournal!=null)w.turnJournal.checkpoint(message);
        return actual;
    }
    int counter(World.City c,World.Unit u){return hurt(c,u,counterDamage(c,u),true);}
    /** Exactly one global-turn volley. Stable rotation avoids permanent low-ID favoritism. */
    void tick(){
        for(World.City c:w.cities){
            if(!ready(c))continue;
            List<World.Unit> enemies=SiegeRules.defendersTargets(w,c);if(enemies.isEmpty())continue;
            Collections.rotate(enemies,-Math.floorMod(w.turn,enemies.size()));
            int peak=0;for(World.Unit u:enemies)peak=Math.max(peak,counterDamage(c,u));
            int budget=peak*2,share=budget/enemies.size(),extra=budget%enemies.size();
            for(int i=0;i<enemies.size();i++){
                World.Unit u=enemies.get(i);int damage=Math.min(counterDamage(c,u),share+(i<extra?1:0));
                hurt(c,u,damage,false);
            }
        }
    }
}
