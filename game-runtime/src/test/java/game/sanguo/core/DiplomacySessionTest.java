package game.sanguo.core;

import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Lives in the core package to arrange treaties without introducing a production mutation API. */
public final class DiplomacySessionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲",new Hex(5,5),0));w.cities.add(new World.City(20,"乙",new Hex(18,5),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,80+i,90+i,85,85));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));
        w.city(10).gold=30000;w.city(10).food=200000;w.city(20).food=200000;
        w.strategy.initializeOffices();w.strategy.setFactionRelation(0,1,30);return w;
    }
    private static DiplomacyCommand command(StateToken state,String operation,int turns){return new DiplomacyCommand(state,10,0,1,operation,turns);}
    public static void main(String[] args)throws Exception{
        for(DiplomacyPlan.Operation op:DiplomacyPlan.Operation.values())for(int duration:new int[]{3,6,12}){
            World initial=fixture();if(op==DiplomacyPlan.Operation.BREAK_TREATY)initial.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);
            try(GameSession game=new GameSession(initial)){
                byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
                DiplomacyCommand c=command(token,op.name(),duration);DiplomacyPreview p=game.preview(c);
                check(p.allowed()&&p.forecast!=null,"allowed preview "+op);
                for(int i=0;i<8;i++)check(game.preview(c).forecast.initialAcceptancePercent==p.forecast.initialAcceptancePercent,"cached query deterministic");
                boolean immutable=false;try{p.treatyDurations.clear();}catch(UnsupportedOperationException expected){immutable=true;}
                check(immutable&&p.treatyDurations.equals(Arrays.asList(3,6,12)),"immutable allowed durations");
                check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"no RNG, save, revision, or event effects from query");
                World direct=SaveCodec.decode(before);check(direct.campaign.diplomaticAction(10,0,1,op,duration).ok,"ordinary core command");
                CommandResult result=game.execute(c);check(result.ok(),result.detail);
                check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"typed equals ordinary full save and RNG");
                check(result.state.revision==token.revision+1&&events.size()==1,"one commit, one notification");
                check(result.event.kind==(op==DiplomacyPlan.Operation.BREAK_TREATY?GameEvent.Kind.TREATY_BROKEN:GameEvent.Kind.DIPLOMACY_DISPATCHED),"event marks departure, not treaty acceptance");
                check(direct.city(10).gold==p.resources.goldRemaining&&direct.actionPoints[0]==p.resources.actionPointsRemaining,"authoritative resources match preview");
                DiplomacyPreview fresh=game.preview(command(game.state(),op.name(),duration));
                check(fresh.reasonCode.equals("LEADER_UNAVAILABLE")&&fresh.resources.goldAvailable==p.resources.goldRemaining,"successful commit invalidates shared query cache");
                byte[] committed=game.captureSave();check(game.preview(c).error==CommandResult.Error.STALE_REVISION&&game.execute(c).error==CommandResult.Error.STALE_REVISION,"stale query and double confirmation rejected");
                check(Arrays.equals(committed,game.captureSave())&&events.size()==1,"duplicate causes no effects");
                World loaded=SaveCodec.decode(committed);
                for(int turn=0;turn<6;turn++){
                    TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                    check(computed.nextTurn().ok&&direct.nextTurn().ok&&loaded.nextTurn().ok,"real complete turn");
                    check(game.commitTurn(ticket,computed),"turn commits");
                    check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct))&&Arrays.equals(game.captureSave(),SaveCodec.encode(loaded)),"departure/arrival/return and save replay across turns");
                }
            }
        }
        try(GameSession game=new GameSession(fixture())){
            StateToken state=game.state();byte[] before=game.captureSave();
            for(String op:new String[]{null,"not-an-operation","ceasefire","BREAK_TREATY"}){
                DiplomacyCommand c=command(state,op,0);DiplomacyPreview p=game.preview(c);CommandResult r=game.execute(c);
                check(p.error==CommandResult.Error.RULE_REJECTED&&r.error==p.error&&r.reasonCode.equals(p.reasonCode)&&r.detail.equals(p.detail),"structured wire failure");
                check(p.forecast==null&&Arrays.equals(before,game.captureSave())&&state.equals(game.state()),"rejection atomic");
            }
            DiplomacyCommand c=command(state,"GOODWILL",0);TurnTicket turn=game.beginTurn();
            check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"turn blocks preview/command");game.cancelTurn(turn);
            AtomicReference<Throwable> wrong=new AtomicReference<>();Thread other=new Thread(()->{try{game.preview(c);}catch(Throwable t){wrong.set(t);}});other.start();other.join();
            check(wrong.get() instanceof IllegalStateException,"serial thread required");
            game.replace(fixture());check(game.preview(c).error==CommandResult.Error.STALE_SESSION&&game.execute(c).error==CommandResult.Error.STALE_SESSION,"replace invalidates token");
            game.close();check(game.preview(c).error==CommandResult.Error.CLOSED&&game.execute(c).error==CommandResult.Error.CLOSED,"close blocks calls");
        }
        System.out.println("PASS DiplomacySessionTest checks="+checks);
    }
}
