package game.sanguo.core;

import java.util.*;

/** Mobile balance rules, not an assertion of the original game's damage formula. */
public final class CityDefense {
    private final World w;
    CityDefense(World w){this.w=w;}
    public int range(World.City city){return city.kind==World.SiteKind.CITY?2:1;}
    private boolean ready(World.City c){return c!=null&&c.owner>=0&&c.troops>0&&c.food>0&&c.defense>0;}
    public boolean inRange(World.City c,World.Unit u){return ready(c)&&u!=null&&w.campaign.hostile(c.owner,u.owner)&&c.hex.distance(u.hex)>0&&c.hex.distance(u.hex)<=range(c);}
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
    public String describe(World.City c){return "守备射程 "+range(c)+" 格 · 每旬对范围内敌军自动射击\n单队基础伤害 ≤"+strength(c)+"，全城每旬总量 ≤"+(strength(c)*2)+"；反击约为单队伤害的⅔。缺粮或无守军停射。";}
    private int hurt(World.City c,World.Unit u,int damage,String reason){
        if(w.unit(u.id)!=u||damage<=0)return 0;
        int actual=Math.min(u.troops,damage);u.troops-=actual;
        w.battleImpact(u.hex,u.troops==0);
        w.note(c.name+reason+"，"+w.officer(u.officerId).name+"部队损失"+actual);
        if(u.troops==0)w.removeUnit(u); // Shared crew, cargo and prisoner cleanup, exactly once.
        return actual;
    }
    int counter(World.City c,World.Unit u){return hurt(c,u,counterDamage(c,u),"反击");}
    /** Called exactly once at the end of a completed global turn. No action/reset hook. */
    void tick(){
        for(World.City c:w.cities){
            int power=strength(c);if(power==0)continue;
            List<World.Unit> enemies=new ArrayList<>();for(World.Unit u:w.fieldUnits())if(inRange(c,u))enemies.add(u);
            enemies.sort(Comparator.comparingInt(u->u.id));if(enemies.isEmpty())continue;
            int budget=power*2,share=Math.min(power,budget/enemies.size()),extra=budget%enemies.size();
            for(int i=0;i<enemies.size();i++){
                World.Unit u=enemies.get(i);int allocated=Math.min(power,share+(i<extra?1:0));
                hurt(c,u,Math.min(allocated,against(u,allocated)),"自动射击");
            }
        }
    }
}
