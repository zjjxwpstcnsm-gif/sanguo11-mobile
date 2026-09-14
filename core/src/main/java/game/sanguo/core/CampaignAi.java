package game.sanguo.core;

import java.util.*;
import java.util.function.Predicate;

/** Deterministic, resource-constrained planning. Scores are engineering policy, not SAN11 formulas.
 * Planning never consumes world RNG, changes state, or grants resources. Commands remain authoritative. */
public final class CampaignAi {
    public enum Kind { ATTACK, TACTIC, ARMY_TACTIC, PLOT, SIEGE, STRUCTURE, RAID, CAMP, EXTINGUISH, JOINT }
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
    public CampaignAi(World w){this.w=Objects.requireNonNull(w);}
    private List<World.Unit> units(){List<World.Unit> out=new ArrayList<>(w.units);out.sort(Comparator.comparingInt(u->u.id));return out;}
    private List<World.City> cities(){List<World.City> out=new ArrayList<>(w.cities);out.sort(Comparator.comparingInt(c->c.id));return out;}
    private List<World.Officer> idle(World.City c){List<World.Officer> out=w.idle(c);out.sort(Comparator.comparingInt(o->o.id));return out;}
    private Action action(Kind k,World.Unit u,int target,Hex h,int score,String reason){return new Action(k,u.id,target,h,score,null,null,null,reason);}
    private Action better(Action a,Action b){
        // Candidates are enumerated by stable IDs and enums; equal scores retain the first.
        return b!=null&&b.score>0&&(a==null||b.score>a.score)?b:a;
    }
    private int damage(World.Unit a,World.Unit b,double scale,boolean tactic){
        int total=0;for(int i=0;i<8;i++)total+=w.war.physicalDamage(a,b,scale,tactic,new Random(7919L*i+17));
        return total/8;
    }
    private int value(World.Unit target,int loss){return loss+(loss>=target.troops?900:0)+(Army.siegeWeapon(target.weapon)?loss/4:0);}
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
                for(World.Unit u:others)amount+=value(u,damage(b,u,1,false))+value(b,damage(u,b,.5,false));
                if(!others.isEmpty())amount/=others.size();break;
            case AMBUSH: amount=value(b,damage(a,b,1.35,true))+100;break;
            case FIRE:
                // Avoid an unmodeled trap cascade hitting friendlies.
                if(w.war.at(b.hex)!=null)return -1;
                amount=w.skills.fireDamage(b,250,a.owner,w.skills.has(a,Skill.HUOSHEN)?2:1,false);break;
            default:return -1;
        }
        int chance=w.war.plotChance(a.id,b.hex,p);
        if(w.skills.has(a,Skill.LIANHUAN)&&(p==War.Plot.CONFUSE||p==War.Plot.MISLEAD||p==War.Plot.FIRE)){
            for(World.Unit u:units())if(u.id!=b.id&&w.campaign.hostile(a.owner,u.owner)&&u.hex.distance(b.hex)==1&&
                (p==War.Plot.FIRE?w.war.fireAt(u.hex)==null&&!w.army.water(u.hex):u.status==War.Status.NORMAL)){
                int chained=p==War.Plot.FIRE?w.skills.fireDamage(u,250,a.owner,1,false):controlValue(u);
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
    private Action bestAction(int unit,boolean attack,Predicate<World.City> objectives){
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
                int hit=damage(a,b,1,false),counter=b.status==War.Status.NORMAL&&w.war.canCounter(a,b)&&hit<b.troops?damage(b,a,.5,false):0;
                best=better(best,action(Kind.ATTACK,a,b.id,b.hex,value(b,hit)-counter,"比较伤害、歼灭收益与反击损失"));
            }
            if(!w.skills.has(b,Skill.TIEBI)&&w.advancedBattle.jointError(a.id,b.id)==null){
                int hit=0,opportunity=0;boolean flank=false;
                for(World.Unit helper:w.advancedBattle.jointParticipants(a.id,b.id)){
                    hit+=damage(helper,b,.65,false);flank|=w.skills.has(helper,Skill.JIJIAO);
                    if(helper.id!=a.id)opportunity+=damage(helper,b,1,false)*3/4;
                }
                int score=value(b,Math.min(hit,b.troops))-opportunity+(flank?controlValue(b)/2:0);
                best=better(best,action(Kind.JOINT,a,b.id,b.hex,score,"比较齐攻歼灭收益与协攻部队行动代价"));
            }
            for(War.Tactic t:War.Tactic.values())if(w.war.tacticError(a.id,b.id,t)==null){
                int hit=value(b,damage(a,b,t.multiplier,true));boolean friendly=false;
                for(World.Unit other:units())if(other.id!=b.id&&other.id!=a.id&&splash(t,a.hex,b.hex,other.hex)){
                    if(t==War.Tactic.VOLLEY&&other.owner==a.owner&&!w.skills.has(a,Skill.GONGSHEN)){friendly=true;break;}
                    if(w.campaign.hostile(a.owner,other.owner))hit+=value(other,damage(a,other,t.multiplier,true));
                }
                if(friendly)continue;
                if(t==War.Tactic.SPIRAL)hit+=controlValue(b);
                int score=hit*w.war.tacticChance(a.id,b.id,t)/100-t.energy*8;
                best=better(best,new Action(Kind.TACTIC,a.id,b.id,b.hex,score,t,null,null,"按命中与范围收益选择战法"));
            }
            for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,b.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,b.hex))continue;
                int score=value(b,damage(a,b,t==Army.Tactic.STONE?1.5:1.3,true))*w.army.tacticChance(a.id,b.hex)/100-t.energy*8;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,b.id,b.hex,score,null,t,null,"兵器与水军选择有效战法"));
            }
            for(War.Plot p:War.Plot.values())if(p!=War.Plot.CALM&&p!=War.Plot.EXTINGUISH&&w.war.plotError(a.id,b.hex,p)==null)
                best=better(best,new Action(Kind.PLOT,a.id,b.id,b.hex,plotValue(a,b,p),null,null,p,"按免疫、命中、反计和友军损失选择计略"));
        }
        if(!attack)return best;
        for(World.City c:cities())if(w.campaign.hostile(a.owner,c.owner)&&objectives.test(c)){
            int score=150+(c.troops==0||c.defense<=w.army.siegeDefenseDamage(a)?1800:0);
            if(w.siegeError(a.id,c.id)==null)best=better(best,action(Kind.SIEGE,a,c.id,c.hex,score,"夺取可占领据点"));
            for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,c.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,c.hex))continue;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,c.id,c.hex,score+w.army.siegeDefenseDamage(a)+w.army.siegeTroopDamage(a)/2-t.energy*5,null,t,null,"使用兵器战法削减城防和守军"));
            }
        }
        for(War.Structure s:w.war.structures())if(w.campaign.hostile(a.owner,s.owner)&&a.hex.distance(s.hex)<=w.war.range(a)){
            int score=200+Math.min(s.hp,200+w.army.war(a)*2)+(s.complete?100:0);
            if(w.army.canAttackUnit(a))best=better(best,action(Kind.STRUCTURE,a,s.id,s.hex,score,"清除敌方设施与通路阻挡"));
            else for(Army.Tactic t:w.army.tactics(a))if(w.army.tacticError(a.id,s.hex,t)==null){
                if(t==Army.Tactic.STONE&&w.campaign.has(a.owner,Campaign.Tech.THUNDERBOLT)&&ownAssetsNear(a,s.hex))continue;
                if((t==Army.Tactic.FIRE_ARROW||t==Army.Tactic.FLAME)&&w.fieldworks.trap(s.kind))continue;
                best=better(best,new Action(Kind.ARMY_TACTIC,a.id,s.id,s.hex,score-t.energy*5,null,t,null,"用合法兵器战法拆除设施并避开友伤"));
            }
        }
        for(Domestic.Mission m:w.domestic.missions)if(w.supply.raidError(a.id,m.id)==null){int hit=w.supply.raidDamage(a.id,m.id);
            best=better(best,action(Kind.RAID,a,m.id,m.hex,hit+(hit>=m.troops?Math.min(2000,m.food/20)+300:0),"截击有价值的运输补给"));}
        for(WorldEvents.Camp camp:w.events.camps())if(w.events.attackError(a.id,camp.id)==null)
            best=better(best,action(Kind.CAMP,a,camp.id,camp.hex,400,"清除持续劫掠的营寨"));
        return best;
    }
    private int controlValueNormal(World.Unit u){return 250+Math.min(700,u.troops/10);}
    public World.Result execute(Action a){
        if(a==null)return w.fail("暂无可执行战术");
        switch(a.kind){
            case ATTACK:return w.attack(a.actor,a.target);
            case TACTIC:return w.war.tactic(a.actor,a.target,a.tactic);
            case ARMY_TACTIC:return w.army.tactic(a.actor,a.hex,a.armyTactic);
            case PLOT:return w.war.plot(a.actor,a.hex,a.plot);
            case SIEGE:return w.siege(a.actor,a.target);
            case STRUCTURE:return w.war.attackStructure(a.actor,a.hex);
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
    public Deployment deployment(int city,int minimumReserve){
        return deployment(city,minimumReserve,c->true);
    }
    Deployment deployment(int city,int minimumReserve,Predicate<World.City> objectives){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||w.gameOver()||!w.districts.directCity(c.id)||w.actionPoints[w.active]<10||c.morale<65)return null;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return null;
        int reserve=Math.max(minimumReserve,reserve(c)),surplus=c.troops-reserve,foodReserve=Math.max(6000,(reserve+49)/50*12);
        if(surplus<3000||c.food<foodReserve+9000)return null;
        boolean objective=false;for(World.City target:w.cities)if(w.campaign.hostile(c.owner,target.owner)&&objectives.test(target))objective=true;
        if(!objective)return null;
        World.Officer administrator=idle.stream().max(Comparator.comparingInt(this::admin).thenComparingInt(o->-o.id)).orElse(null);
        Deployment best=null;int bestScore=Integer.MIN_VALUE;
        for(World.Officer leader:idle)for(World.Weapon weapon:World.Weapon.values()){
            if(weapon==World.Weapon.SWORD||idle.size()>1&&leader==administrator&&leader.politics>leader.leadership+15)continue;
            int troops=Math.min(8000,Math.min(surplus,w.government.commandLimit(leader.id)));
            if(Army.siegeWeapon(weapon)){if(c.equipment[weapon.ordinal()]<1||incoming(c)>0)continue;}
            else troops=Math.min(troops,c.equipment[weapon.ordinal()]);
            troops=Math.min(troops,(c.food-foodReserve)/3)/1000*1000;if(troops<3000)continue;
            int category=Army.category(weapon),score=leader.leadership*2+leader.war+leader.aptitude[category]*100+troops/50;
            if(Army.siegeWeapon(weapon))score-=90;
            if(score<=bestScore)continue;
            List<Integer> deputies=new ArrayList<>();int intelligence=leader.intelligence,aptitude=leader.aptitude[category];
            for(World.Officer o:idle)if(o.id!=leader.id&&o!=administrator&&idle.size()-1-deputies.size()>1&&deputies.size()<2&&
                (o.intelligence>intelligence+15||o.aptitude[category]>aptitude||!o.skillId.equals("none")&&!o.skillId.equals(leader.skillId))){
                deputies.add(o.id);intelligence=Math.max(intelligence,o.intelligence);aptitude=Math.max(aptitude,o.aptitude[category]);
            }
            Army.Ship ship=c.ships[1]>0?Army.Ship.WARSHIP:c.ships[0]>0?Army.Ship.TOWER_SHIP:Army.Ship.BOAT;
            World.Unit probe=new World.Unit(-1,c.owner,leader.id,weapon,c.hex,troops,troops*3);probe.ship=ship;
            boolean route=false;for(World.City target:cities())if(w.campaign.hostile(c.owner,target.owner)&&objectives.test(target)&&route(probe,target.hex,1)!=null){route=true;break;}
            if(!route)continue;
            bestScore=score;best=new Deployment(c.id,leader.id,troops,troops*3,reserve,weapon,ship,deputies.stream().mapToInt(i->i).toArray());
        }
        return best;
    }
    public boolean deploy(int city,int minimumReserve){Deployment d=deployment(city,minimumReserve);return d!=null&&w.army.deploy(d.city,d.leader,d.deputies(),d.weapon,d.ship,d.troops,d.food).ok;}
    boolean deploy(int city,int minimumReserve,Predicate<World.City> objectives){Deployment d=deployment(city,minimumReserve,objectives);return d!=null&&w.army.deploy(d.city,d.leader,d.deputies(),d.weapon,d.ship,d.troops,d.food).ok;}
    /** City action: replenish threatened/low-food units before spending the last idle administrator. */
    public boolean replenish(int city){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||!w.districts.directCity(city))return false;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return false;
        for(World.Unit u:units())if(u.owner==c.owner&&u.hex.distance(c.hex)==1){
            int food=Math.min(Math.max(0,c.food-6000),Math.max(0,u.troops*3-u.food));
            int troops=foodTurns(u)<3?0:Math.min(3000,Math.max(0,Math.min(c.troops-reserve(c),w.government.commandLimit(u.officerId)-u.troops)));
            if(!Army.siegeWeapon(u.weapon))troops=Math.min(troops,c.equipment[u.weapon.ordinal()]);
            if((foodTurns(u)<6&&food>0||u.troops<3000&&troops>0)&&w.supply.replenish(c.id,idle.get(0).id,u.id,troops,food).ok)return true;
        }
        return false;
    }
    /** Reserve-aware equipment production and backline supply, using ordinary paid missions. */
    public boolean prepare(int city){
        World.City c=w.city(city);if(c==null||c.owner!=w.active||!w.districts.directCity(city))return false;
        List<World.Officer> idle=idle(c);if(idle.isEmpty())return false;
        World.Officer o=idle.stream().max(Comparator.comparingInt(this::admin).thenComparingInt(x->-x.id)).orElseThrow();
        if(incoming(c)==0&&c.troops>reserve(c)+1000){
            for(World.City front:cities())if(front.owner==c.owner&&front.id!=c.id&&incoming(front)>0){
                boolean underway=false;for(Domestic.Mission m:w.domestic.missions)if(m.transport&&m.owner==c.owner&&m.targetCity==front.id)underway=true;
                if(underway)continue;
                int food=Math.min(20000,Math.min(Math.max(0,c.food-40000),Math.max(0,40000-front.food)));
                int gold=Math.min(3000,Math.min(Math.max(0,c.gold-5000),Math.max(0,5000-front.gold)));
                if((food>0||gold>0)&&w.domestic.transport(c.id,front.id,o.id,gold,food,1000,new int[World.Weapon.values().length]).ok)return true;
            }
        }
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
    private Route route(World.Unit u,Hex goal,int range){
        Set<Hex> blocked=new HashSet<>();for(World.City c:w.cities)blocked.add(c.hex);
        for(World.Unit other:w.units)if(other.id!=u.id)blocked.add(other.hex);
        for(Domestic.Facility f:w.domestic.facilities)blocked.add(f.hex);
        for(War.Structure s:w.war.structures())blocked.add(s.hex);
        for(WorldEvents.Camp c:w.events.camps())blocked.add(c.hex);
        Map<Hex,Integer> costs=new HashMap<>();Map<Hex,Hex> parents=new HashMap<>();
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.h.q).thenComparingInt(s->s.h.r));
        queue.add(new Step(u.hex,0));costs.put(u.hex,0);
        while(!queue.isEmpty()){
            Step s=queue.remove();if(costs.get(s.h)!=s.cost)continue;
            if(s.h.distance(goal)<=range){LinkedList<Hex> path=new LinkedList<>();for(Hex h=s.h;h!=null;h=parents.get(h))path.addFirst(h);return new Route(path,s.cost);}
            for(Hex h:s.h.neighbors()){
                if(blocked.contains(h))continue;int step=w.army.moveCost(u,s.h,h);if(step<1)continue;
                int cost=s.cost+step+hazard(u,h)+(w.advancedBattle.zone(u,h)?3:0);
                if(cost>=costs.getOrDefault(h,Integer.MAX_VALUE))continue;
                costs.put(h,cost);parents.put(h,s.h);queue.add(new Step(h,cost));
            }
        }
        return null;
    }
    private boolean follow(World.Unit u,Route route){
        if(route==null||route.path.size()<2)return false;int used=0;List<Hex> prefix=new ArrayList<>();prefix.add(u.hex);
        for(int i=1;i<route.path.size();i++){
            Hex h=route.path.get(i);used+=w.army.moveCost(u,route.path.get(i-1),h);if(used>w.orders.remaining(u))break;
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
        for(World.City c:cities())if(homes.test(c)&&canEnter(u,c)){Route r=route(u,c.hex,1);if(r!=null&&(best==null||r.cost<best.cost)){best=r;home=c;}}
        if(home==null)return false;
        if(u.hex.distance(home.hex)>1)follow(u,best);
        if(w.unit(u.id)!=null&&u.hex.distance(home.hex)==1&&w.enter(u.id,home.id).ok)return true;
        return best!=null;
    }
    private World.Unit at(World.Unit u,Hex h){World.Unit copy=new World.Unit(u.id,u.owner,u.officerId,u.weapon,h,u.troops,u.food);copy.ship=u.ship;copy.deputies=u.deputies;copy.energy=u.energy;return copy;}
    private int exposure(World.Unit u,Hex h){
        int threat=hazard(u,h)*100;World.Unit probe=at(u,h);
        for(World.Unit enemy:w.units)if(w.campaign.hostile(u.owner,enemy.owner)&&enemy.status==War.Status.NORMAL){
            int distance=enemy.hex.distance(h);if(distance<=w.war.range(enemy))threat+=damage(enemy,probe,1,false);
            else if(distance<=w.war.movement(enemy)+w.war.range(enemy))threat+=damage(enemy,probe,1,false)/6;
        }
        return threat;
    }
    private void reposition(World.Unit u){
        if(w.war.range(u)<=1||w.orders.remaining(u)==0)return;
        Hex best=u.hex;int bestScore=Integer.MIN_VALUE;
        for(Map.Entry<Hex,Integer> e:w.orders.reachable(u).entrySet()){
            World.Unit probe=at(u,e.getKey());int offense=0;
            for(World.Unit b:w.units)if(w.campaign.hostile(u.owner,b.owner)&&probe.hex.distance(b.hex)<=w.war.range(probe)&&
                w.fieldworks.landTarget(u.owner,b.hex)&&(!w.army.water(probe.hex)&&probe.weapon==World.Weapon.CROSSBOW?w.terrain[b.hex.q][b.hex.r]!=World.Terrain.FOREST||w.skills.has(probe,Skill.SHESHOU):true))
                offense=Math.max(offense,value(b,damage(probe,b,1,false)));
            if(offense==0)continue;
            int score=offense-exposure(u,e.getKey())-e.getValue()*8;
            if(score>bestScore){bestScore=score;best=e.getKey();}
        }
        if(!best.equals(u.hex))follow(u,route(u,best,0));
    }
    /** Shared by computer factions and delegated districts. Attack permission is checked before every offensive action. */
    public void runUnit(World.Unit u,boolean attack,Predicate<World.City> objectives,Predicate<World.City> homes){
        if(w.orders.error(u)!=null)return;
        if((u.troops<1500||foodTurns(u)<4)&&retreat(u,homes)){
            if(w.unit(u.id)!=null&&!u.acted)w.war.waitUnit(u.id);return;
        }
        if(attack)reposition(u);
        Action action=bestAction(u.id,attack,objectives);if(action!=null&&execute(action).ok)return;
        Route best=null;int bestScore=Integer.MAX_VALUE;
        if(attack){
            // Nearby hostile units take precedence over distant city objectives.
            for(World.Unit enemy:units())if(w.campaign.hostile(u.owner,enemy.owner)&&u.hex.distance(enemy.hex)<=8){
                Route r=route(u,enemy.hex,Math.max(1,w.war.range(u)));if(r!=null&&r.path.size()>1&&r.cost<bestScore){best=r;bestScore=r.cost;}
            }
            if(best==null)for(World.City c:cities())if(w.campaign.hostile(u.owner,c.owner)&&objectives.test(c)){
                Route r=route(u,c.hex,Math.max(1,w.army.siegeRange(u)));int score=r==null?Integer.MAX_VALUE:r.cost+c.troops/3000+c.defense/1000;
                if(r!=null&&score<bestScore){best=r;bestScore=score;}
            }
        }
        if(best!=null)follow(u,best);else retreat(u,homes);
        if(w.unit(u.id)==null||u.acted)return;
        action=bestAction(u.id,attack,objectives);if(action==null||!execute(action).ok)w.war.waitUnit(u.id);
    }
    public void runUnits(){for(World.Unit u:units())if(u.owner==w.active&&!w.gameOver())runUnit(u,true,c->true,c->c.owner==u.owner);}
}
