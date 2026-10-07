package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.StateToken;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

/** Session A normal-menu source/preview/new-game/gesture/lifecycle acceptance.
 * No constructed World, snapshot, state injection or direct rule command.
 * Reflection is read-only; all scenario/faction actions use injected touches.
 */
public final class SessionAMapRepairInstrumentation extends Instrumentation {
    private MainActivity activity; private File output; private int checks;
    private StringBuilder log=new StringBuilder(), memory=new StringBuilder("phase,javaUsed,javaTotal,javaLimit,nativeAllocated,totalPss,graphicsPss\n");
    private String run,expectedStartupSha; private int begin,end,portraitNative=-1,coldPortraitSource=-1; private boolean heapProfile,heapDumped,portraitPixels,allFactionPreviews,allPortraitCallers,fastPreviewCancellation;
    @Override public void onCreate(Bundle args){super.onCreate(args);expectedStartupSha=args.getString("expectedStartupSha","");if(!expectedStartupSha.isEmpty()&&!expectedStartupSha.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid expected save SHA");fastPreviewCancellation="fastPreview16".equals(args.getString("suite",""));allFactionPreviews="factions16".equals(args.getString("suite",""));allPortraitCallers="mediaAll16".equals(args.getString("suite",""));portraitPixels="media16".equals(args.getString("suite",""))||allPortraitCallers;heapProfile="true".equals(args.getString("heapProfile","false"));run=args.getString("run","session_a_map");begin=Integer.parseInt(args.getString("begin","0"));end=Integer.parseInt(args.getString("end","16"));portraitNative=Integer.parseInt(args.getString("portraitNative","-1"));coldPortraitSource=Integer.parseInt(args.getString("coldPortraitSource","-1"));if(!run.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException();start();}
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);if(a instanceof MainActivity)activity=(MainActivity)a;}
    private void check(boolean condition,String message){checks++;log.append(condition?"PASS ":"FAIL ").append(message).append('\n');if(!condition)throw new AssertionError(message);}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private View find(View v,Predicate<View> test){if(!v.isShown())return null;if(test.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View x=find(((ViewGroup)v).getChildAt(i),test);if(x!=null)return x;}return null;}
    private View await(Predicate<View> test){long deadline=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<deadline){View[] found={null};ui(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--)if(roots.get(i).hasWindowFocus()&&(found[0]=find(roots.get(i),test))!=null)break;});if(found[0]!=null)return found[0];SystemClock.sleep(100);}throw new AssertionError("Normal widget unavailable");}
    private View desc(String value){return await(v->value.equals(v.getContentDescription()==null?"":v.getContentDescription().toString()));}
    private void tap(View view){Rect rect=new Rect();ui(()->view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true));SystemClock.sleep(200);ui(()->{check(view.getGlobalVisibleRect(rect)&&rect.width()>0&&rect.height()>0,"control reachable "+view.getContentDescription());int[] loc=new int[2];view.getRootView().getLocationOnScreen(loc);rect.offset(loc[0],loc[1]);});long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,rect.centerX(),rect.centerY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"pointer accepted");e.recycle();}SystemClock.sleep(200);}
    private void longPressPortrait(TextView cell,int nativeId)throws Exception {
        Rect rect=new Rect();
        // Use the presently attached row and actual screen location; a recycled
        // TextView can report a local rectangle even after leaving the window.
        ui(()->{
            boolean attached=cell.isAttachedToWindow(),focus=cell.getRootView().hasWindowFocus();
            int[] location=new int[2];cell.getLocationOnScreen(location);
            Rect clipped=new Rect();boolean visible=cell.getGlobalVisibleRect(clipped);
            int[] origin=new int[2];cell.getRootView().getLocationOnScreen(origin);clipped.offset(origin[0],origin[1]);
            rect.set(location[0],location[1],location[0]+cell.getWidth(),location[1]+cell.getHeight());
            log.append("ACTUAL longpress target native=").append(nativeId).append(" attached=").append(attached).append(" focus=").append(focus).append(" screen=").append(rect.flattenToString()).append(" clipped=").append(clipped.flattenToString()).append(" root=").append(cell.getRootView().getClass().getName()).append('\n');
            check(attached&&focus&&cell.isShown()&&visible&&rect.intersect(clipped)&&!rect.isEmpty(),"actual attached current portrait long-press reachable "+nativeId);
        });
        long down=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,rect.centerX(),rect.centerY(),0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"actual portrait long-press down "+nativeId);event.recycle();
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);
        event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,rect.centerX(),rect.centerY(),0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"actual portrait long-press up "+nativeId);event.recycle();SystemClock.sleep(400);
    }
    private void text(String s){tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(s)&&(v.isClickable()||v.getParent() instanceof AdapterView)));}
    private void nav(String page){
        tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航")));tap(desc("导航 · "+page));
        String expected=page.equals("地图")?"map":page.equals("武将")?"officers":page.equals("菜单")?"menu":null;
        if(expected==null)throw new IllegalArgumentException("Unexamined navigation target "+page);
        ui(()->{try{check(expected.equals(((ClientState)field(activity,"ui")).page),"actual navigation page matches touched target "+page);}catch(Exception error){throw new IllegalStateException(error);}});
    }
    private byte[] capture(){byte[][] bytes={null};ui(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new IllegalStateException(e);}});return bytes[0];}
    private StateToken token(){StateToken[] t={null};ui(()->t[0]=activity.deploymentState());return t[0];}
    private void unchanged(byte[] save,StateToken token,String phase){check(Arrays.equals(save,capture()),phase+" complete Save/RNG byte equal");check(token.equals(token()),phase+" full StateToken equal");}
    private MapHost host(boolean preview){return (MapHost)await(v->v instanceof MapHost&&(!preview||v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("开局势力地图")));}
    private void ready(MapHost host)throws Exception{long deadline=SystemClock.uptimeMillis()+120000;boolean[] good={false};while(SystemClock.uptimeMillis()<deadline){ui(()->{try{FilamentMapView view=(FilamentMapView)field(host,"spatial");good[0]=view!=null&&!(Boolean)field(view,"released")&&(Boolean)field(view,"outputVerified")&&(Long)field(view,"renderedFrames")>2&&(Integer)field(view,"pending")==0&&!(Boolean)field(view,"assetSyncPending");}catch(Exception e){throw new IllegalStateException(e);}});if(good[0])break;SystemClock.sleep(100);}check(good[0],"actual3D verified output and completed terrain, not renderer recovery");sample("ready");String[] report={null};ui(()->report[0]=host.report());try(FileOutputStream f=new FileOutputStream(new File(output,"renderer-timeline.txt"),true)){f.write(("\nCHECK "+checks+"\n"+report[0]+"\n").getBytes("UTF-8"));}}
    private void sample(String phase)throws Exception{Runtime rt=Runtime.getRuntime();Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);memory.append(phase).append(',').append(rt.totalMemory()-rt.freeMemory()).append(',').append(rt.totalMemory()).append(',').append(rt.maxMemory()).append(',').append(Debug.getNativeHeapAllocatedSize()).append(',').append(m.getTotalPss()).append(',').append(m.getMemoryStat("summary.graphics")).append('\n');Files.write(new File(output,"memory.csv").toPath(),memory.toString().getBytes("UTF-8"));if(heapProfile&&!heapDumped&&rt.totalMemory()-rt.freeMemory()>340L*1024*1024){heapDumped=true;long started=SystemClock.elapsedRealtime();Debug.dumpHprofData(new File(output,"high-heap.hprof").getAbsolutePath());Files.write(new File(output,"heap-profile.txt").toPath(),("Diagnostic only; dump requests GC, not acceptance memory peak. phase="+phase+" elapsedMs="+(SystemClock.elapsedRealtime()-started)+"\n"+Debug.getRuntimeStats()).getBytes("UTF-8"));}}
    private void shot(String name)throws Exception {
        SystemClock.sleep(400);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        check(bitmap!=null,"screen captured");
        try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){
            bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
        }finally{bitmap.recycle();}
        org.json.JSONArray roots=new org.json.JSONArray();Throwable[] failure={null};
        ui(()->{
            try{
                for(View root:WindowInspector.getGlobalWindowViews())
                    if(root.isShown()&&root.hasWindowFocus())roots.put(SessionAUiReadabilityAudit.collect(root));
            }catch(Throwable error){failure[0]=error;}
        });
        if(failure[0]!=null)throw new IllegalStateException("Actual normal map/media text inventory",failure[0]);
        Files.write(new File(output,name+"-actual-text.json").toPath(),new org.json.JSONObject()
            .put("normalScreenshot",name+".png").put("focusedRoots",roots).toString(2).getBytes("UTF-8"));
    }
    private void pointer(long down,int action,float...xy){MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[xy.length/2];MotionEvent.PointerCoords[] c=new MotionEvent.PointerCoords[p.length];for(int i=0;i<p.length;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=i;p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;c[i]=new MotionEvent.PointerCoords();c[i].x=xy[2*i];c[i].y=xy[2*i+1];c[i].pressure=1;c[i].size=1;}MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p.length,p,c,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);if(!getUiAutomation().injectInputEvent(e,true))throw new AssertionError("Gesture rejected");e.recycle();SystemClock.sleep(35);}
    private void zoom(MapHost host)throws Exception{Rect r=new Rect();ui(()->{host.getGlobalVisibleRect(r);int[] loc=new int[2];host.getRootView().getLocationOnScreen(loc);r.offset(loc[0],loc[1]);});float before=span(host);int resource=activity.getResources().getIdentifier("config_minScalingSpan","dimen","android");int minimum=Build.VERSION.SDK_INT>=29?ViewConfiguration.get(activity).getScaledMinimumScalingSpan():activity.getResources().getDimensionPixelSize(resource);float x=r.centerX(),y=r.top+r.height()*.42f,start=Math.max(r.width()*.14f,minimum*.55f),target=Math.min(r.width()*.44f,start*2),last=start;check(target>start,"real gesture crosses minimum scaling span "+minimum);long down=SystemClock.uptimeMillis();pointer(down,0,x-start,y);pointer(down,5|(1<<8),x-start,y,x+start,y);for(int i=1;i<=12;i++){last=start+(target-start)*i/12f;pointer(down,2,x-last,y,x+last,y);}pointer(down,6|(1<<8),x-last,y,x+last,y);pointer(down,1,x-last,y);SystemClock.sleep(350);float after=span(host);check(after<before*.95f,"actual gesture changes camera span "+before+" -> "+after);sample("zoom");}
    private float span(MapHost host){float[] value={0};ui(()->{try{value[0]=((FilamentMapView)field(host,"spatial")).camera.span;}catch(Exception e){throw new IllegalStateException(e);}});return value[0];}
    private void closeView(MapHost host)throws Exception{float full=span(host);for(int i=0;i<8&&span(host)>10;i++)zoom(host);check(span(host)<=10&&span(host)<full*.5f,"real full-to-near camera "+full+" -> "+span(host));}
    private void pan(MapHost host)throws Exception{Rect r=new Rect();ui(()->host.getGlobalVisibleRect(r));long down=SystemClock.uptimeMillis();for(int i=0;i<10;i++)pointer(down,i==0?0:2,r.centerX()+i*4,r.top+r.height()*.45f+i*2);pointer(down,1,r.centerX()+36,r.top+r.height()*.45f+18);SystemClock.sleep(200);sample("pan");}
    private void normalFit(){text("视图");text("全图");}
    private void allFactionPreviews(int source,MapHost preview,List<Button> chips,byte[] before,StateToken prior)throws Exception{
        org.json.JSONArray rows=new org.json.JSONArray();int available=0;
        for(int side=0;side<chips.size();side++){
            Button chip=chips.get(side);org.json.JSONObject row=new org.json.JSONObject().put("side",side).put("label",chip.getText()).put("enabled",chip.isEnabled()).put("textArgb",Integer.toUnsignedString(chip.getCurrentTextColor()));
            if(chip.isEnabled()){
                tap(chip);ready(preview);int expected=side;ui(()->{try{ScenarioFactionPicker picker=(ScenarioFactionPicker)field(activity,"factionPicker");check((Integer)field(picker,"selected")==expected&&(Integer)field(preview,"previewFaction")==expected&&chip.isSelected(),"actual selected original faction and map preview "+source+":"+expected);}catch(Exception e){throw new IllegalStateException(e);}});
                View summary=await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("已选势力 ·"));row.put("actualSummary",summary.getContentDescription());unchanged(before,prior,"every selectable source force preview "+source+":"+side);shot("source-"+source+"-faction-"+side+"-preview");row.put("actual3DVerified",true);available++;
            }else row.put("notInvokedReason","actual source picker disabled; no scope/availability override");
            rows.put(row);
        }
        check(available>0,"normal source has selectable original force "+source);Files.write(new File(output,"source-"+source+"-faction-previews.json").toPath(),new org.json.JSONObject().put("sourceIndex",source).put("actualEnabledCount",available).put("slots",rows).put("noAuthorityMutation",true).toString(2).getBytes("UTF-8"));
    }

    private org.json.JSONObject originalPortrait(OfficerPortrait image,World visual,int id,String caller,PcOfficerInfo.Person saved)throws Exception{
        PortraitMediaIdentity[] identity={null};Bitmap[] loaded={null};long deadline=SystemClock.uptimeMillis()+30000;int year=visual.life.year();PcPortraitLoader loader=PcPortraitLoader.shared(activity);
        while(SystemClock.uptimeMillis()<deadline){ui(()->{identity[0]=PortraitMediaSources.source(visual,id);loaded[0]=loader.get(identity[0],year,0,image);});if(identity[0]!=null&&loaded[0]!=null)break;SystemClock.sleep(50);}
        check(identity[0]!=null&&loaded[0]!=null&&loader.error().isEmpty(),"actual original portrait ready "+caller);
        check(saved!=null&&identity[0].officerId==id&&identity[0].nativeId==saved.nativeId&&identity[0].sourceVariant.equals(saved.sourceVariant)&&identity[0].sourcePath.equals(saved.sourcePath)&&identity[0].sourceSha.equals(saved.sourceSha)&&identity[0].recordSha.equals(saved.recordSha),"actual native source portrait join "+caller);
        ui(()->{try{check(((World.Officer)field(image,"officer")).id==id&&(Integer)field(image,"sourceYear")==year,"actual caller officer and current year "+caller);check(((java.lang.ref.WeakReference<?>)field(image,"sourceView")).get()==visual,"actual caller uses current presentation view "+caller);PortraitMediaIdentity bound=(PortraitMediaIdentity)field(image,"sourceIdentity");check(bound!=null&&bound.officerId==identity[0].officerId&&bound.nativeId==identity[0].nativeId&&bound.sourceVariant.equals(identity[0].sourceVariant)&&bound.recordSha.equals(identity[0].recordSha)&&field(image,"original")==loader,"actual attached Drawable retains verified identity and shared original loader "+caller);}catch(Exception e){throw new IllegalStateException(e);}});
        PcPortraitCatalog catalog=(PcPortraitCatalog)field(loader,"catalog");PcPortraitCatalog.Image expectedSource=catalog.resolve(identity[0],year,0);check(expectedSource!=null,"verified original current-year asset "+caller);
        Bitmap expected;try(InputStream in=activity.getAssets().open(expectedSource.asset)){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;expected=BitmapFactory.decodeStream(in,null,options);}
        check(expected!=null&&expected.sameAs(loaded[0]),"actual caller bitmap equals complete packaged original pixels "+caller);expected.recycle();check(loader.bytes()<=16*1024*1024,"actual portrait cache stays within16MiB "+caller);
        return new org.json.JSONObject().put("caller",caller).put("officerId",id).put("nativeId",identity[0].nativeId).put("currentName",visual.officer(id).name).put("year",year).put("sourceVariant",identity[0].sourceVariant).put("sourcePath",identity[0].sourcePath).put("sourceSha256",identity[0].sourceSha).put("recordSha256",identity[0].recordSha).put("asset",expectedSource.asset).put("pngSha256",expectedSource.pngSha).put("rgbaSha256",expectedSource.rgbaSha).put("faceId",expectedSource.face).put("fullBitmapSameAs",true).put("width",loaded[0].getWidth()).put("height",loaded[0].getHeight());
    }
    private TextView revealPortrait(DataTable<?> table,World.Officer officer,int nativeId)throws Exception {
        int[] position={-1};
        ui(()->{for(int i=0;i<table.list.getAdapter().getCount();i++)if(((World.Officer)table.list.getAdapter().getItem(i)).id==officer.id){position[0]=i;break;}});
        check(position[0]>=0,"normal name search contains exact source officer "+nativeId);
        long deadline=SystemClock.uptimeMillis()+120000;
        while(SystemClock.uptimeMillis()<deadline){
            TextView[] cell={null};int[] first={0};Rect viewport=new Rect(),candidateBounds=new Rect();boolean[] sufficientlyVisible={false};
            ui(()->{
                first[0]=table.list.getFirstVisiblePosition();
                View found=find(table.list,v->{
                    if(!(v instanceof TextView)||!officer.name.contentEquals(((TextView)v).getText())||!(((TextView)v).getCompoundDrawables()[0] instanceof OfficerPortrait))return false;
                    Rect visible=new Rect();if(!v.getGlobalVisibleRect(visible)||visible.isEmpty())return false;
                    try{return ((World.Officer)field(((TextView)v).getCompoundDrawables()[0],"officer")).id==officer.id;}catch(Exception e){throw new IllegalStateException(e);}
                });
                if(found!=null){
                    cell[0]=(TextView)found;Rect clipped=new Rect();found.getGlobalVisibleRect(clipped);
                    sufficientlyVisible[0]=clipped.height()>=found.getHeight()*.8f;
                    int[] location=new int[2];found.getLocationOnScreen(location);
                    candidateBounds.set(location[0],location[1],location[0]+found.getWidth(),location[1]+found.getHeight());
                }
                check(table.list.getGlobalVisibleRect(viewport)&&viewport.height()>0,"actual filtered roster viewport reachable "+nativeId);
                int[] origin=new int[2];table.list.getRootView().getLocationOnScreen(origin);viewport.offset(origin[0],origin[1]);
            });
            if(cell[0]!=null&&sufficientlyVisible[0])return cell[0];
            // Search includes faction/status columns. The exact person can be
            // below the viewport; use a human-reachable ListView swipe, never
            // select a different name or bypass the callback with performClick.
            // First-visible can be the desired row while its top is clipped.
            // Scroll toward its measured missing portion instead of oscillating
            // a fixed page up/down around that partially visible row.
            float shift;
            if(cell[0]!=null&&candidateBounds.top<viewport.top)shift=viewport.top-candidateBounds.top+UiTheme.dp(activity,8);
            else if(cell[0]!=null&&candidateBounds.bottom>viewport.bottom)shift=viewport.bottom-candidateBounds.bottom-UiTheme.dp(activity,8);
            else shift=viewport.height()*(position[0]>=first[0]?-.4f:.4f);
            float limit=viewport.height()*.4f;shift=Math.max(-limit,Math.min(limit,shift));
            int minimum=ViewConfiguration.get(activity).getScaledTouchSlop()*2;
            if(Math.abs(shift)<minimum)shift=Math.copySign(minimum,shift);
            boolean forward=shift<0;float x=viewport.centerX();
            float start=viewport.top+viewport.height()*(forward?.7f:.3f),finish=start+shift;
            log.append("ACTUAL roster swipe native=").append(nativeId).append(" targetIndex=").append(position[0]).append(" firstVisible=").append(first[0]).append(" candidate=").append(candidateBounds.flattenToString()).append(" viewport=").append(viewport.flattenToString()).append(" deltaY=").append(shift).append(" forward=").append(forward).append('\n');
            long down=SystemClock.uptimeMillis();pointer(down,MotionEvent.ACTION_DOWN,x,start);
            for(int i=1;i<=10;i++)pointer(down,MotionEvent.ACTION_MOVE,x,start+(finish-start)*i/10f);
            pointer(down,MotionEvent.ACTION_UP,x,finish);SystemClock.sleep(400);
        }
        throw new AssertionError("Actual source portrait row not reachable after real swipes native="+nativeId+" name="+officer.name);
    }

    private void finishPortraitSearch(DataTable<?> table)throws Exception {
        boolean[] accepted={false};
        ui(()->{
            android.view.inputmethod.EditorInfo info=new android.view.inputmethod.EditorInfo();
            android.view.inputmethod.InputConnection connection=table.search.onCreateInputConnection(info);
            accepted[0]=connection!=null&&connection.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        });
        check(accepted[0],"normal keyboard search action accepted");
        android.accessibilityservice.AccessibilityServiceInfo original=getUiAutomation().getServiceInfo();
        android.accessibilityservice.AccessibilityServiceInfo observed=getUiAutomation().getServiceInfo();
        observed.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getUiAutomation().setServiceInfo(observed);
        long deadline=SystemClock.uptimeMillis()+15000,stableSince=0;String last="";
        try{
            while(SystemClock.uptimeMillis()<deadline){
                boolean ime=false;
                for(android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()){
                    if(window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)ime=true;
                    window.recycle();
                }
                String[] layout={null};boolean[] focused={true};
                ui(()->{
                    Rect frame=new Rect(),viewport=new Rect();table.getRootView().getWindowVisibleDisplayFrame(frame);table.list.getGlobalVisibleRect(viewport);
                    int[] origin=new int[2];table.getRootView().getLocationOnScreen(origin);
                    layout[0]=frame.flattenToString()+"/"+viewport.flattenToString()+"/"+origin[0]+","+origin[1];focused[0]=table.search.hasFocus();
                });
                long now=SystemClock.uptimeMillis();
                if(!ime&&!focused[0]&&layout[0].equals(last)){
                    if(stableSince==0)stableSince=now;
                    if(now-stableSince>=1000){log.append("ACTUAL search settled ime=false focus=false layout=").append(layout[0]).append(" quietMs=").append(now-stableSince).append('\n');return;}
                }else stableSince=0;
                last=layout[0];SystemClock.sleep(100);
            }
            throw new AssertionError("Normal search keyboard/layout did not settle after actual editor action");
        }finally{getUiAutomation().setServiceInfo(original);}
    }

    private void normalPortraitRows(int source)throws Exception{
        byte[] before=capture();StateToken prior=token();World visual=SessionProbe.view(activity);Map<Integer,PcOfficerInfo.Person> sourcePeople=PcOfficerInfo.saved(visual);org.json.JSONArray rows=new org.json.JSONArray();text("清除");DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);
        List<Integer> ids=new ArrayList<>();
        if(allPortraitCallers){ids.addAll(sourcePeople.keySet());Collections.sort(ids);ui(()->table.search.setText(""));SystemClock.sleep(400);
            Set<Integer> actual=new HashSet<>();ui(()->{for(int i=0;i<table.list.getAdapter().getCount();i++)actual.add(((World.Officer)table.list.getAdapter().getItem(i)).id);});check(actual.equals(new HashSet<>(ids)),"normal all-officer roster equals complete current saved original identity set "+source);
        }else for(int nativeId:new int[]{184,229,249,616})ids.add(sourcePeople.values().stream().filter(p->p.nativeId==nativeId).findFirst().orElseThrow().officerId);
        if(portraitNative>=0){ids.removeIf(id->sourcePeople.get(id).nativeId!=portraitNative);check(ids.size()==1,"targeted original native identity exists in complete actual roster "+portraitNative);}
        int visited=0;
        for(int id:ids){
            var person=sourcePeople.get(id);World.Officer officer=visual.officer(id);check(officer!=null,"source native officer exists "+person.nativeId);
            tap(table.search);ui(()->table.search.setText(officer.name));finishPortraitSearch(table);
            TextView cell=revealPortrait(table,officer,person.nativeId);
            OfficerPortrait rowImage=(OfficerPortrait)cell.getCompoundDrawables()[0];rows.put(originalPortrait(rowImage,visual,officer.id,"normal-roster",person));
            ui(()->check(activity.officerSnapshot().officer(id)!=null,"current authoritative detail identity available "+source+":"+person.nativeId));
            // Use the normal page's advertised long-press detail gesture, on its
            // visible name/portrait cell rather than the horizontally scrolling row.
            // Metadata/bitmap readiness can outlive a recycled list row. Resolve
            // the current stable-ID cell again immediately before real input.
            TextView touchCell=revealPortrait(table,officer,person.nativeId);
            longPressPortrait(touchCell,person.nativeId);
            ImageView detail=(ImageView)await(v->{if(!(v instanceof ImageView)||!(((ImageView)v).getDrawable() instanceof OfficerPortrait))return false;try{return ((World.Officer)field(((ImageView)v).getDrawable(),"officer")).id==officer.id;}catch(Exception e){throw new IllegalStateException(e);}});
            rows.put(originalPortrait((OfficerPortrait)detail.getDrawable(),visual,officer.id,"normal-detail",person));visited++;
            if(!allPortraitCallers||visited==1||visited==ids.size()||visited%32==0||person.nativeId==184||person.nativeId==229||person.nativeId==249||person.nativeId==616)shot("source-"+source+"-native-"+person.nativeId+"-normal-detail");
            text("返回");unchanged(before,prior,"normal source portrait roster/detail "+source+":"+person.nativeId);
            if(visited%16==0)sample("source-"+source+"-portrait-"+visited);
            // Persist completed real caller proofs incrementally; an interrupted source remains partial.
            if(visited%32==0||visited==ids.size())Files.write(new File(output,"source-"+source+"-portrait-callers.json").toPath(),new org.json.JSONObject().put("sourceIndex",source).put("rows",rows).put("expectedOfficers",sourcePeople.size()).put("requestedOfficers",ids.size()).put("portraitNative",portraitNative).put("actualVisitedOfficers",visited).put("requestedCallersComplete",visited==ids.size()).put("sourceComplete",allPortraitCallers&&portraitNative<0&&visited==ids.size()).put("allOriginalOfficerCallers",allPortraitCallers&&portraitNative<0).put("actualNormalMenuRosterDetail",true).put("fullSaveRngStateTokenPure",true).put("ageBoundaryFullScreenVoiceArmAccepted",false).toString(2).getBytes("UTF-8"));
        }
        ui(()->table.search.setText(""));check(rows.length()==ids.size()*2,"every requested original normal roster/detail caller proven "+source);
    }

    /** Real menu cancellation before READY; no constructed scene or direct lifecycle call. */
    private void fastCancelPreviews(List<PcScenarioCatalog.Source> sources)throws Exception {
        byte[] before=capture();StateToken prior=token();int[] baseline={0};
        MapHost main=host(false);ui(()->{try{baseline[0]=(Integer)field(main,"activeNativeHosts");}catch(Exception error){throw new IllegalStateException(error);}});
        org.json.JSONArray rows=new org.json.JSONArray();int pendingCancellations=0;
        for(int source=begin;source<end;source++)for(int cycle=0;cycle<2;cycle++){
            nav("菜单");text("新游戏 / 选择势力");
            tap(desc("选择PC来源剧本 "+sources.get(source).identity.path));
            MapHost preview=host(true);FilamentMapView[] renderer={null};int[] pending={0},assets={0};boolean[] verified={false};long[] generation={0};
            ui(()->{try{
                renderer[0]=(FilamentMapView)field(preview,"spatial");
                check(renderer[0]!=null&&!(Boolean)field(renderer[0],"released"),"fast preview initialized original3D, not recovery");
                pending[0]=(Integer)field(renderer[0],"pending");
                assets[0]=((SceneAssetQueue)field(renderer[0],"assetWork")).pending();
                verified[0]=(Boolean)field(renderer[0],"outputVerified");generation[0]=(Integer)field(renderer[0],"generation");
                check((Integer)field(preview,"activeNativeHosts")<=baseline[0]+1,"fast source adds at most one owned native host");
            }catch(Exception error){throw new IllegalStateException(error);}});
            boolean interrupted=pending[0]>0||assets[0]>0;if(interrupted)pendingCancellations++;
            sample("fast-preview-"+source+"-"+cycle+"-before-cancel");long cancelled=SystemClock.uptimeMillis();
            // Touch the actual picker Return while CPU preparation may still run.
            text("返回");retired(preview);text("取消");unchanged(before,prior,"fast real source cancellation "+source+"/"+cycle);
            boolean[] closed={false};long deadline=SystemClock.uptimeMillis()+30000;
            while(SystemClock.uptimeMillis()<deadline){ui(()->{try{
                SceneWorkQueue<?> meshes=(SceneWorkQueue<?>)field(renderer[0],"meshWork");SceneAssetQueue decoded=(SceneAssetQueue)field(renderer[0],"assetWork");
                closed[0]=(Boolean)field(renderer[0],"released")&&field(renderer[0],"engine")==null
                    &&field(renderer[0],"snapshot")==null&&((List<?>)field(renderer[0],"chunks")).isEmpty()
                    &&((List<?>)field(renderer[0],"woods")).isEmpty()&&(Integer)field(renderer[0],"pending")==0
                    &&(Integer)field(renderer[0],"generation")>generation[0]
                    &&(Boolean)field(meshes,"closed")&&meshes.pending()==0&&meshes.waiting()==0
                    &&((java.util.concurrent.ThreadPoolExecutor)field(meshes,"executor")).isTerminated()
                    &&(Boolean)field(decoded,"closed")&&decoded.pending()==0&&decoded.bytes()==0
                    &&((java.util.concurrent.ThreadPoolExecutor)field(decoded,"executor")).isTerminated()
                    &&(Integer)field(preview,"activeNativeHosts")==baseline[0];
            }catch(Exception error){throw new IllegalStateException(error);}});if(closed[0])break;SystemClock.sleep(50);}
            check(closed[0],"fast cancelled renderer releases CPU/GPU owners and both workers terminate "+source+"/"+cycle);
            sample("fast-preview-"+source+"-"+cycle+"-closed");
            rows.put(new org.json.JSONObject().put("sourceIndex",source).put("cycle",cycle).put("pendingAtObservation",pending[0])
                .put("assetsAtObservation",assets[0]).put("outputVerifiedBeforeCancel",verified[0]).put("actualPendingCancellation",interrupted)
                .put("cancelToWorkersClosedMs",SystemClock.uptimeMillis()-cancelled).put("mainNativeHostBaseline",baseline[0])
                .put("completeSaveRngStateTokenPure",true).put("closedWorkersAndOwners",closed[0]));
            Files.write(new File(output,"fast-preview-cancellation.json").toPath(),new org.json.JSONObject()
                .put("scope","Actual normal menu/Return/cancel before waiting READY; subsequent same-source/new-source retries and normal newgame below. No invented scene or forced GC.")
                .put("rows",rows).put("requestedSources",end-begin).put("actualPendingCancellationCount",pendingCancellations)
                .put("complete",source==end-1&&cycle==1).put("normalMatrixAndColdAndRestoreAccepted",false).toString(2).getBytes("UTF-8"));
        }
        check(rows.length()==2*(end-begin)&&pendingCancellations>0,"actual requested rapid cancellations include observed unfinished CPU work");
    }

    private void exitThroughBackConfirmation(byte[] expected)throws Exception {
        MapHost exiting=host(false);MainActivity original=activity;boolean[] confirm={false};
        for(int attempt=0;attempt<8&&!confirm[0];attempt++){
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(300);
            ui(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()
                &&find(root,v->v instanceof TextView&&((TextView)v).getText().toString().startsWith("退出游戏？当前局面将自动保存。"))!=null){confirm[0]=true;break;}});
        }
        check(confirm[0],"actual Android Back reaches normal auto-save exit confirmation");
        check(Arrays.equals(expected,capture()),"exit prompt keeps complete Save/RNG before human-confirmable action");
        shot("normal-exit-confirmation");text("执行");boolean[] ended={false};long deadline=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<deadline){ui(()->ended[0]=original.isDestroyed()
            &&((GameApplication)original.getApplication()).host().session()==null);if(ended[0])break;SystemClock.sleep(50);}
        check(ended[0],"normal Back/confirmation closes Activity and actual native session");retired(exiting);
        check(Arrays.equals(expected,Files.readAllBytes(new File(original.getFilesDir(),"auto.sg11").toPath())),"normal exit auto-save exact complete Save/RNG bytes");
    }

    private void retired(MapHost host)throws Exception{long began=SystemClock.uptimeMillis(),deadline=began+12000;boolean[] done={false};String[] state={null};while(SystemClock.uptimeMillis()<deadline){ui(()->{try{done[0]=(Boolean)field(host,"released")&&field(host,"spatial")==null&&field(host,"world")==null&&field(host,"ground")==null;state[0]="released="+field(host,"released")+" engineClosed="+(field(host,"spatial")==null)+" worldClosed="+(field(host,"world")==null)+" groundClosed="+(field(host,"ground")==null);}catch(Exception e){throw new IllegalStateException(e);}});if(done[0])break;SystemClock.sleep(50);}check(done[0],"dismissed preview releases engine, detachedWorld and ground in "+(SystemClock.uptimeMillis()-began)+"ms "+state[0]);}
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("session-a-map"),run);check(output.mkdirs(),"fresh evidence directory");ActivityManager manager=(ActivityManager)getTargetContext().getSystemService(android.content.Context.ACTIVITY_SERVICE);ActivityManager.MemoryInfo budget=new ActivityManager.MemoryInfo();manager.getMemoryInfo(budget);Files.write(new File(output,"heap-budget.txt").toPath(),("normalMiB="+manager.getMemoryClass()+" largeMiB="+manager.getLargeMemoryClass()+" actualBytes="+Runtime.getRuntime().maxMemory()+" deviceRamBytes="+budget.totalMem+" largeHeap="+((getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP)!=0)+"\n").getBytes("UTF-8"));activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));nav("地图");ready(host(false));sample("startup");if(!expectedStartupSha.isEmpty()){StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(capture()))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));check(expectedStartupSha.equals(hash.toString()),"fresh process loads exact previously saved complete Save/RNG SHA "+expectedStartupSha);}
        List<PcScenarioCatalog.Source> sources=PcScenarioCatalog.all();check(sources.size()==16,"all16 installed sources");if(fastPreviewCancellation)fastCancelPreviews(sources);
        for(int index=begin;index<end;index++){
            byte[] before=capture();StateToken prior=token();nav("菜单");text("新游戏 / 选择势力");PcScenarioCatalog.Source source=sources.get(index);tap(desc("选择PC来源剧本 "+source.identity.path));MapHost preview=host(true);ready(preview);unchanged(before,prior,"source preview "+index);
            if(index==begin||allFactionPreviews){text("返回");retired(preview);text("取消");unchanged(before,prior,"actual preview and source cancel");nav("菜单");text("新游戏 / 选择势力");tap(desc("选择PC来源剧本 "+source.identity.path));preview=host(true);ready(preview);unchanged(before,prior,"cancel/retry");}
            List<Button> chips=new ArrayList<>();ui(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())collect(root,chips);});check(!chips.isEmpty(),"normal faction chips");for(Button b:chips)check((b.getCurrentTextColor()>>>24)>0&&(b.getCurrentTextColor()&0xffffff)!=0,"faction text has opaque nonzero color "+b.getText());
            Button first=null,last=null;for(Button b:chips)if(b.isEnabled()){if(first==null)first=b;last=b;}if(allFactionPreviews)allFactionPreviews(index,preview,chips,before,prior);tap(last);tap(first);unchanged(before,prior,"faction changes "+index);shot("source-"+index+"-preview");
            for(int cycle=0;cycle<(index==begin?4:1);cycle++){tap(await(v->v instanceof Button&&"全图".contentEquals(((Button)v).getText())));closeView(preview);pan(preview);ready(preview);unchanged(before,prior,"preview gestures "+index+"/"+cycle);}
            tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力")));text("开始新局");long deadline=SystemClock.uptimeMillis()+120000;while(Arrays.equals(before,capture())&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(100);check(!Arrays.equals(before,capture()),"explicit new game committed "+index);retired(preview);nav("地图");MapHost current=host(false);ready(current);byte[] state=capture();StateToken currentToken=token();World saved=SaveCodec.decode(state);check(saved.scenarioId.equals(source.identity.scenarioId),"new game matches clicked exact source");
            for(int cycle=0;cycle<2;cycle++){normalFit();closeView(current);pan(current);ready(current);unchanged(state,currentToken,"new game gestures "+index+"/"+cycle);}
            shot("source-"+index+"-map");Files.write(new File(output,"source-"+index+".sg11").toPath(),state);nav("武将");shot("source-"+index+"-officers");if(portraitPixels)normalPortraitRows(index);unchanged(state,currentToken,"normal directory "+index);nav("地图");sample("source-"+index+"-complete");
            Bundle update=new Bundle();update.putString("stream","SESSION_A source "+index+" normal flow complete\n");sendStatus(0,update);
        }
        if(!expectedStartupSha.isEmpty()&&portraitNative>=0){
            check(allPortraitCallers&&coldPortraitSource>=0&&coldPortraitSource<sources.size(),"targeted cold caller tied to exact normal source and saved SHA");
            check(SessionProbe.view(activity).scenarioId.equals(sources.get(coldPortraitSource).identity.scenarioId),"targeted cold scenario matches exact normal source");
            nav("武将");normalPortraitRows(coldPortraitSource);nav("地图");ready(host(false));sample("cold-original-portrait-"+portraitNative);
        }
        byte[] state=capture();StateToken prior=token();MapHost backgroundHost=host(false);
        check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"system global Home accepted");
        long backgroundDeadline=SystemClock.uptimeMillis()+10000;boolean[] background={false};
        while(SystemClock.uptimeMillis()<backgroundDeadline){ui(()->{try{FilamentMapView view=(FilamentMapView)field(backgroundHost,"spatial");background[0]=!(Boolean)field(backgroundHost,"resumed")&&view!=null&&!(Boolean)field(view,"resumed")&&!activity.hasWindowFocus();}catch(Exception e){throw new IllegalStateException(e);}});if(background[0])break;SystemClock.sleep(50);}
        check(background[0],"actual system Home pauses Activity/map/Filament and removes focus");SystemClock.sleep(800);getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));SystemClock.sleep(1000);ready(host(false));unchanged(state,prior,"actual Home/resume");shot("resumed");
        text("视图");text("屏幕方向");text("横屏");SystemClock.sleep(1200);ready(host(false));check(Arrays.equals(state,capture()),"real orientation menu keeps complete Save/RNG");shot("landscape");text("视图");text("屏幕方向");text("竖屏");SystemClock.sleep(1200);ready(host(false));check(Arrays.equals(state,capture()),"portrait keeps complete Save/RNG");
        nav("菜单");text("保存局面");text("槽位 3");text("覆盖存档");long saveDeadline=SystemClock.uptimeMillis()+120000;File slot=new File(activity.getFilesDir(),"manual3.sg11");while(SystemClock.uptimeMillis()<saveDeadline){ui(()->{});if(slot.isFile()&&Arrays.equals(state,Files.readAllBytes(slot.toPath())))break;SystemClock.sleep(100);}check(slot.isFile()&&Arrays.equals(state,Files.readAllBytes(slot.toPath())),"normal save exact complete bytes after owner confirmation finishes");StateToken loadPrior=token();text("读取存档");text("槽位 3");text("读取存档");long loaded=SystemClock.uptimeMillis()+120000;while(loadPrior.equals(token())&&SystemClock.uptimeMillis()<loaded)SystemClock.sleep(100);check(!loadPrior.equals(token())&&Arrays.equals(state,capture()),"normal load replaces session and keeps complete Save/RNG");nav("地图");ready(host(false));
        exitThroughBackConfirmation(state);SystemClock.sleep(1000);activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));nav("地图");ready(host(false));check(Arrays.equals(state,capture()),"normal exit/reopen exact complete Save/RNG");shot("reopened");
        result.putString("stream","SESSION_A_MAP PASS "+checks+" checks\n"+log);
    }catch(Throwable error){result.putString("stream","SESSION_A_MAP FAIL "+LogTrace(error)+"\n"+log);try{shot("FAIL");}catch(Throwable ignored){}}
    finally{try{Files.write(new File(output,"result.txt").toPath(),result.getString("stream").getBytes("UTF-8"));}catch(Throwable ignored){}}
    // Binder is a bounded status channel. The complete log remains in result.txt;
    // sending every per-officer check made the670-person normal result exceed2MB.
    boolean passed=result.getString("stream").startsWith("SESSION_A_MAP PASS");Bundle terminal=new Bundle();
    terminal.putString("stream","SESSION_A_MAP "+(passed?"PASS ":"FAIL ")+checks+" checks; full evidence: "+new File(output,"result.txt").getAbsolutePath()+"\n");
    finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,terminal);}
    private void collect(View v,List<Button> result){if(v instanceof Button&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("选择势力 ·"))result.add((Button)v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),result);}
    private String LogTrace(Throwable error){return android.util.Log.getStackTraceString(error);}
}
