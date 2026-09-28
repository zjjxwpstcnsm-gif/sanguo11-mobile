package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Production activity and data. Camera API tour is labelled separately from physical UI touches. */
public final class NativeR15Instrumentation extends SceneInstrumentation {
    private String mode="tour",scenario="coalition-190";private File dir;private boolean listOnly;
    @Override public void onCreate(Bundle args){if(args!=null){mode=args.getString("mode",mode);scenario=args.getString("scenario",scenario);listOnly="true".equals(args.getString("log"));}super.onCreate(args);}
    private Bundle status(){Bundle b=new Bundle();b.putString("id","InstrumentationTestRunner");b.putString("class",getClass().getName());b.putString("test",mode);b.putInt("numtests",1);b.putInt("current",1);return b;}
    private void log(String s)throws Exception{Files.write(new File(dir,"r15-"+mode+".txt").toPath(),(s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private Rect bounds(View v){Rect r=new Rect();if(!v.isShown()||!v.isEnabled()||!v.hasWindowFocus()||!v.getLocalVisibleRect(r))return null;int[] xy=new int[2];v.getLocationOnScreen(xy);r.offset(xy[0],xy[1]);return r.width()>16&&r.height()>16?r:null;}
    private View scan(View v,Predicate<View> p){if(!v.isShown())return null;if(p.test(v)&&bounds(v)!=null)return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=scan(((ViewGroup)v).getChildAt(i),p);if(found!=null)return found;}return null;}
    private View find(Predicate<View> p){View[] result={null};runOnMainSync(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--){View r=roots.get(i);if(r.hasWindowFocus()){result[0]=scan(r,p);if(result[0]!=null)break;}}});return result[0];}
    private void pointer(float x,float y,int action,long down,long time){MotionEvent e=MotionEvent.obtain(down,time,action,x,y,0);sendPointerSync(e);e.recycle();}
    private void tap(View v)throws Exception{check(v!=null,"visible touch target");Rect[] r={null};runOnMainSync(()->r[0]=bounds(v));check(r[0]!=null,"focused visible bounds");log("TOUCH "+v.getTag()+" "+r[0]);long t=SystemClock.uptimeMillis();pointer(r[0].exactCenterX(),r[0].exactCenterY(),0,t,t);pointer(r[0].exactCenterX(),r[0].exactCenterY(),1,t,t+80);settle();}
    private View tag(String tag){return find(v->tag.equals(v.getTag()));}
    private void swipe(View v)throws Exception{check(v!=null,"scroll viewport exists");Rect[] b={null};runOnMainSync(()->b[0]=bounds(v));check(b[0]!=null,"scroll viewport has height");Rect r=b[0];float x=r.exactCenterX(),from=r.top+r.height()*.8f,to=r.top+r.height()*.2f;long t=SystemClock.uptimeMillis();pointer(x,from,0,t,t);for(int i=1;i<=10;i++)pointer(x,from+(to-from)*i/10,2,t,t+i*35);pointer(x,to,1,t,t+380);settle();}
    private void layout()throws Exception{
        // Explicit orientation setups; all wizard choices/scrolling/confirmation are real MotionEvents.
        for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}){
            runOnMainSync(()->activity.setRequestedOrientation(orientation));settle();settle();
            runOnMainSync(()->world=SessionProbe.view(activity));World.City city=null;
            for(World.City c:world.cities)if(c.owner==world.player&&!world.idle(c).isEmpty()&&c.troops>=1000){city=c;break;}
            check(city!=null,"natural city with idle crew");final World.City chosen=city;byte[] before=SaveCodec.encode(world);int oldUnits=world.units.size();
            runOnMainSync(()->new DeployWizard(activity,world,null,DeployWizard.start(activity,chosen,false)).show());settle();
            capture("r15-layout-"+orientation+"-initial");
            View select=null;for(int i=0;i<12;i++){select=find(v->v instanceof Button&&"选用".contentEquals(((Button)v).getText()));if(select!=null)break;swipe(tag("deploy.officers"));}
            check(select!=null,"roster choice reachable by scrolling");String selectedTag=String.valueOf(select.getTag());int officer=Integer.parseInt(selectedTag.substring("deploy.role.".length()));tap(select);
            capture("r15-layout-"+orientation+"-selected");
            tap(tag("deploy.tab.1"));View sword=find(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(World.Weapon.SWORD.label));tap(sword);
            tap(tag("deploy.tab.2"));capture("r15-layout-"+orientation+"-details");
            tap(tag("deploy.confirm"));runOnMainSync(()->world=SessionProbe.view(activity));
            check(world.units.size()==oldUnits+1,"one physical confirmation deploys exactly once");
            World.Unit unit=world.unit(world.officer(officer).unitId);check(unit!=null,"selected header-offset officer commands unit");
            World reference=SaveCodec.decode(before);World.Result expected=reference.army.deploy(chosen.id,officer,new int[0],unit.weapon,unit.ship,unit.troops,unit.food,unit.gold);
            check(expected.ok,"reference command valid");check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(world)),"touch deployment equals unchanged rule command and full RNG/save");
            capture("r15-layout-"+orientation+"-deployed");log("LAYOUT_PASS orientation="+orientation+" officer="+officer+" actual="+activity.getResources().getConfiguration()+" fullSaveParity=true");
        }
    }
    private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void camera(Hex at,float span,float yaw)throws Exception{runOnMainSync(()->{try{FilamentMapView f=view();f.center(at);f.camera.span=span;f.camera.yaw=yaw;f.camera.tilt=48;}catch(Exception e){throw new RuntimeException(e);}});settle();ready();}
    private void shot(String name)throws Exception{
        ready();long end=SystemClock.uptimeMillis()+120000;boolean[] loaded={false};
        while(SystemClock.uptimeMillis()<end){runOnMainSync(()->{try{FilamentMapView f=view();loaded[0]=!(Boolean)field(f,"assetSyncPending")&&((SceneAssetQueue)field(f,"assetWork")).pending()==0;}catch(Exception e){throw new RuntimeException(e);}});if(loaded[0])break;settle();}
        check(loaded[0],"visible asset queue ready");check(((Set<?>)field(view(),"missingAssets")).isEmpty(),"no missing asset fallback");
        surfaceCapture();capture(name+"-ui");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
        log("SHOT "+name+" source="+BuildConfig.SOURCE_REVISION+" scenario="+scenario+" season="+world.date()+" quality="+host.quality()+" grid="+host.gridShown()+"\n"+host.report());
    }
    private void tour()throws Exception{
        runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);invoke("closePanel",new Class<?>[0]);});
        byte[] before=SaveCodec.encode(world);settle();runOnMainSync(()->host.fit());settle();shot("r15-overview");
        List<R15TourPlan.Stop> stops=R15TourPlan.regions(world);check(stops.size()>=12,"at least twelve data-driven regions");
        for(R15TourPlan.Stop stop:stops){log("REGION "+stop.id+" "+stop.hex+" "+stop.reason);
            for(float span:new float[]{5,14,30})for(float yaw:new float[]{0,90}){camera(stop.hex,span,yaw);shot("r15-"+stop.id+"-s"+(int)span+"-y"+(int)yaw);}
            runOnMainSync(()->host.setGridShown(true));shot("r15-"+stop.id+"-grid");runOnMainSync(()->host.setGridShown(false));
        }
        // Every anchor gets near/far GPU loading checks and snapshots, separate from sampled region art.
        for(World.City c:world.cities)for(float span:new float[]{5,52}){camera(c.hex,span,0);shot("r15-site-"+c.id+"-s"+(int)span);}
        runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"complete camera tour preserves authority/RNG");
        log("TOUR_COMPLETE regions="+stops.size()+" sites="+world.cities.size()+"; visual review required, not automatically V3 PASS");
    }
    @Override public void onStart(){Bundle result=new Bundle();sendStatus(1,status());if(listOnly){sendStatus(0,status());finish(Activity.RESULT_OK,result);return;}
        boolean pass=false;try{
            dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
            getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            int player=scenario.equals("coalition-190")?5:0;
            runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},scenario,player));
            long end=SystemClock.uptimeMillis()+120000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();host=(MapHost)field(activity,"map");check(host!=null,"normal game host exists");
            if(mode.equals("layout"))layout();else if(mode.equals("tour"))tour();else if(mode.equals("combined")){layout();tour();}else throw new IllegalArgumentException(mode);
            pass=true;result.putString("stream","PASS R15 "+mode+" checks="+checks+"; art and physical performance separate\n");
        }catch(Throwable e){String error=android.util.Log.getStackTraceString(e);result.putString("stream","FAIL R15 "+mode+" "+error);try{log(error);capture("r15-"+mode+"-failed");}catch(Exception ignored){}}
        Bundle done=status();done.putString("stream",result.getString("stream"));if(!pass)done.putString("stack",result.getString("stream"));sendStatus(pass?0:-2,done);finish(Activity.RESULT_OK,result);
    }
}
