package game.sanguo.core;

/** Original5d1df0 discovery,5b8140 date check and5d16b0 score.
 * Candidate eligibility, fees, controller and result callbacks are separate.
 * Pure arithmetic never exposes an undiscovered person's name in preview. */
final class PcSearchRules {
 static int chance(int candidates,int politics,boolean eye,boolean sameRegion){
  if(candidates<0||politics<0||politics>255)throw new IllegalArgumentException("Original search input domain");
  if(candidates==0)return 0;if(eye)return 100;
  return (7+3*Math.min(candidates,5))*politics*(sameRegion?11:10)/300;
 }
 static boolean discovers(int chance,int date,int actorNative,int firstTargetGap){
  if(chance<=0)return false;
  return PcCommandRoll.calculate(100,date,actorNative,firstTargetGap,0,0,0,0)<chance;
 }
 static int score(int affinityGap,int actorAge,PcMerchantRules.Draws random){
  if(affinityGap<0||affinityGap>75||actorAge<0||random==null)throw new IllegalArgumentException("Original search score input domain");
  return ((149-affinityGap)*100+actorAge)*10+random.uniform(10);
 }
 private PcSearchRules(){}
}
