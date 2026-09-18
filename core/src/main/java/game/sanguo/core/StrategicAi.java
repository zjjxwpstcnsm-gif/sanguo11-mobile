package game.sanguo.core;

import java.util.*;
import java.util.function.ToIntFunction;

/** Resource-aware, deterministic priority planning. Execution uses the same commands as the player. */
public final class StrategicAi {
    public enum Command { REWARD, PATROL, HIRE, RECRUIT, TRAIN, SEARCH, APPOINT, BUILD }
    public static final class Decision {
        public final int cityId, officerId, targetId, priority;
        public final Command command;
        public final String reason;
        private Decision(World.City c,World.Officer o,int target,Command command,int priority,String reason){
            cityId=c.id;officerId=o.id;targetId=target;this.command=command;this.priority=priority;this.reason=reason;
        }
    }
    private final World w;
    public StrategicAi(World w){this.w=Objects.requireNonNull(w);}
    public int pressure(World.City c){
        if(c==null)return 0;int score=0;
        for(World.City enemy:w.cities)if(enemy.owner>=0&&w.campaign.hostile(enemy.owner,c.owner))
            score=Math.max(score,Math.max(0,9-c.hex.distance(enemy.hex))*5);
        for(World.Unit u:w.units)if(w.campaign.hostile(u.owner,c.owner)&&u.hex.distance(c.hex)<=6)
            score+=Math.max(5,u.troops/250)*(7-u.hex.distance(c.hex));
        return Math.min(100,score);
    }
    private World.Officer best(List<World.Officer> idle,ToIntFunction<World.Officer> skill){
        return idle.stream().min(Comparator.<World.Officer>comparingInt(o->-skill.applyAsInt(o)).thenComparingInt(o->o.id)).orElseThrow(()->new IllegalStateException("No idle officer"));
    }
    private void add(List<Decision> list,World.City c,World.Officer o,int target,Command command,int score,String reason){
        list.add(new Decision(c,o,target,command,score,reason));
    }
    public Decision plan(int cityId,boolean emergency){
        World.City c=w.city(cityId);
        if(c==null||c.owner!=w.active||w.gameOver()||w.actionPoints[w.active]<10)return null;
        List<World.Officer> idle=w.idle(c);if(idle.isEmpty())return null;
        World.Officer admin=best(idle,o->o.politics+o.charm),charmer=best(idle,o->2*o.charm+o.politics);
        int pressure=pressure(c),residents=0;
        for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id)residents++;
        List<Decision> choices=new ArrayList<>();
        if(c.gold>=Strategy.REWARD_COST)for(World.Officer target:w.officers){
            if(target.owner!=c.owner||target.cityId!=c.id||target.role==Strategy.Role.RULER||target.loyalty>=60||target.lastRewardTurn==w.turn||target.unitId>=0||w.domestic.busy(target.id)||w.strategy.busy(target.id))continue;
            int core=Math.max(target.politics,Math.max(target.leadership,target.charm));
            if(core>=75||target.role==Strategy.Role.GOVERNOR)
                add(choices,c,admin,target.id,Command.REWARD,120+60-target.loyalty,"保留低忠诚核心武将");
        }
        if(c.gold>=Strategy.PATROL_COST&&c.order<(emergency?45:75))
            add(choices,c,admin,-1,Command.PATROL,40+(75-c.order)*2+pressure/4,"恢复治安和收入/征兵条件");
        if(!emergency){
            if(c.gold>=Strategy.HIRE_COST)for(World.Officer target:w.strategy.recruitmentTargets(c.id)){
                int travel=w.recruitment.travelTurns(c.id,target.id);if(travel>3||travel>0&&residents<3)continue;
                int chance=w.strategy.recruitmentChance(c.id,charmer.id,target.id);
                if(chance>=50)add(choices,c,charmer,target.id,Command.HIRE,35+Math.max(0,3-residents)*20+chance/10-travel*8,"补充人才；按实际登用概率决策");
            }
            int desired=pressure>=40?15000:8000;
            if(c.kind==World.SiteKind.CITY&&c.gold>=Strategy.RECRUIT_COST&&c.troops<desired&&c.order>=45&&c.recruitReserve>0&&w.domestic.operationError(c.id,Domestic.Kind.BARRACKS)==null&&w.strategy.recruitAmount(c.id,charmer.id)>0)
                add(choices,c,charmer,-1,Command.RECRUIT,30+(desired-c.troops)/500+pressure/3,"按兵源、守军缺口和周边压力补兵");
            if(c.kind==World.SiteKind.CITY&&!w.domestic.buildSites(c.id).isEmpty()){
                Domestic.Kind needed=c.troops<desired&&c.recruitReserve>0?Domestic.Kind.BARRACKS:null;
                addFacility(choices,c,admin,needed,58,"先建设兵舍，恢复征兵能力");
                if(w.districts.productionError(c.id)==null){
                    World.Weapon preferred=World.Weapon.SPEAR;int rank=-1;
                    for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY})
                        for(World.Officer o:idle)if(o.aptitude[Army.category(weapon)]>rank){preferred=weapon;rank=o.aptitude[Army.category(weapon)];}
                    if(c.equipment[preferred.ordinal()]<8000)addFacility(choices,c,admin,Domestic.productionFacility(preferred),38,"先建设生产设施，补充兵装");
                }
            }
            int readiness=pressure>=40?90:80;
            if(c.gold>=Strategy.TRAIN_COST&&c.troops>0&&c.morale<readiness)
                add(choices,c,best(idle,o->o.leadership+o.war/4),-1,Command.TRAIN,20+readiness-c.morale+pressure/4,"提升现有守军战备");
            boolean hidden=!w.strategy.discoverable(c.id).isEmpty();
            if(hidden||residents<3||c.gold<300)
                add(choices,c,best(idle,o->o.politics+o.intelligence),-1,Command.SEARCH,22+Math.max(0,3-residents)*12+(hidden?15:0)+(c.gold<200?30:0),"人才不足或资金短缺，搜索人才/少量金");
            World.Officer governor=best(idle,o->o.politics);
            if(c.governorId<0&&governor.politics>=60)
                add(choices,c,governor,governor.id,Command.APPOINT,18,"任命太守，提高长期收入");
        }
        choices.sort(Comparator.comparingInt((Decision d)->-d.priority).thenComparingInt(d->d.command.ordinal()).thenComparingInt(d->d.officerId).thenComparingInt(d->d.targetId));
        return choices.isEmpty()?null:choices.get(0);
    }
    public World.Result execute(Decision d){
        if(d==null)return w.fail("无可执行战略决策");
        switch(d.command){
            case SEARCH:return w.strategy.search(d.cityId,d.officerId);
            case HIRE:return w.strategy.recruitOfficer(d.cityId,d.officerId,d.targetId);
            case REWARD:return w.strategy.rewardOfficer(d.cityId,d.officerId,d.targetId);
            case PATROL:return w.strategy.patrol(d.cityId,d.officerId);
            case RECRUIT:return w.strategy.recruitSoldiers(d.cityId,d.officerId);
            case TRAIN:return w.strategy.trainArmy(d.cityId,d.officerId);
            case APPOINT:return w.strategy.appointGovernor(d.cityId,d.officerId,d.targetId);
            case BUILD:
                List<Hex> sites=w.domestic.buildSites(d.cityId);
                return sites.isEmpty()?w.fail("没有空闲开发地"):w.domestic.build(d.cityId,d.officerId,Domestic.Kind.values()[d.targetId],sites.get(0));
            default:throw new AssertionError(d.command);
        }
    }
    private void addFacility(List<Decision> choices,World.City c,World.Officer admin,Domestic.Kind kind,int priority,String reason){
        if(kind!=null&&c.gold>=kind.cost+500&&w.domestic.facilities.stream().noneMatch(f->f.cityId==c.id&&f.kind==kind))
            add(choices,c,admin,kind.ordinal(),Command.BUILD,w.strategy.discoverable(c.id).isEmpty()?priority:Math.min(priority,30),reason);
    }
    /** At most one order per city/pass, preserving AP and officers for existing construction/military AI. */
    public void run(boolean emergency){
        List<World.City> cities=new ArrayList<>();for(World.City c:w.cities)if(c.owner==w.active)cities.add(c);
        cities.sort(Comparator.comparingInt((World.City c)->-pressure(c)).thenComparingInt(c->c.id));
        for(World.City c:cities){Decision d=plan(c.id,emergency);if(d!=null)execute(d);}
    }
}
