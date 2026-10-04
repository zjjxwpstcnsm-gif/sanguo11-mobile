package game.sanguo.runtime.bridge;

import game.sanguo.core.*;
import game.sanguo.runtime.GameSession;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.api.bridge.BridgeMessage;

import java.security.MessageDigest;
import java.util.*;

/** Same production scenario, same Java rules and save bytes through both entry paths. */
public final class BridgeSessionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static String hash(World w)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w));
        StringBuilder hex=new StringBuilder();for(byte value:bytes)hex.append(String.format(Locale.ROOT,"%02x",value&255));return hex.toString();
    }
    public static void main(String[] args)throws Exception{
        World direct=ScenarioCatalog.load("coalition-190",0,20260923L);
        World bridged=ScenarioCatalog.load("coalition-190",0,20260923L);
        check(hash(direct).equals(hash(bridged)),"seed baseline");
        GameSession game=new GameSession(bridged);
        BridgeSession session=new BridgeSession(game);
        session.snapshot();List<BridgeMessage> initial=session.drain();
        check(initial.size()==1&&initial.get(0).terrain.length()==bridged.width*bridged.height,"real full map");
        check(initial.get(0).entities.size()>=bridged.cities.size(),"real sites");
        World.City c=null;World.Officer officer=null;
        for(World.City site:bridged.cities)if(site.owner==bridged.active&&site.order<100&&!bridged.idle(site).isEmpty()){
            c=site;officer=bridged.idle(site).get(0);break;
        }
        check(c!=null,"real patrol entry");
        String before=hash(SaveCodec.decode(game.captureSave()));
        BridgeMessage rejected=session.command("bad",1,0,"patrol",c.id,-1);
        check(rejected.error!=null&&before.equals(hash(SaveCodec.decode(game.captureSave()))),"invalid officer leaves save unchanged");
        session.drain();
        World.Result old=direct.patrol(c.id,officer.id);
        BridgeMessage accepted=session.command("one",2,0,"patrol",c.id,officer.id);
        check(old.ok&&accepted.error==null,"both paths accept real command");
        check(hash(direct).equals(hash(SaveCodec.decode(game.captureSave()))),"canonical SaveCodec state and RNG bytes match");
        List<BridgeMessage> update=session.drain();
        check(update.stream().anyMatch(m->m.type.equals("delta")),"incremental change emitted");
        check(update.stream().anyMatch(m->m.type.equals("event")&&m.detail!=null),"confirmed Java event emitted");
        String after=hash(SaveCodec.decode(game.captureSave()));
        check(session.command("one",2,0,"patrol",c.id,officer.id).error==null,"idempotent receipt");
        check(after.equals(hash(SaveCodec.decode(game.captureSave()))),"duplicate cannot execute twice");
        check("CLIENT_SEQUENCE".equals(session.command("late",1,session.revision(),"patrol",c.id,officer.id).error),"out of order rejected");
        check("STALE_REVISION".equals(session.command("stale",3,0,"patrol",c.id,officer.id).error),"stale revision rejected");
        check(after.equals(hash(SaveCodec.decode(game.captureSave()))),"rejected commands leave rules and RNG unchanged");
        session.drain();var view=game.legacyView();int points=view.draft.campaign.points(view.draft.player);
        byte[] beforeFacts=game.captureSave();World factControl=SaveCodec.decode(beforeFacts);
        check(factControl.editor.apply(factControl.editor.faction(factControl.player,factControl.actionPoints[factControl.player],points+20)).ok,
            "real first point edit control");
        check(factControl.editor.apply(factControl.editor.faction(factControl.player,factControl.actionPoints[factControl.player],points)).ok,
            "real offsetting point edit control");
        check(game.legacy(view.draft,()->{
            var first=view.draft.editor.apply(view.draft.editor.faction(view.draft.player,view.draft.actionPoints[view.draft.player],points+20));
            if(!first.ok)return first;
            return view.draft.editor.apply(view.draft.editor.faction(view.draft.player,view.draft.actionPoints[view.draft.player],points));
        }).ok,"real zero-net commit through existing authority");
        var pointBatch=session.drain();
        check(pointBatch.size()==1&&pointBatch.get(0).type.equals("delta"),"one commit delta carries ordered facts");
        var pointMessage=pointBatch.get(0);
        check(pointMessage.state.equals(game.state())&&pointMessage.techniquePointsFacts.size()==2,"bridge carries full generation/revision and zero-net facts");
        var positive=pointMessage.techniquePointsFacts.get(0);var negative=pointMessage.techniquePointsFacts.get(1);
        check(positive.delta==20&&negative.delta==-20&&positive.sequence==1&&negative.sequence==2,"actual ordered values survive transport DTO");
        check(positive.state.equals(pointMessage.state)&&negative.state.equals(pointMessage.state)&&positive.parentId.equals(negative.parentId)
            &&!positive.id.equals(negative.id),"commit-bound parent and distinct dedup identities");
        check(Arrays.equals(game.captureSave(),SaveCodec.encode(factControl)),"fact transport preserves complete control Save/RNG");
        boolean immutable=false;try{pointMessage.techniquePointsFacts.clear();}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"bridge fact collection is immutable");
        boolean rejectedToken=false;
        try{new BridgeMessage("delta",pointMessage.sessionId,1,pointMessage.revision,0,1,1,0,0,null,null,null,null,List.of(),List.of(),
            new game.sanguo.api.StateToken(pointMessage.sessionId,pointMessage.state.generation+1,pointMessage.revision),pointMessage.techniquePointsFacts);
        }catch(IllegalArgumentException expected){rejectedToken=true;}
        check(rejectedToken,"bridge DTO rejects a fact from another generation");
        session.snapshot();var readBatch=session.drain();
        check(readBatch.size()==1&&readBatch.get(0).techniquePointsFacts.isEmpty()&&readBatch.get(0).state.equals(game.state()),"explicit snapshot does not replay transient rewards");
        byte[] afterFacts=game.captureSave();session.command("one",2,0,"patrol",c.id,officer.id);
        check(session.drain().stream().allMatch(m->m.techniquePointsFacts.isEmpty())&&Arrays.equals(afterFacts,game.captureSave()),"duplicate receipt never republishes point facts");
        session.command("fact-stale",4,0,"patrol",c.id,officer.id);
        check(session.drain().stream().allMatch(m->m.techniquePointsFacts.isEmpty())&&Arrays.equals(afterFacts,game.captureSave()),"failed command has no facts or rule mutation");
        var mapView=game.legacyView();
        check(game.legacy(mapView.draft,()->{
            mapView.draft.terrainRevision++;
            return mapView.draft.editor.apply(mapView.draft.editor.faction(mapView.draft.player,mapView.draft.actionPoints[mapView.draft.player],points+1));
        }).ok,"real commit changes map revision and points together");
        var mapBatch=session.drain();
        check(mapBatch.size()==1&&mapBatch.get(0).type.equals("snapshot")&&mapBatch.get(0).techniquePointsFacts.size()==1
            &&mapBatch.get(0).state.equals(game.state()),"commit-triggered full map includes its facts once");
        session.snapshot();check(session.drain().get(0).techniquePointsFacts.isEmpty(),"subsequent full map does not replay commit facts");
        World small=BridgeFactsFixture.create();
        try(GameSession smallGame=new GameSession(small);BridgeSession smallBridge=new BridgeSession(smallGame)){
            var floodView=smallGame.legacyView();byte[] floodBefore=smallGame.captureSave();
            check(smallGame.legacy(floodView.draft,()->{
                World.Result result=null;
                for(int i=0;i<3000;i++){
                    result=floodView.draft.editor.apply(floodView.draft.editor.faction(floodView.draft.player,
                        floodView.draft.actionPoints[floodView.draft.player],250+(i%2==0?2:1)));
                    if(!result.ok)return result;
                }
                return result;
            }).ok,"real large ordered fact commit accepted by authority");
            var floodBatch=smallBridge.drain();
            check(floodBatch.size()==1&&floodBatch.get(0).type.equals("resync")&&floodBatch.get(0).techniquePointsFacts.isEmpty(),
                "fact payload byte overflow drops facts and requires explicit resync");
            check(!Arrays.equals(floodBefore,smallGame.captureSave()),"queue overflow cannot roll back successful authority commit");
            smallBridge.snapshot();check(smallBridge.drain().get(0).techniquePointsFacts.isEmpty(),"overflow recovery does not replay lost point feedback");
        }
        BridgeSession other=new BridgeSession(new GameSession(bridged));
        check(!other.sessionId.equals(session.sessionId),"session identity rotates");
        for(int i=0;i<BridgeSession.MAX_PENDING+1;i++)session.reject("flood:"+i,"TEST");
        check(session.drain().stream().anyMatch(m->m.type.equals("resync")),"bounded queue requires resync after overflow");
        check(session.droppedCount()>0,"overflow counted");
        session.snapshot();check(session.drain().stream().allMatch(m->m.techniquePointsFacts.isEmpty()),"resync snapshot cannot reconstruct or replay lost rewards");
        session.drain();var oldToken=game.state();
        session.reject("pending-before-restore","TEST");check(session.pendingCount()==1,"old authority has actual queued feedback");
        game.replace(bridged);var restoredToken=game.state();
        check(session.drain().isEmpty(),"restore discards pending messages from old authority");
        check(!oldToken.sessionId.equals(restoredToken.sessionId)&&restoredToken.generation==oldToken.generation+1,
            "actual restore rotates identity and generation together");
        byte[] restored=game.captureSave();
        check("STALE_SESSION".equals(session.command("old-after-restore",1,restoredToken.revision,"patrol",c.id,officer.id).error),
            "old bridge cannot execute against restored revision zero");
        check("STALE_SESSION".equals(session.command("one",2,0,"patrol",c.id,officer.id).error),
            "old successful receipt cannot bypass restored session fence");
        check(Arrays.equals(restored,game.captureSave()),"restore fence preserves complete saved state and RNG");
        try(BridgeSession current=new BridgeSession(game)){
            current.snapshot();var batch=current.drain();
            check(!current.sessionId.equals(session.sessionId)&&batch.size()==1&&batch.get(0).sessionId.equals(restoredToken.sessionId),
                "new bridge publishes only restored identity");
            check(Arrays.equals(restored,game.captureSave()),"rebind is read only");
        }
        session.close();game.close();other.close();
        System.out.println("BridgeSessionTest passed; actual scenario, rule parity, dedup, reorder, stale revision and restored identity fence");
    }
}
