package game.sanguo.mobile;
import game.sanguo.api.*;
import game.sanguo.core.*;
import android.os.SystemClock;
import java.nio.file.Files;
import java.io.File;
import java.util.*;

/** Extends frozen B's real menu/commands. Adds read-only actual A rendering/source checks. */
public class SessionAScenePresentationInstrumentation extends SessionBFieldworksInstrumentation {
 @Override public void finish(int code,android.os.Bundle result){
  try {
   // Frozen B calls Activity.finish(); its 350ms settle can end before autosave/onDestroy.
   // Wait for real normal destruction before starting a separate cold process.
   if(activity!=null&&activity.isFinishing()){
    long until=SystemClock.uptimeMillis()+30000;
    while(!activity.isDestroyed()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
    check(activity.isDestroyed(),"normal Activity actually destroyed before process termination");
    // Android marks destroyed before calling onDestroy; wait for the UI callback to return.
    runOnMainSync(()->{try{MapHost host=(MapHost)field(activity,"map");if(!(Boolean)field(host,"released")||field(host,"spatial")!=null)throw new AssertionError("actual native host teardown completed");}catch(Exception e){throw new RuntimeException(e);}});
    var preferences=getTargetContext().getSharedPreferences("map-renderer",0);
    note("normal exit nativeSession="+preferences.getBoolean("nativeSession",false)+" nativeFailure="+preferences.getBoolean("nativeFailure",false));
    if(result.getString("stream","").startsWith("PASS"))check(!preferences.getBoolean("nativeSession",false)&&!preferences.getBoolean("nativeFailure",false),"normal exit clears only incomplete healthy startup protection");
   }
  }catch(Throwable failure){result.putString("stream",result.getString("stream","")+"\nFAIL A orderly lifecycle "+android.util.Log.getStackTraceString(failure));}
  super.finish(code,result);
 }
 @Override protected void tap(android.view.View target,int presses)throws Exception {
  // A ListView settling after scroll may consume the first down as scroll-stop.
  // Wait for attached, focused, enabled and stable actual screen bounds first.
  android.graphics.Rect previous=new android.graphics.Rect(),bounds=new android.graphics.Rect();long stable=0,deadline=SystemClock.uptimeMillis()+12000;
  runOnMainSync(()->target.requestRectangleOnScreen(new android.graphics.Rect(0,0,target.getWidth(),target.getHeight()),true));
  while(SystemClock.uptimeMillis()<deadline){
   boolean[] visible={false};runOnMainSync(()->{visible[0]=target.isAttachedToWindow()&&target.hasWindowFocus()&&target.isEnabled()&&target.getGlobalVisibleRect(new android.graphics.Rect());int[] screen=new int[2];target.getLocationOnScreen(screen);bounds.set(screen[0],screen[1],screen[0]+target.getWidth(),screen[1]+target.getHeight());for(android.view.ViewParent parent=target.getParent();parent instanceof android.view.View;parent=parent.getParent()){android.view.View view=(android.view.View)parent;int[] at=new int[2];view.getLocationOnScreen(at);bounds.intersect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}});
   if(visible[0]&&!bounds.isEmpty()&&bounds.equals(previous)){if(stable==0)stable=SystemClock.uptimeMillis();if(SystemClock.uptimeMillis()-stable>=600)break;}else stable=0;
   previous.set(bounds);SystemClock.sleep(100);
  }
  check(stable!=0&&SystemClock.uptimeMillis()-stable>=600,"stable actual touch bounds "+target.getTag()+" "+bounds);
  pointer(bounds.centerX(),bounds.centerY(),presses);settle();
 }

 @Override protected void verifySceneFacts(String label)throws Exception {
  byte[] before=capture();StateToken prior=activity.deploymentState();
  nav("地图"); // Cold startup may restore a menu page; enter the actual map normally.
  super.verifySceneFacts(label);
  MapSceneSnapshot[] snap={null};SceneFactsSnapshot[] facts={null};World[] layout={null};boolean[] rendered={false};
  long deadline=SystemClock.uptimeMillis()+120000;
  while(SystemClock.uptimeMillis()<deadline){
   runOnMainSync(()->{try{
    MapHost map=(MapHost)field(activity,"map");snap[0]=(MapSceneSnapshot)field(map,"publishedSnapshot");facts[0]=(SceneFactsSnapshot)field(activity,"sceneFacts");layout[0]=SessionProbe.view(activity);FilamentMapView renderer=(FilamentMapView)field(map,"spatial");rendered[0]=renderer!=null&&(Boolean)field(renderer,"outputVerified")&&(Long)field(renderer,"renderedFrames")>2&&(Integer)field(renderer,"pending")==0&&!(Boolean)field(renderer,"assetSyncPending");
   }catch(Exception e){throw new RuntimeException(e);}});
   if(snap[0]!=null&&snap[0].authoritativeSceneFacts&&prior.equals(snap[0].state)&&rendered[0])break;
   SystemClock.sleep(100);
  }
  note("A map boundary label="+label+" rendered="+rendered[0]+" snapshotState="+(snap[0]==null?"absent":snap[0].state.sessionId+":"+snap[0].state.generation+":"+snap[0].state.revision)+" expected="+prior.sessionId+":"+prior.generation+":"+prior.revision);
  if(!rendered[0]){shot("a-map-unverified-"+label);runOnMainSync(()->{try{android.util.Log.w("SessionAScene",((MapHost)field(activity,"map")).report());}catch(Exception e){throw new RuntimeException(e);}});}
  check(snap[0]!=null&&snap[0].authoritativeSceneFacts&&prior.equals(snap[0].state)&&facts[0]!=null&&prior.equals(facts[0].state)&&rendered[0],"actual A map accepts exact full StateToken "+label);
  if(!facts[0].fires.isEmpty()){
   long[] priorFrames={0};runOnMainSync(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");if(effects!=null)priorFrames[0]=(Long)field(effects,"frames");}catch(Exception e){throw new RuntimeException(e);}});
   var first=facts[0].fires.get(0);runOnMainSync(()->activity.selectAndFocus(new Hex(first.cell.q,first.cell.r)));
   long nativeDeadline=SystemClock.uptimeMillis()+120000;String[] nativeReport={""};boolean[] ready={false};
   while(SystemClock.uptimeMillis()<nativeDeadline){runOnMainSync(()->{try{
    MapHost host=(MapHost)field(activity,"map");FilamentMapView renderer=(FilamentMapView)field(host,"spatial");PcMapEffects effects=(PcMapEffects)field(renderer,"pcMapEffects");
    ready[0]=effects!=null&&effects.fireScene&&(Long)field(effects,"frames")>priorFrames[0]+2&&(Integer)field(renderer,"pending")==0&&!(Boolean)field(renderer,"assetSyncPending")&&(Boolean)field(renderer,"outputVerified")&&(Integer)field(effects,"shown")>0&&((String)field(effects,"error")).isEmpty()&&((PcEffectProcess)field(effects,"process")).fireSummary().contains("active="+facts[0].fires.size()+" ")&&activity.deploymentState().equals(field(effects,"displayedFireState"))&&((PcCellFireSet)field(effects,"acceptedFires")).state.equals(activity.deploymentState());nativeReport[0]=host.report();
   }catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;SystemClock.sleep(100);}
   note("original fire actual native "+label+" "+nativeReport[0]);check(ready[0],"original source13 native actual map submitted "+label);shot("a-original-fire-"+label);
  }
  for(SceneFactsSnapshot.Fire fire:facts[0].fires)if(fire.remaining>0){
   MapSceneSnapshot.FireState shown=snap[0].fires.stream().filter(f->f.hex.equals(new Hex(fire.cell.q,fire.cell.r))).findFirst().orElseThrow();
   check(shown.remaining==fire.remaining&&shown.owner==fire.owner&&shown.power==fire.power&&shown.trap==fire.trap&&shown.sourceX==fire.cell.sourceX&&shown.sourceY==fire.cell.sourceY,"actual A source fire projection "+label);
  }
  for(SceneFactsSnapshot.Military f:facts[0].military){
   MapSceneSnapshot.Item item=snap[0].items.stream().filter(x->x.key.equals("structure:"+f.id)).findFirst().orElseThrow();
   check(item.facility.hp==f.hp&&item.facility.maxHp==f.maxHp&&item.facility.complete==f.complete&&item.facility.direction==f.direction&&item.facility.builderUnitId==f.builderUnitId,"actual A current military lifecycle "+f.id);
  }
  int profiles=0;
  for(var p:PcScenarioPeople.saved(layout[0])){
   PortraitMediaIdentity source=PortraitMediaSources.source(layout[0],p.officerId);if(source==null)continue;
   check(source.nativeId==p.nativeId&&source.recordSha.equals(p.recordSha)&&source.originalFields.equals(p.fields)&&Objects.equals(source.originalVoiceProfile,p.fields.get(48)),"actual A typed original voice source "+p.officerId);profiles++;
  }
  check(Arrays.equals(before,capture())&&prior.equals(activity.deploymentState()),"actual A presentation preserves full Save/bothRNG/StateToken "+label);
  Files.write(new File(evidence,"a-presentation-"+label+".txt").toPath(),("sameToken=true sourceVoiceJoins="+profiles+" fires="+snap[0].fires.size()+" facilities="+facts[0].military.size()+" originalFire13=runtime9-original-native-check originalSpeechCaller=unknown\n").getBytes("UTF-8"));
 }
}
