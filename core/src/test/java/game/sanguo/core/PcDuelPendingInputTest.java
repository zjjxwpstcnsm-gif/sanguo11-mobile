package game.sanguo.core;
import java.util.*;
public final class PcDuelPendingInputTest {
 public static void main(String[]args)throws Exception{
  for(int pending=0;pending<3;pending++){
   byte[]raw=new byte[0x59c];new Random(71).nextBytes(raw);var model=new PcDuelKernel(raw);model.set(0x14,pending);byte[]before=model.state.clone();model.acknowledgeHumanPose();
   var expected=new PcDuelKernel(before);if(pending==1)expected.set(0x14,0);
   if(!Arrays.equals(model.state,expected.state))throw new AssertionError("original acknowledgement whole model "+pending);
  }
  if(args.length>0){int cases=0;for(String row:java.nio.file.Files.readAllLines(java.nio.file.Path.of(args[0]))){var p=row.split("\t");var model=new PcDuelKernel(HexFormat.of().parseHex(p[1]));model.set(0x228,0);byte[]expected=HexFormat.of().parseHex(p[2]);var original=new PcDuelKernel(expected);original.set(0x228,0);
   if(Integer.parseInt(p[0])!=0)model.acknowledgeHumanPose();
   // Original sub2 pending0 takes sub3; pending1 is confirmed on this frame
   // then reaches sub3 on the next frame. Other pending values wait.
   int before=new PcDuelKernel(HexFormat.of().parseHex(p[1])).get(0x14);if(before==0)model.set(12,3);
   if(!Arrays.equals(model.state,original.state))throw new AssertionError("full original sub2 receipt differs "+cases);cases++;}
   if(cases!=6)throw new AssertionError("original coverage");System.out.println("PASS six full original phase5 confirmed/waiting model receipts");}
  System.out.println("PASS original confirmed pending1 only; whole other model bytes unchanged");
 }
}
