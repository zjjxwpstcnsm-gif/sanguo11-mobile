package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;
import game.sanguo.api.bridge.BridgeMessage;
import game.sanguo.core.SaveCodec;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.mobile.bridge.AndroidGameBridge;
import game.sanguo.mobile.bridge.BridgeJson;
import game.sanguo.runtime.GameSession;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual installed Android JSON boundary. Separate from normal 3D/audio acceptance. */
public final class MediaBridgeInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();AtomicReference<Throwable> failure=new AtomicReference<>();
        runOnMainSync(()->{
            try(GameSession game=new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L))){
                AndroidGameBridge.bind(game);String id=AndroidGameBridge.sessionId();
                JSONObject initial=new JSONObject(AndroidGameBridge.poll(id));
                JSONArray evidence=initial.getJSONArray("messages");
                check(evidence.length()==1,"actual initial snapshot");
                check(evidence.getJSONObject(0).getJSONObject("state").getLong("generation")==game.state().generation,"actual full state token");
                check(evidence.getJSONObject(0).getJSONArray("techniquePointsFacts").length()==0,"initial no media facts");
                var view=game.legacyView();int owner=view.draft.player,points=view.draft.campaign.points(owner);
                var control=SaveCodec.decode(game.captureSave());
                check(control.editor.apply(control.editor.faction(owner,control.actionPoints[owner],points+20)).ok,"control gain");
                check(control.editor.apply(control.editor.faction(owner,control.actionPoints[owner],points)).ok,"control loss");
                check(game.legacy(view.draft,()->{
                    var first=view.draft.editor.apply(view.draft.editor.faction(owner,view.draft.actionPoints[owner],points+20));
                    if(!first.ok)return first;
                    return view.draft.editor.apply(view.draft.editor.faction(owner,view.draft.actionPoints[owner],points));
                }).ok,"actual committed zero-net change");
                byte[] saved=game.captureSave();JSONArray update=new JSONObject(AndroidGameBridge.poll(id)).getJSONArray("messages");
                check(update.length()==1,"actual once-only delta");JSONObject m=update.getJSONObject(0);evidence.put(m);
                JSONArray facts=m.getJSONArray("techniquePointsFacts");check(facts.length()==2,"actual ordered wire facts");
                check(facts.getJSONObject(0).getInt("delta")==20&&facts.getJSONObject(1).getInt("delta")==-20,"opposite facts retained");
                check(facts.getJSONObject(0).getString("parentId").equals(facts.getJSONObject(1).getString("parentId")),"parent retained");
                check(facts.getJSONObject(0).getJSONObject("state").getLong("revision")==game.state().revision,"fact commit token retained");
                check(new JSONObject(AndroidGameBridge.poll(id)).getJSONArray("messages").length()==0,"drain no replay");
                check(Arrays.equals(saved,game.captureSave())&&Arrays.equals(saved,SaveCodec.encode(control)),"actual serialization full Save/RNG unchanged");
                var bad=game.legacyView();check(!game.legacy(bad.draft,()->bad.draft.patrol(-1,-1)).ok,"actual failed command");
                check(new JSONObject(AndroidGameBridge.poll(id)).getJSONArray("messages").length()==0,"failure no wire facts");
                check(Arrays.equals(saved,game.captureSave()),"failed command full Save/RNG unchanged");
                long exact=9007199254740993L;StateToken token=new StateToken("exact",exact,exact+1);
                var f=new TechniquePointsFact("parent",token,exact,0,1,2,"EDITOR","COMMIT",-1,-1,"journal");
                var huge=new BridgeMessage("delta","exact",exact,exact+1,0,1,1,0,0,null,null,null,null,List.of(),List.of(),token,List.of(f));
                JSONObject encoded=new JSONObject(BridgeJson.message(huge).toString());
                check(encoded.getLong("sequence")==exact&&encoded.getJSONObject("state").getLong("generation")==exact,"Android exact long beyond 2^53");
                check(encoded.getJSONArray("techniquePointsFacts").getJSONObject(0).getLong("sequence")==exact,"Android exact fact long");
                Files.writeString(getTargetContext().getFilesDir().toPath().resolve("media-wire.json"),new JSONObject().put("status","OK").put("messages",evidence).put("dropped",0).toString());
            }catch(Throwable error){failure.set(error);}
            finally{AndroidGameBridge.bind(null);}
        });
        if(failure.get()!=null){android.util.Log.e("MediaBridgeProbe","failed",failure.get());result.putString("mediaWire","FAIL "+failure.get());finish(Activity.RESULT_CANCELED,result);}
        else{result.putString("mediaWire","MEDIA_WIRE PASS checks="+checks+"; installed Android JSON; no Unity Player/audio claim");finish(Activity.RESULT_OK,result);}
    }
}
