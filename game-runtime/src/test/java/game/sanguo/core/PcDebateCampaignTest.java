package game.sanguo.core;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.*;

/** Actual authority transactions and complete World/dual-RNG saves. */
public final class PcDebateCampaignTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception{
        String source=PcScenarioCatalog.all().get(0).identity.scenarioId;World preview=PcScenarioCatalog.preview(source);int actor=10116,target=10222;
        World w=PcScenarioCatalog.load(source,preview.officer(actor).owner,23);
        int city=w.officer(actor).cityId;
        System.out.println("Source actor/target "+w.officer(actor).name+"/"+w.officer(target).name+" city="+city+" targetCity="+w.officer(target).cityId+" owner="+w.officer(actor).owner+"/"+w.officer(target).owner);
        byte[] old=SaveCodec.encode(w);require(old[7]==38,"old source header");World oldRestored=SaveCodec.decode(old);require(!PcNativeDebatePolicy.enabled(oldRestored)&&Arrays.equals(old,SaveCodec.encode(oldRestored)),"old source strategy not backfilled");
        PcNativeDebatePolicy.initializeOpening(w);byte[] enabled=SaveCodec.encode(w);require(enabled[7]==39&&Arrays.equals(enabled,SaveCodec.encode(SaveCodec.decode(enabled))),"explicit new policy full-save round trip");
        World control=SaveCodec.decode(enabled);require(control.contests.persuade(city,actor,target).ok,"actual direct trigger");
        int inputs=0;
        try(GameSession game=new GameSession(w)){
            LegacyView start=game.legacyView();require(game.legacy(start.draft,()->start.draft.contests.persuade(city,actor,target)).ok,"actual authority trigger");
            require(Arrays.equals(SaveCodec.encode(control),game.captureSave()),"start full World/RNG direct equality");
            for(int step=0;step<100;step++){
                byte[] before=game.captureSave();LegacyView view=game.legacyView();Contests.Session session=view.draft.contests.current();PcDebateCampaign.Facts facts=session.nativeDebate();
                require(facts!=null&&Arrays.equals(before,game.captureSave()),"facts preserve complete World/RNG");
                try{facts.hand.set(0,14);throw new AssertionError("mutable facts");}catch(UnsupportedOperationException expected){}
                if(!facts.waitingCard)break;
                int slot=-1;for(int i=1;i<facts.hand.size();i++)if(session.nativeDebate.cardError(i)==null){slot=i;break;}
                if(slot<0)slot=0;
                StateToken token=game.state();ContestCommand command=ContestCommand.card(token,session.id(),session.revision(),slot);
                World.Result direct=control.contests.debateCard(session.id(),session.revision(),slot);require(direct.ok,"direct original input");
                require(game.execute(command).ok(),"actual session original input");byte[] after=game.captureSave();require(Arrays.equals(after,SaveCodec.encode(control)),"complete authority/control World and both RNGs match");
                require(!game.execute(command).ok()&&Arrays.equals(after,game.captureSave()),"duplicate/stale input preserves full save");
                try(GameSession cold=new GameSession(SaveCodec.decode(after))){require(Arrays.equals(after,cold.captureSave()),"actual GameSession cold restore preserves full native model");}
                World restored=SaveCodec.decode(after);require(restored.contests.current().nativeDebate()!=null,"normal contest model restored");inputs++;
            }
            require(inputs>2,"multiple actual player operations");LegacyView terminal=game.legacyView();require(!terminal.draft.contests.current().nativeDebate().waitingCard,"reached original stable terminal input");
            byte[] before=game.captureSave();Contests.Session current=terminal.draft.contests.current();
            if(current.nativeDebate().waitingMercy){require(game.execute(ContestCommand.finishDebate(game.state(),current.id(),current.revision(),true)).ok(),"actual original terminal choice");before=game.captureSave();current=game.legacyView().draft.contests.current();}
            require(!game.execute(ContestCommand.finishDebate(game.state(),current.id(),current.revision(),true)).ok()&&Arrays.equals(before,game.captureSave()),"unverified original campaign settlement is not substituted");
        }
        System.out.println("PASS PcDebateCampaignTest actual source authority trigger, "+inputs+" typed player inputs/full World dual-RNG matches/stale rejects/cold Session restores; original settlement, effective books and installed APK pending");
    }
}
