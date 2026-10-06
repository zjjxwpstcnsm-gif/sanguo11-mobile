package game.sanguo.api;
import java.util.*;

/** Immutable authoritative contest display. No rule object or RNG crosses API. */
public final class ContestSnapshot {
    public enum Kind { NONE, DUEL, DEBATE, SEARCH_CHOICE }
    public static final class Speaker {
        public final int officerId,nativeId,health,maxHealth,anger,fury,intelligence,war;
        public final String name,personality,sourceVariant;
        public Speaker(int id,int nativeId,String name,String personality,String variant,int health,int max,int anger,int fury,int intelligence,int war){officerId=id;this.nativeId=nativeId;this.name=name;this.personality=personality;sourceVariant=variant;this.health=health;maxHealth=max;this.anger=anger;this.fury=fury;this.intelligence=intelligence;this.war=war;}
    }
    public static final class Card {
        public final int slot,nativeCard;public final String label,error;
        public Card(int slot,int nativeCard,String label,String error){this.slot=slot;this.nativeCard=nativeCard;this.label=label;this.error=error==null?"":error;}
        public boolean enabled(){return error.isEmpty();}
    }
    public static final class Event {
        public final int frame,nativeCallback,side,nativeCard,counter;
        public final boolean reflected;public final List<List<Integer>> choices;
        public Event(int frame,int callback,int side,int card,int counter,boolean reflected,List<List<Integer>> choices){this.frame=frame;nativeCallback=callback;this.side=side;nativeCard=card;this.counter=counter;this.reflected=reflected;this.choices=choices.stream().map(List::copyOf).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));}
    }
    public final StateToken state;public final Kind kind;
    public final int contestId,revision,phase,sub,round,leader,winner;
    public final boolean nativeRules,waitingCard,waitingMercy,settlementAvailable;
    public final String purpose,topic,sourceId,sourceVariant,sourceSha,status;
    public final List<Speaker> speakers;public final List<Card> cards;public final List<Event> events;
    public ContestSnapshot(StateToken state,Kind kind,int id,int revision,int phase,int sub,int round,int leader,int winner,
                           boolean nativeRules,boolean waitingCard,boolean waitingMercy,boolean settlement,
                           String purpose,String topic,String source,String variant,String sha,String status,List<Speaker> speakers,List<Card> cards,List<Event> events){
        this.state=Objects.requireNonNull(state);this.kind=Objects.requireNonNull(kind);contestId=id;this.revision=revision;this.phase=phase;this.sub=sub;this.round=round;this.leader=leader;this.winner=winner;this.nativeRules=nativeRules;this.waitingCard=waitingCard;this.waitingMercy=waitingMercy;settlementAvailable=settlement;this.purpose=purpose;this.topic=topic;sourceId=source;sourceVariant=variant;sourceSha=sha;this.status=status;this.speakers=List.copyOf(speakers);this.cards=List.copyOf(cards);this.events=List.copyOf(events);
    }
    public static ContestSnapshot none(StateToken state){return new ContestSnapshot(state,Kind.NONE,-1,0,0,0,0,-1,-1,false,false,false,false,"","","","","","",List.of(),List.of(),List.of());}
}
