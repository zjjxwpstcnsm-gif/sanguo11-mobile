package game.sanguo.core;

import java.util.Random;
import static game.sanguo.core.Skill.*;

/** Applies a paid/validated command's hit. Queries must never call this class. */
public final class CombatEffects {
    private final World w;
    CombatEffects(World w){this.w=w;}
    int physical(World.Unit a,World.Unit b,double scale,boolean tactic){
        int amount=w.combat.physicalDamage(a,b,scale,tactic,new Random(w.strategy.nextInt(Integer.MAX_VALUE)));
        return hit(a,b,amount,tactic,true);
    }
    int hit(World.Unit source,World.Unit target,int amount,boolean tactic,boolean triggerOnHit){
        if(w.unit(target.id)!=target)return 0;
        int actual=Math.max(0,Math.min(target.troops,amount));target.troops-=actual;
        if(triggerOnHit)onHit(source,target,actual,tactic);
        if(target.troops==0)w.defeatUnit(target,source);
        return actual;
    }
    void onHit(World.Unit source,World.Unit target,int loss,boolean tactic){
        if(loss<=0)return;
        if(!w.army.water(source.hex)&&source.weapon==World.Weapon.SPEAR&&w.campaign.has(source.owner,Campaign.Tech.SUPPLY_RAID)){
            int food=Math.min(Math.min(target.food,Math.max(1,loss)),1000000-source.food);target.food-=food;source.food+=food;
        }
        w.energy.change(target,-w.energy.hitDrain(source),w.skills.has(source,WEIFENG)?EnergyRules.Reason.WEIFENG:EnergyRules.Reason.SAOTAO);
        if(tactic&&w.skills.has(target,NUFA)&&target.troops>0)w.energy.change(target,5,EnergyRules.Reason.NUFA);
        if(w.skills.has(source,XINGONG)&&source.troops>0)source.troops=Math.max(source.troops,Math.min(w.government.commandLimit(source.officerId),source.troops+loss/10));
        if(target.troops==0&&w.skills.has(source,ANGYANG))w.energy.change(source,10,EnergyRules.Reason.ANGYANG);
    }
}
