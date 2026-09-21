package game.sanguo.mobile;

import android.content.Context;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small map-independent boundary: the caller supplies the already resolved World/site collection. */
final class CustomOfficerSetup {
    static CustomOfficers.Definition definition(JSONObject o)throws JSONException{
        CustomOfficers.Definition d=new CustomOfficers.Definition();d.id=o.getString("id");d.runtimeId=o.getInt("runtimeId");d.revision=o.getInt("revision");d.template=CustomOfficerLibrary.template(o);d.targetId=o.optInt("targetId",-1);d.baseName=o.optString("baseName","");
        d.birth=o.optInt("birth",0);d.appearance=o.optInt("appearance",0);d.death=o.optInt("death",0);d.affinity=o.optInt("affinity",75);d.honor=o.optInt("honor",3);d.portrait=o.optString("portrait","");d.replaceRelations=o.optBoolean("replaceRelations",false);
        JSONArray links=o.optJSONArray("relationships");if(links!=null)for(int i=0;i<links.length();i++){JSONObject l=links.getJSONObject(i);d.links.add(new CustomOfficers.Link(Relations.Kind.valueOf(l.getString("kind")),l.getString("target")));}return d;
    }
    static JSONObject json(CustomOfficers.Definition d)throws JSONException{
        JSONObject o=CustomOfficerLibrary.definition(d.template).put("id",d.id).put("runtimeId",d.runtimeId).put("revision",d.revision).put("targetId",d.targetId).put("baseName",d.baseName).put("birth",d.birth).put("appearance",d.appearance).put("death",d.death).put("affinity",d.affinity).put("honor",d.honor).put("portrait",d.portrait).put("replaceRelations",d.replaceRelations);
        JSONArray links=new JSONArray();for(CustomOfficers.Link l:d.links)links.put(new JSONObject().put("kind",l.kind.name()).put("target",l.target));return o.put("relationships",links);
    }
    static List<CustomOfficers.Definition> definitions(JSONObject root)throws JSONException{
        List<CustomOfficers.Definition> defs=new ArrayList<>();JSONArray values=root.getJSONArray("entries");for(int i=0;i<values.length();i++)defs.add(definition(values.getJSONObject(i)));return defs;
    }
    static JSONObject plan(JSONObject root,String scenario)throws JSONException{
        JSONArray plans=root.getJSONArray("plans");for(int i=0;i<plans.length();i++)if(plans.getJSONObject(i).getString("scenario").equals(scenario))return plans.getJSONObject(i);
        return new JSONObject().put("id",UUID.randomUUID().toString()).put("scenario",scenario).put("enabled",false).put("placements",new JSONArray());
    }
    static World apply(Context context,World resolved)throws IOException{
        try{return apply(context,resolved,new CustomOfficerLibrary(context).snapshot());}catch(JSONException|IllegalArgumentException e){throw new IOException(e.getMessage(),e);}
    }
    static World apply(Context context,World resolved,JSONObject root)throws IOException,JSONException{
        JSONObject p=plan(root,resolved.scenarioId);if(!p.optBoolean("enabled",false))return resolved;
        List<CustomOfficers.Definition> defs=definitions(root);List<CustomOfficers.Placement> placements=new ArrayList<>();Set<String> ids=new HashSet<>();JSONArray values=p.getJSONArray("placements");
        for(int i=0;i<values.length();i++){JSONObject item=values.getJSONObject(i);ids.add(item.getString("definition"));placements.add(new CustomOfficers.Placement(item.getString("definition"),CustomOfficers.Mode.valueOf(item.getString("mode")),item.optInt("owner",-1),item.getInt("city"),item.optInt("loyalty",85),item.optBoolean("ignoreDates",false)));}
        JSONArray selected=new JSONArray();for(JSONObject o:new CustomOfficerLibraryView(root).entries)if(ids.contains(o.getString("id")))selected.put(o);
        for(CustomOfficers.Definition d:defs)if(ids.contains(d.id)&&d.portrait.endsWith(".png"))d.portraitPng=CustomOfficerImages.read(context,d.portrait);
        JSONObject snapshot=new JSONObject().put("version",1).put("libraryId",root.getString("libraryId")).put("plan",p).put("definitions",selected);
        return CustomOfficers.apply(resolved,defs,placements,snapshot.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static final class CustomOfficerLibraryView {
        final List<JSONObject> entries=new ArrayList<>();CustomOfficerLibraryView(JSONObject root)throws JSONException{JSONArray array=root.getJSONArray("entries");for(int i=0;i<array.length();i++)entries.add(array.getJSONObject(i));}
    }
    static void putPlan(JSONObject root,JSONObject plan)throws JSONException{
        JSONArray plans=root.getJSONArray("plans");for(int i=0;i<plans.length();i++)if(plans.getJSONObject(i).getString("scenario").equals(plan.getString("scenario"))){plans.put(i,plan);return;}plans.put(plan);
    }
}
