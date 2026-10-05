package game.sanguo.api;

/** Immutable fixed-reward forecast. stat=-1 means no verified award for this
 * operation; unmanaged saves use XP=-1 and retain their historical numbers. */
public final class OfficerExperienceChange {
    public final boolean managed;
    public final int stat,requestedAmount,experienceBefore,experienceAfter,currentBefore,currentAfter;
    public OfficerExperienceChange(boolean managed,int stat,int requestedAmount,int experienceBefore,int experienceAfter,int currentBefore,int currentAfter){
        this.managed=managed;this.stat=stat;this.requestedAmount=requestedAmount;
        this.experienceBefore=experienceBefore;this.experienceAfter=experienceAfter;
        this.currentBefore=currentBefore;this.currentAfter=currentAfter;
    }
}
