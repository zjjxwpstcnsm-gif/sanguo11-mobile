package game.sanguo.core;

import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;

/** Native price metadata must be a pure forecast of one actual host transaction. */
public final class MerchantMarketSessionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(String operation:new String[]{"BUY","SELL"})for(int amount:new int[]{1,999,20001,289062}){
            World w=CityActionSessionTest.fixture();w.officerAbilities.initializeOpening(null,false,false);
            w.merchantMarket.initializeOpening();w.city(10).gold=50000;w.city(10).food=900000;w.city(10).merchantRate=50;
            OfficerAbilities.setBase(w.officer(1),3,80);w.officerAbilities.gainExperience(1,3,95);w.governance.reconcile(false);w.reports.rebase();
            try(GameSession game=new GameSession(w)){
                List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);StateToken token=game.state();byte[] before=game.captureSave();
                TradeCommand command=new TradeCommand(token,operation,10,1,amount);TradePreview p=game.preview(command);
                check(p.quote.nativePricing&&p.quote.marketRate==50&&p.quote.step==1&&p.quote.rawGoldQuote>=p.quote.quotedGold,"typed source pricing metadata and credit cap");
                check(p.quote.pricingPoliticsBefore==80&&p.quote.pricingPoliticsAfter==81,"typed XP ordering");
                check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"preview is pure including RNG and market state");
                World direct=SaveCodec.decode(before);World.Result expected=direct.campaign.trade(10,1,operation.equals("BUY"),amount);
                CommandResult result=game.execute(command);check(result.ok()==expected.ok,"typed validator equals normal command");
                check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"host/ordinary complete save and RNG match");
                if(result.ok()){
                    check(events.size()==1&&events.get(0).kind==GameEvent.Kind.TRADE_COMMITTED&&game.state().revision==token.revision+1,"one committed sound fact and revision");
                    World after=SaveCodec.decode(game.captureSave());check(after.city(10).gold==p.effects.goldAfter&&after.city(10).food==p.effects.foodAfter,"actual settlement equals effects including capped credit");
                    byte[] committed=game.captureSave();check(game.execute(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(committed,game.captureSave())&&events.size()==1,"duplicate cannot retrade or gain XP");
                    TurnTicket ticket=game.beginTurn();World calculated=SaveCodec.decode(ticket.initial());check(calculated.nextTurn().ok&&game.commitTurn(ticket,calculated),"normal turn with saved market commits");
                    check(Arrays.equals(SaveCodec.encode(calculated),game.captureSave()),"turn capture preserves market/ability/full RNG state");
                }else check(events.isEmpty()&&token.equals(game.state())&&Arrays.equals(before,game.captureSave()),"rejection leaves all authority unchanged");
            }
        }
        System.out.println("PASS MerchantMarketSessionTest checks="+checks+" typed native prices/XP/cap/quantity, pure forecast, exact ordinary/save/RNG and one fact");
    }
}
