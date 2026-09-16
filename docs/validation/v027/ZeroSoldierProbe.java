package game.sanguo.core;
/** Reproduces zero-soldier legacy-compatible cargo gaining a soldier on poison terrain. */
public final class ZeroSoldierProbe {
 public static void main(String[] args)throws Exception {
  World w=LogisticsCampaignTest.fixture();
  if(!w.domestic.transport(11,12,4,100,1000,0,new int[4]).ok)throw new AssertionError("dispatch");
  Domestic.Mission m=w.domestic.missions.get(0);Hex target=new Hex(4,15);w.terrain[4][15]=World.Terrain.POISON;
  World.Result moved=w.orders.execute(w.orders.previewMove(m.id,target));if(!moved.ok)throw new AssertionError(moved.message);
  System.out.println("zero-soldier convoy after actual poison movement: troops="+m.troops);
  if(m.troops!=0)throw new AssertionError("terrain damage created soldiers");
 }
}
