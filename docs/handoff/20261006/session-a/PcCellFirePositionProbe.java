package game.sanguo.mobile;
import game.sanguo.core.PcMap;
import java.io.*;
public final class PcCellFirePositionProbe {
 public static void main(String[] args)throws Exception {
  try(var out=new DataOutputStream(new FileOutputStream(args[0]))){
   for(int x=0;x<200;x++)for(int y=0;y<200;y++){
    var map=PcMap.get();int terrain=map.terrain(x,y),fx=PcCellFirePosition.fineX(x),fy=PcCellFirePosition.fineY(x,y);
    int height=terrain==7||terrain==8?map.coarseWaterByte(fx/4,fy/4):map.heightByte(fx,fy);
    var p=PcCellFirePosition.source(x,y,terrain,height);
    for(float f:new float[]{p.x,p.y,p.z,1})out.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(f)));
   }
  }
 }
}
