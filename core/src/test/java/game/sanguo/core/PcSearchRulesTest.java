package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;

/** Actual original source0 actors/candidates/score/RNG, plus independent16-source cities. */
public final class PcSearchRulesTest {
 public static void main(String[]args)throws Exception {
  int checks=0;int[] actors={116,163,195,198,251,355,365,377,466},politics={50,61,63,68,72,53,73,68,74},chances={36,44,46,49,52,38,53,49,54};boolean[] outcomes={false,false,false,true,true,false,false,true,true};
  int[] firstGaps={45,44,45,46,45,44,45,45,45};for(int i=0;i<actors.length;i++){eq(chances[i],PcSearchRules.chance(5,politics[i],false,false));checks++;eq(outcomes[i]?1:0,PcSearchRules.discovers(chances[i],564,actors[i],firstGaps[i])?1:0);checks++;}
  int[][] gaps={{46,41,46,48,5},{45,42,47,49,4},{45,42,47,49,4},{45,42,47,49,4}};int[] ages={24,29,25,34};int[][] scores={{103243,108243,103242,101249,144240},{104293,107293,102292,100299,145290},{104253,107253,102252,100259,145250},{104343,107343,102342,100349,145340}};int[] rng={(int)3031271500L,(int)4034634293L,(int)3590884386L,827042723,1730814600};
  for(int i=0;i<4;i++){var random=new PcMerchantRules.Random(23);for(int j=0;j<5;j++){eq(scores[i][j],PcSearchRules.score(gaps[i][j],ages[i],random));eq(rng[j],random.state);checks+=2;}eq(5,random.draws);checks++;}
  eq(0,PcSearchRules.chance(0,100,true,true));eq(100,PcSearchRules.chance(1,0,true,false));eq(80,PcSearchRules.chance(5,100,false,true));checks+=3;
  Map<String,Integer> counts=new HashMap<>();Set<String> identities=new HashSet<>();try(var in=PcSearchRulesTest.class.getResourceAsStream("/pc-scenarios/search-regions.tsv")){if(in==null)throw new AssertionError("Missing source region input");for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){if(line.startsWith("#")||line.isBlank())continue;String[]p=line.split("\t");if(p.length!=5||!p[0].endsWith(p[1])||!identities.add(p[0]+":"+p[2]))throw new AssertionError(line);int nativeId=Integer.parseInt(p[2]),region=Integer.parseInt(p[4]);if(nativeId<0||nativeId>=42||region<0||region>=12)throw new AssertionError(line);counts.merge(p[0],1,Integer::sum);checks++;}}
  eq(16,counts.size());for(int n:counts.values())eq(42,n);checks+=17;
  if(args.length>0){int rows=0;for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#")||line.isBlank())continue;int[]v=Arrays.stream(line.split("\t")).mapToInt(Integer::parseInt).toArray();eq(v[8],PcCommandRoll.calculate(v[0],Arrays.copyOfRange(v,1,8)));rows++;}eq(570,rows);checks+=rows;}
  System.out.println("PASS original discovery/score/RNG and16-source city region "+checks+"; complete command/menu acceptance separate");
 }
 static void eq(int expected,int actual){if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
}
