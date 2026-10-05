package game.sanguo.core;

import java.util.ArrayList;
import java.util.List;

/** Numeric RNG protocol of original UI queue callbacks. This is not a renderer.
 * Call once in core before recorded presentation. It is not yet enabled by the
 * production contest policy. COUNTER must not also call State.counterQueue,
 * which currently owns its own draws and the bounded card removal. */
final class PcDebateUiRandom {
    enum Callback { INSTANT, SHOUT, ORDINARY, RETHINK, CALM, GUILE, IGNORE,
        COUNTER, BURST_STRIKE, BURST_END, RASH, STAGES }
    static final class Choice {
        final int side,action;
        Choice(int side,int action){this.side=side;this.action=action;}
    }
    private static void choose(List<Choice> result,PcMerchantRules.Random random,int side,int yes,int no){
        result.add(new Choice(side,random.percent(50)?yes:no));
    }
    static List<Choice> choices(PcDebateState state,Callback callback,int side,int nativeCard,
                                boolean deflected,int counter,int[] previousStages){
        if(state==null||callback==null||side<0||side>1)throw new IllegalArgumentException("Original UI callback inputs required");
        if(callback==Callback.ORDINARY&&(nativeCard<1||nativeCard>9))throw new IllegalArgumentException("Original ordinary card required");
        if(callback==Callback.COUNTER&&counter!=-1&&counter!=13&&counter!=14)throw new IllegalArgumentException("Original counter required");
        if(callback==Callback.STAGES&&(previousStages==null||previousStages.length!=2))throw new IllegalArgumentException("Original UI stages required");
        List<Choice> result=new ArrayList<>();PcMerchantRules.Random random=state.random;
        switch(callback){
            case INSTANT:case SHOUT:case RETHINK:case CALM:break;
            case ORDINARY:
                // Original5195c0: fury animation first; a non-deflected reply
                // consumes another draw even when neither actor is furious.
                if(state.speaker(side).fury>0)choose(result,random,side,7,8);
                if(!deflected)choose(result,random,1-side,4,5);break;
            case GUILE:choose(result,random,side,9,10);break;
            case IGNORE:choose(result,random,side,7,8);break;
            case COUNTER:
                choose(result,random,side,7,8);
                if(counter>=0)choose(result,random,1-side,counter==13?9:7,counter==13?10:8);break;
            case BURST_STRIKE:
                choose(result,random,side,7,8);choose(result,random,1-side,4,5);break;
            case BURST_END:case RASH:choose(result,random,side,7,8);break;
            case STAGES:
                // Original51aa70 draws in fixed left/right order only for
                // changed health stages. Fury changes the motion, not this gate.
                for(int actor=0;actor<2;actor++)if(Math.max(0,Math.min(3,(1000-state.speaker(actor).health)/250))!=previousStages[actor])choose(result,random,actor,4,5);
                break;
            default:throw new IllegalStateException("Unexamined original callback");
        }
        return List.copyOf(result);
    }
    private PcDebateUiRandom(){}
}
