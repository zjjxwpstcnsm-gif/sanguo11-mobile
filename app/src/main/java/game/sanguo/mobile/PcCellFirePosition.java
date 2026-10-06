package game.sanguo.mobile;
/** Supplied417770 cell-center placement. Immutable resource reads; no rules/time/RNG. */
final class PcCellFirePosition {
 final float x,y,z;
 private PcCellFirePosition(float x,float y,float z){this.x=x;this.y=y;this.z=z;}
 static int fineX(int sourceX){return 4*sourceX+114;}
 static int fineY(int sourceX,int sourceY){return 4*sourceY+114+2*(sourceX&1);}
 static PcCellFirePosition source(int sourceX,int sourceY,int terrain,int originalHeightByte){
  if(sourceX<0||sourceY<0||sourceX>=200||sourceY>=200)throw new IllegalArgumentException("Original cell fire source bounds");
  int fineX=fineX(sourceX),fineY=fineY(sourceX,sourceY);
  //41781b always adds the original float bias for native terrain7/8, including byte0.
  float height=terrain==7||terrain==8?(float)((originalHeightByte+(double).0025f)*.5):originalHeightByte*.5f;
  return new PcCellFirePosition(fineX*5f,height,fineY*5f);
 }
}
