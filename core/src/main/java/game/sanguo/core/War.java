package game.sanguo.core;

import java.util.*;

/** Campaign command validation and ordering; CombatRules is the authoritative calculation path. */
public final class War {
    public enum Status { NORMAL("正常"), CONFUSED("混乱"), MISLED("伪报");
        public final String label; Status(String label){this.label=label;}
    }
    public enum Tactic {
        THRUST("突刺",World.Weapon.SPEAR,15,1,1,1,1.2,"击退1格"),
        SPIRAL("螺旋突刺",World.Weapon.SPEAR,20,2,1,1,1.35,"命中后概率混乱；条件暴击必定混乱"),
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
        FIRE("火计",10,"即时火伤并点燃目标格；通常持续2旬，暴击延长1旬"), EXTINGUISH("灭火",10,"扑灭目标格火焰"),
        CONFUSE("扰乱",15,"令敌军跳过1次行动"), MISLEAD("伪报",15,"敌军下次行动向己城退却"),
        CALM("镇静",10,"清除己方部队混乱或伪报"), AMBUSH("伏兵",10,"从森林伏击邻接敌军并削减气力"),
        INFIGHT("同讨",20,"使目标与其相邻同势力部队交战；不能针对兵器"),
        SORCERY("妖术",50,"需要妖术或鬼门；使目标及相邻敌军混乱或伪报"),
        LIGHTNING("落雷",50,"需要鬼门；打击目标及相邻格并起火，会伤及己方");
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
        INFERNO_BALL("业火球",200,200,"强化直线爆炸，可穿过军事设施"), FIRE_SHIP("火船",200,200,"在水面设置并引爆"),
        DAM("堤坝",0,1200,"阻挡通行；击破后两格内低地洪水，敌我均受600兵损失");
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
    final Displacement displacement;
    War(World w){this.w=w;displacement=new Displacement(w);}
    public List<Fire> fires(){return Collections.unmodifiableList(fires);}
    public List<Structure> structures(){return Collections.unmodifiableList(structures);}
    public Fire fireAt(Hex h){for(Fire f:fires)if(f.hex.equals(h))return f;return null;}
    public Structure at(Hex h){for(Structure s:structures)if(s.hex.equals(h))return s;return null;}
    public static String rankLabel(int rank){return new String[]{"C","B","A","S"}[Math.max(0,Math.min(3,rank))];}
    public int range(World.Unit u){return w.army.range(u)+(w.skills.has(u,Skill.SHECHENG)&&!w.army.water(u.hex)&&(u.weapon==World.Weapon.SIEGE_TOWER||u.weapon==World.Weapon.CATAPULT)?1:0)+(!w.army.water(u.hex)&&u.weapon==World.Weapon.CROSSBOW&&w.campaign.has(u.owner,Campaign.Tech.STRONG_BOW)?1:0);}
    public int movement(World.Unit u){return movementAt(u,u.hex);}
    public int movementAt(World.Unit u,Hex h){if(u instanceof Domestic.Mission)return 4+(w.campaign.has(u.owner,Campaign.Tech.WOODEN_OX)?1:0)+(w.skills.has(u,Skill.YUNBAN)?2:0);return (w.army.water(h)?u.ship.movement:u.weapon.movement)+w.skills.movementBonusAt(u,h)+(w.campaign.eliteUnitAt(u,h)?1:0)+(!w.army.water(h)&&Army.siegeWeapon(u.weapon)&&w.campaign.has(u.owner,Campaign.Tech.AXLE)?1:0)+(!w.army.water(h)&&u.weapon==World.Weapon.CAVALRY&&w.campaign.has(u.owner,Campaign.Tech.HORSE_BREEDING)?1:0);}
    private String actorError(World.Unit u){
        return u instanceof Domestic.Mission?"运输队只能行军、补给、入库或待命":w.orders.error(u);
    }
    private String targetError(World.Unit a,World.Unit b,int min,int max){
        if(b instanceof Domestic.Mission&&w.cityAt(b.hex)!=null)return "城内运输队受城防保护";
        if(b==null||!w.campaign.hostile(a.owner,b.owner))return "请选择交战势力的部队";
        if(!w.fieldworks.landTarget(a.owner,b.hex))return "需要难所行军才能攻击该地形上的目标";
        int distance=a.hex.distance(b.hex);return distance<min||distance>max?"敌军不在范围内":null;
    }
    private Random random(){return new Random(w.strategy.nextInt(Integer.MAX_VALUE));}
    private void hurt(World.Unit u,int damage){w.combatEffects.hit(null,u,damage,false,false);}
    // Inherited v0.8 engineering parameter; not a verified original-game formula.
    private static final int LEGACY_COLLISION_DAMAGE=100;
    void collision(World.Unit target,World.Unit source){
        if(w.unit(target.id)!=target)return;
        int damage=w.combatEffects.hit(source,target,LEGACY_COLLISION_DAMAGE,false,false);w.battleImpact(target.hex,false);
        w.battleOutcome(w.officer(target.officerId).name+"碰撞损失"+damage+"（工程参数）");
    }
    int strike(World.Unit a,World.Unit b,double scale,boolean tactic){
        w.battleImpact(b.hex,false);int amount=w.combatEffects.physical(a,b,scale,tactic);if(w.campaign.hostile(a.owner,b.owner))w.government.earn(a.officerId,amount/10);return amount;
    }
    public int previewDamage(int actor,int target){World.Unit a=w.unit(actor),b=w.unit(target);return a==null||b==null?0:w.combat.physicalDamage(a,b,1,false,new Random(0));}
    public String attackPreview(int actor,int target){
        String error=attackError(actor,target);if(error!=null)return error;
        World.Unit a=w.unit(actor),b=w.unit(target);
        String text=w.combat.preview(a,b,1,false).describe()+"\n"+w.energy.hitPreview(a,b);
        if(canCounter(a,b)&&b.status==Status.NORMAL)text+="\n反击：0–"+w.combat.preview(b,a,.5,false).max+"（受敌军存续兵力、免疫及回避影响）";
        else text+="\n当前目标不能反击。";
        if(w.skills.has(a,Skill.LIANZHAN))text+="\n连战：50%概率追加一次普攻。";
        return text;
    }
    public String attackError(int actor,int target){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=actorError(a);return error!=null?error:attackPositionError(a,b);
    }
    /** Pure position validation also used by the AI's detached movement probes. */
    String attackPositionError(World.Unit a,World.Unit b){
        String error=targetError(a,b,1,range(a));if(error!=null)return error;
        if(!w.army.canAttackUnit(a))return "兵器需要使用战法";
        if(!w.army.water(a.hex)&&a.weapon==World.Weapon.CROSSBOW&&w.terrain[b.hex.q][b.hex.r]==World.Terrain.FOREST&&!w.skills.has(a,Skill.SHESHOU))return "射向森林需要射手特技";
        return null;
    }
    public World.Result attack(int actor,int target){
        String error=attackError(actor,target);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unit(target);
        w.visualAction(TurnJournal.Kind.ATTACK,actor,b.hex,"攻击");
        w.marches.supersede(a);a.acted=true;int dealt=0,counter=0;
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
    boolean canCounter(World.Unit a,World.Unit b){
        if(!w.army.water(a.hex)&&a.weapon==World.Weapon.SPEAR&&w.terrain[a.hex.q][a.hex.r]==World.Terrain.FOREST&&w.campaign.has(a.owner,Campaign.Tech.FOREST_AMBUSH))return false;
        if(a.hex.distance(b.hex)==1&&w.army.counter(b))return true;
        return !w.army.water(b.hex)&&b.weapon==World.Weapon.CROSSBOW&&w.campaign.has(b.owner,Campaign.Tech.RETURN_FIRE)&&a.hex.distance(b.hex)<=range(b)&&
            (a.weapon==World.Weapon.CROSSBOW||a.hex.distance(b.hex)>1)&&
            (w.terrain[a.hex.q][a.hex.r]!=World.Terrain.FOREST||w.skills.has(b,Skill.SHESHOU));
    }
    private int supportAttack(World.Unit attacker,World.Unit target){
        if(w.unit(attacker.id)==null||w.unit(target.id)==null)return 0;int damage=0;
        List<World.Unit> helpers=new ArrayList<>(w.fieldUnits());helpers.sort(Comparator.comparingInt(u->u.id));
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
        if(a.weapon==World.Weapon.SPEAR&&w.terrain[a.hex.q][a.hex.r]==World.Terrain.SAND)return "枪兵位于沙地，不能施放战法；可普通攻击或移至其他地形";
        if(w.army.aptitude(a)<tactic.rank)return "需要"+rankLabel(tactic.rank)+"级兵科适性";
        if(a.energy<tactic.energy)return "气力不足";
        error=targetError(a,b,tactic.minRange,tactic.maxRange+(a.weapon==World.Weapon.CROSSBOW?range(a)-a.weapon.range:0));if(error!=null)return error;
        if(a.weapon==World.Weapon.CAVALRY&&(w.terrain[a.hex.q][a.hex.r]==World.Terrain.FOREST||w.terrain[b.hex.q][b.hex.r]==World.Terrain.FOREST))return "骑兵战法不能在森林使用";
        if(a.weapon==World.Weapon.CROSSBOW&&w.terrain[b.hex.q][b.hex.r]==World.Terrain.FOREST&&!w.skills.has(a,Skill.SHESHOU))return "射向森林需要射手特技";
        if(tactic==Tactic.PIERCE&&direction(a.hex,b.hex)==null)return "贯射需要直线目标";
        if(w.army.water(b.hex)&&(tactic==Tactic.HOOK||tactic==Tactic.THRUST||tactic==Tactic.DOUBLE_THRUST||tactic==Tactic.CHARGE||tactic==Tactic.ADVANCE||tactic==Tactic.BREAKTHROUGH))return "位移战法不能跨越水陆边界";
        return displacement.requiredError(a,b,Displacement.kind(tactic));
    }
    private List<World.Unit> tacticVictims(World.Unit a,World.Unit b,Tactic tactic){
        Hex origin=a.hex,targetHex=b.hex;List<World.Unit> victims=new ArrayList<>();victims.add(b);
        for(World.Unit u:new ArrayList<>(w.fieldUnits()))if(u.id!=b.id&&u.id!=a.id&&(w.campaign.hostile(a.owner,u.owner)||tactic==Tactic.VOLLEY&&u.owner==a.owner&&!w.skills.has(a,Skill.GONGSHEN))){
            boolean splash=tactic==Tactic.WHIRLWIND&&origin.distance(u.hex)==1
                ||tactic==Tactic.SWEEP&&origin.distance(u.hex)==1&&targetHex.distance(u.hex)==1
                ||tactic==Tactic.VOLLEY&&targetHex.distance(u.hex)<=1;
            int[] ray=direction(origin,targetHex);if(tactic==Tactic.PIERCE&&ray!=null&&u.hex.equals(add(targetHex,ray[0],ray[1])))splash=true;
            if(splash)victims.add(u);
        }
        return victims;
    }
    public Displacement.Preview tacticPreview(int actor,int target,Tactic tactic){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=tacticError(actor,target,tactic);
        String heading=tactic==null?"未选择战法":tactic.label+" · 消耗气力"+tactic.energy+" / 当前"+(a==null?0:a.energy)+"\n命中率"+tacticChance(actor,target,tactic)+"%；命中或失败均结束本旬行动，失败同样扣气力。";
        if(a!=null&&tactic==Tactic.FIRE_ARROW)heading+="\n"+w.combat.firePreview(a,b,CombatRules.DIRECT_FIRE_BASE,false);
        if(b!=null)heading+="\n实际目标："+w.officer(b.officerId).name+" · "+b.hex;
        if(a!=null&&b!=null&&tactic==Tactic.SPIRAL)heading+="\n命中后混乱概率："+w.combat.spiralConfusionChance(a,b)+"%";
        if(tactic!=null)heading+="\n射程："+tactic.minRange+"–"+(tactic.maxRange+(a!=null&&a.weapon==World.Weapon.CROSSBOW?range(a)-a.weapon.range:0))+"格；效果："+tactic.effect;
        Displacement.Preview p=displacement.preview(a,b,Displacement.kind(tactic),error,heading);
        if(error!=null)return p;
        p=new Displacement.Preview(null,p.text+"\n"+w.combat.preview(a,b,tactic.multiplier,true).describe()+"\n"+w.energy.hitPreview(a,b),p.actorPath,p.targetPath,p.riskHexes,p.blocked,p.friendlyRisk);
        List<Hex> risks=new ArrayList<>(p.riskHexes);boolean friendly=p.friendlyRisk;StringBuilder text=new StringBuilder(p.text);
        List<World.Unit> victims=tacticVictims(a,b,tactic);if(victims.size()>1){text.append("\n命中时波及：");for(World.Unit v:victims){text.append(w.officer(v.officerId).name).append("@").append(v.hex).append(" ");risks.add(v.hex);if(v.owner==a.owner)friendly=true;}}
        if(friendly&&!p.friendlyRisk)text.append("\n警示：范围伤害会波及己方部队。");
        return new Displacement.Preview(null,text.toString(),p.actorPath,p.targetPath,risks,p.blocked,friendly);
    }
    public World.Result tactic(int actor,int target,Tactic tactic){
        String error=tacticError(actor,target,tactic);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unit(target);w.visualAction(TurnJournal.Kind.TACTIC,actor,b.hex,tactic.label);int chance=tacticChance(actor,target,tactic);w.marches.supersede(a);a.acted=true;w.energy.change(a,-tactic.energy,EnergyRules.Reason.COMMAND);w.battleImpact(b.hex,false);
        if(w.strategy.nextInt(100)>=chance)return w.success(w.officer(a.officerId).name+"的"+tactic.label+"未命中，消耗气力"+tactic.energy+"，本旬行动结束");
        Hex origin=a.hex,targetHex=b.hex;List<World.Unit> victims=tacticVictims(a,b,tactic);
        double multiplier=tactic.multiplier;
        int dealt=0;for(World.Unit victim:victims){if(w.unit(a.id)!=a||w.unit(victim.id)!=victim)continue;int hit=strike(a,victim,multiplier,true);dealt+=hit;}
        switch(tactic){
            case THRUST:case DOUBLE_THRUST:case CHARGE:case ADVANCE:case HOOK:case BREAKTHROUGH:displacement.execute(a,b,origin,targetHex,Displacement.kind(tactic));break;
            case SPIRAL:if(w.unit(b.id)!=null&&w.strategy.nextInt(100)<w.combat.spiralConfusionChance(a,b)){b.status=Status.CONFUSED;b.statusTurns=w.combat.critical(a,b,true)?2:1;}break;
            case FIRE_ARROW:ignite(targetHex,a);break;default:break;
        }
        if(w.unit(a.id)==a&&w.unit(b.id)==b&&(!a.hex.equals(origin)||!b.hex.equals(targetHex)))w.skills.woundAfterDisplacement(a,b);
        if(w.unit(a.id)==a&&w.unit(b.id)==b&&a.weapon==World.Weapon.CAVALRY&&w.skills.swiftConfusion(a,b)){b.status=Status.CONFUSED;b.statusTurns=1;}
        w.campaign.earn(a.owner,w.unit(b.id)==null&&w.skills.has(a,Skill.JINGMIAO)?80:40);w.checkVictory();return w.success(w.officer(a.officerId).name+"施展"+tactic.label+"，命中"+victims.size()+"队，主伤害"+dealt+"，消耗气力"+tactic.energy+"，本旬行动结束");
    }
    private static Hex add(Hex h,int q,int r){return new Hex(h.q+q,h.r+r);}
    private static int[] direction(Hex a,Hex b){
        int distance=a.distance(b);if(distance==0)return null;
        int dq=b.q-a.q,dr=b.r-a.r;if(dq%distance!=0||dr%distance!=0)return null;
        Hex step=new Hex(dq/distance,dr/distance);return new Hex(0,0).distance(step)==1?new int[]{step.q,step.r}:null;
    }
    private boolean vacant(Hex h,World.Weapon weapon){return w.cost(h,weapon)>0&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&at(h)==null;}
    public int plotChance(int actor,Hex target,Plot plot){
        World.Unit a=w.unit(actor),b=target==null?null:w.unitAt(target);if(a==null||plot==null)return 0;
        if(w.advancedBattle.magic(plot))return w.advancedBattle.magicChance(a,b,plot);
        if(plot==Plot.CALM||plot==Plot.EXTINGUISH)return 100;
        int defense=b==null?50:w.army.intelligence(b);
        return w.skills.plotChance(a,b,plot,Math.max(10,Math.min(95,65+(w.army.intelligence(a)-defense)/2)));
    }
    public String plotError(int actor,Hex target,Plot plot){
        World.Unit a=w.unit(actor);String error=actorError(a);if(error!=null)return error;
        if(!w.advancedBattle.unlocked(a,plot))return "部队尚未掌握该计略所需特技";
        if(plot==null||target==null||!w.inside(target)||a.hex.distance(target)>w.skills.plotRange(a,plot))return "请选择计略范围内目标";
        if(a.energy<w.skills.plotCost(a,plot))return "气力不足";World.Unit b=w.unitAt(target);
        if(plot==Plot.EXTINGUISH)return fireAt(target)==null?"目标没有火焰":null;
        if(plot==Plot.CALM)return b==null||b.owner!=a.owner||b.status==Status.NORMAL?"请选择异常状态的己方部队":null;
        if(plot==Plot.LIGHTNING){
            if(w.terrain[target.q][target.r]==World.Terrain.MOUNTAIN)return "不能在崖上落雷";
            World.City c=w.cityAt(target);Structure s=at(target);Domestic.Facility f=w.domestic.at(target);
            int owner=b!=null?b.owner:c!=null?c.owner:s!=null?s.owner:f!=null?w.city(f.cityId).owner:a.owner;
            return owner!=a.owner&&!w.campaign.hostile(a.owner,owner)?"不能对协定势力施展落雷":null;
        }
        if(plot==Plot.FIRE){
            if((w.cost(target,World.Weapon.SPEAR)<0&&!(at(target)!=null&&at(target).kind==StructureKind.FIRE_SHIP))||w.cityAt(target)!=null)return "此地无法放火";
            Domestic.Facility facility=w.domestic.at(target);if(facility!=null&&!w.campaign.hostile(a.owner,w.city(facility.cityId).owner))return "不能对己方或协定设施放火";
            Structure s=at(target);if(b!=null&&!w.campaign.hostile(a.owner,b.owner)||s!=null&&s.owner!=a.owner&&!w.campaign.hostile(a.owner,s.owner))return "不能对友军或协定势力放火";
            return fireAt(target)!=null?"目标已起火":null;
        }
        error=targetError(a,b,1,w.skills.plotRange(a,plot));if(error!=null)return error;
        if(plot==Plot.INFIGHT&&w.advancedBattle.infightingTargets(b).isEmpty())return "需要目标邻接另一支同势力非兵器部队";
        if(plot==Plot.AMBUSH&&(w.army.water(a.hex)||a.weapon==World.Weapon.CAVALRY||Army.siegeWeapon(a.weapon)))return "当前兵科不能施展伏兵";
        if(plot==Plot.AMBUSH&&w.terrain[a.hex.q][a.hex.r]!=World.Terrain.FOREST)return "伏兵需要自身位于森林";
        if((plot==Plot.CONFUSE||plot==Plot.MISLEAD)&&b.status!=Status.NORMAL)return "目标已经处于异常状态";return null;
    }
    public int plotCost(int actor,Plot plot){World.Unit a=w.unit(actor);return a==null||plot==null?0:w.skills.plotCost(a,plot);}
    public int plotRange(int actor,Plot plot){World.Unit a=w.unit(actor);return a==null||plot==null?0:w.skills.plotRange(a,plot);}
    public World.Result plot(int actor,Hex target,Plot plot){
        String error=plotError(actor,target,plot);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unitAt(target);w.visualAction(TurnJournal.Kind.PLOT,actor,target,plot.label);a.acted=true;w.energy.change(a,-plotCost(actor,plot),EnergyRules.Reason.COMMAND);
        boolean success=resolvePlot(a,b,target,plot,true);
        if(success&&b!=null&&w.skills.has(a,Skill.LIANHUAN)&&(plot==Plot.CONFUSE||plot==Plot.MISLEAD||plot==Plot.FIRE)){
            List<World.Unit> adjacent=new ArrayList<>();
            for(World.Unit u:w.fieldUnits())if(u.id!=b.id&&w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(target)==1&&
                (plot==Plot.FIRE?fireAt(u.hex)==null&&!w.army.water(u.hex):u.status==Status.NORMAL))adjacent.add(u);
            adjacent.sort(Comparator.comparingInt(u->u.id));
            if(!adjacent.isEmpty()){World.Unit chained=adjacent.get(0);resolvePlot(a,chained,chained.hex,plot,false);}
        }
        w.campaign.earn(a.owner,success?25:0);w.checkVictory();
        return w.success(w.officer(a.officerId).name+"施展"+plot.label+(success?"":"被识破，气力已消耗"));
    }
    /** A reflected/chained plot pays no second cost and cannot recurse into another reflection/chain. */
    private boolean resolvePlot(World.Unit a,World.Unit b,Hex target,Plot plot,boolean reflection){
        if(w.advancedBattle.magic(plot))return w.advancedBattle.cast(a,b,target,plot,reflection);
        int chance=plotChance(a.id,target,plot);
        boolean success=chance==100||chance>0&&w.strategy.nextInt(100)<chance;
        if(!success){
            if(reflection&&b!=null&&w.skills.has(b,Skill.FANJI)&&(plot==Plot.CONFUSE||plot==Plot.MISLEAD)&&a.status==Status.NORMAL)
                resolvePlot(b,a,a.hex,plot,false);
            return false;
        }
        boolean critical=w.skills.plotCritical(a,b,plot);
        switch(plot){
            case INFIGHT:w.advancedBattle.infight(a,b,critical);break;
            case FIRE:ignite(target,a);Fire created=fireAt(target);if(critical&&created!=null)created.remaining++;break;
            case EXTINGUISH:
                fires.removeIf(f->f.hex.equals(target)||critical&&f.hex.distance(target)==1);break;
            case CONFUSE:b.status=Status.CONFUSED;b.statusTurns=critical?2:1;break;
            case MISLEAD:b.status=Status.MISLED;b.statusTurns=critical?2:1;break;
            case CALM:
                for(World.Unit u:w.fieldUnits())if(u.owner==a.owner&&(u.id==b.id||critical&&u.hex.distance(target)==1)){u.status=Status.NORMAL;u.statusTurns=0;}break;
            case AMBUSH:
                int hit=Math.min(b.troops,w.combat.rawDamage(a,b,critical?1.35*1.15:1.35,random()));hurt(b,hit);
                if(w.unit(b.id)!=null){w.energy.change(b,-15,EnergyRules.Reason.AMBUSH);if(critical){b.status=Status.CONFUSED;b.statusTurns=1;}}break;
        }
        return true;
    }
    void ignite(Hex target,World.Unit source){w.fieldworks.ignite(target,source);}
    /** Legacy city-based fixture helper. Android gameplay uses Fieldworks.build with unit gold. */
    @Deprecated
    public List<Hex> buildSites(int city){
        World.City c=w.city(city);List<Hex> result=new ArrayList<>();if(c==null)return result;
        for(int q=Math.max(0,c.hex.q-3);q<=Math.min(w.width-1,c.hex.q+3);q++)for(int r=Math.max(0,c.hex.r-3);r<=Math.min(w.height-1,c.hex.r+3);r++){
            Hex h=new Hex(q,r);if(c.hex.distance(h)<=3&&c.hex.distance(h)>=2&&!Fieldworks.blockedMilitaryTerrain(w.terrain[q][r])&&vacant(h,World.Weapon.SPEAR)&&fireAt(h)==null&&w.cities.stream().noneMatch(other->other.hex.distance(h)<=1))result.add(h);
        }return result;
    }
    public World.Result build(int city,int officer,StructureKind kind,Hex h){
        if(kind==null)return w.fail("设施类型无效");World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,kind.gold);if(error!=null)return w.fail(error);
        if(structures.size()>=1000||nextStructureId>=10000000)return w.fail("军事设施已达上限");
        if(!buildSites(city).contains(h))return w.fail("请选择城池两至三格内空地，不可堵塞城池出口");
        w.spend(c,o,kind.gold);structures.add(new Structure(nextStructureId++,c.owner,kind,h,kind.hp));return w.success(c.name+"建造"+kind.label);
    }
    public String facilityAttackError(int unit,Hex h){
        World.Unit u=w.unit(unit);String error=actorError(u);if(error!=null)return error;
        return facilityAttackPositionError(u,h);
    }
    String facilityAttackPositionError(World.Unit u,Hex h){
        Domestic.Facility f=w.domestic.at(h);
        if(f==null||!w.campaign.hostile(u.owner,w.city(f.cityId).owner))return "请选择敌方内政设施";
        if(u.hex.distance(h)<1||u.hex.distance(h)>range(u))return "设施不在攻击射程内";
        if(!w.army.canAttackUnit(u))return "兵器需要使用战法";
        return null;
    }
    public int facilityDamage(int unit){World.Unit u=w.unit(unit);return u==null?0:w.combat.structureDamage(u,false);}
    public World.Result attackFacility(int unit,Hex h){
        String error=facilityAttackError(unit,h);if(error!=null)return w.fail(error);
        World.Unit u=w.unit(unit);Domestic.Facility f=w.domestic.at(h);w.visualAction(TurnJournal.Kind.ATTACK,unit,h,"攻击设施");w.marches.supersede(u);u.acted=true;
        int amount=w.domestic.damage(f,facilityDamage(unit));w.battleImpact(h,f.hp==0);w.campaign.earn(u.owner,20);
        return w.success("攻击"+f.kind.label+"，耐久减少"+amount+"，剩余"+f.hp+"/"+f.maxHp());
    }
    public String structureAttackError(int unit,Hex h){
        World.Unit u=w.unit(unit);String error=actorError(u);if(error!=null)return error;
        String position=structureAttackPositionError(u,h);
        if(position!=null&&!w.army.canAttackUnit(u)){
            List<Army.Tactic> tactics=w.army.tactics(u);
            return tactics.isEmpty()?position:w.army.tacticPositionError(u,h,tactics.get(0));
        }
        return position;
    }
    String structureAttackPositionError(World.Unit u,Hex h){
        Structure s=at(h);
        if(s==null||(s.kind!=StructureKind.DAM&&!w.campaign.hostile(u.owner,s.owner))||u.hex.distance(h)>range(u)||u.hex.equals(h))return "请选择射程内敌方军事设施";
        if(!w.army.canAttackUnit(u))return "兵器需要使用战法";
        return null;
    }
    public World.Result attackStructure(int unit,Hex h){
        String error=structureAttackError(unit,h);if(error!=null)return w.fail(error);World.Unit u=w.unit(unit);Structure s=at(h);w.visualAction(TurnJournal.Kind.ATTACK,unit,h,"攻击工事");
        if(!w.army.canAttackUnit(u))return w.army.tactic(unit,h,w.army.tactics(u).get(0));
        w.marches.supersede(u);int damage=Math.min(s.hp,w.combat.structureDamage(u,false));u.acted=true;s.hp-=damage;
        w.battleImpact(h,s.hp<=0);if(s.hp<=0){w.fieldworks.destroy(s);w.battleOutcome(s.kind.label+"已摧毁，地块已释放");}else w.fieldworks.counter(s,u);w.campaign.earn(u.owner,20);return w.success("攻击"+s.kind.label+"，耐久减少"+damage);
    }
    public World.Result removeStructure(int city,int officer,int id){
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,0);if(error!=null)return w.fail(error);
        Structure s=null;for(Structure item:structures)if(item.id==id)s=item;
        if(s==null||s.owner!=c.owner||s.hex.distance(c.hex)>3)return w.fail("请选择本城三格内己方军事设施");
        w.spend(c,o,0);w.fieldworks.destroy(s);return w.success("已拆除"+s.kind.label);
    }
    public World.Result waitUnit(int unit){World.Unit u=w.unit(unit);String error=actorError(u);if(error!=null)return w.fail(error);w.marches.supersede(u);u.acted=true;w.energy.change(u,5,EnergyRules.Reason.WAIT);return w.success("部队待命，恢复5气力");}
    void resetOwner(int owner){
        for(World.Unit u:new ArrayList<>(w.fieldUnits()))if(u.owner==owner&&u.status!=Status.NORMAL){
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
            World.Unit u=w.unitAt(f.hex);if(u!=null&&(u.owner==f.owner||w.campaign.hostile(f.owner,u.owner))){int hit=250+(w.terrain[f.hex.q][f.hex.r]==World.Terrain.FOREST?150:0);hit=w.combat.ongoingFireDamage(u,hit,f.owner,f.power,f.trap);hurt(u,hit);w.note("火场灼烧，部队损失"+hit);}
            Structure s=at(f.hex);if(s!=null&&(s.owner==f.owner||w.campaign.hostile(f.owner,s.owner))){s.hp-=200;if(s.hp<=0)w.fieldworks.destroy(s);}
            Domestic.Facility facility=w.domestic.at(f.hex);if(facility!=null&&(w.city(facility.cityId).owner==f.owner||w.campaign.hostile(f.owner,w.city(facility.cityId).owner)))w.domestic.damage(facility,200);
            if(--f.remaining==0)fires.remove(f);
        }
        for(Structure s:new ArrayList<>(structures)){
            if(s.kind!=StructureKind.DAM&&!w.alive(s.owner)){w.fieldworks.destroy(s);continue;}
            // Music restoration is deduplicated per unit below, including skill priority.

        }
        w.fieldworks.towers();w.energy.settleTurn();
    }
}
