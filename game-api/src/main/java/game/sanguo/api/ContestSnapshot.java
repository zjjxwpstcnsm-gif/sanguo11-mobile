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
    /** Original single-combat model facts; numerical codes retain source meaning. */
    public static final class DuelFighter {
        public final int side,slot,officerId,nativeId,health,spirit,injury,stance,status,gear,terminalOutcome;
        public final String name;
        public final boolean active;
        public final List<Integer> moveCharges;
        public DuelFighter(int side,int slot,int officerId,int nativeId,String name,int health,int spirit,int injury,int stance,int status,int gear,boolean active,List<Integer> charges){this(side,slot,officerId,nativeId,name,health,spirit,injury,stance,status,gear,active,charges,-1);}
        public DuelFighter(int side,int slot,int officerId,int nativeId,String name,int health,int spirit,int injury,int stance,int status,int gear,boolean active,List<Integer> charges,int terminalOutcome){if(terminalOutcome< -1||terminalOutcome>2)throw new IllegalArgumentException("Original terminal outcome");this.terminalOutcome=terminalOutcome;this.side=side;this.slot=slot;this.officerId=officerId;this.nativeId=nativeId;this.name=name;this.health=health;this.spirit=spirit;this.injury=injury;this.stance=stance;this.status=status;this.gear=gear;this.active=active;moveCharges=List.copyOf(charges);}
    }
    public static final class DuelTeam {
        public final int side,activeSlot,count,context,attackBuff,defenseBuff,spiritBuff;
        public final boolean human;
        public final List<DuelFighter> fighters;
        public DuelTeam(int side,int activeSlot,int count,int context,int attackBuff,int defenseBuff,int spiritBuff,boolean human,List<DuelFighter> fighters){this.side=side;this.activeSlot=activeSlot;this.count=count;this.context=context;this.attackBuff=attackBuff;this.defenseBuff=defenseBuff;this.spiritBuff=spiritBuff;this.human=human;this.fighters=List.copyOf(fighters);}
    }
    public static final class PhysicalRecovery {
        public final boolean enabled,adoptionAvailable;public final String error;
        public PhysicalRecovery(boolean enabled,boolean available,String error){this.enabled=enabled;adoptionAvailable=available;this.error=error==null?"":error;}
    }
    public static final class NativeDuel {
        public final PhysicalRecovery physicalRecovery;
        public final String settlementError;
        public final boolean loyaltyInputEnabled,loyaltyInputAdoptionAvailable;
        public final String loyaltyInputError;
        public final boolean aiActorPolicyEnabled,aiActorPolicyAdoptionAvailable;
        public final String aiActorPolicyError,aiActorName;
        public final int aiActorOfficerId,aiActorNativeId;
        public final boolean humanActorPolicyEnabled,humanActorPolicyAdoptionAvailable;
        public final String humanActorPolicyError,humanActorName;
        public final int humanActorOfficerId,humanActorNativeId;
        public final boolean recruitItemPolicyEnabled,recruitItemPolicyAdoptionAvailable;
        public final String recruitItemPolicyError,recruitItemRecipientName;
        public final int recruitItemRecipientOfficerId,recruitItemRecipientNativeId;
        public final PcOpeningOptionsSnapshot openingOptions;
        public final int roundLimit,frames,inputs,scene,openingSpeechCode;
        /** Saved command facts; speech code is not an audio/profile ID. */
        public final boolean commandBoundary,previousOpponentActed;
        public final boolean waitingInput,terminal;
        public final List<DuelTeam> teams;
        public final List<DuelInput> choices;
        public final List<DuelDisposition> disposition;
        public final boolean dispositionReady;
        public final DuelInheritance inheritance;
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams){this(limit,frames,inputs,waiting,terminal,teams,List.of());}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices){this(limit,frames,inputs,waiting,terminal,teams,choices,List.of(),false);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,-1);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,false,false,-1);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,null);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,null);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,"");}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,settlementError,false,false,"");}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError,boolean loyaltyInputEnabled,boolean loyaltyInputAdoptionAvailable,String loyaltyInputError){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,settlementError,loyaltyInputEnabled,loyaltyInputAdoptionAvailable,loyaltyInputError,false,false,"",-1,-1,"");}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError,boolean loyaltyInputEnabled,boolean loyaltyInputAdoptionAvailable,String loyaltyInputError,boolean aiActorPolicyEnabled,boolean aiActorPolicyAdoptionAvailable,String aiActorPolicyError,int aiActorOfficerId,int aiActorNativeId,String aiActorName){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,settlementError,loyaltyInputEnabled,loyaltyInputAdoptionAvailable,loyaltyInputError,aiActorPolicyEnabled,aiActorPolicyAdoptionAvailable,aiActorPolicyError,aiActorOfficerId,aiActorNativeId,aiActorName,false,false,"",-1,-1,"");}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError,boolean loyaltyInputEnabled,boolean loyaltyInputAdoptionAvailable,String loyaltyInputError,boolean aiActorPolicyEnabled,boolean aiActorPolicyAdoptionAvailable,String aiActorPolicyError,int aiActorOfficerId,int aiActorNativeId,String aiActorName,boolean humanActorPolicyEnabled,boolean humanActorPolicyAdoptionAvailable,String humanActorPolicyError,int humanActorOfficerId,int humanActorNativeId,String humanActorName){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,settlementError,loyaltyInputEnabled,loyaltyInputAdoptionAvailable,loyaltyInputError,aiActorPolicyEnabled,aiActorPolicyAdoptionAvailable,aiActorPolicyError,aiActorOfficerId,aiActorNativeId,aiActorName,humanActorPolicyEnabled,humanActorPolicyAdoptionAvailable,humanActorPolicyError,humanActorOfficerId,humanActorNativeId,humanActorName,false,false,"",-1,-1,"");}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError,boolean loyaltyInputEnabled,boolean loyaltyInputAdoptionAvailable,String loyaltyInputError,boolean aiActorPolicyEnabled,boolean aiActorPolicyAdoptionAvailable,String aiActorPolicyError,int aiActorOfficerId,int aiActorNativeId,String aiActorName,boolean humanActorPolicyEnabled,boolean humanActorPolicyAdoptionAvailable,String humanActorPolicyError,int humanActorOfficerId,int humanActorNativeId,String humanActorName,boolean recruitItemPolicyEnabled,boolean recruitItemPolicyAdoptionAvailable,String recruitItemPolicyError,int recruitItemRecipientOfficerId,int recruitItemRecipientNativeId,String recruitItemRecipientName){this(limit,frames,inputs,waiting,terminal,teams,choices,disposition,ready,scene,commandBoundary,previousOpponentActed,openingSpeechCode,inheritance,openingOptions,settlementError,loyaltyInputEnabled,loyaltyInputAdoptionAvailable,loyaltyInputError,aiActorPolicyEnabled,aiActorPolicyAdoptionAvailable,aiActorPolicyError,aiActorOfficerId,aiActorNativeId,aiActorName,humanActorPolicyEnabled,humanActorPolicyAdoptionAvailable,humanActorPolicyError,humanActorOfficerId,humanActorNativeId,humanActorName,recruitItemPolicyEnabled,recruitItemPolicyAdoptionAvailable,recruitItemPolicyError,recruitItemRecipientOfficerId,recruitItemRecipientNativeId,recruitItemRecipientName,null);}
        public NativeDuel(int limit,int frames,int inputs,boolean waiting,boolean terminal,List<DuelTeam> teams,List<DuelInput> choices,List<DuelDisposition> disposition,boolean ready,int scene,boolean commandBoundary,boolean previousOpponentActed,int openingSpeechCode,DuelInheritance inheritance,PcOpeningOptionsSnapshot openingOptions,String settlementError,boolean loyaltyInputEnabled,boolean loyaltyInputAdoptionAvailable,String loyaltyInputError,boolean aiActorPolicyEnabled,boolean aiActorPolicyAdoptionAvailable,String aiActorPolicyError,int aiActorOfficerId,int aiActorNativeId,String aiActorName,boolean humanActorPolicyEnabled,boolean humanActorPolicyAdoptionAvailable,String humanActorPolicyError,int humanActorOfficerId,int humanActorNativeId,String humanActorName,boolean recruitItemPolicyEnabled,boolean recruitItemPolicyAdoptionAvailable,String recruitItemPolicyError,int recruitItemRecipientOfficerId,int recruitItemRecipientNativeId,String recruitItemRecipientName,PhysicalRecovery physicalRecovery){this.physicalRecovery=physicalRecovery;this.recruitItemPolicyEnabled=recruitItemPolicyEnabled;this.recruitItemPolicyAdoptionAvailable=recruitItemPolicyAdoptionAvailable;this.recruitItemPolicyError=recruitItemPolicyError==null?"":recruitItemPolicyError;this.recruitItemRecipientOfficerId=recruitItemRecipientOfficerId;this.recruitItemRecipientNativeId=recruitItemRecipientNativeId;this.recruitItemRecipientName=recruitItemRecipientName==null?"":recruitItemRecipientName;this.humanActorPolicyEnabled=humanActorPolicyEnabled;this.humanActorPolicyAdoptionAvailable=humanActorPolicyAdoptionAvailable;this.humanActorPolicyError=humanActorPolicyError==null?"":humanActorPolicyError;this.humanActorOfficerId=humanActorOfficerId;this.humanActorNativeId=humanActorNativeId;this.humanActorName=humanActorName==null?"":humanActorName;this.aiActorPolicyEnabled=aiActorPolicyEnabled;this.aiActorPolicyAdoptionAvailable=aiActorPolicyAdoptionAvailable;this.aiActorPolicyError=aiActorPolicyError==null?"":aiActorPolicyError;this.aiActorOfficerId=aiActorOfficerId;this.aiActorNativeId=aiActorNativeId;this.aiActorName=aiActorName==null?"":aiActorName;this.loyaltyInputEnabled=loyaltyInputEnabled;this.loyaltyInputAdoptionAvailable=loyaltyInputAdoptionAvailable;this.loyaltyInputError=loyaltyInputError==null?"":loyaltyInputError;this.settlementError=settlementError==null?"":settlementError;this.openingOptions=openingOptions;this.inheritance=inheritance;if(scene< -1||scene>2||openingSpeechCode!= -1&&openingSpeechCode!=35&&openingSpeechCode!=36)throw new IllegalArgumentException("Original duel scene/speech code");this.commandBoundary=commandBoundary;this.previousOpponentActed=previousOpponentActed;this.openingSpeechCode=openingSpeechCode;this.scene=scene;roundLimit=limit;this.frames=frames;this.inputs=inputs;waitingInput=waiting;this.terminal=terminal;this.teams=List.copyOf(teams);this.choices=List.copyOf(choices);this.disposition=List.copyOf(disposition);dispositionReady=ready;}
    }
    public static final class DuelInheritance {
        public final int rulerId,selectedHeir;public final List<DuelHeir> candidates;
        public DuelInheritance(int ruler,int selected,List<DuelHeir> candidates){if(ruler<0||selected< -1)throw new IllegalArgumentException("Heir identity");rulerId=ruler;selectedHeir=selected;this.candidates=List.copyOf(candidates);}
    }
    public static final class DuelHeir {
        public final int officerId,nativeId;public final String name,error;
        public DuelHeir(int id,int nativeId,String name,String error){officerId=id;this.nativeId=nativeId;this.name=Objects.requireNonNull(name);this.error=error==null?"":error;}
        public boolean enabled(){return error.isEmpty();}
    }
    public static final class DuelDisposition {
        public final int officerId,nativeId,mask,choice;public final String name,error;
        public final List<String> actionErrors;
        public DuelDisposition(int id,int nativeId,String name,int mask,int choice){this(id,nativeId,name,mask,choice,"");}
        public DuelDisposition(int id,int nativeId,String name,int mask,int choice,String error){this(id,nativeId,name,mask,choice,error,List.of("","","",""));}
        public DuelDisposition(int id,int nativeId,String name,int mask,int choice,String error,List<String> actionErrors){if(actionErrors.size()!=4)throw new IllegalArgumentException("Four disposition actions required");officerId=id;this.nativeId=nativeId;this.name=name;this.mask=mask;this.choice=choice;this.error=error==null?"":error;this.actionErrors=List.copyOf(actionErrors);}
        public boolean enabled(int action){return action>=0&&action<4&&choice==4&&(mask&(1<<action))!=0&&actionErrors.get(action).isEmpty();}
    }
    public static final class DuelInput {
        public final int stance,special,replacement,cost;
        public final String label,error;
        public DuelInput(int stance,int special,int replacement,int cost,String label,String error){this.stance=stance;this.special=special;this.replacement=replacement;this.cost=cost;this.label=Objects.requireNonNull(label);this.error=error==null?"":error;}
        public boolean enabled(){return error.isEmpty();}
    }
    public final NativeDuel nativeDuel;
    public final StateToken state;public final Kind kind;
    public final int contestId,revision,phase,sub,round,leader,winner;
    public final boolean nativeRules,waitingCard,waitingMercy,settlementAvailable;
    public final String purpose,topic,sourceId,sourceVariant,sourceSha,status;
    public final List<Speaker> speakers;public final List<Card> cards;public final List<Event> events;
    public ContestSnapshot(StateToken state,Kind kind,int id,int revision,int phase,int sub,int round,int leader,int winner,
                           boolean nativeRules,boolean waitingCard,boolean waitingMercy,boolean settlement,
                           String purpose,String topic,String source,String variant,String sha,String status,List<Speaker> speakers,List<Card> cards,List<Event> events){
        this(state,kind,id,revision,phase,sub,round,leader,winner,nativeRules,waitingCard,waitingMercy,settlement,purpose,topic,source,variant,sha,status,speakers,cards,events,null);
    }
    public ContestSnapshot(StateToken state,Kind kind,int id,int revision,int phase,int sub,int round,int leader,int winner,
                           boolean nativeRules,boolean waitingCard,boolean waitingMercy,boolean settlement,
                           String purpose,String topic,String source,String variant,String sha,String status,List<Speaker> speakers,List<Card> cards,List<Event> events,NativeDuel nativeDuel){
        this.nativeDuel=nativeDuel;
        this.state=Objects.requireNonNull(state);this.kind=Objects.requireNonNull(kind);contestId=id;this.revision=revision;this.phase=phase;this.sub=sub;this.round=round;this.leader=leader;this.winner=winner;this.nativeRules=nativeRules;this.waitingCard=waitingCard;this.waitingMercy=waitingMercy;settlementAvailable=settlement;this.purpose=purpose;this.topic=topic;sourceId=source;sourceVariant=variant;sourceSha=sha;this.status=status;this.speakers=List.copyOf(speakers);this.cards=List.copyOf(cards);this.events=List.copyOf(events);
    }
    public static ContestSnapshot none(StateToken state){return new ContestSnapshot(state,Kind.NONE,-1,0,0,0,0,-1,-1,false,false,false,false,"","","","","","",List.of(),List.of(),List.of());}
}
