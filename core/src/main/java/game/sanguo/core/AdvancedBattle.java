package game.sanguo.core;

import java.util.*;

/** Campaign commands absent from the original engineering battle kernel. See WORLD_V0_14.md. */
public final class AdvancedBattle {
    private final World w;
    AdvancedBattle(World w){this.w=w;}

    public boolean ignoresZone(World.Unit u,Hex hex){
        if(w.army.water(hex))return w.skills.has(u,Skill.TUIJIN);
        return !Army.siegeWeapon(u.weapon)&&(w.skills.has(u,Skill.DUNZOU)||w.skills.has(u,Skill.FEIJIANG));
    }
    public boolean zone(World.Unit u,Hex hex){
        if(ignoresZone(u,hex))return false;
        for(World.Unit enemy:w.units)if(enemy.id!=u.id&&w.campaign.hostile(u.owner,enemy.owner)&&enemy.hex.distance(hex)==1)return true;
        for(War.Structure s:w.war.structures())if(s.complete&&w.campaign.hostile(u.owner,s.owner)&&!w.fieldworks.trap(s.kind)&&s.kind!=War.StructureKind.EARTH_WALL&&s.kind!=War.StructureKind.STONE_WALL&&s.hex.distance(hex)==1)return true;
        return false;
    }
    public List<World.Unit> jointParticipants(int actor,int target){
        World.Unit a=w.unit(actor),b=w.unit(target);List<World.Unit> out=new ArrayList<>();
        if(a==null||b==null)return out;
        for(World.Unit u:w.units)if(u.owner==a.owner&&w.orders.error(u)==null&&u.hex.distance(b.hex)==1&&w.army.counter(u)&&!w.army.water(u.hex))out.add(u);
        out.sort(Comparator.comparingInt(u->u.id==actor?-1:u.id));return Collections.unmodifiableList(out);
    }
    public String jointError(int actor,int target){
        World.Unit a=w.unit(actor),b=w.unit(target);String error=w.orders.error(a);if(error!=null)return error;
        if(b==null||!w.campaign.hostile(a.owner,b.owner)||w.army.water(b.hex)||!w.fieldworks.landTarget(a.owner,b.hex))return "请选择可交战的陆上部队";
        List<World.Unit> group=jointParticipants(actor,target);
        if(!group.contains(a)||group.size()<2)return "齐攻需要至少两支相邻、未行动的陆上近战部队";
        return null;
    }
    public World.Result joint(int actor,int target){
        String error=jointError(actor,target);if(error!=null)return w.fail(error);
        World.Unit a=w.unit(actor),b=w.unit(target);
        if(w.skills.has(b,Skill.TIEBI))return w.war.attack(actor,target);
        List<World.Unit> group=jointParticipants(actor,target);boolean flank=false;
        for(World.Unit u:group){u.acted=true;flank|=w.skills.has(u,Skill.JIJIAO);}
        int dealt=0,counter=0;int repeats=w.skills.has(a,Skill.LIANZHAN)&&w.strategy.nextInt(100)<50?2:1;
        for(int r=0;r<repeats&&w.unit(b.id)!=null;r++)for(World.Unit u:group)if(w.unit(b.id)!=null&&w.unit(u.id)!=null)dealt+=w.war.strike(u,b,.65,false);
        if(w.unit(b.id)!=null){
            if(flank&&w.strategy.nextInt(100)<50){b.status=War.Status.CONFUSED;b.statusTurns=1;}
            if(b.status==War.Status.NORMAL&&w.army.counter(b)&&!w.skills.avoidCounter(a,new Random(w.strategy.nextInt(Integer.MAX_VALUE))))counter=w.war.strike(b,a,.5,false);
        }
        w.campaign.earn(a.owner,30);w.checkVictory();
        return w.success(group.size()+"队齐攻：敌损"+dealt+"，主攻反击损失"+counter);
    }
    public boolean magic(War.Plot p){return p==War.Plot.SORCERY||p==War.Plot.LIGHTNING;}
    public boolean unlocked(World.Unit u,War.Plot p){return p==War.Plot.LIGHTNING?w.skills.has(u,Skill.GUIMEN):p!=War.Plot.SORCERY||w.skills.has(u,Skill.YAOSHU)||w.skills.has(u,Skill.GUIMEN);}
    List<World.Unit> infightingTargets(World.Unit target){
        List<World.Unit> out=new ArrayList<>();if(target==null||Army.siegeWeapon(target.weapon))return out;
        for(World.Unit u:w.units)if(u.id!=target.id&&u.owner==target.owner&&u.hex.distance(target.hex)==1&&!Army.siegeWeapon(u.weapon)&&w.army.water(u.hex)==w.army.water(target.hex))out.add(u);
        out.sort(Comparator.comparingInt(u->u.id));return out;
    }
    void infight(World.Unit source,World.Unit target,boolean critical){
        List<World.Unit> others=infightingTargets(target);if(others.isEmpty())return;
        World.Unit other=others.get(w.strategy.nextInt(others.size()));
        int first=w.war.physicalDamage(target,other,critical?1.15:1,false,new Random(w.strategy.nextInt(Integer.MAX_VALUE)));
        int second=w.war.physicalDamage(other,target,.5,false,new Random(w.strategy.nextInt(Integer.MAX_VALUE)));
        // Simultaneous losses; their same-faction troops cannot capture or award each other merit.
        injure(other,first,source);injure(target,second,source);
        w.note("同讨造成双方损失"+first+" / "+second);
    }
    public int magicChance(World.Unit a,World.Unit b,War.Plot p){
        if(b!=null&&w.skills.plotImmune(a,b,p))return 0;
        return Math.max(5,Math.min(75,50+(w.army.intelligence(a)-(b==null?50:w.army.intelligence(b)))/2));
    }
    boolean cast(World.Unit a,World.Unit primary,Hex center,War.Plot p,boolean reflection){
        int chance=magicChance(a,primary,p);
        if(chance==0||w.strategy.nextInt(100)>=chance){
            if(reflection&&primary!=null&&w.skills.has(primary,Skill.FANJI))cast(primary,a,a.hex,p,false);
            return false;
        }
        List<World.Unit> victims=new ArrayList<>(w.units);victims.sort(Comparator.comparingInt(u->u.id));
        int power=w.army.intelligence(a);boolean critical=w.skills.plotCritical(a,primary,p);
        for(World.Unit target:victims){
            if(target.hex.distance(center)>1||target.owner!=a.owner&&!w.campaign.hostile(a.owner,target.owner))continue;
            if(p==War.Plot.SORCERY){
                if(target.owner==a.owner||w.skills.plotImmune(a,target,p))continue;
                target.status=w.strategy.nextInt(2)==0?War.Status.CONFUSED:War.Status.MISLED;target.statusTurns=critical?2:1;
            }else if(!w.skills.plotImmune(a,target,p))injure(target,(500+power*12)*(critical?115:100)/100,a);
        }
        if(p==War.Plot.LIGHTNING){
            List<Hex> area=new ArrayList<>(center.neighbors());area.add(center);
            for(Hex h:area)if(w.inside(h)&&!w.army.water(h)&&w.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN){
                World.City city=w.cityAt(h);Domestic.Facility facility=w.domestic.at(h);War.Structure structure=w.war.at(h);
                if(city!=null){if(city.owner==a.owner||w.campaign.hostile(a.owner,city.owner)){city.troops=Math.max(0,city.troops-800);city.defense=Math.max(1,city.defense-500);}continue;}
                if(facility!=null){World.City home=w.city(facility.cityId);if(home.owner==a.owner||w.campaign.hostile(a.owner,home.owner)){if(facility.builderId>=0)w.officer(facility.builderId).acted=true;w.domestic.facilities.remove(facility);w.army.cleanup();}continue;}
                if(structure!=null&&structure.owner!=a.owner&&!w.campaign.hostile(a.owner,structure.owner))continue;
                if(w.war.fireAt(h)==null)w.war.fires.add(new War.Fire(h,a.owner,2));
            }
        }
        return true;
    }
    private void injure(World.Unit target,int amount,World.Unit source){
        if(w.unit(target.id)==null)return;w.battleImpact(target.hex,false);target.troops=Math.max(0,target.troops-amount);
        if(target.troops==0){if(source.owner!=target.owner&&w.unit(source.id)!=null)w.defeatUnit(target,source);else w.removeUnit(target);}
    }
}
