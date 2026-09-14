package game.sanguo.core;

import game.sanguo.core.battle.DamageCalculator;
import game.sanguo.core.battle.HexPos;
import game.sanguo.core.battle.adapter.LegacyWorldBattleAdapter;
import java.util.*;

/** Campaign-map combat. Reuses the tactical damage kernel without inventing a separate two-force battlefield. */
public final class War {
    public enum Status { NORMAL("正常"), CONFUSED("混乱"), MISLED("伪报");
        public final String label; Status(String label){this.label=label;}
    }
    public enum Tactic {
        THRUST("突刺",World.Weapon.SPEAR,15,1,1,1,1.2,"击退1格"),
        SPIRAL("螺旋突刺",World.Weapon.SPEAR,20,2,1,1,1.35,"混乱1次行动"),
        DOUBLE_THRUST("二段突刺",World.Weapon.SPEAR,25,3,1,1,1.5,"击退2格"),
        HOOK("熊手",World.Weapon.HALBERD,15,1,1,1,1.15,"自身后退并拉动敌军"),
        SWEEP("横扫",World.Weapon.HALBERD,20,2,1,1,1.15,"攻击正面相邻敌军"),
        WHIRLWIND("旋风",World.Weapon.HALBERD,30,3,1,1,1.25,"攻击周围所有敌军"),
        FIRE_ARROW("火矢",World.Weapon.CROSSBOW,10,1,1,2,1.15,"目标格起火"),
        PIERCE("贯射",World.Weapon.CROSSBOW,15,2,1,2,1.25,"贯穿同一直线的后一格敌军"),
        VOLLEY("乱射",World.Weapon.CROSSBOW,25,3,1,2,1.15,"攻击目标及相邻敌军"),
        CHARGE("突击",World.Weapon.CAVALRY,15,1,1,1,1.25,"击退1格并跟进"),
        BREAKTHROUGH("突破",World.Weapon.CAVALRY,20,2,1,1,1.4,"穿过敌军到其身后"),
        ADVANCE("突进",World.Weapon.CAVALRY,25,3,1,1,1.55,"击退2格并跟进");
        public final String label,effect;public final World.Weapon weapon;public final int energy,rank,minRange,maxRange;final double multiplier;
        Tactic(String label,World.Weapon weapon,int energy,int rank,int min,int max,double mult,String effect){this.label=label;this.weapon=weapon;this.energy=energy;this.rank=rank;minRange=min;maxRange=max;multiplier=mult;this.effect=effect;}
    }
    public enum Plot {
        FIRE("火计",10,"点燃目标格，火场持续2旬"), EXTINGUISH("灭火",10,"扑灭目标格火焰"),
        CONFUSE("扰乱",15,"令敌军跳过1次行动"), MISLEAD("伪报",15,"敌军下次行动向己城退却"),
        CALM("镇静",10,"清除己方部队混乱或伪报"), AMBUSH("伏兵",20,"从森林伏击邻接敌军并削减气力");
        public final String label,effect;public final int energy;
        Plot(String label,int energy,String effect){this.label=label;this.energy=energy;this.effect=effect;}
    }
    public enum StructureKind {
        CAMP("阵",1500,1100,"2格内己军减伤15%、粮耗减少10%"), ARROW_TOWER("箭楼",600,700,"每旬射击2格内一支敌军"),
        MUSIC("军乐台",800,800,"每旬恢复2格内己军10气力"), FIRE_SEED("火种",200,200,"引爆相邻格，可能伤及己军"),
        FORT("砦",1500,1100,"3格内己军减伤25%、粮耗减少30%"), FORTRESS("城塞",1500,1100,"4格内己军减伤35%、粮耗减少50%"),
        CROSSBOW_TOWER("连弩楼",600,700,"每旬射击3格内一支敌军"), CATAPULT_TOWER("投石台",800,1000,"每旬攻击2至3格内一支敌军，邻接为盲区"),
        DRUM("太鼓台",500,800,"2格内己军攻击伤害增加10%"), STONE_MAZE("石兵八阵",1200,1000,"敌军邻接时可能混乱"),
        EARTH_WALL("土垒",300,400,"阻挡地块通行"), STONE_WALL("石壁",300,800,"更坚固的通行障碍"),
        FIRE_BALL("火球",200,200,"沿指定六边形方向引爆3格"), FLAME_SEED("火焰种",200,200,"引爆半径2格"),
        FLAME_BALL("火焰球",200,200,"沿指定方向引爆5格"), INFERNO_SEED("业火种",200,200,"强化爆炸并造成混乱"),
        INFERNO_BALL("业火球",200,200,"强化直线爆炸，可穿过军事设施"), FIRE_SHIP("火船",200,200,"在水面设置并引爆" );
        public final String label,effect;public final int gold,hp;
        StructureKind(String label,int gold,int hp,String effect){this.label=label;this.gold=gold;this.hp=hp;this.effect=effect;}
    }
    public static final class Fire {
        public final Hex hex;public final int owner;public int remaining,power=1;public boolean trap;
        Fire(Hex hex,int owner,int remaining){this.hex=hex;this.owner=owner;this.remaining=remaining;}
    }
    public static final class Structure {
        public final int id,owner;public StructureKind kind;public final Hex hex;public int hp,builder=-1,direction;public boolean complete=true;
        Structure(int id,int owner,StructureKind kind,Hex hex,int hp){this.id=id;this.owner=owner;this.kind=kind;this.hex=hex;this.hp=hp;}
    }
    final World w;final List<Fire> fires=new ArrayList<>();final List<Structure> structures=new ArrayList<>();int nextStructureId=1;
    War(World w){this.w=w;}
    public List<Fire> fires(){return Collections.unmodifiableList(fires);}
    public List<Structure> structures(){return Collections.unmodifiableList(structures);}
    public Fire fireAt(Hex h){for(Fire f:fires)if(f.hex.equals(h))return f;return null;}
    public Structure at(Hex h){for(Structure s:structures)if(s.hex.equals(h))return s;return null;}
    public static String rankLabel(int rank){return new String[]{"C","B","A","S"}[Math.max(0,Math.min(3,rank))];}
    public int range(World.Unit u){return w.army.range(u)+(w.skills.has(u,Skill.SHECHENG)&&!w.army.water(u.hex)&&(u.weapon==World.Weapon.SIEGE_TOWER||u.weapon==World.Weapon.CATAPULT)?1:0)+(!w.army.water(u.hex)&&u.weapon==World.Weapon.CROSSBOW&&w.campaign.has(u.owner,Campaign.Tech.STRONG_BOW)?1:0);}
    public int movement(World.Unit u){return w.army.movement(u)+w.skills.movementBonus(u)+(w.campaign.eliteUnit(u)?1:0)+(!w.army.water(u.hex)&&Army.siegeWeapon(u.weapon)&&w.campaign.has(u.owner,Campaign.Tech.AXLE)?1:0)+(!w.army.water(u.hex)&&u.weapon==World.Weapon.CAVALRY&&w.campaign.has(u.owner,Campaign.Tech.HORSE_BREEDING)?1:0);}
    private String actorError(World.Unit u){
        return w.orders.error(u);
    }
    private String targetError(World.Unit a,World.Unit b,int min,int max){
        if(b==null||!w.campaign.hostile(a.owner,b.owner))return "请选择交战势力的部队";
        if(!w.fieldworks.landTarget(a.owner,b.hex))return "需要难所行军才能攻击该地形上的目标";
        int distance=a.hex.distance(b.hex);return distance<min||distance>max?"敌军不在范围内":null;
    }
    private game.sanguo.core.battle.Terrain terrain(Hex h){
        switch(w.terrain[h.q][h.r]){case FOREST:return game.sanguo.core.battle.Terrain.FOREST;case WATER:return game.sanguo.core.battle.Terrain.RIVER;case MOUNTAIN:return game.sanguo.core.battle.Terrain.MOUNTAIN;default:return game.sanguo.core.battle.Terrain.PLAIN;}
    }
    private int damage(World.Unit a,World.Unit b,double scale,Random rng){
        int amount=w.army.water(a.hex)||w.army.water(b.hex)||a.weapon.ordinal()>=4||b.weapon.ordinal()>=4?w.army.damage(a,b,scale,rng):DamageCalculator.rawDamage(LegacyWorldBattleAdapter.toBattleUnit(a,w.army.combatOfficer(a),new HexPos(a.hex.q,a.hex.r)),
            LegacyWorldBattleAdapter.toBattleUnit(b,w.army.combatOfficer(b),new HexPos(b.hex.q,b.hex.r)),terrain(a.hex),terrain(b.hex),scale,rng);

        amount=amount*(100-w.fieldworks.defensePercent(b))/100;
        return Math.max(1,amount);
    }
    private Random random(){return new Random(w.strategy.nextInt(Integer.MAX_VALUE));}
    private void hurt(World.Unit u,int damage){if(w.unit(u.id)==null)return;u.troops=Math.max(0,u.troops-damage);if(u.troops==0)w.removeUnit(u);}
    int physicalDamage(World.Unit a,World.Unit b,double scale,boolean tactic,Random rng){
        if(w.skills.critical(a,b,tactic))scale*=1.15;
        if(w.campaign.eliteUnit(a))scale*=1.1*1.05;if(w.campaign.eliteUnit(b))scale/=1.1;
        if(w.fieldworks.drum(a))scale*=1.1;
        if(!w.army.water(a.hex)&&a.weapon.ordinal()<4){Campaign.Tech drill=new Campaign.Tech[]{Campaign.Tech.SPEAR_DRILL,Campaign.Tech.HALBERD_DRILL,Campaign.Tech.CROSSBOW_DRILL,Campaign.Tech.CAVALRY_DRILL}[a.weapon.ordinal()];if(w.campaign.has(a.owner,drill))scale*=1.1;}
        int amount=damage(a,b,scale,rng);
        if(!tactic&&!w.army.water(b.hex)&&b.weapon==World.Weapon.HALBERD&&(w.campaign.has(b.owner,Campaign.Tech.LARGE_SHIELD)||a.hex.distance(b.hex)>1&&w.campaign.has(b.owner,Campaign.Tech.SHIELD))&&rng.nextInt(100)<30)return 0;
        if(w.skills.has(b,Skill.TENGJIA))amount=Math.max(1,amount/2);
        if(!tactic&&w.skills.nullifyNormal(b,amount,rng))return 0;
        return Math.min(b.troops,amount);
    }
    private int strike(World.Unit a,World.Unit b,double scale,boolean tactic){
        int amount=physicalDamage(a,b,scale,tactic,random());b.troops-=amount;if(b.troops==0)w.defeatUnit(b,a);w.skills.onHit(a,b,amount,tactic);if(w.campaign.hostile(a.owner,b.owner))w.government.earn(a.officerId,amount/10);return amount;
    }
    public int previewDamage(int actor,int target){World.Unit a=w.unit(actor),b=w.unit(target);return a==null||b==null?0:physicalDamage(a,b,1,false,new Random(0));}
    public World.Result attack(int actor,int target){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=actorError(a);if(error==null)error=targetError(a,b,1,range(a));if(error!=null)return w.fail(error);
        if(!w.army.canAttackUnit(a))return w.fail("兵器需要使用战法");
        if(!w.army.water(a.hex)&&a.weapon==World.Weapon.CROSSBOW&&w.terrain[b.hex.q][b.hex.r]==World.Terrain.FOREST&&!w.skills.has(a,Skill.SHESHOU))return w.fail("射向森林需要射手特技");
        a.acted=true;int dealt=0,counter=0;
        int attacks=w.skills.has(a,Skill.LIANZHAN)&&w.strategy.nextInt(100)<50?2:1;
        for(int i=0;i<attacks&&w.unit(a.id)!=null&&w.unit(b.id)!=null;i++){
            dealt+=strike(a,b,1,false);
            if(w.unit(b.id)!=null&&canCounter(a,b)&&b.status==Status.NORMAL&&!(w.skills.has(a,w.army.water(a.hex)?Skill.QIANGXI:Skill.JIXI)&&w.skills.avoidCounter(a,random())))
                counter+=strike(b,a,.5,false);
        }
        int support=supportAttack(a,b);
        w.campaign.earn(a.owner,w.unit(b.id)==null&&w.skills.has(a,Skill.JINGMIAO)?40:20);w.checkVictory();
        return w.success(w.officer(a.officerId).name+"攻击：敌损"+dealt+"，反击损失"+counter+(support>0?"，支援伤害"+support:""));
    }
    private boolean canCounter(World.Unit a,World.Unit b){
        if(!w.army.water(a.hex)&&a.weapon==World.Weapon.SPEAR&&w.terrain[a.hex.q][a.hex.r]==World.Terrain.FOREST&&w.campaign.has(a.owner,Campaign.Tech.FOREST_AMBUSH))return false;
        if(a.hex.distance(b.hex)==1&&w.army.counter(b))return true;
        return !w.army.water(b.hex)&&b.weapon==World.Weapon.CROSSBOW&&w.campaign.has(b.owner,Campaign.Tech.RETURN_FIRE)&&a.hex.distance(b.hex)<=range(b)&&
            (a.weapon==World.Weapon.CROSSBOW||a.hex.distance(b.hex)>1)&&
            (w.terrain[a.hex.q][a.hex.r]!=World.Terrain.FOREST||w.skills.has(b,Skill.SHESHOU));
    }
    private int supportAttack(World.Unit attacker,World.Unit target){
        if(w.unit(attacker.id)==null||w.unit(target.id)==null)return 0;int damage=0;
        List<World.Unit> helpers=new ArrayList<>(w.units);helpers.sort(Comparator.comparingInt(u->u.id));
        for(World.Unit helper:helpers){
            if(w.unit(target.id)==null)break;
            if(helper.id==attacker.id||helper.owner!=attacker.owner||helper.status!=Status.NORMAL||w.relations.supportChance(helper.officerId,attacker.officerId)==0||!w.army.canAttackUnit(helper))continue;
            int distance=helper.hex.distance(target.hex);if(distance<1||distance>range(helper))continue;
            if(!w.army.water(helper.hex)&&helper.weapon==World.Weapon.CROSSBOW&&w.terrain[target.hex.q][target.hex.r]==World.Terrain.FOREST&&!w.skills.has(helper,Skill.SHESHOU))continue;
            if(w.strategy.nextInt(100)<w.relations.supportChance(helper.officerId,attacker.officerId))damage+=strike(helper,target,.5,false);
        }
        return damage;
    }
    public int tacticChance(int actor,int target,Tactic tactic){
        World.Unit a=w.unit(actor),b=w.unit(target);if(a==null||b==null||tactic==null)return 0;
        if(b.status!=Status.NORMAL)return 100;
        return Math.max(30,Math.min(95,70+w.army.aptitude(a)*5+(w.army.war(a)-w.army.war(b))/5));
    }
    public String tacticError(int actor,int target,Tactic tactic){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=actorError(a);if(error!=null)return error;
        if(w.army.water(a.hex))return "水上需使用水军战法";
        if(tactic==null||a.weapon!=tactic.weapon)return "兵种不能使用该战法";
        if(w.army.aptitude(a)<tactic.rank)return "需要"+rankLabel(tactic.rank)+"级兵科适性";
        if(a.energy<tactic.energy)return "气力不足";
        error=targetError(a,b,tactic.minRange,tactic.maxRange+(a.weapon==World.Weapon.CROSSBOW?range(a)-a.weapon.range:0));if(error!=null)return error;
        if(a.weapon==World.Weapon.CAVALRY&&(terrain(a.hex)==game.sanguo.core.battle.Terrain.FOREST||terrain(b.hex)==game.sanguo.core.battle.Terrain.FOREST))return "骑兵战法不能在森林使用";
        if(a.weapon==World.Weapon.CROSSBOW&&w.terrain[b.hex.q][b.hex.r]==World.Terrain.FOREST&&!w.skills.has(a,Skill.SHESHOU))return "射向森林需要射手特技";
        if(tactic==Tactic.PIERCE&&direction(a.hex,b.hex)==null)return "贯射需要直线目标";
        if(w.army.water(b.hex)&&(tactic==Tactic.HOOK||tactic==Tactic.THRUST||tactic==Tactic.DOUBLE_THRUST||tactic==Tactic.CHARGE||tactic==Tactic.ADVANCE||tactic==Tactic.BREAKTHROUGH))return "位移战法不能跨越水陆边界";
        if(tactic==Tactic.HOOK&&!vacant(add(a.hex,-(b.hex.q-a.hex.q),-(b.hex.r-a.hex.r)),a.weapon))return "熊手后退位置被阻挡";
        if(tactic==Tactic.BREAKTHROUGH&&!vacant(add(b.hex,b.hex.q-a.hex.q,b.hex.r-a.hex.r),a.weapon))return "敌军身后没有可用格";
        return null;
    }
    public World.Result tactic(int actor,int target,Tactic tactic){
        String error=tacticError(actor,target,tactic);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unit(target);int chance=tacticChance(actor,target,tactic);a.acted=true;a.energy-=tactic.energy;
        if(w.strategy.nextInt(100)>=chance)return w.success(w.officer(a.officerId).name+"的"+tactic.label+"未命中，气力已消耗");
        Hex origin=a.hex,targetHex=b.hex;List<World.Unit> victims=new ArrayList<>();victims.add(b);
        for(World.Unit u:new ArrayList<>(w.units))if(u.id!=b.id&&u.id!=a.id&&(w.campaign.hostile(a.owner,u.owner)||tactic==Tactic.VOLLEY&&u.owner==a.owner&&!w.skills.has(a,Skill.GONGSHEN))){
            boolean splash=tactic==Tactic.WHIRLWIND&&origin.distance(u.hex)==1
                ||tactic==Tactic.SWEEP&&origin.distance(u.hex)==1&&targetHex.distance(u.hex)==1
                ||tactic==Tactic.VOLLEY&&targetHex.distance(u.hex)<=1;
            int[] ray=direction(origin,targetHex);if(tactic==Tactic.PIERCE&&ray!=null&&u.hex.equals(add(targetHex,ray[0],ray[1])))splash=true;
            if(splash)victims.add(u);
        }
        double multiplier=tactic.multiplier;
        int dealt=0;for(World.Unit victim:victims){int hit=strike(a,victim,multiplier,true);dealt+=hit;}
        switch(tactic){
            case THRUST:push(a,b,origin,targetHex,1,false);break;case DOUBLE_THRUST:push(a,b,origin,targetHex,2,false);break;
            case CHARGE:push(a,b,origin,targetHex,1,true);break;case ADVANCE:push(a,b,origin,targetHex,2,true);break;
            case SPIRAL:if(w.unit(b.id)!=null){b.status=Status.CONFUSED;b.statusTurns=w.skills.critical(a,b,true)?2:1;}break;
            case HOOK:a.hex=add(origin,-(targetHex.q-origin.q),-(targetHex.r-origin.r));if(w.unit(b.id)!=null)b.hex=origin;break;
            case BREAKTHROUGH:a.hex=add(targetHex,targetHex.q-origin.q,targetHex.r-origin.r);break;
            case FIRE_ARROW:ignite(targetHex,a);break;default:break;
        }
        if(!a.hex.equals(origin)||!b.hex.equals(targetHex))w.skills.woundAfterDisplacement(a,b);
        if(w.unit(b.id)!=null&&a.weapon==World.Weapon.CAVALRY&&w.skills.swiftConfusion(a,b)){b.status=Status.CONFUSED;b.statusTurns=1;}
        w.campaign.earn(a.owner,w.unit(b.id)==null&&w.skills.has(a,Skill.JINGMIAO)?80:40);w.checkVictory();return w.success(w.officer(a.officerId).name+"施展"+tactic.label+"，命中"+victims.size()+"队，敌损"+dealt);
    }
    private static Hex add(Hex h,int q,int r){return new Hex(h.q+q,h.r+r);}
    private static int[] direction(Hex a,Hex b){
        int distance=a.distance(b);if(distance==0)return null;
        int dq=b.q-a.q,dr=b.r-a.r;if(dq%distance!=0||dr%distance!=0)return null;
        Hex step=new Hex(dq/distance,dr/distance);return new Hex(0,0).distance(step)==1?new int[]{step.q,step.r}:null;
    }
    private boolean vacant(Hex h,World.Weapon weapon){return w.cost(h,weapon)>0&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&at(h)==null;}
    private void push(World.Unit a,World.Unit b,Hex origin,Hex target,int steps,boolean follow){
        int dq=target.q-origin.q,dr=target.r-origin.r;
        for(int i=0;i<steps&&w.unit(b.id)!=null;i++){
            Hex old=b.hex,next=add(old,dq,dr);Structure trap=at(next);
            if(trap!=null&&trap.complete&&w.fieldworks.trap(trap.kind)&&w.campaign.hostile(a.owner,trap.owner)){b.hex=next;ignite(next,a);if(follow)a.hex=old;continue;}
            if(!vacant(next,b.weapon)){World.Unit collision=w.unitAt(next);hurt(b,100);if(collision!=null&&(collision.owner==a.owner||w.campaign.hostile(a.owner,collision.owner)))hurt(collision,100);break;}b.hex=next;if(follow)a.hex=old;
        }
        if(follow&&w.unit(b.id)==null&&vacant(target,a.weapon))a.hex=target;
    }
    public int plotChance(int actor,Hex target,Plot plot){
        World.Unit a=w.unit(actor),b=target==null?null:w.unitAt(target);if(a==null||plot==null)return 0;
        if(plot==Plot.CALM||plot==Plot.EXTINGUISH)return 100;
        int defense=b==null?50:w.army.intelligence(b);
        return w.skills.plotChance(a,b,plot,Math.max(10,Math.min(95,65+(w.army.intelligence(a)-defense)/2)));
    }
    public String plotError(int actor,Hex target,Plot plot){
        World.Unit a=w.unit(actor);String error=actorError(a);if(error!=null)return error;
        if(plot==null||target==null||!w.inside(target)||a.hex.distance(target)>w.skills.plotRange(a,plot))return "请选择计略范围内目标";
        if(a.energy<w.skills.plotCost(a,plot))return "气力不足";World.Unit b=w.unitAt(target);
        if(plot==Plot.EXTINGUISH)return fireAt(target)==null?"目标没有火焰":null;
        if(plot==Plot.CALM)return b==null||b.owner!=a.owner||b.status==Status.NORMAL?"请选择异常状态的己方部队":null;
        if(plot==Plot.FIRE){
            if((w.cost(target,World.Weapon.SPEAR)<0&&!(at(target)!=null&&at(target).kind==StructureKind.FIRE_SHIP))||w.cityAt(target)!=null||w.domestic.at(target)!=null)return "此地无法放火";
            Structure s=at(target);if(b!=null&&!w.campaign.hostile(a.owner,b.owner)||s!=null&&s.owner!=a.owner&&!w.campaign.hostile(a.owner,s.owner))return "不能对友军或协定势力放火";
            return fireAt(target)!=null?"目标已起火":null;
        }
        error=targetError(a,b,1,w.skills.plotRange(a,plot));if(error!=null)return error;
        if(plot==Plot.AMBUSH&&(w.army.water(a.hex)||a.weapon==World.Weapon.CAVALRY||Army.siegeWeapon(a.weapon)))return "当前兵科不能施展伏兵";
        if(plot==Plot.AMBUSH&&w.terrain[a.hex.q][a.hex.r]!=World.Terrain.FOREST)return "伏兵需要自身位于森林";
        if((plot==Plot.CONFUSE||plot==Plot.MISLEAD)&&b.status!=Status.NORMAL)return "目标已经处于异常状态";return null;
    }
    public int plotCost(int actor,Plot plot){World.Unit a=w.unit(actor);return a==null||plot==null?0:w.skills.plotCost(a,plot);}
    public int plotRange(int actor,Plot plot){World.Unit a=w.unit(actor);return a==null||plot==null?0:w.skills.plotRange(a,plot);}
    public World.Result plot(int actor,Hex target,Plot plot){
        String error=plotError(actor,target,plot);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unitAt(target);a.acted=true;a.energy-=plotCost(actor,plot);
        boolean success=resolvePlot(a,b,target,plot,true);
        if(success&&b!=null&&w.skills.has(a,Skill.LIANHUAN)&&(plot==Plot.CONFUSE||plot==Plot.MISLEAD||plot==Plot.FIRE)){
            List<World.Unit> adjacent=new ArrayList<>();
            for(World.Unit u:w.units)if(u.id!=b.id&&w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(target)==1&&
                (plot==Plot.FIRE?fireAt(u.hex)==null&&!w.army.water(u.hex):u.status==Status.NORMAL))adjacent.add(u);
            adjacent.sort(Comparator.comparingInt(u->u.id));
            if(!adjacent.isEmpty()){World.Unit chained=adjacent.get(0);resolvePlot(a,chained,chained.hex,plot,false);}
        }
        w.campaign.earn(a.owner,success?25:0);w.checkVictory();
        return w.success(w.officer(a.officerId).name+"施展"+plot.label+(success?"":"被识破，气力已消耗"));
    }
    /** A reflected/chained plot pays no second cost and cannot recurse into another reflection/chain. */
    private boolean resolvePlot(World.Unit a,World.Unit b,Hex target,Plot plot,boolean reflection){
        int chance=plotChance(a.id,target,plot);
        boolean success=chance==100||chance>0&&w.strategy.nextInt(100)<chance;
        if(!success){
            if(reflection&&b!=null&&w.skills.has(b,Skill.FANJI)&&(plot==Plot.CONFUSE||plot==Plot.MISLEAD)&&a.status==Status.NORMAL)
                resolvePlot(b,a,a.hex,plot,false);
            return false;
        }
        boolean critical=w.skills.plotCritical(a,b,plot);
        switch(plot){
            case FIRE:ignite(target,a);break;
            case EXTINGUISH:
                fires.removeIf(f->f.hex.equals(target)||critical&&f.hex.distance(target)==1);break;
            case CONFUSE:b.status=Status.CONFUSED;b.statusTurns=critical?2:1;break;
            case MISLEAD:b.status=Status.MISLED;b.statusTurns=critical?2:1;break;
            case CALM:
                for(World.Unit u:w.units)if(u.owner==a.owner&&(u.id==b.id||critical&&u.hex.distance(target)==1)){u.status=Status.NORMAL;u.statusTurns=0;}break;
            case AMBUSH:
                int hit=Math.min(b.troops,damage(a,b,critical?1.35*1.15:1.35,random()));hurt(b,hit);
                if(w.unit(b.id)!=null){b.energy=Math.max(0,b.energy-15);if(critical){b.status=Status.CONFUSED;b.statusTurns=1;}}break;
        }
        return true;
    }
    void ignite(Hex target,World.Unit source){w.fieldworks.ignite(target,source);}
    /** Legacy city-based fixture helper. Android gameplay uses Fieldworks.build with unit gold. */
    @Deprecated
    public List<Hex> buildSites(int city){
        World.City c=w.city(city);List<Hex> result=new ArrayList<>();if(c==null)return result;
        for(int q=Math.max(0,c.hex.q-3);q<=Math.min(w.width-1,c.hex.q+3);q++)for(int r=Math.max(0,c.hex.r-3);r<=Math.min(w.height-1,c.hex.r+3);r++){
            Hex h=new Hex(q,r);if(c.hex.distance(h)<=3&&c.hex.distance(h)>=2&&vacant(h,World.Weapon.SPEAR)&&fireAt(h)==null&&w.cities.stream().noneMatch(other->other.hex.distance(h)<=1))result.add(h);
        }return result;
    }
    public World.Result build(int city,int officer,StructureKind kind,Hex h){
        if(kind==null)return w.fail("设施类型无效");World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,kind.gold);if(error!=null)return w.fail(error);
        if(structures.size()>=1000||nextStructureId>=10000000)return w.fail("军事设施已达上限");
        if(!buildSites(city).contains(h))return w.fail("请选择城池两至三格内空地，不可堵塞城池出口");
        w.spend(c,o,kind.gold);structures.add(new Structure(nextStructureId++,c.owner,kind,h,kind.hp));return w.success(c.name+"建造"+kind.label);
    }
    public World.Result attackStructure(int unit,Hex h){
        World.Unit u=w.unit(unit);String error=actorError(u);if(error!=null)return w.fail(error);Structure s=at(h);
        if(s==null||!w.campaign.hostile(u.owner,s.owner)||u.hex.distance(h)>range(u))return w.fail("请选择射程内敌方军事设施");
        if(!w.army.canAttackUnit(u))return w.army.tactic(unit,h,w.army.tactics(u).get(0));
        int damage=Math.min(s.hp,w.campaign.constructionDamage(u,200+w.army.war(u)*2));u.acted=true;s.hp-=damage;
        if(s.hp<=0)structures.remove(s);else w.fieldworks.counter(s,u);w.campaign.earn(u.owner,20);return w.success("攻击"+s.kind.label+"，耐久减少"+damage);
    }
    public World.Result removeStructure(int city,int officer,int id){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        Structure s=null;for(Structure item:structures)if(item.id==id)s=item;
        if(s==null||s.owner!=c.owner||s.hex.distance(c.hex)>3)return w.fail("请选择本城三格内己方军事设施");
        w.spend(c,o,0);structures.remove(s);return w.success("已拆除"+s.kind.label);
    }
    public World.Result waitUnit(int unit){World.Unit u=w.unit(unit);String error=actorError(u);if(error!=null)return w.fail(error);u.acted=true;u.energy=Math.min(w.campaign.energyCap(u.owner),u.energy+5);return w.success("部队待命，恢复5气力");}
    void resetOwner(int owner){
        for(World.Unit u:new ArrayList<>(w.units))if(u.owner==owner&&u.status!=Status.NORMAL){
            if(u.statusTurns<=0){u.status=Status.NORMAL;continue;}
            u.acted=true;u.statusTurns--;
            if(u.status==Status.MISLED){
                World.City home=null;for(World.City c:w.cities)if(c.owner==owner&&(home==null||u.hex.distance(c.hex)<u.hex.distance(home.hex)))home=c;
                if(home!=null){final Hex destination=home.hex;List<Hex> steps=u.hex.neighbors();steps.sort(Comparator.comparingInt(h->h.distance(destination)));for(Hex h:steps)if(h.distance(destination)<u.hex.distance(destination)&&w.army.moveCost(u,u.hex,h)>0&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&at(h)==null){u.hex=h;break;}}
            }
            w.note(w.officer(u.officerId).name+"受"+u.status.label+"影响，本旬不能行动");
        }
    }
    void tick(){
        for(Fire f:new ArrayList<>(fires)){
            World.Unit u=w.unitAt(f.hex);if(u!=null&&(u.owner==f.owner||w.campaign.hostile(f.owner,u.owner))){int hit=250+(w.terrain[f.hex.q][f.hex.r]==World.Terrain.FOREST?150:0);hit=w.skills.fireDamage(u,hit,f.owner,f.power,f.trap);hurt(u,hit);w.note("火场灼烧，部队损失"+hit);}
            Structure s=at(f.hex);if(s!=null&&(s.owner==f.owner||w.campaign.hostile(f.owner,s.owner))){s.hp-=200;if(s.hp<=0)structures.remove(s);}
            if(--f.remaining==0)fires.remove(f);
        }
        for(Structure s:new ArrayList<>(structures)){
            if(!w.alive(s.owner)){structures.remove(s);continue;}
            // Music restoration is deduplicated per unit below, including skill priority.

        }
        w.fieldworks.towers();w.skills.restoreEnergy();
    }
    boolean aiAction(World.Unit u,World.Unit enemy){
        if(u.acted||u.status!=Status.NORMAL)return false;
        for(Army.Tactic tactic:w.army.tactics(u))if(w.army.tacticError(u.id,enemy.hex,tactic)==null)return w.army.tactic(u.id,enemy.hex,tactic).ok;
        for(Tactic tactic:Tactic.values())if(tacticError(u.id,enemy.id,tactic)==null)return tactic(u.id,enemy.id,tactic).ok;
        return false;
    }
}
