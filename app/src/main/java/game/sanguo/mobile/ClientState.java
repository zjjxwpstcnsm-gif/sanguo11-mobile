package game.sanguo.mobile;

import android.os.Bundle;

/** Only navigation/filter state; the engine remains the source of truth. */
final class ClientState {
    String page="map", group="概览", query="", summary="";
    int owner=-1, city=-1, officerSort=0, citySort=0, taskType=0;
    boolean panelVisible=true;
    void read(Bundle b) {
        if(b==null)return;
        page=b.getString("page","map");group=b.getString("group","概览");query=b.getString("query","");summary=b.getString("summary","");
        owner=b.getInt("owner",-1);city=b.getInt("city",-1);officerSort=b.getInt("officerSort");citySort=b.getInt("citySort");taskType=b.getInt("taskType");panelVisible=b.getBoolean("panel",true);
    }
    void write(Bundle b) {
        b.putString("page",page);b.putString("group",group);b.putString("query",query);b.putString("summary",summary);
        b.putInt("owner",owner);b.putInt("city",city);b.putInt("officerSort",officerSort);b.putInt("citySort",citySort);b.putInt("taskType",taskType);b.putBoolean("panel",panelVisible);
    }
}
