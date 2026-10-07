package game.sanguo.mobile;
import game.sanguo.api.*;
import game.sanguo.core.*;
import android.os.SystemClock;
import java.nio.file.Files;
import java.io.File;
import java.util.*;

/** Extends frozen B's real menu/commands. Adds read-only actual A rendering/source checks. */
public class SessionAScenePresentationInstrumentation extends SessionBFieldworksInstrumentation {
 @Override protected void selectUnit(int id)throws Exception {
  byte[] before=capture();StateToken prior=activity.deploymentState();
  World.Unit unit=SessionProbe.view(activity).unit(id);
  check(unit!=null,"real current unit exists before normal list selection");
  String commander=SessionProbe.view(activity).officer(unit.officerId).name;
  nav("全部部队");tap(await(v->v instanceof android.widget.Button&&"全部".contentEquals(((android.widget.Button)v).getText())));
  DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);
  tap(table.search);runOnMainSync(()->table.search.setText(commander));
  runOnMainSync(()->{
   android.view.inputmethod.InputConnection input=table.search.onCreateInputConnection(new android.view.inputmethod.EditorInfo());
   if(input==null||!input.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH))throw new AssertionError("normal unit search action unavailable");
  });
  android.view.View[] target={null};long until=SystemClock.uptimeMillis()+120000;
  while(SystemClock.uptimeMillis()<until){
   android.graphics.Rect viewport=new android.graphics.Rect();int[] index={-1},first={0};
   runOnMainSync(()->{
    first[0]=table.list.getFirstVisiblePosition();
    for(int i=0;i<table.list.getAdapter().getCount();i++)if(table.list.getAdapter().getItemId(i)==id){index[0]=i;break;}
    if(!table.list.getGlobalVisibleRect(viewport))return;
    int[] origin=new int[2];table.list.getRootView().getLocationOnScreen(origin);viewport.offset(origin[0],origin[1]);
    if(index[0]>=first[0]&&index[0]<first[0]+table.list.getChildCount()){
     android.view.View row=table.list.getChildAt(index[0]-first[0]);
     android.view.View cell=row instanceof android.view.ViewGroup?((android.view.ViewGroup)row).getChildAt(0):row;
     android.graphics.Rect visible=new android.graphics.Rect();
     if(cell.isAttachedToWindow()&&cell.getGlobalVisibleRect(visible)&&visible.height()>=cell.getHeight()*.8f)target[0]=cell;
    }
   });
   if(target[0]!=null)break;
   if(!viewport.isEmpty()&&index[0]>=0){
    float x=viewport.centerX(),start=viewport.top+viewport.height()*(index[0]>=first[0]?.65f:.35f);
    float dy=viewport.height()*(index[0]>=first[0]?-.35f:.35f);touchDrag(x,start,x,start+dy);
   }else SystemClock.sleep(150);
  }
  check(target[0]!=null,"actual attached source unit row reachable by normal search/scroll id="+id);
  tap(target[0]);long selectedUntil=SystemClock.uptimeMillis()+20000;boolean[] selected={false};
  while(SystemClock.uptimeMillis()<selectedUntil){runOnMainSync(()->{try{ClientState ui=(ClientState)field(activity,"ui");selected[0]=ui.selectedUnit==id&&ui.page.equals("map");}catch(Exception e){throw new RuntimeException(e);}});if(selected[0])break;SystemClock.sleep(100);}
  check(selected[0],"normal real list click selected exact current unit and entered map id="+id);
  check(Arrays.equals(before,capture())&&prior.equals(activity.deploymentState()),"normal list selection preserves full Save/bothRNG/StateToken");
 }
 private void touchDrag(float x,float y,float endX,float endY){
  long began=SystemClock.uptimeMillis();
  for(int i=0;i<=12;i++){
   android.view.MotionEvent event=android.view.MotionEvent.obtain(began,SystemClock.uptimeMillis(),i==0?android.view.MotionEvent.ACTION_DOWN:android.view.MotionEvent.ACTION_MOVE,x+(endX-x)*i/12f,y+(endY-y)*i/12f,0);
   event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(event);event.recycle();SystemClock.sleep(35);
  }
  android.view.MotionEvent up=android.view.MotionEvent.obtain(began,SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,endX,endY,0);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(up);up.recycle();settle();
 }
 private android.graphics.Rect freeMapRect()throws Exception {
  android.graphics.Rect free=new android.graphics.Rect();
  runOnMainSync(()->{try{
   MapHost host=(MapHost)field(activity,"map");int[] at=new int[2];host.getLocationOnScreen(at);
   free.set(at[0],at[1],at[0]+host.getWidth(),at[1]+host.getHeight());int margin=activity.dp(45);free.inset(margin,margin);
   for(String name:new String[]{"panelShell","commandDock"}){
    android.view.View panel=(android.view.View)field(activity,name);if(!panel.isShown())continue;
    panel.getLocationOnScreen(at);android.graphics.Rect bounds=new android.graphics.Rect(at[0]-margin,at[1]-margin,at[0]+panel.getWidth()+margin,at[1]+panel.getHeight()+margin);
    if(!android.graphics.Rect.intersects(free,bounds))continue;
    if(panel.getWidth()<host.getWidth()*.7f&&bounds.left>free.left)free.right=Math.min(free.right,bounds.left);
    else free.bottom=Math.min(free.bottom,bounds.top);
   }
   FilamentMapView view=(FilamentMapView)field(host,"spatial");
   if(view!=null&&(Boolean)field(host,"navigatorShown")){
    android.graphics.RectF mini=new android.graphics.RectF((android.graphics.RectF)field(field(view,"overlay"),"miniRect"));view.getLocationOnScreen(at);mini.offset(at[0],at[1]);
    if(mini.contains(free.centerX(),free.centerY()))free.right=Math.min(free.right,(int)mini.left-margin);
   }
  }catch(Exception e){throw new RuntimeException(e);}});return free;
 }
 private String targetDiagnostic(Hex target,float[] point)throws Exception {
  String[] diagnostic={""};runOnMainSync(()->{try{
   MapHost host=(MapHost)field(activity,"map");FilamentMapView view=(FilamentMapView)field(host,"spatial");MapSceneSnapshot snapshot=(MapSceneSnapshot)field(view,"snapshot");
   int[] origin=new int[2];view.getLocationOnScreen(origin);float x=point[0]-origin[0],y=point[1]-origin[1];
   android.graphics.RectF mini=(android.graphics.RectF)field(field(view,"overlay"),"miniRect");
   diagnostic[0]="target="+target+" screen="+java.util.Arrays.toString(point)+" actualGroundRay="+snapshot.ground.surface.pick(view.camera,x,y)
    +" minimapBlocked="+((Boolean)field(host,"navigatorShown")&&mini.contains(x,y))+" commandTargeting="+field(view,"commandTargeting");
  }catch(Exception e){throw new RuntimeException(e);}});return diagnostic[0];
 }
 @Override protected void tile(Hex target)throws Exception {
  byte[] before=capture();StateToken prior=activity.deploymentState();boolean admitted=false;
  for(int attempt=0;attempt<16;attempt++){
   float[] point=(float[])invoke("screenHex",new Class<?>[]{Hex.class},target);
   boolean visible=(Boolean)invoke("visibleMapPoint",new Class<?>[]{float[].class},point);
   admitted=(Boolean)invoke("routePoint",new Class<?>[]{Hex.class,float[].class},target,point);
   note("actual target admission attempt="+attempt+" visible="+visible+" exactRay="+admitted+" "+targetDiagnostic(target,point));
   if(admitted)break;
   android.graphics.Rect free=freeMapRect();check(!free.isEmpty(),"normal target adjustment has unoccluded map drag area");
   float dx=Math.max(-free.width()*.25f,Math.min(free.width()*.25f,free.centerX()-point[0]));
   float dy=Math.max(-free.height()*.25f,Math.min(free.height()*.25f,free.centerY()-point[1]));
   // A zero-length drag would be a target tap. Never inject it on a failed ray.
   check(Math.hypot(dx,dy)>activity.dp(12),"target cannot be admitted by normal pan; preserve exact ray failure evidence");
   touchDrag(free.centerX(),free.centerY(),free.centerX()+dx,free.centerY()+dy);
  }
  check(java.util.Arrays.equals(before,capture())&&prior.equals(activity.deploymentState()),"normal pre-target pan preserves full Save/bothRNG/StateToken");
  check(admitted,"normal pan admits exact target before actual pointer "+target);
  super.tile(target);
 }
 @Override protected void pose(Hex target)throws Exception {
  byte[] before=capture();StateToken prior=activity.deploymentState();
  // setItems dispatches the real row tap through its ListView; the text cell
  // itself is not a clickable Button. Use the inherited normal option tap.
  nav("地图");text("视图");option("定位");settle();
  boolean visible=false;
  for(int attempt=0;attempt<16;attempt++){
   float[] point=(float[])invoke("screenHex",new Class<?>[]{Hex.class},target);
   android.graphics.Rect viewport=freeMapRect();
   visible=!viewport.isEmpty()&&viewport.contains((int)point[0],(int)point[1])&&(Boolean)invoke("visibleMapPoint",new Class<?>[]{float[].class},point);
   if(visible)break;
   check(!viewport.isEmpty(),"actual visible map admits normal target pan");
   float dx=Math.max(-viewport.width()*.25f,Math.min(viewport.width()*.25f,viewport.centerX()-point[0]));
   float dy=Math.max(-viewport.height()*.25f,Math.min(viewport.height()*.25f,viewport.centerY()-point[1]));
   touchDrag(viewport.centerX(),viewport.centerY(),viewport.centerX()+dx,viewport.centerY()+dy);
  }
  check(visible,"normal selected-object focus and actual map drags expose command target "+target);
  check(Arrays.equals(before,capture())&&prior.equals(activity.deploymentState()),"normal view focus/pan preserves full Save/bothRNG/StateToken");
 }
 @Override protected void shot(String label)throws Exception {
  super.shot(label);
  org.json.JSONArray roots=new org.json.JSONArray();Throwable[] failure={null};
  runOnMainSync(()->{try{for(android.view.View root:android.view.inspector.WindowInspector.getGlobalWindowViews())if(root.isShown()&&root.hasWindowFocus())roots.put(SessionAUiReadabilityAudit.collect(root));}catch(Throwable e){failure[0]=e;}});
  if(failure[0]!=null)throw new IllegalStateException("Actual UI readability inventory",failure[0]);
  Files.write(new File(evidence,label+"-actual-text.json").toPath(),new org.json.JSONObject().put("normalScreenshot",label+".png").put("focusedRoots",roots).toString(2).getBytes("UTF-8"));
 }
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
