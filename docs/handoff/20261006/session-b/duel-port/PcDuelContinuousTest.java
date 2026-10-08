import java.nio.file.*;
import java.util.*;
/** Continuous computed state compared to original; expected frames never feed rules. */
final class PcDuelContinuousTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label+" checks="+checks);}
 public static void main(String[]args)throws Exception{
  PcDuelKernel m=null;PcDuelKernel.Actor[][]actors=null;PcDuelKernel.SupportFacts[][][][]catalog=null;boolean[][]relations=null;PcDuelKernel.Random random=null;PcDuelKernel.OriginalSettings settings=null;byte[]manager=null;boolean terminalSettingValid=false;int terminalSetting=-1,lastResult=0;
  for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");
   if(p[0].equals("START")){
    manager=PcDuelKernel.hex(p[2]);m=new PcDuelKernel(new byte[0x59c]);m.set(0,Integer.parseInt(p[3]));random=new PcDuelKernel.Random(23);actors=new PcDuelKernel.Actor[2][3];String[]af=p[7].split(",");
    for(int i=0;i<6;i++){String[]v=af[i].split(":");int[]wars=new int[4];for(int j=0;j<4;j++)wars[j]=Integer.parseInt(v[9+j]);PcDuelKernel.Actor a=new PcDuelKernel.Actor(Integer.parseInt(v[0]),Integer.parseInt(v[1]),wars[0],Integer.parseInt(v[2]),v[3].equals("1"),v[4].equals("1"));a.originalAge=Integer.parseInt(v[5]);a.raw489080=Integer.parseInt(v[6]);a.virtual48Value=v[7].equals("1");a.sourceInjuryProtection=v[8].equals("1");a.warByInjury=wars;actors[i/3][i%3]=a;}
    catalog=new PcDuelKernel.SupportFacts[2][3][3][3];for(String row:p[8].split(",")){String[]v=row.split(":");int[]values=new int[v.length-4];for(int j=0;j<values.length;j++)values[j]=Integer.parseInt(v[j+4]);catalog[Integer.parseInt(v[0])][Integer.parseInt(v[1])][Integer.parseInt(v[2])][Integer.parseInt(v[3])]=new PcDuelKernel.SupportFacts(values);}
    relations=new boolean[6][6];String[]rr=p[9].split(",");for(int i=0;i<36;i++)relations[i/6][i%6]=rr[i].equals("1");settings=new PcDuelKernel.OriginalSettings(p[10].equals("1"),Integer.parseInt(p[11]),p[10].equals("1"),Integer.parseInt(p[12]));terminalSettingValid=p[13].equals("1");terminalSetting=Integer.parseInt(p[14]);
    check(m.initializeModel(manager,Integer.parseInt(p[4]),actors,new int[6][0][2],random),"full initializer");check(Arrays.equals(m.state,PcDuelKernel.hex(p[5])),"full initial model");check(Integer.toUnsignedLong(random.state)==Long.parseLong(p[6]),"full initial RNG");lastResult=0;
   }else if(p[0].equals("FRAME")){
    int next=m.get(8),phase=next>=0&&next<=12?next:m.get(4);if(phase==1)lastResult=m.frameOpening(actors,relations,random,settings);else if(phase==12)lastResult=m.frameTerminal(actors,relations,random,terminalSettingValid,terminalSetting,manager);else{
     PcDuelKernel.SupportFacts[][]support=new PcDuelKernel.SupportFacts[2][3];for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++)support[side][slot]=catalog[side][m.activeIndex(side)][m.activeIndex(1-side)][slot];if(p.length>=9){boolean[]requests={p[6].equals("request"),false};int[]poses={p[6].equals("pose")?Integer.parseInt(p[7]):-1,-1};lastResult=m.frameHuman(actors,support,random,settings,requests,poses,p[6].equals("special")?Integer.parseInt(p[8]):-1);}else lastResult=m.frameRules(actors,support,random,settings);
    }
    String where="case="+p[1]+" frame="+p[2]+" phase="+phase;check(lastResult==Integer.parseInt(p[5]),where+" return");check(Integer.toUnsignedLong(random.state)==Long.parseLong(p[4]),where+" RNG");byte[]expected=PcDuelKernel.hex(p[3]);if(!Arrays.equals(m.state,expected)){PcDuelKernel e=new PcDuelKernel(expected);for(int at=0;at<m.state.length;at+=4)if(m.get(at)!=e.get(at))System.out.println("DIFF "+Integer.toHexString(at)+" got "+m.get(at)+" expected "+e.get(at));}check(Arrays.equals(m.state,expected),where+" whole model");
   }else if(p[0].equals("END")){check(Arrays.equals(manager,PcDuelKernel.hex(p[2])),"whole final manager case="+p[1]);check((lastResult==1)==p[3].equals("1"),"whole terminal case="+p[1]);System.out.println("PASS continuous case "+p[1]+" terminal "+p[3]);}
  }System.out.println("PASS continuous full initializer/model/manager/RNG "+checks+" checks; normal campaign/APK incomplete");
 }
}
