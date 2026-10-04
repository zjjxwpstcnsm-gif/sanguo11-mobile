package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original bounded model outputs; headed callback and campaign effects pending. */
public final class PcDebateProgressTest {
    private static int[] values(PcDebateState.Speaker s){
        int[] v=new int[37];int i=0;v[i++]=s.health;v[i++]=s.anger;v[i++]=s.fury;v[i++]=s.modifier;
        for(int n:s.hand)v[i++]=n;v[i++]=s.slots;for(int n:s.deck)v[i++]=n;v[i++]=s.deckCursor;for(int n:s.specialPool)v[i++]=n;v[i++]=s.specialCount;return v;
    }
    public static void main(String[]args)throws Exception{
        int rethink=0,bursts=0,terminals=0;PcDebateState burst=null;
        try(InputStream input=PcDebateProgressTest.class.getResourceAsStream("/pc-debate/original-progress.tsv")){
            if(input==null)throw new IOException("Original progress fixture missing");
            for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                String[] p=line.split("\t");int[] n=new int[p.length-1];for(int i=1;i<p.length;i++)n[i-1]=(int)Long.parseLong(p[i]);
                if(p[0].equals("reconsider")){
                    PcDebateState s=new PcDebateState(90,82,n[0]/4,n[0]%4,31,31,23);int side=n[1];s.topic=n[3];s.speaker(side).fury=n[2];if(n[4]>=0)s.speaker(side).deckCursor=n[4];s.random.state=n[5];s.random.draws=0;s.reconsider(side);
                    if(!Arrays.equals(values(s.speaker(side)),Arrays.copyOfRange(n,6,43))||s.reconsiderAvailable[side]!=(n[43]!=0)||s.random.state!=n[44])throw new AssertionError("Native reconsider mismatch "+line+" actual="+Arrays.toString(values(s.speaker(side))));rethink++;
                }else if(p[0].equals("burst")){
                    if(n[3]==0){burst=new PcDebateState(90,82,0,0,31,31,23);burst.topic=n[1];burst.speaker(n[0]).fury=1;burst.burstSide=n[0];burst.burstIndex=0;burst.burstActive=true;burst.random.state=n[2];burst.random.draws=0;}
                    if(burst==null)throw new AssertionError("Original burst sequence missing initial row");burst.timidBurstStep();int[] actual=new int[24];int i=0;
                    for(PcDebateState.Speaker s:new PcDebateState.Speaker[]{burst.left,burst.right}){actual[i++]=s.health;actual[i++]=s.anger;actual[i++]=s.fury;}
                    for(int v:burst.left.hand)actual[i++]=v;for(int v:burst.right.hand)actual[i++]=v;actual[i++]=burst.burstSide;actual[i++]=burst.burstIndex;actual[i++]=burst.burstActive?1:0;actual[i]=burst.random.state;
                    if(!Arrays.equals(actual,Arrays.copyOfRange(n,4,n.length)))throw new AssertionError("Original timid burst mismatch "+line+" actual="+Arrays.toString(actual));bursts++;
                }else if(p[0].equals("terminal")){
                    PcDebateState s=new PcDebateState(90,82,0,0,31,31,23);s.leader=n[0];s.left.health=n[1];s.right.health=n[2];s.terminalWinner=n[3];s.random.state=23;boolean terminal=s.checkTerminal();
                    if(s.terminalWinner!=n[4]||terminal!=(n[5]!=0)||s.random.state!=n[6])throw new AssertionError("Original terminal choice mismatch "+line);terminals++;
                }else throw new AssertionError("Unexpected original progress kind "+line);
            }
        }
        System.out.println("PASS PcDebateProgressTest "+rethink+" native reconsider/deck/RNG, "+bursts+" model-only timid burst steps, "+terminals+" terminal choices; full headed/campaign integration pending");
    }
}
