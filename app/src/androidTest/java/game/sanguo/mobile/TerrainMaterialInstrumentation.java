package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Fixed national scenes captured from the actual Filament Surface, not a host preview. */
public class TerrainMaterialInstrumentation extends SceneInstrumentation {
    Hex boundary(World.Terrain type){
        for(int r=8;r<world.height-8;r++)for(int q=8;q<world.width-8;q++){
            Hex h=new Hex(q,r);if(!world.inside(h)||world.terrain[q][r]!=type)continue;
            for(int dq=-1;dq<=1;dq++)for(int dr=-1;dr<=1;dr++){
                Hex n=new Hex(q+dq,r+dr);if(world.inside(n)&&world.terrain[n.q][n.r]==World.Terrain.PLAIN)return h;
            }
        }
        throw new AssertionError("No actual national boundary for "+type);
    }
    @Override void surfaceCapture()throws Exception{
        ready();
        FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
        // Static art evidence must not race continuously submitted software-GPU frames.
        // Freeze only presentation; ready()/flushAndWait below drains the last real frame.
        runOnMainSync(()->spatial.resume(false));
        try{
        super.surfaceCapture();
        android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeFile(new File(getTargetContext().getExternalFilesDir("s01"),"surface.png").getAbsolutePath());
        java.util.Set<Integer> colors=new java.util.HashSet<>();
        for(int y=0;y<bitmap.getHeight();y+=8)for(int x=0;x<bitmap.getWidth();x+=8)colors.add(bitmap.getPixel(x,y));
        bitmap.recycle();check(colors.size()>64,"Surface has real material variation, not a uniform post-process frame");
        }finally{runOnMainSync(()->spatial.resume(true));}
    }
    Hex[] shots(){return new Hex[]{boundary(World.Terrain.FOREST),boundary(World.Terrain.MOUNTAIN),boundary(World.Terrain.SAND),world.cities.get(0).hex};}
    String[] names(){return new String[]{"forest","rock","sand","city"};}
    String stage(){return "S10";}
    void extra(FilamentMapView spatial)throws Exception{}
    @Override public void onStart(){Bundle result=new Bundle();try{
        World w=ScenarioCatalog.all().get(0);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");byte[] original=SaveCodec.encode(world);
        Hex[] shots=shots();
        String[] names=names();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        StringBuilder report=new StringBuilder("name,q,r,span,quality,facing,tilt,uiWidth,uiHeight,bufferWidth,bufferHeight,mapId,mapRevision,revision\n");
        for(String quality:new String[]{"MEDIUM","LOW","HIGH"}){
            runOnMainSync(()->{host.switchMode(false);getTargetContext().getSharedPreferences("map-renderer",0).edit().putString("quality",quality).commit();host.switchMode(true);});settle();ready();
            FilamentMapView spatial=(FilamentMapView)field(host,"spatial");
            check(field(spatial,"groundMaterial")!=null,"real ground material loaded");check(((List<?>)field(spatial,"groundTextures")).size()==8,"all texture layers loaded");
            extra(spatial);
            for(int i=0;i<shots.length;i++)for(float span:new float[]{4,16,64}){
                final Hex h=shots[i];runOnMainSync(()->activity.selectAndFocus(h));settle();
                // selectAndFocus posts a second focus operation; set the shot only after that barrier.
                runOnMainSync(()->{spatial.camera.span=span;spatial.camera.facing=1;});settle();ready();
                check(spatial.camera.span==span,"fixed span survives deferred UI focus");
                check((Boolean)field(spatial,"distantTerrain")== (span>48),"near/far terrain LOD actually selected");
                surfaceCapture();String name=stage().toLowerCase()+"-"+names[i]+"-"+quality+"-"+(int)span;
                Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                capture(name+"-selection");report.append(names[i]+","+h.q+","+h.r+","+spatial.camera.span+","+quality+",1,"+spatial.camera.tilt+","+spatial.camera.width+","+spatial.camera.height+","+field(spatial,"bufferWidth")+","+field(spatial,"bufferHeight")+","+world.mapId+","+world.mapRevision+","+BuildConfig.SOURCE_REVISION+"\n");
                MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");float x=snap.ground.grid.x(h),z=snap.ground.grid.z(h);
                check(h.equals(snap.ground.surface.pick(spatial.camera,spatial.camera.screenX(x),spatial.camera.screenY(z,snap.ground.surface.at(h)))),"rendered shot center picks same gameplay cell");
            }
            runOnMainSync(()->spatial.camera.facing=-1);settle();surfaceCapture();capture(stage().toLowerCase()+"-reverse-"+quality);
        }
        check(Arrays.equals(original,SaveCodec.encode(world)),"all shots and quality switches preserve full save");
        try(OutputStream out=new FileOutputStream(new File(dir,stage().toLowerCase()+"-shots.csv"))){out.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        runOnMainSync(()->host.switchMode(false));
        result.putString("stream","PASS "+stage()+" native materials: "+checks+" checks, "+(shots.length*9)+" national Surface captures; emulator, not physical device.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){
        try{capture("s10-failure-ui");}catch(Throwable captureError){e.addSuppressed(captureError);}
        result.putString("stream","FAIL S10 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
