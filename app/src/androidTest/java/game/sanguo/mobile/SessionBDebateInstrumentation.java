package game.sanguo.mobile;
import android.app.*;import android.content.Intent;import android.os.*;import android.view.*;import android.widget.*;import game.sanguo.core.*;import game.sanguo.api.*;import java.io.*;import java.nio.file.Files;import java.util.*;
/** Ordinary actual recruitment widgets and human native cards; no injected rules/results. */
public final class SessionBDebateInstrumentation extends SessionBFieldworksInstrumentation {
 private World.Officer person(World w,int nativeId)throws Exception {for(var p:PcScenarioPeople.saved(w))if(p.nativeId==nativeId)return w.officer(p.officerId);throw new AssertionError("Missing native identity "+nativeId);}
 private void newSource(int faction)throws Exception {
  var source=PcScenarioCatalog.all().get(0);String name=PcScenarioCatalog.preview(source.identity.scenarioId).faction(faction);
  nav("菜单");text("新游戏 / 选择势力");revealDescription("选择PC来源剧本 "+source.identity.path);awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));revealDescription("选择势力 · "+name);description("确认开局势力");text("开始新局");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(SessionProbe.view(activity).player==faction,"ordinary selected faction "+faction);
 }
 @Override protected void nav(String name)throws Exception {
  for(int attempt=0;attempt<3;attempt++){
   AlertDialog dialog=(AlertDialog)field(activity,"navigationDialog");
   if(dialog==null||!dialog.isShowing()){
    View trigger=await(v->v.isEnabled()&&v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
    android.graphics.Rect bounds=new android.graphics.Rect();int[] rootAt=new int[2];runOnMainSync(()->{trigger.getGlobalVisibleRect(bounds);trigger.getRootView().getLocationOnScreen(rootAt);});note("nav="+name+" attempt="+attempt+" bounds="+bounds+" root="+Arrays.toString(rootAt));tap(trigger);settle();
   }
   dialog=(AlertDialog)field(activity,"navigationDialog");note("nav="+name+" attempt="+attempt+" showing="+(dialog!=null&&dialog.isShowing()));
   if(dialog!=null&&dialog.isShowing()){revealDescription("导航 · "+name);settle();return;}
   shot("navigation-"+name+"-"+attempt);
  }
  throw new AssertionError("Normal enabled navigation trigger did not open "+name);
 }
 private void focusCity(World.City c)throws Exception {nav("地图");runOnMainSync(()->activity.selectAndFocus(c.hex));settle();ClientState ui=(ClientState)field(activity,"ui");if(!ui.panelVisible)description("选中对象指令 ·");text("展开");}
 private void pageAction(String s)throws Exception {runOnMainSync(()->{View hit=search(activity.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().startsWith(s));if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);});settle();text(s);}
 private void saveActual(String name)throws Exception {byte[] saved=capture();Files.write(new File(evidence,name).toPath(),saved);boolean existed=new File(activity.getFilesDir(),"manual3.sg11").exists();nav("菜单");text("保存局面");preparedOption("槽位 3");if(existed)text("覆盖存档");check(Arrays.equals(saved,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"ordinary fullWorld/bothRNG saved "+name);}
 private void loadActual(String name)throws Exception {byte[] expected=Files.readAllBytes(new File(evidence,name).toPath());nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"ordinary manual load fullWorld/bothRNG "+name);}

 private ContestSnapshot facts(){ContestSnapshot[] value={null};runOnMainSync(()->value[0]=activity.contestSnapshot());return value[0];}
 private void choosePerson(String name)throws Exception {EditText search=(EditText)await(v->v instanceof EditText&&"表格搜索".equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},search,name);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(name)));}
 private void enterDebate(boolean cancel)throws Exception {
  World w=SessionProbe.view(activity);World.Officer actor=person(w,116),target=person(w,222);World.City city=w.city(actor.cityId);byte[] before=capture();int gold=city.gold,ap=w.actionPoints[w.player];
  focusCity(city);pageAction("武将");pageAction("舌战登用");choosePerson(target.name);choosePerson(actor.name);shot(cancel?"recruit-cancel-preview":"recruit-preview");
  if(cancel){text("取消");check(Arrays.equals(before,capture()),"ordinary recruitment preview/cancel entireWorld/all RNG pure");return;}
  text("执行");w=SessionProbe.view(activity);check(w.contests.busy()&&facts().nativeRules,"ordinary actual recruitment entry native model");check(w.city(city.id).gold==gold-100&&w.actionPoints[w.player]==ap-10,"declared engineering gold100/AP10 start, original admission/cost not claimed");check(facts().speakers.get(0).nativeId==116&&facts().speakers.get(1).nativeId==222,"current source/native actor-target joins");shot("ordinary-native-entry");
 }
 private void legalCard(boolean poor)throws Exception {
  var f=facts();check(f.waitingCard,"actual human native card boundary");ContestSnapshot.Card best=null;int score=poor?Integer.MAX_VALUE:Integer.MIN_VALUE;
  for(var c:f.cards)if(c.enabled()&&c.nativeCard!=0){int n=c.nativeCard,value=n==12?120:n==10?110:n==11?20:n==13||n==14?0:(n-1)%3+1;if(best==null||(poor?value<score:value>score)){best=c;score=value;}}
  if(best==null)for(var c:f.cards)if(c.enabled()){best=c;break;}check(best!=null,"actual enabled legal card");String label=(best.nativeCard==0?"再考 · ":"出牌 · ")+best.label;int id=f.contestId,revision=f.revision;note("human contest="+id+" revision="+revision+" phase="+f.phase+" round="+f.round+" card="+best.nativeCard+" label="+best.label);pageAction(label);var next=facts();check(next.kind==ContestSnapshot.Kind.NONE||next.contestId==id&&next.revision>revision,"normal actual button commits human progress once");
 }
 private int finishDebate(boolean poor,String label)throws Exception {
  int moves=0;while(SessionProbe.view(activity).contests.busy()&&moves++<400){var f=facts();if(f.waitingMercy){shot(label+"-mercy");pageAction("选择智力经验奖励");}else if(f.phase==9){shot(label+"-terminal");pageAction("结算原登用终局");}else legalCard(poor);}
  World w=SessionProbe.view(activity);check(!w.contests.busy(),"ordinary native campaign session released "+label);int won=person(w,222).owner==w.player?1:0;note("result="+w.contests.lastResult()+" won="+won);check(!w.contests.lastResult().isEmpty(),"real terminal campaign result "+label);shot(label+"-campaign-result");advance(label+"-nextturn");return won;
 }
 @Override public void onStart(){Bundle result=new Bundle();try {
  evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"debate-cold":"debate");evidence.mkdirs();put("output",evidence);activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
  if(coldMode){byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"debate/actual-mid.sg11").toPath());check(Arrays.equals(expected,capture()),"true cold auto-resume entire native model/campaign/all RNG");loadActualFrom(expected);nav("地图");shot("cold-native-human");finishDebate(false,"cold");result.putString("stream","PASS SESSION B DEBATE COLD full model/save/human terminal/nextturn; original admission/fee/abandon/diplomacy and ARM pending\n");}
  else {int wins=0,losses=0;for(int trial=0;trial<12&&(wins==0||losses==0);trial++){newSource(2);if(trial==0)enterDebate(true);enterDebate(false);legalCard(trial%2==1);saveActual("trial-"+trial+"-mid.sg11");loadActual("trial-"+trial+"-mid.sg11");nav("地图");int won=finishDebate(trial%2==1,"trial-"+trial);wins+=won;losses+=1-won;saveActual("trial-"+trial+"-finished.sg11");advance("after-terminal-save-"+trial);loadActual("trial-"+trial+"-finished.sg11");}
   check(wins>0&&losses>0,"actual ordinary human wins/losses without forced native result");newSource(2);enterDebate(false);legalCard(false);saveActual("actual-mid.sg11");nav("地图");shot("saved-native-human");result.putString("stream","PASS SESSION B DEBATE ordinary menu/recruit/cancel/human/win-loss/save/model/RNG/wholeturn; original admission/fee/abandon/diplomacy and ARM pending\n");}
  finish(Activity.RESULT_OK,result);
 }catch(Throwable t){try{shot("failure");note("FAIL "+t);}catch(Throwable ignored){}result.putString("stream","FAIL SESSION B DEBATE "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
 private void loadActualFrom(byte[] expected)throws Exception {nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"true cold manual whole model/campaign/all RNG");}
}
