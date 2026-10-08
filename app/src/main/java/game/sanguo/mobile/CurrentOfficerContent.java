package game.sanguo.mobile;
import game.sanguo.api.OfficerSnapshot;
import java.util.*;

/** Text-only projection of the saved authority DTO; never derives rules or consults a catalog. */
final class CurrentOfficerContent {
 private CurrentOfficerContent(){}
 static String search(OfficerSnapshot.Officer o){return (o.searchText()+" "+o.id+(o.source==null?"":" "+o.source.nativeId+" "+o.source.sourceVariant)).toLowerCase(Locale.ROOT);}
 static String row(OfficerSnapshot.Officer o){return stats(o.current)+"\n"+o.faction+" · "+o.location+" · "+o.status+(o.source==null?" · 来源未保存":" · 原编号"+o.source.nativeId);}
 private static String stats(List<Integer> values){if(values.size()!=5)return "本档未存完整五维字段";return "统"+values.get(0)+" 武"+values.get(1)+" 智"+values.get(2)+" 政"+values.get(3)+" 魅"+values.get(4);}
 static String detail(OfficerSnapshot.Officer o){
  StringBuilder b=new StringBuilder("稳定人物 ID ").append(o.id).append("\n性别：").append(o.sex.equals("MALE")?"男":o.sex.equals("FEMALE")?"女":"本档未知");
  b.append("\n身份：").append(o.role).append(" · ").append(o.faction).append("\n所在地：").append(o.location).append("\n状态：").append(o.status);
  b.append("\n当前能力：").append(stats(o.current)).append("\n基础能力：").append(stats(o.base)).append("\n成长类型：").append(o.growth.isEmpty()?"本档未存，保留原策略":o.growth).append("\n经验：").append(o.experience.isEmpty()?"本档未存，保留原策略":o.experience);
  String[] apt={"枪","戟","弩","骑","兵器","水军"},rank={"C","B","A","S"};b.append("\n适性：");for(int i=0;i<apt.length;i++){if(i>0)b.append(" / ");int value=i<o.aptitudes.size()?o.aptitudes.get(i):-1;b.append(apt[i]).append(value>=0&&value<rank.length?rank[value]:"未知");}
  b.append("\n忠诚：").append(o.loyalty).append("\n官职：").append(o.office.isEmpty()?"无":o.office).append(" · 功绩").append(o.merit).append(" · 统兵").append(o.commandLimit);
  b.append("\n伤病：").append(o.injury).append(" · 剩").append(o.injuryTurns).append("旬\n").append(o.lifeDescription).append("\n").append(o.loyaltyDescription);
  b.append("\n特技：").append(o.skillName).append("（").append(o.skillId).append("）\n").append(o.skillDescription).append("\n当前关系：\n").append(o.relationsDescription).append("\n宝物：\n").append(o.treasuresDescription);
  if(o.source==null)b.append("\n原字 / 原传记 / 来源：本档未保存，未从补充目录追填");
  else{
   var s=o.source;b.append("\n原编号：").append(s.nativeId).append(" · 标准人物连接：").append(s.canonicalOfficerId==null?"未知":s.canonicalOfficerId).append("\n来源变体：").append(s.sourceVariant).append("\n身份核验：").append(s.identityStatus);
   b.append("\n字：").append(s.courtesy.isEmpty()?s.courtesyRaw.isEmpty()?"原记录未记":"未解码（"+s.courtesyRaw+"）":s.courtesy);
   b.append("\n").append(s.originalInformation).append("\n原传记：\n").append(s.biography.isEmpty()?"本档未保存已核实正文":s.biography);
   if(!s.originalFields.isEmpty())b.append("\n原记录字段（原编号；未列语义不猜测）：\n").append(s.originalFields);
   b.append("\n来源文件：").append(s.sourcePath).append("\n来源 SHA：").append(s.sourceSha).append("\n人物记录 SHA：").append(s.recordSha);
   if(!s.biographyResourceSha.isEmpty())b.append("\n传记资源 SHA：").append(s.biographyResourceSha);if(!s.biographyRenderedSha.isEmpty())b.append("\n原传记输出 SHA：").append(s.biographyRenderedSha);
  }
  LinkedHashSet<String> gaps=new LinkedHashSet<>(o.unknown);if(o.source!=null)gaps.addAll(o.source.unknown);if(!gaps.isEmpty())b.append("\n未核实 / 未保存字段：\n").append(String.join("\n",gaps));
  return b.toString();
 }
}
