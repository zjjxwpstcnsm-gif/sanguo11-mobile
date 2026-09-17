package game.sanguo.core;

import java.util.Random;
import static game.sanguo.core.Skill.*;

/** Read-only combat calculations. Only an explicitly supplied execution RNG can be consumed. */
public final class CombatRules {
    private final World w;
    CombatRules(World w){this.w=w;}
    public static final class DamageRange {
        public final int min,max,estimate; public final boolean critical;
        DamageRange(int min,int max,int estimate,boolean critical){this.min=min;this.max=max;this.estimate=estimate;this.critical=critical;}
        public String describe(){return "物理伤害："+min+"–"+max+"（随机范围）"+(critical?" · 条件暴击×1.15（工程系数）":"");}
    }
    /** Local deterministic RNGs only; never copies World or advances strategy RNG. */
    public int expectedDamage(World.Unit a,World.Unit b,double scale,boolean tactic){
        int total=0;for(int i=0;i<8;i++)total+=physicalDamage(a,b,scale,tactic,new Random(7919L*i+17));
        return total/8;
    }
    public DamageRange preview(World.Unit a,World.Unit b,double scale,boolean tactic){
        return new DamageRange(physicalDamage(a,b,scale,tactic,new BoundRandom(false)),physicalDamage(a,b,scale,tactic,new BoundRandom(true)),expectedDamage(a,b,scale,tactic),critical(a,b,tactic));
    }
    private static final class BoundRandom extends Random {
        private final boolean high;
        BoundRandom(boolean high){super(0);this.high=high;}
        @Override public double nextDouble(){return high?Math.nextDown(1.0):0;}
        @Override public int nextInt(int bound){return high?bound-1:0;}
    }
    /** Preserves the former tactical projection numerically, without allocating another unit model. */
    int rawDamage(World.Unit a,World.Unit b,double scale,Random rng){
        int amount;
        if(w.army.water(a.hex)||w.army.water(b.hex)||a.weapon.ordinal()>=4||b.weapon.ordinal()>=4)amount=advancedDamage(a,b,scale,rng);
        else {
            double offense=(150+0.60*w.army.leadership(a)+0.40*w.army.war(a))/200.0;
            offense*=attackFactor(a.weapon)*terrainAttack(w.terrain[a.hex.q][a.hex.r]);
            double defense=(170+0.85*w.army.leadership(b)+0.15*w.army.intelligence(b))/250.0;
            defense*=defenseFactor(b.weapon);defense*=terrainDefense(w.terrain[b.hex.q][b.hex.r]);
            double ratio=Math.max(.35,Math.min(2.5,offense/defense));
            double raw=230*StrictMath.sqrt(a.troops/1000.0)*ratio*matchup(a.weapon,b.weapon)*scale;
            amount=(int)Math.max(20,Math.min(2500,Math.round(raw*(.9+.2*rng.nextDouble()))));
        }
        amount=amount*(100-w.fieldworks.defensePercent(b))/100;
        return Math.max(1,amount);
    }
    private static double attackFactor(World.Weapon weapon){switch(weapon){case SPEAR:return 1.05;case HALBERD:return .98;case CROSSBOW:return .95;case CAVALRY:return 1.15;default:throw new IllegalArgumentException("Not a field weapon");}}
    private static double defenseFactor(World.Weapon weapon){switch(weapon){case SPEAR:return 1;case HALBERD:return 1.2;case CROSSBOW:return .85;case CAVALRY:return .95;default:throw new IllegalArgumentException("Not a field weapon");}}
    private static double terrainAttack(World.Terrain t){return t==World.Terrain.FOREST?.95:t==World.Terrain.MOUNTAIN?.9:1;}
    private static double terrainDefense(World.Terrain t){return t==World.Terrain.FOREST?1.2:t==World.Terrain.MOUNTAIN?1.25:1;}
    private static boolean advantage(World.Weapon a,World.Weapon b){return a==World.Weapon.SPEAR&&b==World.Weapon.CAVALRY||a==World.Weapon.CAVALRY&&b==World.Weapon.HALBERD||a==World.Weapon.HALBERD&&b==World.Weapon.SPEAR;}
    private static double matchup(World.Weapon a,World.Weapon b){return advantage(a,b)?1.25:advantage(b,a)?.90:1;}
    private int advancedDamage(World.Unit a,World.Unit b,double scale,Random random){
        double ap=w.army.water(a.hex)?a.ship.power:a.weapon.power,bp=w.army.water(b.hex)?b.ship.power:b.weapon.power;
        double offense=80+w.army.leadership(a)+w.army.war(a)/2.0;
        double defense=80+w.army.leadership(b)+w.army.intelligence(b)/4.0;
        if(b instanceof Domestic.Mission)defense/=2; // Weak convoy defense: engineering coefficient, not a calibrated original formula.
        int amount=(int)(250*StrictMath.sqrt(a.troops/1000.0)*ap/100*Math.max(.4,offense/defense)*scale*(.9+random.nextDouble()*.2));
        if(!w.army.water(b.hex)&&Army.siegeWeapon(b.weapon))amount=amount*3/2;
        if(w.army.water(b.hex)&&bp>100)amount=amount*9/10;
        return Math.max(20,Math.min(2500,amount));
    }
    public int physicalDamage(World.Unit a,World.Unit b,double scale,boolean tactic,Random rng){
        if(critical(a,b,tactic))scale*=1.15;
        if(w.campaign.eliteUnit(a))scale*=1.1*1.05;if(w.campaign.eliteUnit(b))scale/=1.1;
        if(w.fieldworks.drum(a))scale*=1.1;
        if(!w.army.water(a.hex)&&a.weapon.ordinal()<4){Campaign.Tech drill=new Campaign.Tech[]{Campaign.Tech.SPEAR_DRILL,Campaign.Tech.HALBERD_DRILL,Campaign.Tech.CROSSBOW_DRILL,Campaign.Tech.CAVALRY_DRILL}[a.weapon.ordinal()];if(w.campaign.has(a.owner,drill))scale*=1.1;}
        int amount=rawDamage(a,b,scale,rng);
        if(!tactic&&!w.army.water(b.hex)&&b.weapon==World.Weapon.HALBERD&&(w.campaign.has(b.owner,Campaign.Tech.LARGE_SHIELD)||a.hex.distance(b.hex)>1&&w.campaign.has(b.owner,Campaign.Tech.SHIELD))&&rng.nextInt(100)<30)return 0;
        if(w.skills.has(b,Skill.TENGJIA))amount=Math.max(1,amount/2);
        if(!tactic&&w.skills.nullifyNormal(b,amount,rng))return 0;
        return Math.min(b.troops,amount);
    }
    public boolean critical(World.Unit a,World.Unit b,boolean tactic){
        if(!tactic&&b!=null&&!w.army.water(a.hex)&&a.weapon==World.Weapon.CAVALRY&&a.hex.distance(b.hex)>1&&w.skills.has(a,BAIMA))return true;
        if(b!=null&&w.terrain[a.hex.q][a.hex.r]==World.Terrain.FOREST&&w.skills.has(a,LUANZHAN))return true;
        int category=w.army.water(a.hex)?5:Army.category(a.weapon);
        if(!tactic)return b!=null&&(w.skills.holderStat(a,QUZHU,false)>w.army.war(b)||w.skills.holderStat(a,SHENJIANG,false)>w.army.war(b));
        if(w.skills.has(a,BAWANG))return true;
        Skill god=category==0?QIANGSHEN:category==1?JISHEN:category==2?GONGSHEN:category==3?QISHEN:category==4?GONGSHEN_SIEGE:category==5?SHUISHEN:null;
        if(god!=null&&w.skills.has(a,god)||category>=0&&category<=1&&w.skills.has(a,DOUSHEN))return true;
        if(b==null)return w.skills.has(a,GONGCHENG);
        int defense=w.army.war(b);
        if(w.skills.holderStat(a,YONGJIANG,false)>defense)return true;
        if(category>=0&&category<=3&&(w.skills.holderStat(a,FEIJIANG,false)>defense||w.skills.holderStat(a,SHENJIANG,false)>defense))return true;
        Skill general=category==0?QIANGJIANG:category==1?JIJIANG:category==2?GONGJIANG:category==3?QIJIANG:category==5?SHUIJIANG:null;
        return general!=null&&w.skills.holderStat(a,general,false)>defense;
    }
    public int fireDamage(World.Unit target,int base,int owner,int power,boolean trap){
        if(w.skills.has(target,HUOSHEN))return 0;
        int amount=base;
        if(w.campaign.has(target.owner,Campaign.Tech.EXPLOSIVES))amount+=300;
        if(trap&&w.campaign.has(owner,Campaign.Tech.EXPLOSIVES))amount+=300;
        amount*=power;
        if(w.skills.has(target,TENGJIA))amount*=2;
        if(trap&&w.skills.has(target,TAPO))amount/=2;
        return Math.min(target.troops,amount);
    }
    /** Only the elemental component is doubled; physical arrow impact is separate. */
    public int firePower(World.Unit source){return w.skills.has(source,HUOSHEN)?2:1;}
    public int ongoingFireDamage(World.Unit target,int base,int owner,int power,boolean trap){
        return w.skills.has(target,HUWEI)?0:fireDamage(target,base,owner,power,trap);
    }
    public String firePreview(World.Unit source,World.Unit target,boolean trap){
        int base=trap?700:400;
        return "火焰伤害："+(target==null?"按实际波及部队分别结算":fireDamage(target,base,source.owner,firePower(source),trap))
            +(w.skills.has(source,HUOSHEN)?" · 火神×2":"")+(target!=null&&w.skills.has(target,HUOSHEN)?" · 目标火神免疫":"")
            +"\n火矢物理伤害独立计算；火神不免疫箭矢物理伤害。";
    }
    public int siegeDefenseDamage(World.Unit u){
        if(w.army.water(u.hex))return u.ship==Army.Ship.WARSHIP?350:100;
        switch(u.weapon){case RAM:return 650;case WOODEN_BEAST:return 750;case CATAPULT:return 600;case SIEGE_TOWER:return 120;default:return 100;}
    }
    public int siegeTroopDamage(World.Unit u){
        if(w.army.water(u.hex))return u.ship==Army.Ship.WARSHIP?450:200;
        switch(u.weapon){case RAM:return 60;case WOODEN_BEAST:return 500;case CATAPULT:return 400;case SIEGE_TOWER:return 800;default:return 100;}
    }
    public static final class SiegeDamage {
        public final int wall,troops;
        SiegeDamage(int wall,int troops){this.wall=wall;this.troops=troops;}
    }
    public int structureDamage(World.Unit source,boolean tactic){
        int base=tactic?siegeDefenseDamage(source):200+w.army.war(source)*2;
        int amount=w.campaign.constructionDamage(source,base);
        return tactic&&critical(source,null,true)?amount*115/100:amount;
    }
    public SiegeDamage siege(World.Unit u,boolean tactic){
        boolean engine=Army.siegeWeapon(u.weapon)||w.army.water(u.hex);
        long value=(long)u.troops*u.weapon.power*(60+w.officer(u.officerId).leadership)*(50+u.energy);
        int legacy=Math.max(80,(int)(value/(100L*(80+70)*100*120/10)));
        int hit=engine?siegeDefenseDamage(u):Math.max(100,legacy/2);
        int troopHit=engine?siegeTroopDamage(u):hit;
        if(w.skills.has(u,GONGCHENG)||tactic&&critical(u,null,true)){hit=hit*115/100;troopHit=troopHit*115/100;}
        return new SiegeDamage(w.campaign.constructionDamage(u,hit),w.campaign.constructionDamage(u,troopHit));
    }
}
