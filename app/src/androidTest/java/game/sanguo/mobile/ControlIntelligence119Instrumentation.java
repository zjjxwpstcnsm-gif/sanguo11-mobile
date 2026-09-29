package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/** Installed normal MainActivity/WarUi/session. Explicit fixture and programmatic target
 * selection; accessibility confirms the actual dialog. Not full-touch or 3D acceptance. */
public final class ControlIntelligence119Instrumentation extends SceneInstrumentation {
    private byte[] authority(){byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return b[0];}
    private void click(String text)throws Exception{
        long deadline=SystemClock.uptimeMillis()+10000;
        while(SystemClock.uptimeMillis()<deadline){
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(AccessibilityNodeInfo found:root.findAccessibilityNodeInfosByText(text)){
                AccessibilityNodeInfo node=found;
                while(node!=null&&!node.isClickable())node=node.getParent();
                if(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){settle();return;}
            }
            settle();
        }
        throw new AssertionError("visible clickable text missing: "+text);
    }
    private World fixture(War.Plot plot,boolean success)throws Exception{
        for(long seed=0;seed<1000;seed++){
            World candidate=ControlIntelligenceFixture.world(40,80,40,80,seed);
            candidate.war.plot(1,candidate.unit(2).hex,plot);
            if((candidate.unit(2).status!=War.Status.NORMAL)==success)
                return ControlIntelligenceFixture.world(40,80,40,80,seed);
        }
        throw new AssertionError("no seeded outcome");
    }
    @Override public void onStart(){Bundle result=new Bundle();StringBuilder log=new StringBuilder();try{
        World start=ControlIntelligenceFixture.world(40,80,40,80,119);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(start));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");check(!host.is3D(),"explicit 2D rule/UI scope");
        for(War.Plot plot:new War.Plot[]{War.Plot.CONFUSE,War.Plot.MISLEAD})for(boolean success:new boolean[]{false,true}){
            World initial=fixture(plot,success);
            runOnMainSync(()->{SessionProbe.install(activity,initial);activity.refresh();activity.selectUnitAndFocus(1);});settle();
            byte[] before=authority();World expected=SaveCodec.decode(before);
            check(expected.army.intelligence(expected.unit(1))==80&&expected.army.intelligence(expected.unit(2))==80,"highest deputies used on both sides");
            check(expected.war.plotChance(1,expected.unit(2).hex,plot)==20,"installed candidate predicts 20 percent");
            check(expected.war.plot(1,expected.unit(2).hex,plot).ok,"headless reference paid command accepted");
            runOnMainSync(()->{World w=SessionProbe.view(activity);new WarUi(activity,w,activity::applyResult).plots(w.unit(1));});settle();
            click(plot.label+" · 气力15");
            runOnMainSync(()->{try{((Consumer<Hex>)field(activity,"mapPick")).accept(new Hex(9,8));}catch(Exception e){throw new RuntimeException(e);}});settle();
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            check(root!=null&&!root.findAccessibilityNodeInfosByText("成功率 20%").isEmpty(),"real WarUi confirmation displays 20 percent");
            check(Arrays.equals(before,authority()),"UI preview preserves complete authority/RNG");
            String name=plot.name()+"-"+(success?"success":"failure");capture(name+"-confirm");
            click("执行");settle();
            byte[] actual=authority();check(Arrays.equals(SaveCodec.encode(expected),actual),"normal dialog command matches entire reference save/RNG");
            World after=SaveCodec.decode(actual);check((after.unit(2).status!=War.Status.NORMAL)==success,"actual target status outcome");
            check(after.unit(1).acted&&after.unit(1).energy==85,"actual action and energy cost");
            try(InputStream in=getTargetContext().openFileInput("auto.sg11")){ByteArrayOutputStream saved=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)saved.write(buffer,0,n);check(Arrays.equals(actual,saved.toByteArray()),"normal autosave matches committed session");}
            File dir=getTargetContext().getExternalFilesDir("s01");Files.write(new File(dir,name+"-expected.sg11").toPath(),SaveCodec.encode(expected));Files.write(new File(dir,name+"-actual.sg11").toPath(),actual);
            capture(name+"-result");log.append(name).append(" PASS preview=20 highestInt=80/80 full-save/RNG/autosave equal\n");
        }
        log.append("PASS CONTROL119 checks=").append(checks).append(" source=").append(BuildConfig.SOURCE_REVISION).append("\n");
    }catch(Throwable e){log.append("FAIL CONTROL119 ").append(android.util.Log.getStackTraceString(e));try{capture("control119-failed");}catch(Exception ignored){}}
        try{File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();Files.write(new File(dir,"control119.txt").toPath(),log.toString().getBytes("UTF-8"));}catch(Exception e){log.append("FAIL evidence ").append(e);}
        result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);
    }
}
