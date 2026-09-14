package game.sanguo.core;

import java.util.*;

/** Formation, equipment and amphibious campaign rules. Balance is an explicit engineering approximation. */
public final class Army {
    public enum Tactic {
        FIRE_ARROW("火矢",10,1,"伤害并燃烧2旬"), RAM("破碎 / 撞击",15,1,"重创城防；水上撞击并推退敌舰"),
        FLAME("放射",20,1,"木兽火焰攻击"), STONE("投石",20,1,"远距离重创目标");
        public final String label,effect;public final int energy,rank;
        Tactic(String label,int energy,int rank,String effect){this.label=label;this.energy=energy;this.rank=rank;this.effect=effect;}
    }
    public enum Ship {
        BOAT("走舸",4,1,70,0), TOWER_SHIP("楼船",5,2,105,1800), WARSHIP("斗舰",6,3,120,2000);
        public final String label; public final int movement,range,power,gold;
        Ship(String label,int movement,int range,int power,int gold){this.label=label;this.movement=movement;this.range=range;this.power=power;this.gold=gold;}
    }
    public static final class Production {
        public final int cityId,officerId,owner; public final World.Weapon weapon; public final Ship ship;
        Production(int city,int officer,int owner,World.Weapon weapon,Ship ship){cityId=city;officerId=officer;this.owner=owner;this.weapon=weapon;this.ship=ship;}
        public String label(){return "制造"+(weapon==null?ship.label:weapon.label);}
    }
    final World w;
    final List<Production> productions=new ArrayList<>();
    Army(World w){this.w=w;}
    public List<Production> productions(){return Collections.unmodifiableList(productions);}
    public static boolean siegeWeapon(World.Weapon weapon){return weapon!=null&&weapon.ordinal()>=World.Weapon.RAM.ordinal();}
    public static int equipmentNeeded(World.Weapon weapon,int troops){return weapon==World.Weapon.SWORD?0:siegeWeapon(weapon)?1:troops;}
    /** Listed PC manual base costs; specialty/difficulty modifiers are not yet implemented. */
    public static int productionGold(World.Weapon weapon){
        if(weapon==null||weapon==World.Weapon.SWORD)return 0;
        switch(weapon){case RAM:return 1500;case SIEGE_TOWER:return 1600;case WOODEN_BEAST:return 1700;case CATAPULT:return 1800;default:return 700;}
    }
    public static int category(World.Weapon weapon){return siegeWeapon(weapon)?4:weapon==World.Weapon.SWORD?-1:weapon.ordinal();}
    public List<World.Officer> crew(World.Unit unit){
        List<World.Officer> members=new ArrayList<>();members.add(w.officer(unit.officerId));
        for(int id:unit.deputies)members.add(w.officer(id));return members;
    }
    public boolean contains(World.Unit unit,int officer){if(unit.officerId==officer)return true;if(unit.deputies!=null)for(int id:unit.deputies)if(id==officer)return true;return false;}
    public int war(World.Unit unit){int leader=w.contests.war(w.officer(unit.officerId)),value=leader;for(int id:unit.deputies)value=Math.max(value,w.relations.contribution(unit.officerId,id,leader,w.contests.war(w.officer(id))));return value;}
    public int leadership(World.Unit unit){int leader=w.officer(unit.officerId).leadership,value=leader;for(int id:unit.deputies)value=Math.max(value,w.relations.contribution(unit.officerId,id,leader,w.officer(id).leadership));return value;}
    public int intelligence(World.Unit unit){int value=0;for(World.Officer o:crew(unit))value=Math.max(value,o.intelligence);return value;}
    public int aptitude(World.Unit unit){return aptitude(crew(unit),water(unit.hex)?5:category(unit.weapon));}
    public int aptitude(List<World.Officer> crew,int category){int value=0;if(category>=0)for(World.Officer o:crew)value=Math.max(value,o.aptitude[category]);return value;}
    World.Officer combatOfficer(World.Unit u){World.Officer leader=w.officer(u.officerId);return new World.Officer(leader.id,leader.name,leader.owner,-1,leadership(u),war(u),intelligence(u),leader.politics,leader.charm);}
    public boolean water(Hex h){return h!=null&&w.inside(h)&&w.terrain[h.q][h.r]==World.Terrain.WATER;}
    public String equipmentLabel(World.Unit u){return water(u.hex)?u.ship.label+"（携"+u.weapon.label+"）":u.weapon.label;}
    public int movement(World.Unit u){return water(u.hex)?u.ship.movement:u.weapon.movement;}
    public int range(World.Unit u){return water(u.hex)?u.ship.range:u.weapon==World.Weapon.CAVALRY&&(w.skills.has(u,Skill.BAIMA)||w.campaign.has(u.owner,Campaign.Tech.MOUNTED_ARCHERY))?2:u.weapon.range;}
    public int moveCost(World.Unit u,Hex from,Hex to){
        if(to==null||!w.inside(to)||w.terrain[to.q][to.r]==World.Terrain.MOUNTAIN)return -1;
        if(water(to))return water(from)?1:2; // Embark consumes movement, not another inventory item.
        int land=w.fieldworks.landCost(to,u.weapon,u.owner);
        if(land<0)return -1; // Landing must obey the same terrain prerequisites as land movement.
        return water(from)?Math.max(2,land):land;
    }
    public World.Result deploy(int city,int commander,int[] deputies,World.Weapon weapon,Ship ship,int troops,int food){return deploy(city,commander,deputies,weapon,ship,troops,food,0);}
    public World.Result deploy(int city,int commander,int[] deputies,World.Weapon weapon,Ship ship,int troops,int food,int gold){
        World.City c=w.city(city);World.Officer leader=w.officer(commander);String error=w.cityError(c,leader,0);if(error!=null)return w.fail(error);
        if(gold<0||gold>10000||c.gold<gold)return w.fail("携金须为0至10000，且据点有足够金");
        if(weapon==null||ship==null||deputies==null||deputies.length>2)return w.fail("请选择主将、至多两名副将及有效兵装舰船");
        if(troops<1000||troops>w.government.commandLimit(commander)||food<troops||food>1000000)return w.fail("兵力1000至"+w.government.commandLimit(commander)+"，携粮至少与兵力相同且不超过100万");
        List<World.Officer> idle=w.idle(c),members=new ArrayList<>();members.add(leader);Set<Integer> ids=new HashSet<>();ids.add(commander);
        for(int id:deputies){World.Officer o=w.officer(id);if(!ids.add(id)||o==null||!idle.contains(o))return w.fail("副将不能重复，须为同城未行动的闲将");members.add(o);}
        int equipment=equipmentNeeded(weapon,troops);
        if(c.troops<troops||c.food<food||c.equipment[weapon.ordinal()]<equipment||ship!=Ship.BOAT&&c.ships[ship.ordinal()-1]<1)return w.fail("兵力、携粮、兵装或舰船库存不足");
        if(w.nextUnitId>=10000000)return w.fail("部队编号达到上限");
        Hex exit=null;for(Hex h:c.hex.neighbors())if(w.fieldworks.landCost(h,weapon,c.owner)>0&&w.unitAt(h)==null&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.war.fireAt(h)==null){exit=h;break;}
        if(exit==null)return w.fail("城外没有可用出征格");
        w.spend(c,leader,0);c.troops-=troops;c.food-=food;c.equipment[weapon.ordinal()]-=equipment;if(ship!=Ship.BOAT)c.ships[ship.ordinal()-1]--;
        World.Unit u=new World.Unit(w.nextUnitId++,w.active,commander,weapon,exit,troops,food);u.gold=gold;c.gold-=gold;u.ship=ship;u.deputies=deputies.clone();u.energy=c.morale;w.units.add(u);
        for(World.Officer o:members){w.strategy.releaseGovernor(o.id);o.acted=true;o.cityId=-1;o.unitId=u.id;}
        w.districts.deployed(city,u);
        return w.success(leader.name+"率"+troops+weapon.label+"出征 · 编队"+members.size()+"将 · 携"+ship.label);
    }
    private boolean completed(int city,Domestic.Kind kind){return w.domestic.facilities.stream().anyMatch(f->f.cityId==city&&f.kind==kind&&f.remaining==0);}
    public String productionError(int city,int officer,World.Weapon weapon,Ship ship){
        if((weapon==null)==(ship==null)||weapon!=null&&!siegeWeapon(weapon)||ship==Ship.BOAT)return "请选择攻城器械或高级舰船";
        World.City c=w.city(city);World.Officer o=w.officer(officer);String error=w.cityError(c,o,weapon!=null?productionGold(weapon):ship.gold);if(error!=null)return error;
        Domestic.Kind facility=weapon!=null?Domestic.Kind.WORKSHOP:Domestic.Kind.SHIPYARD;
        if(!completed(city,facility))return "需要已建成的"+facility.label;
        Campaign.Tech tech=weapon==World.Weapon.WOODEN_BEAST?Campaign.Tech.WOODEN_BEAST:weapon==World.Weapon.CATAPULT?Campaign.Tech.CATAPULT:ship==Ship.WARSHIP?Campaign.Tech.WARSHIP:null;
        if(tech!=null&&!w.campaign.has(c.owner,tech))return "需要先研究"+(tech==Campaign.Tech.WARSHIP?Campaign.Tech.CATAPULT:tech).label;
        int amount=weapon!=null?c.equipment[weapon.ordinal()]:c.ships[ship.ordinal()-1];
        long pending=productions.stream().filter(p->p.cityId==city&&p.weapon==weapon&&p.ship==ship).count();
        if(amount+pending>=100)return "该类器械或舰船库存与在制品合计已达100";return null;
    }
    public World.Result produce(int city,int officer,World.Weapon weapon,Ship ship){
        String error=productionError(city,officer,weapon,ship);if(error!=null)return w.fail(error);
        World.City c=w.city(city);World.Officer o=w.officer(officer);Production p=new Production(city,officer,c.owner,weapon,ship);
        w.spend(c,o,weapon!=null?productionGold(weapon):ship.gold);o.otherTask=p.label();o.otherTaskTurns=w.skills.productionTurns(officer,weapon);productions.add(p);
        return w.success(o.name+"开始"+p.label()+"，"+o.otherTaskTurns+"旬后完成1件");
    }
    private boolean valid(Production p){World.City c=w.city(p.cityId);World.Officer o=w.officer(p.officerId);return c!=null&&o!=null&&c.owner==p.owner&&o.owner==p.owner&&o.cityId==c.id&&o.otherTask.equals(p.label())&&completed(c.id,p.weapon!=null?Domestic.Kind.WORKSHOP:Domestic.Kind.SHIPYARD);}
    void cleanup(){for(Production p:new ArrayList<>(productions))if(!valid(p)){productions.remove(p);World.Officer o=w.officer(p.officerId);if(o!=null&&o.otherTask.equals(p.label())){o.otherTask="";o.otherTaskTurns=0;}w.note(p.label()+"因城池或工场失守/拆除而中止");}}
    void tick(){
        for(World.Unit u:new ArrayList<>(w.units))if(u.burning>0){
            u.burning--;if(u.burningOwner==u.owner||w.campaign.hostile(u.burningOwner,u.owner)){int hit=w.skills.fireDamage(u,200,u.burningOwner,u.burningPower,false);u.troops-=hit;w.note(w.officer(u.officerId).name+"的部队持续燃烧，损失"+hit+"兵");}
            if(u.burning==0){u.burningOwner=-1;u.burningPower=1;}if(u.troops==0)w.removeUnit(u);
        }
        cleanup();for(Production p:new ArrayList<>(productions))if(w.officer(p.officerId).otherTaskTurns==1){
            World.City c=w.city(p.cityId);int count=p.weapon!=null?c.equipment[p.weapon.ordinal()]:c.ships[p.ship.ordinal()-1];
            if(count>=100){w.officer(p.officerId).otherTaskTurns=2;continue;}
            if(p.weapon!=null)c.equipment[p.weapon.ordinal()]++;else c.ships[p.ship.ordinal()-1]++;
            productions.remove(p);w.note(c.name+p.label()+"完成，1件入库");
        }
    }
    public World.Result cancelProduction(int officer){Production p=productions.stream().filter(x->x.officerId==officer).findFirst().orElse(null);
        if(w.commandsBlocked()||w.gameOver()||p==null||p.owner!=w.active)return w.fail("请选择本势力制造任务");productions.remove(p);World.Officer o=w.officer(officer);o.otherTask="";o.otherTaskTurns=0;o.acted=true;return w.success("制造中止，费用不退还");}
    /** Current modeled unit attack; base attribute formula still awaits original executable calibration. */
    public double attackPower(World.Unit u){return (80+leadership(u)+war(u)/2.0)*(water(u.hex)?u.ship.power:u.weapon.power)/100.0;}
    int damage(World.Unit a,World.Unit b,double scale,Random random){
        double ap=water(a.hex)?a.ship.power:a.weapon.power,bp=water(b.hex)?b.ship.power:b.weapon.power;
        double offense=80+leadership(a)+war(a)/2.0;
        double defense=80+leadership(b)+intelligence(b)/4.0;
        int amount=(int)(250*StrictMath.sqrt(a.troops/1000.0)*ap/100*Math.max(.4,offense/defense)*scale*(.9+random.nextDouble()*.2));
        if(!water(b.hex)&&siegeWeapon(b.weapon))amount=amount*3/2;
        if(water(b.hex)&&bp>100)amount=amount*9/10;
        return Math.max(20,Math.min(2500,amount));
    }
    public int siegeRange(World.Unit u){return w.war.range(u);}
    public int siegeDefenseDamage(World.Unit u){
        if(water(u.hex))return u.ship==Ship.WARSHIP?350:100;
        switch(u.weapon){case RAM:return 650;case WOODEN_BEAST:return 750;case CATAPULT:return 600;case SIEGE_TOWER:return 120;default:return 100;}
    }
    public int siegeTroopDamage(World.Unit u){
        if(water(u.hex))return u.ship==Ship.WARSHIP?450:200;
        switch(u.weapon){case RAM:return 60;case WOODEN_BEAST:return 500;case CATAPULT:return 400;case SIEGE_TOWER:return 800;default:return 100;}
    }
    public boolean canAttackUnit(World.Unit u){return water(u.hex)||!siegeWeapon(u.weapon);}
    public boolean counter(World.Unit u){return !water(u.hex)&&!siegeWeapon(u.weapon)&&u.weapon!=World.Weapon.CROSSBOW;}
    public List<Tactic> tactics(World.Unit u){
        List<Tactic> result=new ArrayList<>();
        if(water(u.hex)){if(u.ship!=Ship.BOAT){result.add(Tactic.FIRE_ARROW);result.add(Tactic.RAM);}if(u.ship==Ship.WARSHIP)result.add(Tactic.STONE);}
        else switch(u.weapon){case RAM:result.add(Tactic.RAM);break;case SIEGE_TOWER:result.add(Tactic.FIRE_ARROW);break;case WOODEN_BEAST:result.add(Tactic.FLAME);break;case CATAPULT:result.add(Tactic.STONE);break;default:break;}
        return result;
    }
    public String tacticError(int unit,Hex target,Tactic tactic){
        World.Unit u=w.unit(unit);if(w.commandsBlocked()||w.gameOver()||u==null||u.owner!=w.active||u.acted||u.status!=War.Status.NORMAL)return "需要当前势力可行动部队";
        if(tactic==null||!tactics(u).contains(tactic))return "当前兵装或舰船不能施展此战法";
        if(water(u.hex)&&aptitude(u)<tactic.rank||u.energy<tactic.energy)return "适性或气力不足";
        int distance=target==null?0:u.hex.distance(target),max=tactic==Tactic.RAM?1:w.war.range(u);
        if(target==null||!w.inside(target)||distance<1||distance>max||!w.fieldworks.landTarget(u.owner,target))return "目标不在战法范围内";
        World.Unit enemy=w.unitAt(target);World.City city=w.cityAt(target);War.Structure structure=w.war.at(target);
        if(enemy==null&&city==null&&structure==null||enemy!=null&&!w.campaign.hostile(u.owner,enemy.owner)||city!=null&&!w.campaign.hostile(u.owner,city.owner)||structure!=null&&!w.campaign.hostile(u.owner,structure.owner))return "请选择交战部队、城池或军事设施";
        if(enemy!=null&&!water(u.hex)&&u.weapon==World.Weapon.RAM)return "冲车只能攻击城池";
        if(enemy!=null&&tactic==Tactic.FIRE_ARROW&&!water(u.hex)&&w.terrain[target.q][target.r]==World.Terrain.FOREST&&!w.skills.has(u,Skill.SHESHOU))return "射向森林需要射手特技";
        if(tactic==Tactic.RAM&&water(u.hex)&&!water(target))return "撞击只能针对水上敌舰";
        return null;
    }
    public int tacticChance(int unit){World.Unit u=w.unit(unit);return u==null?0:!water(u.hex)&&siegeWeapon(u.weapon)?100:Math.min(95,75+aptitude(u)*5);}
    public int tacticChance(int unit,Hex target){World.Unit enemy=w.unitAt(target);return enemy!=null&&enemy.status!=War.Status.NORMAL?100:tacticChance(unit);}
    public World.Result tactic(int unit,Hex target,Tactic tactic){
        String error=tacticError(unit,target,tactic);if(error!=null)return w.fail(error);
        World.Unit u=w.unit(unit),enemy=w.unitAt(target);World.City city=w.cityAt(target);
        u.acted=true;u.energy-=tactic.energy;
        if(tacticChance(unit,target)<100&&w.strategy.nextInt(100)>=tacticChance(unit,target))return w.success(tactic.label+"未命中，气力已消耗");
        if(city!=null){World.Result result=w.resolveSiege(u,city,true);if(tactic==Tactic.STONE)w.fieldworks.stoneSplash(u,target);w.checkVictory();return result;}
        War.Structure structure=w.war.at(target);
        if(structure!=null){
            if(structure.complete&&w.fieldworks.trap(structure.kind)&&(tactic==Tactic.FIRE_ARROW||tactic==Tactic.FLAME)){w.war.ignite(target,u);w.checkVictory();return w.success(tactic.label+"引爆"+structure.kind.label);}
            int amount=w.campaign.constructionDamage(u,w.army.siegeDefenseDamage(u));if(w.skills.critical(u,null,true))amount=amount*115/100;
            amount=Math.min(structure.hp,amount);structure.hp-=amount;if(structure.hp==0)w.war.structures.remove(structure);
            if(tactic==Tactic.FIRE_ARROW||tactic==Tactic.FLAME)w.war.ignite(target,u);if(tactic==Tactic.STONE)w.fieldworks.stoneSplash(u,target);
            w.campaign.earn(u.owner,20);return w.success(tactic.label+"命中"+structure.kind.label+"，耐久减少"+amount);
        }
        int amount=w.war.physicalDamage(u,enemy,tactic==Tactic.STONE?1.5:1.3,true,new Random(w.strategy.nextInt(Integer.MAX_VALUE)));
        enemy.troops-=amount;
        if(enemy.troops==0)w.defeatUnit(enemy,u);
        else if((tactic==Tactic.FIRE_ARROW||tactic==Tactic.FLAME)&&!w.skills.has(enemy,Skill.HUOSHEN)){enemy.burning=2;enemy.burningOwner=u.owner;enemy.burningPower=w.skills.has(u,Skill.HUOSHEN)?2:1;}
        else if(tactic==Tactic.RAM&&water(u.hex)){
            Hex next=new Hex(target.q+target.q-u.hex.q,target.r+target.r-u.hex.r);
            if(water(next)&&w.unitAt(next)==null&&w.cityAt(next)==null&&w.domestic.at(next)==null&&w.war.at(next)==null)enemy.hex=next;
        }
        w.skills.onHit(u,enemy,amount,true);if(tactic==Tactic.STONE)w.fieldworks.stoneSplash(u,target);
        w.campaign.earn(u.owner,enemy.troops==0&&w.skills.has(u,Skill.JINGMIAO)?80:40);w.checkVictory();return w.success(tactic.label+"命中，敌损"+amount);
    }
    public World.Result extinguish(int unit){World.Unit u=w.unit(unit);
        if(w.commandsBlocked()||w.gameOver()||u==null||u.owner!=w.active||u.acted||u.status!=War.Status.NORMAL||u.burning==0||u.energy<5)return w.fail("需要可行动且正在燃烧的己方部队，消耗5气力");
        u.burning=0;u.burningOwner=-1;u.burningPower=1;u.energy-=5;u.acted=true;return w.success("部队已扑灭火焰");}
}
