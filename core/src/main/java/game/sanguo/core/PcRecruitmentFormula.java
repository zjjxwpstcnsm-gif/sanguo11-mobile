package game.sanguo.core;
/** Original5c4f80 fallback only. Original4af7d0 relationship/eligibility gate
 * remains a separate required caller; this arithmetic alone never admits a command. */
final class PcRecruitmentFormula {
 static int calculate(int rawLoyalty,int effectiveHonor,int oldRulerGap,int newRulerGap,int charmFloor,
                      int captiveBonus,int oldFamilyPenalty,int oldLikedPenalty,int oldDislikedBonus,int deterministicAdjustment){
  int loyaltyCost=(effectiveHonor+18)*rawLoyalty/20;
  int gapBonus=(oldRulerGap-newRulerGap)/5;
  int charmBonus=charmFloor*3/5;
  return Math.max(0,gapBonus-loyaltyCost+charmBonus-oldLikedPenalty-oldFamilyPenalty+oldDislikedBonus+deterministicAdjustment+captiveBonus+45);
 }
 private PcRecruitmentFormula(){}
}
