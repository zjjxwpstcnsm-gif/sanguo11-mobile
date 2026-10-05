package game.sanguo.mobile.bridge;

import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;
import game.sanguo.api.bridge.BridgeMessage;
import game.sanguo.core.SaveCodec;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.runtime.GameSession;
import game.sanguo.runtime.bridge.BridgeSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual wire output plus full Save/RNG guard; consumed by the real C# client. */
public final class BridgeMediaWireTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        long exact=9007199254740993L;
        StateToken token=new StateToken("exact",exact,exact+1);
        var fact=new TechniquePointsFact("parent",token,exact,0,1,2,"EDITOR","COMMIT",-1,-1,"journal");
        var huge=new BridgeMessage("delta","exact",exact,exact+1,0,1,1,0,0,null,null,null,null,List.of(),List.of(),token,List.of(fact));
        JSONObject json=new JSONObject(BridgeJson.message(huge).toString());
        check(json.get("sequence") instanceof Long&&json.getLong("sequence")==exact,"message long exact");
        check(json.getJSONObject("state").getLong("generation")==exact,"generation exact");
        check(json.getJSONArray("techniquePointsFacts").getJSONObject(0).getLong("sequence")==exact,"fact long exact");
        check(json.getJSONArray("techniquePointsFacts").getJSONObject(0).getString("presentationParentId").equals("journal"),"parent preserved");
        try(GameSession game=new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L));BridgeSession bridge=new BridgeSession(game)){
            bridge.snapshot();JSONArray batch=new JSONArray();
            for(var m:bridge.drain())batch.put(BridgeJson.message(m));
            var view=game.legacyView();int owner=view.draft.player,points=view.draft.campaign.points(owner);
            byte[] initial=game.captureSave();var control=SaveCodec.decode(initial);
            check(control.editor.apply(control.editor.faction(owner,control.actionPoints[owner],points+20)).ok,"control gain");
            check(control.editor.apply(control.editor.faction(owner,control.actionPoints[owner],points)).ok,"control loss");
            check(game.legacy(view.draft,()->{
                var first=view.draft.editor.apply(view.draft.editor.faction(owner,view.draft.actionPoints[owner],points+20));
                if(!first.ok)return first;
                return view.draft.editor.apply(view.draft.editor.faction(owner,view.draft.actionPoints[owner],points));
            }).ok,"actual zero-net commit");
            List<BridgeMessage> committed=bridge.drain();
            check(committed.size()==1&&committed.get(0).techniquePointsFacts.size()==2,"actual ordered facts");
            byte[] saved=game.captureSave();
            for(var m:committed)batch.put(BridgeJson.message(m));
            check(Arrays.equals(saved,game.captureSave())&&Arrays.equals(saved,SaveCodec.encode(control)),"serialization preserves complete save/RNG");
            bridge.snapshot();for(var m:bridge.drain()){
                var wire=BridgeJson.message(m);check(wire.getJSONArray("techniquePointsFacts").length()==0,"active snapshot no replay");batch.put(wire);
            }
            var receipt=bridge.command("denied",1,game.state().revision,"patrol",-1,-1);
            check(receipt.error!=null,"actual failed receipt");
            for(var m:bridge.drain()){check(m.techniquePointsFacts.isEmpty(),"failure no facts");batch.put(BridgeJson.message(m));}
            check(Arrays.equals(saved,game.captureSave()),"failure full save/RNG unchanged");
            Files.writeString(Path.of(args[0]),new JSONObject().put("status","OK").put("messages",batch).put("dropped",0).toString());
        }
        System.out.println("Bridge media wire PASS: "+checks+"; actual JSON and full save/RNG; NOT installed audio");
    }
}
