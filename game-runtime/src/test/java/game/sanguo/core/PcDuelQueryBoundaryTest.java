package game.sanguo.core;
import java.util.*;
import java.io.*;
import game.sanguo.api.*;
import game.sanguo.runtime.query.ContestQuery;

/** Query-only artificial model. Does not enable campaign admission or save an
 * unfinished gameplay session. Original continuous corpus verifies the model. */
public final class PcDuelQueryBoundaryTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
        byte[] world=SaveCodec.encode(w);byte[] manager=new byte[0xd0];PcDuelKernel.Actor[][] actors=new PcDuelKernel.Actor[2][3];int[] ids=new int[6],natives=new int[6];
        var people=PcDuelSourceFacts.saved(w).values().stream().limit(6).toList();
        for(int i=0;i<6;i++){var p=people.get(i);ids[i]=p.officerId;natives[i]=p.nativeId;var actor=new PcDuelKernel.Actor(p.nativeId,1,80,0,true,false);actor.originalAge=30;actors[i/3][i%3]=actor;PcDuelKernel.writeManager(manager,4*i,p.officerId);PcDuelKernel.writeManager(manager,0x7c+4*i,100);}
        PcDuelKernel.writeManager(manager,0x40,50);for(int side=0;side<2;side++){PcDuelKernel.writeManager(manager,0x38+4*side,side);PcDuelKernel.writeManager(manager,0x18+4*side,side);}
        var campaign=PcDuelCampaign.initialize(ids,natives,manager,actors,new int[6][0][2],23,new PcDuelKernel.OriginalSettings(false,-1));
        var session=new Contests.Session(1,w.player,w.turn,0,1,-1);session.nativeDuel=campaign;w.contests.session=session;
        var bytes=new ByteArrayOutputStream();campaign.write(new DataOutputStream(bytes));var token=new StateToken("duel-query-test",1,2);
        for(int i=0;i<20;i++){
            var snapshot=ContestQuery.capture(w,token);check(snapshot.kind==ContestSnapshot.Kind.DUEL&&snapshot.nativeRules&&snapshot.nativeDuel!=null,"native duel has its own query branch");
            check(snapshot.nativeDuel.scene==PcDuelKernel.readManager(campaign.state.manager,0x44),"saved original scene DTO");check(!snapshot.nativeDuel.commandBoundary&&!snapshot.nativeDuel.previousOpponentActed&&snapshot.nativeDuel.openingSpeechCode==-1,"old model does not gain command/action/speech facts");
            check(snapshot.state.equals(token)&&snapshot.speakers.size()==2&&!snapshot.settlementAvailable,"token and dormant settlement retained");
            for(var team:snapshot.nativeDuel.teams)for(var f:team.fighters)check(f.officerId==ids[team.side*3+f.slot]&&f.nativeId==natives[team.side*3+f.slot]&&f.name.equals(w.officer(f.officerId).name),"source and runtime IDs remain separate");
            var after=new ByteArrayOutputStream();campaign.write(new DataOutputStream(after));check(Arrays.equals(bytes.toByteArray(),after.toByteArray()),"query retains all model manager RNG bytes");
            try{snapshot.nativeDuel.teams.clear();throw new AssertionError("mutable DTO");}catch(UnsupportedOperationException expected){}
            check(snapshot.nativeDuel.choices.size()==campaign.facts().choices.size(),"API preserves authoritative input list");
            try{snapshot.nativeDuel.choices.clear();throw new AssertionError("mutable choices DTO");}catch(UnsupportedOperationException expected){}
        }
        // Stable runtime identity zero is legal and must not be mistaken for
        // an absent native pointer when selecting detached participant slots.
        int old=campaign.state.officers[0];campaign.state.officers[0]=0;
        check(campaign.facts().teams.get(0).fighters.size()==3&&campaign.facts().teams.get(0).fighters.get(0).officerId==0,"stable ID zero is not a missing slot");campaign.state.officers[0]=old;
        w.contests.session=null;check(Arrays.equals(world,SaveCodec.encode(w)),"query leaves wholeWorld and both RNG unchanged");check(!PcDuelCampaignPolicy.enabled(w),"query cannot enable unfinished gameplay");
        System.out.println("PASS native duel runtime/API query "+checks+" checks; artificial query model, normal admission remains disabled");
    }
}
