package game.sanguo.core;
/** Nine actual original source0/city8/target222 decisions. Admission/result/APK separate. */
public final class PcDirectRecruitmentRulesTest {
 public static void main(String[]args){int[]actors={116,163,195,198,251,355,365,377,466},charm={67,81,64,78,74,65,91,69,85},chance={13,21,11,21,19,14,29,15,25};boolean[]wins={false,true,false,false,false,false,false,true,false};int checks=0;
  for(int i=0;i<actors.length;i++){boolean actual=PcDirectRecruitmentRules.succeeds(chance[i],564,222,actors[i],0,charm[i],i==1||i==5?34:i==3?36:35);if(actual!=wins[i])throw new AssertionError("Original source decision differs actor"+actors[i]+" "+actual);checks++;}
  if(PcDirectRecruitmentRules.meritAward(false)!=10||PcDirectRecruitmentRules.meritAward(true)!=200||PcDirectRecruitmentRules.charmExperience(false)!=1||PcDirectRecruitmentRules.charmExperience(true)!=5)throw new AssertionError("Direct and search callbacks conflated");checks+=4;
  System.out.println("PASS original independent samecity direct datehash "+checks+" checks; special gates/currentraw/admission/fullsource/player/normalAPK still separate");
 }
}
