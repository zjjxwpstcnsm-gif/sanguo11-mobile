package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.api.bridge.*;
import game.sanguo.core.*;
import game.sanguo.runtime.bridge.BridgeSession;
import game.sanguo.runtime.query.TerrainWireCode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class GameSessionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static String hash(GameSession s)throws Exception{return BaselineSequence.hash(s.captureSave());}
    public static void main(String[] args)throws Exception{
        World initial=BaselineSequence.prepared();
        baselineParity(initial);atomicityAndLifecycle(initial);protocol(initial);coordinates();
        System.out.println("GameSessionTest PASS: "+checks+" assertions; old-source full-save parity, native/bridge commands, isolation, lifecycle, turns and protocol.");
    }
    private static void baselineParity(World initial)throws Exception{
        List<String> golden;
        try(InputStream in=GameSessionTest.class.getResourceAsStream("/architecture/baseline-9548bb35.txt")){
            if(in==null)throw new AssertionError("Missing independently recorded old baseline");
            golden=new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
        }
        GameSession nativeGame=new GameSession(initial),bridgeGame=new GameSession(initial);
        BridgeSession bridge=new BridgeSession(bridgeGame);bridge.snapshot();bridge.drain();
        check(golden.get(0).equals("initial "+hash(nativeGame)),"exact old-scenario starting state");int index=0;
        for(int[] op:BaselineSequence.operations(initial)){
            GameCommand.Operation kind=op[0]==1?GameCommand.Operation.RECRUIT:GameCommand.Operation.PATROL;
            CommandResult n=nativeGame.execute(new GameCommand(kind,nativeGame.state(),op[1],op[2]));
            BridgeMessage b=bridge.command("sequence:"+index,index+1,bridge.revision(),op[0]==1?"recruit":"patrol",op[1],op[2]);
            String expected=golden.get(index+1);
            check(expected.equals(index+" "+n.ok()+" "+hash(nativeGame)),"old native baseline complete state step "+index);
            check(expected.equals(index+" "+(b.error==null)+" "+hash(bridgeGame)),"old bridge baseline complete state step "+index);
            check(Arrays.equals(nativeGame.captureSave(),bridgeGame.captureSave()),"new paths byte parity "+index);
            if(n.ok())check(n.event!=null&&n.event.cityId==op[1]&&n.event.officerId==op[2],"structured committed result");
            bridge.drain();index++;
        }
        // Save/restore retains complete relationships, map/custom fields and RNG state.
        GameSession restored=new GameSession(SaveCodec.decode(nativeGame.captureSave()));
        check(hash(restored).equals(hash(nativeGame)),"full save/restore round trip");
        bridge.close();nativeGame.close();bridgeGame.close();restored.close();
    }
    private static void atomicityAndLifecycle(World initial)throws Exception{
        GameSession game=new GameSession(initial,e->{});StateToken first=game.state();
        LegacyView one=game.legacyView(),two=game.legacyView();
        check(one.draft!=two.draft&&first.equals(game.state()),"multiple presentation binds don't create game or advance revision");
        String original=hash(game);one.draft.home().gold--;
        check(hash(game).equals(original),"render/query draft cannot mutate authority");
        World.Result rejected=game.legacy(one.draft,()->{one.draft.strategy.search(one.draft.home().id,one.draft.idle(one.draft.home()).get(0).id);return World.Result.rejected("test failure");});
        check(!rejected.ok&&hash(game).equals(original)&&game.state().equals(first),"failure preserves full save including RNG");
        LegacyView exception=game.legacyView();
        check(!game.legacy(exception.draft,()->{exception.draft.home().gold--;throw new IllegalStateException("injected after mutation");}).ok,"exception becomes rejection");
        check(hash(game).equals(original),"exception atomicity");
        int[] notified={0};GameApi.Subscription subscription=game.subscribe(e->notified[0]++);subscription.close();
        game.subscribe(e->{throw new IllegalStateException("dead view listener");});
        LegacyView toggle=game.legacyView();check(game.legacy(toggle.draft,()->toggle.draft.life.toggle()).ok,"compatible real native command");
        check(notified[0]==0,"unsubscribed page receives no callback");
        check(game.state().revision==1,"one successful commit increments once despite observer failure");
        String committed=hash(game);toggle.draft.home().gold=0;
        check(committed.equals(hash(game)),"retained submitted draft cannot alter installed authority");
        check(!game.legacy(two.draft,()->two.draft.life.toggle()).ok,"old view cannot submit after another commit");
        game.snapshot();game.snapshot();check(hash(game).equals(committed),"queries never alter rule save");
        TurnTicket ticket=game.beginTurn();StateToken old=game.state();
        int actor=initial.idle(initial.home()).get(0).id;
        check(game.execute(new GameCommand(GameCommand.Operation.PATROL,old,initial.home().id,actor)).error==CommandResult.Error.HOST_BUSY,"commands serialized against turn");
        game.replace(initial);StateToken replacement=game.state();
        check(!replacement.sessionId.equals(old.sessionId)&&replacement.generation>old.generation,"new world generation and identity");
        check(!game.commitTurn(ticket,SaveCodec.decode(ticket.initial())),"old computation cannot overwrite loaded game");
        check(game.execute(new GameCommand(GameCommand.Operation.RECRUIT,old,initial.home().id,actor)).error==CommandResult.Error.STALE_SESSION,"stale command cannot affect new world");
        TurnTicket valid=game.beginTurn();World computed=SaveCodec.decode(valid.initial());
        check(computed.nextTurn().ok,"actual isolated next-turn calculation");
        check(game.commitTurn(valid,computed),"current complete turn commits");
        check(game.state().revision==1&&!game.commitTurn(valid,computed),"turn commit is once only, independent of animation");
        check(Arrays.equals(game.captureSave(),SaveCodec.encode(computed)),"turn capture saves final authority");
        AtomicReference<Throwable> wrongThread=new AtomicReference<>();Thread t=new Thread(()->{try{game.snapshot();}catch(Throwable e){wrongThread.set(e);}});t.start();t.join();
        check(wrongThread.get() instanceof IllegalStateException,"wrong-thread rule reads rejected");
        game.close();check(game.execute(new GameCommand(GameCommand.Operation.PATROL,replacement,1,1)).error==CommandResult.Error.CLOSED,"explicit exit closes game");
    }
    private static void protocol(World initial)throws Exception{
        GameSession game=new GameSession(initial);BridgeSession bridge=new BridgeSession(game);bridge.snapshot();bridge.drain();
        LegacyView invisible=game.legacyView();check(game.legacy(invisible.draft,()->invisible.draft.life.toggle()).ok,"hidden state mutation");
        List<BridgeMessage> hidden=bridge.drain();
        check(hidden.size()==1&&hidden.get(0).type.equals("delta")&&hidden.get(0).revision==1&&hidden.get(0).entities.isEmpty(),"invisible authoritative change has versioned empty delta");
        LegacyView deployed=game.legacyView();int city=deployed.draft.home().id;int actor=deployed.draft.idle(deployed.draft.home()).get(0).id;
        check(game.legacy(deployed.draft,()->deployed.draft.deploy(city,actor,World.Weapon.SPEAR,3000)).ok,"legacy deploy still executable");
        check(bridge.drain().stream().anyMatch(m->m.entities.stream().anyMatch(e->e.entityId.startsWith("unit:"))),"unit add delta");
        LegacyView returned=game.legacyView();int unit=returned.draft.officer(actor).unitId;
        World.Result entered=game.legacy(returned.draft,()->returned.draft.marches.execute(returned.draft.marches.previewCity(unit,city)));check(entered.ok,"legacy enter: "+entered.message);
        check(bridge.drain().stream().anyMatch(m->m.removed.contains("unit:"+unit)),"unit deletion delta");
        java.lang.reflect.Field revision=GameSession.class.getDeclaredField("revision");revision.setAccessible(true);revision.setLong(game,9007199254740995L);
        bridge.snapshot();check(bridge.drain().get(0).revision==9007199254740995L,"64-bit revision above double exactness");
        LegacyView view=game.legacyView();int officer=view.draft.idle(view.draft.home()).get(0).id;
        BridgeMessage receipt=bridge.command("large",Long.MAX_VALUE-1,bridge.revision(),"patrol",city,officer);
        check(receipt.error==null&&receipt.revision==9007199254740996L,"receipt matches authority at large revision");
        String after=hash(game);check(bridge.command("large",Long.MAX_VALUE-1,0,"patrol",city,officer).error==null&&after.equals(hash(game)),"retry exactly once");
        check("CLIENT_SEQUENCE".equals(bridge.command("old",5,bridge.revision(),"patrol",city,officer).error),"client order rejection");
        check("STALE_REVISION".equals(bridge.command("stale",Long.MAX_VALUE,2,"patrol",city,officer).error),"version rejection");
        for(int i=0;i<BridgeSession.MAX_PENDING+5;i++)bridge.reject("overflow:"+i,"TEST");
        check(bridge.pendingCount()<=BridgeSession.MAX_PENDING&&bridge.droppedCount()>0,"bounded transport queue");
        check(bridge.drain().stream().anyMatch(m->m.type.equals("resync")),"overflow requests resync");
        bridge.snapshot();check(bridge.drain().get(0).type.equals("snapshot"),"full recovery after overflow");
        game.replace(initial);check("STALE_SESSION".equals(bridge.command("large",1,0,"patrol",city,officer).error),"old duplicate cannot cross world replacement");
        bridge.close();game.close();
        for(World.Terrain terrain:World.Terrain.values())check(TerrainWireCode.encode(terrain)==(char)('A'+terrain.ordinal()),"frozen v1 terrain "+terrain);
    }
    private static void coordinates(){
        for(boolean staggered:new boolean[]{false,true}){
            GridLayout grid=new GridLayout(staggered,99,3,7);
            for(int q=-10;q<210;q+=7)for(int r=-10;r<210;r+=9){
                double x=grid.x(q,r),z=grid.z(q,r);
                check(grid.q(x,z)==q&&grid.r(x,z)==r,"coordinate inverse");
            }
        }
    }
}
