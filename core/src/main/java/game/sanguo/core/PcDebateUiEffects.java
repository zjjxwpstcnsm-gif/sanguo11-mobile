package game.sanguo.core;

import java.util.*;

/** Core-owned bounded original queue effects. Media consumes recorded choices.
 * Optional GUI scheduling/widget/campaign branches remain outside this policy. */
final class PcDebateUiEffects {
    static final class Event {
        final int nativeFunction,side,card,counter;
        final boolean reflected;
        final List<PcDebateUiRandom.Choice> choices;
        Event(int function,int side,int card,boolean reflected,int counter,List<PcDebateUiRandom.Choice> choices){
            nativeFunction=function;this.side=side;this.card=card;this.reflected=reflected;this.counter=counter;this.choices=List.copyOf(choices);
        }
    }
    final int[] stages={0,0};
    /** Explicit original relationship results per possible winning speaker. */
    final int[] terminalPreferences={-1,-1};
    final List<Event> events=new ArrayList<>();
    private static int function(PcDebateUiRandom.Callback kind){
        switch(kind){
            case INSTANT:return 0x519150;case SHOUT:return 0x5193f0;case ORDINARY:return 0x5195c0;
            case RETHINK:return 0x5198d0;case CALM:return 0x5199e0;case GUILE:return 0x519b90;
            case IGNORE:return 0x519e00;case COUNTER:return 0x519fd0;case BURST_STRIKE:return 0x51a4a0;
            case BURST_END:return 0x51a730;case RASH:return 0x51a870;case STAGES:return 0x51aa70;
            default:throw new IllegalArgumentException("Unexamined original callback");
        }
    }
    void callback(PcDebateState state,PcDebateUiRandom.Callback kind,int side,int card,boolean reflected,int counter){
        List<PcDebateUiRandom.Choice> choices=PcDebateUiRandom.choices(state,kind,side,card,reflected,counter,stages);
        events.add(new Event(function(kind),side,card,reflected,counter,choices));
        if(kind==PcDebateUiRandom.Callback.COUNTER&&counter>=0){
            PcDebateState.Speaker target=state.speaker(1-side);
            for(int slot=0;slot<target.slots;slot++)if(target.hand[slot]==counter){target.select(slot);break;}
        }
        if(kind==PcDebateUiRandom.Callback.STAGES)for(int actor=0;actor<2;actor++)stages[actor]=Math.max(0,Math.min(3,(1000-state.speaker(actor).health)/250));
    }
    void tie(){events.add(new Event(0x5184a0,0,-1,false,-1,List.of()));}
}
