package game.sanguo.mobile;
import game.sanguo.core.PcMap;
import java.io.*;
public final class PcCellFirePositionProbe {
 public static void main(String[] args)throws Exception {
  try(var out=new DataOutputStream(new FileOutputStream(args[0]))){
   for(int x=0;x<200;x++)for(int y=0;y<200;y++){
    var p=PcCellFirePosition.source(PcMap.get(),x,y);
    for(float f:new float[]{p.x,p.y,p.z,1})out.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(f)));
   }
  }
 }
}
