package game.sanguo.core;
import java.io.*;

/** One immutable admission/resource decision shared by ordinary recruitment UI
 * and submission. Native date check is pure; delayed/special branches explicit. */
public final class RecruitmentPlan {
 public final int cityId,officerId,targetId,goldCost,actionPointsCost,goldAvailable,actionPointsAvailable,chance,travelTurns;
 public final boolean nativeRules,nativeSuccess;public final RuleFailure failure;
 RecruitmentPlan(World w,int city,int officer,int target){
  cityId=city;officerId=officer;targetId=target;World.City c=w.city(city);World.Officer a=w.officer(officer),t=w.officer(target);boolean nativeBranch=false,success=false;int probability=0;RuleFailure error=null;
  try{PcDirectRecruitmentPolicy.Raw raw=PcDirectRecruitmentPolicy.eligible(w,c,a,t);nativeBranch=raw!=null;if(nativeBranch){probability=PcDirectRecruitmentPolicy.probability(w,a,t);success=PcDirectRecruitmentPolicy.decision(w,a,t,raw,probability);}else probability=c==null?0:w.strategy.recruitChance(c.owner,officer,target);}catch(IOException e){throw new IllegalStateException(e);}
  nativeRules=nativeBranch;nativeSuccess=success;chance=probability;goldCost=nativeBranch?0:100;actionPointsCost=nativeBranch?20:10;goldAvailable=c==null?0:c.gold;actionPointsAvailable=w.cityActionPoints(c);travelTurns=c==null||t==null?0:w.recruitment.travelTurns(city,target);
  error=w.cityFailure(c,a,goldCost,actionPointsCost);
  if(error==null&&!w.strategy.canRecruitTarget(city,target))error=new RuleFailure("RECRUIT_TARGET","target","目标须为已登场、可登用的在野或其他势力非君主武将；不能重复派遣");
  if(error==null&&w.relations.refuses(target,officer,c.owner))error=new RuleFailure("RECRUIT_RELATION","target","目标因结义、配偶或厌恶关系拒绝登用");
  if(error==null&&!nativeBranch&&probability==0)error=new RuleFailure("RECRUIT_ZERO_CHANCE","target","当前登用成功率为0，请先改善关系或降低目标忠诚");failure=error;
 }
 public boolean allowed(){return failure==null;}
 public String description(){return nativeRules?"原当城登用：金0、行动力20；使用原日期/人物编号/原忠诚字节/魅力/相性检定，原随机数不重抽。成功功绩200/魅力经验5，失败功绩10/魅力经验1。":"现有工程登用：金100、行动力10；跨城任务和特殊准入的原完整规则仍待核实。";}
}
