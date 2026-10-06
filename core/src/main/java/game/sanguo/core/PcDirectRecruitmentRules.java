package game.sanguo.core;

/** Original4afd60 same-city mode0 after the separate4af7d0 eligibility gate.
 * This uses the caller's raw loyalty byte, never the capped display value.
 * No global RNG draw or optional search debate belongs to this decision. */
final class PcDirectRecruitmentRules {
 static boolean succeeds(int probability,int date,int targetNative,int actorNative,int rawLoyalty,int charm,int affinityGap){
  if(probability<0||rawLoyalty<0||rawLoyalty>255||charm<0||charm>255||affinityGap<0||affinityGap>75)throw new IllegalArgumentException("Original direct recruitment input domain");
  return PcCommandRoll.calculate(100,date,targetNative,actorNative,rawLoyalty,charm,affinityGap,0)<Math.min(100,probability);
 }
 static int meritAward(boolean success){return success?200:10;}
 static int charmExperience(boolean success){return success?5:1;}
 private PcDirectRecruitmentRules(){}
}
