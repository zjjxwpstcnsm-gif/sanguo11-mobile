package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcDebateModelTest{
 static int[] values(PcDebateModel m){PcDebateState s=m.state;int[] values=new int[95];int i=0;
  for(int n:new int[]{m.previousPhase,m.phase,m.sub,s.topic,s.leader,m.selected[0],m.selected[1],s.selectedWinner,s.furySide,m.mercy,s.reconsiderAvailable[0]?1:0,s.reconsiderAvailable[1]?1:0,s.stage[0],s.stage[1],s.stageChanged[0]?1:0,s.stageChanged[1]?1:0,s.terminalWinner,m.outcome,s.burstSide,s.burstIndex,s.burstActive?1:0})values[i++]=n;
  for(PcDebateState.Speaker p:new PcDebateState.Speaker[]{s.left,s.right}){values[i++]=p.health;values[i++]=p.anger;values[i++]=p.fury;values[i++]=p.modifier;for(int v:p.hand)values[i++]=v;values[i++]=p.slots;for(int v:p.deck)values[i++]=v;values[i++]=p.deckCursor;for(int v:p.specialPool)values[i++]=v;values[i++]=p.specialCount;}
  return values;
 }
 public static void main(String[]args)throws Exception{PcDebateModel m=null;int caseId=-1,frame=-1,checks=0;
  for(String line:new String(PcDebateModelTest.class.getResourceAsStream("/pc-debate/original-model.tsv").readAllBytes(),StandardCharsets.UTF_8).lines().toList()){int[] n=Arrays.stream(line.split("\t")).mapToInt(v->(int)Long.parseLong(v)).toArray();if(n[0]!=caseId){caseId=n[0];frame=-1;m=new PcDebateModel(new PcDebateState(90,82,caseId/4,caseId%4,31,31,23),0);}
   while(frame<n[1]){m.frame();frame++;}int[] actual=values(m);int[] wanted=Arrays.copyOfRange(n,3,n.length);
   for(int i=0;i<actual.length;i++)if(actual[i]!=wanted[i])throw new AssertionError("Native frame mismatch case="+caseId+" frame="+frame+" field="+i+" expected="+wanted[i]+" actual="+actual[i]+" phase="+m.phase+" sub="+m.sub);
   if(m.state.random.state!=n[2])throw new AssertionError("Native RNG mismatch case="+caseId+" frame="+frame+" expected="+Integer.toUnsignedLong(n[2])+" actual="+Integer.toUnsignedLong(m.state.random.state));checks++;
  }System.out.println("PASS PcDebateModelTest original null-UI model "+checks+" recorded frames across16 cases");
 }
}
