package game.sanguo.core;
/** Original4b5460 comparison for checked valid candidates. Binding supplies
 * original current getters; no cached successor ID or engineering score. */
final class PcRulerSuccessionRules {
 static final int[] PREFERRED={91,403,493,343,18,16,636,365,180,370,517,635,367,347,660,515,442};
 static int preference(int nativeId){for(int i=0;i<PREFERRED.length;i++)if(PREFERRED[i]==nativeId)return PREFERRED.length-i;return 0;}
 static final class Person {
  final int nativeId,age,merit,preference;final boolean[] flags;
  // child, sameFather, fatherOfDead, sameRootOfDeadRoot, motherOfDead,
  // sameMother, original4887d0, original488790, original488780(male).
  Person(int id,int age,int merit,int preference,boolean[] flags){
   if(id<0||id>=1100||merit<0||merit>65535||preference<0||preference>17||flags.length!=9)throw new IllegalArgumentException("Original successor input domain");
   this.nativeId=id;this.age=age;this.merit=merit;this.preference=preference;this.flags=flags.clone();
  }
 }
 static boolean better(Person a,Person b,boolean fatherValid,boolean rootValid,boolean motherValid){
  boolean family=false;
  if(a.flags[0]!=b.flags[0])return a.flags[0];family=a.flags[0]&&b.flags[0];
  if(!family){
   if(fatherValid){if(a.flags[1]!=b.flags[1])return a.flags[1];family=a.flags[1]&&b.flags[1];}
   if(!family){if(a.flags[2]!=b.flags[2])return a.flags[2];family=a.flags[2]&&b.flags[2];}
  }
  if(!family){
   if(rootValid){if(a.flags[3]!=b.flags[3])return a.flags[3];family=a.flags[3]&&b.flags[3];}
   if(!family){if(a.flags[4]!=b.flags[4])return a.flags[4];family=a.flags[4]&&b.flags[4];}
  }
  if(!family&&motherValid){if(a.flags[5]!=b.flags[5])return a.flags[5];family=a.flags[5]&&b.flags[5];}
  if(!family){
   if(a.flags[6]!=b.flags[6])return a.flags[6];family=a.flags[6]&&b.flags[6];
   if(!family){if(a.flags[7]!=b.flags[7])return a.flags[7];family=a.flags[7]&&b.flags[7];}
  }
  if(family){if(a.flags[8]!=b.flags[8])return a.flags[8];if(a.age!=b.age)return a.age>b.age;return a.nativeId<b.nativeId;}
  if(a.preference!=b.preference)return a.preference>b.preference;
  if(a.age!=b.age)return a.age>b.age;
  if(a.merit!=b.merit)return a.merit>b.merit;
  if(a.flags[8]!=b.flags[8])return a.flags[8];
  return a.nativeId<b.nativeId;
 }
 private PcRulerSuccessionRules(){}
}
