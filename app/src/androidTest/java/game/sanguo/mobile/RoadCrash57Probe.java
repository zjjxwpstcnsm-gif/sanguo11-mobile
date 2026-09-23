package game.sanguo.mobile;
import game.sanguo.core.map.SourceGridCoord;
import android.app.Instrumentation;
import game.sanguo.core.*;
import java.io.*;

/** Must run against the real unmodified v056 APK. Expected outcome: process FATAL. */
final class RoadCrash57Probe extends MapTap57Harness {
    RoadCrash57Probe(Instrumentation t){super(t);}
    void run()throws Exception{
        World w=ScenarioCatalog.load("coalition-190",5,56L);
        require(w.mapRevision==56,"actual unmodified baseline revision56");
        byte[] save=SaveCodec.encode(w);writeInternal("manual1.sg11",save);
        try(OutputStream o=new FileOutputStream(new File(dir(),"baseline-v056.sg11"))){o.write(save);}
        launch(w);Hex h=MapCoordinates.fromNationalSource(world(),new SourceGridCoord(77,76));
        require(world().terrain[h.q][h.r]==World.Terrain.ROAD&&world().cityAt(h)==null&&world().war.at(h)==null,"verified bare ROAD candidate, not user-reported coordinate");
        focus(h,3.4f);shot("baseline-v056-before-ROAD");record(h,screen(h));flush("baseline-v056-repro.txt");
        tap(h); // Real MapView dispatch should terminate the old process in MainActivity.showTerrain.
    }
}
