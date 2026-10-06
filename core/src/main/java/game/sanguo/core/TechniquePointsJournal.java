package game.sanguo.core;

import java.util.*;

/** Transient observations of actual point writes; never saved and never a rule input. */
public final class TechniquePointsJournal {
    public enum Phase { COMMAND, DELEGATED, AI, GLOBAL, PLAYER_RESUME }
    public enum Cause { RAW_WRITE, CAMPAIGN_REWARD, NATIVE_PRODUCTION, NATIVE_MANUFACTURE,
        RESEARCH_COST, RELATION_COST, EDITOR_SET, TRAINING, TREATY, GOODWILL, RUMOR, CITY_REPAIR,
        TERRITORY_TURN, FIELD_REPAIR, DEBATE, DUEL, CITY_CAPTURE, ATTACK, TACTIC, PLOT,
        FACILITY_ATTACK, FIELD_ATTACK, BANDIT_DEFEAT, ADVANCED_BATTLE, BRONZE_TERRACE, SEAL, DIRECT_RECRUITMENT }
    public static final class Fact {
        public final long sequence;
        public final int owner,before,after,delta,cityId,officerId;
        public final Cause cause;
        public final Phase phase;
        public final String presentationParentId;
        private Fact(long sequence,int owner,int before,int after,Cause cause,Phase phase,int city,int officer,String presentationParentId){
            this.sequence=sequence;this.owner=owner;this.before=before;this.after=after;delta=after-before;
            this.cause=cause;this.phase=phase;cityId=city;officerId=officer;
            this.presentationParentId=presentationParentId;
        }
    }
    private final List<Fact> facts=new ArrayList<>();
    private boolean capturing;
    private Phase phase=Phase.COMMAND;
    public void begin(){facts.clear();capturing=true;phase=Phase.COMMAND;}
    public void ensureCapture(){if(!capturing)begin();}
    public void phase(Phase value){phase=Objects.requireNonNull(value);}
    public int size(){return facts.size();}
    public List<Fact> facts(){return Collections.unmodifiableList(new ArrayList<>(facts));}
    public List<Fact> since(int cursor){return Collections.unmodifiableList(new ArrayList<>(facts.subList(cursor,facts.size())));}
    public void clear(){facts.clear();capturing=false;phase=Phase.COMMAND;}
    public void inherit(TechniquePointsJournal source){clear();facts.addAll(source.facts);}
    void record(int owner,int before,int after,Cause cause,int city,int officer,String presentationParentId){
        if(capturing&&before!=after)facts.add(new Fact(facts.size()+1L,owner,before,after,cause,phase,city,officer,presentationParentId));
    }
}
