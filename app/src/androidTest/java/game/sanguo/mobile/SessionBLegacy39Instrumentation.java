package game.sanguo.mobile;
import android.app.*;import android.content.Intent;import android.os.*;import android.view.*;import android.widget.*;import game.sanguo.core.*;import game.sanguo.api.*;import java.io.*;import java.nio.file.Files;import java.util.*;
/** Genuine saved39 normal menu/human/terminal/cold acceptance only.
 * Read-only model inspection; no gameplay result, RNG or source mutation. */
public final class SessionBLegacy39Instrumentation extends SessionBFieldworksInstrumentation {
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
 private void pageAction(String s)throws Exception {runOnMainSync(()->{View hit=search(activity.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().startsWith(s));if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);});settle();text(s);}
 private void saveActual(String name)throws Exception {byte[] saved=capture();Files.write(new File(evidence,name).toPath(),saved);boolean existed=new File(activity.getFilesDir(),"manual3.sg11").exists();nav("菜单");text("保存局面");preparedOption("槽位 3");if(existed)text("覆盖存档");check(Arrays.equals(saved,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"ordinary fullWorld/bothRNG saved "+name);}
 private void loadActual(String name)throws Exception {byte[] expected=Files.readAllBytes(new File(evidence,name).toPath());nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"ordinary manual load fullWorld/bothRNG "+name);}
 private ContestSnapshot facts(){ContestSnapshot[] value={null};runOnMainSync(()->value[0]=activity.contestSnapshot());return value[0];}
 private void legalCard(boolean poor)throws Exception {
  var f=facts();check(f.waitingCard,"actual human native card boundary");ContestSnapshot.Card best=null;int score=poor?Integer.MAX_VALUE:Integer.MIN_VALUE;
  for(var c:f.cards)if(c.enabled()&&c.nativeCard!=0){int n=c.nativeCard,value=n==12?120:n==10?110:n==11?20:n==13||n==14?0:(n-1)%3+1;if(best==null||(poor?value<score:value>score)){best=c;score=value;}}
  if(best==null)for(var c:f.cards)if(c.enabled()){best=c;break;}check(best!=null,"actual enabled legal card");String label=(best.nativeCard==0?"再考 · ":"出牌 · ")+best.label;int id=f.contestId,revision=f.revision;note("human contest="+id+" revision="+revision+" phase="+f.phase+" round="+f.round+" card="+best.nativeCard+" label="+best.label);pageAction(label);var next=facts();check(next.kind==ContestSnapshot.Kind.NONE||next.contestId==id&&next.revision>revision,"normal actual button commits human progress once");
 }
 private int finishDebate(boolean poor,String label)throws Exception {
  int targetId=facts().speakers.get(1).officerId;
  World start=SessionProbe.view(activity);int actorId=facts().speakers.get(0).officerId,cityId=start.officer(actorId).cityId,ap=start.actionPoints[start.player],goldBefore=start.city(cityId).gold;
  int moves=0;while(SessionProbe.view(activity).contests.busy()&&moves++<400){var f=facts();if(f.waitingMercy){shot(label+"-mercy");pageAction("选择智力经验奖励");}else if(f.phase==9){shot(label+"-terminal");pageAction("结算原登用终局");}else legalCard(poor);}
  World w=SessionProbe.view(activity);check(!w.contests.busy(),"ordinary native campaign session released "+label);int won=w.officer(targetId).owner==w.player?1:0;note("result="+w.contests.lastResult()+" won="+won);check(!w.contests.lastResult().isEmpty(),"real terminal campaign result "+label);shot(label+"-campaign-result");advance(label+"-nextturn");return won;
 }
 private byte[] nativeContestBytes(World w)throws Exception {
  Object contest=field(w.contests.current(),"nativeDebate");java.lang.reflect.Method method=contest.getClass().getDeclaredMethod("write",DataOutputStream.class);method.setAccessible(true);ByteArrayOutputStream bytes=new ByteArrayOutputStream();method.invoke(contest,new DataOutputStream(bytes));return bytes.toByteArray();
 }
 private void verifyLegacyMap(String label)throws Exception {
  byte[]before=capture();MapHost host=(MapHost)field(activity,"map");if(!host.is3D())pageAction("重试3D地图");long deadline=SystemClock.elapsedRealtime()+300000;boolean verified=false;
  while(SystemClock.elapsedRealtime()<deadline){boolean[]ready={false};runOnMainSync(()->{try{Object view=field(host,"spatial");ready[0]=view!=null&&(Boolean)field(view,"outputVerified");}catch(Exception e){throw new RuntimeException(e);}});if(ready[0]){verified=true;break;}SystemClock.sleep(300);}
  check(verified,"real3D nonuniform Surface output after ordinary retry "+label);check(Arrays.equals(before,capture()),"existing A renderer/metadata consume no World or gameplay RNG "+label);shot("legacy39-verified-map-"+label);
 }
 private void legacyFlow(Bundle result)throws Exception {
  File warm=new File(getTargetContext().getExternalFilesDir("session-b"),"legacy39");
  if(coldMode){byte[]mid=Files.readAllBytes(new File(warm,"actual-mid.sg11").toPath());check(Arrays.equals(mid,capture()),"genuine39 adopted true process cold fullWorld/native model/allRNG");loadActualFrom(mid);nav("地图");verifyLegacyMap("cold");finishDebate(false,"legacy39-cold");advance("legacy39-cold-turn2");advance("legacy39-cold-turn3");byte[]done=capture();Files.write(new File(evidence,"finished.sg11").toPath(),done);check(Arrays.equals(done,Files.readAllBytes(new File(warm,"finished.sg11").toPath())),"same genuine39 checkpoint cold replay fullWorld/allRNG exact");result.putString("stream","PASS SESSION B LEGACY39 COLD genuine saved native model/ordinary load/human/once terminal/three wholeturns/exact warm replay; original concession/diplomacy/ARM pending\n");return;}
  byte[]original;try(InputStream in=getContext().getAssets().open("session-b/legacy39-original.sg11")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[]block=new byte[8192];int count;while((count=in.read(block))!=-1){if(bytes.size()+count>2139833)throw new IOException("Genuine39 fixture length exceeds pinned source");bytes.write(block,0,count);}original=bytes.toByteArray();}
  check(original.length==2139833,"genuine39 original fixture length");World decoded=SaveCodec.decode(original);byte[]expected=SaveCodec.encode(decoded);check(!decoded.contests.nativeCampaignSettlementEnabled()&&decoded.contests.busy(),"real prototype39 requires explicit adoption");Files.write(new File(activity.getFilesDir(),"manual3.sg11").toPath(),original);loadActualFrom(expected);nav("地图");verifyLegacyMap("original");shot("legacy39-original-loaded");
  World before=SessionProbe.view(activity);byte[]beforeSave=capture(),model=nativeContestBytes(before),nativeRng=before.extensions.get("pc-native-debate-effects-v1");long strategy=before.strategy.getRandomState(),life=(Long)field(before.life,"randomState");
  pageAction("旧原舌战 · 明确采用已核实终局策略");shot("legacy39-adoption-cancel");text("取消");check(Arrays.equals(beforeSave,capture()),"genuine39 adoption preview/cancel wholeWorld/allRNG unchanged");
  pageAction("旧原舌战 · 明确采用已核实终局策略");text("执行");World adopted=SessionProbe.view(activity);check(adopted.contests.nativeCampaignSettlementEnabled(),"actual explicit adoption button committed");check(Arrays.equals(model,nativeContestBytes(adopted))&&Arrays.equals(nativeRng,adopted.extensions.get("pc-native-debate-effects-v1"))&&strategy==adopted.strategy.getRandomState()&&life==(Long)field(adopted.life,"randomState"),"actual adoption preserves complete native contest and allRNG");legalCard(false);saveActual("actual-mid.sg11");loadActual("actual-mid.sg11");nav("地图");shot("legacy39-adopted-saved-human");finishDebate(false,"legacy39-warm");advance("legacy39-warm-turn2");advance("legacy39-warm-turn3");Files.write(new File(evidence,"finished.sg11").toPath(),capture());loadActual("actual-mid.sg11");nav("地图");result.putString("stream","PASS SESSION B LEGACY39 genuine original save/menu load/explicit adoption/cancel/human/once terminal/three wholeturns/fullmodel-allRNG/manual load; original concession/diplomacy/ARM pending\n");
 }
 private void loadActualFrom(byte[] expected)throws Exception {nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"true cold manual whole model/campaign/all RNG");}
 @Override public void onStart(){Bundle result=new Bundle();try{
  evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"legacy39-cold":"legacy39");evidence.mkdirs();put("output",evidence);activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));legacyFlow(result);finish(Activity.RESULT_OK,result);
 }catch(Throwable t){try{shot("failure");note("FAIL "+t);}catch(Throwable ignored){}result.putString("stream","FAIL SESSION B LEGACY39 "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}}
}
