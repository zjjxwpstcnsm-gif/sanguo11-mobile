package game.sanguo.core;

import java.util.*;

/** Seasonal hazards and persistent raider camps; independent saved RNG does not perturb combat draws. */
public final class WorldEvents {
    public enum Tribe {
        BANDIT("盗贼",null), WU("乌丸",Skill.QINWU), QIANG("羌",Skill.QINQIANG), YUE("山越",Skill.QINYUE), MAN("南蛮",Skill.QINMAN);
        public final String label;final Skill protection;Tribe(String label,Skill protection){this.label=label;this.protection=protection;}
    }
    public enum Disaster { LOCUST("蝗灾"), PLAGUE("疫病");public final String label;Disaster(String label){this.label=label;} }
    public static final class Hazard {
        public final int city;public final Disaster kind;public final int until;
        Hazard(int city,Disaster kind,int until){this.city=city;this.kind=kind;this.until=until;}
    }
    public static final class Camp {
        public final int id,city;public final Tribe tribe;public final Hex hex;public int troops;
        Camp(int id,int city,Tribe tribe,Hex hex,int troops){this.id=id;this.city=city;this.tribe=tribe;this.hex=hex;this.troops=troops;}
    }
    private final World w;
    final SortedMap<Integer,Tribe> regions=new TreeMap<>();
    final SortedMap<Integer,Hazard> hazards=new TreeMap<>();
    final List<Camp> camps=new ArrayList<>();
    boolean enabled;long randomState=0x3b19114c7e2dL;int nextCamp=1,lastTick=-1;
    WorldEvents(World w){this.w=w;}
    public boolean enabled(){return enabled;}
    public List<Hazard> hazards(){return Collections.unmodifiableList(new ArrayList<>(hazards.values()));}
    public List<Camp> camps(){return Collections.unmodifiableList(camps);}
    public Camp at(Hex h){for(Camp c:camps)if(c.hex.equals(h))return c;return null;}
    public Camp camp(int id){for(Camp c:camps)if(c.id==id)return c;return null;}
    public Tribe region(int city){return regions.getOrDefault(city,Tribe.BANDIT);}
    public String cityStatus(int city){Hazard h=hazards.get(city);String text=h==null?"无灾害":h.kind.label+" · 剩"+Math.max(0,h.until-w.turn)+"旬";for(Camp c:camps)if(c.city==city)text+=" · "+c.tribe.label+"营寨 "+c.troops+"兵";return text;}
    public World.Result toggle(){
        if(w.contests.busy()||w.gameOver()||w.active!=w.player)return w.fail("当前不能变更灾害设置");
        enabled=!enabled;return w.success(enabled?"已开启季节灾害和月度贼患":"已停止产生新灾害和贼患；已有灾害和营寨仍会结算");
    }
    public void configureRegion(int city,Tribe tribe){if(w.city(city)==null||tribe==null)throw new IllegalArgumentException("异族区域无效");if(tribe==Tribe.BANDIT)regions.remove(city);else regions.put(city,tribe);}
    int nextInt(int bound){randomState=randomState*6364136223846793005L+1442695040888963407L;return (int)((randomState>>>1)%bound);}
    public int raidChance(int city,Tribe tribe){
        World.City c=w.city(city);if(c==null||c.owner<0||c.kind!=World.SiteKind.CITY||tribe==null)return 0;
        if(tribe!=Tribe.BANDIT&&(tribe!=region(city)||w.skills.city(city,tribe.protection)))return 0;
        int threshold=w.skills.city(city,Skill.WEIYA)?60:80;
        return c.order>=threshold?0:Math.min(40,(threshold-c.order+1)/2);
    }
    public int harvestChance(int city){return w.skills.city(city,Skill.QIYUAN)?30:10;}
    boolean beginDisaster(int city,Disaster kind){
        World.City c=w.city(city);if(c==null||c.owner<0||c.kind!=World.SiteKind.CITY||hazards.containsKey(city)||w.skills.city(city,Skill.FENGSHUI))return false;
        hazards.put(city,new Hazard(city,kind,w.turn+3));w.note(c.name+"发生"+kind.label);return true;
    }
    boolean spawn(int city,Tribe tribe){
        World.City c=w.city(city);if(c==null||nextCamp>=10000000||raidChance(city,tribe)==0)return false;
        for(Camp existing:camps)if(existing.city==city)return false;
        List<Hex> sites=new ArrayList<>();
        for(int q=Math.max(0,c.hex.q-4);q<=Math.min(w.width-1,c.hex.q+4);q++)for(int r=Math.max(0,c.hex.r-4);r<=Math.min(w.height-1,c.hex.r+4);r++){
            Hex h=new Hex(q,r);int distance=h.distance(c.hex);
            if(distance>=3&&distance<=4&&w.cost(h,World.Weapon.SPEAR)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.war.fireAt(h)==null&&at(h)==null&&w.cities.stream().noneMatch(other->other.hex.distance(h)<=2))sites.add(h);
        }
        if(sites.isEmpty())return false;
        camps.add(new Camp(nextCamp++,city,tribe,sites.get(nextInt(sites.size())),3000));w.note(c.name+"附近出现"+tribe.label+"营寨，可派兵讨伐");return true;
    }
    public String attackError(int unit,int camp){
        World.Unit u=w.unit(unit);String error=w.orders.error(u);if(error!=null)return error;Camp c=camp(camp);
        return c==null||u.hex.distance(c.hex)<1||u.hex.distance(c.hex)>w.war.range(u)?"请选择射程内贼寨":null;
    }
    public World.Result attack(int unit,int camp){
        String error=attackError(unit,camp);if(error!=null)return w.fail(error);World.Unit u=w.unit(unit);Camp c=camp(camp);u.acted=true;
        int hit=Math.min(c.troops,300+w.army.war(u)*6+u.troops/12);c.troops-=hit;
        int counter=0;if(c.troops==0){camps.remove(c);u.gold=Math.min(10000,u.gold+500);w.campaign.earn(u.owner,50);w.government.earn(u.officerId,200);}
        else if(u.hex.distance(c.hex)==1){counter=Math.min(u.troops,100+c.troops/15);u.troops-=counter;if(u.troops==0)w.removeUnit(u);}
        return w.success("讨伐"+c.tribe.label+"：敌损"+hit+"，反击损失"+counter+(c.troops==0?"，营寨已毁，获得500金（受携金上限限制）":""));
    }
    void tick(){
        if(lastTick==w.turn)return;lastTick=w.turn;
        for(World.Unit u:w.units)if(w.terrain[u.hex.q][u.hex.r]==World.Terrain.POISON&&!w.skills.has(u,Skill.JIEDU)){int loss=Math.max(1,u.troops/20);u.troops=Math.max(1,u.troops-loss);w.note(w.officer(u.officerId).name+"驻留毒泉，损失"+loss+"兵");}
        hazards.values().removeIf(h->h.until<=w.turn);
        for(Hazard h:new ArrayList<>(hazards.values())){
            World.City c=w.city(h.city);if(c.owner<0){hazards.remove(c.id);continue;}
            c.order=Math.max(0,c.order-3);
            if(h.kind==Disaster.LOCUST){c.food-=c.food/10;if(nextInt(100)<20)destroyFacility(c.id,Domestic.Kind.FARM);}
            else {c.troops-=c.troops/10;List<World.Officer> people=new ArrayList<>();for(World.Officer o:w.officers)if(o.owner==c.owner&&o.cityId==c.id&&!w.government.captive(o.id)&&!w.skills.has(o,Skill.QIANGYUN))people.add(o);if(!people.isEmpty()&&nextInt(100)<20){World.Officer o=people.get(nextInt(people.size()));w.contests.injuries.put(o.id,new Contests.Injury(Math.min(3,w.contests.injury(o.id)+1),w.turn+3));}}
        }
        for(Camp camp:new ArrayList<>(camps)){
            World.City c=w.city(camp.city);if(c.owner<0)continue;
            World.Unit nearest=null;for(World.Unit u:w.units)if(u.hex.distance(camp.hex)<=1&&(nearest==null||u.id<nearest.id))nearest=u;
            if(nearest!=null){int hit=Math.min(nearest.troops,100+camp.troops/20);nearest.troops-=hit;if(nearest.troops==0)w.removeUnit(nearest);w.note(camp.tribe.label+"袭击邻近部队，损失"+hit+"兵");}
            if(w.turn%3==0){c.order=Math.max(0,c.order-5);c.food=Math.max(0,c.food-1000);camp.troops=Math.min(6000,camp.troops+200);if(nextInt(100)<25)destroyFacility(c.id,null);w.note(c.name+"受到"+camp.tribe.label+"劫掠，粮草与治安下降");}
        }
        if(!enabled||w.turn%3!=0)return;
        int month=(w.startMonth-1+w.turn/3)%12+1;
        List<World.City> cities=new ArrayList<>(w.cities);cities.sort(Comparator.comparingInt(c->c.id));
        for(World.City c:cities)if(c.owner>=0&&c.kind==World.SiteKind.CITY){
            int roll=nextInt(100);if(roll<raidChance(c.id,Tribe.BANDIT))spawn(c.id,Tribe.BANDIT);
            Tribe tribe=region(c.id);if(tribe!=Tribe.BANDIT&&nextInt(100)<raidChance(c.id,tribe))spawn(c.id,tribe);
            if((month-1)%3==0){
                if(nextInt(100)<4)beginDisaster(c.id,Disaster.PLAGUE);
                if(month!=10&&nextInt(100)<4)beginDisaster(c.id,Disaster.LOCUST);
                if(month==7&&nextInt(100)<harvestChance(c.id)){int food=w.domestic.foodIncome(c.id,w.turn);c.food=Math.min(w.campaign.foodCap(c),c.food+food);w.note(c.name+"丰收，增加"+food+"粮（受容量限制）");}
            }
        }
    }
    private void destroyFacility(int city,Domestic.Kind kind){
        List<Domestic.Facility> eligible=new ArrayList<>();for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==city&&(kind==null||f.kind==kind))eligible.add(f);
        if(!eligible.isEmpty()){Domestic.Facility f=eligible.get(nextInt(eligible.size()));if(f.builderId>=0)w.officer(f.builderId).acted=true;w.domestic.facilities.remove(f);w.army.cleanup();w.note(w.city(city).name+"的"+f.kind.label+"被毁");}
    }
}
