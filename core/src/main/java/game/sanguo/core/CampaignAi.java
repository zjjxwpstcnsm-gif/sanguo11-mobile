package game.sanguo.core;

import java.util.*;
import java.util.function.Predicate;

/** Deterministic, resource-constrained planning. Scores are engineering policy, not SAN11 formulas.
 * Planning never consumes world RNG, changes state, or grants resources. Commands remain authoritative. */
public final class CampaignAi {
    public enum Kind { ATTACK, TACTIC, ARMY_TACTIC, PLOT, SIEGE, STRUCTURE, FACILITY, RAID, CAMP, EXTINGUISH, JOINT }
    public static final class Action {
        public final Kind kind;
        public final int actor, target, score;
        public final Hex hex;
        public final War.Tactic tactic;
        public final Army.Tactic armyTactic;
        public final War.Plot plot;
        public final String reason;
        private Action(Kind kind,int actor,int target,Hex hex,int score,War.Tactic t,Army.Tactic at,War.Plot p,String reason){
            this.kind=kind;this.actor=actor;this.target=target;this.hex=hex;this.score=score;this.tactic=t;this.armyTactic=at;this.plot=p;this.reason=reason;
        }
    }
    public static final class Deployment {
        public final int city, leader, troops, food, reserve;
        public final World.Weapon weapon;
        public final Army.Ship ship;
        private final int[] deputies;
        private Deployment(int city,int leader,int troops,int food,int reserve,World.Weapon weapon,Army.Ship ship,int[] deputies){
            this.city=city;this.leader=leader;this.troops=troops;this.food=food;this.reserve=reserve;this.weapon=weapon;this.ship=ship;this.deputies=deputies.clone();
        }
        public int[] deputies(){return deputies.clone();}
    }
    private final World w;
    private Route selectedRoute;
    private boolean actedProductively,formationWait;
    public CampaignAi(World w){this.w=Objects.requireNonNull(w);}
    private List<World.Unit> units(){List<World.Unit> out=new ArrayList<>(w.fieldUnits());out.sort(Comparator.comparingInt(u->u.id));return out;}
    private List<World.City> cities(){List<World.City> out=new ArrayList<>(w.cities);out.sort(Comparator.comparingInt(c->c.id));return out;}
    private List<World.Officer> idle(World.City c){List<World.Officer> out=w.idle(c);out.sort(Comparator.comparingInt(o->o.id));return out;}
    private Action action(Kind k,World.Unit u,int target,Hex h,int score,String reason){return new Action(k,u.id,target,h,score,null,null,null,reason);}
    private Action better(Action a,Action b){
        // Candidates are enumerated by stable IDs and enums; equal scores retain the first.
        return b!=null&&b.score>0&&(a==null||b.score>a.score)?b:a;
    }
    private int value(World.Unit target,int loss){return (target instanceof Domestic.Mission?Math.min(1500,target.gold/5+target.food/50):0)+loss+(loss>=target.troops?900:0)+(Army.siegeWeapon(target.weapon)?loss/4:0);}
    private int controlValue(World.Unit u){return u.status==War.Status.NORMAL?250+Math.min(700,u.troops/10):0;}
    private boolean splash(War.Tactic t,Hex origin,Hex center,Hex h){
        if(t==War.Tactic.WHIRLWIND)return origin.distance(h)==1;
        if(t==War.Tactic.SWEEP)return origin.distance(h)==1&&center.distance(h)==1;
        if(t==War.Tactic.VOLLEY)return center.distance(h)<=1;
        if(t==War.Tactic.PIERCE){int d=origin.distance(center);return d>0&&h.equals(new Hex(center.q+(center.q-origin.q)/d,center.r+(center.r-origin.r)/d));}
        return false;
    }
    private boolean ownAssetsNear(World.Unit actor,Hex center){
        for(World.Unit u:w.units)if(u.owner==actor.owner&&u.hex.distance(center)<=1)return true;
        for(World.City c:w.cities)if(c.owner==actor.owner&&c.hex.distance(center)<=1)return true;
        for(Domestic.Facility f:w.domestic.facilities)if(w.city(f.cityId).owner==actor.owner&&f.hex.distance(center)<=1)return true;
        for(War.Structure s:w.war.structures())if(s.owner==actor.owner&&s.hex.distance(center)<=1)return true;
        return false;
    }
    private int plotValue(World.Unit a,World.Unit b,War.Plot p){
        int amount=0;
        switch(p){
            case CONFUSE: case MISLEAD: amount=controlValue(b);break;
            case SORCERY:
                for(World.Unit u:w.units)if(w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(b.hex)<=1&&!w.skills.plotImmune(a,u,p))amount+=controlValue(u);
                break;
            case LIGHTNING:
                if(ownAssetsNear(a,b.hex))return -1;
                for(World.Unit u:w.units)if(w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(b.hex)<=1&&!w.skills.plotImmune(a,u,p))amount+=value(u,Math.min(u.troops,500+w.army.intelligence(a)*12));
                break;
            case INFIGHT:
                List<World.Unit> others=w.advancedBattle.infightingTargets(b);
                for(World.Unit u:others)amount+=value(u,w.combat.expectedDamage(b,u,1,false))+value(b,w.combat.expectedDamage(u,b,.5,false));
                if(!others.isEmpty())amount/=others.size();break;
            case AMBUSH: amount=value(b,w.combat.expectedDamage(a,b,1.35,true))+100;break;
            case FIRE:
                // Avoid an unmodeled trap cascade hitting friendlies.
                if(w.war.at(b.hex)!=null)return -1;
                amount=w.combat.fireDamage(b,CombatRules.DIRECT_FIRE_BASE,a.owner,w.combat.firePower(a),false);break;
            default:return -1;
        }
        int chance=w.war.plotChance(a.id,b.hex,p);
        if(w.skills.has(a,Skill.LIANHUAN)&&(p==War.Plot.CONFUSE||p==War.Plot.MISLEAD||p==War.Plot.FIRE)){
            for(World.Unit u:units())if(u.id!=b.id&&w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(b.hex)==1&&
                (p==War.Plot.FIRE?w.war.fireAt(u.hex)==null&&!w.army.water(u.hex):u.status==War.Status.NORMAL)){
                int chained=p==War.Plot.FIRE?w.combat.fireDamage(u,CombatRules.DIRECT_FIRE_BASE,a.owner,w.combat.firePower(a),false):controlValue(u);
                amount+=chained*w.war.plotChance(a.id,u.hex,p)/100;break;
            }
        }
        int reflection=w.skills.has(b,Skill.FANJI)&&(p==War.Plot.CONFUSE||p==War.Plot.MISLEAD)?controlValue(a)*(100-chance)/100:0;
        return amount*chance/100-w.skills.plotCost(a,p)*8-reflection;
    }
    /** Legal current-position actions across all targets, including rescue, with no RNG consumption. */
    public Action bestAction(int unit,boolean attack){
        return bestAction(unit,attack,c->true);
    }
    private Action bestAction(int unit,boolean attack,Predicate<World.City> objectives){return w.fieldworks.queryAuras(()->evaluateActions(unit,attack,objectives));}
    private Action evaluateActions(int unit,boolean attack,Predicate<World.City> objectives){
        World.Unit a=w.unit(unit);if(w.orders.error(a)!=null)return null;Action best=null;
        if(a.burning>0&&a.energy>=5)best=action(Kind.EXTINGUISH,a,-1,a.hex,Math.min(a.troops,400),"扑灭部队持续燃烧");
        for(World.Unit b:units()){
            if(b.owner==a.owner){
                if(w.war.plotError(a.id,b.hex,War.Plot.CALM)==null){int rescue=controlValueNormal(b)+(b.acted?0:300);
                    best=better(best,new Action(Kind.PLOT,a.id,b.id,b.hex,rescue,null,null,War.Plot.CALM,"优先恢复友军行动能力"));}
                if(w.war.fireAt(b.hex)!=null&&w.war.plotError(a.id,b.hex,War.Plot.EXTINGUISH)==null)
                    best=better(best,new Action(Kind.PLOT,a.id,b.id,b.hex,350,null,null,War.Plot.EXTINGUISH,"熄灭友军脚下火场"));
                continue;
            }
            if(!attack||!w.campaign.hostile(a.owner,b.owner))continue;
            if(w.war.attackError(a.id,b.id)==null){
                int hit=w.combat.expectedDamage(a,b,1,false),counter=b.status==War.Status.NORMAL&&w.war.canCounter(a,b)&&hit<b.troops?w.combat.expectedDamage(b,a,.5,false):0;
                best=better(best,action(Kind.ATTACK,a,b.id,b.hex,value(b,hit)-counter,"比较伤害、歼灭收益与反击损失"));
            }
            if(!w.skills.has(b,Skill.TIEBI)&&w.advancedBattle.jointError(a.id,b.id)==null){
                int hit=0,opportunity=0;boolean flank=false;
                for(World.Unit helper:w.advancedBattle.jointParticipants(a.id,b.id)){
                    hit+=w.combat.expectedDamage(helper,b,.65,false);flank|=w.skills.has(helper,Skill.JIJIAO);
                    if(helper.id!=a.id)opportunity+=w.combat.expectedDamage(helper,b,1,false)*3/4;
                }
                int score=value(b,Math.min(hit,b.troops))-opportunity+(flank?controlValue(b)/2:0);
                best=better(best,action(Kind.JOINT,a,b.id,b.hex,score,"比较齐攻歼灭收益与协攻部队行动代价"));
            }
            for(War.Tactic t:War.Tactic.values())if(w.war.tacticError(a.id,b.id,t)==null){
                if(w.war.tacticPreview(a.id,b.id,t).friendlyRisk)continue;
                int hit=value(b,w.combat.expectedWithFire(a,b,t.multiplier,t==War.Tactic.FIRE_ARROW));boolean friendly=false;
                for(World.Unit other:units())if(other.id!=b.id&&other.id!=a.id&&splash(t,a.hex,b.hex,other.hex)){
                    if(t==War.Tactic.VOLLEY&&other.owner==a.owner&&!w.skills.has(a,Skill.GONGSHEN)){friendly=true;break;}
                    if(w.campaign.hostile(a.owner,other.owner))hit+=value(other,w.combat.expectedDamage(a,other,t.multiplier,true));
                }
                if(friendly)continue;
                if(t==War.Tactic.SPIRAL)hit+=controlValue(b)*w.combat.spiralConfusionChance(a,b)/100;
                int score=hit*w.war.tacticChance(a.id,b.id,t)/100-t.energy*8;
                best=better(best,new Action(Kind.TACTIC,a.id,b.id,b.hex,score,t,null,null,"按命中与范围收益选择战法"));
            }
            for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,b.hex,t)==null){
                if(w.army.tacticPreview(a.id,b.hex,t).friendlyRisk)continue;
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,b.hex))continue;
                int score=value(b,w.combat.expectedWithFire(a,b,t==Army.Tactic.STONE?1.5:1.3,t==Army.Tactic.FIRE_ARROW||t==Army.Tactic.FLAME))*w.army.tacticChance(a.id,b.hex)/100-t.energy*8;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,b.id,b.hex,score,null,t,null,"兵器与水军选择有效战法"));
            }
            for(War.Plot p:War.Plot.values())if(p!=War.Plot.CALM&&p!=War.Plot.EXTINGUISH&&w.war.plotError(a.id,b.hex,p)==null)
                best=better(best,new Action(Kind.PLOT,a.id,b.id,b.hex,plotValue(a,b,p),null,null,p,"按免疫、命中、反计和友军损失选择计略"));
        }
        if(!attack)return best;
        for(World.City c:cities())if(w.campaign.hostile(a.owner,c.owner)&&objectives.test(c)){
            CombatRules.SiegeDamage normal=w.combat.siege(a,c,false),tactic=w.combat.siege(a,c,true);
            int score=150+Math.min(c.defense,normal.wall)+Math.min(c.troops,normal.troops)/2+(c.troops<=normal.troops||c.defense<=normal.wall?1800:0);
            if(w.siegeError(a.id,c.id)==null)best=better(best,action(Kind.SIEGE,a,c.id,c.hex,score,"夺取可占领据点"));
            for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,c.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,c.hex))continue;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,c.id,c.hex,150+(c.troops<=tactic.troops||c.defense<=tactic.wall?1800:0)+tactic.wall+tactic.troops/2-t.energy*5,null,t,null,"使用兵器战法削减城防和守军"));
            }
        }
        for(War.Structure s:w.war.structures())if(w.campaign.hostile(a.owner,s.owner)&&a.hex.distance(s.hex)<=w.war.range(a)){
            int score=200+Math.min(s.hp,w.combat.structureDamage(a,false))+(s.complete?100:0);
            if(w.army.canAttackUnit(a)&&w.war.structureAttackError(a.id,s.hex)==null)best=better(best,action(Kind.STRUCTURE,a,s.id,s.hex,score,"清除敌方设施与通路阻挡"));
            else for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,s.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,s.hex))continue;
                if((t==Army.Tactic.FIRE_ARROW||t==Army.Tactic.FLAME)&&w.fieldworks.trap(s.kind))continue;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,s.id,s.hex,200+Math.min(s.hp,w.combat.structureDamage(a,true))+(s.complete?100:0)-t.energy*5,null,t,null,"用合法兵器战法拆除设施并避开友伤"));
            }
        }
        for(Domestic.Facility f:w.domestic.facilities)if(w.campaign.hostile(a.owner,w.city(f.cityId).owner)&&objectives.test(w.city(f.cityId))){
            int score=150+Math.min(f.hp,w.combat.structureDamage(a,false));
            if(w.war.facilityAttackError(a.id,f.hex)==null)best=better(best,action(Kind.FACILITY,a,f.id,f.hex,score,"破坏敌方内政并清除道路阻挡"));
            else for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,f.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,f.hex))continue;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,f.id,f.hex,150+Math.min(f.hp,w.combat.structureDamage(a,true))-t.energy*5,null,t,null,"兵器拆除敌方内政设施"));
            }
        }
        for(Domestic.Mission m:w.domestic.missions)if(w.supply.raidError(a.id,m.id)==null){int hit=w.supply.raidDamage(a.id,m.id);
            best=better(best,action(Kind.RAID,a,m.id,m.hex,hit+(hit>=m.troops?Math.min(2000,m.food/20)+300:0),"截击有价值的运输补给"));}
        for(WorldEvents.Camp camp:w.events.camps())if(w.events.attackError(a.id,camp.id)==null)
            best=better(best,action(Kind.CAMP,a,camp.id,camp.hex,400,"清除持续劫掠的营寨"));
        return best;
    }
    private int controlValueNormal(World.Unit u){return 250+Math.min(700,u.troops/10);}
    public World.Result execute(Action a){World.Result result=executeAction(a);if(result.ok)actedProductively=true;return result;}
    private World.Result executeAction(Action a){
        if(a==null)return w.fail("暂无可执行战术");
        switch(a.kind){
            case ATTACK:return w.attack(a.actor,a.target);
            case TACTIC:return w.war.tactic(a.actor,a.target,a.tactic);
            case ARMY_TACTIC:return w.army.tactic(a.actor,a.hex,a.armyTactic);
            case PLOT:return w.war.plot(a.actor,a.hex,a.plot);
            case SIEGE:return w.siege(a.actor,a.target);
            case STRUCTURE:return w.war.attackStructure(a.actor,a.hex);
            case FACILITY:return w.war.attackFacility(a.actor,a.hex);
            case RAID:return w.supply.raid(a.actor,a.target);
            case CAMP:return w.events.attack(a.actor,a.target);
            case EXTINGUISH:return w.army.extinguish(a.actor);
            case JOINT:return w.advancedBattle.joint(a.actor,a.target);
            default:throw new AssertionError(a.kind);
        }
    }
    public int incoming(World.City c){int troops=0;for(World.Unit u:w.units)if(w.campaign.hostile(c.owner,u.owner)&&u.hex.distance(c.hex)<=7)troops+=u.troops;return troops;}
    public int reserve(World.City c){return Math.min(w.campaign.troopCap(c),Math.max(6000,incoming(c)*2/3+4000));}
    public int foodTurns(World.Unit u){int use=w.fieldworks.foodUse(u,Math.max(1,(u.troops+19)/20));return use==0?999:u.food/use;}
    private int admin(World.Officer o){return o.politics*2+o.charm;}
    private int combatSkill(Skill s,int category){
        if(s==null)return 0;
        switch(s){
            case SHENSUAN:case XUSHI:case DONGCHA:case HUOSHEN:return 90;
            case BAICHU:case LIANHUAN:case GUIMOU:case KANPO:case FANJI:case MINGJING:return 55;
            case GUIMEN:case YAOSHU:case SHENMOU:case JILUE:case YANDU:case GUIJI:return 60;
            case BAWANG:case SHENJIANG:case YONGJIANG:case FEIJIANG:case MENGZHE:case LIANZHAN:return 70;
            case QIANGSHEN:case QIANGJIANG:return category==0?80:0;
            case JISHEN:case JIJIANG:return category==1?80:0;
            case GONGSHEN:case GONGJIANG:case SHECHENG:case SHESHOU:return category==2?80:0;
            case QISHEN:case QIJIANG:case JICHI:case BAIMA:return category==3?80:0;
            case GONGSHEN_SIEGE:case GONGCHENG:return category==4?80:0;
            case SHUISHEN:case SHUIJIANG:return category==5?80:0;
            case DOUSHEN:return category<=1?80:0;
            case BOFU:case XUELU:case QIANGYUN:case HUWEI:case XINGONG:case WEIFENG:case TIEBI:return 40;
            default:return 0; // City-only or unsupported interactions do not justify a field deputy.
        }
    }
    private int formationValue(World.Unit u){
        int category=Army.category(u.weapon),score=w.army.leadership(u)*2+w.army.war(u)+w.army.intelligence(u)/2+w.army.aptitude(w.army.crew(u),category)*100;
        Set<Skill> skills=EnumSet.noneOf(Skill.class);
        for(World.Officer o:w.army.crew(u)){Skill s=Skill.find(o.skillId);if(s!=null&&skills.add(s))score+=combatSkill(s,category);}
        boolean caster=skills.contains(Skill.SHENSUAN)||skills.contains(Skill.XUSHI)||skills.contains(Skill.GUIMEN)||skills.contains(Skill.YAOSHU);
        if(caster&&skills.contains(Skill.BAICHU))score+=100;
        if(caster&&skills.contains(Skill.LIANHUAN))score+=80;
        return score;
    }
    private int[] deputies(World.Unit probe,List<World.Officer> idle,World.Officer administrator){
        List<Integer> chosen=new ArrayList<>();
        while(chosen.size()<2&&idle.size()-chosen.size()>2){
            int base=formationValue(probe),gain=0;World.Officer best=null;
            for(World.Officer o:idle){
                if(o.id==probe.officerId||o==administrator||chosen.contains(o.id))continue;
                boolean conflict=false;for(World.Officer member:w.army.crew(probe))if(w.relations.dislikes(o.id,member.id)||w.relations.dislikes(member.id,o.id))conflict=true;
                if(conflict)continue;
                int[] prior=probe.deputies;probe.deputies=Arrays.copyOf(prior,prior.length+1);probe.deputies[prior.length]=o.id;
                int value=formationValue(probe)-base;probe.deputies=prior;
                if(value>gain){gain=value;best=o;}
            }
            if(best==null)break;chosen.add(best.id);probe.deputies=chosen.stream().mapToInt(i->i).toArray();
        }
        return probe.deputies.clone();
    }
    public Deployment deployment(int city,int minimumReserve){
        return deployment(city,minimumReserve,c->true);
    }
    Deployment deployment(int city,int minimumReserve,Predicate<World.City> objectives){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||w.gameOver()||!w.districts.directCity(c.id)||w.actionPoints[w.active]<10||c.morale<65)return null;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return null;
        Districts.District district=w.districts.city(c.id);
        if(district!=null&&new DistrictManagement(w).residents(c)<3&&incoming(c)==0)return null;
        int reserve=Math.max(minimumReserve,reserve(c)),surplus=c.troops-reserve,foodReserve=Math.max(district==null?6000:district.reserveFood,(reserve+49)/50*12);
        if(surplus<3000||c.food<foodReserve+9000)return null;
        boolean objective=false;for(World.City target:w.cities)if(w.campaign.hostile(c.owner,target.owner)&&objectives.test(target))objective=true;
        if(!objective&&incoming(c)==0)return null;
        World.Officer administrator=idle.stream().max(Comparator.comparingInt(this::admin).thenComparingInt(o->-o.id)).orElse(null);
        Deployment best=null;int bestScore=Integer.MIN_VALUE;
        for(World.Officer leader:idle)for(World.Weapon weapon:World.Weapon.values()){
            if(weapon==World.Weapon.SWORD||idle.size()>1&&leader==administrator&&leader.politics>leader.leadership+15)continue;
            int troops=Math.min(8000,Math.min(surplus,w.government.commandLimit(leader.id)));
            if(Army.siegeWeapon(weapon)){if(c.equipment[weapon.ordinal()]<1||incoming(c)>0)continue;
                }
            else troops=Math.min(troops,c.equipment[weapon.ordinal()]);
            troops=Math.min(troops,(c.food-foodReserve)/3)/1000*1000;if(troops<3000)continue;
            World.Unit probe=new World.Unit(-1,c.owner,leader.id,weapon,c.hex,troops,troops*3);
            int[] deputies=deputies(probe,idle,administrator);int slots=Math.max(0,new DistrictManagement(w).residents(c)-(new DistrictManagement(w).residents(c)>=3?3:2));if(deputies.length>slots){deputies=Arrays.copyOf(deputies,slots);probe.deputies=deputies;}
            int score=formationValue(probe)+troops/50;
            if(Army.siegeWeapon(weapon))score+=escorts(probe)>=6000?180:-90;
            if(score<=bestScore)continue;
            Army.Ship ship=c.ships[1]>0?Army.Ship.WARSHIP:c.ships[0]>0?Army.Ship.TOWER_SHIP:Army.Ship.BOAT;
            probe.ship=ship;
            List<Hex> goals=new ArrayList<>();for(World.City target:cities())if(w.campaign.hostile(c.owner,target.owner)&&objectives.test(target))goals.add(target.hex);
            if(incoming(c)==0&&routes(probe,goals,1).isEmpty())continue;
            bestScore=score;best=new Deployment(c.id,leader.id,troops,troops*3,reserve,weapon,ship,deputies);
        }
        return best;
    }
    public boolean deploy(int city,int minimumReserve){return deploy(city,minimumReserve,c->true);}
    boolean deploy(int city,int minimumReserve,Predicate<World.City> objectives){
        Deployment d=deployment(city,minimumReserve,objectives);if(d==null)return false;
        World.City source=w.city(city);World.Unit probe=new World.Unit(-1,source.owner,d.leader,d.weapon,source.hex,d.troops,d.food);probe.ship=d.ship;
        World.City target=objective(probe,objectives,-1);boolean defense=incoming(source)>0;
        if(!defense&&target==null)return false;
        if(!defense&&Army.siegeWeapon(d.weapon)&&escorts(probe)<3000){
            World.Officer escort=null;for(World.Officer o:idle(source))if(o.id!=d.leader&&o.role!=Strategy.Role.RULER){escort=o;break;}
            if(escort==null||source.troops-d.reserve<6000||source.food<d.food+9000)return false;
            int escortId=w.nextUnitId;
            if(!w.army.deploy(city,escort.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,3000,9000).ok)return false;
            AiOrders.Order guard=w.aiOrders.get(w.unit(escortId));guard.home=city;guard.target=target.id;guard.staging=true;
            d=deployment(city,minimumReserve,objectives);if(d==null)return true;
        }
        int id=w.nextUnitId;
        if(!w.army.deploy(d.city,d.leader,d.deputies(),d.weapon,d.ship,d.troops,d.food).ok)return false;
        AiOrders.Order order=w.aiOrders.get(w.unit(id));order.home=source.id;order.target=defense?-1:target.id;order.staging=!defense;order.defending=defense;return true;
    }
    private int escorts(World.Unit u){int n=0;for(World.Unit friend:w.units)if(friend.owner==u.owner&&!Army.siegeWeapon(friend.weapon)&&friend.hex.distance(u.hex)<=8)n+=friend.troops;return n;}
    private int resistance(World.City c){int force=c.troops+c.defense*2;for(World.Unit enemy:w.units)if(enemy.owner==c.owner&&enemy.hex.distance(c.hex)<=7)force+=enemy.troops;return Math.max(3000,force);}
    private World.City objective(World.Unit u,Predicate<World.City> allowed,int prior){
        List<Hex> goals=new ArrayList<>();for(World.City c:cities())if(w.campaign.hostile(u.owner,c.owner)&&allowed.test(c))goals.add(c.hex);
        Map<Hex,Route> paths=routes(u,goals,Math.max(1,w.army.siegeRange(u)));World.City best=null;long bestScore=Long.MAX_VALUE;
        for(World.City c:cities()){Route r=paths.get(c.hex);if(r==null||r.cost/Math.max(1,w.war.movement(u))+4>foodTurns(u))continue;
            long score=r.cost*300L+resistance(c);if(c.id==prior)score-=12000;
            for(World.Unit friend:w.units){AiOrders.Order order=w.aiOrders.orders.get(friend.id);if(friend.owner==u.owner&&order!=null&&order.target==c.id)score-=Math.min(4000,friend.troops/2);}
            if(score<bestScore){best=c;bestScore=score;}
        }selectedRoute=best==null?null:paths.get(best.hex);return best;
    }
    /** City action: replenish threatened/low-food units before spending the last idle administrator. */
    public boolean replenish(int city){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||!w.districts.directCity(city))return false;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return false;
        for(World.Unit u:units())if(u.owner==c.owner&&u.hex.distance(c.hex)==1){
            Districts.District district=w.districts.city(c.id);if(district!=null&&!district.supplyEnabled)return false;
            int food=Math.min(Math.max(0,c.food-(district==null?6000:district.reserveFood)),Math.max(0,u.troops*3-u.food));
            int troops=foodTurns(u)<3?0:Math.min(3000,Math.max(0,Math.min(c.troops-Math.max(reserve(c),district==null?0:district.reserveTroops),w.government.commandLimit(u.officerId)-u.troops)));
            if(!Army.siegeWeapon(u.weapon))troops=Math.min(troops,c.equipment[u.weapon.ordinal()]);
            if((foodTurns(u)<6&&food>0||u.troops<3000&&troops>0)&&w.supply.replenish(c.id,idle.get(0).id,u.id,troops,food).ok)return true;
        }
        return false;
    }
    public boolean support(int city){World.City c=w.city(city);if(c==null||c.owner!=w.active||idle(c).isEmpty()||!w.districts.directCity(city))return false;
        Districts.District actual=w.districts.city(city);if(actual!=null)return new DistrictManagement(w).supply(actual,c,idle(c).get(0));
        Districts.District d=new Districts.District(-1,c.owner,"后方支援");d.reserveTroops=6000;d.reserveGold=5000;d.reserveFood=40000;
        for(World.City own:cities())if(own.owner==c.owner)d.cities.add(own.id);
        return new DistrictManagement(w).supply(d,c,idle(c).stream().max(Comparator.comparingInt(this::admin)).get());
    }
    /** Reserve-aware equipment production and backline supply, using ordinary paid missions. */
    public boolean prepare(int city){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||!w.districts.directCity(city))return false;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return false;
        World.Officer o=idle.stream().max(Comparator.comparingInt(this::admin).thenComparingInt(x->-x.id)).orElseThrow();
        if(support(city))return true;
        if(c.troops<reserve(c)+3000||c.gold<1500)return false;
        World.Weapon preferred=World.Weapon.SPEAR;int aptitude=-1;
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY}){
            int rank=0;for(World.Officer leader:idle)rank=Math.max(rank,leader.aptitude[Army.category(weapon)]);
            if(rank>aptitude){preferred=weapon;aptitude=rank;}
        }
        return c.equipment[preferred.ordinal()]<8000&&w.produce(c.id,o.id,preferred).ok;
    }
    private static final class Step {final Hex h;final int cost;Step(Hex h,int c){this.h=h;cost=c;}}
    private static final class Route {final List<Hex> path;final int cost;Route(List<Hex> p,int c){path=p;cost=c;}}
    private int hazard(World.Unit u,Hex h){
        int cost=w.war.fireAt(h)!=null&&!w.skills.has(u,Skill.HUOSHEN)?8:0;
        if(w.terrain[h.q][h.r]==World.Terrain.POISON&&!w.skills.has(u,Skill.JIEDU))cost+=8;
        if(w.terrain[h.q][h.r]==World.Terrain.PLANK_ROAD&&!w.skills.has(u,Skill.TAPO))cost+=3;
        return cost;
    }
    int routeSearches,routeExpanded; // Per planner diagnostics; never part of world state or RNG.
    private Route route(World.Unit u,Hex goal,int range){return routes(u,Collections.singleton(goal),range).get(goal);}
    /** One weighted flood answers every candidate objective for this decision. No cache survives a command. */
    private Map<Hex,Route> routes(World.Unit u,Collection<Hex> goals,int range){
        Map<Hex,Route> result=new HashMap<>();if(goals.isEmpty())return result;routeSearches++;
        Set<Hex> blocked=new HashSet<>(),friendly=new HashSet<>();for(World.City c:w.cities)blocked.add(c.hex);
        for(World.Unit other:w.fieldUnits())if(other.id!=u.id){if(other.owner==u.owner)friendly.add(other.hex);else blocked.add(other.hex);}
        for(Domestic.Facility f:w.domestic.facilities)blocked.add(f.hex);
        for(War.Structure s:w.war.structures())blocked.add(s.hex);
        for(WorldEvents.Camp c:w.events.camps())blocked.add(c.hex);
        java.util.function.Predicate<Hex> zone=w.advancedBattle.zoneForSearch(u);
        Set<Hex> targets=new HashSet<>(goals);
        Map<Hex,List<Hex>> arrivals=new HashMap<>();
        for(Hex goal:targets)for(int dq=-range;dq<=range;dq++)for(int dr=-range;dr<=range;dr++){
            Hex h=new Hex(goal.q+dq,goal.r+dr);
            if(w.inside(h)&&h.distance(goal)<=range)arrivals.computeIfAbsent(h,x->new ArrayList<>()).add(goal);
        }
        Map<Hex,Integer> costs=new HashMap<>();Map<Hex,Hex> parents=new HashMap<>();
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
        queue.add(new Step(u.hex,0));costs.put(u.hex,0);
        while(!queue.isEmpty()){
            Step s=queue.remove();if(costs.get(s.h)!=s.cost)continue;routeExpanded++;
            for(Hex goal:arrivals.getOrDefault(s.h,Collections.emptyList()))if(!result.containsKey(goal)&&!friendly.contains(s.h)){
                LinkedList<Hex> path=new LinkedList<>();for(Hex h=s.h;h!=null;h=parents.get(h))path.addFirst(h);
                result.put(goal,new Route(path,s.cost));
            }
            if(result.size()==targets.size())break;
            for(Hex h:s.h.neighbors()){
                if(blocked.contains(h))continue;int step=w.army.moveCost(u,s.h,h);if(step<1)continue;
                int cost=s.cost+step+hazard(u,h)+(zone.test(h)?3:0)+(friendly.contains(h)?3:0);
                if(cost>=costs.getOrDefault(h,Integer.MAX_VALUE))continue;
                costs.put(h,cost);parents.put(h,s.h);queue.add(new Step(h,cost));
            }
        }
        return result;
    }
    private boolean follow(World.Unit u,Route route){
        if(route==null||route.path.size()<2)return false;int used=0;List<Hex> prefix=new ArrayList<>();prefix.add(u.hex);
        for(int i=1;i<route.path.size();i++){
            Hex h=route.path.get(i);if(w.unitAt(h)!=null)break;used+=w.army.moveCost(u,route.path.get(i-1),h);if(used>w.orders.remaining(u))break;
            prefix.add(h);if(w.advancedBattle.zone(u,h))break;
        }
        if(prefix.size()<2)return false;
        UnitOrders.MovePlan plan=w.orders.previewMove(u.id,prefix.get(prefix.size()-1));
        return plan.valid()&&w.orders.executeRoute(plan,prefix).ok;
    }
    private boolean canEnter(World.Unit u,World.City c){
        return c.owner==u.owner&&c.troops+u.troops<=w.campaign.troopCap(c)&&c.food+u.food<=w.campaign.foodCap(c)&&c.gold+u.gold<=w.campaign.goldCap(c)&&
            c.equipment[u.weapon.ordinal()]+Army.equipmentNeeded(u.weapon,u.troops)<=w.campaign.equipmentCap(c,u.weapon)&&(u.ship==Army.Ship.BOAT||c.ships[u.ship.ordinal()-1]<100);
    }
    private boolean retreat(World.Unit u,Predicate<World.City> homes){
        World.City home=null;Route best=null;
        List<World.City> candidates=new ArrayList<>();List<Hex> goals=new ArrayList<>();
        for(World.City c:cities())if(homes.test(c)&&canEnter(u,c)){candidates.add(c);goals.add(c.hex);}
        Map<Hex,Route> paths=routes(u,goals,1);
        for(World.City c:candidates){Route r=paths.get(c.hex);if(r!=null&&(best==null||r.cost<best.cost)){best=r;home=c;}}
        if(home==null)return false;
        boolean moved=u.hex.distance(home.hex)>1&&follow(u,best);
        if(w.unit(u.id)!=null&&u.hex.distance(home.hex)==1&&w.enter(u.id,home.id).ok)return true;
        return moved;
    }
    private World.Unit at(World.Unit u,Hex h){World.Unit copy=new World.Unit(u.id,u.owner,u.officerId,u.weapon,h,u.troops,u.food);copy.ship=u.ship;copy.deputies=u.deputies;copy.energy=u.energy;return copy;}
    private int exposure(World.Unit u,Hex h){
        int threat=hazard(u,h)*100;World.Unit probe=at(u,h);
        for(World.Unit enemy:w.units)if(w.campaign.hostile(u.owner,enemy.owner)&&enemy.status==War.Status.NORMAL){
            int distance=enemy.hex.distance(h);if(w.war.attackPositionError(enemy,probe)==null)threat+=w.combat.expectedDamage(enemy,probe,1,false);
            else if(distance<=w.war.movement(enemy)+w.war.range(enemy))threat+=w.combat.expectedDamage(enemy,probe,1,false)/6;
        }
        return threat;
    }
    private void reposition(World.Unit u){
        if(!w.army.canAttackUnit(u)||w.orders.remaining(u)==0)return;
        boolean nearby=false;for(World.Unit enemy:w.units)if(w.campaign.hostile(u.owner,enemy.owner)&&u.hex.distance(enemy.hex)<=w.orders.remaining(u)+4){nearby=true;break;}
        if(!nearby)return;
        Hex best=u.hex;int bestScore=Integer.MIN_VALUE;
        for(Map.Entry<Hex,Integer> e:w.orders.reachable(u).entrySet()){
            World.Unit probe=at(u,e.getKey());int offense=0;
            for(World.Unit b:w.fieldUnits())if(w.war.attackPositionError(probe,b)==null){
                int hit=w.combat.expectedDamage(probe,b,1,false),counter=b.status==War.Status.NORMAL&&w.war.canCounter(probe,b)&&hit<b.troops?w.combat.expectedDamage(b,probe,.5,false):0;
                offense=Math.max(offense,value(b,hit)-counter);
            }
            if(offense==0)continue;
            // A melee army must close before spending its action on a low-value ranged plot.
            // Count future enemy fire without treating unavoidable retaliation as a reason to freeze.
            int score=offense-exposure(u,e.getKey())*(w.war.range(probe)>1?100:55)/100-e.getValue()*8;
            if(score>bestScore||score==bestScore&&e.getKey().equals(u.hex)){bestScore=score;best=e.getKey();}
        }
        if(!best.equals(u.hex))w.orders.execute(w.orders.previewMove(u.id,best));
    }
    /** Shared by computer factions and delegated districts. Attack permission is checked before every offensive action. */
    public void runUnit(World.Unit u,boolean attack,Predicate<World.City> objectives,Predicate<World.City> homes){
        if(u instanceof Domestic.Mission||w.orders.error(u)!=null)return;
        AiOrders.Order order=w.aiOrders.get(u);Hex before=u.hex;actedProductively=false;formationWait=false;
        if(order.home<0||w.city(order.home).owner!=u.owner){World.City home=null;for(World.City c:cities())if(homes.test(c)&&(home==null||u.hex.distance(c.hex)<u.hex.distance(home.hex)))home=c;order.home=home==null?-1:home.id;}
        if(!escort(u,attack,objectives,order))stepUnit(u,attack,objectives,homes,order);
        if(w.unit(u.id)!=null&&order.turn!=w.turn){order.stalled=before.equals(u.hex)&&!actedProductively&&!formationWait?Math.min(100,order.stalled+1):0;order.last=u.hex;order.turn=w.turn;}
    }

    /** A saved association, not a bonus: the escort spends its own movement and combat action. */
    private boolean escort(World.Unit u,boolean attack,Predicate<World.City> objectives,AiOrders.Order order){
        World.City home=w.city(order.home);
        if(order.defending||Army.siegeWeapon(u.weapon)||u.troops<3000||foodTurns(u)<4||home!=null&&incoming(home)>0){clearEscort(u);return false;}
        Domestic.Mission chosen=null;
        for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.owner==u.owner&&!m.stopped&&w.cityAt(m.hex)==null&&w.districts.city(m.sourceCity)==w.districts.unit(u.id)){
            if(w.unit(m.escortId)==null)m.escortId=-1;
            if(m.escortId>=0&&m.escortId!=u.id||u.hex.distance(m.hex)>6||m.gold+m.food/10+m.troops<1500)continue;
            boolean threatened=false;for(World.Unit enemy:w.units)if(w.campaign.hostile(u.owner,enemy.owner)&&enemy.hex.distance(m.hex)<=7)threatened=true;
            if(threatened&&(chosen==null||u.hex.distance(m.hex)<u.hex.distance(chosen.hex)))chosen=m;
        }
        clearEscort(u);if(chosen==null)return false;chosen.escortId=u.id;
        Action action=bestAction(u.id,attack,objectives);if(action!=null&&execute(action).ok)return true;
        if(u.hex.distance(chosen.hex)>2)follow(u,route(u,chosen.hex,2));
        else{ // Do not camp on the next convoy route tile; yield only to a strictly lower-ID convoy.
            MarchOrders.Plan path=w.marches.convoyRoute(chosen);
            if(path.path.contains(u.hex))for(Hex h:u.hex.neighbors())if(!path.path.contains(h)&&h.distance(chosen.hex)<=3&&w.orders.previewMove(u.id,h).valid()){w.orders.execute(w.orders.previewMove(u.id,h));break;}
        }
        formationWait=true;if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return true;
    }
    private void clearEscort(World.Unit u){for(Domestic.Mission m:w.domestic.missions)if(m.escortId==u.id)m.escortId=-1;}
    private boolean yieldFriendlyLane(World.Unit u){
        if(w.orders.remaining(u)==0)return false;
        for(World.Unit other:units())if(other.owner==u.owner&&other.id!=u.id&&u.hex.distance(other.hex)==1){
            AiOrders.Order plan=w.aiOrders.orders.get(other.id);if(plan==null||plan.target<0)continue;
            World.City target=w.city(plan.target);if(target==null||u.hex.distance(target.hex)>=other.hex.distance(target.hex))continue;
            for(Hex h:u.hex.neighbors())if(w.unitAt(h)==null&&w.cityAt(h)==null&&h.distance(target.hex)>=u.hex.distance(target.hex)&&w.orders.previewMove(u.id,h).valid())
                return w.orders.execute(w.orders.previewMove(u.id,h)).ok;
        }return false;
    }
    private boolean yieldSiegeLane(World.Unit u){
        if(Army.siegeWeapon(u.weapon)||w.orders.remaining(u)==0)return false;
        for(World.City c:cities())if(w.campaign.hostile(u.owner,c.owner)&&u.hex.distance(c.hex)==1){
            boolean engine=false;for(World.Unit ally:w.units)if(ally.owner==u.owner&&Army.siegeWeapon(ally.weapon)&&ally.hex.distance(u.hex)<=3&&ally.hex.distance(c.hex)>w.army.siegeRange(ally))engine=true;
            if(engine){for(Map.Entry<Hex,Integer> e:w.orders.reachable(u).entrySet())if(e.getKey().distance(c.hex)==2&&w.unitAt(e.getKey())==null&&exposure(u,e.getKey())<=exposure(u,u.hex))return w.orders.execute(w.orders.previewMove(u.id,e.getKey())).ok;}
        }return false;
    }
    private boolean reinforcementsApproaching(World.Unit u,AiOrders.Order order){
        for(World.Unit ally:w.units){AiOrders.Order o=w.aiOrders.orders.get(ally.id);if(ally.owner==u.owner&&ally.id!=u.id&&o!=null&&o.staging&&o.target==order.target&&o.stalled<3&&ally.hex.distance(u.hex)>8&&foodTurns(ally)>=6)return true;}return false;
    }
    private boolean convoySupport(World.Unit u){
        if(foodTurns(u)>=4)return false;
        Districts.District receiver=w.districts.unit(u.id);
        for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.owner==u.owner){
            Districts.District source=w.districts.city(m.sourceCity);
            if(source!=receiver||source!=null&&!source.supplyEnabled)continue;
            int ration=Math.max(1,(u.troops+19)/20),available=m.food-w.domestic.foodUse(m)*4;
            if(m.hex.distance(u.hex)==1&&available>0&&w.supply.convoyTransfer(m.id,u.id,0,Math.min(ration*8-u.food,available),0).ok){actedProductively=true;return false;}
            World.City destination=w.city(m.targetCity);int eta=w.domestic.deliverableEta(m);
            if(destination!=null&&u.hex.distance(destination.hex)<=4&&eta>=0&&eta<=foodTurns(u)&&foodTurns(u)>1){formationWait=true;w.war.waitUnit(u.id);return true;}
        }return false;
    }
    private void stepUnit(World.Unit u,boolean attack,Predicate<World.City> objectives,Predicate<World.City> homes,AiOrders.Order order){
        if(order.defending&&order.home>=0){
            World.City home=w.city(order.home);if(home.owner!=u.owner||incoming(home)==0||u.hex.distance(home.hex)>7){retreat(u,homes);if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return;}
            objectives=c->false;
        }
        if(order.target>=0&&(!w.campaign.hostile(u.owner,w.city(order.target).owner)||!objectives.test(w.city(order.target))))order.target=-1;
        if(!attack||!order.staging&&order.stalled>=3){order.target=-1;if(retreat(u,homes)){if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return;}}
        if(order.staging&&order.target>=0&&order.home>=0&&incoming(w.city(order.home))==0){
            int assembled=0;for(World.Unit ally:w.units){AiOrders.Order plan=w.aiOrders.orders.get(ally.id);if(ally.owner==u.owner&&plan!=null&&plan.target==order.target&&ally.hex.distance(u.hex)<=8)assembled+=ally.troops;}
            boolean approaching=reinforcementsApproaching(u,order);
            if((assembled<resistance(w.city(order.target))||approaching)&&foodTurns(u)>=6&&(order.stalled<6||approaching)){
                World.City rally=w.city(order.home);for(World.Unit ally:w.units){AiOrders.Order plan=w.aiOrders.orders.get(ally.id);if(ally.owner==u.owner&&plan!=null&&plan.target==order.target&&plan.home>=0&&w.city(plan.home).owner==u.owner){World.City candidate=w.city(plan.home),target=w.city(order.target);if(candidate.hex.distance(target.hex)<rally.hex.distance(target.hex)||candidate.hex.distance(target.hex)==rally.hex.distance(target.hex)&&candidate.id<rally.id)rally=candidate;}}
                if(u.hex.distance(rally.hex)>4){follow(u,route(u,rally.hex,3));w.war.waitUnit(u.id);return;}
                // Clear deployment exits while gathering. This is an ordinary paid movement, never teleportation.
                World.City home=rally;if(u.hex.distance(home.hex)<=1)for(Hex h:w.orders.reachable(u).keySet())if(h.distance(home.hex)==3&&w.unitAt(h)==null){w.orders.execute(w.orders.previewMove(u.id,h));break;}
                w.war.waitUnit(u.id);return;
            }if(foodTurns(u)<6||assembled<resistance(w.city(order.target))&&order.stalled>=6){order.staging=false;order.target=-1;if(retreat(u,homes)){if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return;}}else{order.staging=false;for(World.Unit ally:w.units){AiOrders.Order plan=w.aiOrders.orders.get(ally.id);if(ally.owner==u.owner&&plan!=null&&plan.target==order.target&&ally.hex.distance(u.hex)<=8)plan.staging=false;}}
        }
        if(attack&&yieldSiegeLane(u)){w.war.waitUnit(u.id);return;}
        if(convoySupport(u))return;
        if((u.troops<1500||foodTurns(u)<4)&&retreat(u,homes)){
            if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return;
        }
        Action action=bestAction(u.id,attack,objectives);
        if(action!=null&&(action.kind==Kind.EXTINGUISH||action.plot==War.Plot.CALM)&&execute(action).ok)return;
        if(attack)reposition(u);
        action=bestAction(u.id,attack,objectives);if(action!=null&&execute(action).ok)return;
        Route best=null;int bestScore=Integer.MAX_VALUE;
        if(attack){
            // Nearby hostile units take precedence over distant city objectives.
            List<World.Unit> enemies=new ArrayList<>();List<Hex> goals=new ArrayList<>();
            for(World.Unit enemy:units())if(w.campaign.hostile(u.owner,enemy.owner)&&u.hex.distance(enemy.hex)<=(enemy instanceof Domestic.Mission?6:8)&&(!(enemy instanceof Domestic.Mission)||order.home<0||incoming(w.city(order.home))==0||enemy.hex.distance(w.city(order.home).hex)<=4)&&(!Army.siegeWeapon(u.weapon)||escorts(u)>=enemy.troops)&&(order.target>=0||order.home<0||enemy.hex.distance(w.city(order.home).hex)<=8)){enemies.add(enemy);goals.add(enemy.hex);}
            Map<Hex,Route> paths=routes(u,goals,Math.max(1,w.war.range(u)));
            for(World.Unit enemy:enemies){Route r=paths.get(enemy.hex);if(r!=null&&r.path.size()>1&&r.cost<bestScore){best=r;bestScore=r.cost;}}
            if(best==null){
                World.City target=objective(u,objectives,order.target);
                if(target!=null){order.target=target.id;best=selectedRoute;}
                else if(order.target>=0&&order.stalled<3&&w.campaign.hostile(u.owner,w.city(order.target).owner)){if(yieldFriendlyLane(u)){w.war.waitUnit(u.id);return;}w.war.waitUnit(u.id);return;}
                else order.target=-1;
            }
        }
        if(best!=null&&order.target>=0&&!order.staging&&!Army.siegeWeapon(u.weapon)){
            World.City target=w.city(order.target);
            for(World.Unit engine:w.units){AiOrders.Order plan=w.aiOrders.orders.get(engine.id);int gap=u.hex.distance(engine.hex);
                if(engine.owner!=u.owner||!Army.siegeWeapon(engine.weapon)||plan==null||plan.staging||plan.target!=order.target||plan.stalled>=3||gap>16||engine.hex.distance(target.hex)<=u.hex.distance(target.hex)||foodTurns(engine)<6)continue;
                int exits=0;for(Hex h:u.hex.neighbors())if(w.army.moveCost(u,u.hex,h)>0)exits++;
                if(exits<=2)continue; // Clear a narrow passage before waiting for the engine.
                int limit=1;while(limit<best.path.size()&&best.path.get(limit).distance(engine.hex)<=4)limit++;
                if(limit==1){yieldFriendlyLane(u);formationWait=true;w.war.waitUnit(u.id);return;}
                best=new Route(new ArrayList<>(best.path.subList(0,limit)),best.cost);break;
            }
        }
        if(best!=null)follow(u,best);else retreat(u,homes);
        if(w.unit(u.id)==null||u.acted)return;
        action=bestAction(u.id,attack,objectives);if(action==null||!execute(action).ok)w.war.waitUnit(u.id);
    }
    public void runUnits(){for(World.Unit u:units())if(u.owner==w.active&&!w.gameOver()){
        Diplomacy.Aid aid=w.diplomacy.aidForUnit(u.id);
        runUnit(u,aid==null||!aid.returning,c->aid==null||c.id==aid.target,c->c.owner==u.owner);
    }}
}
