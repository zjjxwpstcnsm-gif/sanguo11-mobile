#!/usr/bin/env python3
"""Real source14 click and actual preparation state/stack/time; strict120s retained."""
from pathlib import Path
import json
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/prepare-diagnostic309'
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);name='app/src/androidTest/java/game/sanguo/mobile/SessionAScenarioPrepareInstrumentation.java';target=OUT/name;target.parent.mkdir(parents=True)
 source='''package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.inspector.WindowInspector;
import game.sanguo.core.*;
import game.sanguo.api.StateToken;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Future;
import org.json.*;
/** Actual menu click; observe longer to diagnose, never relax120s functional threshold. */
public final class SessionAScenarioPrepareInstrumentation extends SessionAScenePresentationInstrumentation {
 private String run;
 @Override public void onCreate(Bundle args){run=args.getString("run");super.onCreate(args);}
 @Override public void onStart(){Bundle result=new Bundle();JSONObject summary=new JSONObject();try{
  evidence=new File(getTargetContext().getExternalFilesDir("uiux"),run);check(evidence.mkdirs(),"fresh actual prepare diagnostic evidence");put("output",evidence);
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
  byte[] before=capture();StateToken token=activity.deploymentState();nav("菜单");text("新游戏 / 选择势力");PcScenarioCatalog.Source source=PcScenarioCatalog.all().get(14);
  long began=SystemClock.elapsedRealtime();revealDescription("选择PC来源剧本 "+source.identity.path);long firstHost=-1,firstOutput=-1,lastSample=-5000;boolean thresholdShot=false;
  try(BufferedWriter writer=Files.newBufferedWriter(new File(evidence,"prepare-observations.jsonl").toPath(),java.nio.charset.StandardCharsets.UTF_8)){
   while(SystemClock.elapsedRealtime()-began<600000){
    long elapsed=SystemClock.elapsedRealtime()-began;JSONObject row=new JSONObject();MapHost[] found={null};boolean[] rendered={false};
    runOnMainSync(()->{try{
     UiReadTask task=(UiReadTask)field(activity,"uiReads");Future<?> job=(Future<?>)field(task,"job");
     row.put("pendingDialog",field(task,"pending")!=null).put("jobPresent",job!=null).put("jobDone",job!=null&&job.isDone()).put("jobCancelled",job!=null&&job.isCancelled()).put("generation",field(task,"generation"));
     for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){
      View v=visible(root,x->x instanceof MapHost&&x.getContentDescription()!=null&&x.getContentDescription().toString().startsWith("开局势力地图"));
      if(v!=null){found[0]=(MapHost)v;break;}
     }
     row.put("visibleFocusedPreview",found[0]!=null);
     if(found[0]!=null){World w=(World)field(found[0],"world");row.put("previewScenario",w.scenarioId);FilamentMapView f=(FilamentMapView)field(found[0],"spatial");
      row.put("spatialExists",f!=null);if(f!=null){boolean output=(Boolean)field(f,"outputVerified");int pending=(Integer)field(f,"pending");boolean assets=(Boolean)field(f,"assetSyncPending");row.put("outputVerified",output).put("pendingMeshes",pending).put("assetSyncPending",assets);rendered[0]=output&&pending==0&&!assets;}
     }
    }catch(Exception e){throw new RuntimeException(e);}});
    if(found[0]!=null&&firstHost<0)firstHost=SystemClock.elapsedRealtime()-began;
    if(rendered[0]&&firstOutput<0)firstOutput=SystemClock.elapsedRealtime()-began;
    if(elapsed-lastSample>=5000||rendered[0]){
     JSONArray threads=new JSONArray();for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet())if(entry.getKey().getName().equals("ui-read")){
      JSONArray stack=new JSONArray();StackTraceElement[] trace=entry.getValue();for(int i=0;i<Math.min(64,trace.length);i++)stack.put(trace[i].toString());threads.put(new JSONObject().put("name",entry.getKey().getName()).put("state",entry.getKey().getState().name()).put("stack",stack).put("stackTruncated",trace.length>64));
     }
     Runtime rt=Runtime.getRuntime();row.put("elapsedMillis",elapsed).put("uiReadThreads",threads).put("javaUsedBytes",rt.totalMemory()-rt.freeMemory()).put("javaLimitBytes",rt.maxMemory());writer.write(row.toString());writer.newLine();writer.flush();lastSample=elapsed;
    }
    if(elapsed>=120000&&!thresholdShot){shot("strict-120s-preparation");thresholdShot=true;}
    if(rendered[0]){check(source.identity.scenarioId.equals(row.getString("previewScenario")),"actual preview exact clicked source14");break;}
    SystemClock.sleep(100);
   }
  }
  boolean pure=Arrays.equals(before,capture())&&token.equals(activity.deploymentState());check(pure,"real source preview read leaves entire Save/RNG/StateToken unchanged");
  summary.put("sourceIndex",14).put("sourcePath",source.identity.path).put("firstFocusedPreviewMillis",firstHost).put("firstVerified3DMillis",firstOutput).put("functionalHostLimitMillis",120000).put("observationOnlyLimitMillis",600000).put("fullSaveRngTokenPure",pure).put("functionalSource14Accepted",firstHost>=0&&firstHost<=120000&&firstOutput>=0).put("observationComplete",true);
  shot("actual-preparation-end");if(firstHost>=0){text("返回");text("取消");}else text("取消");check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"normal actual cancel leaves entire Save/RNG/StateToken unchanged");
  result.putString("stream",summary.getBoolean("functionalSource14Accepted")?"UIUX PASS source14 actual preview preparation diagnostic; bounded scope only\\n":"UIUX FAIL source14 preview exceeded120s or never rendered; longer observation collected only\\n");
 }catch(Throwable e){result.putString("stream","UIUX FAIL source preparation diagnostic "+android.util.Log.getStackTraceString(e));try{shot("diagnostic-failed");}catch(Throwable ignored){}}
 finally{try{if(activity!=null)runOnMainSync(()->activity.finish());settle();}catch(Throwable e){result.putString("stream",result.getString("stream","")+"\\nFAIL lifecycle "+e);}}
 try{Files.write(new File(evidence,"prepare-summary.json").toPath(),summary.toString(2).getBytes("UTF-8"));Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream","").getBytes("UTF-8"));}catch(Throwable ignored){}
 finish(Activity.RESULT_OK,result);
 }
}
'''
 target.write_text(source);base=ROOT/'out/session-a/fire-cache-apk296/source';name2='app/src/androidTest/AndroidManifest.xml';manifest=(base/name2).read_text();needle='</manifest>';assert manifest.count(needle)==1;manifest=manifest.replace(needle,'    <instrumentation android:name="game.sanguo.mobile.SessionAScenarioPrepareInstrumentation" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />\n'+needle);other=OUT/name2;other.parent.mkdir(parents=True,exist_ok=True);other.write_text(manifest)
 assert not (ROOT/name).exists();report={'paths':[{'path':name,'beforeSha256':None,'afterSha256':sha(target),'stagedPath':str(target)},{'path':name2,'beforeSha256':sha(base/name2),'afterSha256':sha(other),'stagedPath':str(other)}],'currentApkOrCanonicalUnchanged':True,'functionalThresholdRelaxed':False,'actualInstalled':False,'scope':'Fresh diagnostic test only: actual menu source14 pointer, UIReadTask pending/job/done/generation, exact ui-read stacks every5s, actual focused preview/current source and verified output times, full SaveRNGToken before/after/cancel.600s observation can identify slow completion but >120s host remains functionalFAIL. Stack/screenshot/measurement overhead recorded; no World/snapshot/command/RNG injection, original rule/renderer code unchanged.','wholeGoalComplete':False};(DOC/'PREPARE_DIAGNOSTIC309.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'stagedPaths':[x['path'] for x in report['paths']],'actualInstalled':False}),flush=True)
if __name__=='__main__':main()
