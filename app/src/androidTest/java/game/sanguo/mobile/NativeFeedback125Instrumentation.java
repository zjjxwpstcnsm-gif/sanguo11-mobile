package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.Button;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed normal MainActivity, original 120s ready, no renderer/authority bypass. */
public final class NativeFeedback125Instrumentation extends SceneInstrumentation {
    private String mode="taishan";private File dir;
    @Override public void onCreate(Bundle b){if(b!=null)mode=b.getString("mode",mode);super.onCreate(b);}
    private void log(String text)throws Exception{Files.write(new File(dir,"feedback125.txt").toPath(),(text+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    private byte[] authority(){byte[][] b={null};runOnMainSync(()->{try{b[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});return b[0];}
    private Button button(View root,String tag){if(tag.equals(root.getTag())&&root instanceof Button)return (Button)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){Button b=button(group.getChildAt(i),tag);if(b!=null)return b;}}return null;}
    private void installedBattle()throws Exception{
        final Button[] buttons=new Button[3];
        runOnMainSync(()->{
            World view=SessionProbe.view(activity);invoke("showTactics",new Class<?>[]{World.Unit.class},view.unit(1));
            try{AlertDialog dialog=(AlertDialog)field(activity,"confirmationDialog");View root=dialog.getWindow().getDecorView();
                buttons[0]=button(root,"choice.CHARGE");buttons[1]=button(root,"choice.BREAKTHROUGH");buttons[2]=button(root,"choice.ADVANCE");
                check(buttons[0]!=null&&buttons[0].isEnabled(),"B cavalry can choose CHARGE");
                for(int i=1;i<3;i++)check(buttons[i]!=null&&!buttons[i].isEnabled()&&buttons[i].getCurrentTextColor()==0xff888888,"insufficient rank rendered grey and disabled");
            }catch(Exception e){throw new RuntimeException(e);}
        });settle();capture("battle-grey-disabled-2d");
        byte[] before=authority();
        int[] loc=new int[2];runOnMainSync(()->buttons[1].getLocationOnScreen(loc));
        long time=SystemClock.uptimeMillis();float x=loc[0]+buttons[1].getWidth()/2f,y=loc[1]+buttons[1].getHeight()/2f;
        MotionEvent event=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0);sendPointerSync(event);event.recycle();
        event=MotionEvent.obtain(time,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);sendPointerSync(event);event.recycle();settle();
        check(Arrays.equals(before,authority()),"real tap on unavailable tactic has no command, cost or RNG effect");
        runOnMainSync(()->{try{((AlertDialog)field(activity,"confirmationDialog")).dismiss();}catch(Exception e){throw new RuntimeException(e);}});
        World high=SaveCodec.decode(before);Arrays.fill(high.officer(1).aptitude,3);
        World expected=SaveCodec.decode(SaveCodec.encode(high));World.Result expectedResult=expected.war.tactic(1,2,War.Tactic.BREAKTHROUGH);check(expectedResult.ok,"headless blocked breakthrough castable");
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},high);activity.refresh();
            World.Result r=SessionProbe.command(activity,w->w.war.tactic(1,2,War.Tactic.BREAKTHROUGH));check(r.ok,"actual GameSession blocked breakthrough");activity.refresh();});settle();
        check(Arrays.equals(SaveCodec.encode(expected),authority()),"installed command equals exact headless full save/RNG");
        capture("battle-blocked-result-2d");log("PASS INSTALLED_BATTLE 2D UI disabled tap and GameSession damage/optional landing/save RNG checks="+checks);
        // Separate the actual 3D rendering gate: a loading failure cannot become
        // an installed gameplay PASS or hide the earlier completed UI checks.
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},SaveUnchecked.decode(before));activity.refresh();invoke("closePanel",new Class<?>[0]);host.switchMode(true);});settle();ready();
        surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"battle-3d-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("battle-grey-3d-scene");
        runOnMainSync(()->invoke("showTactics",new Class<?>[]{World.Unit.class},SessionProbe.view(activity).unit(1)));settle();capture("battle-grey-disabled-3d");
        runOnMainSync(()->{try{((AlertDialog)field(activity,"confirmationDialog")).dismiss();}catch(Exception e){throw new RuntimeException(e);}
            World high3d=SaveUnchecked.decode(before);Arrays.fill(high3d.officer(1).aptitude,3);invoke("activateWorld",new Class<?>[]{World.class},high3d);activity.refresh();
            World.Result r=SessionProbe.command(activity,w->w.war.tactic(1,2,War.Tactic.BREAKTHROUGH));check(r.ok,"actual 3D GameSession blocked breakthrough");activity.refresh();});settle();
        check(Arrays.equals(SaveCodec.encode(expected),authority()),"same 2D/3D command complete save and RNG");capture("battle-blocked-result-3d");
    }
    static final class SaveUnchecked {static World decode(byte[] b){try{return SaveCodec.decode(b);}catch(IOException e){throw new RuntimeException(e);}}}
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        World seed;
        if(mode.equals("battle")){seed=DisplacementFixture.create("mountain",World.Weapon.CAVALRY);Arrays.fill(seed.officer(1).aptitude,1);}
        else seed=ScenarioCatalog.all().get(0);
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(seed));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).putString("quality","MEDIUM").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
        if(mode.equals("battle"))installedBattle();
        else{
            int[] p=mode.equals("southwest")?new int[]{31,183}:new int[]{147,59};
            Hex focus=MapCoordinates.fromNationalSource(world,new SourceGridCoord(p[0],p[1]));byte[] before=authority();
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.setGridShown(false);host.setTerritoryMode(0);activity.selectAndFocus(focus);host.switchMode(true);});settle();
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");runOnMainSync(()->{renderer.center(focus);renderer.camera.span=6;renderer.camera.tilt=55;});ready();
            for(int span:new int[]{3,6,10})for(int yaw:new int[]{0,180}){
                runOnMainSync(()->{renderer.camera.span=span;renderer.camera.yaw=yaw;});settle();ready();
                for(boolean grid:new boolean[]{false,true}){
                    runOnMainSync(()->host.setGridShown(grid));settle();surfaceCapture();String id=mode+"-span"+span+"-yaw"+yaw+"-grid"+grid;
                    Files.copy(new File(dir,"surface.png").toPath(),new File(dir,id+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture(id+"-ui");
                }
            }
            int water=0;for(Object gpu:((Map<?,?>)field(renderer,"vegetation")).values()){
                SceneMesh mesh=(SceneMesh)field(gpu,"source");for(int i=0;i<mesh.uv.length;i+=2)if(mesh.uv[i]>.625f&&mesh.uv[i]<.75f)water++;
            }
            check(water>0,"actual production GPU landscape contains cascade geometry");
            check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no fallback");
            check(Arrays.equals(before,authority()),"all map views preserve entire game and RNG");
            Files.write(new File(dir,"expected.sg11").toPath(),before);Files.write(new File(dir,"observed.sg11").toPath(),authority());
            log("GPU cascadeVertices="+water+" source="+Arrays.toString(p)+"\n"+host.report());
        }
        log("PASS FEEDBACK125 mode="+mode+" checks="+checks);result.putString("stream","PASS FEEDBACK125 "+mode+" checks="+checks+"\n");
    }catch(Throwable e){result.putString("stream","FAIL FEEDBACK125 "+android.util.Log.getStackTraceString(e));try{log(result.getString("stream")+"\n"+(host==null?"no host":host.report()));capture(mode+"-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}
