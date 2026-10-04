package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Independently captured original517600 choices and complete original RNG. */
public final class PcDebateAiTest {
    public static void main(String[] args)throws Exception{
        int[][] hands={{0,1,2,3,10,13,14},{0,1,4,7,10,11,12},{0,2,3,5,6,8,9}};int checks=0;
        try(InputStream input=PcDebateAiTest.class.getResourceAsStream("/pc-debate/original-ai.tsv")){
            if(input==null)throw new IOException("Missing independently captured original AI fixture");
            for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                int[] n=Arrays.stream(line.split("\t")).mapToInt(v->(int)Long.parseLong(v)).toArray();
                PcDebateState state=new PcDebateState(n[0],90,n[1],n[2],31,31,23);
                state.topic=n[5];state.left.health=state.right.health=750;state.left.anger=state.right.anger=85;state.left.fury=n[3];state.right.fury=n[4];
                Arrays.fill(state.left.hand,-1);System.arraycopy(hands[n[7]],0,state.left.hand,0,state.left.slots);System.arraycopy(hands[0],0,state.right.hand,0,7);state.random.state=23;state.random.draws=0;
                int[] leftBefore=state.left.hand.clone(),rightBefore=state.right.hand.clone();int slot=new PcDebateAi(state,0).choose(n[6]);
                if(slot!=n[8]||state.random.state!=n[9])throw new AssertionError("Original AI mismatch "+line+" slot="+slot+" rng="+Integer.toUnsignedLong(state.random.state));
                if(!Arrays.equals(state.left.hand,leftBefore)||!Arrays.equals(state.right.hand,rightBefore)||state.left.health!=750||state.right.health!=750||state.left.anger!=85||state.right.anger!=85||state.left.fury!=n[3]||state.right.fury!=n[4])throw new AssertionError("AI choice mutated rule state beyond original RNG");
                checks++;
            }
        }
        System.out.println("PASS PcDebateAiTest "+checks+" original priority-table choices and exact original RNG; full contest integration pending");
    }
}
