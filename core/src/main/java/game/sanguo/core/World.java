package game.sanguo.core;

import java.util.*;

/** Engineering rules, NOT original SAN11 formulas. All commands validate before mutation. */
public final class World {
    public enum Terrain { PLAIN, FOREST, MOUNTAIN, WATER }
    public enum Weapon {
        SPEAR("枪兵",4,1,115), HALBERD("戟兵",3,1,105), CROSSBOW("弩兵",3,2,95), CAVALRY("骑兵",6,1,120);
        public final String label;
        public final int movement, range, power;
        Weapon(String label,int movement,int range,int power) { this.label=label;this.movement=movement;this.range=range;this.power=power; }
    }
    public static final class City {
        public final int id;
        public final String name;
        public final Hex hex;
        public int owner, gold=5000, food=40000, troops=12000, order=90, morale=70, defense=3000;
        public final int[] equipment={12000,12000,12000,12000};
        public City(int id,String name,Hex hex,int owner) { this.id=id;this.name=name;this.hex=hex;this.owner=owner; }
    }
    public static final class Officer {
        public final int id;
        public final String name;
        public int owner, cityId, unitId=-1, leadership, war, intelligence, politics, charm;
        public boolean acted;
        public Officer(int id,String name,int owner,int city,int l,int w,int i,int p,int c) {
            this.id=id;this.name=name;this.owner=owner;this.cityId=city;
            leadership=l;war=w;intelligence=i;politics=p;charm=c;
        }
    }
    public static final class Unit {
        public final int id, owner, officerId;
        public final Weapon weapon;
        public Hex hex;
        public int troops, food, energy=80;
        public boolean acted;
        public Unit(int id,int owner,int officerId,Weapon weapon,Hex hex,int troops,int food) {
            this.id=id;this.owner=owner;this.officerId=officerId;this.weapon=weapon;this.hex=hex;this.troops=troops;this.food=food;
        }
    }
    public static final class Result {
        public final boolean ok;
        public final String message;
        private Result(boolean ok,String message) { this.ok=ok;this.message=message; }
    }
    public final int width,height;
    public final Terrain[][] terrain;
    public final List<City> cities=new ArrayList<>();
    public final List<Officer> officers=new ArrayList<>();
    public final List<Unit> units=new ArrayList<>();
    public final List<String> log=new ArrayList<>();
    public final String[] factions;
    public final int[] actionPoints;
    public String scenarioId="m0-skirmish", scenarioName="基础演练", dataSource="engineering-original", dataHash="";
    public int dataRevision=1, startYear=190, startMonth=1, player=0;
    public int turn=0, active=0, nextUnitId=1, winner=-1;
    public World(int width,int height) {
        this(width,height,"刘备军","曹操军");
    }
    public World(int width,int height,String... factions) {
        if(width<1||width>128||height<1||height>128||factions.length<2||factions.length>32)throw new IllegalArgumentException("地图或势力数量无效");
        this.factions=factions.clone();actionPoints=new int[factions.length];Arrays.fill(actionPoints,60);
        this.width=width;this.height=height;terrain=new Terrain[width][height];
        for (Terrain[] row:terrain) Arrays.fill(row,Terrain.PLAIN);
    }
    public String date() { int month=startMonth-1+turn/3;return (startYear+month/12)+"年 "+(month%12+1)+"月 "+new String[]{"上旬","中旬","下旬"}[turn%3]; }
    public String faction(int owner) { return owner>=0&&owner<factions.length?factions[owner]:"空城"; }
    public boolean alive(int owner) {
        for(City c:cities)if(c.owner==owner)return true;
        for(Unit u:units)if(u.owner==owner)return true;
        return false;
    }
    public boolean gameOver() { return winner>=0||!alive(player); }
    public City home() { for(City c:cities)if(c.owner==player)return c;return cities.isEmpty()?null:cities.get(0); }
    public boolean inside(Hex h) { return h.q>=0&&h.r>=0&&h.q<width&&h.r<height; }
    public City city(int id) { for(City c:cities) if(c.id==id) return c;return null; }
    public Officer officer(int id) { for(Officer o:officers) if(o.id==id) return o;return null; }
    public Unit unit(int id) { for(Unit u:units) if(u.id==id) return u;return null; }
    public City cityAt(Hex h) { for(City c:cities) if(c.hex.equals(h)) return c;return null; }
    public Unit unitAt(Hex h) { for(Unit u:units) if(u.hex.equals(h)) return u;return null; }
    private Result fail(String text) { return new Result(false,text); }
    private Result success(String text) { note(text);return new Result(true,text); }
    public void note(String text) { log.add(text);while(log.size()>40)log.remove(0); }
    private boolean available(Officer o,City c) { return o!=null&&o.owner==active&&o.cityId==c.id&&o.unitId<0&&!o.acted; }
    public List<Officer> idle(City c) {
        List<Officer> found=new ArrayList<>();
        if(c!=null&&c.owner==active) for(Officer o:officers) if(available(o,c))found.add(o);
        return found;
    }
    private String cityError(City c,Officer o,int gold) {
        if(gameOver())return "本局已结束";
        if(c==null||c.owner!=active)return "请选择己方城池";
        if(!available(o,c))return "需要一名本旬尚未行动的在城武将";
        if(actionPoints[active]<10)return "行动力不足10";
        if(c.gold<gold)return "金不足";
        return null;
    }
    private void spend(City c,Officer o,int gold) { c.gold-=gold;actionPoints[active]-=10;o.acted=true; }
    public Result recruit(int cityId,int officerId) {
        City c=city(cityId);Officer o=officer(officerId);String error=cityError(c,o,300);
        if(error!=null)return fail(error);
        if(c.order<30)return fail("治安低于30，先执行巡察");
        if(c.troops>98000)return fail("测试城池兵力已接近上限");
        spend(c,o,300);c.troops+=2000;c.order-=5;
        return success(c.name+"征得2000兵，治安−5");
    }
    public Result train(int cityId,int officerId) {
        City c=city(cityId);Officer o=officer(officerId);String error=cityError(c,o,100);
        if(error!=null)return fail(error);
        if(c.morale>=100)return fail("气力已满");
        spend(c,o,100);c.morale=Math.min(100,c.morale+15);return success(c.name+"训练完成");
    }
    public Result patrol(int cityId,int officerId) {
        City c=city(cityId);Officer o=officer(officerId);String error=cityError(c,o,100);
        if(error!=null)return fail(error);
        if(c.order>=100)return fail("治安已满");
        spend(c,o,100);c.order=Math.min(100,c.order+10);return success(c.name+"治安提升");
    }
    public Result produce(int cityId,int officerId,Weapon weapon) {
        City c=city(cityId);Officer o=officer(officerId);String error=cityError(c,o,400);
        if(error!=null)return fail(error);
        if(weapon==null)return fail("兵装无效");
        if(c.equipment[weapon.ordinal()]>98000)return fail("兵装已接近上限");
        spend(c,o,400);c.equipment[weapon.ordinal()]+=2000;return success(c.name+"生产2000份"+weapon.label+"兵装");
    }
    public Result deploy(int cityId,int officerId,Weapon weapon,int troops) {
        City c=city(cityId);Officer o=officer(officerId);String error=cityError(c,o,0);
        if(error!=null)return fail(error);
        if(weapon==null||troops<1000||troops>10000)return fail("出征人数必须在1000至10000之间");
        if(c.troops<troops||c.food<troops*2||c.equipment[weapon.ordinal()]<troops)return fail("兵、兵装或出征粮草不足");
        Hex exit=null;
        for(Hex h:c.hex.neighbors()) if(inside(h)&&cost(h,weapon)>0&&unitAt(h)==null&&cityAt(h)==null){exit=h;break;}
        if(exit==null)return fail("城外相邻格全部被占用或无法通行");
        spend(c,o,0);c.troops-=troops;c.food-=troops*2;c.equipment[weapon.ordinal()]-=troops;
        Unit u=new Unit(nextUnitId++,active,o.id,weapon,exit,troops,troops*2);u.energy=c.morale;
        units.add(u);o.unitId=u.id;o.cityId=-1;
        return success(o.name+"率"+troops+weapon.label+"出征");
    }
    public int cost(Hex h,Weapon weapon) {
        if(!inside(h))return -1;
        Terrain t=terrain[h.q][h.r];
        if(t==Terrain.MOUNTAIN||t==Terrain.WATER)return -1;
        return t==Terrain.FOREST?(weapon==Weapon.CAVALRY?3:2):1;
    }
    private static final class Step {
        final Hex hex;final int cost;
        Step(Hex h,int c){hex=h;cost=c;}
    }
    /** Dijkstra: excludes city/unit occupancy and includes the starting tile at cost zero. */
    public Map<Hex,Integer> reachable(Unit u) {
        Map<Hex,Integer> distances=new LinkedHashMap<>();
        if(u==null||u.acted)return distances;
        PriorityQueue<Step> todo=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.hex.q).thenComparingInt(s->s.hex.r));
        distances.put(u.hex,0);todo.add(new Step(u.hex,0));
        while(!todo.isEmpty()) {
            Step s=todo.remove();if(s.cost!=distances.get(s.hex))continue;
            for(Hex next:s.hex.neighbors()) {
                int c=cost(next,u.weapon);if(c<0||cityAt(next)!=null)continue;
                Unit occupant=unitAt(next);if(occupant!=null&&occupant.id!=u.id)continue;
                int total=s.cost+c;
                if(total<=u.weapon.movement&&total<distances.getOrDefault(next,Integer.MAX_VALUE)) {
                    distances.put(next,total);todo.add(new Step(next,total));
                }
            }
        }
        return distances;
    }
    private String unitError(Unit u) {
        if(gameOver())return "本局已结束";
        if(u==null||u.owner!=active)return "请选择当前势力的部队";
        if(u.acted)return "这支部队本旬已行动";
        return null;
    }
    public Result move(int unitId,Hex destination) {
        Unit u=unit(unitId);String error=unitError(u);if(error!=null)return fail(error);
        if(destination==null||destination.equals(u.hex)||!reachable(u).containsKey(destination))return fail("目标格不可达");
        u.hex=destination;u.acted=true;return success(officer(u.officerId).name+"部队移动");
    }
    public Result attack(int attackerId,int targetId) {
        Unit a=unit(attackerId),b=unit(targetId);String error=unitError(a);if(error!=null)return fail(error);
        if(b==null||b.owner==a.owner)return fail("请选择敌军");
        if(a.hex.distance(b.hex)>a.weapon.range)return fail("敌军不在攻击范围");
        int damage=damage(a,officer(b.officerId).leadership,b.weapon==Weapon.HALBERD?110:100);
        damage=Math.min(damage,b.troops);b.troops-=damage;a.acted=true;a.energy=Math.max(0,a.energy-5);
        String message=officer(a.officerId).name+"攻击，敌军损失"+damage;
        if(b.troops==0){message+="，敌军溃散";removeUnit(b);}
        checkVictory();return success(message);
    }
    private int damage(Unit a,int enemyLeadership,int defenseScale) {
        Officer o=officer(a.officerId);
        long value=(long)a.troops*a.weapon.power*(60+o.leadership)*(50+a.energy);
        return Math.max(80,(int)(value/(100L*(80+enemyLeadership)*100*defenseScale/10)));
    }
    public Result siege(int unitId,int cityId) {
        Unit u=unit(unitId);City c=city(cityId);String error=unitError(u);if(error!=null)return fail(error);
        if(c==null||c.owner==u.owner)return fail("请选择非己方城池");
        if(u.hex.distance(c.hex)>1)return fail("攻城需要邻接城池");
        int hit=Math.max(100,damage(u,70,120)/2);c.defense=Math.max(0,c.defense-hit);c.troops=Math.max(0,c.troops-hit);
        u.acted=true;u.energy=Math.max(0,u.energy-5);
        String message=officer(u.officerId).name+"攻城，城防−"+hit;
        if(c.defense==0) {
            int old=c.owner;c.owner=u.owner;c.defense=1500;c.troops=0;c.morale=50;c.order=60;
            for(Officer o:officers) if(o.cityId==c.id&&o.owner==old)retreat(o,c.hex);
            message=c.name+"被"+faction(u.owner)+"攻占";
        }
        checkVictory();return success(message);
    }
    public Result enter(int unitId,int cityId) {
        Unit u=unit(unitId);City c=city(cityId);String error=unitError(u);if(error!=null)return fail(error);
        if(c==null||c.owner!=u.owner||u.hex.distance(c.hex)>1)return fail("请选择相邻己方城池");
        if(c.troops+u.troops>100000||c.equipment[u.weapon.ordinal()]+u.troops>100000||c.food+u.food>1000000)return fail("城池库存容量不足");
        c.troops+=u.troops;c.food+=u.food;c.equipment[u.weapon.ordinal()]+=u.troops;
        Officer o=officer(u.officerId);o.unitId=-1;o.cityId=c.id;o.acted=true;units.remove(u);
        return success(o.name+"入城休整");
    }
    private void retreat(Officer o,Hex from) {
        City destination=null;
        for(City c:cities)if(c.owner==o.owner&&(destination==null||from.distance(c.hex)<from.distance(destination.hex)))destination=c;
        o.unitId=-1;o.cityId=destination==null?-1:destination.id;o.acted=true;
    }
    private void removeUnit(Unit u) { retreat(officer(u.officerId),u.hex);units.remove(u); }
    public Result nextTurn() {
        if(gameOver())return fail("本局已结束，请重开");
        if(active!=player)return fail("等待电脑行动");
        for(int offset=1;offset<factions.length;offset++) {
            active=(player+offset)%factions.length;
            if(!alive(active))continue;
            reset(active);runAi();checkVictory();
            if(gameOver()){active=player;return success(winner==player?"战场胜利":"我方势力已覆灭");}
        }
        turn++;
        for(Unit u:new ArrayList<>(units)) {
            int consumption=Math.max(1,(u.troops+19)/20);
            if(u.food<consumption){u.food=0;u.troops-=Math.max(1,u.troops/10);note(officer(u.officerId).name+"部队断粮，兵力减少");}
            else u.food-=consumption;
            if(u.troops<=0)removeUnit(u);
        }
        for(City c:cities) if(c.owner>=0) {
            int consumption=(c.troops+49)/50;
            if(c.food<consumption){c.food=0;c.troops=Math.max(0,c.troops-Math.max(1,c.troops/20));}
            else c.food-=consumption;
            if(turn%3==0){c.gold=Math.min(1000000,c.gold+800);c.food=Math.min(1000000,c.food+5000);}
        }
        active=player;reset(player);checkVictory();return success(date()+" · 行动力恢复");
    }
    private void reset(int owner) {
        actionPoints[owner]=60;
        for(Officer o:officers)if(o.owner==owner)o.acted=false;
        for(Unit u:units)if(u.owner==owner)u.acted=false;
    }
    private void runAi() {
        for(City c:cities) if(c.owner==active) {
            List<Officer> available=idle(c);
            if(!available.isEmpty()) {
                Officer o=available.get(0);
                Weapon weapon=null;
                for(Weapon candidate:Weapon.values())if(c.equipment[candidate.ordinal()]>=3000){weapon=candidate;break;}
                if(c.troops>=6000&&c.food>=6000&&weapon!=null)deploy(c.id,o.id,weapon,3000);
                else if(c.order<50)patrol(c.id,o.id);
                else if(weapon==null)produce(c.id,o.id,Weapon.SPEAR);
                else recruit(c.id,o.id);
            }
        }
        for(Unit u:new ArrayList<>(units)) {
            if(gameOver())break;
            if(u.owner!=active||u.acted)continue;
            Unit enemy=null;
            for(Unit b:units)if(b.owner!=u.owner&&u.hex.distance(b.hex)<=u.weapon.range){enemy=b;break;}
            if(enemy!=null){attack(u.id,enemy.id);continue;}
            List<City> targets=new ArrayList<>();
            for(City c:cities)if(c.owner!=active)targets.add(c);
            targets.sort(Comparator.comparingInt((City c)->u.hex.distance(c.hex)).thenComparingInt(c->c.id));
            for(City target:targets) {
                if(u.hex.distance(target.hex)==1){siege(u.id,target.id);break;}
                if(advance(u,target.hex,1))break;
            }
            if(!u.acted)for(Unit b:new ArrayList<>(units))if(b.owner!=active&&advance(u,b.hex,u.weapon.range))break;
        }
    }
    /** Plan beyond one turn so AI can detour around rivers and mountains. */
    private boolean advance(Unit u,Hex target,int range) {
        Map<Hex,Integer> distances=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
        Set<Hex> blocked=new HashSet<>();for(City c:cities)blocked.add(c.hex);for(Unit b:units)if(b.id!=u.id)blocked.add(b.hex);
        PriorityQueue<Step> todo=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost).thenComparingInt(s->s.hex.q).thenComparingInt(s->s.hex.r));
        distances.put(u.hex,0);todo.add(new Step(u.hex,0));
        while(!todo.isEmpty()) {
            Step step=todo.remove();if(step.cost!=distances.get(step.hex))continue;
            if(step.hex.distance(target)<=range) {
                Hex destination=step.hex;
                while(distances.get(destination)>u.weapon.movement)destination=previous.get(destination);
                return !destination.equals(u.hex)&&move(u.id,destination).ok;
            }
            for(Hex next:step.hex.neighbors()) {
                int cost=cost(next,u.weapon);if(cost<0||blocked.contains(next))continue;
                int total=step.cost+cost;
                if(total<distances.getOrDefault(next,Integer.MAX_VALUE)) {
                    distances.put(next,total);previous.put(next,step.hex);todo.add(new Step(next,total));
                }
            }
        }
        return false;
    }
    public void checkVictory() {
        int count=0,last=-1;
        for(int side=0;side<factions.length;side++)if(alive(side)){count++;last=side;}
        winner=count==1?last:-1;
    }
}
