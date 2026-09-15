package game.sanguo.core;

import java.util.*;

/** Campaign construction and technology effects. Parameters still needing calibration are documented. */
public final class Fieldworks {
    private final World w;
    Fieldworks(World w){this.w=w;}
    public boolean trap(War.StructureKind kind){return kind==War.StructureKind.FIRE_SEED||kind.ordinal()>=War.StructureKind.FIRE_BALL.ordinal();}
    public boolean ball(War.StructureKind kind){return kind==War.StructureKind.FIRE_BALL||kind==War.StructureKind.FLAME_BALL||kind==War.StructureKind.INFERNO_BALL;}
    public boolean camp(War.StructureKind k){return k==War.StructureKind.CAMP||k==War.StructureKind.FORT||k==War.StructureKind.FORTRESS;}
    private boolean military(War.StructureKind k){return !trap(k)&&k!=War.StructureKind.EARTH_WALL&&k!=War.StructureKind.STONE_WALL;}
    public War.StructureKind upgraded(int owner,War.StructureKind kind){
        if(camp(kind))return w.campaign.has(owner,Campaign.Tech.WALLS)?War.StructureKind.FORTRESS:w.campaign.has(owner,Campaign.Tech.FACILITY_REINFORCEMENT)?War.StructureKind.FORT:kind;
        if(kind==War.StructureKind.ARROW_TOWER&&w.campaign.has(owner,Campaign.Tech.FACILITY_REINFORCEMENT))return War.StructureKind.CROSSBOW_TOWER;
        if(trap(kind)&&kind!=War.StructureKind.FIRE_SHIP){
            if(w.campaign.has(owner,Campaign.Tech.EXPLOSIVES))return ball(kind)?War.StructureKind.INFERNO_BALL:War.StructureKind.INFERNO_SEED;
            if(w.campaign.has(owner,Campaign.Tech.GUNPOWDER))return ball(kind)?War.StructureKind.FLAME_BALL:War.StructureKind.FLAME_SEED;
        }
        return kind;
    }
    private boolean unlocked(int owner,War.StructureKind k){
        switch(k){
            case FORT:case CROSSBOW_TOWER:return w.campaign.has(owner,Campaign.Tech.FACILITY_REINFORCEMENT);
            case FORTRESS:return w.campaign.has(owner,Campaign.Tech.WALLS);
            case STONE_WALL:case STONE_MAZE:return w.campaign.has(owner,Campaign.Tech.STONE_BUILDING);
            case CATAPULT_TOWER:return w.campaign.has(owner,Campaign.Tech.CATAPULT);
            case FLAME_SEED:case FLAME_BALL:return w.campaign.has(owner,Campaign.Tech.GUNPOWDER);
            case INFERNO_SEED:case INFERNO_BALL:return w.campaign.has(owner,Campaign.Tech.EXPLOSIVES);
            default:return true;
        }
    }
    public List<War.StructureKind> available(int owner){List<War.StructureKind> out=new ArrayList<>();for(War.StructureKind k:War.StructureKind.values())if(unlocked(owner,k)&&upgraded(owner,k)==k)out.add(k);return out;}
    void upgrade(int owner){for(War.Structure s:w.war.structures)if(s.owner==owner){War.StructureKind k=upgraded(owner,s.kind);if(k!=s.kind){s.hp=Math.min(k.hp,(int)((long)s.hp*k.hp/s.kind.hp));s.kind=k;}}}
    public War.Structure project(int unit){for(War.Structure s:w.war.structures)if(s.builder==unit)return s;return null;}
    public int constructionRate(World.Unit u){
        int politics=0;for(World.Officer o:w.army.crew(u))politics+=o.politics;
        int rate=100+politics*2+Math.min(100,u.troops/100);
        return w.skills.has(u,Skill.ZHUCHENG)?rate*2:rate;
    }
    public String buildError(int unit,War.StructureKind kind,Hex target,int direction){
        World.Unit u=w.unit(unit);String error=w.orders.error(u);if(error!=null)return error;
        if(kind==null||!available(u.owner).contains(kind))return "需要前置技巧，或该设施已被强化版替代";
        if(direction<0||direction>5)return "请选择六个有效方向之一";
        if(project(unit)!=null)return "部队正在施工，请先中止";
        if(u.gold<kind.gold)return "部队携金不足，需要"+kind.gold+"金";
        if(target==null||!w.inside(target)||u.hex.distance(target)!=1)return "只能在部队相邻格设置";
        if(w.unitAt(target)!=null||w.cityAt(target)!=null||w.domestic.at(target)!=null||w.war.at(target)!=null||w.war.fireAt(target)!=null)return "目标地块已被占用或燃烧";
        if(kind==War.StructureKind.FIRE_SHIP?!w.army.water(target):w.army.water(target)||w.terrain[target.q][target.r]==World.Terrain.MOUNTAIN||w.terrain[target.q][target.r]==World.Terrain.MOUNTAIN_PATH||w.terrain[target.q][target.r]==World.Terrain.PLANK_ROAD||w.terrain[target.q][target.r]==World.Terrain.POISON||w.events.at(target)!=null)return "该地形不能设置此设施";
        for(World.City c:w.cities)if(c.hex.distance(target)<=2)return "据点两格以内不能设置";
        if(military(kind))for(War.Structure s:w.war.structures)if(military(s.kind)&&s.hex.distance(target)<=2)return "军事设施两格以内不能重复设置";
        if(w.war.structures.size()>=1000||w.war.nextStructureId>=10000000)return "军事设施达到上限";
        return null;
    }
    public List<Hex> sites(int unit,War.StructureKind kind){List<Hex> out=new ArrayList<>();World.Unit u=w.unit(unit);if(u!=null)for(Hex h:u.hex.neighbors())if(buildError(unit,kind,h,0)==null)out.add(h);return out;}
    public World.Result build(int unit,War.StructureKind kind,Hex target,int direction){
        String error=buildError(unit,kind,target,direction);if(error!=null)return w.fail(error);
        World.Unit u=w.unit(unit);u.gold-=kind.gold;u.acted=true;
        War.Structure s=new War.Structure(w.war.nextStructureId++,u.owner,kind,target,0);s.complete=false;s.builder=unit;s.direction=direction;w.war.structures.add(s);advance(s,u);
        return w.success("设置"+kind.label+" · "+(s.complete?"已建成":"施工"+s.hp+"/"+kind.hp+"，后续自动补修"));
    }
    public World.Result repair(int unit,int structure){
        World.Unit u=w.unit(unit);String error=w.orders.error(u);if(error!=null)return w.fail(error);War.Structure s=byId(structure);
        if(s==null||s.owner!=u.owner||u.hex.distance(s.hex)!=1||s.hp>=s.kind.hp||s.builder>=0&&s.builder!=unit||project(unit)!=null&&project(unit)!=s)return w.fail("请选择邻接、受损且无其他部队施工的己方设施");
        u.acted=true;s.builder=unit;advance(s,u);return w.success("补修"+s.kind.label+" · 耐久"+s.hp+"/"+s.kind.hp);
    }
    public World.Result stop(int unit){
        World.Unit u=w.unit(unit);War.Structure s=project(unit);
        if(w.commandsBlocked()||w.gameOver()||u==null||u.owner!=w.active||s==null)return w.fail("请选择己方施工部队");
        s.builder=-1;return w.success("已中止施工，保留当前设施与耐久，费用不退还");
    }
    public World.Result withdraw(int unit,int city,int gold){
        World.Unit u=w.unit(unit);World.City c=w.city(city);String error=w.orders.error(u);if(error!=null)return w.fail(error);
        if(c==null||c.owner!=u.owner||u.hex.distance(c.hex)!=1||gold<=0||gold>10000||u.gold>10000-gold||c.gold<gold)return w.fail("需要相邻己方据点、足够金及部队携金容量（10000）");
        c.gold-=gold;u.gold+=gold;u.acted=true;return w.success("部队补充"+gold+"金");
    }
    public War.Structure byId(int id){for(War.Structure s:w.war.structures)if(s.id==id)return s;return null;}
    private void advance(War.Structure s,World.Unit u){
        s.hp=Math.min(s.kind.hp,s.hp+constructionRate(u));
        if(s.hp==s.kind.hp){s.complete=true;s.builder=-1;w.campaign.earn(u.owner,30);w.note(s.kind.label+"完成补修");}
    }
    void cleanup(){for(War.Structure s:w.war.structures)if(s.builder>=0){World.Unit u=w.unit(s.builder);if(u==null||u.owner!=s.owner||u.hex.distance(s.hex)!=1)s.builder=-1;}}
    void continueOwner(int owner){cleanup();for(War.Structure s:new ArrayList<>(w.war.structures))if(s.owner==owner&&s.builder>=0){World.Unit u=w.unit(s.builder);if(u.status==War.Status.NORMAL&&!u.acted){u.acted=true;advance(s,u);}}}
    public int landCost(Hex h,World.Weapon weapon,int owner){
        if(h==null||!w.inside(h)||w.events.at(h)!=null)return -1;World.Terrain t=w.terrain[h.q][h.r];
        if((t==World.Terrain.MOUNTAIN_PATH||t==World.Terrain.SHALLOWS)&&!w.campaign.has(owner,Campaign.Tech.DIFFICULT_MARCH))return -1;
        return t==World.Terrain.MOUNTAIN_PATH||t==World.Terrain.PLANK_ROAD?3:t==World.Terrain.SHALLOWS?2:w.cost(h,weapon);
    }
    void traveled(World.Unit u,List<Hex> path){
        if(!w.skills.has(u,Skill.JIEDU))for(int i=1;i<path.size();i++)if(w.terrain[path.get(i).q][path.get(i).r]==World.Terrain.POISON){int loss=Math.max(1,u.troops/20);u.troops=Math.max(1,u.troops-loss);w.note("经过毒泉，部队损失"+loss+"兵");}
        if(!w.campaign.has(u.owner,Campaign.Tech.DIFFICULT_MARCH))for(int i=1;i<path.size();i++)if(w.terrain[path.get(i).q][path.get(i).r]==World.Terrain.PLANK_ROAD&&!w.skills.has(u,Skill.TAPO))u.troops=Math.max(1,u.troops-Math.max(1,u.troops/100));
        for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.STONE_MAZE&&w.campaign.hostile(s.owner,u.owner)&&s.hex.distance(u.hex)==1&&!w.skills.has(u,Skill.TAPO)&&!w.skills.has(u,Skill.DONGCHA)&&w.strategy.nextInt(100)<35){u.status=War.Status.CONFUSED;u.statusTurns=1;u.acted=true;break;}
    }
    public int defensePercent(World.Unit u){int best=0;for(War.Structure s:w.war.structures)if(s.complete&&s.owner==u.owner&&camp(s.kind)){
        int tier=s.kind==War.StructureKind.FORTRESS?3:s.kind==War.StructureKind.FORT?2:1;
        if(s.hex.distance(u.hex)<=tier+1)best=Math.max(best,tier==3?35:tier==2?25:15);
    }return best;}
    public int foodUse(World.Unit u,int base){int reduction=0;for(War.Structure s:w.war.structures)if(s.complete&&s.owner==u.owner&&camp(s.kind)){
        int tier=s.kind==War.StructureKind.FORTRESS?3:s.kind==War.StructureKind.FORT?2:1;
        if(s.hex.distance(u.hex)<=tier+1)reduction=Math.max(reduction,tier==3?50:tier==2?30:10);
    }return Math.max(1,base*(100-reduction)/100);}
    public boolean drum(World.Unit u){for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.DRUM&&s.owner==u.owner&&s.hex.distance(u.hex)<=2)return true;return false;}
    void counter(War.Structure s,World.Unit u){if(s.complete&&camp(s.kind)&&u.hex.distance(s.hex)==1&&w.army.counter(u)){
        int damage=w.campaign.has(s.owner,Campaign.Tech.DEFENSE_REINFORCEMENT)?400:200;u.troops=Math.max(0,u.troops-damage);if(u.troops==0)w.removeUnit(u);
    }}
    void towers(){for(War.Structure s:new ArrayList<>(w.war.structures))if(s.complete){
        int min=1,max=s.kind==War.StructureKind.ARROW_TOWER?2:s.kind==War.StructureKind.CROSSBOW_TOWER?3:s.kind==War.StructureKind.CATAPULT_TOWER?3:0;
        if(s.kind==War.StructureKind.CATAPULT_TOWER)min=2;if(max==0)continue;
        World.Unit target=null;for(World.Unit u:w.units){int d=s.hex.distance(u.hex);if(d>=min&&d<=max&&w.campaign.hostile(s.owner,u.owner)&&landTarget(s.owner,u.hex)&&(target==null||u.id<target.id))target=u;}
        if(target!=null){int damage=s.kind==War.StructureKind.CATAPULT_TOWER?300:200;target.troops=Math.max(0,target.troops-damage);if(target.troops==0)w.removeUnit(target);w.note(s.kind.label+"射击敌军，损失"+damage+"兵");}
    }}
    boolean landTarget(int owner,Hex h){return w.army.water(h)||landCost(h,World.Weapon.SPEAR,owner)>0;}
    void stoneSplash(World.Unit source,Hex center){
        if(!w.campaign.has(source.owner,Campaign.Tech.THUNDERBOLT))return;
        for(Hex h:center.neighbors()){
            World.Unit target=w.unitAt(h);if(target!=null&&(target.owner==source.owner||w.campaign.hostile(source.owner,target.owner))){
                int hit=w.war.physicalDamage(source,target,.75,true,new Random(w.strategy.nextInt(Integer.MAX_VALUE)));target.troops-=hit;if(target.troops<=0)w.defeatUnit(target,source);
            }
            War.Structure s=w.war.at(h);if(s!=null&&(s.owner==source.owner||w.campaign.hostile(source.owner,s.owner))){s.hp-=w.campaign.constructionDamage(source,300);if(s.hp<=0)w.war.structures.remove(s);}
        }
    }
    /** Queue contains only ignitions: trap removal precedes propagation, bounding chains by map size. */
    void ignite(Hex target,World.Unit source){
        ArrayDeque<Hex> queue=new ArrayDeque<>();Set<Hex> visited=new HashSet<>();queue.add(target);
        while(!queue.isEmpty()){
            Hex h=queue.remove();if(!w.inside(h)||!visited.add(h))continue;
            War.Structure s=w.war.at(h);
            if(s!=null&&s.owner!=source.owner&&!w.campaign.hostile(source.owner,s.owner))continue;
            if(s!=null&&s.complete&&trap(s.kind)){
                w.war.structures.remove(s);List<Hex> affected=new ArrayList<>();affected.add(h);
                if(ball(s.kind)){
                    Hex delta=new Hex(0,0).neighbors().get(s.direction);int range=s.kind==War.StructureKind.FIRE_BALL?3:5;
                    for(int n=1;n<=range;n++){Hex next=new Hex(h.q+delta.q*n,h.r+delta.r*n);if(!w.inside(next)||w.terrain[next.q][next.r]==World.Terrain.MOUNTAIN||w.army.water(next))break;affected.add(next);if(w.war.at(next)!=null&&s.kind!=War.StructureKind.INFERNO_BALL)break;}
                }else{
                    int radius=s.kind==War.StructureKind.FIRE_SEED?1:2;
                    for(int q=Math.max(0,h.q-radius);q<=Math.min(w.width-1,h.q+radius);q++)for(int r=Math.max(0,h.r-radius);r<=Math.min(w.height-1,h.r+radius);r++){Hex next=new Hex(q,r);if(!next.equals(h)&&h.distance(next)<=radius)affected.add(next);}
                }
                int base=s.kind==War.StructureKind.INFERNO_SEED||s.kind==War.StructureKind.INFERNO_BALL?1500:s.kind==War.StructureKind.FIRE_SEED||s.kind==War.StructureKind.FIRE_BALL?700:1000;
                for(Hex next:affected){
                    if(s.kind==War.StructureKind.FIRE_SHIP?!w.army.water(next):w.army.water(next))continue;
                    World.Unit victim=w.unitAt(next);if(victim!=null&&(victim.owner==source.owner||w.campaign.hostile(source.owner,victim.owner))){
                        victim.troops-=w.skills.fireDamage(victim,base,source.owner,w.skills.has(source,Skill.HUOSHEN)?2:1,true);
                        if(victim.troops==0)w.defeatUnit(victim,source);else if(s.kind==War.StructureKind.INFERNO_SEED&&!w.skills.has(victim,Skill.HUOSHEN)){victim.status=War.Status.CONFUSED;victim.statusTurns=2;}
                    }
                    War.Structure adjacent=w.war.at(next);if(adjacent!=null&&adjacent.complete&&trap(adjacent.kind))queue.add(next);else if(adjacent!=null&&(adjacent.owner==source.owner||w.campaign.hostile(source.owner,adjacent.owner))){adjacent.hp-=base/2;if(adjacent.hp<=0)w.war.structures.remove(adjacent);}
                    flame(next,source,true);
                }
                w.note(s.kind.label+"引爆");
            }else flame(h,source,false);
        }
    }
    private void flame(Hex h,World.Unit source,boolean trap){
        if(w.cost(h,World.Weapon.SPEAR)<0||w.cityAt(h)!=null||w.domestic.at(h)!=null)return;
        War.Structure s=w.war.at(h);if(s!=null&&s.owner!=source.owner&&!w.campaign.hostile(source.owner,s.owner))return;
        War.Fire old=w.war.fireAt(h);if(old!=null)w.war.fires.remove(old);
        War.Fire f=new War.Fire(h,source.owner,2);f.power=w.skills.has(source,Skill.HUOSHEN)?2:1;f.trap=trap;w.war.fires.add(f);
    }
}
