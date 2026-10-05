package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed PC landscape, actual Filament surfaces and authority isolation. */
public final class PcMapInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){Bundle result=new Bundle();File dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        try{
            try(InputStream in=getTargetContext().getAssets().open("3d/pc-map/texture-indices.png")){
                android.graphics.Bitmap index=android.graphics.BitmapFactory.decodeStream(in);
                check(((index.getPixel(432,420)>>16)&255)==28,"Android preserves numeric PC texture IDs");index.recycle();
            }
            try(InputStream in=getTargetContext().getAssets().open("3d/pc-map/face-low.png")){
                android.graphics.Bitmap face=android.graphics.BitmapFactory.decodeStream(in);
                check((face.getPixel(432,420)&0xffffff)==0xb53de7,"Android preserves original near face bytes");face.recycle();
            }
            World seed=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(seed);
            try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(before);}
            getTargetContext().getSharedPreferences("map-renderer",0).edit().clear().putString("quality","MEDIUM").commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            host=(MapHost)field(activity,"map");world=SessionProbe.view(activity);
            runOnMainSync(()->{try{
                ScenarioFactionPicker picker=new ScenarioFactionPicker(activity,world,side->{});
                ((android.widget.Button)field(picker,"start")).performClick();
                check((Boolean)field(activity,"nextScenario3D"),"normal PC faction confirmation defaults to 3D");
                ((MapHost)field(picker,"map")).release();
            }catch(Exception e){throw new RuntimeException(e);}});
            runOnMainSync(()->{invoke("closePanel",new Class<?>[0]);host.switchMode(true);host.setGridShown(false);});settle();
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");
            int[][] places={{79,76},{123,66},{27,174}};String[] names={"luoyang","river-port","southwest"};
            for(int i=0;i<places.length;i++){
                final Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(places[i][0],places[i][1]));
                runOnMainSync(()->{renderer.focus(h);renderer.camera.span=15;});settle();ready();
                check(((MapSceneSnapshot)field(renderer,"snapshot")).ground.pcMap!=null,"PC geometry resident");
                check(field(renderer,"pcGroundMaterial")!=null,"original texture material resident");
                check((Integer)field(renderer,"pcSeason")>=0,"seasonal PC textures resident");
                check(((Set<?>)field(renderer,"missingAssets")).isEmpty(),"no missing assets");
                surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-"+names[i]+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("pc-"+names[i]+"-ui");
                Files.write(new File(dir,"pc-map-report.txt").toPath(),(names[i]+"\n"+host.report()+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }
            byte[][] after={null};runOnMainSync(()->{try{after[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
            check(Arrays.equals(before,after[0]),"3D map viewing preserves complete authority and RNG");
            runOnMainSync(()->host.switchMode(false));
            check(((Map<?,?>)field(renderer,"pcTextureSizes")).isEmpty(),"PC textures released with native engine");
            result.putString("stream","PASS PC MAP installed checks="+checks+"\n");
            Files.write(new File(dir,"pc-map-report.txt").toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.APPEND);
        }catch(Throwable e){result.putString("stream","FAIL PC MAP "+android.util.Log.getStackTraceString(e));try{capture("pc-map-failed");}catch(Exception ignored){}}
        finish(Activity.RESULT_OK,result);
    }
}
