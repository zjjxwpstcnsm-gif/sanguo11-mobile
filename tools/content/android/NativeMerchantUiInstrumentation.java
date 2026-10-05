package game.sanguo.mobile;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Separate acceptance APK: real native forms/commands, no rule injection. */
public final class NativeMerchantUiInstrumentation extends Instrumentation {
    private MainActivity activity;private File output;private int checks;private String run="native-market-r26";private final StringBuilder log=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)run=args.getString("run",run).replaceAll("[^a-zA-Z0-9_-]","_");start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("rule-ui"),run);output.mkdirs();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        byte[] original=capture();check(((original[7]&255)==35)&&world().merchantMarket.enabled(),"actual normal v35 campaign loaded");
        check(Arrays.equals(original,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"external test-owned slot3 matches full authority");
        transact(1500,false,original);restore(original);
        transact(1,true,original);restore(original);
        check(Arrays.equals(original,capture()),"normal load restores full v35 price/ability/RNG state");
        result.putString("stream","NATIVE MERCHANT UI PASS "+checks+" checks\n"+log);write("result.txt",result.getString("stream"));finish(Activity.RESULT_OK,result);
    }catch(Throwable failure){try{shot("FAIL");write("failure.txt",log+"\n"+failure);}catch(Throwable ignored){}StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));result.putString("stream","NATIVE MERCHANT UI FAIL "+trace);finish(Activity.RESULT_CANCELED,result);}}
    private void transact(int quantity,boolean minimum,byte[] original)throws Exception{
        World before=world();int city=before.home().id;String name=before.home().name;long rng=before.strategy.getRandomState();
        nav("地图");description("定位己方据点 "+name);text("展开");text("内政");scrollTo("商人 / 粮食买卖");text("商人 / 粮食买卖");
        tap(tag("trade.actor"));ListView roster=(ListView)await(v->v instanceof ListView&&((ListView)v).getAdapter()!=null&&((ListView)v).getAdapter().getCount()>0&&((ListView)v).getAdapter().getItem(0) instanceof World.Officer);
        int actor=((World.Officer)roster.getAdapter().getItem(0)).id;tap(roster.getChildAt(0));settle();
        EditText amount=(EditText)tag("trade.amount");
        if(minimum){text("最少");check(amount.getText().toString().equals("1"),"actual minimum control chooses one grain");}
        else{enter(amount,Integer.toString(quantity));sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
        await(v->"trade.review".equals(v.getTag())&&v.isEnabled());
        TradePreview[] quote={null};ui(()->quote[0]=activity.previewTrade(new TradeCommand(activity.deploymentState(),"BUY",city,actor,quantity)));
        TradePreview p=quote[0];check(p.allowed()&&p.quote.nativePricing&&p.quote.step==1&&p.quote.minimum==1,"native source forecast allows real non-thousand quantity");
        check(Arrays.equals(original,capture())&&world().strategy.getRandomState()==rng,"form/input/forecast are pure");shot("quantity-"+quantity+"-form");
        text("可用上限");settle();TradePreview[] maximum={null};int maximumInput=Integer.parseInt(amount.getText().toString());ui(()->maximum[0]=activity.previewTrade(new TradeCommand(activity.deploymentState(),"BUY",city,actor,maximumInput)));
        check(maximum[0].allowed()&&maximumInput==maximum[0].quote.availableMaximum,"actual max control uses native current-politics capacity");
        enter(amount,Integer.toString(quantity));sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();tap(tag("trade.review"));text("返回修改");
        check(amount.getText().toString().equals(Integer.toString(quantity))&&Arrays.equals(original,capture()),"cancel review preserves real input and full authority");
        tap(tag("trade.review"));View confirm=awaitText("确认交易");long revision=activity.deploymentState().revision;tap(confirm,2);settle();
        World after=world();check(activity.deploymentState().revision==revision+1,"double real confirmation commits once");
        check(after.city(city).gold==p.effects.goldAfter&&after.city(city).food==p.effects.foodAfter&&after.actionPoints[after.player]==p.effects.actionPointsAfter,"real balances/AP match post-XP source quote");
        check(after.officerAbilities.experience(actor,3)==p.effects.politicsExperienceAfter&&after.officer(actor).politics==p.effects.politicsAfter,"real command gains XP then refreshes politics");
        check(after.campaign.traded(city)==quantity&&after.officer(actor).acted&&after.strategy.getRandomState()==rng,"real quantity/use/action and unchanged RNG");shot("quantity-"+quantity+"-committed");
    }
    private void restore(byte[] expected)throws Exception{nav("菜单");text("读取存档");tap(awaitText("槽位 3"));text("读取存档");long end=SystemClock.uptimeMillis()+90000;while(!Arrays.equals(expected,capture())&&SystemClock.uptimeMillis()<end)settle();check(Arrays.equals(expected,capture()),"real menu/slot load restores complete campaign");}
    private void nav(String name)throws Exception{description("打开功能导航");description("导航 · "+name);settle();}
    private void description(String value)throws Exception{tap(await(v->v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(value)));}
    private void text(String value)throws Exception{tap(await(v->v.isClickable()&&v instanceof TextView&&((TextView)v).getText().toString().startsWith(value)));}
    private View awaitText(String value)throws Exception{return await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(value));}
    private View tag(String value)throws Exception{return await(v->value.equals(v.getTag()));}
    private void enter(EditText input,String value)throws Exception{tap(input);long now=SystemClock.uptimeMillis();sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_A,0,KeyEvent.META_CTRL_ON));sendKeySync(new KeyEvent(now,now+30,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_A,0,KeyEvent.META_CTRL_ON));sendStringSync(value);settle();}
    private void tap(View view)throws Exception{tap(view,1);}
    private void tap(View view,int count)throws Exception{settle();Rect rect=new Rect();boolean[] visible={false};ui(()->{int[] screen=new int[2];visible[0]=view.isAttachedToWindow()&&view.hasWindowFocus()&&view.getGlobalVisibleRect(rect);view.getRootView().getLocationOnScreen(screen);rect.offset(screen[0],screen[1]);});check(visible[0]&&rect.width()>0&&rect.height()>0,"real visible touch target "+view.getClass().getSimpleName());for(int n=0;n<count;n++){long down=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,rect.exactCenterX(),rect.exactCenterY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,rect.exactCenterX(),rect.exactCenterY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);}settle();}
    private void settle(){SystemClock.sleep(350);waitForIdleSync();}
    private void scrollTo(String text)throws Exception{for(int n=0;n<15;n++){View[] hit={null};ui(()->hit[0]=find(activity.getWindow().getDecorView(),v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(text)));if(hit[0]!=null){boolean[] full={false};ui(()->{Rect r=new Rect();full[0]=hit[0].getGlobalVisibleRect(r)&&r.height()>=hit[0].getHeight();});if(full[0])return;}View scroll=await(v->v instanceof ScrollView);Rect r=new Rect();ui(()->{int[] screen=new int[2];scroll.getGlobalVisibleRect(r);scroll.getRootView().getLocationOnScreen(screen);r.offset(screen[0],screen[1]);});long start=SystemClock.uptimeMillis();for(int step=0;step<=12;step++){MotionEvent e=MotionEvent.obtain(start,start+step*25,step==0?MotionEvent.ACTION_DOWN:step==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE,r.centerX(),r.top+r.height()*(.8f-.55f*step/12),0);sendPointerSync(e);e.recycle();}settle();}throw new AssertionError("cannot scroll to "+text);}
    private View find(View v,Predicate<View> predicate){Rect r=new Rect();if(!v.isShown()||!v.getGlobalVisibleRect(r)||r.width()<8||r.height()<8)return null;if(predicate.test(v))return v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){View match=find(((ViewGroup)v).getChildAt(n),predicate);if(match!=null)return match;}return null;}
    private View await(Predicate<View> predicate)throws Exception{long end=SystemClock.uptimeMillis()+90000;while(SystemClock.uptimeMillis()<end){View[] result={null};ui(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int n=roots.size()-1;n>=0;n--)if(roots.get(n).hasWindowFocus()&&(result[0]=find(roots.get(n),predicate))!=null)break;});if(result[0]!=null)return result[0];settle();}throw new AssertionError("visible control timed out");}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private World world()throws Exception{Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);return (World)field.get(activity);}
    private byte[] capture(){byte[][] bytes={null};ui(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception failure){throw new RuntimeException(failure);}});return bytes[0];}
    private void check(boolean value,String text)throws Exception{if(!value)throw new AssertionError(text);checks++;log.append("PASS ").append(text).append('\n');write("progress.txt",log.toString());}
    private void write(String name,String text)throws Exception{Files.write(new File(output,name).toPath(),text.getBytes("UTF-8"));}
    private void shot(String name)throws Exception{Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap!=null)try(OutputStream out=new FileOutputStream(new File(output,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
