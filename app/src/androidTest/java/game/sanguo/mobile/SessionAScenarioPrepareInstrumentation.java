package game.sanguo.mobile;
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
/** Two actual source selections, normal cancels, allocator/phase observations.
 * 600s is diagnosis only; original120s host and120s rendered readiness stay strict. */
public final class SessionAScenarioPrepareInstrumentation extends SessionAScenePresentationInstrumentation {
 private String run;
 @Override public void onCreate(Bundle args){run=args.getString("run");super.onCreate(args);}
 @Override public void onStart(){Bundle result=new Bundle();JSONObject summary=new JSONObject();SessionAMemorySampler sampler=null;
 try{
  evidence=new File(getTargetContext().getExternalFilesDir("uiux"),run);check(evidence.mkdirs(),"fresh actual repeat preparation evidence");put("output",evidence);
  sampler=new SessionAMemorySampler(new File(evidence,"allocator-samples.csv"));
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
  byte[] before=capture();StateToken token=activity.deploymentState();PcScenarioCatalog.Source source=PcScenarioCatalog.all().get(14);JSONArray passes=new JSONArray();boolean functional=true;
  try(BufferedWriter writer=Files.newBufferedWriter(new File(evidence,"repeat-observations.jsonl").toPath(),java.nio.charset.StandardCharsets.UTF_8)){
   for(int pass=0;pass<2;pass++){
    nav("菜单");text("新游戏 / 选择势力");long began=SystemClock.elapsedRealtime();revealDescription("选择PC来源剧本 "+source.identity.path);
    long firstHost=-1,firstOutput=-1,lastSample=-5000;boolean thresholdShot=false,rendererThresholdShot=false;
    while(SystemClock.elapsedRealtime()-began<600000){
     long elapsed=SystemClock.elapsedRealtime()-began;JSONObject row=new JSONObject();MapHost[] found={null};boolean[] rendered={false};
     runOnMainSync(()->{try{
      UiReadTask task=(UiReadTask)field(activity,"uiReads");Future<?> job=(Future<?>)field(task,"job");row.put("pendingDialog",field(task,"pending")!=null).put("jobPresent",job!=null).put("jobDone",job!=null&&job.isDone()).put("jobCancelled",job!=null&&job.isCancelled()).put("generation",field(task,"generation"));
      for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){
       View v=visible(root,x->x instanceof MapHost&&x.getContentDescription()!=null&&x.getContentDescription().toString().startsWith("开局势力地图"));if(v!=null){found[0]=(MapHost)v;break;}
      }
      row.put("visibleFocusedPreview",found[0]!=null);
      if(found[0]!=null){World w=(World)field(found[0],"world");row.put("previewScenario",w.scenarioId);FilamentMapView f=(FilamentMapView)field(found[0],"spatial");row.put("spatialExists",f!=null);
       if(f!=null){boolean output=(Boolean)field(f,"outputVerified");int pending=(Integer)field(f,"pending");boolean assets=(Boolean)field(f,"assetSyncPending");long frames=(Long)field(f,"renderedFrames");
        row.put("outputVerified",output).put("pendingMeshes",pending).put("assetSyncPending",assets).put("renderedFrames",frames);rendered[0]=output&&pending==0&&!assets&&frames>2;
        row.put("rendererReport",f.startupReport());
       }
      }
     }catch(Exception e){throw new RuntimeException(e);}});
     if(found[0]!=null&&firstHost<0)firstHost=SystemClock.elapsedRealtime()-began;
     if(rendered[0]&&firstOutput<0)firstOutput=SystemClock.elapsedRealtime()-began;
     if(elapsed-lastSample>=5000||rendered[0]){
      JSONArray threads=new JSONArray();for(var entry:Thread.getAllStackTraces().entrySet())if(entry.getKey().getName().equals("ui-read")||entry.getKey().getName().equals("scene-cpu")){
       JSONArray stack=new JSONArray();var trace=entry.getValue();for(int i=0;i<Math.min(64,trace.length);i++)stack.put(trace[i].toString());threads.put(new JSONObject().put("name",entry.getKey().getName()).put("state",entry.getKey().getState().name()).put("stack",stack).put("stackTruncated",trace.length>64));
      }
      row.put("pass",pass).put("elapsedMillis",elapsed).put("workerThreads",threads);writer.write(row.toString());writer.newLine();writer.flush();lastSample=elapsed;
     }
     if(elapsed>=120000&&!thresholdShot){shot("pass-"+pass+"-host120s");thresholdShot=true;}
     if(firstHost>=0&&elapsed-firstHost>=120000&&!rendererThresholdShot){shot("pass-"+pass+"-renderer120s");rendererThresholdShot=true;}
     if(rendered[0]){check(source.identity.scenarioId.equals(row.getString("previewScenario")),"actual source14 exact identity");break;}
     SystemClock.sleep(100);
    }
    boolean pure=Arrays.equals(before,capture())&&token.equals(activity.deploymentState());check(pure,"actual preview leaves wholeSave/RNG/StateToken pure pass="+pass);
    boolean accepted=firstHost>=0&&firstHost<=120000&&firstOutput>=firstHost&&firstOutput-firstHost<=120000;functional&=accepted;
    passes.put(new JSONObject().put("pass",pass).put("firstFocusedPreviewMillis",firstHost).put("firstVerified3DMillis",firstOutput).put("functionalHostLimitMillis",120000).put("functionalRendererAfterHostLimitMillis",120000).put("functionalAccepted",accepted).put("fullSaveRngTokenPure",pure));
    shot("pass-"+pass+"-actual-end");if(firstHost>=0){text("返回");text("取消");}else{text("取消");check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"cancel pending read pure");break;}
    check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"normal cancel wholeSave/RNG/StateToken pure pass="+pass);
   }
  }
  summary.put("sourceIndex",14).put("sourcePath",source.identity.path).put("passes",passes).put("bothPassesObserved",passes.length()==2).put("functionalBothAccepted",functional&&passes.length()==2).put("observationOnlyLimitPerPassMillis",600000).put("fullSaveRngTokenPure",Arrays.equals(before,capture())&&token.equals(activity.deploymentState()));
  result.putString("stream",summary.getBoolean("functionalBothAccepted")?"UIUX PASS bounded two actual source14 preparation observations only\n":"UIUX FAIL actual repeated source14 exceeded120s host/renderer or incomplete; longer observation is diagnosis only\n");
 }catch(Throwable e){result.putString("stream","UIUX FAIL actual repeat diagnostic "+android.util.Log.getStackTraceString(e));try{shot("diagnostic-failed");}catch(Throwable ignored){}}
 finally{
  try{if(activity!=null)runOnMainSync(()->activity.finish());settle();}catch(Throwable e){result.putString("stream",result.getString("stream","")+"\nFAIL lifecycle "+e);}
  try{if(sampler!=null)sampler.close();}catch(Throwable e){result.putString("stream",result.getString("stream","")+"\nFAIL allocator observer "+e);}
 }
 try{Files.write(new File(evidence,"repeat-summary.json").toPath(),summary.toString(2).getBytes("UTF-8"));Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream","").getBytes("UTF-8"));}catch(Throwable ignored){}
 finish(Activity.RESULT_OK,result);
 }
}
