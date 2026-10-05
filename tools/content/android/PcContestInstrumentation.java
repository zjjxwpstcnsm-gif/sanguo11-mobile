package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import game.sanguo.api.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;

/** Actual installed list/search/row-click/details, compared to saved authority. */
public final class PcContestInstrumentation extends Instrumentation {
    private MainActivity activity;private int checks;private File output;
    private final StringBuilder log=new StringBuilder();
    private boolean resume;
    @Override public void onCreate(Bundle args){super.onCreate(args);resume=args!=null&&"1".equals(args.getString("contestResume"));start();}
    private void settle(){runOnMainSync(()->{});SystemClock.sleep(200);}
    private void check(boolean ok,String text)throws Exception{
        if(!ok)throw new AssertionError(text);checks++;log.append("PASS ").append(text).append('\n');
        try(FileOutputStream stream=new FileOutputStream(new File(output,"progress.txt"))){stream.write(log.toString().getBytes("UTF-8"));}
    }
    private View find(View view,Predicate<View> predicate){
        Rect rect=new Rect();if(!view.isShown()||!view.getGlobalVisibleRect(rect)||rect.width()<8||rect.height()<8)return null;
        if(predicate.test(view))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),predicate);if(found!=null)return found;}}
        return null;
    }
    private View await(Predicate<View> predicate){
        long end=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<end){View[] result={null};runOnMainSync(()->{
            List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--){View root=roots.get(i);if(root.hasWindowFocus()&&(result[0]=find(root,predicate))!=null)break;}
        });if(result[0]!=null)return result[0];settle();}
        throw new AssertionError("Visible officer control timed out");
    }
    private void tap(View view){
        settle();Rect rect=new Rect();runOnMainSync(()->{int[] screen=new int[2];view.getGlobalVisibleRect(rect);view.getRootView().getLocationOnScreen(screen);rect.offset(screen[0],screen[1]);});long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,rect.centerX(),rect.centerY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);if(!getUiAutomation().injectInputEvent(event,true))throw new AssertionError("Pointer rejected");event.recycle();SystemClock.sleep(50);}settle();
    }
    private void description(String value){tap(await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(value)));}
    private void text(String value){tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(value)&&
        (v.isClickable()||v.getParent() instanceof AdapterView)));}
    private void nav(String value){description("打开功能导航");description("导航 · "+value);}
    private Object field(Object object,String name)throws Exception{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void panelText(String value)throws Exception{
        View[] expand={null};runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())expand[0]=find(root,v->v instanceof TextView&&v.isClickable()&&"展开".equals(((TextView)v).getText().toString()));});if(expand[0]!=null)tap(expand[0]);
        for(int i=0;i<12;i++){
            View[] found={null};runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())
                found[0]=find(root,v->v instanceof TextView&&v.isClickable()&&((TextView)v).getText().toString().startsWith(value));});
            if(found[0]!=null){tap(found[0]);return;}
            ScrollView[] scroll={null};runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){View heading=find(root,v->v instanceof TextView&&((TextView)v).getText().toString().startsWith("舌战 ·"));if(heading!=null){View parent=heading;while(parent!=null&&!(parent instanceof ScrollView))parent=parent.getParent() instanceof View?(View)parent.getParent():null;if(parent instanceof ScrollView)scroll[0]=(ScrollView)parent;}}});if(scroll[0]==null)scroll[0]=(ScrollView)field(activity,"panelScroll");runOnMainSync(()->scroll[0].performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null));settle();
        }
        throw new AssertionError("Actual panel command not visible: "+value);
    }
    private byte[] capture(){byte[][] result={null};runOnMainSync(()->{try{result[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return result[0];}
    private void screenshot(String name)throws Exception{
        Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new AssertionError("Screenshot unavailable");
        try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{image.recycle();}
    }
    private void row(int id)throws Exception{
        DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);int[] position={-1};runOnMainSync(()->{for(int i=0;i<table.list.getAdapter().getCount();i++)if(table.list.getAdapter().getItemId(i)==id){position[0]=i;break;}});
        check(position[0]>=0,"normal picker retains actual officer ID "+id);runOnMainSync(()->table.list.setSelection(position[0]));settle();View[] view={null};runOnMainSync(()->view[0]=table.list.getChildAt(position[0]-table.list.getFirstVisiblePosition()));check(view[0]!=null,"normal picker actual row visible");tap(view[0]);
    }
    private void manualSave(byte[] bytes)throws Exception{
        nav("菜单");text("保存局面");text("槽位 3");text("覆盖存档");settle();check(Arrays.equals(bytes,java.nio.file.Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"normal manual save stores every contest/source/RNG byte");
    }
    private void card(World control)throws Exception{
        Contests.Session s=control.contests.current();Debate d=s.debate();int chosen=-1;
        for(int i=0;i<d.speaker(0).hand().size();i++)if(d.cardError(i)==null){if(chosen<0)chosen=i;if(d.speaker(0).hand().get(i).talk!=null){chosen=i;break;}}
        check(chosen>=0,"normal sourced debate has a legal card");String label=d.speaker(0).hand().get(chosen).label();check(control.contests.debateCard(s.id(),s.revision(),chosen).ok,"independent ordinary source card control");
        panelText("出牌 · "+label);check(Arrays.equals(SaveCodec.encode(control),capture()),"normal card widget commits complete contest/source save and RNG · "+label);
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            output=getTargetContext().getExternalFilesDir("session1-pc-contest-"+(resume?"resume":"opening"));output.mkdirs();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
            if(!resume){
                nav("菜单");text("新游戏 / 选择势力");PcScenarioCatalog.Source source=PcScenarioCatalog.all().get(0);String target="选择PC来源剧本 "+source.identity.path;View[] selected={null};
                for(int scroll=0;scroll<14&&selected[0]==null;scroll++){
                    runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus())selected[0]=find(root,v->target.equals(String.valueOf(v.getContentDescription())));});
                    if(selected[0]!=null)break;View scroller=await(v->v instanceof ScrollView);runOnMainSync(()->scroller.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null));settle();
                }
                check(selected[0]!=null,"actual original source is selected from ordinary menu");tap(selected[0]);description("确认开局势力");text("开始新局");
                long deadline=SystemClock.uptimeMillis()+60000;World w=SaveCodec.decode(capture());while(!w.scenarioId.equals(source.identity.scenarioId)&&SystemClock.uptimeMillis()<deadline){settle();w=SaveCodec.decode(capture());}
                check(w.scenarioId.equals(source.identity.scenarioId)&&PcContestProfiles.saved(w).size()==670,"real source menu imports checked contest traits");
                World.City city=null;World.Officer actor=null,person=null;
                outer:for(World.City c:w.cities)if(c.owner==w.player)for(World.Officer a:w.idle(c))for(World.Officer t:w.officers)if(w.contests.debateError(c.id,a.id,t.id)==null){city=c;actor=a;person=t;break outer;}
                check(city!=null&&city.id==w.home().id,"unmodified source opening has an ordinary local debate target");
                int actorId=actor.id,targetId=person.id;String targetName=person.name;World control=SaveCodec.decode(capture());check(control.contests.persuade(city.id,actorId,targetId).ok,"ordinary source debate trigger control");
                nav("地图");description("定位己方据点 "+city.name);text("武将");text("展开");panelText("舌战登用");row(targetId);row(actorId);text("执行");
                check(Arrays.equals(SaveCodec.encode(control),capture()),"real recruitment dialog commits full source profiles/costs/RNG");
                nav("地图");await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith("舌战 ·"));
                screenshot("normal-source-debate");
                for(int i=0;i<2&&control.contests.current().debate().winner()==-2;i++)card(control);
                byte[] mid=capture();manualSave(mid);java.nio.file.Files.write(new File(output,"contest-mid.sg11").toPath(),mid);
                nav("地图");if(control.contests.current().debate().winner()==-2)card(control);
                nav("菜单");text("读取存档");text("槽位 3");text("读取存档");settle();check(Arrays.equals(mid,capture()),"normal load restores complete mid-contest data/RNG");nav("地图");
            }else{
                check(Arrays.equals(capture(),java.nio.file.Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"cold process startup preserves entire captured mid-contest/RNG byte sequence");nav("地图");
            }
            World control=SaveCodec.decode(capture());check(control.contests.busy()&&!control.contests.current().isDuel()&&PcContestProfiles.saved(control).size()==670,"actual normal source contest survives save/reopen");
            check(control.contests.profile(control.contests.current().debate().speaker(0).officerId()).talkMask!=0,"actual participant consumes sourced talk flags");
            int guard=0;while(control.contests.current().debate().winner()==-2){check(guard++<101,"normal debate reaches bounded result");card(control);}
            int winner=control.contests.current().debate().winner();check(control.contests.finishDebate(control.contests.current().id(),control.contests.current().revision(),true).ok,"ordinary campaign result control");
            panelText(winner==0?"留情 · 技巧+50":"结算结果");check(Arrays.equals(SaveCodec.encode(control),capture())&&!SaveCodec.decode(capture()).contests.busy(),"normal result widget commits once with complete save/RNG");
            byte[] finalSave=capture();manualSave(finalSave);java.nio.file.Files.write(new File(output,"contest-final.sg11").toPath(),finalSave);screenshot("normal-source-contest-result");
            result.putString("stream","PC SOURCE OPENING PASS "+checks+" actual contest checks; original numeric state machine integration still pending\n"+log);finish(Activity.RESULT_OK,result);
        }catch(Throwable error){try{screenshot("FAIL");}catch(Throwable ignored){}result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
    }
}
