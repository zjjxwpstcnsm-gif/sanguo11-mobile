package game.sanguo.mobile;

import android.util.Log;
import game.sanguo.core.*;

/** Opt-in, per-tap diagnostics: adb shell setprop log.tag.MapTap57 DEBUG.
 * No frame logging, no visible debug UI, and no changes to authoritative state. */
final class MapTapTrace {
    static final String TAG="MapTap57";
    private MapTapTrace() {}
    static void tap(World w,Hex h,float x,float y,float scale,float density) {
        if(!Log.isLoggable(TAG,Log.DEBUG))return;
        Log.d(TAG,"callback=MapView.onSingleTapUp screen="+x+","+y+" scale="+scale+
                " lod="+(scale*TileGeometry.RADIUS>=12*density?"DETAIL":"OVERVIEW")+" "+describe(w,h));
    }
    static void detail(String callback,World w,Hex h) {
        if(Log.isLoggable(TAG,Log.DEBUG))Log.d(TAG,"callback="+callback+" "+describe(w,h));
    }
    private static String describe(World w,Hex h) {
        if(w==null||h==null)return "hit=none";
        War.Structure s=w.war.at(h);
        return "scenario="+w.scenarioId+" map="+w.mapId+" revision="+w.mapRevision+
                " national="+MapCoordinates.nationalSource(w,h)+" local="+MapCoordinates.source(w,h)+
                " axial="+h+" terrain="+(w.sourceInside(h)?w.terrain[h.q][h.r]:"PADDING")+
                " structure="+(s==null?"none":s.kind+"/"+s.id+"/owner="+s.owner);
    }
}
