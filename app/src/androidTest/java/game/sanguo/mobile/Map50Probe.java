package game.sanguo.mobile;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.SystemClock;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

/** Installed official-map evidence; no reference JPEG, substitute renderer or test-only map. */
final class Map50Probe {
    private final Instrumentation test;
    private MainActivity activity;
    private MapView map;
    private World world;
    private int checks;
    private final StringBuilder report=new StringBuilder();
    Map50Probe(Instrumentation test){this.test=test;}
    void run() throws Exception {
        World opening=ScenarioCatalog.load("heroes-250",0);
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(opening));}
        test.getTargetContext().getSharedPreferences("map-display",0).edit().clear().commit();
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        world=(World)field(activity,"world");map=(MapView)field(field(activity,"map"),"flat");
        require(world.height==100&&world.sourceMapWidth==100&&world.cities.size()==87,"official 100x100 87-site map loaded");
        require(MapCoordinates.source(world.city(20048).hex,100).equals(new Hex(6,48)),"corrected Jiange appears in installed game");
        require(MapCoordinates.source(world.city(20050).hex,100).equals(new Hex(5,59)),"corrected Fushui appears in installed game");
        require(MapCoordinates.source(world.city(20070).hex,100).equals(new Hex(88,49)),"corrected QuA appears in installed game");
        byte[] before=SaveCodec.encode(world);
        test.runOnMainSync(()->{page();map.fit();});settle();shot("01-national");
        int[][] centers={{13,30},{38,38},{47,27},{9,51},{41,62},{50,75},{83,51},{69,49}};
        String[] names={"02-northwest","03-guanluo","04-huguan","05-bashu-gates","06-hanjin","07-dongting","08-jiangdong","09-huai"};
        for(int i=0;i<centers.length;i++){
            final Hex center=MapCoordinates.axial(centers[i][0],centers[i][1],world.height);
            test.runOnMainSync(()->{page();map.focus(center);});settle();
            MapCamera camera=(MapCamera)field(map,"camera");
            test.runOnMainSync(()->{camera.zoom(1.35f,map.getWidth()/2f,map.getHeight()/2f);map.center(center);});settle();shot(names[i]);
            int builds=map.sceneBuilds();test.runOnMainSync(()->{for(int j=0;j<12;j++)activity.refresh();});settle();
            require(map.sceneBuilds()==builds,"presentation refresh reuses map scene "+names[i]);
        }
        test.runOnMainSync(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();
        test.runOnMainSync(()->{page();map.fit();});settle();shot("10-landscape-national");
        require(map.getWidth()>map.getHeight(),"landscape map measures correctly");
        require(Arrays.equals(before,SaveCodec.encode(world)),"zoom, pan, refresh and rotation preserve authoritative map and rules");
        File dir=test.getTargetContext().getExternalFilesDir("smoke");
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v050-checks.txt")),"UTF-8")){out.write("PASS: "+checks+" installed map checks\n"+report);}
    }
    private void page(){try{ClientState ui=(ClientState)field(activity,"ui");ui.page="map";ui.panelVisible=false;ui.panelExpanded=false;activity.refresh();}catch(Exception e){throw new RuntimeException(e);}}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(550);test.waitForIdleSync();}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;report.append("PASS ").append(message).append('\n');}
    private void shot(String name)throws Exception{Bitmap image=test.getUiAutomation().takeScreenshot();require(image!=null,"screenshot "+name);File dir=test.getTargetContext().getExternalFilesDir("smoke");if(dir==null)throw new IOException("Missing evidence directory");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,"v050-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
}
