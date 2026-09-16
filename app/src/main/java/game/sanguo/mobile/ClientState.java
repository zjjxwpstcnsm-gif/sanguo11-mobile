package game.sanguo.mobile;

import android.os.Bundle;

/** Only navigation/filter state; the engine remains the source of truth. */
final class ClientState {
    String page="map", group="概览", query="", summary="";
    String cityQuery="",taskQuery="",factionQuery="",contentQuery="",contentKind="officers",contentFirstId="";
    int cityOwner=-1,contentTop=0,cityFilter=0,cityDistrict=-1,cityPosition=0,cityTop=0;
    int owner=-1, city=-1, officerSort=0, citySort=0, taskType=0;
    boolean panelVisible=false,panelExpanded=false;
    void read(Bundle b) {
        if(b==null)return;
        cityFilter=b.getInt("cityFilter",0);cityDistrict=b.getInt("cityDistrict",-1);cityPosition=b.getInt("cityPosition",0);cityTop=b.getInt("cityTop",0);
        page=b.getString("page","map");group=b.getString("group","概览");query=b.getString("query","");summary=b.getString("summary","");
        cityQuery=b.getString("cityQuery","");taskQuery=b.getString("taskQuery","");factionQuery=b.getString("factionQuery","");contentQuery=b.getString("contentQuery","");contentKind=b.getString("contentKind","officers");cityOwner=b.getInt("cityOwner",-1);contentFirstId=b.getString("contentFirstId","");contentTop=b.getInt("contentTop",0);
        owner=b.getInt("owner",-1);city=b.getInt("city",-1);officerSort=b.getInt("officerSort");citySort=b.getInt("citySort");taskType=b.getInt("taskType");panelVisible=b.getBoolean("panel",false);panelExpanded=b.getBoolean("panelExpanded",false);
    }
    void write(Bundle b) {
        b.putInt("cityFilter",cityFilter);b.putInt("cityDistrict",cityDistrict);b.putInt("cityPosition",cityPosition);b.putInt("cityTop",cityTop);
        b.putString("page",page);b.putString("group",group);b.putString("query",query);b.putString("summary",summary);
        b.putString("cityQuery",cityQuery);b.putString("taskQuery",taskQuery);b.putString("factionQuery",factionQuery);b.putString("contentQuery",contentQuery);b.putString("contentKind",contentKind);b.putInt("cityOwner",cityOwner);b.putString("contentFirstId",contentFirstId);b.putInt("contentTop",contentTop);
        b.putInt("owner",owner);b.putInt("city",city);b.putInt("officerSort",officerSort);b.putInt("citySort",citySort);b.putInt("taskType",taskType);b.putBoolean("panel",panelVisible);b.putBoolean("panelExpanded",panelExpanded);
    }
}
