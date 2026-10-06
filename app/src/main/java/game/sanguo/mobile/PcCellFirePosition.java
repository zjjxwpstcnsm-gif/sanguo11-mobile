package game.sanguo.mobile;
import game.sanguo.core.PcMap;
/** Supplied417770 cell-center placement. Immutable resource reads; no rules/time/RNG. */
final class PcCellFirePosition {
 final float x,y,z;
 private PcCellFirePosition(float x,float y,float z){this.x=x;this.y=y;this.z=z;}
 static PcCellFirePosition source(PcMap map,int sourceX,int sourceY){
  if(map==null||sourceX<0||sourceY<0||sourceX>=200||sourceY>=200)throw new IllegalArgumentException("Original cell fire source bounds");
  int fineX=4*sourceX+114,fineY=4*sourceY+114+2*(sourceX&1),terrain=map.terrain(sourceX,sourceY);
  //41781b always adds the original float bias for native terrain7/8, including byte0.
  float height=terrain==7||terrain==8?(float)((map.coarseWaterByte(fineX/4,fineY/4)+(double).0025f)*.5):map.heightByte(fineX,fineY)*.5f;
  return new PcCellFirePosition(fineX*5f,height,fineY*5f);
 }
}
