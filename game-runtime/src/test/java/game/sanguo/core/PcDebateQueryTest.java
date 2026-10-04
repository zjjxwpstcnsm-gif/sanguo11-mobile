package game.sanguo.core;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.*;

/** Actual API query and its displayed-token commands, not a UI-side rule copy. */
public final class PcDebateQueryTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        String id=PcScenarioCatalog.all().get(0).identity.scenarioId;World p=PcScenarioCatalog.preview(id),w=PcScenarioCatalog.load(id,p.officer(10116).owner,23);PcNativeDebatePolicy.initializeOpening(w);
        int queries=0,steps=0,choices=0;
        try(GameSession game=new GameSession(w)){
            check(game.contest().kind==ContestSnapshot.Kind.NONE,"empty API state");LegacyView start=game.legacyView();check(game.legacy(start.draft,()->start.draft.contests.persuade(start.draft.officer(10116).cityId,10116,10222)).ok,"actual native trigger");
            for(int step=0;step<50;step++){
                byte[] before=game.captureSave();StateToken token=game.state();ContestSnapshot facts=null;
                for(int i=0;i<20;i++){facts=game.contest();check(facts.state.equals(token)&&Arrays.equals(before,game.captureSave()),"API query changes authority/RNG");queries++;}
                check(facts.nativeRules&&facts.kind==ContestSnapshot.Kind.DEBATE&&facts.speakers.size()==2&&facts.speakers.get(0).officerId==10116&&facts.speakers.get(1).officerId==10222,"normal API participant identity");
                check(facts.sourceVariant.equals(PcScenarioIdentity.saved(w).sourceVariant)&&facts.speakers.get(0).sourceVariant.equals(facts.sourceVariant)&&facts.speakers.get(1).sourceVariant.equals(facts.sourceVariant),"native ID/media variant connection");
                try{facts.cards.clear();throw new AssertionError("mutable cards");}catch(UnsupportedOperationException expected){}
                try{facts.speakers.clear();throw new AssertionError("mutable speakers");}catch(UnsupportedOperationException expected){}
                for(ContestSnapshot.Event e:facts.events){choices+=e.choices.size();try{e.choices.add(List.of(0,7));throw new AssertionError("mutable choices");}catch(UnsupportedOperationException expected){}}
                LegacyView detached=game.legacyView();PcDebateCampaign.Facts core=detached.draft.contests.current().nativeDebate();for(ContestSnapshot.Card c:facts.cards)check(c.enabled()==(detached.draft.contests.current().nativeDebate.cardError(c.slot)==null),"core legality source");
                check(core.inputs==facts.revision&&core.round==facts.round,"normal DTO comes from saved model");
                if(!facts.waitingCard)break;
                ContestSnapshot.Card selected=null;for(ContestSnapshot.Card card:facts.cards)if(card.enabled()&&card.nativeCard!=0){selected=card;break;}if(selected==null)for(ContestSnapshot.Card card:facts.cards)if(card.enabled()){selected=card;break;}check(selected!=null,"legal original card");
                ContestCommand command=ContestCommand.card(facts.state,facts.contestId,facts.revision,selected.slot);check(game.execute(command).ok(),"displayed-token normal command");byte[] after=game.captureSave();check(!game.execute(command).ok()&&Arrays.equals(after,game.captureSave()),"displayed-token duplicate rejection");steps++;
            }
            check(steps>2&&choices>0,"actual multiple inputs and recorded original choices");check(queries>100,"repeated normal pure API queries");
        }
        System.out.println("PASS PcDebateQueryTest "+queries+" normal pure API queries, "+steps+" displayed-token commands, "+choices+" saved original choices, immutable lists/source/native IDs/core legality/full World dual-RNG; Android page still pending");
    }
}
