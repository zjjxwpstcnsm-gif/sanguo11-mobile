package game.sanguo.core;

import java.util.*;
import static game.sanguo.core.Skill.*;

/** Explicit campaign rule hooks. See docs/RULES_V0_8.md for evidence and remaining approximations. */
public final class Skills {
    private final World w;
    Skills(World w){this.w=w;}
    public boolean has(World.Officer o,Skill s){return o!=null&&s.id.equals(o.skillId);}
    public boolean has(World.Unit u,Skill s){if(u!=null)for(World.Officer o:w.army.crew(u))if(has(o,s))return true;return false;}
    public boolean city(int city,Skill s){World.City c=w.city(city);if(c!=null)for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==city&&o.unitId<0&&has(o,s))return true;return false;}
    public int holderStat(World.Unit u,Skill s,boolean intelligence){int best=-1;if(u!=null)for(World.Officer o:w.army.crew(u))if(has(o,s))best=Math.max(best,intelligence?o.intelligence:o.war);return best;}
    public int plotCost(World.Unit u,War.Plot plot){return has(u,BAICHU)?1:plot.energy;}
    public int plotRange(World.Unit u,War.Plot plot){return (plot==War.Plot.FIRE?(w.campaign.has(u.owner,Campaign.Tech.FIRE_MASTERY)?3:1):plot==War.Plot.AMBUSH?1:2)+(has(u,GUIMOU)?1:0);}
    public boolean plotImmune(World.Unit source,World.Unit target,War.Plot plot){
        if(target==null||plot==War.Plot.CALM||plot==War.Plot.EXTINGUISH)return false;
        if(has(target,DONGCHA))return true;
        if(plot==War.Plot.CONFUSE&&(has(target,CHENZHUO)||has(target,MINGJING)))return true;
        if(plot==War.Plot.MISLEAD&&(has(target,GUILV)||has(target,MINGJING)))return true;
        if(plot==War.Plot.FIRE&&has(target,HUOSHEN))return true;
        int attack=w.army.intelligence(source);
        return holderStat(target,KANPO,true)>attack||holderStat(target,SHENSUAN,true)>attack;
    }
    public int plotChance(World.Unit a,World.Unit b,War.Plot p,int base){
        if(p==War.Plot.CALM||p==War.Plot.EXTINGUISH)return base;
        if(plotImmune(a,b,p))return 0; // Defensive immunities precede guaranteed-hit skills.
        int defense=b==null?50:w.army.intelligence(b);
        if(b!=null&&(holderStat(a,XUSHI,true)>defense||holderStat(a,SHENSUAN,true)>defense||
            p==War.Plot.CONFUSE&&holderStat(a,JILUE,true)>defense||
            p==War.Plot.INFIGHT&&holderStat(a,GUIJI,true)>defense||
            p==War.Plot.MISLEAD&&holderStat(a,YANDU,true)>defense||
            p==War.Plot.FIRE&&(holderStat(a,HUOGONG,true)>defense||holderStat(a,HUOSHEN,true)>defense)))return 100;
        // Unknown sex is not evidence that a formation is all male.
        if(b!=null&&has(a,QINGGUO)&&w.army.crew(b).stream().allMatch(o->o.sex==World.Sex.MALE))base=Math.min(100,base*2);
        return base;
    }
    public boolean plotCritical(World.Unit a,World.Unit b,War.Plot p){
        int defense=b==null?50:w.army.intelligence(b);
        if(has(a,SHENMOU)||has(a,SHENSUAN)||p==War.Plot.AMBUSH&&has(a,DAIFU))return true;
        if(holderStat(a,MIAOJI,true)>defense)return true;
        if(a!=null)for(World.Officer o:w.army.crew(a))if(has(o,MIJI)&&o.intelligence<defense)return true;
        return false;
    }
    public boolean nullifyNormal(World.Unit target,int damage,Random rng){
        return ((target.troops<3000&&has(target,BUQU))||(damage<500&&has(target,JINGANG)))&&rng.nextInt(100)<50;
    }
    public boolean avoidCounter(World.Unit a,Random rng){return has(a,w.army.water(a.hex)?QIANGXI:JIXI)&&rng.nextInt(100)<50;}
    public boolean swiftConfusion(World.Unit source,World.Unit target){
        return has(source,JICHI)&&w.army.attackPower(source)>w.army.attackPower(target);
    }
    public int produceAmount(int city,int officer,World.Weapon weapon){
        return w.domestic.produceAmount(city,weapon);
    }
    public int productionGold(int officer,World.Weapon weapon){
        World.Officer o=w.officer(officer);int base=Army.productionGold(weapon);
        boolean discount=weapon==World.Weapon.CAVALRY&&has(o,FANZHI)||weapon!=null&&weapon.ordinal()<3&&has(o,NENGLI);
        // Cost reduction is documented; 50% remains an explicitly provisional coefficient.
        return discount?base/2:base;
    }
    public int productionTurns(int officer,World.Weapon weapon){return has(w.officer(officer),weapon==null?ZAOCHUAN:FAMING)?2:3;}
    public int researchGold(int officer,Campaign.Tech tech){return has(w.officer(officer),ZHIDAO)?tech.gold/2:tech.gold;}
    public int movementBonus(World.Unit u){return movementBonusAt(u,u.hex);}
    int movementBonusAt(World.Unit u,Hex h){
        if(w.army.water(h))return has(u,CAODUO)?1:0;
        if(u.weapon==World.Weapon.CAVALRY)return has(u,CHANGQU)?1:0;
        return u.weapon.ordinal()<3&&has(u,QIANGXING)?1:0;
    }
    void woundAfterDisplacement(World.Unit source,World.Unit target){
        if(target==null||w.unit(target.id)==null||!has(source,MENGZHE)||w.strategy.nextInt(100)>=50)return;
        List<World.Officer> crew=w.army.crew(target);World.Officer victim=crew.get(w.strategy.nextInt(crew.size()));
        if(has(victim,QIANGYUN))return;
        for(World.Officer guard:crew)if(guard.id!=victim.id&&has(guard,HUWEI))return;
        w.contests.injuries.put(victim.id,new Contests.Injury(Math.min(3,w.contests.injury(victim.id)+1),w.turn+3));
        w.note(victim.name+"受到猛者战法影响而负伤");
    }

}
