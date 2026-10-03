package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcDelayedCounterTest {
 public static void main(String[] args)throws Exception{
  int count=0;try(var reader=new BufferedReader(new InputStreamReader(PcDelayedCounterTest.class.getResourceAsStream("/pc-delayed-counter-native.tsv"),StandardCharsets.UTF_8))){reader.readLine();for(String row;(row=reader.readLine())!=null;){String[] v=row.split("\t");int[] current=Arrays.stream(v[0].split(",")).mapToInt(Integer::parseInt).toArray();int item=Integer.parseInt(v[1]),skill=Integer.parseInt(v[2]),expected=Integer.parseInt(v[3]);int actual=PcProduction.delayedCounter(current,item,skill==82,skill==83,false);if(actual!=expected)throw new AssertionError(row+" actual="+actual);count++;}}
  if(count!=588)throw new AssertionError("Original cases incomplete");System.out.println("PASS PcDelayedCounterTest original588; pure counter only, normal delayed flow pending");
 }
}
