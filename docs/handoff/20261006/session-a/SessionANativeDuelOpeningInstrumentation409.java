package game.sanguo.mobile;
import android.app.*;import android.content.Intent;import android.os.*;import android.view.View;import android.widget.*;
import game.sanguo.core.*;import game.sanguo.api.*;
import java.io.*;import java.nio.file.Files;import java.util.*;

/** Actual menu draft/cancel/new game/save/read/cold; never edits World/RNG. */
public final class SessionANativeDuelOpeningInstrumentation extends SessionAScenePresentationInstrumentation {
 private void pure(byte[] before,StateToken token,String label)throws Exception{check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"full Save/allRNG/Token pure "+label);}
 private void settingsChoice(String group,int value)throws Exception{description("新局设置 "+group+" "+value+" ");}
 private void inspect(int life,int death,int difficulty)throws Exception{
  byte[] before=capture();StateToken token=activity.deploymentState();World w=SessionProbe.view(activity);
  var options=PcDuelOptions.current(w);var source=PcSourceOpeningOptions.saved(w);
  check(options!=null&&options.life==life&&options.death==death&&options.difficulty==difficulty,"normal persisted effective source options");
  check(source!=null&&source.requested.death==death&&source.requested.difficulty==difficulty,"normal persisted exact requested source options");
  GameApi api=((NativeGameHost)field(activity,"gameHost")).session();var facts=api.pcOpeningOptions();
  check(facts.supported&&facts.state.equals(token)&&facts.hasSavedValues()&&!facts.defaultsKnown,"normal saved immutable options same Token and no defaults");
  pure(before,token,"source option inspection");
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"fieldworks-cold":"fieldworks");evidence.mkdirs();put("output",evidence);
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);
  awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
  if(coldMode){
   byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"fieldworks/actual-build.sg11").toPath());check(Arrays.equals(expected,capture()),"new process automatic complete option Save/allRNG read");inspect(3,2,2);
   nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"new process manual complete option Save/allRNG read");inspect(3,2,2);verifySceneFacts("cold-explicit-options");shot("cold-explicit-options");
   result.putString("stream","PASS SESSION A OPENING COLD actual auto/manual fullSave/allRNG/options; ARM pending\n");
  }else{
   for(int sourceIndex:new int[]{0,14}){
    byte[] before=capture();StateToken token=activity.deploymentState();
    nav("菜单");text("新游戏 / 选择势力");var source=PcScenarioCatalog.all().get(sourceIndex);
    revealDescription("选择PC来源剧本 "+source.identity.path);awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));
    description("选择原新局设置");View apply=await(v->v instanceof Button&&"采用设置".contentEquals(((Button)v).getText()));check(!apply.isEnabled(),"no original GUI defaults silently selected");
    settingsChoice("difficulty",sourceIndex==0?0:2);settingsChoice("death",sourceIndex==0?0:2);
    if(sourceIndex==0)settingsChoice("life",0);
    else{View fixed=await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("新局设置 life 0 "));check(!fixed.isEnabled(),"real fixed source constraint disables incompatible lifespan");}
    shot("source-"+sourceIndex+"-settings");text("返回选择");pure(before,token,"cancel settings "+sourceIndex);
    description("选择原新局设置");check(!await(v->v instanceof Button&&"采用设置".contentEquals(((Button)v).getText())).isEnabled(),"cancel does not accept draft choices");
    settingsChoice("difficulty",sourceIndex==0?0:2);settingsChoice("death",sourceIndex==0?0:2);if(sourceIndex==0)settingsChoice("life",0);text("采用设置");pure(before,token,"accept local settings draft "+sourceIndex);
    description("确认开局势力");shot("source-"+sourceIndex+"-confirmation");text("返回选择");pure(before,token,"cancel final new game confirmation "+sourceIndex);
    description("确认开局势力");text("开始新局");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
    inspect(sourceIndex==0?0:3,sourceIndex==0?0:2,sourceIndex==0?0:2);verifySceneFacts("explicit-new-source-"+sourceIndex);shot("source-"+sourceIndex+"-map");
    Files.write(new File(evidence,"source-"+sourceIndex+"-explicit.sg11").toPath(),capture());
   }
   byte[] expected=capture();File slot=new File(activity.getFilesDir(),"manual3.sg11");boolean existed=slot.exists();nav("菜单");text("保存局面");preparedOption("槽位 3");if(existed)text("覆盖存档");
   long until=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<until&&(!slot.exists()||!Arrays.equals(expected,Files.readAllBytes(slot.toPath()))))SystemClock.sleep(100);check(slot.exists()&&Arrays.equals(expected,Files.readAllBytes(slot.toPath())),"normal entire option Save/allRNG manual write");
   nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(expected,capture()),"normal option save full readback");inspect(3,2,2);Files.write(new File(evidence,"actual-build.sg11").toPath(),capture());
   result.putString("stream","PASS SESSION A OPENING normal actual source0/source14/draft/cancel/fixed/explicit-new/fullsave/read; cold/ARM pending\n");
  }
 }catch(Throwable failure){result.putString("stream","FAIL SESSION A OPENING "+android.util.Log.getStackTraceString(failure));try{shot("failed");}catch(Throwable ignored){}}
 finally{try{if(activity!=null)runOnMainSync(()->activity.finish());settle();}catch(Throwable failure){result.putString("stream",result.getString("stream","")+"\nFAIL lifecycle "+failure);}}
 try{Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream","").getBytes("UTF-8"));}catch(Exception ignored){}finish(Activity.RESULT_OK,result);
 }
}
