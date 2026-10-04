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
    private void awaitSave(byte[] expected,String message)throws Exception{long until=SystemClock.uptimeMillis()+30000;byte[] actual=null;while(SystemClock.uptimeMillis()<until){actual=capture();if(Arrays.equals(expected,actual)){check(true,message);return;}settle();}java.nio.file.Files.write(new File(output,"load-expected.sg11").toPath(),expected);java.nio.file.Files.write(new File(output,"load-actual.sg11").toPath(),actual);check(false,message);}
    private ContestSnapshot facts(){ContestSnapshot[] result={null};runOnMainSync(()->result[0]=((GameApplication)activity.getApplication()).host().session().contest());return result[0];}
    private void card(World control)throws Exception{
        ContestSnapshot dto=facts();Contests.Session s=control.contests.current();check(dto.nativeRules&&dto.waitingCard&&dto.contestId==s.id()&&dto.revision==s.revision(),"normal authoritative original card boundary");
        ContestSnapshot.Card chosen=null;for(ContestSnapshot.Card c:dto.cards)if(c.enabled()&&c.nativeCard!=0){chosen=c;break;}if(chosen==null)for(ContestSnapshot.Card c:dto.cards)if(c.enabled()){chosen=c;break;}
        check(chosen!=null,"original core returns actual legal card");String label=(chosen.nativeCard==0?"再考 · ":"出牌 · ")+chosen.label;check(control.contests.debateCard(s.id(),s.revision(),chosen.slot).ok,"independent actual native campaign control");
        panelText(label);check(Arrays.equals(SaveCodec.encode(control),capture()),"real native card widget stores full World/model/both RNGs · "+label);
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=getTargetContext().getExternalFilesDir("session1-pc-native-debate-"+(resume?"resume":"opening"));output.mkdirs();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        byte[] initial=capture();World control=SaveCodec.decode(initial);check(control.contests.busy()&&control.contests.current().nativeDebate()!=null,"normal process restores complete actual Source World39 native session");
        byte[] supplied=java.nio.file.Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath());byte[] canonical=SaveCodec.encode(SaveCodec.decode(supplied));java.nio.file.Files.write(new File(output,"startup-actual.sg11").toPath(),initial);java.nio.file.Files.write(new File(output,"startup-supplied.sg11").toPath(),supplied);java.nio.file.Files.write(new File(output,"startup-art-canonical.sg11").toPath(),canonical);check(Arrays.equals(initial,canonical),"actual process restore matches every supplied World39 field after ART decode/encode; raw host encoding separately retained");
        nav("地图");await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith("舌战 ·"));screenshot("native-original-page");
        byte[] before=capture();for(int i=0;i<20;i++)facts();check(Arrays.equals(before,capture()),"installed normal DTO queries preserve complete World/RNG");
        if(!resume){
            for(int i=0;i<2&&facts().waitingCard;i++)card(control);
            byte[] mid=capture();manualSave(mid);java.nio.file.Files.write(new File(output,"contest-mid.sg11").toPath(),mid);
            nav("地图");if(facts().waitingCard)card(control);
            nav("菜单");text("读取存档");text("槽位 3");text("读取存档");settle();awaitSave(mid,"normal slot UI restores complete original model/recorded choices/World/RNG");control=SaveCodec.decode(mid);nav("地图");
        }
        int guard=0;while(facts().waitingCard){check(guard++<101,"original game reaches terminal input");card(control);}
        if(facts().waitingMercy){Contests.Session s=control.contests.current();check(control.contests.finishDebate(s.id(),s.revision(),true).ok,"original terminal choice control");panelText("选择智力经验奖励");check(Arrays.equals(SaveCodec.encode(control),capture()),"actual terminal-choice widget full World/RNG parity");}
        check(facts().phase==9,"normal original model reaches terminal phase");check(facts().status.contains("结算")&&!facts().settlementAvailable,"unverified campaign settlement remains clearly pending");
        byte[] end=capture();manualSave(end);java.nio.file.Files.write(new File(output,"contest-final.sg11").toPath(),end);screenshot("native-original-terminal");
        result.putString("stream","PC SOURCE OPENING PASS "+checks+" actual installed native page/card/save/reopen checks; campaign settlement remains pending\n"+log);finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{screenshot("FAIL");}catch(Throwable ignored){}result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
