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
        Physical evaluation=new Physical(a,b,scale,tactic);return expectedDamage(evaluation);
    }
    public int expectedWithFire(World.Unit a,World.Unit b,double scale,boolean fire){
        int physical=expectedDamage(a,b,scale,true);
        return physical+(fire?Math.min(b.troops-physical,fireDamage(b,DIRECT_FIRE_BASE,a.owner,firePower(a),false)):0);
    }
    private int expectedDamage(Physical evaluation){
        int total=0;for(int i=0;i<8;i++)total+=evaluation.roll(new Random(7919L*i+17));
        return total/8;
    }
    public DamageRange preview(World.Unit a,World.Unit b,double scale,boolean tactic){
        Physical evaluation=new Physical(a,b,scale,tactic);
        return new DamageRange(evaluation.roll(new BoundRandom(false)),evaluation.roll(new BoundRandom(true)),expectedDamage(evaluation),evaluation.critical);
    }
    private static final class BoundRandom extends Random {
        private final boolean high;
        BoundRandom(boolean high){super(0);this.high=high;}
        @Override public double nextDouble(){return high?Math.nextDown(1.0):0;}
        @Override public int nextInt(int bound){return high?bound-1:0;}
    }
    /** One field/naval formula; previews and execution never allocate a second unit model. */
    int rawDamage(World.Unit a,World.Unit b,double scale,Random rng){return rawDamage(a,b,scale,rng,w.fieldworks.defensePercent(b));}
    private int rawDamage(World.Unit a,World.Unit b,double scale,Random rng,int defensePercent){
        int amount;
        double ratio=Math.max(.35,Math.min(2.5,attackRating(a)/Math.max(1,defenseRating(b))));
        double counter=w.army.water(a.hex)||w.army.water(b.hex)?1:matchup(a.weapon,b.weapon);
        double raw=230*StrictMath.sqrt(Math.max(1,a.troops)/1000.0)*ratio*counter*scale;
        amount=(int)Math.max(1,Math.min(2500,Math.round(raw*(.9+.2*rng.nextDouble()))));
        amount=amount*(100-defensePercent)/100;
        return Math.max(1,amount);
    }
    private static double attackFactor(World.Weapon k){switch(k){case SPEAR:return .95;case HALBERD:return .90;case CROSSBOW:return .85;case CAVALRY:return 1.05;case SWORD:return .36;case WOODEN_BEAST:return 1;case CATAPULT:return .95;default:return .90;}}
    private static double defenseFactor(World.Weapon k){switch(k){case SPEAR:return .95;case HALBERD:return 1.05;case CROSSBOW:case CAVALRY:return .90;case SWORD:return .36;default:return .85;}}
    private double aptitudeFactor(World.Unit u){return .7+.1*Math.max(0,Math.min(3,w.army.aptitude(u)));}
    private double factor(World.Unit u,boolean attack){
        if(u instanceof Domestic.Mission)return attack?.14:.12;
        if(w.army.water(u.hex))return u.ship==Army.Ship.BOAT?.75:u.ship==Army.Ship.TOWER_SHIP?(attack?.90:.85):(attack?1:.95);
        return attack?attackFactor(u.weapon):defenseFactor(u.weapon);
    }
    private double statAptitude(World.Unit u){return u instanceof Domestic.Mission||!w.army.water(u.hex)&&u.weapon==World.Weapon.SWORD?1:aptitudeFactor(u);}
    public int baseAttack(World.Unit u){return (int)StrictMath.floor(w.army.war(u)*factor(u,true)*statAptitude(u)+1e-8);}
    public int baseDefense(World.Unit u){return (int)StrictMath.floor(w.army.leadership(u)*factor(u,false)*statAptitude(u)+1e-8);}
    public double attackRating(World.Unit u){return baseAttack(u)*(w.army.water(u.hex)?1:terrainAttack(w.terrain[u.hex.q][u.hex.r]));}
    public double defenseRating(World.Unit u){return baseDefense(u)*(w.army.water(u.hex)?1:terrainDefense(w.terrain[u.hex.q][u.hex.r]));}
    public String unitStats(World.Unit u){
        return String.format(java.util.Locale.ROOT,"攻击 %.1f · 防御 %.1f\n基础攻击 %d · 基础防御 %d\n编队统率 %d · 武力 %d · 智力 %d",attackRating(u),defenseRating(u),baseAttack(u),baseDefense(u),w.army.leadership(u),w.army.war(u),w.army.intelligence(u));
    }
    public String statExplanation(World.Unit u){
        StringBuilder text=new StringBuilder(unitStats(u));
        text.append("\n\n攻击取编队武力，防御取编队统率，再乘兵装和适性系数。显示值含当前地形；科技、特技、设施、暴击等按实际交战条件另行结算。\n主将为基础，副将只补高于主将的差值；夫妻/义兄弟全额、亲爱1/2、血缘1/3、普通1/4、厌恶无补正。两名副将分别计算后取较高结果，不相加。\n部队智力、兵种适性取全队最高。\n");
        World.Officer leader=w.officer(u.officerId);
        for(int id:u.deputies){World.Officer deputy=w.officer(id);text.append("\n").append(deputy.name).append("：补正后统率 ").append(w.relations.contribution(leader.id,id,leader.leadership,deputy.leadership)).append(" / 武力 ").append(w.relations.contribution(leader.id,id,w.contests.war(leader),w.contests.war(deputy)));}
        return text.append("\n\n伤害、地形与部分科技效果仍为移动版平衡规则，非原版伤害公式的逐位复刻。").toString();
    }
    private static double terrainAttack(World.Terrain t){if(t==World.Terrain.SWAMP)return 0.85;return t==World.Terrain.FOREST?.95:t==World.Terrain.MOUNTAIN?.9:1;}
    private static double terrainDefense(World.Terrain t){if(t==World.Terrain.SWAMP)return 0.9;return t==World.Terrain.FOREST?1.2:t==World.Terrain.MOUNTAIN?1.25:1;}
    private static boolean advantage(World.Weapon a,World.Weapon b){return a==World.Weapon.SPEAR&&b==World.Weapon.CAVALRY||a==World.Weapon.CAVALRY&&b==World.Weapon.HALBERD||a==World.Weapon.HALBERD&&b==World.Weapon.SPEAR;}
    private static double matchup(World.Weapon a,World.Weapon b){return advantage(a,b)?1.25:advantage(b,a)?.90:1;}
    public int physicalDamage(World.Unit a,World.Unit b,double scale,boolean tactic,Random rng){return new Physical(a,b,scale,tactic).roll(rng);}
    /** One synchronous evaluation snapshots modifiers once for all preview/AI samples. Never retained across a command. */
    private final class Physical {
        final World.Unit a,b;final boolean tactic,critical,shield,wicker;final int defense;final double scale;
        Physical(World.Unit a,World.Unit b,double base,boolean tactic){
            this.a=a;this.b=b;this.tactic=tactic;critical=critical(a,b,tactic);
            if(critical)base*=1.15;
            if(w.campaign.eliteUnit(a))base*=1.1*1.05;if(w.campaign.eliteUnit(b))base/=1.1;
            if(w.fieldworks.drum(a))base*=1.1;
            if(!w.army.water(a.hex)&&a.weapon.ordinal()<4){Campaign.Tech drill=new Campaign.Tech[]{Campaign.Tech.SPEAR_DRILL,Campaign.Tech.HALBERD_DRILL,Campaign.Tech.CROSSBOW_DRILL,Campaign.Tech.CAVALRY_DRILL}[a.weapon.ordinal()];if(w.campaign.has(a.owner,drill))base*=1.1;}
            scale=base;defense=w.fieldworks.defensePercent(b);wicker=w.skills.has(b,Skill.TENGJIA);
            shield=!tactic&&!w.army.water(b.hex)&&b.weapon==World.Weapon.HALBERD&&(w.campaign.has(b.owner,Campaign.Tech.LARGE_SHIELD)||a.hex.distance(b.hex)>1&&w.campaign.has(b.owner,Campaign.Tech.SHIELD));
        }
        int roll(Random rng){
            int amount=rawDamage(a,b,scale,rng,defense);
            if(shield&&rng.nextInt(100)<30)return 0;
            if(wicker)amount=Math.max(1,amount/2);
            if(!tactic&&w.skills.nullifyNormal(b,amount,rng))return 0;
            return Math.min(b.troops,amount);
        }
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
    public static final int DIRECT_FIRE_BASE=400;
    public static int trapBase(War.StructureKind kind){
        return kind==War.StructureKind.INFERNO_SEED||kind==War.StructureKind.INFERNO_BALL?1500:kind==War.StructureKind.FIRE_SEED||kind==War.StructureKind.FIRE_BALL?700:1000;
    }
    public String firePreview(World.Unit source,World.Unit target,int base,boolean trap){
        return "火焰伤害："+(target==null?"按实际波及部队分别结算":fireDamage(target,base,source.owner,firePower(source),trap))
            +(w.skills.has(source,HUOSHEN)?" · 火神×2":"")+(target!=null&&w.skills.has(target,HUOSHEN)?" · 目标火神免疫":"")
            +"\n火矢物理伤害独立计算；火神不免疫箭矢物理伤害。";
    }
    public int siegeDefenseDamage(World.Unit u){
        if(w.army.water(u.hex))return u.ship==Army.Ship.WARSHIP?500:u.ship==Army.Ship.TOWER_SHIP?380:200;
        switch(u.weapon){case RAM:return 900;case WOODEN_BEAST:return 1000;case CATAPULT:return 800;case SIEGE_TOWER:return 230;case SPEAR:return 320;case HALBERD:return 300;case CROSSBOW:return 220;case CAVALRY:return 240;default:return 200;}
    }
    public int siegeTroopDamage(World.Unit u){
        if(w.army.water(u.hex))return u.ship==Army.Ship.WARSHIP?450:200;
        switch(u.weapon){case RAM:return 100;case WOODEN_BEAST:return 550;case CATAPULT:return 450;case SIEGE_TOWER:return 1000;default:return 200;}
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
    public SiegeDamage siege(World.Unit u,boolean tactic){return siege(u,null,tactic);}
    /** At 10k/80 leadership/80 energy a spear deals about 480 to ports, 267 to cities.
     * Engines keep a breach role; small detachments no longer deliver full engine damage. */
    public SiegeDamage siege(World.Unit u,World.City target,boolean tactic){
        boolean engine=Army.siegeWeapon(u.weapon)||w.army.water(u.hex);
        double strength=StrictMath.sqrt(Math.max(1,u.troops)/10000.0)
            *(.65+.35*Math.min(100,w.army.leadership(u))/100.0)
            *(engine?1:.75+.25*Math.max(0,Math.min(100,u.energy))/100.0)*aptitudeFactor(u);
        double wallFactor=target==null||target.kind==World.SiteKind.CITY?1:target.kind==World.SiteKind.PORT?1.8:.9;
        int hit=Math.max(1,(int)Math.round(siegeDefenseDamage(u)*strength*wallFactor));
        int troopHit=Math.max(1,(int)Math.round(siegeTroopDamage(u)*strength));
        if(w.skills.has(u,GONGCHENG)||tactic&&critical(u,null,true)){hit=hit*115/100;troopHit=troopHit*115/100;}
        return new SiegeDamage(w.campaign.constructionDamage(u,hit),w.campaign.constructionDamage(u,troopHit));
    }
    public String siegePreview(World.Unit u,World.City c,boolean tactic){
        SiegeDamage d=siege(u,c,tactic);
        int strikes=Math.max(1,(c.defense+d.wall-1)/d.wall);
        return "预计城防 −"+Math.min(c.defense,d.wall)+" / "+c.defense+" · 守军 −"+Math.min(c.troops,d.troops)
            +"\n按当前兵力约 "+strikes+" 次命中破防（不含后续战损、补修）"
            +"\n"+w.cityDefense.preview(c,u);
    }
    public int spiralConfusionChance(World.Unit a,World.Unit b){
        return critical(a,b,true)?100:Math.max(10,Math.min(40,25+(w.army.war(a)-w.army.war(b))/2));
    }
}
