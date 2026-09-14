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
    public int plotRange(World.Unit u,War.Plot plot){return (plot==War.Plot.FIRE||plot==War.Plot.AMBUSH?1:2)+(has(u,GUIMOU)?1:0);}
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
        if(holderStat(a,XUSHI,true)>defense||holderStat(a,SHENSUAN,true)>defense||
            p==War.Plot.CONFUSE&&holderStat(a,JILUE,true)>defense||
            p==War.Plot.MISLEAD&&holderStat(a,YANDU,true)>defense||
            p==War.Plot.FIRE&&(holderStat(a,HUOGONG,true)>defense||holderStat(a,HUOSHEN,true)>defense))return 100;
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
    public boolean critical(World.Unit a,World.Unit b,boolean tactic){
        if(b!=null&&w.terrain[a.hex.q][a.hex.r]==World.Terrain.FOREST&&has(a,LUANZHAN))return true;
        int category=w.army.water(a.hex)?5:Army.category(a.weapon);
        if(!tactic)return b!=null&&(holderStat(a,QUZHU,false)>w.army.war(b)||holderStat(a,SHENJIANG,false)>w.army.war(b));
        if(has(a,BAWANG))return true;
        Skill god=category==0?QIANGSHEN:category==1?JISHEN:category==2?GONGSHEN:category==3?QISHEN:category==4?GONGSHEN_SIEGE:category==5?SHUISHEN:null;
        if(god!=null&&has(a,god)||category>=0&&category<=1&&has(a,DOUSHEN))return true;
        if(b==null)return has(a,GONGCHENG);
        int defense=w.army.war(b);
        if(holderStat(a,YONGJIANG,false)>defense)return true;
        if(category>=0&&category<=3&&(holderStat(a,FEIJIANG,false)>defense||holderStat(a,SHENJIANG,false)>defense))return true;
        Skill general=category==0?QIANGJIANG:category==1?JIJIANG:category==2?GONGJIANG:category==3?QIJIANG:category==5?SHUIJIANG:null;
        return general!=null&&holderStat(a,general,false)>defense;
    }
    public boolean nullifyNormal(World.Unit target,int damage,Random rng){
        return ((target.troops<3000&&has(target,BUQU))||(damage<500&&has(target,JINGANG)))&&rng.nextInt(100)<50;
    }
    public boolean avoidCounter(World.Unit a,Random rng){return has(a,w.army.water(a.hex)?QIANGXI:JIXI)&&rng.nextInt(100)<50;}
    public void onHit(World.Unit source,World.Unit target,int loss,boolean tactic){
        if(loss<=0)return;
        target.energy=Math.max(0,target.energy-(has(source,WEIFENG)?20:has(source,SAOTAO)?5:0));
        if(tactic&&has(target,NUFA)&&target.troops>0)target.energy=Math.min(100,target.energy+5);
        if(has(source,XINGONG)&&source.troops>0)source.troops=Math.min(10000,source.troops+loss/10);
        if(target.troops==0&&has(source,ANGYANG))source.energy=Math.min(100,source.energy+10);
    }
    public int produceAmount(int city,int officer,World.Weapon weapon){
        int amount=w.domestic.produceAmount(city,weapon);World.Officer o=w.officer(officer);
        return amount*(weapon==World.Weapon.CAVALRY?has(o,FANZHI)?2:1:weapon.ordinal()<3&&has(o,NENGLI)?2:1);
    }
    public int productionTurns(int officer,World.Weapon weapon){return has(w.officer(officer),weapon==null?ZAOCHUAN:FAMING)?2:3;}
    public int researchGold(int officer,Campaign.Tech tech){return has(w.officer(officer),ZHIDAO)?tech.gold/2:tech.gold;}
    public void restoreEnergy(){
        for(World.Unit u:w.units){
            boolean music=false;for(War.Structure s:w.war.structures)if(s.kind==War.StructureKind.MUSIC&&s.owner==u.owner&&s.hex.distance(u.hex)<=2){music=true;break;}
            int gain=music?(has(u,SHIXIANG)?20:10):has(u,ZOUYUE)?5:0;
            u.energy=Math.min(100,u.energy+gain);
        }
    }
}
