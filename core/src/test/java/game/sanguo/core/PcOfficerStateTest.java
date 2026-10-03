package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Ordinary commands, separate base/current state and complete saved continuations. */
public final class PcOfficerStateTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void ok(World.Result r){check(r.ok,r.message);}
 static byte[] save(World w)throws Exception{return SaveCodec.encode(w);}
 static World copy(World w)throws Exception{return SaveCodec.decode(save(w));}
 static World world(){World w=PcOfficerRankTest.world();w.officerAbilities.initializeOpening(null,false,false);return w;}
 static int version(byte[] bytes)throws Exception{DataInputStream d=new DataInputStream(new ByteArrayInputStream(bytes));d.readInt();return d.readInt();}
 public static void main(String[] args)throws Exception{
  if(args.length==2&&args[0].equals("--write-opening")){
   World opening=ScenarioCatalog.load("coalition-190",1,23L);byte[] bytes=save(opening);
   check(opening.officerAbilities.enabled()&&version(bytes)==34,"normal new opening managed");
   check(Arrays.equals(bytes,save(SaveCodec.decode(bytes))),"opening writer same-runtime full roundtrip");
   java.nio.file.Files.write(java.nio.file.Paths.get(args[1]),bytes);
   System.out.println("PASS PcOfficerStateTest opening=coalition-190 player=1 seed=23 version=34 bytes="+bytes.length);return;
  }
  for(boolean buy:new boolean[]{true,false})for(int xp:new int[]{0,94,95,99,100,2994,2995,2999,3000}){
   World w=world();w.officerAbilities.gainExperience(2,3,xp);byte[] before=save(w);long rng=w.strategy.getRandomState();
   TradePlan p=w.campaign.previewTrade(10,2,buy?TradePlan.Operation.BUY:TradePlan.Operation.SELL,1000);
   check(p.allowed()&&p.effects.abilityStateManaged,"managed normal forecast");
   check(p.effects.politicsExperienceBefore==xp&&p.effects.politicsExperienceAfter==Math.min(3000,xp+5),"XP capped forecast");
   check(Arrays.equals(before,save(w)),"preview full save/RNG pure");
   ok(w.campaign.trade(10,2,buy,1000));
   check(w.officerAbilities.experience(2,3)==Math.min(3000,xp+5)&&w.officer(2).politics==p.effects.politicsAfter,"ordinary command consumes ability forecast");
   check(w.officerAbilities.base(2,3)==80&&w.strategy.getRandomState()==rng,"base/RNG unchanged");
   check(w.government.merit(2)==50&&w.actionPoints[0]==40&&w.city(10).gold==p.effects.goldAfter&&w.city(10).food==p.effects.foodAfter,"other transaction effects");
   byte[] after=save(w);check(version(after)==34&&Arrays.equals(after,save(copy(w))),"new save exact round trip");
   check(!w.campaign.trade(10,2,buy,1000).ok&&Arrays.equals(after,save(w)),"duplicate rejection preserves experience and state");
   World restored=copy(w);for(int t=0;t<4;t++){ok(w.nextTurn());ok(restored.nextTurn());check(Arrays.equals(save(w),save(restored)),"saved continuation all RNGs and state");}
  }
  World w=world();byte[] before=save(w);check(!w.campaign.trade(10,2,true,999).ok&&Arrays.equals(before,save(w)),"bad quantity never earns XP");
  // Every imported rank's ability bonus goes through the actual appointment/removal commands.
  for(Government.Rank rank:Government.ranks()){
   w=world();w.government.earn(2,60000);ok(w.government.appointRank(10,1,2,rank.id));
   for(int stat=0;stat<5;stat++)check(OfficerAbilities.raw(w.officer(2),stat)==80+(rank.abilityStat==stat?rank.abilityBonus:0),"normal rank bonus "+rank.id);
   w=copy(w);ok(w.nextTurn());ok(w.government.removeRank(10,1,2));check(w.officer(2).politics==80&&w.officerAbilities.base(2,3)==80,"remove rank restores saved base");
  }
  w=world();OfficerAbilities.setBase(w.officer(2),3,99);w.government.earn(2,60000);ok(w.government.appointRank(10,1,2,Government.ranks().stream().filter(r->r.nativeId==0).findFirst().get().id));
  check(w.officer(2).politics==100&&w.officerAbilities.base(2,3)==99,"cap never destroys base");w=copy(w);ok(w.nextTurn());ok(w.government.removeRank(10,1,2));check(w.officer(2).politics==99,"cap reversible after save");
  w=world();w.officer(1).sex=World.Sex.MALE;w.officer(2).sex=World.Sex.FEMALE;w.officer(2).skillId=Skill.NEIZHU.id;w.government.earn(1,500);w.government.earn(2,500);w.campaign.earn(0,500);
  ok(w.relations.mediate(10,1,2,Relations.Kind.SPOUSE));check(w.officer(1).politics==81&&w.officer(2).politics==81&&w.officerAbilities.base(1,3)==80,"normal marriage dynamic NEIZHU");
  w=copy(w);w.relations.unlink(1,2,Relations.Kind.SPOUSE);check(w.officer(1).politics==80&&w.officer(2).politics==80,"unlink restores base");w.relations.link(1,2,Relations.Kind.SPOUSE);check(w.officer(1).politics==81,"link no accumulated bonus");
  Editor.Template template=w.editor.template(2);check(template.stat(3)==80,"editor reads base not current");before=save(w);
  Editor.Draft draft=w.editor.officer(2,new int[]{80,80,80,80,80},w.officer(2).aptitude,World.Sex.FEMALE,"none",85,500,template.temper,template.talkMask);
  check(draft.valid()&&Arrays.equals(before,save(w)),"skill edit preview pure");ok(w.editor.apply(draft));check(w.officer(1).politics==80&&w.officer(2).politics==80,"skill edit removes dynamic bonus from both");
  for(int severity=1;severity<=3;severity++){
   w=world();w.contests.injuries.put(2,new Contests.Injury(severity,3));w.officerAbilities.refresh();int expected=severity==1?64:severity==2?40:24;
   for(int s=0;s<4;s++)check(OfficerAbilities.raw(w.officer(2),s)==expected,"native injury percent all four stats");
   check(w.officer(2).charm==80&&w.contests.war(w.officer(2))==expected,"charm unaffected and duel no double penalty");w=copy(w);
   for(int t=0;t<3;t++)ok(w.nextTurn());check(w.officer(2).war==80&&w.officerAbilities.base(2,1)==80,"injury expiry restores base");
  }
  // Original validity rejects spouse status6(unappeared)/8(dead), while captive/undiscovered remain valid.
  w=world();w.officer(2).skillId=Skill.NEIZHU.id;w.relations.link(1,2,Relations.Kind.SPOUSE);check(w.officer(1).politics==81,"valid spouse bonus before death");
  w.life.die(2,"test death through normal lifecycle");check(w.officer(1).politics==80&&w.officerAbilities.base(1,3)==80,"normal death invalidates spouse bonus without erasing relationship");check(Arrays.equals(save(w),save(copy(w))),"death and bonus state saved together");
  w=world();w.officer(2).owner=-1;w.officer(2).role=Strategy.Role.UNAFFILIATED;w.officer(2).loyalty=0;w.officer(2).skillId=Skill.NEIZHU.id;w.relations.link(1,2,Relations.Kind.SPOUSE);
  w.life.configure(2,170,191,0,10,Lifecycle.State.UNAPPEARED);check(w.officer(1).politics==80,"not-yet-appeared spouse contributes no bonus");w.startMonth=12;w.officerAbilities.refresh();
  for(int t=0;t<3;t++)ok(w.nextTurn());check(w.life.present(2)&&w.officer(1).politics==81,"normal January appearance activates spouse bonus");check(Arrays.equals(save(w),save(copy(w))),"appearance state persisted");
  // Normal training writes the base, never the injured current cache; the existing +5 reward is retained.
  w=world();OfficerAbilities.setBase(w.officer(2),0,50);w.contests.injuries.put(2,new Contests.Injury(2,2));w.officerAbilities.refresh();w.abilities.states[0].learned.add("lead.low");
  ok(w.campaign.study(10,2,Campaign.Study.LEADERSHIP));World trainingCopy=copy(w);
  for(int t=0;t<3;t++){ok(w.nextTurn());ok(trainingCopy.nextTurn());check(Arrays.equals(save(w),save(trainingCopy)),"training save continuation");}
  check(w.officerAbilities.base(2,0)==55&&w.officer(2).leadership==55&&w.abilities.gained(2,0)==5,"training survives injury recovery and updates base exactly once");
  // Explicit source import is distinct from merely reusing an integer ID.
  w=world();w.officers.add(new World.Officer(2000,"同号自建",0,10,80,80,80,80,80));check(w.officer(2000).abilityProfile.sourceId==-1&&w.officerAbilities.growthCode(2000,0)==-1,"arbitrary ID does not inherit historical growth");
  w=world();Editor.Draft source=w.editor.sourceOfficer(2000,10,true,false);check(source.valid(),"explicit source preview");ok(w.editor.apply(source));check(w.officer(2000).abilityProfile.sourceId==2000&&w.officerAbilities.growthCode(2000,0)==PcOfficerGrowth.get(2000).curves[0],"explicit source import binds proven growth");
  w=world();ContentProfiles.add(w,ContentCatalog.get(),2000,10,true,false);w.officerAbilities.gainExperience(2000,3,95);
  CustomOfficers.Definition definition=CustomOfficers.historical(ContentCatalog.get().officer(2000),true);Editor.Template old=definition.template;
  definition.template=new Editor.Template("曹操自定义",new int[]{70,71,72,73,74},w.officer(2000).aptitude,old.sex,old.skill,old.temper,old.talkMask);
  World customized=CustomOfficers.apply(w,Collections.singletonList(definition),Collections.singletonList(new CustomOfficers.Placement(definition.id,CustomOfficers.Mode.KEEP,0,10,85,false)),new byte[0]);
  check(customized.officer(2000).name.equals("曹操自定义")&&customized.officerAbilities.base(2000,3)==73&&customized.officerAbilities.experience(2000,3)==95&&customized.officer(2000).abilityProfile.sourceId==2000,"custom historical rename preserves state identity while replacing base");
  check(w.officerAbilities.base(2000,3)!=73&&Arrays.equals(save(customized),save(copy(customized))),"custom composition independent and persisted");
  // Source growth, December boundary, fixed-age setting, persisted modes and no catalog lookup on load.
  for(boolean fixed:new boolean[]{false,true}){
   w=world();w.startMonth=12;w.life.configure(2,140,180,0,10,Lifecycle.State.ACTIVE);w.officer(2).abilityProfile.growth[3]=8;w.officerAbilities.refresh();
   if(fixed){World setup=PcOfficerRankTest.world();setup.startMonth=12;setup.life.configure(2,140,180,0,10,Lifecycle.State.ACTIVE);setup.officerAbilities.initializeOpening(null,true,false);setup.officer(2).abilityProfile.growth[3]=8;setup.officerAbilities.refresh();w=setup;}
   World restored=copy(w);for(int turn=0;turn<39;turn++){ok(w.nextTurn());ok(restored.nextTurn());check(Arrays.equals(save(w),save(restored)),"39-turn saved replay");}
   check(w.officer(2).politics==(fixed?80:100)&&w.officerAbilities.fixedAge()==fixed,"age growth crosses year or stays fixed");
  }
  // New catalog openings are managed; all proven growth mappings and relocation identities are retained.
  int openings=0;
  for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
   w=ScenarioCatalog.load(summary.id,0);check(w.officerAbilities.enabled()&&version(save(w))==34,"all nine normal new openings enable state");
   int imported=0;for(World.Officer o:w.officers){PcOfficerGrowth.Definition d=PcOfficerGrowth.get(o.id);if(o.abilityProfile.sourceId>=0){imported++;check(d!=null&&o.abilityProfile.nativeIds.equals(d.nativeIds),"only verified source identity");for(int s=0;s<5;s++)check(w.officerAbilities.growthCode(o.id,s)==d.curves[s]&&w.officerAbilities.experience(o.id,s)==0,"source growth with proven zero initial experience");}}
   check(imported>0,"opening source profiles");check(Arrays.equals(save(w),save(copy(w))),"opening round trip");openings++;
  }
  check(openings==9,"entire project scenario catalog");
  check(PcOfficerGrowth.get(10279).nativeIds.equals("279,333")&&PcOfficerGrowth.get(10333).nativeIds.equals("279,333"),"relocated identities do not invent canonical native slot");
  w=world();w.officerAbilities.gainExperience(2,3,95);World.Officer retained=w.officer(2);w.officers.remove(retained);w.officers.add(retained);Collections.reverse(w.officers);check(w.officerAbilities.experience(2,3)==95&&Arrays.equals(save(w),save(copy(w))),"roster mutations preserve state identity and experience");
  // Historical writer fixture is generated by frozen AM core.jar, not today's codec.
  try(InputStream in=PcOfficerStateTest.class.getResourceAsStream("Dalvik".equals(System.getProperty("java.vm.name"))?"/pc-officer-legacy-am-art.sg11":"/pc-officer-legacy-am.sg11")){
   byte[] raw=read(in);w=SaveCodec.decode(raw);check(!w.officerAbilities.enabled()&&version(raw)==33&&Arrays.equals(raw,save(w)),"frozen AM legacy exact read/write");
   TradePlan p=w.campaign.previewTrade(10,2,TradePlan.Operation.BUY,1000);check(p.allowed()&&!p.effects.abilityStateManaged&&p.effects.politicsExperienceAfter==-1,"legacy mode explicit in preview");ok(w.campaign.trade(10,2,true,1000));check(w.officer(2).politics==99&&w.government.merit(2)==60000&&version(save(w))==33,"legacy ordinary command preserved without invented base");
  }
  w=world();w.officer(2).politics++;boolean rejected=false;try{save(w);}catch(IOException e){rejected=e.getMessage().contains("当前能力");}check(rejected,"mismatched current cache rejected, never silently repaired by save");
  w=world();w.officer(2).abilityProfile.experience[3]=3001;rejected=false;try{save(w);}catch(IOException e){rejected=true;}check(rejected,"XP overflow rejected");
  System.out.println("PASS PcOfficerStateTest checks="+checks+" openings="+openings+" (prices and non-merchant XP awards remain pending)");
 }
 static byte[] read(InputStream in)throws Exception{if(in==null)throw new IOException("fixture missing");ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] data=new byte[4096];int n;while((n=in.read(data))!=-1)b.write(data,0,n);return b.toByteArray();}
}
