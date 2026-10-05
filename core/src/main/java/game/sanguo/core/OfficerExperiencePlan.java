package game.sanguo.core;

/** Read-only before/after for one source-verified fixed experience award.
 * Unmanaged saves keep their historical numeric mode and receive no invented XP. */
public final class OfficerExperiencePlan {
    public final boolean managed;
    public final int stat,requestedAmount,experienceBefore,experienceAfter,currentBefore,currentAfter;
    OfficerExperiencePlan(World w,World.Officer officer,int stat,int amount){
        this.stat=stat;requestedAmount=stat<0?0:amount;
        managed=officer.abilityProfile!=null;
        experienceBefore=stat<0||!managed?-1:w.officerAbilities.experience(officer.id,stat);
        experienceAfter=stat<0||!managed?-1:w.officerAbilities.experienceAfter(officer.id,stat,amount);
        currentBefore=stat<0?-1:OfficerAbilities.raw(officer,stat);
        currentAfter=stat<0?-1:w.officerAbilities.afterExperience(officer.id,stat,amount);
    }
    static int cityStat(CityActionPlan.Operation operation){
        return switch(operation){case PATROL->0;case TRAIN->1;case RECRUIT->4;default->-1;};
    }
    /** Original immediate production5c66aa awards intelligence2. Delayed
     * siege/ship production5c70 records a task; its ongoing rewards are separate. */
    static int productionStat(World.Weapon weapon,Army.Ship ship){
        return ship==null&&weapon!=null&&!Army.siegeWeapon(weapon)&&weapon!=World.Weapon.SWORD?2:-1;
    }
    static int merit(World.Officer officer,int stat,Government government){
        return stat>=0&&officer.abilityProfile!=null?PcMerchantRules.meritGain(government.merit(officer.id)):100;
    }
    static void award(World w,World.Officer officer,int stat){if(stat>=0)w.officerAbilities.gainExperience(officer.id,stat,2);}
}
