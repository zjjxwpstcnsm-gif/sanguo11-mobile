package game.sanguo.core;
/** Original5986a0 numeric replenishment. The original47e9b0 scope/counts,
 * live commander/adviser admission and opening/turn caller remain separate. */
final class PcArmyActionRules {
 static int after(int current,boolean leaderValid,int leadership,int charm,int cities,int officers,
                  boolean adviserValid,int adviserIntelligence,int tallyPlatforms){
  if(!leaderValid)return 0;
  int cityLimit=Math.min(cities,6)*10;
  int base=Math.max(6,Math.max(leadership,charm)/5)+10+cityLimit+Math.min(officers,cityLimit);
  int gain=adviserValid?base*(100+(adviserIntelligence-60)/2)/100:base;
  gain+=tallyPlatforms*5;
  return Math.min(255,current+gain);
 }
 private PcArmyActionRules(){}
}
