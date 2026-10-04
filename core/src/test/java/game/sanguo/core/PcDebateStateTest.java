package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen original model output comparisons, not arithmetic self-consistency. */
public final class PcDebateStateTest {
    private static List<String> lines(String name)throws IOException{
        try(InputStream input=PcDebateStateTest.class.getResourceAsStream("/pc-debate/"+name)){
            if(input==null)throw new IOException("Missing original fixture "+name);
            return new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
        }
    }
    private static int[] numbers(String[] parts,int start){int[] values=new int[parts.length-start];for(int i=start;i<parts.length;i++)values[i-start]=(int)Long.parseLong(parts[i]);return values;}
    private static void require(boolean pass,String detail){if(!pass)throw new AssertionError(detail);}
    public static void main(String[] args)throws Exception{
        int initial=0,effects=0;
        for(String line:lines("original-initial.tsv")){
            int[] n=numbers(line.split("\t"),0);PcDebateState state=new PcDebateState(n[0],n[1],n[2],n[3],n[4],n[5],n[6]);int cursor=9;
            require(state.topic==n[7]&&state.random.state==n[8],"Initial native topic/RNG mismatch");
            for(PcDebateState.Speaker s:new PcDebateState.Speaker[]{state.left,state.right}){
                int[] values=new int[37];int i=0;values[i++]=s.health;values[i++]=s.anger;values[i++]=s.fury;values[i++]=s.modifier;
                for(int v:s.hand)values[i++]=v;values[i++]=s.slots;for(int v:s.deck)values[i++]=v;values[i++]=s.deckCursor;for(int v:s.specialPool)values[i++]=v;values[i++]=s.specialCount;
                for(int v:values){require(v==n[cursor],"Native initial field "+cursor+" expected="+n[cursor]+" actual="+v);cursor++;}
            }
            require(cursor==n.length,"Unexpected native initial fixture fields");initial++;
        }
        // Immediate effects do not use the constructed decks, nor consume RNG.
        PcDebateState state=new PcDebateState(90,82,0,0,31,31,23);
        for(String line:lines("original-effects.tsv")){
            String[] p=line.split("\t");
            if(p[0].equals("legal")){
                int[] n=numbers(p,1);require(PcDebateRules.legal(n[5],n[0],n[2],n[1],n[3],n[4]!=0)==(n[6]!=0),"Native legality mismatch "+line);
            }else if(p[0].equals("anger")){
                int[] n=numbers(p,1);require(PcDebateRules.ordinaryAnger(n[0])==n[1],"Native ordinary anger mismatch "+line);
            }else if(p[0].equals("effect")){
                int[] n=numbers(p,2);int side=n[0],amount=n[3];boolean reflected=n[4]!=0;
                state.left.health=n[1];state.right.health=1000-n[1];state.left.anger=n[2];state.right.anger=100-n[2];state.left.fury=state.right.fury=0;state.random.state=23;int draws=state.random.draws;
                switch(p[1]){
                    case "tie":state.tie();break;
                    case "shout":state.damageEffect(side,amount,15,false);break;
                    case "ordinary":state.damageEffect(side,amount,20,reflected);break;
                    case "ignore":state.calmEffect(side,amount,false);break;
                    case "calm":state.calmEffect(side,amount,reflected);break;
                    case "rage":state.rageEffect(side,amount,reflected);break;
                    default:throw new AssertionError("Unknown native effect "+line);
                }
                int[] actual={state.left.health,state.left.anger,state.left.fury,state.right.health,state.right.anger,state.right.fury,state.random.state};
                require(Arrays.equals(actual,Arrays.copyOfRange(n,5,n.length))&&state.random.draws==draws,"Native immediate effect mismatch "+line);
            }else throw new AssertionError("Unknown fixture kind "+line);
            effects++;
        }
        int counters=0;
        for(String line:lines("original-ui-counters.tsv")){
            int[] n=numbers(line.split("\t"),0);PcDebateState counterState=new PcDebateState(90,90,n[0],n[0],31,31,n[3]);
            for(PcDebateState.Speaker s:new PcDebateState.Speaker[]{counterState.left,counterState.right})System.arraycopy(new int[]{0,1,2,3,10,13,14},0,s.hand,0,7);
            counterState.random.state=n[3];counterState.random.draws=0;int[] choices=counterState.counterQueue(n[2],n[1]);
            require(counterState.random.state==n[4]&&counterState.random.draws==n[5],"Native counter queue RNG mismatch "+line);
            require(Arrays.equals(counterState.left.hand,Arrays.copyOfRange(n,6,13))&&Arrays.equals(counterState.right.hand,Arrays.copyOfRange(n,13,20)),"Native counter queue hand consumption mismatch "+line);
            require(Arrays.equals(choices,Arrays.copyOfRange(n,20,22)),"Native counter animation record mismatch "+line);
            counters++;
        }
        System.out.println("PASS PcDebateStateTest "+initial+" native initial model/deck/hand/RNG cases, "+effects+" original legality/immediate effect outputs, "+counters+" original bounded counter queue/hand/RNG cases; complete UI/fury/AI/campaign integration pending");
    }
}
