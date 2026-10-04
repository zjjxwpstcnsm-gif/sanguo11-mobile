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
public final class OfficerInfoInstrumentation extends Instrumentation {
    private MainActivity activity;private int checks;private File output;
    private final StringBuilder log=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
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
    private byte[] capture(){byte[][] result={null};runOnMainSync(()->{try{result[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception e){throw new RuntimeException(e);}});return result[0];}
    private void screenshot(String name)throws Exception{
        Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new AssertionError("Screenshot unavailable");
        try(FileOutputStream stream=new FileOutputStream(new File(output,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{image.recycle();}
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            output=getTargetContext().getExternalFilesDir("session1-officer-info");output.mkdirs();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
            byte[] before=capture();World control=SaveCodec.decode(before);
            description("打开功能导航");description("导航 · 武将");
            await(v->v instanceof TextView&&"武将一览".equals(((TextView)v).getText().toString()));
            DataTable<?> table=(DataTable<?>)await(v->v instanceof DataTable);
            OfficerSnapshot[] snapshots={null};runOnMainSync(()->snapshots[0]=activity.officerSnapshot());OfficerSnapshot snapshot=snapshots[0];
            check(snapshot.officers.size()==control.officers.size(),"installed DTO includes complete saved roster");
            LinkedHashMap<Integer,OfficerSnapshot.Officer> owners=new LinkedHashMap<>();
            for(OfficerSnapshot.Officer row:snapshot.officers)if(row.owner>=0&&row.present)owners.putIfAbsent(row.owner,row);
            check(owners.size()>=3,"three actual factions available");int count=0;
            for(OfficerSnapshot.Officer row:owners.values()){
                if(count==3)break;
                Bundle input=new Bundle();input.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,row.name);
                boolean[] changed={false};runOnMainSync(()->changed[0]=table.search.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SET_TEXT,input));
                check(changed[0],"native accessibility text input accepted "+row.name);settle();
                int[] matched={0};runOnMainSync(()->matched[0]=table.list.getAdapter().getCount());check(matched[0]>0,"actual search includes saved name "+row.name);
                int[] position={-1};runOnMainSync(()->{for(int i=0;i<table.list.getAdapter().getCount();i++)if(table.list.getAdapter().getItemId(i)==row.id){position[0]=i;break;}});
                check(position[0]>=0,"search preserves stable target ID "+row.id);
                View[] item={null};runOnMainSync(()->table.list.setSelection(position[0]));settle();
                runOnMainSync(()->item[0]=table.list.getChildAt(position[0]-table.list.getFirstVisiblePosition()));
                check(item[0]!=null,"actual officer row visible");tap(item[0]);
                TextView detail=(TextView)await(v->v instanceof TextView&&((TextView)v).getText().toString().contains("基础（统武智政魅）"));
                String text=detail.getText().toString();World.Officer saved=control.officer(row.id);
                String current="统率 "+saved.leadership+"    武力 "+saved.war+"\n智力 "+saved.intelligence+"    政治 "+saved.politics+"\n魅力 "+saved.charm;
                check(text.startsWith(current),"all five current values match independent saved world "+row.id);
                List<Integer> base=new ArrayList<>(),xp=new ArrayList<>();for(int i=0;i<5;i++){base.add(control.officerAbilities.base(row.id,i));xp.add(control.officerAbilities.experience(row.id,i));}
                check(text.contains("基础（统武智政魅）："+base)&&text.contains("经验（统武智政魅）："+xp),"base and XP retain separate saved values "+row.id);
                check(text.contains("功绩 "+control.government.merit(row.id))&&text.contains("忠诚 "+saved.loyalty)&&text.contains("所在地："+row.location),"merit loyalty and location use same DTO");
                check(text.contains("字 / 原传记：尚未核实"),"missing source fields stay explicit");
                screenshot("detail-"+row.id);
                tap(await(v->v.getId()==android.R.id.button2&&v instanceof Button));
                check(Arrays.equals(before,capture()),"search/details preserve every saved byte and RNG "+row.id);count++;
            }
            check(count==3,"three factions inspected through real rows and dialogs");
            result.putString("stream","OFFICER INFO PASS "+checks+" checks\n"+log);finish(Activity.RESULT_OK,result);
        }catch(Throwable error){try{screenshot("FAIL");}catch(Throwable ignored){}result.putString("stream",log+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
    }
}
