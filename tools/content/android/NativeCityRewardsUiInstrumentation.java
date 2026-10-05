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
public final class NativeCityRewardsUiInstrumentation extends Instrumentation {
    private MainActivity activity;private File output;private int checks;private String run="native-city-rewards-r28";private final StringBuilder log=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)run=args.getString("run",run).replaceAll("[^a-zA-Z0-9_-]","_");start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("rule-ui"),run);output.mkdirs();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        byte[] original=capture();check(((original[7]&255)==35)&&world().merchantMarket.enabled(),"actual normal v35 campaign loaded");
        check(Arrays.equals(original,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"external test-owned slot3 matches full authority");
        cityAction("PATROL","内政","巡察",0);restore(original);
        cityAction("TRAIN","军事","训练",1);restore(original);
        build("兵舍");cityAction("RECRUIT","军事","征兵",4);restore(original);
        build("锻冶所");production("SPEAR","枪兵");advance();production("HALBERD","戟兵");advance();production("CROSSBOW","弩兵");restore(original);
        build("厩舍");production("CAVALRY","骑兵");restore(original);
        check(Arrays.equals(original,capture()),"normal load restores full v35 fixed rewards/market/base/RNG state");
        result.putString("stream","NATIVE CITY REWARDS UI PASS "+checks+" checks\n"+log);write("result.txt",result.getString("stream"));finish(Activity.RESULT_OK,result);
    }catch(Throwable failure){try{shot("FAIL");write("failure.txt",log+"\n"+failure);}catch(Throwable ignored){}StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));result.putString("stream","NATIVE CITY REWARDS UI FAIL "+trace);finish(Activity.RESULT_CANCELED,result);}}
    private ListView roster()throws Exception{return (ListView)await(v->v instanceof ListView&&((ListView)v).getAdapter()!=null&&((ListView)v).getAdapter().getCount()>0&&((ListView)v).getAdapter().getItem(0) instanceof World.Officer);}
    private void open(String group,String command)throws Exception{nav("地图");description("定位己方据点 "+world().home().name);text("展开");text(group);scrollTo(command);text(command);}
    private void cityAction(String operation,String group,String label,int stat)throws Exception{
        open(group,label);ListView roster=roster();int actor=((World.Officer)roster.getAdapter().getItem(0)).id,city=world().home().id;
        byte[] before=capture();World initial=world();int xp=initial.officerAbilities.experience(actor,stat),merit=initial.government.merit(actor);long rng=initial.strategy.getRandomState();int[] base=new int[5],growth=new int[5];for(int n=0;n<5;n++){base[n]=initial.officerAbilities.base(actor,n);growth[n]=initial.officerAbilities.growthCode(actor,n);}
        CityActionPreview[] forecast={null};ui(()->forecast[0]=activity.previewCityAction(new CityActionCommand(activity.deploymentState(),operation,city,actor,new int[0])));CityActionPreview p=forecast[0];
        check(p.allowed(),"real "+operation+" authority allowed");var effect=p.effects.officers.stream().filter(o->o.id==actor).findFirst().get();check(effect.experience.managed&&effect.experience.stat==stat&&effect.experience.experienceAfter==Math.min(3000,xp+2)&&effect.meritAfter==Math.max(merit,Math.min(60000,merit+50)),"source fixed XP2/merit50 forecast");
        tap(roster.getChildAt(0));check(Arrays.equals(before,capture()),"real city confirmation is pure");shot(operation+"-review");text("返回修改");check(Arrays.equals(before,capture())&&roster()==roster,"real city cancel retains actor and complete authority");tap(roster.getChildAt(0));long revision=activity.deploymentState().revision;tap(awaitText("执行"),2);
        World after=world();check(activity.deploymentState().revision==revision+1,"double city confirmation commits once");check(after.officerAbilities.experience(actor,stat)==Math.min(3000,xp+2)&&after.government.merit(actor)==Math.max(merit,Math.min(60000,merit+50))&&after.officerAbilities.experience(actor,stat)==effect.experience.experienceAfter,"actual normal city action pays original fixed rewards");
        check(after.city(city).gold==p.resources.goldRemaining&&after.actionPoints[after.player]==p.resources.actionPointsRemaining&&OfficerCurrent(after.officer(actor),stat)==effect.experience.currentAfter&&after.strategy.getRandomState()==rng,"real city resources/current/RNG follow authority");for(int n=0;n<5;n++)check(after.officerAbilities.base(actor,n)==base[n]&&after.officerAbilities.growthCode(actor,n)==growth[n],"real city command retains saved base/growth");shot(operation+"-committed");
    }
    private int OfficerCurrent(World.Officer o,int stat){return stat==0?o.leadership:stat==1?o.war:stat==2?o.intelligence:stat==3?o.politics:o.charm;}
    private void production(String item,String label)throws Exception{
        open("军事","生产兵装");tap(awaitText(label));ListView roster=roster();int actor=((World.Officer)roster.getAdapter().getItem(0)).id,city=world().home().id;
        byte[] before=capture();World initial=world();int xp=initial.officerAbilities.experience(actor,2),merit=initial.government.merit(actor);long rng=initial.strategy.getRandomState();int base=initial.officerAbilities.base(actor,2),growth=initial.officerAbilities.growthCode(actor,2);
        ProductionPreview[] forecast={null};ui(()->forecast[0]=activity.previewProduction(new ProductionCommand(activity.deploymentState(),"EQUIPMENT",city,actor,item)));ProductionPreview p=forecast[0];check(p.allowed()&&!p.effects.delayed&&p.effects.experience.managed&&p.effects.experience.stat==2,"real immediate "+item+" source reward forecast");
        tap(roster.getChildAt(0));check(Arrays.equals(before,capture()),"production review pure");text("返回修改");check(Arrays.equals(before,capture())&&roster()==roster,"production cancel retains actor and authority");tap(roster.getChildAt(0));shot(item+"-review");long revision=activity.deploymentState().revision;tap(awaitText("执行"),2);
        World after=world();check(activity.deploymentState().revision==revision+1&&after.officerAbilities.experience(actor,2)==Math.min(3000,xp+2)&&after.government.merit(actor)==Math.max(merit,Math.min(60000,merit+50)),"double normal production pays XP2/merit50 exactly once");check(after.city(city).gold==p.effects.goldAfter&&after.actionPoints[after.player]==p.effects.actionPointsAfter&&after.city(city).equipment[World.Weapon.valueOf(item).ordinal()]==p.effects.stockAfterImmediate&&after.officer(actor).intelligence==p.effects.experience.currentAfter&&after.strategy.getRandomState()==rng,"normal production resources/current/stock/RNG agree");check(after.officerAbilities.base(actor,2)==base&&after.officerAbilities.growthCode(actor,2)==growth,"production retains base/growth");shot(item+"-committed");
    }
    private Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private void advance()throws Exception{nav("地图");int turn=world().turn;text("下一旬");text("执行");text("演示控制");tap(awaitText("跳过剩余演示"));long end=SystemClock.uptimeMillis()+180000;while(field(activity,"turnWork")!=null&&SystemClock.uptimeMillis()<end)settle();check(field(activity,"turnWork")==null&&world().turn==turn+1,"real full turn progresses exactly once");}
    private void build(String facility)throws Exception{
        nav("地图");description("定位己方据点 "+world().home().name);text("设施开发");settle();World initial=world();Hex target=null;float[] point=null;
        for(Hex h:initial.domestic.buildSites(initial.home().id)){float[] p=screenHex(h);if(visibleMapPoint(p)){target=h;point=p;break;}}
        check(target!=null,"normal 3D campaign has visible legal development parcel");tapPoint(point[0],point[1]);EditText search=(EditText)await(v->v instanceof EditText&&"建设搜索".contentEquals(v.getContentDescription()));enter(search,facility);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();tap(awaitText(facility+" ·"));World.Officer actor=initial.idle(initial.home()).stream().max(Comparator.comparingInt(o->o.politics)).get();tap(awaitText(actor.name+" · 政治"));text("开工");check(world().domestic.at(target)!=null,"real normal build starts "+facility);
        for(int n=0;n<4&&world().domestic.at(target).remaining>0;n++)advance();check(world().domestic.at(target).remaining==0,"real normal turns finish "+facility);shot(facility+"-ready");
    }
    private boolean visibleMapPoint(float[] point)throws Exception{
        boolean[] visible={false};ui(()->{try{View map=(View)field(activity,"map");int[] origin=new int[2];map.getLocationOnScreen(origin);int margin=UiTheme.dp(activity,32);Rect free=new Rect(origin[0]+margin,origin[1]+margin,origin[0]+map.getWidth()-margin,origin[1]+map.getHeight()-margin);visible[0]=free.contains((int)point[0],(int)point[1]);for(String name:new String[]{"commandDock","panelShell"}){View overlay=(View)field(activity,name);if(overlay.isShown()){overlay.getLocationOnScreen(origin);visible[0]&=!new Rect(origin[0]-margin,origin[1]-margin,origin[0]+overlay.getWidth()+margin,origin[1]+overlay.getHeight()+margin).contains((int)point[0],(int)point[1]);}}}catch(Exception e){throw new RuntimeException(e);}});return visible[0];
    }
    private float[] screenHex(Hex h)throws Exception{
        float[][] point={null};ui(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView spatial=(FilamentMapView)field(host,"spatial");if(spatial==null)throw new AssertionError("actual pure3D renderer required");MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");int[] origin=new int[2];spatial.getLocationOnScreen(origin);float x=snap.ground.grid.x(h),z=snap.ground.grid.z(h);point[0]=new float[]{origin[0]+spatial.camera.screenX(x,z,snap.ground.surface.at(h)),origin[1]+spatial.camera.screenY(x,z,snap.ground.surface.at(h))};}catch(Exception e){throw new RuntimeException(e);}});return point[0];
    }
    private void tapPoint(float x,float y){long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);}settle();}
    private void restore(byte[] expected)throws Exception{nav("菜单");text("读取存档");tap(awaitText("槽位 3"));text("读取存档");long end=SystemClock.uptimeMillis()+90000;while(!Arrays.equals(expected,capture())&&SystemClock.uptimeMillis()<end)settle();check(Arrays.equals(expected,capture()),"real menu/slot load restores complete campaign");}
    private void nav(String name)throws Exception{description("打开功能导航");description("导航 · "+name);settle();}
    private void description(String value)throws Exception{tap(await(v->v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(value)));}
    private void text(String value)throws Exception{tap(await(v->v.isClickable()&&v instanceof TextView&&((TextView)v).getText().toString().startsWith(value)));}
    private View awaitText(String value)throws Exception{return await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(value));}
    private View tag(String value)throws Exception{return await(v->value.equals(v.getTag()));}
    private void enter(EditText input,String value)throws Exception{tap(input);long now=SystemClock.uptimeMillis();sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_A,0,KeyEvent.META_CTRL_ON));sendKeySync(new KeyEvent(now,now+30,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_A,0,KeyEvent.META_CTRL_ON));ui(()->((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("test-owned input",value)));now=SystemClock.uptimeMillis();sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_V,0,KeyEvent.META_CTRL_ON));sendKeySync(new KeyEvent(now,now+30,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_V,0,KeyEvent.META_CTRL_ON));settle();check(input.getText().toString().equals(value),"real paste supplies complete Chinese input");}
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
