package game.sanguo.core;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Original raw-byte access versus current untrusted ordinary campaign.
 * The required-raw synthetic vector is labelled and retains strict rejection. */
public final class PcDuelSupportRawConsumptionTest {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of("out/session-b/support-raw-consumption-source0-v1.json"));
  check(PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals("3a19c232c4e23c619bd4cee86a1436aa232d13f7879d9d6c988af25b23032421"),"pinned complete original508890 read observation");
  var ints=java.util.regex.Pattern.compile("(?<=[\\s:\\[,])\\d{10,}(?=\\s*[,}\\]])").matcher(new String(raw,java.nio.charset.StandardCharsets.UTF_8));
  var receipt=MapJson.object(MapJson.parse(ints.replaceAll(m->Integer.toString((int)Long.parseLong(m.group()))).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  World w=SaveCodec.decode(Files.readAllBytes(Path.of("out/session-b/normal-deployed-duel-current-v4.sg11")));byte[]before=SaveCodec.encode(w);
  int candidate=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==669).findFirst().orElseThrow().officerId;
  int own=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==411).findFirst().orElseThrow().officerId;
  int other=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==503).findFirst().orElseThrow().officerId;
  boolean blocked=false;try{PcDuelRawLoyalty.current(w,candidate);}catch(IOException e){blocked=e.getMessage().equals("当前原始忠诚变更尚未核实");}check(blocked,"actual helper raw remains untrusted");
  var facts=PcDuelBindings.supportCurrent(w,candidate,own,other,PcDuelRuntimeFacts.saved(w));check(facts.rawLoyalty==-1&&!facts.ownStatusZero,"actual non-ruler lazy source has no fake raw value");
  var kernel=new PcDuelKernel(new byte[0x59c]);
  for(Object item:MapJson.array(receipt.get("cases"))){var row=MapJson.object(item);kernel.set(0x10,((Number)row.get("round")).intValue());var random=new PcDuelKernel.Random(23);boolean result=kernel.supportRoll(facts,random);check(result==(((Number)row.get("originalReturn")).intValue()!=0)&&random.state==((Number)row.get("originalRng")).intValue(),"full original nonconsumed-raw result/RNG "+row.get("round")+"/"+row.get("declaredCandidateRaw"));check(MapJson.array(row.get("rawByteReads")).isEmpty(),"original no raw read");}
  check(Arrays.equals(before,SaveCodec.encode(w)),"current support/raw reads whole World/dualRNG/policy byte pure");
  // Declared relation/ruler vector tests the retained needed-raw boundary.
  var needed=new PcDuelKernel.SupportFacts(new int[]{1,70,0,0,0,1,0,0,1,1,1,-1,0,20});int[]reads={0};
  needed.rawLoyaltyGetter=()->{reads[0]++;throw new UncheckedIOException(new IOException("当前原始忠诚变更尚未核实"));};
  kernel.set(0x10,3);var random=new PcDuelKernel.Random(23);check(!kernel.supportRoll(needed,random)&&reads[0]==0&&random.state==23&&random.draws==0,"original before-round gate avoids unknown raw and RNG");
  kernel.set(0x10,4);blocked=false;try{kernel.supportRoll(needed,random);}catch(UncheckedIOException e){blocked=e.getCause().getMessage().equals("当前原始忠诚变更尚未核实");}check(blocked&&reads[0]==1&&random.state==23&&random.draws==0,"actually consumed unknown raw still strict before random");
  needed.rawLoyaltyGetter=()->96;var known=new PcDuelKernel.SupportFacts(new int[]{1,70,0,0,0,1,0,0,1,1,1,96,0,20});var a=new PcDuelKernel.Random(23);var b=new PcDuelKernel.Random(23);check(kernel.supportRoll(needed,a)==kernel.supportRoll(known,b)&&a.state==b.state&&a.draws==b.draws,"lazy known raw preserves original chance and RNG exactly");
  needed.p4887d0=true;needed.rawLoyaltyGetter=()->{throw new AssertionError("higher priority branch cannot read raw");};check(kernel.supportRoll(needed,new PcDuelKernel.Random(23))==kernel.supportRoll(new PcDuelKernel.SupportFacts(new int[]{1,70,1,0,0,1,0,0,1,1,1,96,0,20}),new PcDuelKernel.Random(23)),"original sworn branch precedence does not touch irrelevant raw");
  System.out.println("PASS original support raw consumption/current actual unknown policy/needed guard "+checks+" checks; synthetic required branch distinct from original normal no-read cases");
 }
}
